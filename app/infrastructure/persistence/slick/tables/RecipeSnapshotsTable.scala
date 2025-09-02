package infrastructure.persistence.slick.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, TableQuery}
import java.time.Instant
import java.util.UUID

/**
 * Table Slick pour les snapshots Recipe (Event Sourcing optimization)
 * Suit exactement la structure définie dans migration 12.sql
 */
case class RecipeSnapshotRow(
  id: String,
  aggregateId: String,
  aggregateType: String,
  aggregateVersion: Int,
  snapshotData: String,
  createdAt: Instant
)

class RecipeSnapshotsTable(tag: Tag) extends Table[RecipeSnapshotRow](tag, "recipe_snapshots") {
  
  def id = column[String]("id", O.PrimaryKey)
  def aggregateId = column[String]("aggregate_id")
  def aggregateType = column[String]("aggregate_type")
  def aggregateVersion = column[Int]("aggregate_version")
  def snapshotData = column[String]("snapshot_data")
  def createdAt = column[Instant]("created_at")

  // Projection pour mapping avec RecipeSnapshotRow
  def * : ProvenShape[RecipeSnapshotRow] = (
    id, 
    aggregateId, 
    aggregateType, 
    aggregateVersion, 
    snapshotData, 
    createdAt
  ) <> (RecipeSnapshotRow.tupled, RecipeSnapshotRow.unapply)
  
  // Index pour optimiser les requêtes (suivant migration 12.sql)
  def idxAggregateId = index("idx_recipe_snapshots_aggregate_id", aggregateId)
  def idxAggregateVersion = index("idx_recipe_snapshots_aggregate_version", aggregateVersion)
  def idxCreatedAt = index("idx_recipe_snapshots_created_at", createdAt)
  
  // Contrainte d'unicité par agregat (un seul snapshot par agrégat)
  def uqAggregateId = index("uq_recipe_snapshots_aggregate_id", aggregateId, unique = true)
}

object RecipeSnapshotsTable {
  val recipeSnapshots = TableQuery[RecipeSnapshotsTable]
}

object RecipeSnapshotRow {
  def tupled = (RecipeSnapshotRow.apply _).tupled
  
  def unapply(row: RecipeSnapshotRow): Option[(String, String, String, Int, String, Instant)] = {
    Some((row.id, row.aggregateId, row.aggregateType, row.aggregateVersion, row.snapshotData, row.createdAt))
  }
}