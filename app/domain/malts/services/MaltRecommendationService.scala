package domain.malts.services

import domain.malts.model._
import domain.malts.repositories.MaltReadRepository
import domain.common.PaginatedResult
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service de recommandations intelligentes pour les malts
 * Algorithmes de suggestion basés sur les caractéristiques de brassage
 * Réplique exactement YeastRecommendationService pour les malts
 */
@Singleton
class MaltRecommendationService @Inject()(
  maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Recommandations pour débutants
   */
  def getBeginnerFriendlyMalts(maxResults: Int = 5): Future[List[RecommendedMalt]] = {
    val beginnerCriteria = MaltFilter(
      isActive = Some(true)
    ).copy(size = 50)
    
    maltReadRepository.findByFilter(beginnerCriteria).map { result =>
      result.items
        .filter(isBeginnerFriendly)
        .map(malt => RecommendedMalt(
          malt = malt,
          score = calculateBeginnerScore(malt),
          reason = generateBeginnerReason(malt),
          tips = generateBeginnerTips(malt)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations par saison
   */
  def getSeasonalRecommendations(season: Season, maxResults: Int = 8): Future[List[RecommendedMalt]] = {
    val seasonalTypes = getSeasonalMaltTypes(season)
    
    Future.sequence(seasonalTypes.map { maltType =>
      val filter = MaltFilter(
        maltType = Some(maltType),
        isActive = Some(true)
      ).copy(size = 20)
      
      maltReadRepository.findByFilter(filter)
    }).map { results =>
      results.flatMap(_.items)
        .map(malt => RecommendedMalt(
          malt = malt,
          score = calculateSeasonalScore(malt, season),
          reason = generateSeasonalReason(malt, season),
          tips = generateSeasonalTips(malt, season)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations pour expérimentateurs
   */
  def getExperimentalMalts(maxResults: Int = 6): Future[List[RecommendedMalt]] = {
    val experimentalCriteria = MaltFilter(
      isActive = Some(true)
    ).copy(size = 50)
    
    maltReadRepository.findByFilter(experimentalCriteria).map { result =>
      result.items
        .filter(isExperimental)
        .map(malt => RecommendedMalt(
          malt = malt,
          score = calculateExperimentalScore(malt),
          reason = generateExperimentalReason(malt),
          tips = generateExperimentalTips(malt)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations par profil aromatique/gustatif désiré
   */
  def getByFlavorProfile(
    desiredFlavors: List[String], 
    maxResults: Int = 10
  ): Future[List[RecommendedMalt]] = {
    val filter = MaltFilter(
      flavorProfiles = desiredFlavors,
      isActive = Some(true)
    ).copy(size = 100)
    
    maltReadRepository.findByFilter(filter).map { result =>
      result.items
        .map(malt => RecommendedMalt(
          malt = malt,
          score = calculateFlavorMatchScore(malt, desiredFlavors),
          reason = generateFlavorReason(malt, desiredFlavors),
          tips = generateFlavorTips(malt)
        ))
        .filter(_.score > 0.3) // Seuil minimum de pertinence
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Alternatives à un malt donné
   */
  def findAlternatives(
    originalMaltId: MaltId, 
    reason: AlternativeReason = AlternativeReason.Unavailable,
    maxResults: Int = 5
  ): Future[List[RecommendedMalt]] = {
    
    maltReadRepository.findById(originalMaltId).flatMap {
      case Some(original) =>
        val similarityFilter = MaltFilter(
          maltType = Some(original.maltType),
          isActive = Some(true)
        ).copy(size = 50)
        
        maltReadRepository.findByFilter(similarityFilter).map { result =>
          result.items
            .filter(_.id != originalMaltId)
            .map(malt => RecommendedMalt(
              malt = malt,
              score = calculateSimilarityScore(original, malt),
              reason = generateAlternativeReason(malt, original, reason),
              tips = generateAlternativeTips(malt, original)
            ))
            .sortBy(-_.score)
            .take(maxResults)
        }
        
      case None => Future.successful(List.empty)
    }
  }

  /**
   * Recommandations pour style de bière spécifique
   */
  def getForBeerStyle(
    beerStyle: String,
    maxResults: Int = 8
  ): Future[List[RecommendedMalt]] = {
    
    val styleCompatibleTypes = getMaltTypesForBeerStyle(beerStyle)
    
    Future.sequence(styleCompatibleTypes.map { maltType =>
      val filter = MaltFilter(
        maltType = Some(maltType),
        isActive = Some(true)
      ).copy(size = 20)
      
      maltReadRepository.findByFilter(filter)
    }).map { results =>
      results.flatMap(_.items)
        .map(malt => RecommendedMalt(
          malt = malt,
          score = calculateBeerStyleScore(malt, beerStyle),
          reason = generateBeerStyleReason(malt, beerStyle),
          tips = generateBeerStyleTips(malt, beerStyle)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - SCORING ET CLASSIFICATION
  // ==========================================================================

  private def isBeginnerFriendly(malt: MaltAggregate): Boolean = {
    // Malts de base sont plus faciles pour débuter
    val isBaseMalt = malt.maltType == MaltType.BASE
    
    // Malts avec extraction élevée sont plus prévisibles
    val hasGoodExtraction = malt.extractionRate.value >= 75.0
    
    // Score de crédibilité élevé = plus fiable pour débuter
    val isReliable = malt.credibilityScore >= 0.8
    
    isBaseMalt || (hasGoodExtraction && isReliable)
  }

  private def calculateBeginnerScore(malt: MaltAggregate): Double = {
    var score = 0.0
    
    // Bonus pour les malts de base (plus simples)
    if (malt.maltType == MaltType.BASE) score += 0.4
    else if (malt.maltType == MaltType.WHEAT) score += 0.3 // Blé aussi accessible
    
    // Bonus extraction élevée (plus prévisible)
    if (malt.extractionRate.value >= 80.0) score += 0.25
    else if (malt.extractionRate.value >= 75.0) score += 0.15
    
    // Bonus crédibilité élevée (plus fiable)
    if (malt.credibilityScore >= 0.9) score += 0.2
    else if (malt.credibilityScore >= 0.8) score += 0.1
    
    // Bonus couleur modérée (plus facile à utiliser)
    if (malt.ebcColor.value <= 20) score += 0.15 // Malts clairs plus polyvalents
    
    score
  }

  private def isExperimental(malt: MaltAggregate): Boolean = {
    // Malts spéciaux et torréfiés sont plus expérimentaux
    val isSpecialtyMalt = malt.maltType match {
      case MaltType.ROASTED | MaltType.SPECIALTY => true
      case MaltType.RYE | MaltType.OATS => true // Céréales alternatives
      case _ => false
    }
    
    // Malts avec profils de saveur uniques
    val hasUniqueFlavors = malt.flavorProfiles.exists(f => 
      f.toLowerCase.matches(".*(smoky|chocolate|coffee|roasted|caramel|toffee).*")
    )
    
    // Couleur très sombre = plus expérimental
    val isDarkMalt = malt.ebcColor.value > 100
    
    isSpecialtyMalt || hasUniqueFlavors || isDarkMalt
  }

  private def calculateExperimentalScore(malt: MaltAggregate): Double = {
    var score = 0.0
    
    // Score selon le type
    malt.maltType match {
      case MaltType.ROASTED => score += 0.4
      case MaltType.SPECIALTY => score += 0.35
      case MaltType.RYE => score += 0.3
      case MaltType.OATS => score += 0.25
      case _ => score += 0.0
    }
    
    // Bonus profils de saveur uniques
    val uniqueFlavors = List("smoky", "chocolate", "coffee", "roasted", "complex", "unique")
    val matchingFlavors = malt.flavorProfiles
      .count(f => uniqueFlavors.exists(u => f.toLowerCase.contains(u)))
    score += matchingFlavors * 0.1
    
    // Bonus couleur intense
    if (malt.ebcColor.value > 200) score += 0.2
    else if (malt.ebcColor.value > 100) score += 0.1
    
    score
  }

  private def getSeasonalMaltTypes(season: Season): List[MaltType] = {
    season match {
      case Season.Spring => List(MaltType.BASE, MaltType.WHEAT, MaltType.BASE)
      case Season.Summer => List(MaltType.BASE, MaltType.WHEAT, MaltType.CRYSTAL)
      case Season.Autumn => List(MaltType.CRYSTAL, MaltType.SPECIALTY, MaltType.BASE)
      case Season.Winter => List(MaltType.ROASTED, MaltType.CRYSTAL, MaltType.SPECIALTY)
    }
  }

  private def calculateSeasonalScore(malt: MaltAggregate, season: Season): Double = {
    val baseScore = if (getSeasonalMaltTypes(season).contains(malt.maltType)) 0.5 else 0.2
    
    val seasonalBonus = season match {
      case Season.Summer if malt.flavorProfiles.exists(_.toLowerCase.contains("light")) => 0.2
      case Season.Winter if malt.flavorProfiles.exists(_.toLowerCase.contains("rich")) => 0.2
      case Season.Autumn if malt.maltType == MaltType.CRYSTAL => 0.15
      case Season.Spring if malt.maltType == MaltType.WHEAT => 0.15
      case _ => 0.0
    }
    
    baseScore + seasonalBonus
  }

  private def calculateFlavorMatchScore(malt: MaltAggregate, desiredFlavors: List[String]): Double = {
    val maltFlavors = malt.flavorProfiles.map(_.toLowerCase)
    val matchingFlavors = desiredFlavors.count(desired => 
      maltFlavors.exists(_.contains(desired.toLowerCase))
    )
    
    matchingFlavors.toDouble / desiredFlavors.length
  }

  private def calculateSimilarityScore(original: MaltAggregate, alternative: MaltAggregate): Double = {
    var score = 0.0
    
    if (original.maltType == alternative.maltType) score += 0.3
    
    // Comparaison couleur EBC
    val colorDiff = math.abs(original.ebcColor.value - alternative.ebcColor.value)
    if (colorDiff <= 5) score += 0.25
    else if (colorDiff <= 15) score += 0.15
    else if (colorDiff <= 30) score += 0.05
    
    // Comparaison taux d'extraction
    val extractDiff = math.abs(original.extractionRate.value - alternative.extractionRate.value)
    if (extractDiff <= 2.0) score += 0.2
    else if (extractDiff <= 5.0) score += 0.1
    
    // Comparaison pouvoir diastasique
    val diastaticDiff = math.abs(original.diastaticPower.value - alternative.diastaticPower.value)
    if (diastaticDiff <= 10) score += 0.15
    else if (diastaticDiff <= 30) score += 0.05
    
    // Profils de saveur similaires
    val commonFlavors = original.flavorProfiles.intersect(alternative.flavorProfiles)
    if (commonFlavors.nonEmpty) {
      score += (commonFlavors.size.toDouble / math.max(original.flavorProfiles.size, alternative.flavorProfiles.size)) * 0.1
    }
    
    score
  }

  private def getMaltTypesForBeerStyle(beerStyle: String): List[MaltType] = {
    beerStyle.toLowerCase match {
      case s if s.contains("ipa") || s.contains("pale ale") => List(MaltType.BASE, MaltType.CRYSTAL)
      case s if s.contains("lager") || s.contains("pilsner") => List(MaltType.BASE)
      case s if s.contains("wheat") => List(MaltType.WHEAT, MaltType.BASE)
      case s if s.contains("stout") || s.contains("porter") => List(MaltType.ROASTED, MaltType.BASE, MaltType.CRYSTAL)
      case s if s.contains("brown") => List(MaltType.CRYSTAL, MaltType.BASE, MaltType.SPECIALTY)
      case s if s.contains("amber") => List(MaltType.CRYSTAL, MaltType.BASE)
      case _ => List(MaltType.BASE, MaltType.CRYSTAL)
    }
  }

  private def calculateBeerStyleScore(malt: MaltAggregate, beerStyle: String): Double = {
    val compatibleTypes = getMaltTypesForBeerStyle(beerStyle)
    val baseScore = if (compatibleTypes.contains(malt.maltType)) 0.5 else 0.2
    
    val styleBonus = beerStyle.toLowerCase match {
      case s if s.contains("stout") && malt.ebcColor.value > 200 => 0.3
      case s if s.contains("pilsner") && malt.ebcColor.value < 10 => 0.3
      case s if s.contains("wheat") && malt.maltType == MaltType.WHEAT => 0.3
      case s if s.contains("ipa") && malt.maltType == MaltType.BASE => 0.2
      case _ => 0.0
    }
    
    baseScore + styleBonus
  }

  // ==========================================================================
  // GÉNÉRATION DE TEXTES D'AIDE
  // ==========================================================================

  private def generateBeginnerReason(malt: MaltAggregate): String = {
    s"${malt.name.value} est idéal pour débuter : ${malt.maltType.category.toLowerCase}, extraction ${malt.extractionRate.value}%"
  }

  private def generateBeginnerTips(malt: MaltAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    if (malt.maltType == MaltType.BASE) {
      tips += "Peut représenter 80-100% du grain bill"
      tips += s"Extraction attendue : ${malt.extractionRate.value}%"
    }
    
    if (malt.ebcColor.value <= 20) {
      tips += "Couleur claire et polyvalente"
    }
    
    if (malt.diastaticPower.canConvertAdjuncts) {
      tips += "Possède assez d'enzymes pour convertir des adjuvants"
    }
    
    tips.toList
  }

  private def generateSeasonalReason(malt: MaltAggregate, season: Season): String = {
    val seasonName = season.toString.toLowerCase
    s"Parfait pour un brassage de $seasonName avec son profil ${malt.maltType.category.toLowerCase}"
  }

  private def generateSeasonalTips(malt: MaltAggregate, season: Season): List[String] = {
    val baseTips = List(s"Couleur : ${malt.ebcColor.value} EBC")
    val seasonalTips = season match {
      case Season.Summer => List("Parfait pour bières rafraîchissantes", "Utilisez avec houblons citriques")
      case Season.Winter => List("Idéal pour bières riches et chaleureuses", "Combine bien avec épices")
      case Season.Autumn => List("Excellent pour bières ambrées", "Profils caramélisés appréciés")
      case Season.Spring => List("Parfait pour bières légères", "Renouveau des saveurs fraîches")
    }
    baseTips ++ seasonalTips
  }

  private def generateExperimentalReason(malt: MaltAggregate): String = {
    s"${malt.name.value} offre un profil unique pour l'expérimentation avec ${malt.flavorProfiles.take(3).mkString(", ")}"
  }

  private def generateExperimentalTips(malt: MaltAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    tips += "Commencez par de petites proportions (5-15%)"
    
    if (malt.maltType == MaltType.ROASTED) {
      tips += "Peut dominer rapidement - goûtez en cours de brassage"
    }
    
    if (malt.ebcColor.value > 100) {
      tips += "Couleur intense - ajustez selon l'effet désiré"
    }
    
    tips += s"Profils de saveur : ${malt.flavorProfiles.mkString(", ")}"
    tips.toList
  }

  private def generateFlavorReason(malt: MaltAggregate, desiredFlavors: List[String]): String = {
    val matchingFlavors = malt.flavorProfiles
      .filter(f => desiredFlavors.exists(d => f.toLowerCase.contains(d.toLowerCase)))
    s"Produit les saveurs recherchées : ${matchingFlavors.mkString(", ")}"
  }

  private def generateFlavorTips(malt: MaltAggregate): List[String] = {
    List(
      s"Proportion recommandée : ${malt.maxRecommendedPercent.map(_ + "%").getOrElse("variable")}",
      "Température influence l'extraction des saveurs",
      s"Couleur finale : ${malt.ebcColor.value} EBC"
    )
  }

  private def generateAlternativeReason(
    alternative: MaltAggregate, 
    original: MaltAggregate, 
    reason: AlternativeReason
  ): String = {
    val reasonText = reason match {
      case AlternativeReason.Unavailable => "indisponible"
      case AlternativeReason.TooExpensive => "trop cher"
      case AlternativeReason.Experiment => "pour expérimenter"
    }
    s"Alternative à ${original.name.value} ($reasonText) : profil ${alternative.maltType.category.toLowerCase} similaire"
  }

  private def generateAlternativeTips(alternative: MaltAggregate, original: MaltAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    val colorDiff = alternative.ebcColor.value - original.ebcColor.value
    if (math.abs(colorDiff) > 10) {
      if (colorDiff > 0) {
        tips += s"Plus foncé de ${colorDiff.toInt} EBC - ajustez la couleur finale"
      } else {
        tips += s"Plus clair de ${math.abs(colorDiff).toInt} EBC - ajustez la couleur finale"
      }
    }
    
    val extractDiff = alternative.extractionRate.value - original.extractionRate.value
    if (math.abs(extractDiff) > 2) {
      if (extractDiff > 0) {
        tips += s"Extraction plus élevée (+${extractDiff.formatted("%.1f")}%) - ajustez les quantités"
      } else {
        tips += s"Extraction plus faible (${extractDiff.formatted("%.1f")}%) - augmentez légèrement"
      }
    }
    
    tips += "Testez sur petit batch d'abord"
    tips.toList
  }

  private def generateBeerStyleReason(malt: MaltAggregate, beerStyle: String): String = {
    s"Compatible avec ${beerStyle} : ${malt.maltType.category} adapté, couleur appropriée"
  }

  private def generateBeerStyleTips(malt: MaltAggregate, beerStyle: String): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    beerStyle.toLowerCase match {
      case s if s.contains("ipa") =>
        tips += "Base neutre pour mettre en valeur les houblons"
        tips += "Proportion : 80-90% du grain bill"
        
      case s if s.contains("stout") =>
        tips += "Utiliser en petites quantités pour couleur/saveur"
        tips += "5-15% selon l'intensité désirée"
        
      case s if s.contains("wheat") =>
        tips += "30-60% de blé recommandé"
        tips += "Améliore mousse et corps"
        
      case _ =>
        tips += s"Proportion : ${malt.maxRecommendedPercent.map(_ + "%").getOrElse("selon style")}"
    }
    
    tips.toList
  }
}

// ==========================================================================
// TYPES DE SUPPORT
// ==========================================================================

/**
 * Malt recommandé avec justification
 */
case class RecommendedMalt(
  malt: MaltAggregate,
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
  
  def fromString(season: String): Either[String, Season] = {
    season.toLowerCase match {
      case "spring" | "printemps" => Right(Spring)
      case "summer" | "été" => Right(Summer)
      case "autumn" | "automne" => Right(Autumn)
      case "winter" | "hiver" => Right(Winter)
      case _ => Left(s"Saison invalide: $season")
    }
  }
}

/**
 * Raisons de recherche d'alternatives
 */
sealed trait AlternativeReason {
  def value: String
}
object AlternativeReason {
  case object Unavailable extends AlternativeReason { val value = "unavailable" }
  case object TooExpensive extends AlternativeReason { val value = "too_expensive" }
  case object Experiment extends AlternativeReason { val value = "experiment" }
  
  def fromString(reason: String): Either[String, AlternativeReason] = {
    reason.toLowerCase match {
      case "unavailable" | "indisponible" => Right(Unavailable)
      case "too_expensive" | "expensive" | "cher" => Right(TooExpensive)
      case "experiment" | "experimental" => Right(Experiment)
      case _ => Left(s"Raison invalide: $reason")
    }
  }
}