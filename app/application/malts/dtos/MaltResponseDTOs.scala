package application.malts.dtos

import domain.malts.model.{MaltFilter, MaltSource, MaltType}
import play.api.libs.json._
import java.time.Instant

/**
 * DTOs pour les réponses des APIs de malts
 * Réplique exactement YeastResponseDTOs et HopResponseDTOs pour les malts
 */

// ============================================================================
// DTOs DE BASE
// ============================================================================

case class MaltResponseDTO(
  id: String,
  name: String,
  maltType: String,
  description: Option[String],
  characteristics: MaltCharacteristicsDTO,
  brewing: MaltBrewingInfoDTO,
  quality: MaltQualityDTO,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
)

case class MaltCharacteristicsDTO(
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  flavorProfiles: List[String],
  maxRecommendedPercent: Option[Double]
)

case class MaltBrewingInfoDTO(
  isBaseMalt: Boolean,
  canSelfConvert: Boolean,
  enzymePowerCategory: String,
  usageRecommendations: List[String]
)

case class MaltQualityDTO(
  credibilityScore: Double,
  qualityScore: Double,
  source: String,
  needsReview: Boolean,
  canBeUsedInBrewing: Boolean
)

case class MaltOriginDTO(
  originCode: String,
  region: Option[String],
  country: Option[String]
)

// ============================================================================
// DTOs DE RECOMMANDATIONS
// ============================================================================

case class MaltRecommendationDTO(
  malt: MaltResponseDTO,
  score: Double,
  reason: String,
  tips: List[String],
  confidence: String // "HIGH", "MEDIUM", "LOW"
)

case class MaltRecommendationListResponseDTO(
  recommendations: List[MaltRecommendationDTO],
  totalCount: Int,
  context: String, // "beginner", "seasonal_spring", etc.
  description: String,
  metadata: RecommendationMetadataDTO
)

case class RecommendationMetadataDTO(
  algorithmVersion: String = "1.0",
  requestTimestamp: Long = System.currentTimeMillis(),
  processingTimeMs: Option[Long] = None,
  parameters: Map[String, String] = Map.empty
)

// ============================================================================
// DTOs DE RECHERCHE
// ============================================================================

case class MaltSearchResponseDTO(
  malts: List[MaltResponseDTO],
  totalCount: Int,
  page: Int,
  size: Int,
  hasNext: Boolean,
  appliedFilters: MaltFilter,
  suggestions: List[String], // Suggestions d'amélioration de la recherche
  facets: MaltFacetsDTO
)

case class MaltFacetsDTO(
  maltTypes: List[FacetDTO],
  colorRanges: List[FacetDTO],
  extractionRanges: List[FacetDTO],
  sources: List[FacetDTO],
  flavorCategories: List[FacetDTO]
)

case class FacetDTO(
  value: String,
  count: Int,
  displayName: String
)

case class MaltFilterSummaryDTO(
  totalFilters: Int,
  activeFilters: Map[String, String], // nom du filtre -> valeur
  availableOptions: Map[String, List[String]] // nom du filtre -> options disponibles
)

// ============================================================================
// DTOs D'ANALYTICS
// ============================================================================

case class MaltUsageStatsDTO(
  totalMalts: Int,
  baseMalts: Int,
  specialtyMalts: Int,
  crystalMalts: Int,
  roastedMalts: Int,
  averageExtractionRate: Double,
  averageEbcColor: Double,
  popularSources: List[SourceStatsDTO],
  flavorProfileDistribution: List[FlavorStatsDTO],
  colorDistribution: List[ColorRangeStatsDTO]
)

case class SourceStatsDTO(
  source: String,
  maltCount: Int,
  percentage: Double,
  averageQuality: Double
)

case class FlavorStatsDTO(
  profile: String,
  maltCount: Int,
  percentage: Double,
  averageIntensity: Option[Double]
)

case class ColorRangeStatsDTO(
  range: String, // "Light (0-20)", "Amber (20-60)", etc.
  maltCount: Int,
  percentage: Double,
  minEbc: Double,
  maxEbc: Double
)

// ============================================================================
// DTOs DE COMPARAISON
// ============================================================================

case class MaltComparisonDTO(
  malts: List[MaltResponseDTO],
  similarities: List[SimilarityDTO],
  differences: List[DifferenceDTO],
  recommendations: List[String], // Conseils d'usage
  substitutionMatrix: List[SubstitutionDTO]
)

case class SimilarityDTO(
  aspect: String, // "ebc_color", "extraction_rate", "flavor_profiles", etc.
  description: String,
  score: Double // 0.0 à 1.0
)

case class DifferenceDTO(
  aspect: String,
  malt1Value: String,
  malt2Value: String,
  significance: String, // "major", "moderate", "minor"
  impact: String // "color", "flavor", "extraction", "none"
)

