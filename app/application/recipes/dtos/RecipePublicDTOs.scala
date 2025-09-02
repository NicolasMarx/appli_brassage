package application.recipes.dtos

import play.api.libs.json._
import domain.recipes.model._
import java.time.Instant

// ============================================================================
// RESPONSE DTOs POUR API PUBLIQUE
// ============================================================================

/**
 * DTO principal pour recette publique avec informations optimisées pour consultation
 */
case class RecipePublicDTO(
  id: String,
  name: String,
  description: Option[String],
  style: BeerStyleDTO,
  batchSize: BatchSizeDTO,
  difficulty: String,
  estimatedTime: EstimatedTimeDTO,
  ingredients: RecipeIngredientsDTO,
  calculations: RecipeCalculationsPublicDTO,
  characteristics: RecipeCharacteristicsDTO,
  tags: List[String],
  popularity: Double,
  createdAt: Instant,
  updatedAt: Instant
)

object RecipePublicDTO {
  implicit val format: Format[RecipePublicDTO] = Json.format[RecipePublicDTO]
}

/**
 * DTO détaillé pour consultation complète avec guide de brassage
 */
case class RecipeDetailResponseDTO(
  recipe: RecipePublicDTO,
  brewingGuide: Option[BrewingGuideDTO],
  nutritionalInfo: NutritionalInfoDTO,
  substitutions: List[IngredientSubstitutionDTO],
  tips: List[BrewingTipDTO],
  relatedRecipes: List[RecipePublicDTO]
)

object RecipeDetailResponseDTO {
  def fromAggregate(recipe: RecipeAggregate): RecipeDetailResponseDTO = {
    RecipeDetailResponseDTO(
      recipe = convertRecipeToPublicDTO(recipe),
      brewingGuide = generateBrewingGuide(recipe),
      nutritionalInfo = calculateNutritionalInfo(recipe),
      substitutions = generateSubstitutions(recipe),
      tips = generateBrewingTips(recipe),
      relatedRecipes = List.empty // À remplir par le service
    )
  }
  
  // Méthodes utilitaires simplifiées
  private def convertRecipeToPublicDTO(recipe: RecipeAggregate): RecipePublicDTO = {
    RecipePublicDTO(
      id = recipe.id.value.toString,
      name = recipe.name.value,
      description = recipe.description.map(_.value),
      style = BeerStyleDTO.fromDomain(recipe.style),
      batchSize = BatchSizeDTO.fromDomain(recipe.batchSize),
      difficulty = "intermediate", // Calculé
      estimatedTime = EstimatedTimeDTO(4.0, 2.0, 3.0, TimeBreakdownDTO(60, 90, 60, 30, 60)),
      ingredients = RecipeIngredientsDTO(
        hops = recipe.hops.map(RecipeHopDTO.fromDomain),
        malts = recipe.malts.map(RecipeMaltDTO.fromDomain),
        yeast = recipe.yeast.map(RecipeYeastDTO.fromDomain),
        others = recipe.otherIngredients.map(OtherIngredientDTO.fromDomain)
      ),
      calculations = RecipeCalculationsPublicDTO.fromDomain(recipe.calculations),
      characteristics = RecipeCharacteristicsDTO(None, None, None, AromaProfileDTO(List.empty, "balanced"), MouthfeelProfileDTO("medium", "moderate")),
      tags = List("intermediate", recipe.style.category.toLowerCase),
      popularity = 0.7,
      createdAt = recipe.createdAt,
      updatedAt = recipe.updatedAt
    )
  }
  
  private def generateBrewingGuide(recipe: RecipeAggregate): Option[BrewingGuideDTO] = None
  private def calculateNutritionalInfo(recipe: RecipeAggregate): NutritionalInfoDTO = 
    NutritionalInfoDTO(150, 12.0, 3.0, 0.0, 1.0, 10)
  private def generateSubstitutions(recipe: RecipeAggregate): List[IngredientSubstitutionDTO] = List.empty
  private def generateBrewingTips(recipe: RecipeAggregate): List[BrewingTipDTO] = List.empty
  
