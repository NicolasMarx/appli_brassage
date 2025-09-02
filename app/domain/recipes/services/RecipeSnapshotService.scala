package domain.recipes.services

import domain.recipes.model.RecipeAggregate
import infrastructure.persistence.slick.tables.{RecipeSnapshotRow, RecipeSnapshotsTable}
import slick.jdbc.PostgresProfile.api._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID
import slick.jdbc.SetParameter

/**
 * Service pour la gestion des snapshots Recipe
 * Optimise les performances de reconstruction d'agrégats en Event Sourcing
 * Suit exactement le pattern des autres domaines
 */
@Singleton
class RecipeSnapshotService @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  implicit val instantSetParameter: SetParameter[Instant] = SetParameter { (instant, pp) =>
    pp.setTimestamp(java.sql.Timestamp.from(instant))
  }
  
  private val db = dbConfigProvider.get.db

  /**
   * Sauvegarde un snapshot d'agrégat
   * Replace le snapshot existant s'il y en a un (contrainte UNIQUE sur aggregate_id)
   */
  def saveSnapshot(aggregate: RecipeAggregate): Future[Unit] = {
    val snapshotRow = RecipeSnapshotRow(
      id = UUID.randomUUID().toString,
      aggregateId = aggregate.id.asString,
      aggregateType = "RecipeAggregate",
      aggregateVersion = aggregate.version,
      snapshotData = Json.stringify(aggregate.toJson),
      createdAt = Instant.now()
    )

    // Utilise INSERT ... ON CONFLICT pour remplacer le snapshot existant
    val action = sqlu"""
      INSERT INTO recipe_snapshots (id, aggregate_id, aggregate_type, aggregate_version, snapshot_data, created_at)
      VALUES (${snapshotRow.id}, ${snapshotRow.aggregateId}, ${snapshotRow.aggregateType}, ${snapshotRow.aggregateVersion}, ${snapshotRow.snapshotData}, ${snapshotRow.createdAt})
      ON CONFLICT (aggregate_id) DO UPDATE SET
        id = EXCLUDED.id,
        aggregate_version = EXCLUDED.aggregate_version,
        snapshot_data = EXCLUDED.snapshot_data,
        created_at = EXCLUDED.created_at
    """

    db.run(action).map(_ => ())
  }

  /**
   * Récupère le snapshot le plus récent pour un agrégat
   */
  def getSnapshot(aggregateId: String): Future[Option[RecipeSnapshotRow]] = {
    val query = RecipeSnapshotsTable.recipeSnapshots
      .filter(_.aggregateId === aggregateId)

    db.run(query.result.headOption)
  }

  /**
   * Reconstruit un agrégat à partir de son snapshot et des événements suivants
   * Optimisation majeure pour les agrégats avec beaucoup d'événements
   */
  def rebuildAggregateFromSnapshot(
    aggregateId: String,
    eventStoreService: RecipeEventStoreService
  ): Future[Option[RecipeAggregate]] = {
    getSnapshot(aggregateId).flatMap {
      case Some(snapshot) =>
        // Désérialise le snapshot
        deserializeSnapshot(snapshot) match {
          case Some(aggregateFromSnapshot) =>
            // Récupère les événements depuis la version du snapshot
            eventStoreService.getEventsFromVersion(aggregateId, snapshot.aggregateVersion).map { events =>
              if (events.isEmpty) {
                // Pas d'événements depuis le snapshot, on retourne l'agrégat du snapshot
                Some(aggregateFromSnapshot)
              } else {
                // Applique les événements manquants au snapshot
                val finalAggregate = events.foldLeft(aggregateFromSnapshot) { (aggregate, event) =>
                  aggregate.applyEvent(event)
                }
                Some(finalAggregate)
              }
            }
          case None =>
            // Erreur de désérialisation du snapshot, fallback sur Event Sourcing complet
            eventStoreService.rebuildAggregate(aggregateId)
        }
      case None =>
        // Pas de snapshot, utilise Event Sourcing standard
        eventStoreService.rebuildAggregate(aggregateId)
    }
  }

  /**
   * Détermine si un snapshot doit être créé
   * Politique: créer un snapshot tous les N événements
   */
  def shouldCreateSnapshot(currentVersion: Int, snapshotFrequency: Int = 10): Boolean = {
    currentVersion > 0 && currentVersion % snapshotFrequency == 0
  }

  /**
   * Supprime le snapshot d'un agrégat
   * Utilisé lors de la suppression d'un agrégat
   */
  def deleteSnapshot(aggregateId: String): Future[Unit] = {
    val action = RecipeSnapshotsTable.recipeSnapshots
      .filter(_.aggregateId === aggregateId)
      .delete

    db.run(action).map(_ => ())
  }

  /**
   * Récupère tous les snapshots créés avant une date donnée
   * Utile pour le nettoyage périodique
   */
  def getSnapshotsOlderThan(date: Instant): Future[List[RecipeSnapshotRow]] = {
    val query = RecipeSnapshotsTable.recipeSnapshots
      .filter(_.createdAt < date)

    db.run(query.result).map(_.toList)
  }

  /**
   * Supprime les snapshots anciens
   * Maintenance périodique pour éviter l'accumulation
   */
  def cleanupOldSnapshots(olderThan: Instant): Future[Int] = {
    val action = RecipeSnapshotsTable.recipeSnapshots
      .filter(_.createdAt < olderThan)
      .delete

    db.run(action)
  }

  /**
   * Obtient des statistiques sur les snapshots
   */
  def getSnapshotStats(): Future[SnapshotStats] = {
    val query = for {
      total <- RecipeSnapshotsTable.recipeSnapshots.length.result
      oldest <- RecipeSnapshotsTable.recipeSnapshots.map(_.createdAt).min.result
      newest <- RecipeSnapshotsTable.recipeSnapshots.map(_.createdAt).max.result
      avgVersion <- RecipeSnapshotsTable.recipeSnapshots.map(_.aggregateVersion.asColumnOf[Double]).avg.result
    } yield SnapshotStats(total, oldest, newest, avgVersion)

    db.run(query)
  }

  /**
   * Désérialise un snapshot JSON en agrégat
   * Méthode privée avec gestion d'erreur
   */
  private def deserializeSnapshot(snapshot: RecipeSnapshotRow): Option[RecipeAggregate] = {
    try {
      Json.parse(snapshot.snapshotData).validate[RecipeAggregate] match {
        case play.api.libs.json.JsSuccess(aggregate, _) => Some(aggregate)
        case play.api.libs.json.JsError(_) => None
      }
    } catch {
      case _: Exception => None
    }
  }
}

/**
 * Statistiques des snapshots
 */
case class SnapshotStats(
  totalSnapshots: Int,
  oldestSnapshot: Option[Instant],
  newestSnapshot: Option[Instant],
  averageVersion: Option[Double]
)