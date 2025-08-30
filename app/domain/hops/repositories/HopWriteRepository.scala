// app/domain/hops/repositories/HopWriteRepository.scala
package domain.hops.repositories

import domain.hops.model.{HopId, HopAggregate}
import scala.concurrent.Future

/**
 * Repository écriture pour les houblons (CQRS Write Side)
 * Interface pure du domaine, sans dépendances techniques
 */
trait HopWriteRepository {

  /**
   * Sauvegarde un houblon (création ou mise à jour)
   */
  def save(hop: HopAggregate): Future[HopAggregate]

  /**
   * Supprime un houblon
   */
  def delete(id: HopId): Future[Boolean]

  /**
   * Sauvegarde plusieurs houblons en une transaction
   */
  def saveAll(hops: List[HopAggregate]): Future[List[HopAggregate]]
}