  implicit val format: Format[RecipeDetailResponseDTO] = Json.format[RecipeDetailResponseDTO]
}

/**
 * DTO pour réponses de recherche avec facettes
 */
case class RecipeSearchResponseDTO(
  recipes: List[RecipePublicDTO],
  totalCount: Int,
  page: Int,
  size: Int,
  hasNext: Boolean,
  appliedFilters: RecipePublicFilter,
  facets: RecipeFacetsDTO,
  suggestions: List[String]
)

object RecipeSearchResponseDTO {
  implicit val format: Format[RecipeSearchResponseDTO] = Json.format[RecipeSearchResponseDTO]
}

/**
 * DTO pour découverte de recettes
 */
case class RecipeDiscoveryResponseDTO(
  category: String,
  recipes: List[RecipePublicDTO],
  totalFound: Int,
  description: String
)

object RecipeDiscoveryResponseDTO {
  implicit val format: Format[RecipeDiscoveryResponseDTO] = Json.format[RecipeDiscoveryResponseDTO]
}

// ============================================================================
// RECOMMENDATION DTOs
// ============================================================================

/**
 * DTO pour recommandations de recettes
 */
case class RecipeRecommendationDTO(
  recipe: RecipePublicDTO,
  score: Double,
  reason: String,
  tips: List[String],
  confidence: String,
  matchFactors: List[String]
)

object RecipeRecommendationDTO {
  implicit val format: Format[RecipeRecommendationDTO] = Json.format[RecipeRecommendationDTO]
}

/**
 * DTO pour listes de recommandations
 */
case class RecipeRecommendationListResponseDTO(
  recommendations: List[RecipeRecommendationDTO],
  totalCount: Int,
  context: String,
  description: String,
  metadata: RecommendationMetadataDTO
)

object RecipeRecommendationListResponseDTO {
  implicit val format: Format[RecipeRecommendationListResponseDTO] = Json.format[RecipeRecommendationListResponseDTO]
}

/**
 * Métadonnées de recommandation
 */
case class RecommendationMetadataDTO(
  parameters: Map[String, String],
  algorithm: String = "default",
  confidence: Double = 0.0
)

object RecommendationMetadataDTO {
  implicit val format: Format[RecommendationMetadataDTO] = Json.format[RecommendationMetadataDTO]
}

// ============================================================================
// COMPARISON DTOs
// ============================================================================

/**
 * DTO pour comparaison de recettes
 */
case class RecipeComparisonDTO(
  recipes: List[RecipePublicDTO],
  similarities: List[SimilarityDTO],
  differences: List[DifferenceDTO],
  recommendations: List[String],
  substitutionMatrix: List[SubstitutionDTO],
  summary: String
)

object RecipeComparisonDTO {
  implicit val format: Format[RecipeComparisonDTO] = Json.format[RecipeComparisonDTO]
}

/**
 * DTO pour scaling de recettes
 */
case class RecipeScalingResponseDTO(
  originalRecipe: RecipePublicDTO,
  scaledRecipe: RecipePublicDTO,
  scalingFactor: Double,
  adjustments: List[String],
  warnings: List[String]
)

object RecipeScalingResponseDTO {
  implicit val format: Format[RecipeScalingResponseDTO] = Json.format[RecipeScalingResponseDTO]
}

// ============================================================================
// INGREDIENT DTOs
// ============================================================================

/**
 * DTO groupé pour tous les ingrédients
 */
case class RecipeIngredientsDTO(
  hops: List[RecipeHopDTO],
  malts: List[RecipeMaltDTO],
  yeast: Option[RecipeYeastDTO],
  others: List[OtherIngredientDTO]
)

object RecipeIngredientsDTO {
  implicit val format: Format[RecipeIngredientsDTO] = Json.format[RecipeIngredientsDTO]
}