case class SubstitutionDTO(
  originalMalt: String,
  substituteMalt: String,
  ratio: Double, // ratio de substitution (ex: 1.2 = utiliser 20% de plus)
  adjustments: List[String], // ajustements nécessaires
  confidence: String // "high", "medium", "low"
)

// ============================================================================
// DTOs SPÉCIALISÉS PAR STYLE DE BIÈRE
// ============================================================================

case class BeerStyleMaltRecommendationDTO(
  beerStyle: String,
  grainBillSuggestion: GrainBillDTO,
  malts: List[MaltRecommendationDTO],
  brewingNotes: List[String],
  colorPrediction: ColorPredictionDTO,
  alternatives: List[AlternativeMaltDTO]
)

case class GrainBillDTO(
  baseMalt: GrainBillItemDTO,
  specialtyMalts: List[GrainBillItemDTO],
  totalWeight: Double,
  expectedColor: Double,
  expectedExtraction: Double
)

case class GrainBillItemDTO(
  maltId: String,
  maltName: String,
  percentage: Double,
  weight: Double,
  contribution: String // "base", "color", "flavor", "texture"
)

case class ColorPredictionDTO(
  predictedEBC: Double,
  predictedSRM: Double,
  colorDescription: String,
  confidence: String,
  factors: List[String] // facteurs influençant la couleur
)

case class AlternativeMaltDTO(
  originalMalt: MaltResponseDTO,
  alternatives: List[MaltRecommendationDTO],
  reason: String
)

// ============================================================================
// DTOs D'ERREUR ET VALIDATION
// ============================================================================

case class MaltValidationErrorDTO(
  field: String,
  error: String,
  suggestedValue: Option[String] = None,
  validOptions: List[String] = List.empty
)

case class MaltNotFoundErrorDTO(
  maltId: String,
  message: String,
  suggestions: List[MaltResponseDTO] = List.empty,
  similarMalts: List[MaltResponseDTO] = List.empty
)

case class MaltSearchErrorDTO(
  query: String,
  message: String,
  suggestions: List[String],
  didYouMean: Option[String] = None
)

// ============================================================================
// DTOs DE BATCH ET RECETTES
// ============================================================================

case class BatchRecommendationDTO(
  batchSize: Double,
  malts: List[BatchMaltItemDTO],
  instructions: List[String],
  expectedResults: BatchExpectedResultsDTO,
  tips: List[String]
)

case class BatchMaltItemDTO(
  malt: MaltResponseDTO,
  weight: Double,
  percentage: Double,
  timing: String, // "mash", "boil", "secondary"
  purpose: String // "base", "color", "flavor", "body"
)

case class BatchExpectedResultsDTO(
  originalGravity: Double,
  finalGravity: Double,
  abv: Double,
  ibu: Double,
  ebc: Double,
  efficiency: Double
)

// ============================================================================
// DTOs DE PERFORMANCE ET MONITORING
// ============================================================================

case class MaltServiceHealthDTO(
  status: String, // "healthy", "degraded", "unhealthy"
  version: String,
  features: List[String],
  statistics: ServiceStatsDTO,
  lastUpdated: Instant
)

case class ServiceStatsDTO(
  totalRequests: Long,
  averageResponseTime: Double,
  cacheHitRate: Double,
  errorRate: Double,
  activeFilters: Int
)

// ============================================================================
// IMPLICITS JSON
// ============================================================================

object MaltResponseDTOs {
  implicit val maltCharacteristicsFormat: Format[MaltCharacteristicsDTO] = Json.format[MaltCharacteristicsDTO]
  implicit val maltBrewingInfoFormat: Format[MaltBrewingInfoDTO] = Json.format[MaltBrewingInfoDTO]
  implicit val maltQualityFormat: Format[MaltQualityDTO] = Json.format[MaltQualityDTO]
  implicit val maltOriginFormat: Format[MaltOriginDTO] = Json.format[MaltOriginDTO]
  implicit val maltResponseFormat: Format[MaltResponseDTO] = Json.format[MaltResponseDTO]
  
  implicit val recommendationMetadataFormat: Format[RecommendationMetadataDTO] = Json.format[RecommendationMetadataDTO]
  implicit val maltRecommendationFormat: Format[MaltRecommendationDTO] = Json.format[MaltRecommendationDTO]
  implicit val maltRecommendationListFormat: Format[MaltRecommendationListResponseDTO] = Json.format[MaltRecommendationListResponseDTO]
  
  implicit val facetFormat: Format[FacetDTO] = Json.format[FacetDTO]
  implicit val maltFacetsFormat: Format[MaltFacetsDTO] = Json.format[MaltFacetsDTO]
  implicit val maltSearchResponseFormat: Format[MaltSearchResponseDTO] = Json.format[MaltSearchResponseDTO]
  implicit val maltFilterSummaryFormat: Format[MaltFilterSummaryDTO] = Json.format[MaltFilterSummaryDTO]
  
