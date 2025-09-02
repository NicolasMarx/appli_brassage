package domain.hops.services

import domain.hops.model._
import domain.hops.repositories.HopReadRepository
import domain.recipes.model.{BeerStyle, Season}
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

/**
 * Service de recommandations intelligentes pour les houblons
 * Réplique exactement les fonctionnalités de YeastRecommendationService
 */
@Singleton
class HopRecommendationService @Inject()(
  hopRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  // ============================================================================
  // RECOMMANDATIONS POUR DÉBUTANTS
  // ============================================================================
  
  def getBeginnerFriendlyHops(maxResults: Int = 5): Future[List[RecommendedHop]] = {
    hopRepository.findAll().map { hops =>
      hops
        .map(hop => (hop, calculateBeginnerScore(hop)))
        .filter(_._2 >= 0.3) // Seuil minimum de pertinence
        .sortBy(-_._2) // Tri par score décroissant
        .take(maxResults)
        .map { case (hop, score) =>
          RecommendedHop(
            hop = hop,
            score = score,
            reason = generateBeginnerReason(hop),
            tips = generateBeginnerTips(hop)
          )
        }
    }
  }

  private def calculateBeginnerScore(hop: HopAggregate): Double = {
    var score = 0.0
    
    // Score basé sur l'amertume modérée (alpha acids 4-8%)
    val alpha = hop.alphaAcid.value
    if (alpha >= 4.0 && alpha <= 8.0) score += 0.4
    else if (alpha < 4.0 || alpha > 12.0) score += 0.1
    else score += 0.2
    
    // Bonus pour les houblons polyvalents (dual-purpose)
    if (hop.usage == HopUsage.DualPurpose) score += 0.3
    
    // Bonus pour les profils aromatiques équilibrés
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    if (profiles.exists(_.contains("citrus")) || profiles.exists(_.contains("floral"))) {
      score += 0.2
    }
    
    // Bonus pour les origines connues et fiables
    if (hop.origin.name == "United States" || hop.origin.name == "Germany" || 
        hop.origin.name == "Czech Republic") {
      score += 0.1
    }
    
    math.min(1.0, score)
  }

  private def generateBeginnerReason(hop: HopAggregate): String = {
    val reasons = scala.collection.mutable.ListBuffer[String]()
    
    val alpha = hop.alphaAcid.value
    if (alpha >= 4.0 && alpha <= 8.0) {
      reasons += f"Alpha acids modérés ($alpha%.1f%%)"
    }
    
    if (hop.usage == HopUsage.DualPurpose) {
      reasons += "Polyvalent (amertume et arôme)"
    }
    
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    if (profiles.nonEmpty) {
      reasons += s"Profil aromatique accessible: ${profiles.take(2).mkString(", ")}"
    }
    
    if (reasons.isEmpty) "Choix équilibré pour débuter"
    else reasons.mkString(", ")
  }

  private def generateBeginnerTips(hop: HopAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    val alpha = hop.alphaAcid.value
    if (alpha >= 4.0 && alpha <= 8.0) {
      tips += s"Utilisez 20-30g pour 20L en amertume (60min)"
      tips += s"Ajoutez 10-15g en fin d'ébullition pour l'arôme"
    }
    
    if (hop.usage == HopUsage.DualPurpose) {
      tips += "Parfait pour une seule variété dans une recette simple"
    }
    
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    if (profiles.exists(_.contains("citrus"))) {
      tips += "Excellent pour les IPA et Pale Ales"
    }
    if (profiles.exists(_.contains("floral"))) {
      tips += "Idéal pour les Lagers et Pilsners"
    }
    
    if (tips.isEmpty) tips += "Commencez avec de petites quantités et ajustez selon vos goûts"
    tips.toList
  }

  // ============================================================================
  // RECOMMANDATIONS SAISONNIÈRES
  // ============================================================================
  
  def getSeasonalRecommendations(season: Season, maxResults: Int = 8): Future[List[RecommendedHop]] = {
    hopRepository.findAll().map { hops =>
      hops
        .map(hop => (hop, calculateSeasonalScore(hop, season)))
        .filter(_._2 >= 0.3)
        .sortBy(-_._2)
        .take(maxResults)
        .map { case (hop, score) =>
          RecommendedHop(
            hop = hop,
            score = score,
            reason = generateSeasonalReason(hop, season),
            tips = generateSeasonalTips(hop, season)
          )
        }
    }
  }

  private def calculateSeasonalScore(hop: HopAggregate, season: Season): Double = {
    var score = 0.3 // Score de base
    
    season match {
      case Season.Spring =>
        // Houblons frais et floraux pour le printemps
        val profiles = hop.aromaProfile.map(_.toLowerCase)
        if (profiles.exists(p => p.contains("floral") || p.contains("herbal") || p.contains("spicy"))) {
          score += 0.4
        }
        
      case Season.Summer =>
        // Houblons citrus et tropicaux pour l'été
        val profiles = hop.aromaProfile.map(_.toLowerCase)
        if (profiles.exists(p => p.contains("citrus") || p.contains("tropical") || p.contains("fruity"))) {
          score += 0.5
        }
        
      case Season.Autumn =>
        // Houblons terreux et épicés pour l'automne
        val profiles = hop.aromaProfile.map(_.toLowerCase)
        if (profiles.exists(p => p.contains("earthy") || p.contains("woody") || p.contains("spicy"))) {
          score += 0.4
        }
        
      case Season.Winter =>
        // Houblons pineux et résineux pour l'hiver
        val profiles = hop.aromaProfile.map(_.toLowerCase)
        if (profiles.exists(p => p.contains("pine") || p.contains("resinous") || p.contains("woody"))) {
          score += 0.4
        }
    }
    
    math.min(1.0, score)
  }

  private def generateSeasonalReason(hop: HopAggregate, season: Season): String = {
    val seasonName = season match {
      case Season.Spring => "printemps"
      case Season.Summer => "été"
      case Season.Autumn => "automne"
      case Season.Winter => "hiver"
    }
    
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    val relevantProfiles = season match {
      case Season.Spring => profiles.filter(p => p.contains("floral") || p.contains("herbal"))
      case Season.Summer => profiles.filter(p => p.contains("citrus") || p.contains("tropical"))
      case Season.Autumn => profiles.filter(p => p.contains("earthy") || p.contains("woody"))
      case Season.Winter => profiles.filter(p => p.contains("pine") || p.contains("resinous"))
    }
    
    if (relevantProfiles.nonEmpty) {
      s"Parfait pour le ${seasonName}: ${relevantProfiles.take(2).mkString(", ")}"
    } else {
      s"Adapté aux brassages de ${seasonName}"
    }
  }

  private def generateSeasonalTips(hop: HopAggregate, season: Season): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    season match {
      case Season.Spring =>
        tips += "Utilisez en dry-hopping pour capturer les arômes délicats"
        tips += "Parfait pour les bières de printemps légères et rafraîchissantes"
        
      case Season.Summer =>
        tips += "Excellent en additions tardives (0-10 min)"
        tips += "Idéal pour les IPA et wheat beers estivales"
        
      case Season.Autumn =>
        tips += "Combine bien avec les malts caramélisés d'automne"
        tips += "Parfait pour les harvest ales et amber ales"
        
      case Season.Winter =>
        tips += "Excellent pour les IPA hivernales riches"
        tips += "Utilisez généreusement en amertume pour réchauffer"
    }
    
    tips.toList
  }

  // ============================================================================
  // RECOMMANDATIONS EXPÉRIMENTALES
  // ============================================================================
  
  def getExperimentalHops(maxResults: Int = 6): Future[List[RecommendedHop]] = {
    hopRepository.findAll().map { hops =>
      hops
        .map(hop => (hop, calculateExperimentalScore(hop)))
        .filter(_._2 >= 0.3)
        .sortBy(-_._2)
        .take(maxResults)
        .map { case (hop, score) =>
          RecommendedHop(
            hop = hop,
            score = score,
            reason = generateExperimentalReason(hop),
            tips = generateExperimentalTips(hop)
          )
        }
    }
  }

  private def calculateExperimentalScore(hop: HopAggregate): Double = {
    var score = 0.0
    
    // Score pour les houblons récents (développés après 2000)
    if (hop.name.value.contains("™") || hop.name.value.contains("®")) {
      score += 0.3 // Houblons propriétaires récents
    }
    
    // Score pour les profils aromatiques uniques
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    val uniqueProfiles = profiles.filter(p => 
      p.contains("tropical") || p.contains("exotic") || p.contains("unique") ||
      p.contains("dank") || p.contains("funky") || p.contains("wine")
    )
    score += uniqueProfiles.size * 0.2
    
    // Score pour les alpha acids très élevés (>15%)
    val alpha = hop.alphaAcid.value
    if (alpha > 15.0) score += 0.3
    if (alpha > 20.0) score += 0.2 // Bonus supplémentaire
    
    // Score pour les origines moins communes
    if (hop.origin.name != "United States" && hop.origin.name != "Germany" &&
        hop.origin.name != "Czech Republic" && hop.origin.name != "United Kingdom") {
      score += 0.2
    }
    
    math.min(1.0, score)
  }

  private def generateExperimentalReason(hop: HopAggregate): String = {
    val reasons = scala.collection.mutable.ListBuffer[String]()
    
    if (hop.name.value.contains("™") || hop.name.value.contains("®")) {
      reasons += "Variété propriétaire récente"
    }
    
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    val uniqueProfiles = profiles.filter(p => 
      p.contains("tropical") || p.contains("exotic") || p.contains("unique") ||
      p.contains("dank") || p.contains("funky")
    )
    if (uniqueProfiles.nonEmpty) {
      reasons += s"Profils aromatiques uniques: ${uniqueProfiles.take(2).mkString(", ")}"
    }
    
    val alpha = hop.alphaAcid.value
    if (alpha > 15.0) {
      reasons += f"Alpha acids très élevés ($alpha%.1f%%)"
    }
    
    if (reasons.isEmpty) "Choix aventureux pour brasseurs expérimentés"
    else reasons.mkString(", ")
  }

  private def generateExperimentalTips(hop: HopAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    tips += "Commencez par de petites quantités pour évaluer l'impact"
    tips += "Excellent pour des bières expérimentales mono-houblon"
    
    val alpha = hop.alphaAcid.value
    if (alpha > 15.0) {
      tips += f"Attention: alpha acids élevés ($alpha%.1f%%) - ajustez les quantités"
    }
    
    val profiles = hop.aromaProfile.map(_.toLowerCase)
    if (profiles.exists(_.contains("tropical"))) {
      tips += "Parfait pour des NEIPA expérimentales"
    }
    if (profiles.exists(_.contains("dank"))) {
      tips += "Idéal pour des West Coast IPA intenses"
    }
    
    tips += "Documentez vos recettes pour reproduire les résultats"
    tips.toList
  }

  // ============================================================================
  // RECOMMANDATIONS PAR PROFIL AROMATIQUE
  // ============================================================================
  
  def getByAromaProfile(desiredAromas: List[String], maxResults: Int = 10): Future[List[RecommendedHop]] = {
    hopRepository.findAll().map { hops =>
      hops
        .map(hop => (hop, calculateAromaMatchScore(hop, desiredAromas)))
        .filter(_._2 >= 0.3)
        .sortBy(-_._2)
        .take(maxResults)
        .map { case (hop, score) =>
          RecommendedHop(
            hop = hop,
            score = score,
            reason = generateAromaReason(hop, desiredAromas),
            tips = generateAromaTips(hop, desiredAromas)
          )
        }
    }
  }

  private def calculateAromaMatchScore(hop: HopAggregate, desiredAromas: List[String]): Double = {
    val hopProfiles = hop.aromaProfile.map(_.toLowerCase)
    val desiredLower = desiredAromas.map(_.toLowerCase)
    
    var matchScore = 0.0
    val totalDesired = desiredAromas.size.toDouble
    
    for (desired <- desiredLower) {
      val directMatch = hopProfiles.exists(_.contains(desired))
      val fuzzyMatch = hopProfiles.exists(profile => 
        calculateStringsSimilarity(profile, desired) > 0.6
      )
      
      if (directMatch) matchScore += 1.0
      else if (fuzzyMatch) matchScore += 0.5
    }
    
    matchScore / totalDesired
  }

  private def calculateStringsSimilarity(s1: String, s2: String): Double = {
    // Implémentation simple de similarité de chaînes
    val longer = if (s1.length > s2.length) s1 else s2
    val shorter = if (s1.length > s2.length) s2 else s1
    
    if (longer.length == 0) return 1.0
    
    val editDistance = calculateLevenshteinDistance(longer, shorter)
    (longer.length - editDistance) / longer.length.toDouble
  }

  private def calculateLevenshteinDistance(s1: String, s2: String): Int = {
    val len1 = s1.length
    val len2 = s2.length
    val matrix = Array.ofDim[Int](len1 + 1, len2 + 1)

    for (i <- 0 to len1) matrix(i)(0) = i
    for (j <- 0 to len2) matrix(0)(j) = j

    for (i <- 1 to len1; j <- 1 to len2) {
      val cost = if (s1(i - 1) == s2(j - 1)) 0 else 1
      matrix(i)(j) = math.min(
        math.min(matrix(i - 1)(j) + 1, matrix(i)(j - 1) + 1),
        matrix(i - 1)(j - 1) + cost
      )
    }

    matrix(len1)(len2)
  }

  private def generateAromaReason(hop: HopAggregate, desiredAromas: List[String]): String = {
    val hopProfiles = hop.aromaProfile
    val matches = hopProfiles.filter(profile => 
      desiredAromas.exists(desired => profile.toLowerCase.contains(desired.toLowerCase))
    )
    
    if (matches.nonEmpty) {
      s"Profils correspondants: ${matches.mkString(", ")}"
    } else {
      s"Profils compatibles avec ${desiredAromas.mkString(", ")}"
    }
  }

  private def generateAromaTips(hop: HopAggregate, desiredAromas: List[String]): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    if (desiredAromas.exists(_.toLowerCase.contains("citrus"))) {
      tips += "Utilisez en fin d'ébullition (0-5 min) pour préserver les notes citriques"
      tips += "Excellent en dry-hopping à froid"
    }
    
    if (desiredAromas.exists(_.toLowerCase.contains("floral"))) {
      tips += "Ajoutez en milieu d'ébullition (15-20 min) pour les notes florales"
    }
    
    if (desiredAromas.exists(_.toLowerCase.contains("tropical"))) {
      tips += "Maximisez avec du dry-hopping intensif"
      tips += "Parfait pour les NEIPA et IPA tropicales"
    }
    
    if (desiredAromas.exists(_.toLowerCase.contains("pine"))) {
      tips += "Utilisez généreusement en amertume pour les notes résineuses"
    }
    
    if (tips.isEmpty) {
      tips += "Ajustez les additions selon l'intensité aromatique désirée"
    }
    
    tips.toList
  }

  // ============================================================================
  // ALTERNATIVES INTELLIGENTES
  // ============================================================================
  
  def findAlternatives(
    originalHopId: HopId, 
    reason: AlternativeReason, 
    maxResults: Int = 5
  ): Future[List[RecommendedHop]] = {
    for {
      originalHop <- hopRepository.findById(originalHopId)
      allHops <- hopRepository.findAll()
    } yield {
      originalHop match {
        case Some(original) =>
          allHops
            .filter(_.id != originalHopId) // Exclure l'original
            .map(hop => (hop, calculateSimilarityScore(original, hop, reason)))
            .filter(_._2 >= 0.3)
            .sortBy(-_._2)
            .take(maxResults)
            .map { case (hop, score) =>
              RecommendedHop(
                hop = hop,
                score = score,
                reason = generateAlternativeReason(original, hop, reason),
                tips = generateAlternativeTips(original, hop, reason)
              )
            }
        case None => List.empty
      }
    }
  }

  private def calculateSimilarityScore(
    original: HopAggregate, 
    alternative: HopAggregate,
    reason: AlternativeReason
  ): Double = {
    var score = 0.0
    
    // Score basé sur les alpha acids similaires
    val origAlpha = original.alphaAcid.value
    val altAlpha = alternative.alphaAcid.value
    val diff = math.abs(origAlpha - altAlpha)
    if (diff <= 1.0) score += 0.4
    else if (diff <= 2.0) score += 0.2
    else if (diff <= 3.0) score += 0.1
    
    // Score basé sur les profils aromatiques
    val origProfiles = original.aromaProfile.map(_.toLowerCase).toSet
    val altProfiles = alternative.aromaProfile.map(_.toLowerCase).toSet
    val commonProfiles = origProfiles.intersect(altProfiles)
    val unionProfiles = origProfiles.union(altProfiles)
    
    if (unionProfiles.nonEmpty) {
      score += (commonProfiles.size.toDouble / unionProfiles.size) * 0.3
    }
    
    // Score basé sur l'usage (bittering, aroma, dual-purpose)
    if (original.usage == alternative.usage) score += 0.2
    
    // Ajustements basés sur la raison de l'alternative
    reason match {
      case AlternativeReason.Unavailable =>
        // Prioriser la disponibilité (simulation)
        score += 0.1
        
      case AlternativeReason.TooExpensive =>
        // Prioriser les variétés plus communes (simulation)
        if (alternative.origin.name == "United States" || 
            alternative.origin.name == "Germany") {
          score += 0.1
        }
        
      case AlternativeReason.Experiment =>
        // Prioriser les profils légèrement différents pour l'expérimentation
        if (commonProfiles.size > 0 && commonProfiles.size < origProfiles.size) {
          score += 0.1
        }
    }
    
    math.min(1.0, score)
  }

  private def generateAlternativeReason(
    original: HopAggregate,
    alternative: HopAggregate, 
    reason: AlternativeReason
  ): String = {
    val reasons = scala.collection.mutable.ListBuffer[String]()
    
    // Similarités
    val origAlpha = original.alphaAcid.value
    val altAlpha = alternative.alphaAcid.value
    val diff = math.abs(origAlpha - altAlpha)
    if (diff <= 1.0) {
      reasons += f"Alpha acids similaires ($origAlpha%.1f%% → $altAlpha%.1f%%)"
    }
    
    val origProfiles = original.aromaProfile.map(_.toLowerCase).toSet
    val altProfiles = alternative.aromaProfile.map(_.toLowerCase).toSet
    val commonProfiles = origProfiles.intersect(altProfiles)
    
    if (commonProfiles.nonEmpty) {
      reasons += s"Profils aromatiques partagés: ${commonProfiles.toList.sorted.take(2).mkString(", ")}"
    }
    
    // Raison spécifique
    val reasonText = reason match {
      case AlternativeReason.Unavailable => "Alternative disponible"
      case AlternativeReason.TooExpensive => "Option plus économique"
      case AlternativeReason.Experiment => "Variante expérimentale"
    }
    
    if (reasons.nonEmpty) {
      s"${reasonText}: ${reasons.mkString(", ")}"
    } else {
      reasonText
    }
  }

  private def generateAlternativeTips(
    original: HopAggregate,
    alternative: HopAggregate,
    reason: AlternativeReason
  ): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    // Conseils d'ajustement basés sur les différences d'alpha acids
    val origAlpha = original.alphaAcid.value
    val altAlpha = alternative.alphaAcid.value
    val ratio = altAlpha / origAlpha
    if (ratio > 1.1) {
      tips += f"Réduisez la quantité de ${ratio * 100 - 100}%.0f%% (alpha acids plus élevés)"
    } else if (ratio < 0.9) {
      tips += f"Augmentez la quantité de ${100 - ratio * 100}%.0f%% (alpha acids plus faibles)"
    }
    
    // Conseils spécifiques à la raison
    reason match {
      case AlternativeReason.Unavailable =>
        tips += "Ajustez progressivement selon les résultats"
        
      case AlternativeReason.TooExpensive =>
        tips += "Alternative économique avec profil similaire"
        
      case AlternativeReason.Experiment =>
        tips += "Parfait pour explorer de nouvelles saveurs"
        tips += "Comparez côte à côte avec l'original"
    }
    
    if (tips.isEmpty) {
      tips += "Remplaçement direct possible avec ajustements mineurs"
    }
    
    tips.toList
  }
}