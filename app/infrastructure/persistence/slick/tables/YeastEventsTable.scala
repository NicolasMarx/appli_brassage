package infrastructure.persistence.slick.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, TableQuery}
import java.time.Instant

/**
 * Table Slick pour les événements Yeast
 * CORRECTION: Création de la table manquante
 */
class YeastEventsTable(tag: Tag) extends Table[YeastEventRow](tag, "yeast_events") {
  
  def id = column[String]("id", O.PrimaryKey)
  def yeastId = column[String]("yeast_id")
  def eventType = column[String]("event_type")
  def eventData = column[String]("event_data")
  def version = column[Int]("version")
  def occurredAt = column[Instant]("occurred_at")
  def createdBy = column[Option[String]]("created_by")

  // CORRECTION: Projection correcte avec parenthèses
  def * : ProvenShape[YeastEventRow] = (id, yeastId, eventType, eventData, version, occurredAt, createdBy) <> (YeastEventRow.tupled, YeastEventRow.unapply)
  
  // Index pour optimiser les requêtes
  def idxYeastId = index("idx_yeast_events_yeast_id", yeastId)
  def idxEventType = index("idx_yeast_events_type", eventType)
  def idxOccurredAt = index("idx_yeast_events_occurred_at", occurredAt)
}

object YeastEventsTable {
  // CORRECTION: Propriété yeastEvents manquante
  val yeastEvents = TableQuery[YeastEventsTable]
}
