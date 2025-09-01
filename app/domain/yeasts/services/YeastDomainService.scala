package domain.yeasts.services

import domain.yeasts.model._
import domain.yeasts.repositories.YeastReadRepository
import domain.shared.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service domaine pour la logique métier complexe des levures
 * Orchestration des règles business transversales
 */
@Singleton
class YeastDomainService @Inject()(
  yeastReadRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Vérifie l'unicité d'une combinaison laboratoire/souche
   */
  def checkUniqueLaboratoryStrain(
    laboratory: YeastLaboratory, 
    strain: YeastStrain,
    excludeId: Option[YeastId] = None
  ): Future[Either[DomainError, Unit]] = {
    yeastReadRepository.findByLaboratoryAndStrain(laboratory, strain).map {
      case Some(existing) if excludeId.forall(_ != existing.id) =>
        Left(DomainError.BusinessRuleViolation(
          s"La combinaison ${laboratory.name} ${strain.value} existe déjà"
        ))
      case _ => Right(())
    }
  }

  /**
   * Recherche de levures similaires par caractéristiques
   */
  def findSimilarYeasts(
    yeast: YeastAggregate,
    maxResults: Int = 5
  ): Future[List[YeastAggregate]] = {
    val filter = YeastFilter(
      yeastType = Some(yeast.yeastType),
      status = List(YeastStatus.Active)
    ).copy(size = maxResults * 2) // Plus large pour filtrer ensuite
    
    yeastReadRepository.findByFilter(filter).map { result =>
      result.items
        .filter(_.id != yeast.id) // Exclure la levure elle-même
        .sortBy(other => calculateSimilarityScore(yeast, other))
        .reverse
        .take(maxResults)
    }
  }

  /**
   * Recommande des levures pour un style de bière
   */
  def recommendYeastsForBeerStyle(
    beerStyle: String,
    targetAbv: Option[Double] = None,
    maxResults: Int = 10
  ): Future[List[(YeastAggregate, Double)]] = {
    
    val recommendedTypes = YeastType.recommendedForBeerStyle(beerStyle)
    val typeFilters = recommendedTypes.map(t => YeastFilter(yeastType = Some(t)))
    
    // Rechercher dans tous les types recommandés
    Future.sequence(typeFilters.map(filter => 
      yeastReadRepository.findByFilter(filter.copy(size = 50))
    )).map { results =>
      val allYeasts = results.flatMap(_.items)
      
      // Scorer et trier par pertinence
      allYeasts
        .filter(_.status == YeastStatus.Active)
        .filter(yeast => targetAbv.forall(abv => yeast.alcoholTolerance.canFerment(abv)))
        .map(yeast => (yeast, calculateBeerStyleScore(yeast, beerStyle, targetAbv)))
        .sortBy(-_._2)
        .take(maxResults)
    }
  }

  /**
   * Analyse de compatibilité fermentation
   */
  def analyzeFermentationCompatibility(
    yeast: YeastAggregate,
    targetTemp: Int,
    targetAbv: Double,
    targetAttenuation: Int
  ): FermentationCompatibilityReport = {
    
    val tempCompatible = yeast.temperature.contains(targetTemp)
    val abvCompatible = yeast.alcoholTolerance.canFerment(targetAbv)
    val attenuationCompatible = yeast.attenuation.contains(targetAttenuation)
    
    val warnings = List(
      if (!tempCompatible) Some(s"Température ${targetTemp}°C hors plage optimale ${yeast.temperature}") else None,
      if (!abvCompatible) Some(s"ABV ${targetAbv}% dépasse la tolérance ${yeast.alcoholTolerance}") else None,
      if (!attenuationCompatible) Some(s"Atténuation ${targetAttenuation}% hors plage ${yeast.attenuation}") else None
    ).flatten
    
    val overallScore = List(tempCompatible, abvCompatible, attenuationCompatible)
      .count(identity).toDouble / 3.0
    
    FermentationCompatibilityReport(
      yeast = yeast,
      temperatureCompatible = tempCompatible,
      alcoholCompatible = abvCompatible,
      attenuationCompatible = attenuationCompatible,
      overallScore = overallScore,
      warnings = warnings,
      recommendations = generateRecommendations(yeast, targetTemp, targetAbv, targetAttenuation)
    )
  }

  /**
   * Détecte les levures en double potentielles
   */
  def detectPotentialDuplicates(
    yeast: YeastAggregate,
    threshold: Double = 0.8
  ): Future[List[YeastAggregate]] = {
    val filter = YeastFilter(
      laboratory = Some(yeast.laboratory),
      yeastType = Some(yeast.yeastType)
    ).copy(size = 100)
    
    yeastReadRepository.findByFilter(filter).map { result =>
      result.items
        .filter(_.id != yeast.id)
        .filter(other => calculateSimilarityScore(yeast, other) >= threshold)
    }
  }

  /**
   * Validation des modifications de levure
   */
  def validateYeastModification(
    original: YeastAggregate,
    updates: YeastUpdateRequest
  ): Future[Either[List[DomainError], Unit]] = {
    
    val validations = List(
      // Validation unicité si laboratoire/souche changent
      if (updates.laboratory.isDefined || updates.strain.isDefined) {
        val newLab = updates.laboratory.getOrElse(original.laboratory)
        val newStrain = updates.strain.getOrElse(original.strain)
        if (newLab != original.laboratory || newStrain != original.strain) {
          checkUniqueLaboratoryStrain(newLab, newStrain, Some(original.id))
        } else {
          Future.successful(Right(()))
        }
      } else {
        Future.successful(Right(()))
      }
    )
    
    Future.sequence(validations).map { results =>
      val errors = results.collect { case Left(error) => error }
      if (errors.nonEmpty) Left(errors) else Right(())
    }
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - ALGORITHMES DE SCORING
  // ==========================================================================

  private def calculateSimilarityScore(yeast1: YeastAggregate, yeast2: YeastAggregate): Double = {
    var score = 0.0
    var factors = 0
    
    // Type identique (poids fort)
    if (yeast1.yeastType == yeast2.yeastType) {
      score += 0.3
    }
    factors += 1
    
    // Laboratoire identique
    if (yeast1.laboratory == yeast2.laboratory) {
      score += 0.2
    }
    factors += 1
    
    // Plages d'atténuation qui se chevauchent
    if (rangesOverlap(yeast1.attenuation.min, yeast1.attenuation.max, 
                     yeast2.attenuation.min, yeast2.attenuation.max)) {
      score += 0.2
    }
    factors += 1
    
    // Plages de température qui se chevauchent
    if (rangesOverlap(yeast1.temperature.min, yeast1.temperature.max,
                     yeast2.temperature.min, yeast2.temperature.max)) {
      score += 0.15
    }
    factors += 1
    
    // Floculation similaire
    if (yeast1.flocculation == yeast2.flocculation) {
      score += 0.1
    }
    factors += 1
    
    // Caractéristiques communes
    val commonCharacteristics = yeast1.characteristics.allCharacteristics
      .intersect(yeast2.characteristics.allCharacteristics)
    if (commonCharacteristics.nonEmpty) {
      score += 0.05
    }
    factors += 1
    
    score / factors
  }

  private def calculateBeerStyleScore(
    yeast: YeastAggregate, 
    beerStyle: String, 
    targetAbv: Option[Double]
  ): Double = {
    var score = 0.0
    
    // Score de base selon le type
    if (yeast.isCompatibleWith(beerStyle)) score += 0.4
    
    // Bonus pour tolérance alcool si spécifiée
    targetAbv.foreach { abv =>
      if (yeast.alcoholTolerance.canFerment(abv)) {
        score += 0.3
      } else if (yeast.alcoholTolerance.percentage >= abv - 1.0) {
        score += 0.15 // Tolérable avec marge
      }
    }
    
    // Bonus caractéristiques spécifiques
    val styleLC = beerStyle.toLowerCase
    val characteristics = yeast.characteristics.allCharacteristics.map(_.toLowerCase)
    
    if (styleLC.contains("ipa") && characteristics.exists(_.contains("citrus"))) score += 0.1
    if (styleLC.contains("wheat") && characteristics.exists(_.contains("banana"))) score += 0.1
    if (styleLC.contains("lager") && characteristics.exists(_.contains("clean"))) score += 0.1
    if (styleLC.contains("saison") && characteristics.exists(_.contains("spic"))) score += 0.1
    
    score
  }

  private def rangesOverlap(min1: Int, max1: Int, min2: Int, max2: Int): Boolean = {
    math.max(min1, min2) <= math.min(max1, max2)
  }

  private def generateRecommendations(
    yeast: YeastAggregate,
    targetTemp: Int, 
    targetAbv: Double, 
    targetAttenuation: Int
  ): List[String] = {
    var recommendations = List.empty[String]
    
    if (!yeast.temperature.contains(targetTemp)) {
      recommendations = s"Ajuster température à ${yeast.temperature.min}-${yeast.temperature.max}°C pour optimiser" :: recommendations
    }
    
    if (!yeast.alcoholTolerance.canFerment(targetAbv)) {
      recommendations = s"Réduire l'ABV cible sous ${yeast.alcoholTolerance.percentage}%" :: recommendations
    }
    
    if (yeast.flocculation == FlocculationLevel.High && targetAttenuation > yeast.attenuation.min) {
      recommendations = "Envisager remise en suspension pour atteindre l'atténuation cible" :: recommendations
    }
    
    recommendations.reverse
  }
}

/**
 * Rapport de compatibilité fermentation
 */
case class FermentationCompatibilityReport(
  yeast: YeastAggregate,
  temperatureCompatible: Boolean,
  alcoholCompatible: Boolean,
  attenuationCompatible: Boolean,
  overallScore: Double,
  warnings: List[String],
  recommendations: List[String]
) {
  def isFullyCompatible: Boolean = temperatureCompatible && alcoholCompatible && attenuationCompatible
  def compatibilityLevel: String = overallScore match {
    case s if s >= 0.9 => "Excellent"
    case s if s >= 0.7 => "Bon"
    case s if s >= 0.5 => "Acceptable"
    case _ => "Problématique"
  }
}

/**
 * Requête de mise à jour de levure
 */
case class YeastUpdateRequest(
  name: Option[YeastName] = None,
  laboratory: Option[YeastLaboratory] = None,
  strain: Option[YeastStrain] = None,
  attenuation: Option[AttenuationRange] = None,
  temperature: Option[FermentationTemp] = None,
  alcoholTolerance: Option[AlcoholTolerance] = None,
  flocculation: Option[FlocculationLevel] = None,
  characteristics: Option[YeastCharacteristics] = None
)
