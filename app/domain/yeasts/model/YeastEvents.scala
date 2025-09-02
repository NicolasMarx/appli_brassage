package domain.yeasts.model

import domain.common.BaseDomainEvent
import play.api.libs.json._
import java.time.Instant

/**
 * Événements de domaine pour l'agrégat YeastAggregate
 * CORRECTION: Fichier unique consolidé sans conflit
 */
sealed trait YeastEvent extends BaseDomainEvent

case class YeastCreated(
  yeastId: String,
  name: String,
  yeastType: String,
  laboratory: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastCreated", version) with YeastEvent

case class YeastBasicInfoUpdated(
  yeastId: String,
  newName: String,
  newStrain: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastBasicInfoUpdated", version) with YeastEvent

case class YeastActivated(
  yeastId: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastActivated", version) with YeastEvent

case class YeastDeactivated(
  yeastId: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastDeactivated", version) with YeastEvent

case class YeastSpecsUpdated(
  yeastId: String,
  newAttenuationRange: String,
  newTemperatureRange: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastSpecsUpdated", version) with YeastEvent

case class YeastStatusChanged(
  yeastId: String,
  newStatus: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastStatusChanged", version) with YeastEvent

case class YeastReviewRequested(
  yeastId: String,
  reviewReason: String,
  requestedBy: String,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastReviewRequested", version) with YeastEvent

case class YeastQualityScoreUpdated(
  yeastId: String,
  previousScore: Double,
  newScore: Double,
  override val version: Int
) extends BaseDomainEvent(yeastId, "YeastQualityScoreUpdated", version) with YeastEvent

/**
 * Objet companion pour la sérialisation JSON
 */
object YeastEvent {
  
  implicit val yeastCreatedFormat: Format[YeastCreated] = Json.format[YeastCreated]
  implicit val yeastBasicInfoUpdatedFormat: Format[YeastBasicInfoUpdated] = Json.format[YeastBasicInfoUpdated]
  implicit val yeastActivatedFormat: Format[YeastActivated] = Json.format[YeastActivated]
  implicit val yeastDeactivatedFormat: Format[YeastDeactivated] = Json.format[YeastDeactivated]
  implicit val yeastSpecsUpdatedFormat: Format[YeastSpecsUpdated] = Json.format[YeastSpecsUpdated]
  implicit val yeastStatusChangedFormat: Format[YeastStatusChanged] = Json.format[YeastStatusChanged]
  implicit val yeastReviewRequestedFormat: Format[YeastReviewRequested] = Json.format[YeastReviewRequested]
  implicit val yeastQualityScoreUpdatedFormat: Format[YeastQualityScoreUpdated] = Json.format[YeastQualityScoreUpdated]

  implicit val format: Format[YeastEvent] = Format(
    Reads { js =>
      (js \ "eventType").asOpt[String] match {
        case Some("YeastCreated") => js.validate[YeastCreated]
        case Some("YeastBasicInfoUpdated") => js.validate[YeastBasicInfoUpdated]
        case Some("YeastActivated") => js.validate[YeastActivated]
        case Some("YeastDeactivated") => js.validate[YeastDeactivated]
        case Some("YeastSpecsUpdated") => js.validate[YeastSpecsUpdated]
        case Some("YeastStatusChanged") => js.validate[YeastStatusChanged]
        case Some("YeastReviewRequested") => js.validate[YeastReviewRequested]
        case Some("YeastQualityScoreUpdated") => js.validate[YeastQualityScoreUpdated]
        case eventType => JsError(s"Type d'événement yeast inconnu: $eventType")
      }
    },
    Writes {
      case e: YeastCreated => Json.toJson(e)
      case e: YeastBasicInfoUpdated => Json.toJson(e)
      case e: YeastActivated => Json.toJson(e)
      case e: YeastDeactivated => Json.toJson(e)
      case e: YeastSpecsUpdated => Json.toJson(e)
      case e: YeastStatusChanged => Json.toJson(e)
      case e: YeastReviewRequested => Json.toJson(e)
      case e: YeastQualityScoreUpdated => Json.toJson(e)
    }
  )
}
