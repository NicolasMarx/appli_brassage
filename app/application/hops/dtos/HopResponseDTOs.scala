package application.hops.dtos

import domain.hops.model.HopFilter
import play.api.libs.json._
import java.time.Instant

/**
 * DTOs pour les réponses des APIs de houblons
 * Réplique exactement YeastResponseDTOs pour les houblons
 */

// ============================================================================
// DTOs DE BASE
// ============================================================================

case class HopResponseDTO(
  id: String,
  name: String,
  description: Option[String],
  origin: HopOriginDTO,
  characteristics: HopCharacteristicsDTO,
  availability: HopAvailabilityDTO,
  createdAt: Instant,
  updatedAt: Instant,
  version: Int
)

case class HopOriginDTO(
  country: String,
  region: Option[String],
  farm: Option[String]
)

case class HopCharacteristicsDTO(
  alphaAcids: Option[Double],
  betaAcids: Option[Double],
  cohumulone: Option[Double],
  totalOils: Option[Double],
  usage: List[String],
  aromaProfiles: List[AromaProfileDTO],
  notes: Option[String]
)

case class AromaProfileDTO(
  name: String,
  intensity: Option[Int],
  description: Option[String]
)

case class HopAvailabilityDTO(
  isAvailable: Boolean,
  harvestYear: Option[Int],
  freshness: Option[String],
  suppliers: List[String]
)

// ============================================================================
// DTOs DE RECOMMANDATIONS
// ============================================================================

case class HopRecommendationDTO(
  hop: HopResponseDTO,
  score: Double,
  reason: String,
  tips: List[String],
  confidence: String // "HIGH", "MEDIUM", "LOW"
)

case class HopRecommendationListResponseDTO(
  recommendations: List[HopRecommendationDTO],
  totalCount: Int,
  context: String, // "beginner", "seasonal_spring", etc.
  description: String
)

// ============================================================================
// DTOs DE RECHERCHE
// ============================================================================

case class HopSearchResponseDTO(
  hops: List[HopResponseDTO],
  totalCount: Int,
  page: Int,
  size: Int,
  hasNext: Boolean,
  appliedFilters: HopFilter,
  suggestions: List[String] // Suggestions d'amélioration de la recherche
)

case class HopFilterSummaryDTO(
  totalFilters: Int,
  activeFilters: Map[String, String], // nom du filtre -> valeur
  availableOptions: Map[String, List[String]] // nom du filtre -> options disponibles
)

// ============================================================================
// DTOs D'ANALYTICS
// ============================================================================

case class HopUsageStatsDTO(
  totalHops: Int,
  bitteringHops: Int,
  aromaHops: Int,
  dualPurposeHops: Int,
  averageAlphaAcids: Double,
  popularOrigins: List[CountryStatsDTO],
  aromaProfileDistribution: List[AromaStatsDTO]
)

case class CountryStatsDTO(
  country: String,
  hopCount: Int,
  percentage: Double
)

case class AromaStatsDTO(
  profile: String,
  hopCount: Int,
  percentage: Double,
  averageIntensity: Option[Double]
)

// ============================================================================
// DTOs DE COMPARAISON
// ============================================================================

case class HopComparisonDTO(
  hops: List[HopResponseDTO],
  similarities: List[SimilarityDTO],
  differences: List[DifferenceDTO],
  recommendations: List[String] // Conseils d'usage
)

case class SimilarityDTO(
  aspect: String, // "alpha_acids", "aroma_profiles", etc.
  description: String,
  score: Double // 0.0 à 1.0
)

case class DifferenceDTO(
  aspect: String,
  hop1Value: String,
  hop2Value: String,
  significance: String // "major", "moderate", "minor"
)

// ============================================================================
// DTOs D'ERREUR ET VALIDATION
// ============================================================================

case class HopValidationErrorDTO(
  field: String,
  error: String,
  suggestedValue: Option[String] = None
)

case class HopNotFoundErrorDTO(
  hopId: String,
  message: String,
  suggestions: List[HopResponseDTO] = List.empty
)

// ============================================================================
// IMPLICITS JSON
// ============================================================================

object HopResponseDTOs {
  implicit val hopOriginFormat: Format[HopOriginDTO] = Json.format[HopOriginDTO]
  implicit val aromaProfileFormat: Format[AromaProfileDTO] = Json.format[AromaProfileDTO]
  implicit val hopCharacteristicsFormat: Format[HopCharacteristicsDTO] = Json.format[HopCharacteristicsDTO]
  implicit val hopAvailabilityFormat: Format[HopAvailabilityDTO] = Json.format[HopAvailabilityDTO]
  implicit val hopResponseFormat: Format[HopResponseDTO] = Json.format[HopResponseDTO]
  
  implicit val hopRecommendationFormat: Format[HopRecommendationDTO] = Json.format[HopRecommendationDTO]
  implicit val hopRecommendationListFormat: Format[HopRecommendationListResponseDTO] = Json.format[HopRecommendationListResponseDTO]
  
  implicit val hopSearchResponseFormat: Format[HopSearchResponseDTO] = Json.format[HopSearchResponseDTO]
  implicit val hopFilterSummaryFormat: Format[HopFilterSummaryDTO] = Json.format[HopFilterSummaryDTO]
  
  implicit val countryStatsFormat: Format[CountryStatsDTO] = Json.format[CountryStatsDTO]
  implicit val aromaStatsFormat: Format[AromaStatsDTO] = Json.format[AromaStatsDTO]
  implicit val hopUsageStatsFormat: Format[HopUsageStatsDTO] = Json.format[HopUsageStatsDTO]
  
  implicit val similarityFormat: Format[SimilarityDTO] = Json.format[SimilarityDTO]
  implicit val differenceFormat: Format[DifferenceDTO] = Json.format[DifferenceDTO]
  implicit val hopComparisonFormat: Format[HopComparisonDTO] = Json.format[HopComparisonDTO]
  
  implicit val hopValidationErrorFormat: Format[HopValidationErrorDTO] = Json.format[HopValidationErrorDTO]
  implicit val hopNotFoundErrorFormat: Format[HopNotFoundErrorDTO] = Json.format[HopNotFoundErrorDTO]
}