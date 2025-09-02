package infrastructure.persistence.slick.repositories.recipes

import domain.recipes.model._
import domain.recipes.repositories.{RecipeRepository, PaginatedResult}
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Repository combiné qui délègue aux implémentations read/write
 * Suit exactement le pattern SlickYeastRepository pour cohérence
 */
@Singleton
class SlickRecipeRepository @Inject()(
  readRepository: SlickRecipeReadRepository,
  writeRepository: SlickRecipeWriteRepository
)(implicit ec: ExecutionContext) extends RecipeRepository {

  // ==========================================================================
  // DÉLÉGATION AUX REPOSITORIES SPÉCIALISÉS
  // ==========================================================================

  // Read operations (suivant exact pattern SlickYeastRepository)
  override def findById(recipeId: RecipeId): Future[Option[RecipeAggregate]] = 
    readRepository.findById(recipeId)

  override def findByName(name: RecipeName): Future[Option[RecipeAggregate]] = 
    readRepository.findByName(name)

  override def findByStyle(styleId: String): Future[List[RecipeAggregate]] = 
    readRepository.findByStyle(styleId)

  override def findByStatus(status: RecipeStatus): Future[List[RecipeAggregate]] = 
    readRepository.findByStatus(status)

  override def findByCreatedBy(userId: UserId): Future[List[RecipeAggregate]] = 
    readRepository.findByCreatedBy(userId)

  override def findByFilter(filter: RecipeFilter): Future[PaginatedResult[RecipeAggregate]] = 
    readRepository.findByFilter(filter)

  override def findByBatchSizeRange(minLiters: Double, maxLiters: Double): Future[List[RecipeAggregate]] = 
    readRepository.findByBatchSizeRange(minLiters, maxLiters)

  override def findByAbvRange(minAbv: Double, maxAbv: Double): Future[List[RecipeAggregate]] = 
    readRepository.findByAbvRange(minAbv, maxAbv)

  override def findByIbuRange(minIbu: Double, maxIbu: Double): Future[List[RecipeAggregate]] = 
    readRepository.findByIbuRange(minIbu, maxIbu)

  override def findBySrmRange(minSrm: Double, maxSrm: Double): Future[List[RecipeAggregate]] = 
    readRepository.findBySrmRange(minSrm, maxSrm)

  override def searchText(query: String): Future[List[RecipeAggregate]] = 
    readRepository.searchText(query)

  override def countByStatus: Future[Map[RecipeStatus, Long]] = 
    readRepository.countByStatus

  override def getStatsByStyle: Future[Map[String, Long]] = 
    readRepository.getStatsByStyle

  override def findMostPopular(limit: Int): Future[List[RecipeAggregate]] = 
    readRepository.findMostPopular(limit)

  override def findRecentlyAdded(limit: Int): Future[List[RecipeAggregate]] = 
    readRepository.findRecentlyAdded(limit)

  // Write operations (suivant exact pattern SlickYeastRepository)
  override def create(recipe: RecipeAggregate): Future[RecipeAggregate] = 
    writeRepository.create(recipe)

  override def update(recipe: RecipeAggregate): Future[RecipeAggregate] = 
    writeRepository.update(recipe)

  override def save(recipe: RecipeAggregate): Future[RecipeAggregate] = 
    writeRepository.save(recipe)

  override def delete(recipeId: RecipeId): Future[Unit] = 
    writeRepository.delete(recipeId)

  override def archive(recipeId: RecipeId, reason: Option[String]): Future[Unit] = 
    writeRepository.archive(recipeId, reason)

  override def changeStatus(recipeId: RecipeId, newStatus: RecipeStatus, reason: Option[String]): Future[Unit] = 
    writeRepository.changeStatus(recipeId, newStatus, reason)

  override def saveEvents(recipeId: RecipeId, events: List[RecipeEvent], expectedVersion: Long): Future[Unit] = 
    writeRepository.saveEvents(recipeId, events, expectedVersion)

  override def getEvents(recipeId: RecipeId): Future[List[RecipeEvent]] = 
    writeRepository.getEvents(recipeId)

  override def getEventsSinceVersion(recipeId: RecipeId, sinceVersion: Long): Future[List[RecipeEvent]] = 
    writeRepository.getEventsSinceVersion(recipeId, sinceVersion)

  override def createBatch(recipes: List[RecipeAggregate]): Future[List[RecipeAggregate]] = 
    writeRepository.createBatch(recipes)

  override def updateBatch(recipes: List[RecipeAggregate]): Future[List[RecipeAggregate]] = 
    writeRepository.updateBatch(recipes)

  // ==========================================================================
  // OPÉRATIONS SPÉCIFIQUES AU REPOSITORY COMBINÉ
  // ==========================================================================

  override def exists(recipeId: RecipeId): Future[Boolean] = {
    findById(recipeId).map(_.isDefined)
  }
}