/**
 * DTO pour substitutions d'ingrédients
 */
case class IngredientSubstitutionDTO(
  originalIngredient: String,
  alternatives: List[AlternativeIngredientDTO],
  difficulty: String,
  notes: Option[String]
)

object IngredientSubstitutionDTO {
  implicit val format: Format[IngredientSubstitutionDTO] = Json.format[IngredientSubstitutionDTO]
}

case class AlternativeIngredientDTO(
  name: String,
  ratio: Double,
  availability: String,
  notes: String
)

object AlternativeIngredientDTO {
  implicit val format: Format[AlternativeIngredientDTO] = Json.format[AlternativeIngredientDTO]
}

// ============================================================================
// CALCULATION DTOs
// ============================================================================

/**
 * DTO optimisé pour calculs publics
 */
case class RecipeCalculationsPublicDTO(
  originalGravity: Option[Double],
  finalGravity: Option[Double],
  abv: Option[Double],
  ibu: Option[Double],
  srm: Option[Double],
  efficiency: Option[Double],
  calories: Option[Int],
  isComplete: Boolean,
  advancedMetrics: Option[AdvancedMetricsDTO],
  warnings: List[String]
)

object RecipeCalculationsPublicDTO {
  def fromDomain(calculations: RecipeCalculations): RecipeCalculationsPublicDTO = {
    RecipeCalculationsPublicDTO(
      originalGravity = calculations.originalGravity,
      finalGravity = calculations.finalGravity,
      abv = calculations.abv,
      ibu = calculations.ibu,
      srm = calculations.srm,
      efficiency = calculations.efficiency,
      calories = calculations.abv.map(abv => (abv * 150 / 5).toInt), // Estimation
      isComplete = calculations.isComplete,
      advancedMetrics = buildAdvancedMetrics(calculations),
      warnings = calculations.warnings
    )
  }
  
  private def buildAdvancedMetrics(calculations: RecipeCalculations): Option[AdvancedMetricsDTO] = {
    calculations.brewingCalculationResult.map { result =>
      AdvancedMetricsDTO(
        balanceRatio = result.balanceRatio.getOrElse(0.0),
        bitternessRatio = result.bitternessRatio.getOrElse(0.0),
        attenuation = result.attenuation.apparent,
        realExtract = result.efficiency.calculated,
        caloriesPerServing = result.calories.perServingMl.toInt
      )
    }
  }
  
  implicit val format: Format[RecipeCalculationsPublicDTO] = Json.format[RecipeCalculationsPublicDTO]
}

case class AdvancedMetricsDTO(
  balanceRatio: Double,
  bitternessRatio: Double,
  attenuation: Double,
  realExtract: Double,
  caloriesPerServing: Int
)

object AdvancedMetricsDTO {
  implicit val format: Format[AdvancedMetricsDTO] = Json.format[AdvancedMetricsDTO]
}

// ============================================================================
// CHARACTERISTIC DTOs
// ============================================================================

/**
 * DTO pour caractéristiques sensorielles
 */
case class RecipeCharacteristicsDTO(
  color: Option[ColorCharacteristicDTO],
  bitterness: Option[BitternessCharacteristicDTO],
  strength: Option[StrengthCharacteristicDTO],
  aroma: AromaProfileDTO,
  mouthfeel: MouthfeelProfileDTO
)

object RecipeCharacteristicsDTO {
  implicit val format: Format[RecipeCharacteristicsDTO] = Json.format[RecipeCharacteristicsDTO]
}

case class ColorCharacteristicDTO(
  srm: Double,
  ebc: Double,
  description: String
)

object ColorCharacteristicDTO {
  implicit val format: Format[ColorCharacteristicDTO] = Json.format[ColorCharacteristicDTO]
}

case class BitternessCharacteristicDTO(
  ibu: Double,
  level: String,
  balance: String
)

object BitternessCharacteristicDTO {
  implicit val format: Format[BitternessCharacteristicDTO] = Json.format[BitternessCharacteristicDTO]
}

