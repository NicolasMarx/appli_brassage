package infrastructure.persistence.slick.tables

import domain.yeasts.model.YeastEvent
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.UUID

/**
 * Table pour Event Sourcing des levures
 */
class YeastEventsTable(tag: Tag) extends Table[YeastEventRow](tag, "yeast_events") {
  
  def id = column[UUID]("id", O.PrimaryKey, O.AutoInc)
  def yeastId = column[UUID]("yeast_id")
  def eventType = column[String]("event_type")
  def eventData = column[String]("event_data") // JSON
  def version = column[Long]("version")
  def occurredAt = column[Instant]("occurred_at")
  def createdBy = column[Option[UUID]]("created_by")
  
  def idxYeastId = index("idx_yeast_events_yeast_id", yeastId)
  def idxYeastIdVersion = index("idx_yeast_events_yeast_id_version", (yeastId, version))
  def idxOccurredAt = index("idx_yeast_events_occurred_at", occurredAt)
  
  def * = (id, yeastId, eventType, eventData, version, occurredAt, createdBy) <> 
    (YeastEventRow.tupled, YeastEventRow.unapply)
}

case class YeastEventRow(
  id: UUID,
  yeastId: UUID,
  eventType: String,
  eventData: String,
  version: Long,
  occurredAt: Instant,
  createdBy: Option[UUID]
)

object YeastEventRow {
  
  def fromEvent(event: YeastEvent): YeastEventRow = {
    val eventType = event.getClass.getSimpleName
    val eventData = Json.toJson(event)(eventWrites).toString()
    val createdBy = extractCreatedBy(event)
    
    YeastEventRow(
      id = UUID.randomUUID(),
      yeastId = event.yeastId.value,
      eventType = eventType,
      eventData = eventData,
      version = event.version,
      occurredAt = event.occurredAt,
      createdBy = createdBy
    )
  }
  
  def toEvent(row: YeastEventRow): Either[String, YeastEvent] = {
    try {
      val json = Json.parse(row.eventData)
      row.eventType match {
        case "YeastCreated" => json.as[YeastEvent.YeastCreated](yeastCreatedReads).asRight
        case "YeastUpdated" => json.as[YeastEvent.YeastUpdated](yeastUpdatedReads).asRight
        case "YeastStatusChanged" => json.as[YeastEvent.YeastStatusChanged](yeastStatusChangedReads).asRight
        case "YeastActivated" => json.as[YeastEvent.YeastActivated](yeastActivatedReads).asRight
        case "YeastDeactivated" => json.as[YeastEvent.YeastDeactivated](yeastDeactivatedReads).asRight
        case "YeastArchived" => json.as[YeastEvent.YeastArchived](yeastArchivedReads).asRight
        case "YeastTechnicalDataUpdated" => json.as[YeastEvent.YeastTechnicalDataUpdated](yeastTechnicalDataUpdatedReads).asRight
        case "YeastLaboratoryInfoUpdated" => json.as[YeastEvent.YeastLaboratoryInfoUpdated](yeastLaboratoryInfoUpdatedReads).asRight
        case unknown => Left(s"Type d'événement inconnu: $unknown")
      }
    } catch {
      case e: Exception => Left(s"Erreur parsing événement: ${e.getMessage}")
    }
  }
  
  private def extractCreatedBy(event: YeastEvent): Option[UUID] = {
    event match {
      case e: YeastEvent.YeastCreated => Some(e.createdBy)
      case e: YeastEvent.YeastUpdated => Some(e.updatedBy)
      case e: YeastEvent.YeastStatusChanged => Some(e.changedBy)
      case e: YeastEvent.YeastActivated => Some(e.activatedBy)
      case e: YeastEvent.YeastDeactivated => Some(e.deactivatedBy)
      case e: YeastEvent.YeastArchived => Some(e.archivedBy)
      case e: YeastEvent.YeastTechnicalDataUpdated => Some(e.updatedBy)
      case e: YeastEvent.YeastLaboratoryInfoUpdated => Some(e.updatedBy)
    }
  }
  
  // JSON Formatters pour les événements
  implicit val yeastIdWrites: Writes[YeastId] = (o: YeastId) => JsString(o.value.toString)
  implicit val yeastIdReads: Reads[YeastId] = (json: JsValue) => json.validate[String].map(s => YeastId(UUID.fromString(s)))
  
  implicit val yeastNameWrites: Writes[YeastName] = (o: YeastName) => JsString(o.value)
  implicit val yeastNameReads: Reads[YeastName] = (json: JsValue) => json.validate[String].flatMap(s => 
    YeastName.fromString(s).fold(e => JsError(e), JsSuccess(_)))
  
  // Continuer avec les autres formatters...
  implicit val eventWrites: Writes[YeastEvent] = new Writes[YeastEvent] {
    def writes(event: YeastEvent): JsValue = event match {
      case e: YeastEvent.YeastCreated => Json.toJson(e)
      case e: YeastEvent.YeastUpdated => Json.toJson(e)
      case e: YeastEvent.YeastStatusChanged => Json.toJson(e)
      case e: YeastEvent.YeastActivated => Json.toJson(e)
      case e: YeastEvent.YeastDeactivated => Json.toJson(e)
      case e: YeastEvent.YeastArchived => Json.toJson(e)
      case e: YeastEvent.YeastTechnicalDataUpdated => Json.toJson(e)
      case e: YeastEvent.YeastLaboratoryInfoUpdated => Json.toJson(e)
    }
  }
  
  // Readers spécifiques (simplifiés pour l'exemple)
  implicit val yeastCreatedReads: Reads[YeastEvent.YeastCreated] = Json.reads[YeastEvent.YeastCreated]
  implicit val yeastUpdatedReads: Reads[YeastEvent.YeastUpdated] = Json.reads[YeastEvent.YeastUpdated]
  implicit val yeastStatusChangedReads: Reads[YeastEvent.YeastStatusChanged] = Json.reads[YeastEvent.YeastStatusChanged]
  implicit val yeastActivatedReads: Reads[YeastEvent.YeastActivated] = Json.reads[YeastEvent.YeastActivated]
  implicit val yeastDeactivatedReads: Reads[YeastEvent.YeastDeactivated] = Json.reads[YeastEvent.YeastDeactivated]
  implicit val yeastArchivedReads: Reads[YeastEvent.YeastArchived] = Json.reads[YeastEvent.YeastArchived]
  implicit val yeastTechnicalDataUpdatedReads: Reads[YeastEvent.YeastTechnicalDataUpdated] = Json.reads[YeastEvent.YeastTechnicalDataUpdated]
  implicit val yeastLaboratoryInfoUpdatedReads: Reads[YeastEvent.YeastLaboratoryInfoUpdated] = Json.reads[YeastEvent.YeastLaboratoryInfoUpdated]
}

object YeastEventsTable {
  val yeastEvents = TableQuery[YeastEventsTable]
}
