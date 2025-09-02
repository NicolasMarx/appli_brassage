package domain.recipes.repositories

import domain.recipes.model._
import scala.concurrent.Future

/**
 * Repository d'écriture pour les recettes (CQRS - Command side)
 * Interface optimisée pour les commandes et modifications
 * Suit exactement le pattern YeastWriteRepository pour cohérence
 */
trait RecipeWriteRepository {
  
  /**
   * Création d'une nouvelle recette
   */
  def create(recipe: RecipeAggregate): Future[RecipeAggregate]
  
  /**
   * Mise à jour d'une recette existante
   */
  def update(recipe: RecipeAggregate): Future[RecipeAggregate]
  
  /**
   * Sauvegarde (create ou update selon l'existence)
   */
  def save(recipe: RecipeAggregate): Future[RecipeAggregate]
  
  /**
   * Suppression d'une recette
   */
  def delete(recipeId: RecipeId): Future[Unit]
  
  /**
   * Archivage d'une recette
   */
  def archive(recipeId: RecipeId, reason: Option[String] = None): Future[Unit]
  
  /**
   * Changement de statut
   */
  def changeStatus(recipeId: RecipeId, newStatus: RecipeStatus, reason: Option[String] = None): Future[Unit]
  
  // ==========================================================================
  // EVENT SOURCING
  // ==========================================================================
  
  /**
   * Sauvegarde d'événements
   */
  def saveEvents(recipeId: RecipeId, events: List[RecipeEvent], expectedVersion: Long): Future[Unit]
  
  /**
   * Récupération de tous les événements d'un agrégat
   */
  def getEvents(recipeId: RecipeId): Future[List[RecipeEvent]]
  
  /**
   * Récupération des événements depuis une version
   */
  def getEventsSinceVersion(recipeId: RecipeId, sinceVersion: Long): Future[List[RecipeEvent]]
  
  // ==========================================================================
  // OPÉRATIONS EN LOT
  // ==========================================================================
  
  /**
   * Création en lot
   */
  def createBatch(recipes: List[RecipeAggregate]): Future[List[RecipeAggregate]]
  
  /**
   * Mise à jour en lot
   */
  def updateBatch(recipes: List[RecipeAggregate]): Future[List[RecipeAggregate]]
}