case class StrengthCharacteristicDTO(
  abv: Double,
  category: String,
  calories: Int
)

object StrengthCharacteristicDTO {
  implicit val format: Format[StrengthCharacteristicDTO] = Json.format[StrengthCharacteristicDTO]
}

case class AromaProfileDTO(
  notes: List[String],
  intensity: String
)

object AromaProfileDTO {
  implicit val format: Format[AromaProfileDTO] = Json.format[AromaProfileDTO]
}

case class MouthfeelProfileDTO(
  body: String,
  carbonation: String
)

object MouthfeelProfileDTO {
  implicit val format: Format[MouthfeelProfileDTO] = Json.format[MouthfeelProfileDTO]
}

// ============================================================================
// TIME DTOs
// ============================================================================

/**
 * DTO pour temps estimés
 */
case class EstimatedTimeDTO(
  brewDayHours: Double,
  fermentationWeeks: Double,
  totalWeeks: Double,
  breakdown: TimeBreakdownDTO
)

object EstimatedTimeDTO {
  implicit val format: Format[EstimatedTimeDTO] = Json.format[EstimatedTimeDTO]
}

case class TimeBreakdownDTO(
  preparation: Int,
  mashing: Int,
  boiling: Int,
  cooling: Int,
  cleanup: Int
)

object TimeBreakdownDTO {
  implicit val format: Format[TimeBreakdownDTO] = Json.format[TimeBreakdownDTO]
}

// ============================================================================
// BREWING GUIDE DTOs
// ============================================================================

/**
 * DTO pour guide de brassage complet
 */
case class BrewingGuideDTO(
  steps: List[BrewingStepDTO],
  timeline: BrewingTimelineDTO,
  equipment: List[String],
  tips: List[BrewingTipDTO],
  troubleshooting: List[TroubleshootingDTO]
)

object BrewingGuideDTO {
  implicit val format: Format[BrewingGuideDTO] = Json.format[BrewingGuideDTO]
}

case class BrewingStepDTO(
  stepNumber: Int,
  phase: String,
  title: String,
  description: String,
  duration: Option[Int],
  temperature: Option[String],
  notes: List[String]
)

object BrewingStepDTO {
  implicit val format: Format[BrewingStepDTO] = Json.format[BrewingStepDTO]
}

case class BrewingTimelineDTO(
  brewDay: List[TimelineEventDTO],
  fermentation: List[TimelineEventDTO],
  conditioning: List[TimelineEventDTO]
)

object BrewingTimelineDTO {
  implicit val format: Format[BrewingTimelineDTO] = Json.format[BrewingTimelineDTO]
}

case class TimelineEventDTO(
  day: Int,
  event: String,
  action: String,
  isOptional: Boolean = false
)

object TimelineEventDTO {
  implicit val format: Format[TimelineEventDTO] = Json.format[TimelineEventDTO]
}

case class BrewingTipDTO(
  category: String,
  tip: String,
  importance: String,
  phase: String
)

object BrewingTipDTO {
  implicit val format: Format[BrewingTipDTO] = Json.format[BrewingTipDTO]
}

case class TroubleshootingDTO(
  problem: String,
  symptoms: List[String],
  solutions: List[String],
  prevention: String
)

object TroubleshootingDTO {
  implicit val format: Format[TroubleshootingDTO] = Json.format[TroubleshootingDTO]
}

// ============================================================================
// NUTRITIONAL DTOs
// ============================================================================

/**
 * DTO pour informations nutritionnelles
 */
case class NutritionalInfoDTO(
  caloriesPerServing: Int,
  carbohydrates: Double,
  protein: Double,
  fat: Double,
  fiber: Double,
  servingSize: Int
)

object NutritionalInfoDTO {
  implicit val format: Format[NutritionalInfoDTO] = Json.format[NutritionalInfoDTO]
}

// ============================================================================
// FACET DTOs
// ============================================================================

