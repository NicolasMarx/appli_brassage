package infrastructure.persistence.slick.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, TableQuery}
import java.time.Instant

/**
 * Table Slick pour les événements Recipe
 * Suit exactement le pattern YeastEventsTable pour cohérence
 */
class RecipeEventsTable(tag: Tag) extends Table[RecipeEventRow](tag, "recipe_events") {
  
  def id = column[String]("id", O.PrimaryKey)
  def aggregateId = column[String]("aggregate_id")
  def aggregateType = column[String]("aggregate_type")
  def eventType = column[String]("event_type")
  def eventVersion = column[Int]("event_version")
  def eventData = column[String]("event_data")
  def metadata = column[String]("metadata")
  def sequenceNumber = column[Long]("sequence_number", O.AutoInc)
  def occurredAt = column[Instant]("occurred_at")
  def createdAt = column[Instant]("created_at")

  // Projection pour mapping avec RecipeEventRow (adaptée pour schéma 12.sql)
  def * : ProvenShape[RecipeEventRow] = (
    id, 
    aggregateId, 
    eventType, 
    eventData, 
    eventVersion, 
    occurredAt, 
    LiteralColumn(None: Option[String]) // createdBy - mappé depuis structure mais pas depuis table
  ) <> (
    {
      case (id, aggregateId, eventType, eventData, eventVersion, occurredAt, createdBy) =>
        RecipeEventRow(id, aggregateId, eventType, eventData, eventVersion, occurredAt, createdBy)
    },
    {
      (r: RecipeEventRow) => Some((r.id, r.recipeId, r.eventType, r.eventData, r.version, r.occurredAt, r.createdBy))
    }
  )
  
  // Index pour optimiser les requêtes (suivant migration 12.sql)
  def idxAggregateId = index("idx_recipe_events_aggregate_id", aggregateId)
  def idxAggregateType = index("idx_recipe_events_aggregate_type", aggregateType)
  def idxEventType = index("idx_recipe_events_event_type", eventType)
  def idxOccurredAt = index("idx_recipe_events_occurred_at", occurredAt)
  def idxSequenceNumber = index("idx_recipe_events_sequence_number", sequenceNumber)
}

object RecipeEventsTable {
  val recipeEvents = TableQuery[RecipeEventsTable]
}