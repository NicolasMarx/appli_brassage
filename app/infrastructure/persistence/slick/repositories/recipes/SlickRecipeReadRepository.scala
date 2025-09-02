package infrastructure.persistence.slick.repositories.recipes

import domain.recipes.model._
import domain.recipes.repositories.{RecipeReadRepository, PaginatedResult}
import domain.recipes.services.{RecipeEventStoreService, RecipeSnapshotService}
import infrastructure.persistence.slick.tables.{RecipesTable, RecipeRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

/**
 * Implémentation Slick du repository de lecture Recipe
 * Suit exactement le pattern SlickYeastReadRepository pour cohérence
 */
@Singleton
class SlickRecipeReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  recipeEventStoreService: RecipeEventStoreService,
  recipeSnapshotService: RecipeSnapshotService
)(implicit ec: ExecutionContext) 
  extends RecipeReadRepository 
    with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._

  private val recipes = RecipesTable.recipes

  override def findById(id: RecipeId): Future[Option[RecipeAggregate]] = {
    // Stratégie hybride: utiliser snapshots + Event Sourcing si disponible, sinon projection
    recipeSnapshotService.rebuildAggregateFromSnapshot(id.asString, recipeEventStoreService).flatMap {
      case Some(aggregate) => 
        // Reconstruit avec succès depuis Event Sourcing
        Future.successful(Some(aggregate))
      case None =>
        // Fallback sur la table de projection
        val query = recipes.filter(_.id === id.value)
        db.run(query.result.headOption).map(_.map(rowToAggregate))
    }
  }

  // Méthode utilitaire (pas dans l'interface, suivant pattern YeastReadRepository)
  def findAll(page: Int = 0, pageSize: Int = 20): Future[List[RecipeAggregate]] = {
    val offset = page * pageSize
    val query = recipes.sortBy(_.name).drop(offset).take(pageSize)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByName(name: RecipeName): Future[Option[RecipeAggregate]] = {
    val query = recipes.filter(_.name === name.value)
    db.run(query.result.headOption).map(_.map(rowToAggregate))
  }

  // Méthode de compatibilité avec String (suivant pattern Yeast)
  def findByNameString(name: String): Future[Option[RecipeAggregate]] = {
    val query = recipes.filter(_.name === name)
    db.run(query.result.headOption).map(_.map(rowToAggregate))
  }

  override def findByStyle(styleId: String): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.styleId === styleId)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode de compatibilité avec BeerStyle
  def findByBeerStyle(style: BeerStyle): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.styleId === style.id)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByStatus(status: RecipeStatus): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.status === status.name)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode de compatibilité avec String
  def findByStatusString(status: String): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.status === status)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByCreatedBy(userId: UserId): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.createdBy === userId.value)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode utilitaire (pas dans l'interface, suivant pattern Yeast)
  def findActive(): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.status.inSet(List("DRAFT", "PUBLISHED")))
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode utilitaire (pas dans l'interface, suivant pattern Yeast)
  def findPublished(): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(_.status === "PUBLISHED")
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByFilter(filter: RecipeFilter): Future[PaginatedResult[RecipeAggregate]] = {
    
    // Construction de la requête de base (suivant pattern YeastReadRepository)
    var baseQuery = recipes.filter(_ => true) // Start with all
    
    // Filtrage par nom
    filter.name.foreach { name =>
      baseQuery = baseQuery.filter(_.name.toLowerCase like s"%${name.toLowerCase}%")
    }
    
    // Filtrage par style
    filter.style.foreach { style =>
      baseQuery = baseQuery.filter(_.styleCategory === (style: String))
    }
    
    // Filtrage par statut
    filter.status.foreach { status =>
      baseQuery = baseQuery.filter(_.status === status.name)
    }
    
    // Filtrage par créateur
    filter.createdBy.foreach { userId =>
      baseQuery = baseQuery.filter(_.createdBy === userId.value)
    }
    
    val countQuery = baseQuery.length
    val dataQuery = baseQuery
      .sortBy(_.name)
      .drop(filter.page * filter.size)
      .take(filter.size)
      
    for {
      totalCount <- db.run(countQuery.result)
      items <- db.run(dataQuery.result).recover {
        case ex => 
          println(s"Erreur requête recipes: ${ex.getMessage}")
          Seq.empty
      }
    } yield {
      PaginatedResult(
        items = items.map(rowToAggregate).toList,
        totalCount = totalCount.toLong,
        page = filter.page,
        size = filter.size
      )
    }
  }

  // Méthode utilitaire de recherche (suivant pattern Yeast)
  def search(
    query: String,
    styleCategory: Option[String] = None,
    status: Option[String] = None,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[List[RecipeAggregate]] = {
    var baseQuery = recipes.filter(_.name.toLowerCase like s"%${query.toLowerCase}%")

    styleCategory.foreach { category =>
      baseQuery = baseQuery.filter(_.styleCategory === (category: String))
    }

    status.foreach { stat =>
      baseQuery = baseQuery.filter(_.status === stat)
    }

    val paginatedQuery = baseQuery.sortBy(_.name).drop(page * pageSize).take(pageSize)
    db.run(paginatedQuery.result).map(_.map(rowToAggregate).toList)
  }

  override def searchText(query: String): Future[List[RecipeAggregate]] = {
    val searchQuery = recipes.filter { row =>
      row.name.toLowerCase.like(s"%${query.toLowerCase}%") ||
      row.description.toLowerCase.like(s"%${query.toLowerCase}%")
    }
    db.run(searchQuery.result).map(_.map(rowToAggregate).toList)
  }

  override def findByBatchSizeRange(minLiters: Double, maxLiters: Double): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(row =>
      row.batchSizeLiters >= minLiters && row.batchSizeLiters <= maxLiters
    )
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByAbvRange(minAbv: Double, maxAbv: Double): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(row =>
      row.abv.isDefined && row.abv >= minAbv && row.abv <= maxAbv
    )
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByIbuRange(minIbu: Double, maxIbu: Double): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(row =>
      row.ibu.isDefined && row.ibu >= minIbu && row.ibu <= maxIbu
    )
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findBySrmRange(minSrm: Double, maxSrm: Double): Future[List[RecipeAggregate]] = {
    val query = recipes.filter(row =>
      row.srm.isDefined && row.srm >= minSrm && row.srm <= maxSrm
    )
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode utilitaire (pas dans l'interface, suivant pattern Yeast)
  def count(): Future[Long] = {
    db.run(recipes.length.result).map(_.toLong)
  }

  // Méthode utilitaire (pas dans l'interface, suivant pattern Yeast)
  def countByStatus(status: String): Future[Long] = {
    val query = recipes.filter(_.status === status).length
    db.run(query.result).map(_.toLong)
  }

  // Méthode utilitaire (pas dans l'interface, suivant pattern Yeast)
  def existsByName(name: String): Future[Boolean] = {
    val query = recipes.filter(_.name === name).exists
    db.run(query.result)
  }

  override def countByStatus: Future[Map[RecipeStatus, Long]] = {
    val query = recipes.groupBy(_.status).map {
      case (status, group) => (status, group.length)
    }
    db.run(query.result).map { results =>
      results.flatMap { case (statusName, count) =>
        RecipeStatus.fromName(statusName).map(_ -> count.toLong)
      }.toMap
    }
  }

  override def getStatsByStyle: Future[Map[String, Long]] = {
    val query = recipes.groupBy(_.styleCategory).map {
      case (category, group) => (category, group.length)
    }
    db.run(query.result).map(_.toMap.map { case (category, count) => category -> count.toLong })
  }

  override def findMostPopular(limit: Int = 10): Future[List[RecipeAggregate]] = {
    // Implémentation simple - les plus récents publiés (suivant pattern Yeast)
    val query = recipes
      .filter(_.status === "PUBLISHED")
      .sortBy(_.updatedAt.desc)
      .take(limit)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findRecentlyAdded(limit: Int = 5): Future[List[RecipeAggregate]] = {
    val query = recipes
      .sortBy(_.createdAt.desc)
      .take(limit)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  /**
   * Reconstruction complète d'un agrégat depuis Event Sourcing
   * Méthode utilitaire pour forcer la reconstruction depuis les événements
   */
  def rebuildFromEvents(id: RecipeId): Future[Option[RecipeAggregate]] = {
    recipeEventStoreService.rebuildAggregate(id.asString)
  }

  /**
   * Reconstruction depuis snapshot avec optimisation
   * Méthode utilitaire pour reconstruction optimisée
   */
  def rebuildFromSnapshot(id: RecipeId): Future[Option[RecipeAggregate]] = {
    recipeSnapshotService.rebuildAggregateFromSnapshot(id.asString, recipeEventStoreService)
  }

  // Méthode de conversion privée (suivant exact pattern YeastReadRepository)
  private def rowToAggregate(row: RecipeRow): RecipeAggregate = {
    RecipeRow.toAggregate(row) match {
      case Right(aggregate) => aggregate
      case Left(error) => throw new RuntimeException(s"Erreur conversion RecipeRow: $error")
    }
  }
}