/**
 * DTO pour facettes de recherche
 */
case class RecipeFacetsDTO(
  styles: List[FacetItemDTO],
  difficulties: List[FacetItemDTO],
  abvRanges: List[FacetItemDTO],
  ibuRanges: List[FacetItemDTO],
  batchSizeRanges: List[FacetItemDTO],
  popularIngredients: List[FacetItemDTO]
)

object RecipeFacetsDTO {
  implicit val format: Format[RecipeFacetsDTO] = Json.format[RecipeFacetsDTO]
}

case class FacetItemDTO(
  value: String,
  count: Int
)

object FacetItemDTO {
  implicit val format: Format[FacetItemDTO] = Json.format[FacetItemDTO]
}

// ============================================================================
// COLLECTION DTOs
// ============================================================================

/**
 * DTO pour collections de recettes
 */
case class RecipeCollectionDTO(
  id: String,
  name: String,
  description: String,
  category: String,
  recipeCount: Int,
  recipes: List[RecipePublicDTO],
  tags: List[String],
  createdAt: Instant,
  updatedAt: Instant
)

object RecipeCollectionDTO {
  implicit val format: Format[RecipeCollectionDTO] = Json.format[RecipeCollectionDTO]
}

case class RecipeCollectionListResponseDTO(
  collections: List[RecipeCollectionDTO],
  totalCount: Int,
  page: Int,
  size: Int,
  hasNext: Boolean,
  categories: List[String]
)

object RecipeCollectionListResponseDTO {
  implicit val format: Format[RecipeCollectionListResponseDTO] = Json.format[RecipeCollectionListResponseDTO]
}

// ============================================================================
// ANALYSIS DTOs
// ============================================================================

/**
 * DTO pour analyse de recettes personnalisées
 */
case class CustomRecipeAnalysisRequestDTO(
  name: String,
  style: String,
  batchSize: Double,
  batchUnit: String,
  ingredients: CustomIngredientListDTO,
  targetParameters: Option[TargetParametersDTO]
)

object CustomRecipeAnalysisRequestDTO {
  implicit val format: Format[CustomRecipeAnalysisRequestDTO] = Json.format[CustomRecipeAnalysisRequestDTO]
}

case class CustomIngredientListDTO(
  malts: List[CustomMaltDTO],
  hops: List[CustomHopDTO],
  yeast: CustomYeastDTO,
  others: List[CustomOtherIngredientDTO] = List.empty
)

object CustomIngredientListDTO {
  implicit val format: Format[CustomIngredientListDTO] = Json.format[CustomIngredientListDTO]
}

case class CustomMaltDTO(
  name: String,
  amount: Double,
  unit: String,
  color: Double
)

object CustomMaltDTO {
  implicit val format: Format[CustomMaltDTO] = Json.format[CustomMaltDTO]
}

case class CustomHopDTO(
  name: String,
  amount: Double,
  unit: String,
  alphaAcids: Double,
  additionTime: Int
)

object CustomHopDTO {
  implicit val format: Format[CustomHopDTO] = Json.format[CustomHopDTO]
}

case class CustomYeastDTO(
  name: String,
  attenuation: Double,
  temperature: Int
)

object CustomYeastDTO {
  implicit val format: Format[CustomYeastDTO] = Json.format[CustomYeastDTO]
}

case class CustomOtherIngredientDTO(
  name: String,
  amount: Double,
  unit: String,
  additionTime: Int
)

object CustomOtherIngredientDTO {
  implicit val format: Format[CustomOtherIngredientDTO] = Json.format[CustomOtherIngredientDTO]
}

case class TargetParametersDTO(
  og: Option[Double],
  fg: Option[Double],
  abv: Option[Double],
  ibu: Option[Double],
  srm: Option[Double]
)

object TargetParametersDTO {
  implicit val format: Format[TargetParametersDTO] = Json.format[TargetParametersDTO]
}

