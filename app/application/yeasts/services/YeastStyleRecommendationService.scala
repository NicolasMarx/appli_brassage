package application.yeasts.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.yeasts.model._
import domain.yeasts.repositories.YeastReadRepository
import domain.yeasts.services.{YeastRecommendationService, RecommendedYeast}
import play.api.libs.json._
import java.time.Instant

/**
 * Service pour les recommandations de levures par style de bière
 * Architecture DDD/CQRS respectée - Application Layer
 * 
 * Mapping intelligent styles → caractéristiques levures optimales
 * Algorithmes basés sur les meilleures pratiques du brassage
 */
@Singleton
class YeastStyleRecommendationService @Inject()(
  yeastReadRepository: YeastReadRepository,
  yeastRecommendationService: YeastRecommendationService
)(implicit ec: ExecutionContext) {

  /**
   * Recommandations de levures pour un style de bière spécifique
   */
  def getRecommendationsForStyle(
    style: BeerStyle,
    maxResults: Int = 8,
    includeAlternatives: Boolean = true
  ): Future[StyleRecommendationResult] = {
    
    for {
      // Récupérer les critères optimaux pour ce style
      styleCriteria <- getStyleCriteria(style)
      
      // Rechercher les levures compatibles
      compatibleYeasts <- findCompatibleYeasts(styleCriteria)
      
      // Scorer et trier les recommandations
      scoredRecommendations <- scoreYeastsForStyle(compatibleYeasts, styleCriteria)
      
      // Générer des alternatives si demandé
      alternatives <- if (includeAlternatives) {
        generateStyleAlternatives(style, scoredRecommendations.take(3))
      } else {
        Future.successful(List.empty)
      }
      
    } yield StyleRecommendationResult(
      style = style,
      primaryRecommendations = scoredRecommendations.take(maxResults),
      alternatives = alternatives,
      styleCriteria = styleCriteria,
      brewingNotes = generateBrewingNotes(style, scoredRecommendations.take(3)),
      confidenceLevel = calculateConfidenceLevel(scoredRecommendations),
      generatedAt = Instant.now()
    )
  }

  /**
   * Recommandations multi-styles pour comparaison
   */
  def getMultiStyleRecommendations(
    styles: List[BeerStyle],
    maxResultsPerStyle: Int = 5
  ): Future[MultiStyleRecommendationResult] = {
    
    Future.traverse(styles) { style =>
      getRecommendationsForStyle(style, maxResultsPerStyle, includeAlternatives = false)
        .map(style -> _)
    }.map { styleResults =>
      MultiStyleRecommendationResult(
        styleRecommendations = styleResults.toMap,
        crossStyleAnalysis = analyzeCrossStylePatterns(styleResults.toMap),
        versatileYeasts = findVersatileYeasts(styleResults.toMap),
        generatedAt = Instant.now()
      )
    }
  }

  /**
   * Recommandations pour styles innovants ou fusion
   */
  def getFusionStyleRecommendations(
    baseStyle: BeerStyle,
    fusionElement: FusionElement,
    experimentalLevel: ExperimentalLevel = ExperimentalLevel.Moderate
  ): Future[FusionRecommendationResult] = {
    
    for {
      baseRecommendations <- getRecommendationsForStyle(baseStyle, 5, includeAlternatives = false)
      fusionYeasts <- findFusionYeasts(baseStyle, fusionElement, experimentalLevel)
      hybridRecommendations <- generateHybridRecommendations(baseRecommendations, fusionYeasts)
      
    } yield FusionRecommendationResult(
      baseStyle = baseStyle,
      fusionElement = fusionElement,
      experimentalLevel = experimentalLevel,
      hybridRecommendations = hybridRecommendations,
      traditionalOptions = baseRecommendations.primaryRecommendations,
      innovationNotes = generateInnovationNotes(baseStyle, fusionElement),
      riskAssessment = assessFusionRisk(fusionElement, experimentalLevel),
      generatedAt = Instant.now()
    )
  }

  // ==========================================================================
  // MAPPING STYLES → CRITÈRES DE LEVURES
  // ==========================================================================

  private def getStyleCriteria(style: BeerStyle): Future[StyleCriteria] = {
    Future.successful {
      style match {
        // ALES CLASSIQUES
        case BeerStyle.AmericanIPA => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(18, 22),
          attenuationRange = AttenuationRange(75, 85),
          flocculation = List(FlocculationLevel.MEDIUM, FlocculationLevel.HIGH),
          characteristicKeywords = List("clean", "neutral", "citrus", "pine"),
          avoidKeywords = List("estery", "phenolic", "wild"),
          alcoholTolerance = AlcoholToleranceRange(6, 12),
          specialNotes = "Levure propre pour laisser s'exprimer les houblons américains"
        )
        
        case BeerStyle.EnglishPale => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(18, 22),
          attenuationRange = AttenuationRange(68, 75),
          flocculation = List(FlocculationLevel.MEDIUM, FlocculationLevel.HIGH),
          characteristicKeywords = List("fruity", "estery", "traditional"),
          avoidKeywords = List("phenolic", "wild", "brett"),
          alcoholTolerance = AlcoholToleranceRange(4, 6),
          specialNotes = "Esters fruités caractéristiques du style anglais traditionnel"
        )
        
        case BeerStyle.BelgianTripel => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(20, 26),
          attenuationRange = AttenuationRange(80, 90),
          flocculation = List(FlocculationLevel.LOW, FlocculationLevel.MEDIUM),
          characteristicKeywords = List("spicy", "phenolic", "banana", "clove"),
          avoidKeywords = List("clean", "neutral"),
          alcoholTolerance = AlcoholToleranceRange(8, 12),
          specialNotes = "Phénols et esters complexes typiques de la levure belge"
        )
        
        case BeerStyle.Weissbier => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(18, 24),
          attenuationRange = AttenuationRange(70, 78),
          flocculation = List(FlocculationLevel.LOW),
          characteristicKeywords = List("banana", "clove", "phenolic", "wheat"),
          avoidKeywords = List("clean", "hoppy"),
          alcoholTolerance = AlcoholToleranceRange(4, 6),
          specialNotes = "Levure de blé avec phénols clou de girofle et esters banane"
        )
        
        // LAGERS
        case BeerStyle.GermanLager => StyleCriteria(
          preferredTypes = List(YeastType.LAGER),
          temperatureRange = TemperatureRange(8, 12),
          attenuationRange = AttenuationRange(75, 82),
          flocculation = List(FlocculationLevel.MEDIUM, FlocculationLevel.HIGH),
          characteristicKeywords = List("clean", "crisp", "neutral"),
          avoidKeywords = List("estery", "phenolic", "fruity"),
          alcoholTolerance = AlcoholToleranceRange(4, 6),
          specialNotes = "Fermentation froide et propre, longue garde à froid"
        )
        
        case BeerStyle.CzechPilsner => StyleCriteria(
          preferredTypes = List(YeastType.LAGER),
          temperatureRange = TemperatureRange(8, 12),
          attenuationRange = AttenuationRange(70, 78),
          flocculation = List(FlocculationLevel.MEDIUM),
          characteristicKeywords = List("soft", "rounded", "traditional"),
          avoidKeywords = List("aggressive", "phenolic"),
          alcoholTolerance = AlcoholToleranceRange(4, 5),
          specialNotes = "Fermentation lente à froid avec souche tchèque traditionnelle"
        )
        
        // STYLES SPÉCIAUX
        case BeerStyle.Saison => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(22, 28),
          attenuationRange = AttenuationRange(85, 95),
          flocculation = List(FlocculationLevel.LOW, FlocculationLevel.MEDIUM),
          characteristicKeywords = List("spicy", "phenolic", "dry", "rustic"),
          avoidKeywords = List("sweet", "clean"),
          alcoholTolerance = AlcoholToleranceRange(6, 8),
          specialNotes = "Fermentation chaude avec atténuation très élevée"
        )
        
        case BeerStyle.Sour => StyleCriteria(
          preferredTypes = List(YeastType.WILD, YeastType.BRETT),
          temperatureRange = TemperatureRange(18, 24),
          attenuationRange = AttenuationRange(75, 90),
          flocculation = List(FlocculationLevel.LOW),
          characteristicKeywords = List("funky", "sour", "barnyard", "wild"),
          avoidKeywords = List("clean", "neutral"),
          alcoholTolerance = AlcoholToleranceRange(4, 8),
          specialNotes = "Fermentation mixte avec cultures sauvages, patience requise"
        )
        
        case BeerStyle.ImperialStout => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(18, 22),
          attenuationRange = AttenuationRange(70, 80),
          flocculation = List(FlocculationLevel.MEDIUM, FlocculationLevel.HIGH),
          characteristicKeywords = List("robust", "alcohol-tolerant", "clean"),
          avoidKeywords = List("light", "delicate"),
          alcoholTolerance = AlcoholToleranceRange(8, 15),
          specialNotes = "Tolérance alcool élevée pour styles impériaux"
        )
        
        // Style générique par défaut
        case _ => StyleCriteria(
          preferredTypes = List(YeastType.ALE),
          temperatureRange = TemperatureRange(18, 22),
          attenuationRange = AttenuationRange(72, 82),
          flocculation = List(FlocculationLevel.MEDIUM),
          characteristicKeywords = List("balanced"),
          avoidKeywords = List(),
          alcoholTolerance = AlcoholToleranceRange(4, 8),
          specialNotes = "Recommandations génériques pour style non spécialisé"
        )
      }
    }
  }

  private def findCompatibleYeasts(criteria: StyleCriteria): Future[List[YeastAggregate]] = {
    val filter = YeastFilter(
      yeastType = if (criteria.preferredTypes.length == 1) criteria.preferredTypes.headOption else None,
      status = List(YeastStatus.Active),
      size = 100
    )
    
    yeastReadRepository.findByFilter(filter).map { result =>
      result.items.filter { yeast =>
        // Filtrage par type si multiple
        criteria.preferredTypes.contains(yeast.yeastType) &&
        // Compatibilité température
        (yeast.fermentationTemp.min <= criteria.temperatureRange.max &&
         yeast.fermentationTemp.max >= criteria.temperatureRange.min) &&
        // Compatibilité atténuation
        (yeast.attenuationRange.min <= criteria.attenuationRange.max &&
         yeast.attenuationRange.max >= criteria.attenuationRange.min) &&
        // Compatibilité tolérance alcool
        yeast.alcoholTolerance.percentage >= criteria.alcoholTolerance.min &&
        yeast.alcoholTolerance.percentage <= criteria.alcoholTolerance.max
      }
    }
  }

  private def scoreYeastsForStyle(
    yeasts: List[YeastAggregate], 
    criteria: StyleCriteria
  ): Future[List[StyleRecommendedYeast]] = {
    
    Future.successful {
      yeasts.map { yeast =>
        val score = calculateStyleScore(yeast, criteria)
        StyleRecommendedYeast(
          yeast = yeast,
          styleScore = score,
          compatibilityReason = generateCompatibilityReason(yeast, criteria, score),
          brewingTips = generateStyleSpecificTips(yeast, criteria),
          confidenceLevel = calculateIndividualConfidence(score)
        )
      }.sortBy(-_.styleScore)
    }
  }

  private def calculateStyleScore(yeast: YeastAggregate, criteria: StyleCriteria): Double = {
    var score = 0.0
    
    // Score type de levure (30%)
    if (criteria.preferredTypes.contains(yeast.yeastType)) {
      score += 0.3
    }
    
    // Score température (20%)
    val tempOverlap = calculateRangeOverlap(
      yeast.fermentationTemp.min, yeast.fermentationTemp.max,
      criteria.temperatureRange.min, criteria.temperatureRange.max
    )
    score += tempOverlap * 0.2
    
    // Score atténuation (20%)
    val attOverlap = calculateRangeOverlap(
      yeast.attenuationRange.min, yeast.attenuationRange.max,
      criteria.attenuationRange.min, criteria.attenuationRange.max
    )
    score += attOverlap * 0.2
    
    // Score floculation (10%)
    if (criteria.flocculation.contains(yeast.flocculation)) {
      score += 0.1
    }
    
    // Score caractéristiques (15%)
    val characteristicsScore = calculateCharacteristicsScore(yeast, criteria)
    score += characteristicsScore * 0.15
    
    // Score tolérance alcool (5%)
    if (yeast.alcoholTolerance.percentage >= criteria.alcoholTolerance.min &&
        yeast.alcoholTolerance.percentage <= criteria.alcoholTolerance.max) {
      score += 0.05
    }
    
    Math.max(0.0, Math.min(1.0, score))
  }

  private def calculateCharacteristicsScore(yeast: YeastAggregate, criteria: StyleCriteria): Double = {
    val yeastChars = yeast.characteristics.allCharacteristics.map(_.toLowerCase)
    
    // Score positif pour mots-clés souhaités
    val positiveMatches = criteria.characteristicKeywords.count { keyword =>
      yeastChars.exists(_.contains(keyword.toLowerCase))
    }
    val positiveScore = if (criteria.characteristicKeywords.nonEmpty) {
      positiveMatches.toDouble / criteria.characteristicKeywords.length
    } else 0.5
    
    // Score négatif pour mots-clés à éviter
    val negativeMatches = criteria.avoidKeywords.count { keyword =>
      yeastChars.exists(_.contains(keyword.toLowerCase))
    }
    val negativeScore = if (criteria.avoidKeywords.nonEmpty) {
      1.0 - (negativeMatches.toDouble / criteria.avoidKeywords.length)
    } else 1.0
    
    // Score combiné
    (positiveScore + negativeScore) / 2.0
  }

  private def calculateRangeOverlap(min1: Int, max1: Int, min2: Int, max2: Int): Double = {
    val overlapStart = Math.max(min1, min2)
    val overlapEnd = Math.min(max1, max2)
    val overlap = Math.max(0, overlapEnd - overlapStart)
    val totalSpan = Math.max(max1 - min1, max2 - min2)
    
    if (totalSpan == 0) 1.0 else overlap.toDouble / totalSpan
  }

  private def generateStyleAlternatives(
    style: BeerStyle, 
    primaryRecommendations: List[StyleRecommendedYeast]
  ): Future[List[AlternativeRecommendation]] = {
    
    Future.successful {
      // Alternatives basées sur des styles similaires ou croisés
      style match {
        case BeerStyle.AmericanIPA => List(
          AlternativeRecommendation(
            category = "Style Croisé",
            description = "Pour IPA Belge",
            reason = "Ajouter complexité belge à l'IPA américaine"
          ),
          AlternativeRecommendation(
            category = "Fermentation Froide", 
            description = "Lager pour Cold IPA",
            reason = "Tendance moderne fermentation froide"
          )
        )
        
        case BeerStyle.BelgianTripel => List(
          AlternativeRecommendation(
            category = "Intensité Modérée",
            description = "Pour Dubbel",
            reason = "Version moins alcoolisée du style"
          )
        )
        
        case _ => List(
          AlternativeRecommendation(
            category = "Expérimentation",
            description = "Souches wild pour version brett",
            reason = "Exploration de profils funkys"
          )
        )
      }
    }
  }

  private def generateBrewingNotes(
    style: BeerStyle, 
    recommendations: List[StyleRecommendedYeast]
  ): BrewingNotes = {
    
    style match {
      case BeerStyle.AmericanIPA => BrewingNotes(
        fermentationAdvice = "Fermenter à 18-20°C pour profil propre, 20-22°C pour plus d'esters",
        temperatureTips = List(
          "Contrôler température pendant pic fermentation",
          "Éviter températures >24°C (fusel alcohols)"
        ),
        timingNotes = "Fermentation primaire 5-7 jours, dry-hop après pic",
        commonMistakes = List(
          "Température trop élevée masque les houblons",
          "Sous-atténuation = bière lourde"
        ),
        successIndicators = List(
          "Atténuation 75-85%", 
          "Profil propre sans off-flavors"
        )
      )
      
      case BeerStyle.BelgianTripel => BrewingNotes(
        fermentationAdvice = "Commencer à 20°C puis monter à 24-26°C pour développer esters/phénols",
        temperatureTips = List(
          "Rampe de température progressive",
          "Pic à 26°C pendant 48h puis refroidir"
        ),
        timingNotes = "Fermentation 10-14 jours, garde chaude 3-5 jours",
        commonMistakes = List(
          "Température constante = profil plat",
          "Arrêt fermentation prématuré"
        ),
        successIndicators = List(
          "Atténuation >85%",
          "Arômes banana/clove équilibrés"
        )
      )
      
      case _ => BrewingNotes(
        fermentationAdvice = "Suivre les recommandations de température de la levure",
        temperatureTips = List("Contrôler température", "Éviter fluctuations importantes"),
        timingNotes = "Fermentation standard selon le type de levure",
        commonMistakes = List("Négligence du contrôle température"),
        successIndicators = List("Atténuation dans la plage attendue")
      )
    }
  }

  private def generateCompatibilityReason(
    yeast: YeastAggregate, 
    criteria: StyleCriteria, 
    score: Double
  ): String = {
    
    val reasons = scala.collection.mutable.ListBuffer[String]()
    
    if (criteria.preferredTypes.contains(yeast.yeastType)) {
      reasons += s"Type ${yeast.yeastType.name} adapté au style"
    }
    
    if (yeast.fermentationTemp.min >= criteria.temperatureRange.min && 
        yeast.fermentationTemp.max <= criteria.temperatureRange.max) {
      reasons += "Température parfaitement compatible"
    }
    
    if (score >= 0.8) {
      reasons += "Excellent match pour ce style"
    } else if (score >= 0.6) {
      reasons += "Bon choix pour ce style"  
    } else {
      reasons += "Compatible avec adaptations"
    }
    
    reasons.mkString(", ")
  }

  private def generateStyleSpecificTips(yeast: YeastAggregate, criteria: StyleCriteria): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    // Tips température
    tips += s"Fermenter à ${criteria.temperatureRange.optimal}°C pour profil optimal"
    
    // Tips atténuation  
    tips += s"Atténuation attendue: ${yeast.attenuationRange.min}-${yeast.attenuationRange.max}%"
    
    // Tips spécifiques au style
    if (criteria.specialNotes.nonEmpty) {
      tips += criteria.specialNotes
    }
    
    tips.toList
  }

  private def calculateIndividualConfidence(score: Double): ConfidenceLevel = {
    if (score >= 0.8) ConfidenceLevel.High
    else if (score >= 0.6) ConfidenceLevel.Medium  
    else ConfidenceLevel.Low
  }

  private def calculateConfidenceLevel(recommendations: List[StyleRecommendedYeast]): ConfidenceLevel = {
    if (recommendations.isEmpty) return ConfidenceLevel.Low
    
    val avgScore = recommendations.map(_.styleScore).sum / recommendations.length
    if (avgScore >= 0.75) ConfidenceLevel.High
    else if (avgScore >= 0.5) ConfidenceLevel.Medium
    else ConfidenceLevel.Low
  }

  // Méthodes pour multi-style et fusion (implémentation simplifiée)
  
  private def analyzeCrossStylePatterns(styleResults: Map[BeerStyle, StyleRecommendationResult]): JsValue = {
    Json.obj(
      "commonYeasts" -> "Analysis à implémenter",
      "styleGroups" -> "Clustering à implémenter"
    )
  }
  
  private def findVersatileYeasts(styleResults: Map[BeerStyle, StyleRecommendationResult]): List[VersatileYeast] = {
    // Implémentation simplifiée
    List.empty
  }
  
  private def findFusionYeasts(
    baseStyle: BeerStyle, 
    fusionElement: FusionElement, 
    experimentalLevel: ExperimentalLevel
  ): Future[List[YeastAggregate]] = {
    // Implémentation simplifiée - retourner levures expérimentales
    val filter = YeastFilter(status = List(YeastStatus.Active), size = 20)
    yeastReadRepository.findByFilter(filter).map(_.items.take(3))
  }
  
  private def generateHybridRecommendations(
    baseRecommendations: StyleRecommendationResult,
    fusionYeasts: List[YeastAggregate]
  ): Future[List[HybridRecommendation]] = {
    // Implémentation simplifiée
    Future.successful(List.empty)
  }
  
  private def generateInnovationNotes(baseStyle: BeerStyle, fusionElement: FusionElement): String = {
    s"Innovation: combiner ${baseStyle.name} avec ${fusionElement.name}"
  }
  
  private def assessFusionRisk(fusionElement: FusionElement, experimentalLevel: ExperimentalLevel): RiskLevel = {
    experimentalLevel match {
      case ExperimentalLevel.Conservative => RiskLevel.Low
      case ExperimentalLevel.Moderate => RiskLevel.Medium
      case ExperimentalLevel.Aggressive => RiskLevel.High
    }
  }
}

