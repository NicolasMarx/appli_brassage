package domain.yeasts.repositories

import domain.yeasts.model._
import scala.concurrent.Future

/**
 * Repository de lecture pour les levures (CQRS - Query side)
 * Interface optimisée pour les requêtes et recherches
 */
trait YeastReadRepository {
  
  /**
   * Recherche par identifiant
   */
  def findById(yeastId: YeastId): Future[Option[YeastAggregate]]
  
  /**
   * Recherche par nom exact
   */
  def findByName(name: YeastName): Future[Option[YeastAggregate]]
  
  /**
   * Recherche par laboratoire et souche
   */
  def findByLaboratoryAndStrain(
    laboratory: YeastLaboratory, 
    strain: YeastStrain
  ): Future[Option[YeastAggregate]]
  
  /**
   * Recherche par filtre avec pagination
   */
  def findByFilter(filter: YeastFilter): Future[PaginatedResult[YeastAggregate]]
  
  /**
   * Recherche par type de levure
   */
  def findByType(yeastType: YeastType): Future[List[YeastAggregate]]
  
  /**
   * Recherche par laboratoire
   */
  def findByLaboratory(laboratory: YeastLaboratory): Future[List[YeastAggregate]]
  
  /**
   * Recherche par statut
   */
  def findByStatus(status: YeastStatus): Future[List[YeastAggregate]]
  
  /**
   * Recherche par plage d'atténuation
   */
  def findByAttenuationRange(
    minAttenuation: Int, 
    maxAttenuation: Int
  ): Future[List[YeastAggregate]]
  
  /**
   * Recherche par plage de température
   */
  def findByTemperatureRange(
    minTemp: Int, 
    maxTemp: Int
  ): Future[List[YeastAggregate]]
  
  /**
   * Recherche par caractéristiques aromatiques
   */
  def findByCharacteristics(characteristics: List[String]): Future[List[YeastAggregate]]
  
  /**
   * Recherche full-text dans nom et caractéristiques
   */
  def searchText(query: String): Future[List[YeastAggregate]]
  
  /**
   * Compte total des levures par statut
   */
  def countByStatus: Future[Map[YeastStatus, Long]]
  
  /**
   * Statistiques par laboratoire
   */
  def getStatsByLaboratory: Future[Map[YeastLaboratory, Long]]
  
  /**
   * Statistiques par type
   */
  def getStatsByType: Future[Map[YeastType, Long]]
  
  /**
   * Levures les plus populaires (à implémenter selon métriques usage)
   */
  def findMostPopular(limit: Int = 10): Future[List[YeastAggregate]]
  
  /**
   * Levures ajoutées récemment
   */
  def findRecentlyAdded(limit: Int = 5): Future[List[YeastAggregate]]
}

/**
 * Résultat paginé générique
 */
case class PaginatedResult[T](
  items: List[T],
  totalCount: Long,
  page: Int,
  size: Int
) {
  def hasNextPage: Boolean = (page + 1) * size < totalCount
  def hasPreviousPage: Boolean = page > 0
  def totalPages: Long = math.ceil(totalCount.toDouble / size).toLong
}