case class RecipeAnalysisResponseDTO(
  calculations: RecipeCalculationsPublicDTO,
  styleCompliance: StyleComplianceDTO,
  recommendations: List[String],
  warnings: List[String],
  improvements: List[ImprovementSuggestionDTO]
)

object RecipeAnalysisResponseDTO {
  implicit val format: Format[RecipeAnalysisResponseDTO] = Json.format[RecipeAnalysisResponseDTO]
}

case class StyleComplianceDTO(
  style: String,
  overallScore: Double,
  abvCompliance: ComplianceItemDTO,
  ibuCompliance: ComplianceItemDTO,
  srmCompliance: ComplianceItemDTO,
  ogCompliance: ComplianceItemDTO
)

object StyleComplianceDTO {
  implicit val format: Format[StyleComplianceDTO] = Json.format[StyleComplianceDTO]
}

case class ComplianceItemDTO(
  inRange: Boolean,
  value: Double,
  expectedMin: Double,
  expectedMax: Double,
  score: Double
)

object ComplianceItemDTO {
  implicit val format: Format[ComplianceItemDTO] = Json.format[ComplianceItemDTO]
}

case class ImprovementSuggestionDTO(
  category: String,
  suggestion: String,
  impact: String,
  difficulty: String
)

object ImprovementSuggestionDTO {
  implicit val format: Format[ImprovementSuggestionDTO] = Json.format[ImprovementSuggestionDTO]
}

// ============================================================================
// STATISTICS DTOs
// ============================================================================

/**
 * DTO pour statistiques publiques
 */
case class RecipePublicStatsDTO(
  totalRecipes: Int,
  totalStyles: Int,
  averageAbv: Double,
  averageIbu: Double,
  popularStyles: List[PopularStyleDTO],
  popularIngredients: PopularIngredientsDTO,
  trends: TrendStatsDTO,
  communityMetrics: CommunityMetricsDTO
)

object RecipePublicStatsDTO {
  implicit val format: Format[RecipePublicStatsDTO] = Json.format[RecipePublicStatsDTO]
}

case class PopularStyleDTO(
  style: String,
  count: Int,
  percentage: Double,
  averageAbv: Double
)

object PopularStyleDTO {
  implicit val format: Format[PopularStyleDTO] = Json.format[PopularStyleDTO]
}

case class PopularIngredientsDTO(
  hops: List[PopularIngredientDTO],
  malts: List[PopularIngredientDTO],
  yeasts: List[PopularIngredientDTO]
)

object PopularIngredientsDTO {
  implicit val format: Format[PopularIngredientsDTO] = Json.format[PopularIngredientsDTO]
}

case class PopularIngredientDTO(
  name: String,
  usage: Int,
  percentage: Double
)

object PopularIngredientDTO {
  implicit val format: Format[PopularIngredientDTO] = Json.format[PopularIngredientDTO]
}

case class TrendStatsDTO(
  trending: List[String],
  seasonal: List[String],
  emerging: List[String]
)

object TrendStatsDTO {
  implicit val format: Format[TrendStatsDTO] = Json.format[TrendStatsDTO]
}

case class CommunityMetricsDTO(
  averageRating: Double,
  totalRatings: Int,
  activeBrewers: Int,
  monthlyGrowth: Double
)

object CommunityMetricsDTO {
  implicit val format: Format[CommunityMetricsDTO] = Json.format[CommunityMetricsDTO]
}

// ============================================================================
// HEALTH DTOs
// ============================================================================

/**
 * DTO pour santé du service
 */
case class RecipeServiceHealthDTO(
  status: String,
  version: String,
  features: List[String],
  statistics: ServiceStatsDTO,
  algorithms: AlgorithmInfoDTO,
  lastUpdated: Instant
)

object RecipeServiceHealthDTO {
  implicit val format: Format[RecipeServiceHealthDTO] = Json.format[RecipeServiceHealthDTO]
}

