package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId, MaltType}
import domain.shared.NonEmptyString
import scala.concurrent.Future

/**
 * Interface repository lecture pour les malts
 * Version complète avec toutes les méthodes requises
 */
trait MaltReadRepository {
  
  // Méthodes existantes
  def findById(id: MaltId): Future[Option[MaltAggregate]]
  def findAll(page: Int, pageSize: Int, activeOnly: Boolean = false): Future[List[MaltAggregate]]
  def count(activeOnly: Boolean = false): Future[Long]
  def findByType(maltType: MaltType): Future[List[MaltAggregate]]
  def findActive(): Future[List[MaltAggregate]]
  def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]]
  
  // NOUVELLES MÉTHODES REQUISES PAR LES HANDLERS
  
  /**
   * Vérifie si un malt avec ce nom existe déjà
   */
  def existsByName(name: NonEmptyString): Future[Boolean]
  
  /**
   * Recherche avec filtres multiples (pour MaltSearchQueryHandler)
   */
  def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None, 
    originCode: Option[String] = None,
    status: Option[String] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[List[MaltAggregate]]
  
  /**
   * Recherche par filtre MaltFilter avec pagination
   */
  def findByFilter(filter: domain.malts.model.MaltFilter): Future[domain.common.PaginatedResult[MaltAggregate]]
}