  implicit val sourceStatsFormat: Format[SourceStatsDTO] = Json.format[SourceStatsDTO]
  implicit val flavorStatsFormat: Format[FlavorStatsDTO] = Json.format[FlavorStatsDTO]
  implicit val colorRangeStatsFormat: Format[ColorRangeStatsDTO] = Json.format[ColorRangeStatsDTO]
  implicit val maltUsageStatsFormat: Format[MaltUsageStatsDTO] = Json.format[MaltUsageStatsDTO]
  
  implicit val similarityFormat: Format[SimilarityDTO] = Json.format[SimilarityDTO]
  implicit val differenceFormat: Format[DifferenceDTO] = Json.format[DifferenceDTO]
  implicit val substitutionFormat: Format[SubstitutionDTO] = Json.format[SubstitutionDTO]
  implicit val maltComparisonFormat: Format[MaltComparisonDTO] = Json.format[MaltComparisonDTO]
  
  implicit val grainBillItemFormat: Format[GrainBillItemDTO] = Json.format[GrainBillItemDTO]
  implicit val grainBillFormat: Format[GrainBillDTO] = Json.format[GrainBillDTO]
  implicit val colorPredictionFormat: Format[ColorPredictionDTO] = Json.format[ColorPredictionDTO]
  implicit val alternativeMaltFormat: Format[AlternativeMaltDTO] = Json.format[AlternativeMaltDTO]
  implicit val beerStyleMaltRecommendationFormat: Format[BeerStyleMaltRecommendationDTO] = Json.format[BeerStyleMaltRecommendationDTO]
  
  implicit val batchExpectedResultsFormat: Format[BatchExpectedResultsDTO] = Json.format[BatchExpectedResultsDTO]
  implicit val batchMaltItemFormat: Format[BatchMaltItemDTO] = Json.format[BatchMaltItemDTO]
  implicit val batchRecommendationFormat: Format[BatchRecommendationDTO] = Json.format[BatchRecommendationDTO]
  
  implicit val maltValidationErrorFormat: Format[MaltValidationErrorDTO] = Json.format[MaltValidationErrorDTO]
  implicit val maltNotFoundErrorFormat: Format[MaltNotFoundErrorDTO] = Json.format[MaltNotFoundErrorDTO]
  implicit val maltSearchErrorFormat: Format[MaltSearchErrorDTO] = Json.format[MaltSearchErrorDTO]
  
  implicit val serviceStatsFormat: Format[ServiceStatsDTO] = Json.format[ServiceStatsDTO]
  implicit val maltServiceHealthFormat: Format[MaltServiceHealthDTO] = Json.format[MaltServiceHealthDTO]
}

// ============================================================================
// UTILITAIRES DE CONVERSION
// ============================================================================

object MaltDTOConverters {
  
  def convertMaltToDTO(malt: domain.malts.model.MaltAggregate): MaltResponseDTO = {
    MaltResponseDTO(
      id = malt.id.value.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      description = malt.description,
      characteristics = MaltCharacteristicsDTO(
        ebcColor = malt.ebcColor.value,
        extractionRate = malt.extractionRate.value,
        diastaticPower = malt.diastaticPower.value,
        flavorProfiles = malt.flavorProfiles,
        maxRecommendedPercent = malt.maxRecommendedPercent
      ),
      brewing = MaltBrewingInfoDTO(
        isBaseMalt = malt.isBaseMalt,
        canSelfConvert = malt.canSelfConvert,
        enzymePowerCategory = malt.enzymePowerCategory,
        usageRecommendations = generateUsageRecommendations(malt)
      ),
      quality = MaltQualityDTO(
        credibilityScore = malt.credibilityScore,
        qualityScore = malt.qualityScore,
        source = malt.source.toString,
        needsReview = malt.needsReview,
        canBeUsedInBrewing = malt.canBeUsedInBrewing
      ),
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt,
      version = malt.version
    )
  }
  
  def convertRecommendedMaltToDTO(
    recommendedMalt: domain.malts.services.RecommendedMalt
  ): MaltRecommendationDTO = {
    MaltRecommendationDTO(
      malt = convertMaltToDTO(recommendedMalt.malt),
      score = recommendedMalt.score,
      reason = recommendedMalt.reason,
      tips = recommendedMalt.tips,
      confidence = calculateConfidenceLevel(recommendedMalt.score)
    )
  }
  
