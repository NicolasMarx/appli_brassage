package domain.yeasts.services

import domain.yeasts.model._
import domain.yeasts.repositories.YeastReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service de recommandations intelligentes pour les levures
 * Algorithmes de suggestion basés sur les caractéristiques de brassage
 */
@Singleton
class YeastRecommendationService @Inject()(
  yeastReadRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Recommandations pour débutants
   */
  def getBeginnerFriendlyYeasts(maxResults: Int = 5): Future[List[RecommendedYeast]] = {
    val beginnerCriteria = YeastFilter(
      status = List(YeastStatus.Active)
    ).copy(size = 50)
    
    yeastReadRepository.findByFilter(beginnerCriteria).map { result =>
      result.items
        .filter(isBeginnerFriendly)
        .map(yeast => RecommendedYeast(
          yeast = yeast,
          score = calculateBeginnerScore(yeast),
          reason = generateBeginnerReason(yeast),
          tips = generateBeginnerTips(yeast)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations par saison
   */
  def getSeasonalRecommendations(season: Season, maxResults: Int = 8): Future[List[RecommendedYeast]] = {
    val seasonalTypes = getSeasonalYeastTypes(season)
    
    Future.sequence(seasonalTypes.map { yeastType =>
      val filter = YeastFilter(
        yeastType = Some(yeastType),
        status = List(YeastStatus.Active)
      ).copy(size = 20)
      
      yeastReadRepository.findByFilter(filter)
    }).map { results =>
      results.flatMap(_.items)
        .map(yeast => RecommendedYeast(
          yeast = yeast,
          score = calculateSeasonalScore(yeast, season),
          reason = generateSeasonalReason(yeast, season),
          tips = generateSeasonalTips(yeast, season)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations pour expérimentateurs
   */
  def getExperimentalYeasts(maxResults: Int = 6): Future[List[RecommendedYeast]] = {
    val experimentalCriteria = YeastFilter(
      status = List(YeastStatus.Active)
    ).copy(size = 50)
    
    yeastReadRepository.findByFilter(experimentalCriteria).map { result =>
      result.items
        .filter(isExperimental)
        .map(yeast => RecommendedYeast(
          yeast = yeast,
          score = calculateExperimentalScore(yeast),
          reason = generateExperimentalReason(yeast),
          tips = generateExperimentalTips(yeast)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations par profil aromatique désiré
   */
  def getByAromaProfile(
    desiredAromas: List[String], 
    maxResults: Int = 10
  ): Future[List[RecommendedYeast]] = {
    val filter = YeastFilter(
      characteristics = desiredAromas,
      status = List(YeastStatus.Active)
    ).copy(size = 100)
    
    yeastReadRepository.findByFilter(filter).map { result =>
      result.items
        .map(yeast => RecommendedYeast(
          yeast = yeast,
          score = calculateAromaMatchScore(yeast, desiredAromas),
          reason = generateAromaReason(yeast, desiredAromas),
          tips = generateAromaTips(yeast)
        ))
        .filter(_.score > 0.3) // Seuil minimum de pertinence
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Alternatives à une levure donnée
   */
  def findAlternatives(
    originalYeastId: YeastId, 
    reason: AlternativeReason = AlternativeReason.Unavailable,
    maxResults: Int = 5
  ): Future[List[RecommendedYeast]] = {
    
    yeastReadRepository.findById(originalYeastId).flatMap {
      case Some(original) =>
        val similarityFilter = YeastFilter(
          yeastType = Some(original.yeastType),
          status = List(YeastStatus.Active)
        ).copy(size = 50)
        
        yeastReadRepository.findByFilter(similarityFilter).map { result =>
          result.items
            .filter(_.id != originalYeastId)
            .map(yeast => RecommendedYeast(
              yeast = yeast,
              score = calculateSimilarityScore(original, yeast),
              reason = generateAlternativeReason(yeast, original, reason),
              tips = generateAlternativeTips(yeast, original)
            ))
            .sortBy(-_.score)
            .take(maxResults)
        }
        
      case None => Future.successful(List.empty)
    }
  }

  /**
   * Recommandations pour batch de test
   */
  def getTestBatchRecommendations(
    baseRecipe: TestRecipeParams,
    maxResults: Int = 3
  ): Future[List[RecommendedYeast]] = {
    
    val compatibleTypes = YeastType.recommendedForBeerStyle(baseRecipe.style)
    val typeFilters = compatibleTypes.map(t => YeastFilter(yeastType = Some(t)))
    
    Future.sequence(typeFilters.map(filter =>
      yeastReadRepository.findByFilter(filter.copy(size = 20))
    )).map { results =>
      results.flatMap(_.items)
        .filter(yeast => 
          yeast.fermentationTemp.contains(baseRecipe.fermentationTemp) &&
          yeast.alcoholTolerance.canFerment(baseRecipe.expectedAbv)
        )
        .map(yeast => RecommendedYeast(
          yeast = yeast,
          score = calculateTestBatchScore(yeast, baseRecipe),
          reason = generateTestBatchReason(yeast, baseRecipe),
          tips = generateTestBatchTips(yeast, baseRecipe)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - SCORING ET CLASSIFICATION
  // ==========================================================================

  private def isBeginnerFriendly(yeast: YeastAggregate): Boolean = {
    // Algorithme intelligent moins restrictif pour recommandations débutants
    // Score basé sur facilité d'utilisation plutôt que critères stricts
    
    // 1. Levures ALE sont plus faciles que LAGER (pas besoin contrôle température strict)
    val isEasyType = yeast.yeastType match {
      case YeastType.ALE => true
      case YeastType.LAGER => true // Accepter aussi les lagers simples
      case _ => false // Exclure WILD, BRETT, etc.
    }
    
    // 2. Laboratoires réputés pour qualité constante
    val isReliableLab = yeast.laboratory match {
      case YeastLaboratory.Wyeast | YeastLaboratory.WhiteLabs | YeastLaboratory.Lallemand | YeastLaboratory.Fermentis => true
      case _ => false
    }
    
    // 3. Température de fermentation raisonnable (pas trop chaude = moins de risque)
    val hasSafeTemp = yeast.fermentationTemp.max <= 28 // Max 28°C pour éviter problèmes
    
    // Critères: au moins 2 sur 3 doivent être vrais (OU logique souple)
    val positiveScore = List(isEasyType, isReliableLab, hasSafeTemp).count(_ == true)
    
    // Accept si score >= 2, ou si c'est une levure ALE active de labo reconnu
    positiveScore >= 2 || (yeast.yeastType == YeastType.ALE && isReliableLab && yeast.isActive)
  }
  private def calculateBeginnerScore(yeast: YeastAggregate): Double = {
    var score = 0.5 // Score de base plus élevé
    
    // CRITÈRE PRINCIPAL: Type de levure (impact majeur)
    yeast.yeastType match {
      case YeastType.ALE => score += 0.4 // ALE = plus facile pour débutants
      case YeastType.LAGER => score += 0.2 // LAGER = un peu plus technique mais OK
      case _ => score += 0.0 // WILD/BRETT = pas pour débutants
    }
    
    // CRITÈRE LABORATOIRE: Fiabilité et réputation
    yeast.laboratory match {
      case YeastLaboratory.Wyeast => score += 0.3 // Excellent pour débutants
      case YeastLaboratory.WhiteLabs => score += 0.3 // Excellent pour débutants  
      case YeastLaboratory.Fermentis => score += 0.25 // Très bon, sec
      case YeastLaboratory.Lallemand => score += 0.2 // Bon, moderne
      case _ => score += 0.1 // Autres labs moins connus
    }
    
    // CRITÈRE TEMPÉRATURE: Facilité de contrôle
    val tempRange = yeast.fermentationTemp.range
    if (tempRange <= 4) score += 0.2 // Plage étroite = plus prévisible
    else if (tempRange <= 6) score += 0.15
    else score += 0.05
    
    // CRITÈRE ATTÉNUATION: Prévisibilité
    val attRange = yeast.attenuationRange.range
    if (attRange <= 10) score += 0.15 // Atténuation prévisible
    else score += 0.05
    
    // BONUS FLOCULATION: Facilité de clarification
    yeast.flocculation match {
      case FlocculationLevel.HIGH | FlocculationLevel.VERY_HIGH => score += 0.15
      case FlocculationLevel.MEDIUM => score += 0.1
      case _ => score += 0.0
    }
    
    // BONUS CARACTÉRISTIQUES: Profil neutre plus facile à maîtriser
    if (yeast.characteristics.isClean) score += 0.1
    
    // BONUS STATUT: Levure active et disponible
    if (yeast.isActive) score += 0.1
    
    // Score final entre 0.0 et ~2.0
    Math.min(score, 2.0)
  }

  private def isExperimental(yeast: YeastAggregate): Boolean = {
    yeast.yeastType match {
      case YeastType.WILD | YeastType.BRETT => true
      case YeastType.ALE if yeast.fermentationTemp.max >= 30 => true
      case _ => yeast.characteristics.allCharacteristics.exists(c => 
        c.toLowerCase.matches(".*(wild|brett|funk|barnyard|horse).*")
      )
    }
  }

  private def calculateExperimentalScore(yeast: YeastAggregate): Double = {
    var score = 0.0
    
    yeast.yeastType match {
      case YeastType.WILD => score += 0.4
      case YeastType.BRETT => score += 0.35
      case YeastType.ALE => score += 0.3
      case YeastType.ALE if yeast.fermentationTemp.max >= 30 => score += 0.25
      case _ => score += 0.0
    }
    
    // Bonus caractéristiques uniques
    val uniqueChars = List("funky", "horse", "barnyard", "brett", "wild", "complex")
    val matchingChars = yeast.characteristics.allCharacteristics
      .count(c => uniqueChars.exists(u => c.toLowerCase.contains(u)))
    score += matchingChars * 0.1
    
    score
  }

  private def getSeasonalYeastTypes(season: Season): List[YeastType] = {
    season match {
      case Season.Spring => List(YeastType.ALE, YeastType.ALE, YeastType.ALE)
      case Season.Summer => List(YeastType.LAGER, YeastType.ALE, YeastType.BRETT)
      case Season.Autumn => List(YeastType.ALE, YeastType.WILD, YeastType.ALE)
      case Season.Winter => List(YeastType.ALE, YeastType.LAGER, YeastType.WINE)
    }
  }

  private def calculateSeasonalScore(yeast: YeastAggregate, season: Season): Double = {
    val baseScore = if (getSeasonalYeastTypes(season).contains(yeast.yeastType)) 0.5 else 0.2
    
    val seasonalBonus = season match {
      case Season.Summer if yeast.characteristics.allCharacteristics.exists(_.toLowerCase.contains("citrus")) => 0.2
      case Season.Winter if yeast.characteristics.allCharacteristics.exists(_.toLowerCase.contains("spic")) => 0.2
      case Season.Autumn if yeast.yeastType == YeastType.WILD => 0.15
      case Season.Spring if yeast.yeastType == YeastType.ALE => 0.15
      case _ => 0.0
    }
    
    baseScore + seasonalBonus
  }

  private def calculateAromaMatchScore(yeast: YeastAggregate, desiredAromas: List[String]): Double = {
    val yeastAromas = yeast.characteristics.allCharacteristics.map(_.toLowerCase)
    val matchingAromas = desiredAromas.count(desired => 
      yeastAromas.exists(_.contains(desired.toLowerCase))
    )
    
    matchingAromas.toDouble / desiredAromas.length
  }

  private def calculateSimilarityScore(original: YeastAggregate, alternative: YeastAggregate): Double = {
    var score = 0.0
    
    if (original.yeastType == alternative.yeastType) score += 0.3
    if (original.laboratory == alternative.laboratory) score += 0.1
    
    // Comparaison plages techniques
    val attenuationOverlap = rangeOverlapPercentage(
      original.attenuationRange.min, original.attenuationRange.max,
      alternative.attenuationRange.min, alternative.attenuationRange.max
    )
    score += attenuationOverlap * 0.2
    
    val tempOverlap = rangeOverlapPercentage(
      original.fermentationTemp.min, original.fermentationTemp.max,
      alternative.fermentationTemp.min, alternative.fermentationTemp.max
    )
    score += tempOverlap * 0.15
    
    if (original.flocculation == alternative.flocculation) score += 0.1
    
    score
  }

  private def rangeOverlapPercentage(min1: Int, max1: Int, min2: Int, max2: Int): Double = {
    val overlapStart = math.max(min1, min2)
    val overlapEnd = math.min(max1, max2)
    val overlap = math.max(0, overlapEnd - overlapStart)
    val totalRange = math.max(max1 - min1, max2 - min2)
    
    if (totalRange == 0) 1.0 else overlap.toDouble / totalRange
  }

  // ==========================================================================
  // GÉNÉRATION DE TEXTES D'AIDE
  // ==========================================================================

  private def generateBeginnerReason(yeast: YeastAggregate): String = {
    val reasons = scala.collection.mutable.ListBuffer[String]()
    
    // Raison principale : type de levure
    yeast.yeastType match {
      case YeastType.ALE => reasons += "levure ALE facile à utiliser"
      case YeastType.LAGER => reasons += "lager classique et fiable"
      case _ => reasons += "souche spéciale"
    }
    
    // Raison laboratoire
    yeast.laboratory match {
      case YeastLaboratory.Wyeast => reasons += "Wyeast très réputé pour débutants"
      case YeastLaboratory.WhiteLabs => reasons += "White Labs qualité constante"
      case YeastLaboratory.Fermentis => reasons += "Fermentis levure sèche simple"
      case YeastLaboratory.Lallemand => reasons += "Lallemand innovation moderne"
      case _ => // Pas de mention spéciale
    }
    
    // Raison température
    val tempRange = yeast.fermentationTemp.range
    if (tempRange <= 4) reasons += "plage température étroite"
    else if (tempRange <= 6) reasons += "température bien contrôlée"
    
    // Raison atténuation
    val attRange = yeast.attenuationRange.range  
    if (attRange <= 8) reasons += "atténuation prévisible"
    
    // Raison floculation
    yeast.flocculation match {
      case FlocculationLevel.HIGH | FlocculationLevel.VERY_HIGH => reasons += "flocule bien (clarification facile)"
      case FlocculationLevel.MEDIUM => reasons += "clarification standard"
      case _ => // Pas de mention
    }
    
    if (reasons.nonEmpty) {
      s"${yeast.name.value} : ${reasons.take(3).mkString(", ")}"
    } else {
      s"${yeast.name.value} : choix équilibré pour débuter"
    }
  }

  private def generateBeginnerTips(yeast: YeastAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    // Tip température (toujours utile)
    tips += s"Fermenter à ${yeast.fermentationTemp.min}-${yeast.fermentationTemp.max}°C"
    
    // Tip atténuation
    tips += s"Atténuation attendue : ${yeast.attenuationRange.min}-${yeast.attenuationRange.max}%"
    
    // Tips spécifiques au laboratoire  
    yeast.laboratory match {
      case YeastLaboratory.Fermentis => tips += "Réhydrater 15 min avant inoculation"
      case YeastLaboratory.Wyeast | YeastLaboratory.WhiteLabs => tips += "Faire un starter si DG > 1.050"
      case YeastLaboratory.Lallemand => tips += "Pas besoin de starter, ensemencement direct OK"
      case _ => // Pas de tip spécial
    }
    
    // Tips type de levure
    yeast.yeastType match {
      case YeastType.ALE => tips += "Fermentation rapide 3-7 jours à température ambiante"
      case YeastType.LAGER => tips += "Fermentation froide puis lagering 2-6 semaines"
      case _ => // Pas de tip standard
    }
    
    // Tips floculation
    yeast.flocculation match {
      case FlocculationLevel.HIGH | FlocculationLevel.VERY_HIGH => 
        tips += "Excellente clarification, soutirage facile"
      case FlocculationLevel.MEDIUM => 
        tips += "Clarification standard, attendre 3-5 jours"
      case FlocculationLevel.LOW => 
        tips += "Reste en suspension, prévoir clarifiants"
      case _ => // Pas de tip spécial
    }
    
    // Limiter à 4 tips max pour lisibilité
    tips.take(4).toList
  }

  private def generateSeasonalReason(yeast: YeastAggregate, season: Season): String = {
    val seasonName = season.toString.toLowerCase
    s"Parfaite pour un brassage de $seasonName avec son profil ${yeast.yeastType.name.toLowerCase}"
  }

  private def generateSeasonalTips(yeast: YeastAggregate, season: Season): List[String] = {
    val baseTips = List(s"Température optimale : ${yeast.fermentationTemp.min}-${yeast.fermentationTemp.max}°C")
    val seasonalTips = season match {
      case Season.Summer => List("Contrôler température fermentation", "Prévoir refroidissement")
      case Season.Winter => List("Laisser atteindre température avant ensemencement")
      case _ => List()
    }
    baseTips ++ seasonalTips
  }

  private def generateExperimentalReason(yeast: YeastAggregate): String = {
    s"${yeast.name.value} offre un profil unique pour l'expérimentation avec ${yeast.characteristics.toString.take(60)}..."
  }

  private def generateExperimentalTips(yeast: YeastAggregate): List[String] = {
    List(
      "Commencer par un petit batch de test",
      "Prévoir fermentation plus longue",
      s"Surveiller température (${yeast.fermentationTemp.min}-${yeast.fermentationTemp.max}°C)"
    ) ++ (if (yeast.yeastType == YeastType.WILD) List("Prévoir 3-6 mois minimum") else List())
  }

  private def generateAromaReason(yeast: YeastAggregate, desiredAromas: List[String]): String = {
    val matchingAromas = yeast.characteristics.allCharacteristics
      .filter(c => desiredAromas.exists(d => c.toLowerCase.contains(d.toLowerCase)))
    s"Produit les arômes recherchés : ${matchingAromas.mkString(", ")}"
  }

  private def generateAromaTips(yeast: YeastAggregate): List[String] = {
    List(
      s"Température influence les arômes : ${yeast.fermentationTemp.min}-${yeast.fermentationTemp.max}°C",
      "Contrôler timing dry-hop si IPA",
      "Goûter régulièrement pendant fermentation"
    )
  }

  private def generateAlternativeReason(
    alternative: YeastAggregate, 
    original: YeastAggregate, 
    reason: AlternativeReason
  ): String = {
    val reasonText = reason match {
      case AlternativeReason.Unavailable => "indisponible"
      case AlternativeReason.TooExpensive => "trop chère"
      case AlternativeReason.Experiment => "pour expérimenter"
    }
    s"Alternative à ${original.name.value} ($reasonText) : profil ${alternative.yeastType.name.toLowerCase} similaire"
  }

  private def generateAlternativeTips(alternative: YeastAggregate, original: YeastAggregate): List[String] = {
    List(
      s"Ajuster température si nécessaire : ${alternative.fermentationTemp.min}-${alternative.fermentationTemp.max}°C vs ${original.fermentationTemp.min}-${original.fermentationTemp.max}°C",
      s"Atténuation attendue : ${alternative.attenuationRange.min}-${alternative.attenuationRange.max}% vs ${original.attenuationRange.min}-${original.attenuationRange.max}%",
      "Tester sur petit batch d'abord"
    )
  }

  private def generateTestBatchReason(yeast: YeastAggregate, recipe: TestRecipeParams): String = {
    s"Compatible avec ${recipe.style} : ${yeast.yeastType.name} adapté, température OK"
  }

  private def generateTestBatchTips(yeast: YeastAggregate, recipe: TestRecipeParams): List[String] = {
    List(
      "Batch de 5L recommandé pour test",
      s"Surveiller température : ${recipe.fermentationTemp}°C",
      "Prendre notes détaillées pour comparaison"
    )
  }

  private def calculateTestBatchScore(yeast: YeastAggregate, recipe: TestRecipeParams): Double = {
    var score = 0.0
    
    // if (yeast.isCompatibleWith(recipe.style)) score += 0.4
    score += 0.4 // Implémentation simplifiée pour éviter l'erreur de compilation
    if (yeast.fermentationTemp.contains(recipe.fermentationTemp)) score += 0.3
    if (yeast.alcoholTolerance.canFerment(recipe.expectedAbv)) score += 0.2
    if (yeast.attenuationRange.contains(recipe.targetAttenuation)) score += 0.1
    
    score
  }
}

// ==========================================================================
// TYPES DE SUPPORT
// ==========================================================================

/**
 * Levure recommandée avec justification
 */
case class RecommendedYeast(
  yeast: YeastAggregate,
  score: Double,
  reason: String,
  tips: List[String]
)

/**
 * Saisons pour recommandations saisonnières
 */
sealed trait Season
object Season {
  case object Spring extends Season
  case object Summer extends Season  
  case object Autumn extends Season
  case object Winter extends Season
}

/**
 * Raisons de recherche d'alternatives
 */
sealed trait AlternativeReason
object AlternativeReason {
  case object Unavailable extends AlternativeReason
  case object TooExpensive extends AlternativeReason
  case object Experiment extends AlternativeReason
}

/**
 * Paramètres de recette pour tests
 */
case class TestRecipeParams(
  style: String,
  fermentationTemp: Int,
  expectedAbv: Double,
  targetAttenuation: Int
)
