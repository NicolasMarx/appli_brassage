package domain.yeasts.repositories

import domain.yeasts.model._
import domain.yeasts.model.YeastEvent._
import scala.concurrent.Future

/**
 * Repository d'écriture pour les levures (CQRS - Command side)
 * Interface optimisée pour les modifications et Event Sourcing
 */
trait YeastWriteRepository {
  
  /**
   * Crée une nouvelle levure
   */
  def create(yeast: YeastAggregate): Future[YeastAggregate]
  
  /**
   * Met à jour une levure existante
   */
  def update(yeast: YeastAggregate): Future[YeastAggregate]
  
  /**
   * Archive une levure (soft delete)
   */
  def archive(yeastId: YeastId, reason: Option[String]): Future[Unit]
  
  /**
   * Active une levure
   */
  def activate(yeastId: YeastId): Future[Unit]
  
  /**
   * Désactive une levure
   */
  def deactivate(yeastId: YeastId, reason: Option[String]): Future[Unit]
  
  /**
   * Change le statut d'une levure
   */
  def changeStatus(
    yeastId: YeastId, 
    newStatus: YeastStatus, 
    reason: Option[String] = None
  ): Future[Unit]
  
  /**
   * Sauvegarde les événements (Event Sourcing)
   */
  def saveEvents(yeastId: YeastId, events: List[YeastEvent], expectedVersion: Long): Future[Unit]
  
  /**
   * Récupère tous les événements d'une levure
   */
  def getEvents(yeastId: YeastId): Future[List[YeastEvent]]
  
  /**
   * Récupère les événements depuis une version
   */
  def getEventsSinceVersion(yeastId: YeastId, sinceVersion: Long): Future[List[YeastEvent]]
  
  /**
   * Opération batch : créer plusieurs levures
   */
  def createBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]]
  
  /**
   * Opération batch : mettre à jour plusieurs levures
   */
  def updateBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]]
}