case class ServiceStatsDTO(
  totalRequests: Long,
  averageResponseTime: Double,
  cacheHitRate: Double,
  errorRate: Double,
  activeFilters: Int
)

object ServiceStatsDTO {
  implicit val format: Format[ServiceStatsDTO] = Json.format[ServiceStatsDTO]
}

case class AlgorithmInfoDTO(
  recommendationVersions: Map[String, String],
  lastUpdated: Instant
)

object AlgorithmInfoDTO {
  implicit val format: Format[AlgorithmInfoDTO] = Json.format[AlgorithmInfoDTO]
}

// ============================================================================
// UTILITY DTOs
// ============================================================================

/**
 * DTO pour similitudes dans comparaisons
 */
case class SimilarityDTO(
  aspect: String,
  description: String,
  score: Double
)

object SimilarityDTO {
  implicit val format: Format[SimilarityDTO] = Json.format[SimilarityDTO]
}

/**
 * DTO pour différences dans comparaisons
 */
case class DifferenceDTO(
  aspect: String,
  recipe1Value: String,
  recipe2Value: String,
  significance: String,
  impact: String
)

object DifferenceDTO {
  implicit val format: Format[DifferenceDTO] = Json.format[DifferenceDTO]
}

/**
 * DTO pour substitutions
 */
case class SubstitutionDTO(
  originalIngredient: String,
  substituteIngredient: String,
  ratio: Double,
  adjustments: List[String],
  confidence: String
)

object SubstitutionDTO {
  implicit val format: Format[SubstitutionDTO] = Json.format[SubstitutionDTO]
}

// ============================================================================
// OBJECT PRINCIPAL AVEC TOUS LES FORMATS
// ============================================================================

object RecipePublicDTOs {
  // Response DTOs
  implicit val recipePublicFormat: Format[RecipePublicDTO] = RecipePublicDTO.format
  implicit val recipeDetailFormat: Format[RecipeDetailResponseDTO] = RecipeDetailResponseDTO.format
  implicit val searchResponseFormat: Format[RecipeSearchResponseDTO] = RecipeSearchResponseDTO.format
  implicit val discoveryResponseFormat: Format[RecipeDiscoveryResponseDTO] = RecipeDiscoveryResponseDTO.format
  
  // Recommendation DTOs
  implicit val recommendationFormat: Format[RecipeRecommendationDTO] = RecipeRecommendationDTO.format
  implicit val recommendationListFormat: Format[RecipeRecommendationListResponseDTO] = RecipeRecommendationListResponseDTO.format
  implicit val recommendationMetadataFormat: Format[RecommendationMetadataDTO] = RecommendationMetadataDTO.format
  
  // Comparison DTOs
  implicit val comparisonFormat: Format[RecipeComparisonDTO] = RecipeComparisonDTO.format
  implicit val scalingResponseFormat: Format[RecipeScalingResponseDTO] = RecipeScalingResponseDTO.format
  
  // Ingredient DTOs
  implicit val ingredientsFormat: Format[RecipeIngredientsDTO] = RecipeIngredientsDTO.format
  implicit val substitutionIngredientFormat: Format[IngredientSubstitutionDTO] = IngredientSubstitutionDTO.format
  implicit val alternativeIngredientFormat: Format[AlternativeIngredientDTO] = AlternativeIngredientDTO.format
  
  // Calculation DTOs
  implicit val calculationsPublicFormat: Format[RecipeCalculationsPublicDTO] = RecipeCalculationsPublicDTO.format
  implicit val advancedMetricsFormat: Format[AdvancedMetricsDTO] = AdvancedMetricsDTO.format
  
  // Characteristic DTOs
  implicit val characteristicsFormat: Format[RecipeCharacteristicsDTO] = RecipeCharacteristicsDTO.format
  implicit val colorFormat: Format[ColorCharacteristicDTO] = ColorCharacteristicDTO.format
  implicit val bitternessFormat: Format[BitternessCharacteristicDTO] = BitternessCharacteristicDTO.format
  implicit val strengthFormat: Format[StrengthCharacteristicDTO] = StrengthCharacteristicDTO.format
  implicit val aromaFormat: Format[AromaProfileDTO] = AromaProfileDTO.format
  implicit val mouthfeelFormat: Format[MouthfeelProfileDTO] = MouthfeelProfileDTO.format
  
