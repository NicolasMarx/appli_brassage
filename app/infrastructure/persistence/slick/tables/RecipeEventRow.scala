package infrastructure.persistence.slick.tables

import domain.recipes.model._
import play.api.libs.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Row pour les événements Recipe (Event Sourcing)
 * Suit exactement le pattern YeastEventRow pour cohérence
 */
case class RecipeEventRow(
  id: String,
  recipeId: String,
  eventType: String,
  eventData: String,
  version: Int,
  occurredAt: Instant,
  createdBy: Option[String]
)

object RecipeEventRow {
  
  /**
   * Conversion RecipeEvent -> RecipeEventRow
   * Suit exactement le pattern YeastEventRow.fromEvent
   */
  def fromEvent(event: RecipeEvent): RecipeEventRow = {
    RecipeEventRow(
      id = UUID.randomUUID().toString,
      recipeId = event.aggregateId,
      eventType = event.eventType,
      eventData = Json.stringify(Json.toJson(event)(RecipeEvent.format)),
      version = event.version,
      occurredAt = event.occurredAt,
      createdBy = None
    )
  }

  /**
   * Conversion d'une ligne de base de données vers un événement de domaine
   * Utilise la désérialisation JSON complète pour reconstituer l'événement exact
   */
  def toEvent(row: RecipeEventRow): Either[String, RecipeEvent] = {
    try {
      Json.parse(row.eventData).validate[RecipeEvent](RecipeEvent.format) match {
        case play.api.libs.json.JsSuccess(event, _) => Right(event)
        case play.api.libs.json.JsError(errors) => 
          Left(s"Erreur de désérialisation pour l'événement ${row.eventType}: ${errors.mkString(", ")})")
      }
    } catch {
      case e: Exception => 
        Left(s"Erreur lors du parsing JSON pour l'événement ${row.eventType}: ${e.getMessage}")
    }
  }

  // Méthodes pour Slick mapping (suivant exact pattern YeastEventRow)
  def tupled = (RecipeEventRow.apply _).tupled

  def unapply(row: RecipeEventRow): Option[(String, String, String, String, Int, Instant, Option[String])] = {
    Some((row.id, row.recipeId, row.eventType, row.eventData, row.version, row.occurredAt, row.createdBy))
  }
}