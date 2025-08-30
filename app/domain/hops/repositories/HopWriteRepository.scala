package domain.hops.repositories

import scala.concurrent.Future
import domain.hops.model.{HopAggregate, HopId}

/**
 * Repository pour les opérations d'écriture sur les houblons
 * Suit le pattern CQRS avec séparation Read/Write
 */
trait HopWriteRepository {

  /**
   * Sauvegarde un houblon (insert ou update)
   */
  def save(hop: HopAggregate): Future[HopAggregate]

  /**
   * Sauvegarde une liste de houblons en lot
   */
  def saveAll(hops: List[HopAggregate]): Future[List[HopAggregate]]

  /**
   * Met à jour un houblon existant
   */
  def update(hop: HopAggregate): Future[Option[HopAggregate]]

  /**
   * Supprime un houblon par son ID
   */
  def delete(id: HopId): Future[Boolean]

  /**
   * Vérifie si un houblon existe
   */
  def exists(id: HopId): Future[Boolean]
}