  // Time DTOs
  implicit val estimatedTimeFormat: Format[EstimatedTimeDTO] = EstimatedTimeDTO.format
  implicit val timeBreakdownFormat: Format[TimeBreakdownDTO] = TimeBreakdownDTO.format
  
  // Brewing Guide DTOs
  implicit val brewingGuideFormat: Format[BrewingGuideDTO] = BrewingGuideDTO.format
  implicit val brewingStepFormat: Format[BrewingStepDTO] = BrewingStepDTO.format
  implicit val brewingTimelineFormat: Format[BrewingTimelineDTO] = BrewingTimelineDTO.format
  implicit val timelineEventFormat: Format[TimelineEventDTO] = TimelineEventDTO.format
  implicit val brewingTipFormat: Format[BrewingTipDTO] = BrewingTipDTO.format
  implicit val troubleshootingFormat: Format[TroubleshootingDTO] = TroubleshootingDTO.format
  
  // Nutritional DTOs
  implicit val nutritionalFormat: Format[NutritionalInfoDTO] = NutritionalInfoDTO.format
  
  // Facet DTOs
  implicit val facetsFormat: Format[RecipeFacetsDTO] = RecipeFacetsDTO.format
  implicit val facetItemFormat: Format[FacetItemDTO] = FacetItemDTO.format
  
  // Collection DTOs
  implicit val collectionFormat: Format[RecipeCollectionDTO] = RecipeCollectionDTO.format
  implicit val collectionListFormat: Format[RecipeCollectionListResponseDTO] = RecipeCollectionListResponseDTO.format
  
  // Analysis DTOs
  implicit val analysisRequestFormat: Format[CustomRecipeAnalysisRequestDTO] = CustomRecipeAnalysisRequestDTO.format
  implicit val analysisResponseFormat: Format[RecipeAnalysisResponseDTO] = RecipeAnalysisResponseDTO.format
  implicit val styleComplianceFormat: Format[StyleComplianceDTO] = StyleComplianceDTO.format
  implicit val complianceItemFormat: Format[ComplianceItemDTO] = ComplianceItemDTO.format
  implicit val improvementSuggestionFormat: Format[ImprovementSuggestionDTO] = ImprovementSuggestionDTO.format
  
  // Statistics DTOs
  implicit val publicStatsFormat: Format[RecipePublicStatsDTO] = RecipePublicStatsDTO.format
  implicit val popularStyleFormat: Format[PopularStyleDTO] = PopularStyleDTO.format
  implicit val popularIngredientsFormat: Format[PopularIngredientsDTO] = PopularIngredientsDTO.format
  implicit val popularIngredientFormat: Format[PopularIngredientDTO] = PopularIngredientDTO.format
  implicit val trendStatsFormat: Format[TrendStatsDTO] = TrendStatsDTO.format
  implicit val communityMetricsFormat: Format[CommunityMetricsDTO] = CommunityMetricsDTO.format
  
  // Health DTOs
  implicit val serviceHealthFormat: Format[RecipeServiceHealthDTO] = RecipeServiceHealthDTO.format
  implicit val serviceStatsFormat: Format[ServiceStatsDTO] = ServiceStatsDTO.format
  implicit val algorithmInfoFormat: Format[AlgorithmInfoDTO] = AlgorithmInfoDTO.format
  
  // Utility DTOs
  implicit val similarityFormat: Format[SimilarityDTO] = SimilarityDTO.format
  implicit val differenceFormat: Format[DifferenceDTO] = DifferenceDTO.format
  implicit val substitutionFormat: Format[SubstitutionDTO] = SubstitutionDTO.format
}