// ==========================================================================
// TYPES DE SUPPORT - STYLES ET CRITÈRES
// ==========================================================================

sealed trait BeerStyle {
  def name: String
}
object BeerStyle {
  case object AmericanIPA extends BeerStyle { val name = "American IPA" }
  case object EnglishPale extends BeerStyle { val name = "English Pale Ale" }
  case object BelgianTripel extends BeerStyle { val name = "Belgian Tripel" }
  case object Weissbier extends BeerStyle { val name = "Weissbier" }
  case object GermanLager extends BeerStyle { val name = "German Lager" }
  case object CzechPilsner extends BeerStyle { val name = "Czech Pilsner" }
  case object Saison extends BeerStyle { val name = "Saison" }
  case object Sour extends BeerStyle { val name = "Sour Beer" }
  case object ImperialStout extends BeerStyle { val name = "Imperial Stout" }
  case object Generic extends BeerStyle { val name = "Generic Style" }
}

case class TemperatureRange(min: Int, max: Int) {
  def optimal: Int = (min + max) / 2
}

case class AttenuationRange(min: Int, max: Int)

case class AlcoholToleranceRange(min: Int, max: Int)

case class StyleCriteria(
  preferredTypes: List[YeastType],
  temperatureRange: TemperatureRange,
  attenuationRange: AttenuationRange,
  flocculation: List[FlocculationLevel],
  characteristicKeywords: List[String],
  avoidKeywords: List[String],
  alcoholTolerance: AlcoholToleranceRange,
  specialNotes: String
)

