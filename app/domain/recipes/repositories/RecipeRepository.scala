package domain.recipes.repositories

import domain.recipes.model._
import scala.concurrent.Future

/**
 * Repository combiné pour les recettes
 * Suit exactement le pattern YeastRepository pour cohérence architecturale
 */
trait RecipeRepository extends RecipeReadRepository with RecipeWriteRepository {
  
  /**
   * Vérification d'existence d'une recette
   */
  def exists(recipeId: RecipeId): Future[Boolean]
}