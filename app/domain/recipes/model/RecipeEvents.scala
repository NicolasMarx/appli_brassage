package domain.recipes.model

import domain.common.BaseDomainEvent
import play.api.libs.json._
import java.time.Instant

/**
 * Événements de domaine pour l'agrégat RecipeAggregate
 * Suit exactement le pattern du domaine Yeasts pour cohérence architecturale
 */
sealed trait RecipeEvent extends BaseDomainEvent

case class RecipeCreated(
  recipeId: String,
  name: String,
  description: Option[String],
  styleId: String,
  styleName: String,
  styleCategory: String,
  batchSizeValue: Double,
  batchSizeUnit: String,
  createdBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeCreated", version) with RecipeEvent

case class RecipeUpdated(
  recipeId: String,
  name: Option[String] = None,
  description: Option[String] = None,
  styleId: Option[String] = None,
  styleName: Option[String] = None,
  styleCategory: Option[String] = None,
  batchSizeValue: Option[Double] = None,
  batchSizeUnit: Option[String] = None,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeUpdated", version) with RecipeEvent

case class RecipeIngredientAdded(
  recipeId: String,
  ingredientType: String, // "HOP", "MALT", "YEAST", "OTHER"
  ingredientId: String,
  quantity: Double,
  additionalData: Option[String] = None, // JSON pour données spécifiques (temps, température, etc.)
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeIngredientAdded", version) with RecipeEvent

case class RecipeIngredientRemoved(
  recipeId: String,
  ingredientType: String,
  ingredientId: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeIngredientRemoved", version) with RecipeEvent

case class RecipeIngredientUpdated(
  recipeId: String,
  ingredientType: String,
  ingredientId: String,
  oldQuantity: Double,
  newQuantity: Double,
  oldAdditionalData: Option[String] = None,
  newAdditionalData: Option[String] = None,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeIngredientUpdated", version) with RecipeEvent

case class RecipeProcedureUpdated(
  recipeId: String,
  procedureType: String, // "MASH", "BOIL", "FERMENTATION", "PACKAGING"
  procedureData: String, // JSON serialized procedure
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeProcedureUpdated", version) with RecipeEvent

case class RecipeCalculationsUpdated(
  recipeId: String,
  originalGravity: Option[Double] = None,
  finalGravity: Option[Double] = None,
  abv: Option[Double] = None,
  ibu: Option[Double] = None,
  srm: Option[Double] = None,
  efficiency: Option[Double] = None,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeCalculationsUpdated", version) with RecipeEvent

case class RecipeStatusChanged(
  recipeId: String,
  oldStatus: String,
  newStatus: String,
  reason: Option[String] = None,
  changedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeStatusChanged", version) with RecipeEvent

case class RecipePublished(
  recipeId: String,
  publishedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipePublished", version) with RecipeEvent

case class RecipeArchived(
  recipeId: String,
  archivedBy: String,
  reason: Option[String] = None,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeArchived", version) with RecipeEvent

case class RecipeRestored(
  recipeId: String,
  restoredBy: String,
  reason: Option[String] = None,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeRestored", version) with RecipeEvent

case class RecipeDeleted(
  recipeId: String,
  deletedBy: String,
  reason: Option[String] = None,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeDeleted", version) with RecipeEvent

case class RecipeReviewRequested(
  recipeId: String,
  reviewReason: String,
  requestedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeReviewRequested", version) with RecipeEvent

case class RecipeQualityScoreUpdated(
  recipeId: String,
  previousScore: Option[Double],
  newScore: Double,
  evaluatedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeQualityScoreUpdated", version) with RecipeEvent

// ========== NOUVEAUX EVENTS POUR PHASE D ==========

case class RecipeScaled(
  recipeId: String,
  originalBatchSize: Double,
  targetBatchSize: Double,
  scalingFactor: Double,
  scaledIngredients: String, // JSON des ingrédients mis à l'échelle
  calculationContext: String, // JSON du contexte de calcul
  scaledBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeScaled", version) with RecipeEvent

case class BrewingGuideGenerated(
  recipeId: String,
  guideType: String, // "BASIC", "DETAILED", "PROFESSIONAL"
  brewingSteps: String, // JSON de la timeline de brassage
  estimatedDuration: Int, // En minutes
  complexityLevel: String,
  generatedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "BrewingGuideGenerated", version) with RecipeEvent

case class RecipeAnalyzed(
  recipeId: String,
  analysisType: String, // "BALANCE", "STYLE_COMPLIANCE", "FULL_ANALYSIS"
  analysisResults: String, // JSON des résultats d'analyse
  validationStatus: String, // "VALID", "WARNINGS", "ERRORS"
  recommendations: String, // JSON des recommandations
  analyzedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeAnalyzed", version) with RecipeEvent

case class RecipeAlternativesCalculated(
  recipeId: String,
  alternativeType: String, // "INGREDIENT_SUBSTITUTION", "STYLE_VARIATION", "AVAILABILITY"
  alternatives: String, // JSON des alternatives calculées
  reasoning: String, // JSON de la logique de recommandation
  confidenceScore: Double,
  calculatedBy: String,
  override val version: Int
) extends BaseDomainEvent(recipeId, "RecipeAlternativesCalculated", version) with RecipeEvent

/**
 * Objet companion pour la sérialisation JSON
 * Suit exactement le pattern YeastEvent
 */
object RecipeEvent {
  
  implicit val recipeCreatedFormat: Format[RecipeCreated] = Json.format[RecipeCreated]
  implicit val recipeUpdatedFormat: Format[RecipeUpdated] = Json.format[RecipeUpdated]
  implicit val recipeIngredientAddedFormat: Format[RecipeIngredientAdded] = Json.format[RecipeIngredientAdded]
  implicit val recipeIngredientRemovedFormat: Format[RecipeIngredientRemoved] = Json.format[RecipeIngredientRemoved]
  implicit val recipeIngredientUpdatedFormat: Format[RecipeIngredientUpdated] = Json.format[RecipeIngredientUpdated]
  implicit val recipeProcedureUpdatedFormat: Format[RecipeProcedureUpdated] = Json.format[RecipeProcedureUpdated]
  implicit val recipeCalculationsUpdatedFormat: Format[RecipeCalculationsUpdated] = Json.format[RecipeCalculationsUpdated]
  implicit val recipeStatusChangedFormat: Format[RecipeStatusChanged] = Json.format[RecipeStatusChanged]
  implicit val recipePublishedFormat: Format[RecipePublished] = Json.format[RecipePublished]
  implicit val recipeArchivedFormat: Format[RecipeArchived] = Json.format[RecipeArchived]
  implicit val recipeRestoredFormat: Format[RecipeRestored] = Json.format[RecipeRestored]
  implicit val recipeDeletedFormat: Format[RecipeDeleted] = Json.format[RecipeDeleted]
  implicit val recipeReviewRequestedFormat: Format[RecipeReviewRequested] = Json.format[RecipeReviewRequested]
  implicit val recipeQualityScoreUpdatedFormat: Format[RecipeQualityScoreUpdated] = Json.format[RecipeQualityScoreUpdated]
  
  // Nouveaux formats pour PHASE D
  implicit val recipeScaledFormat: Format[RecipeScaled] = Json.format[RecipeScaled]
  implicit val brewingGuideGeneratedFormat: Format[BrewingGuideGenerated] = Json.format[BrewingGuideGenerated]
  implicit val recipeAnalyzedFormat: Format[RecipeAnalyzed] = Json.format[RecipeAnalyzed]
  implicit val recipeAlternativesCalculatedFormat: Format[RecipeAlternativesCalculated] = Json.format[RecipeAlternativesCalculated]

  implicit val format: Format[RecipeEvent] = Format(
    Reads { js =>
      (js \ "eventType").asOpt[String] match {
        case Some("RecipeCreated") => js.validate[RecipeCreated]
        case Some("RecipeUpdated") => js.validate[RecipeUpdated]
        case Some("RecipeIngredientAdded") => js.validate[RecipeIngredientAdded]
        case Some("RecipeIngredientRemoved") => js.validate[RecipeIngredientRemoved]
        case Some("RecipeIngredientUpdated") => js.validate[RecipeIngredientUpdated]
        case Some("RecipeProcedureUpdated") => js.validate[RecipeProcedureUpdated]
        case Some("RecipeCalculationsUpdated") => js.validate[RecipeCalculationsUpdated]
        case Some("RecipeStatusChanged") => js.validate[RecipeStatusChanged]
        case Some("RecipePublished") => js.validate[RecipePublished]
        case Some("RecipeArchived") => js.validate[RecipeArchived]
        case Some("RecipeRestored") => js.validate[RecipeRestored]
        case Some("RecipeDeleted") => js.validate[RecipeDeleted]
        case Some("RecipeReviewRequested") => js.validate[RecipeReviewRequested]
        case Some("RecipeQualityScoreUpdated") => js.validate[RecipeQualityScoreUpdated]
        case Some("RecipeScaled") => js.validate[RecipeScaled]
        case Some("BrewingGuideGenerated") => js.validate[BrewingGuideGenerated]
        case Some("RecipeAnalyzed") => js.validate[RecipeAnalyzed]
        case Some("RecipeAlternativesCalculated") => js.validate[RecipeAlternativesCalculated]
        case eventType => JsError(s"Type d'événement recipe inconnu: $eventType")
      }
    },
    Writes {
      case e: RecipeCreated => Json.toJson(e)
      case e: RecipeUpdated => Json.toJson(e)
      case e: RecipeIngredientAdded => Json.toJson(e)
      case e: RecipeIngredientRemoved => Json.toJson(e)
      case e: RecipeIngredientUpdated => Json.toJson(e)
      case e: RecipeProcedureUpdated => Json.toJson(e)
      case e: RecipeCalculationsUpdated => Json.toJson(e)
      case e: RecipeStatusChanged => Json.toJson(e)
      case e: RecipePublished => Json.toJson(e)
      case e: RecipeArchived => Json.toJson(e)
      case e: RecipeRestored => Json.toJson(e)
      case e: RecipeDeleted => Json.toJson(e)
      case e: RecipeReviewRequested => Json.toJson(e)
      case e: RecipeQualityScoreUpdated => Json.toJson(e)
      case e: RecipeScaled => Json.toJson(e)
      case e: BrewingGuideGenerated => Json.toJson(e)
      case e: RecipeAnalyzed => Json.toJson(e)
      case e: RecipeAlternativesCalculated => Json.toJson(e)
    }
  )
}