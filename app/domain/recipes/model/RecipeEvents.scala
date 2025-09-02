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
    }
  )
}