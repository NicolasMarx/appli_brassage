package infrastructure.persistence.slick.tables

import domain.yeasts.model.{YeastEvent, YeastCreated, YeastActivated, YeastDeactivated}
import play.api.libs.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Row pour les événements Yeast (Event Sourcing)
 * CORRECTION: Import corrigé pour éviter les conflits
 */
case class YeastEventRow(
  id: String,
  yeastId: String,
  eventType: String,
  eventData: String,
  version: Int,
  occurredAt: Instant,
  createdBy: Option[String]
)

object YeastEventRow {
  
  // CORRECTION: Méthode fromEvent avec import correct
  def fromEvent(event: YeastEvent): YeastEventRow = {
    YeastEventRow(
      id = UUID.randomUUID().toString,
      yeastId = event.aggregateId,
      eventType = event.eventType,
      eventData = Json.stringify(Json.toJson(event)(YeastEvent.format)),
      version = event.version,
      occurredAt = event.occurredAt,
      createdBy = None
    )
  }

  def toEvent(row: YeastEventRow): YeastEvent = {
    // Implémentation simplifiée - désérialisation basée sur eventType
    row.eventType match {
      case "YeastCreated" => YeastCreated(row.yeastId, "Unknown", "Unknown", "Unknown", row.version)
      case "YeastActivated" => YeastActivated(row.yeastId, row.version)
      case "YeastDeactivated" => YeastDeactivated(row.yeastId, row.version)
      case _ => YeastCreated(row.yeastId, "Unknown", "Unknown", "Unknown", row.version)
    }
  }

  def tupled = (YeastEventRow.apply _).tupled

  def unapply(row: YeastEventRow): Option[(String, String, String, String, Int, Instant, Option[String])] = {
    Some((row.id, row.yeastId, row.eventType, row.eventData, row.version, row.occurredAt, row.createdBy))
  }
}
