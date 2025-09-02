package domain.recipes.repositories

import domain.recipes.model._
import scala.concurrent.Future

/**
 * Repository de lecture pour les recettes (CQRS - Query side)
 * Interface optimisée pour les requêtes et recherches
 * Suit exactement le pattern YeastReadRepository pour cohérence
 */
trait RecipeReadRepository {
  
  /**
   * Recherche par identifiant
   */
  def findById(recipeId: RecipeId): Future[Option[RecipeAggregate]]
  
  /**
   * Recherche par nom exact
   */
  def findByName(name: RecipeName): Future[Option[RecipeAggregate]]
  
  /**
   * Recherche par style de bière
   */
  def findByStyle(styleId: String): Future[List[RecipeAggregate]]
  
  /**
   * Recherche par statut
   */
  def findByStatus(status: RecipeStatus): Future[List[RecipeAggregate]]
  
  /**
   * Recherche par créateur
   */
  def findByCreatedBy(userId: UserId): Future[List[RecipeAggregate]]
  
  /**
   * Recherche paginée avec filtres
   */
  def findByFilter(filter: RecipeFilter): Future[PaginatedResult[RecipeAggregate]]
  
  /**
   * Recherche par taille de lot (en litres)
   */
  def findByBatchSizeRange(minLiters: Double, maxLiters: Double): Future[List[RecipeAggregate]]
  
  /**
   * Recherche par degré d'alcool
   */
  def findByAbvRange(minAbv: Double, maxAbv: Double): Future[List[RecipeAggregate]]
  
  /**
   * Recherche par amertume (IBU)
   */
  def findByIbuRange(minIbu: Double, maxIbu: Double): Future[List[RecipeAggregate]]
  
  /**
   * Recherche par couleur (SRM)
   */
  def findBySrmRange(minSrm: Double, maxSrm: Double): Future[List[RecipeAggregate]]
  
  /**
   * Recherche full-text dans nom et description
   */
  def searchText(query: String): Future[List[RecipeAggregate]]
  
  /**
   * Compte total des recettes par statut
   */
  def countByStatus: Future[Map[RecipeStatus, Long]]
  
  /**
   * Statistiques par style de bière
   */
  def getStatsByStyle: Future[Map[String, Long]]
  
  /**
   * Recettes les plus populaires
   */
  def findMostPopular(limit: Int): Future[List[RecipeAggregate]]
  
  /**
   * Recettes récemment ajoutées
   */
  def findRecentlyAdded(limit: Int): Future[List[RecipeAggregate]]
}

/**
 * Classe pour résultats paginés (copiée exactement de YeastReadRepository)
 */
case class PaginatedResult[T](
  items: List[T],
  totalCount: Long,
  page: Int,
  size: Int
) {
  val hasNext: Boolean = (page + 1) * size < totalCount
  val hasPrevious: Boolean = page > 0
  val totalPages: Int = math.ceil(totalCount.toDouble / size).toInt
}