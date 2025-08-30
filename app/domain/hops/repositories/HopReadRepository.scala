// app/domain/hops/repositories/HopReadRepository.scala
package domain.hops.repositories

import domain.hops.model.{HopId, HopAggregate}
import scala.concurrent.Future

/**
 * Repository lecture pour les houblons (CQRS Read Side)
 * Interface pure du domaine, sans dépendances techniques
 */
trait HopReadRepository {

  /**
   * Récupère un houblon par son ID
   */
  def findById(id: HopId): Future[Option[HopAggregate]]

  /**
   * Récupère tous les houblons actifs avec pagination
   */
  def findActiveHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)]

  /**
   * Récupère tous les houblons (y compris inactifs) avec pagination - admin seulement
   */
  def findAllHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)]

  /**
   * Recherche de houblons avec filtres
   */
  def searchHops(
                  name: Option[String] = None,
                  originCode: Option[String] = None,
                  usage: Option[String] = None,
                  minAlphaAcid: Option[Double] = None,
                  maxAlphaAcid: Option[Double] = None,
                  activeOnly: Boolean = true
                ): Future[List[HopAggregate]]

  /**
   * Récupère les houblons par origine
   */
  def findByOrigin(originCode: String, activeOnly: Boolean = true): Future[List[HopAggregate]]

  /**
   * Récupère les houblons par usage
   */
  def findByUsage(usage: String, activeOnly: Boolean = true): Future[List[HopAggregate]]

  /**
   * Récupère les houblons avec un profil aromatique donné
   */
  def findByAromaProfile(aromaId: String, activeOnly: Boolean = true): Future[List[HopAggregate]]

  /**
   * Vérifie si un houblon avec ce nom existe déjà
   */
  def existsByName(name: String): Future[Boolean]
}