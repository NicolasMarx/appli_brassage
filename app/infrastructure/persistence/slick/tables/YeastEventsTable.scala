package infrastructure.persistence.slick.tables

import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import java.time.Instant

// Version simplifiée sans JSON complexe
case class YeastEventRow(
  id: UUID,
  yeastId: UUID,
  eventType: String,
  eventData: String,
  version: Long,
  occurredAt: Instant,
  createdBy: Option[UUID]
)

class YeastEventsTable(tag: Tag) extends Table[YeastEventRow](tag, "yeast_events") {
  def id = column[UUID]("id", O.PrimaryKey)
  def yeastId = column[UUID]("yeast_id")
  def eventType = column[String]("event_type")
  def eventData = column[String]("event_data")
  def version = column[Long]("version")
  def occurredAt = column[Instant]("occurred_at")
  def createdBy = column[Option[UUID]]("created_by")

  def * = (id, yeastId, eventType, eventData, version, occurredAt, createdBy) <> 
    ((YeastEventRow.apply _).tupled, YeastEventRow.unapply)
}

object YeastEventsTable {
  val table = TableQuery[YeastEventsTable]
}
