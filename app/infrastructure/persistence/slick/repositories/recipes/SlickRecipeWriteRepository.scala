package infrastructure.persistence.slick.repositories.recipes

import domain.recipes.model._
import domain.recipes.repositories.RecipeWriteRepository
import domain.recipes.services.{RecipeEventStoreService, RecipeSnapshotService}
import infrastructure.persistence.slick.tables.{RecipeEventsTable, RecipeEventRow, RecipesTable, RecipeRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

/**
 * Implémentation Slick du repository d'écriture Recipe
 * Suit exactement le pattern SlickYeastWriteRepository pour cohérence
 */
@Singleton
class SlickRecipeWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  recipeEventStoreService: RecipeEventStoreService,
  recipeSnapshotService: RecipeSnapshotService
)(implicit ec: ExecutionContext) 
  extends RecipeWriteRepository 
    with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._

  // Tables (suivant exact pattern YeastWriteRepository)
  private val recipes = RecipesTable.recipes
  private val recipeEvents = RecipeEventsTable.recipeEvents

  override def create(recipe: RecipeAggregate): Future[RecipeAggregate] = {
    val events = recipe.getUncommittedEvents
    
    if (events.nonEmpty) {
      // Approche Event Sourcing: sauvegarder les événements
      recipeEventStoreService.saveEvents(recipe.id.asString, events, 0).flatMap { _ =>
        // Optionnellement sauvegarder dans la table de projection recipes
        val recipeRow = RecipeRow.fromAggregate(recipe)
        db.run((recipes += recipeRow).transactionally).map { _ =>
          recipe.markEventsAsCommitted()
          
          // Créer un snapshot si nécessaire
          if (recipeSnapshotService.shouldCreateSnapshot(recipe.version)) {
            recipeSnapshotService.saveSnapshot(recipe)
          }
          
          recipe
        }
      }
    } else {
      Future.successful(recipe)
    }
  }

  override def update(recipe: RecipeAggregate): Future[RecipeAggregate] = {
    val events = recipe.getUncommittedEvents
    
    if (events.nonEmpty) {
      // Récupérer la version actuelle pour éviter les conflits
      recipeEventStoreService.getCurrentVersion(recipe.id.asString).flatMap { currentVersion =>
        val expectedVersion = recipe.version - events.length
        
        if (currentVersion != expectedVersion) {
          Future.failed(new IllegalStateException(s"Version conflict: expected $expectedVersion, got $currentVersion"))
        } else {
          // Sauvegarder les nouveaux événements
          recipeEventStoreService.saveEvents(recipe.id.asString, events, currentVersion).flatMap { _ =>
            // Mettre à jour la table de projection
            val recipeRow = RecipeRow.fromAggregate(recipe)
            val updateAction = recipes
              .filter(_.id === recipe.id.value)
              .update(recipeRow)
            
            db.run(updateAction.transactionally).map { _ =>
              recipe.markEventsAsCommitted()
              
              // Créer un snapshot si nécessaire
              if (recipeSnapshotService.shouldCreateSnapshot(recipe.version)) {
                recipeSnapshotService.saveSnapshot(recipe)
              }
              
              recipe
            }
          }
        }
      }
    } else {
      Future.successful(recipe)
    }
  }

  override def save(recipe: RecipeAggregate): Future[RecipeAggregate] = {
    // Logique save-or-update (suivant pattern Yeast)
    val existsQuery = recipes.filter(_.id === recipe.id.value).exists
    
    db.run(existsQuery.result).flatMap { exists =>
      if (exists) update(recipe)
      else create(recipe)
    }
  }

  override def delete(recipeId: RecipeId): Future[Unit] = {
    val action = recipes.filter(_.id === recipeId.value).delete
    db.run(action).map(_ => ())
  }

  override def archive(recipeId: RecipeId, reason: Option[String]): Future[Unit] = {
    val action = recipes
      .filter(_.id === recipeId.value)
      .map(_.status)
      .update("ARCHIVED")
    
    db.run(action).map(_ => ())
  }

  override def changeStatus(
    recipeId: RecipeId, 
    newStatus: RecipeStatus, 
    reason: Option[String] = None
  ): Future[Unit] = {
    val action = recipes
      .filter(_.id === recipeId.value)
      .map(_.status)
      .update(newStatus.name)
    
    db.run(action).map(_ => ())
  }

  // Event Sourcing methods - délégués aux services spécialisés
  override def saveEvents(recipeId: RecipeId, events: List[RecipeEvent], expectedVersion: Long): Future[Unit] = {
    recipeEventStoreService.saveEvents(recipeId.asString, events, expectedVersion.toInt)
  }

  override def getEvents(recipeId: RecipeId): Future[List[RecipeEvent]] = {
    recipeEventStoreService.getEvents(recipeId.asString)
  }

  override def getEventsSinceVersion(recipeId: RecipeId, sinceVersion: Long): Future[List[RecipeEvent]] = {
    recipeEventStoreService.getEventsFromVersion(recipeId.asString, sinceVersion.toInt)
  }

  override def createBatch(recipes: List[RecipeAggregate]): Future[List[RecipeAggregate]] = {
    if (recipes.isEmpty) {
      Future.successful(List.empty)
    } else {
      // Pour chaque recette, sauvegarder les événements individuellement
      val eventSaveFutures = recipes.map { recipe =>
        val events = recipe.getUncommittedEvents
        if (events.nonEmpty) {
          recipeEventStoreService.saveEvents(recipe.id.asString, events, 0)
        } else {
          Future.successful(())
        }
      }
      
      Future.sequence(eventSaveFutures).flatMap { _ =>
        // Sauvegarder les projections en batch
        val recipeRows = recipes.map(RecipeRow.fromAggregate)
        db.run((this.recipes ++= recipeRows).transactionally).map { _ =>
          recipes.foreach(_.markEventsAsCommitted())
          
          // Créer des snapshots si nécessaire
          recipes.foreach { recipe =>
            if (recipeSnapshotService.shouldCreateSnapshot(recipe.version)) {
              recipeSnapshotService.saveSnapshot(recipe)
            }
          }
          
          recipes
        }
      }
    }
  }

  override def updateBatch(recipes: List[RecipeAggregate]): Future[List[RecipeAggregate]] = {
    if (recipes.isEmpty) {
      Future.successful(List.empty)
    } else {
      // Pour chaque recette, sauvegarder les événements avec gestion de version
      val updateFutures = recipes.map { recipe =>
        val events = recipe.getUncommittedEvents
        if (events.nonEmpty) {
          recipeEventStoreService.getCurrentVersion(recipe.id.asString).flatMap { currentVersion =>
            val expectedVersion = recipe.version - events.length
            recipeEventStoreService.saveEvents(recipe.id.asString, events, expectedVersion)
          }
        } else {
          Future.successful(())
        }
      }
      
      Future.sequence(updateFutures).map { _ =>
        recipes.foreach(_.markEventsAsCommitted())
        
        // Créer des snapshots si nécessaire
        recipes.foreach { recipe =>
          if (recipeSnapshotService.shouldCreateSnapshot(recipe.version)) {
            recipeSnapshotService.saveSnapshot(recipe)
          }
        }
        
        recipes
      }
    }
  }
}