case class StyleRecommendedYeast(
  yeast: YeastAggregate,
  styleScore: Double,
  compatibilityReason: String,
  brewingTips: List[String],
  confidenceLevel: ConfidenceLevel
)

case class AlternativeRecommendation(
  category: String,
  description: String,
  reason: String
)

case class BrewingNotes(
  fermentationAdvice: String,
  temperatureTips: List[String],
  timingNotes: String,
  commonMistakes: List[String],
  successIndicators: List[String]
)

sealed trait ConfidenceLevel
object ConfidenceLevel {
  case object High extends ConfidenceLevel
  case object Medium extends ConfidenceLevel
  case object Low extends ConfidenceLevel
}

case class StyleRecommendationResult(
  style: BeerStyle,
  primaryRecommendations: List[StyleRecommendedYeast],
  alternatives: List[AlternativeRecommendation],
  styleCriteria: StyleCriteria,
  brewingNotes: BrewingNotes,
  confidenceLevel: ConfidenceLevel,
  generatedAt: Instant
)

case class MultiStyleRecommendationResult(
  styleRecommendations: Map[BeerStyle, StyleRecommendationResult],
  crossStyleAnalysis: JsValue,
  versatileYeasts: List[VersatileYeast],
  generatedAt: Instant
)

case class VersatileYeast(
  yeast: YeastAggregate,
  compatibleStyles: List[BeerStyle],
  versatilityScore: Double
)

// Types pour fusion (implémentation future)
sealed trait FusionElement { def name: String }
object FusionElement {
  case object Wild extends FusionElement { val name = "Wild" }
  case object Brett extends FusionElement { val name = "Brettanomyces" }
  case object Fruit extends FusionElement { val name = "Fruit" }
}

sealed trait ExperimentalLevel
object ExperimentalLevel {
  case object Conservative extends ExperimentalLevel
  case object Moderate extends ExperimentalLevel  
  case object Aggressive extends ExperimentalLevel
}

sealed trait RiskLevel
object RiskLevel {
  case object Low extends RiskLevel
  case object Medium extends RiskLevel
  case object High extends RiskLevel
}

case class HybridRecommendation(
  primaryYeast: YeastAggregate,
  secondaryYeast: Option[YeastAggregate],
  blendRatio: String,
  expectedProfile: String
)

case class FusionRecommendationResult(
  baseStyle: BeerStyle,
  fusionElement: FusionElement,
  experimentalLevel: ExperimentalLevel,
  hybridRecommendations: List[HybridRecommendation],
  traditionalOptions: List[StyleRecommendedYeast],
  innovationNotes: String,
  riskAssessment: RiskLevel,
  generatedAt: Instant
)