package domain.recipes.services

import domain.recipes.model._
import infrastructure.persistence.slick.tables.{RecipeEventRow, RecipeEventsTable}
import slick.jdbc.PostgresProfile.api._
import play.api.db.slick.DatabaseConfigProvider
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

/**
 * Service pour la persistance et la reconstruction des événements Recipe
 * Suit exactement le pattern EventStore des autres domaines (Yeast)
 */
@Singleton
class RecipeEventStoreService @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val db = dbConfigProvider.get.db

  /**
   * Sauvegarde une liste d'événements pour un agrégat
   * Transactionnel - tous les événements sont sauvés ou aucun
   */
  def saveEvents(aggregateId: String, events: List[RecipeEvent], expectedVersion: Int): Future[Unit] = {
    if (events.isEmpty) {
      Future.successful(())
    } else {
      val eventRows = events.map(RecipeEventRow.fromEvent)
      
      val action = for {
        // Vérification de la version pour éviter les conflits de concurrence
        currentVersion <- RecipeEventsTable.recipeEvents
          .filter(_.aggregateId === aggregateId)
          .map(_.eventVersion)
          .max
          .result
          .map(_.getOrElse(0))
        
        _ <- if (currentVersion != expectedVersion) {
          DBIO.failed(new IllegalStateException(s"Version conflict: expected $expectedVersion, got $currentVersion"))
        } else {
          DBIO.successful(())
        }
        
        // Insertion des nouveaux événements
        _ <- RecipeEventsTable.recipeEvents ++= eventRows
      } yield ()
      
      db.run(action.transactionally)
    }
  }

  /**
   * Récupère tous les événements pour un agrégat donné, ordonnés par sequence_number
   */
  def getEvents(aggregateId: String): Future[List[RecipeEvent]] = {
    val query = RecipeEventsTable.recipeEvents
      .filter(_.aggregateId === aggregateId)
      .sortBy(_.sequenceNumber)
    
    db.run(query.result).flatMap { rows =>
      val eventResults = rows.map(RecipeEventRow.toEvent).toList
      
      // Séparer les succès des erreurs
      val (errors, events) = eventResults.foldLeft((List.empty[String], List.empty[RecipeEvent])) {
        case ((errs, evts), Left(error)) => (error :: errs, evts)
        case ((errs, evts), Right(event)) => (errs, event :: evts)
      }
      
      if (errors.nonEmpty) {
        Future.failed(new RuntimeException(s"Erreurs lors de la désérialisation des événements: ${errors.mkString(", ")}"))
      } else {
        Future.successful(events.reverse) // Reverse car on construit la liste à l'envers
      }
    }
  }

  /**
   * Récupère les événements depuis une version donnée
   * Utile pour les projections et les handlers d'événements
   */
  def getEventsFromVersion(aggregateId: String, fromVersion: Int): Future[List[RecipeEvent]] = {
    val query = RecipeEventsTable.recipeEvents
      .filter(r => r.aggregateId === aggregateId && r.eventVersion > fromVersion)
      .sortBy(_.sequenceNumber)
    
    db.run(query.result).flatMap { rows =>
      val eventResults = rows.map(RecipeEventRow.toEvent).toList
      
      val (errors, events) = eventResults.foldLeft((List.empty[String], List.empty[RecipeEvent])) {
        case ((errs, evts), Left(error)) => (error :: errs, evts)
        case ((errs, evts), Right(event)) => (errs, event :: evts)
      }
      
      if (errors.nonEmpty) {
        Future.failed(new RuntimeException(s"Erreurs lors de la désérialisation des événements: ${errors.mkString(", ")}"))
      } else {
        Future.successful(events.reverse)
      }
    }
  }

  /**
   * Reconstruit un agrégat à partir de ses événements
   * Pattern Event Sourcing - replay des événements sur l'agrégat
   */
  def rebuildAggregate(aggregateId: String): Future[Option[RecipeAggregate]] = {
    getEvents(aggregateId).map { events =>
      if (events.isEmpty) {
        None
      } else {
        // Récupère le premier événement qui doit être RecipeCreated
        events.headOption.flatMap {
          case created: RecipeCreated =>
            // Reconstruit l'état initial de l'agrégat
            val initialState = createInitialAggregateFromCreatedEvent(created)
            
            // Applique tous les événements suivants
            val finalState = events.tail.foldLeft(initialState) { (aggregate, event) =>
              aggregate.applyEvent(event)
            }
            
            Some(finalState)
          case _ =>
            // Le premier événement n'est pas RecipeCreated, agrégat corrompu
            None
        }
      }
    }
  }

  /**
   * Récupère la version actuelle d'un agrégat
   */
  def getCurrentVersion(aggregateId: String): Future[Int] = {
    val query = RecipeEventsTable.recipeEvents
      .filter(_.aggregateId === aggregateId)
      .map(_.eventVersion)
      .max
    
    db.run(query.result).map(_.getOrElse(0))
  }

  /**
   * Vérifie si un agrégat existe (a au moins un événement)
   */
  def aggregateExists(aggregateId: String): Future[Boolean] = {
    val query = RecipeEventsTable.recipeEvents
      .filter(_.aggregateId === aggregateId)
      .exists
    
    db.run(query.result)
  }

  /**
   * Récupère les événements dans une plage de temps
   * Utile pour les audits et les analyses
   */
  def getEventsBetween(from: Instant, to: Instant): Future[List[RecipeEvent]] = {
    val query = RecipeEventsTable.recipeEvents
      .filter(r => r.occurredAt >= from && r.occurredAt <= to)
      .sortBy(_.occurredAt)
    
    db.run(query.result).flatMap { rows =>
      val eventResults = rows.map(RecipeEventRow.toEvent).toList
      
      val (errors, events) = eventResults.foldLeft((List.empty[String], List.empty[RecipeEvent])) {
        case ((errs, evts), Left(error)) => (error :: errs, evts)
        case ((errs, evts), Right(event)) => (errs, event :: evts)
      }
      
      if (errors.nonEmpty) {
        Future.failed(new RuntimeException(s"Erreurs lors de la désérialisation des événements: ${errors.mkString(", ")}"))
      } else {
        Future.successful(events.reverse)
      }
    }
  }

  /**
   * Crée l'état initial d'un agrégat à partir de l'événement RecipeCreated
   * Méthode privée pour la reconstruction d'agrégat
   */
  private def createInitialAggregateFromCreatedEvent(created: RecipeCreated): RecipeAggregate = {
    // Note: Cette implémentation nécessite d'avoir accès aux factories des value objects
    // Pour l'instant, on utilise des valeurs par défaut et on laisse applyEvent faire le travail
    val now = created.occurredAt
    RecipeAggregate(
      id = RecipeId.unsafe(created.recipeId),
      name = RecipeName.unsafe(created.name),
      description = created.description.map(RecipeDescription.unsafe),
      style = BeerStyle(created.styleId, created.styleName, created.styleCategory),
      batchSize = BatchSize.create(created.batchSizeValue, VolumeUnit.fromString(created.batchSizeUnit).getOrElse(VolumeUnit.Liters)).getOrElse(BatchSize.create(20.0, VolumeUnit.Liters).right.get),
      status = RecipeStatus.Draft,
      hops = List.empty,
      malts = List.empty,
      yeast = None,
      otherIngredients = List.empty,
      calculations = RecipeCalculations.empty,
      procedures = BrewingProcedures.empty,
      createdAt = now,
      updatedAt = now,
      createdBy = UserId.unsafe(created.createdBy),
      aggregateVersion = created.version
    )
  }
}