  private def generateUsageRecommendations(malt: domain.malts.model.MaltAggregate): List[String] = {
    val recommendations = scala.collection.mutable.ListBuffer[String]()
    
    if (malt.isBaseMalt) {
      recommendations += "Peut représenter 80-100% du grain bill"
    }
    
    malt.maxRecommendedPercent match {
      case Some(percent) if percent <= 20 =>
        recommendations += s"Utiliser maximum $percent% du grain bill"
      case Some(percent) =>
        recommendations += s"Proportion recommandée : jusqu'à $percent%"
      case None =>
    }
    
    if (malt.canSelfConvert) {
      recommendations += "Possède assez d'enzymes pour convertir des adjuvants"
    }
    
    malt.maltType match {
      case MaltType.ROASTED =>
        recommendations += "Ajouter en fin de empâtage pour éviter l'astringence"
      case MaltType.CRYSTAL =>
        recommendations += "Ajouter pendant l'empâtage principal"
      case MaltType.WHEAT =>
        recommendations += "Améliore la rétention de mousse"
      case _ =>
    }
    
    recommendations.toList
  }
  
  private def calculateConfidenceLevel(score: Double): String = {
    if (score >= 0.8) "HIGH"
    else if (score >= 0.5) "MEDIUM"
    else "LOW"
  }
  
  def buildSearchSuggestions(
    filter: MaltFilter,
    result: domain.common.PaginatedResult[domain.malts.model.MaltAggregate]
  ): List[String] = {
    val suggestions = scala.collection.mutable.ListBuffer[String]()
    
    if (result.totalCount == 0) {
      suggestions += "Essayez d'élargir vos critères de recherche"
      
      if (filter.minEbcColor.isDefined || filter.maxEbcColor.isDefined) {
        suggestions += "Ajustez les plages de couleur EBC"
      }
      
      if (filter.flavorProfiles.nonEmpty) {
        suggestions += "Utilisez des profils de saveur plus généraux"
      }
      
      if (filter.maltType.isDefined) {
        suggestions += "Essayez d'autres types de malts"
      }
    } else if (result.totalCount < 5) {
      suggestions += "Peu de résultats trouvés - considérez des critères plus larges"
    }
    
    if (filter.minExtractionRate.isDefined && filter.minExtractionRate.get > 80) {
      suggestions += "Réduisez le taux d'extraction minimum"
    }
    
    if (filter.maxCredibilityScore.isDefined && filter.maxCredibilityScore.get < 0.7) {
      suggestions += "Augmentez le score de crédibilité pour plus d'options"
    }
    
    suggestions.toList
  }
  
  def buildMaltFacets(
    malts: List[domain.malts.model.MaltAggregate]
  ): MaltFacetsDTO = {
    val maltTypeGroups = malts.groupBy(_.maltType)
    val maltTypeFacets = maltTypeGroups.map { case (maltType, maltList) =>
      FacetDTO(
        value = maltType.name,
        count = maltList.size,
        displayName = maltType.category
      )
    }.toList.sortBy(-_.count)
    
    val colorRanges = List(
      ("0-20", "Clair", malts.count(_.ebcColor.value <= 20)),
      ("20-60", "Ambré", malts.count(m => m.ebcColor.value > 20 && m.ebcColor.value <= 60)),
      ("60-150", "Brun", malts.count(m => m.ebcColor.value > 60 && m.ebcColor.value <= 150)),
      ("150+", "Noir", malts.count(_.ebcColor.value > 150))
    ).map { case (range, display, count) =>
      FacetDTO(range, count, display)
    }.filter(_.count > 0)
    
    val extractionRanges = List(
      ("0-70", "Faible", malts.count(_.extractionRate.value <= 70)),
      ("70-80", "Moyenne", malts.count(m => m.extractionRate.value > 70 && m.extractionRate.value <= 80)),
      ("80-90", "Élevée", malts.count(m => m.extractionRate.value > 80 && m.extractionRate.value <= 90)),
      ("90+", "Très élevée", malts.count(_.extractionRate.value > 90))
    ).map { case (range, display, count) =>
      FacetDTO(range, count, display)
    }.filter(_.count > 0)
    
    val sourceGroups = malts.groupBy(_.source)
    val sourceFacets = sourceGroups.map { case (source, maltList) =>
      FacetDTO(
        value = source.toString,
        count = maltList.size,
        displayName = source.toString
      )
    }.toList.sortBy(-_.count)
    
    // Analyse des profils de saveur les plus fréquents
    val allFlavors = malts.flatMap(_.flavorProfiles)
    val flavorGroups = allFlavors.groupBy(identity)
    val flavorFacets = flavorGroups.map { case (flavor, occurrences) =>
      FacetDTO(
        value = flavor,
        count = occurrences.size,
        displayName = flavor.capitalize
      )
    }.toList.sortBy(-_.count).take(10)
    
    MaltFacetsDTO(
      maltTypes = maltTypeFacets,
      colorRanges = colorRanges,
      extractionRanges = extractionRanges,
      sources = sourceFacets,
      flavorCategories = flavorFacets
    )
  }
}