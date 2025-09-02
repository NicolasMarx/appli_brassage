package interfaces.controllers.recipes

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

import domain.recipes.model._
import domain.recipes.repositories.RecipeRepository
import application.recipes.dtos._
import infrastructure.auth.SimpleAuth

@Singleton
class RecipeAdminController @Inject()(
  cc: ControllerComponents,
  auth: SimpleAuth,
  recipeRepository: RecipeRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // READ - Liste basique avec repository réel
  def list(page: Int = 0, size: Int = 20): Action[AnyContent] = auth.secure {
    Action.async { request =>
      val filter = RecipeFilter(page = page, size = size)
      recipeRepository.findByFilter(filter).map { result =>
        val recipes = result.items.map(aggregate => convertAggregateToDTO(aggregate))
        val response = RecipeListResponseDTO(
          recipes = recipes,
          totalCount = result.totalCount.toInt,
          page = result.page,
          size = result.size,
          hasNext = result.hasNext
        )
        Ok(Json.toJson(response))
      }.recover {
        case ex =>
          InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération: ${ex.getMessage}"))
      }
    }
  }

  // READ - Détail basique avec repository réel
  def get(recipeId: String): Action[AnyContent] = auth.secure {
    Action.async { request =>
      RecipeId.fromString(recipeId) match {
        case Right(id) =>
          recipeRepository.findById(id).map {
            case Some(aggregate) => Ok(Json.toJson(convertAggregateToDTO(aggregate)))
            case None => NotFound(Json.obj("error" -> "Recette non trouvée"))
          }.recover {
            case ex =>
              InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération: ${ex.getMessage}"))
          }
        case Left(_) =>
          Future.successful(BadRequest(Json.obj("error" -> "ID invalide")))
      }
    }
  }

  // CREATE - Basique
  def create(): Action[JsValue] = auth.secure {
    Action.async(parse.json) { request =>
      val mockRecipe = createMockRecipe()
      Future.successful(Created(Json.toJson(mockRecipe)))
    }
  }

  // DELETE - Basique
  def delete(recipeId: String): Action[AnyContent] = auth.secure {
    Action.async { request =>
      Future.successful(Ok(Json.obj("message" -> "Recette supprimée (mock)")))
    }
  }

  // STATUS - Health check
  def health(): Action[AnyContent] = auth.secure {
    Action { request =>
      Ok(Json.obj(
        "status" -> "healthy",
        "service" -> "RecipeAdmin",
        "version" -> "prototype",
        "timestamp" -> System.currentTimeMillis()
      ))
    }
  }

  // Conversion RecipeAggregate -> RecipeResponseDTO
  private def convertAggregateToDTO(aggregate: RecipeAggregate): RecipeResponseDTO = {
    RecipeResponseDTO(
      id = aggregate.id.value.toString,
      name = aggregate.name.value,
      description = aggregate.description.map(_.value),
      style = BeerStyleDTO(
        aggregate.style.id, 
        aggregate.style.name, 
        aggregate.style.category
      ),
      batchSize = BatchSizeDTO(
        aggregate.batchSize.value,
        aggregate.batchSize.unit.name,
        aggregate.batchSize.toLiters
      ),
      status = aggregate.status.name,
      hops = aggregate.hops.map(hop => RecipeHopDTO(
        hop.hopId.value.toString,
        hop.quantityGrams,
        hop.additionTime,
        hop.usage.name
      )),
      malts = aggregate.malts.map(malt => RecipeMaltDTO(
        malt.maltId.value.toString,
        malt.quantityKg,
        malt.percentage
      )),
      yeast = aggregate.yeast.map(yeast => RecipeYeastDTO(
        yeast.yeastId.value.toString,
        yeast.quantityGrams,
        yeast.starterSize
      )),
      otherIngredients = aggregate.otherIngredients.map(ingredient => 
        OtherIngredientDTO(
          ingredient.name,
          ingredient.quantityGrams,
          ingredient.additionTime,
          ingredient.ingredientType.value
        )
      ),
      calculations = RecipeCalculationsDTO(
        originalGravity = aggregate.calculations.originalGravity,
        finalGravity = aggregate.calculations.finalGravity,
        abv = aggregate.calculations.abv,
        ibu = aggregate.calculations.ibu,
        srm = aggregate.calculations.srm,
        efficiency = aggregate.calculations.efficiency,
        calculatedAt = aggregate.calculations.calculatedAt
      ),
      procedures = BrewingProceduresDTO(
        hasMashProfile = aggregate.procedures.hasMashProfile,
        hasBoilProcedure = aggregate.procedures.hasBoilProcedure,
        hasFermentationProfile = aggregate.procedures.hasFermentationProfile,
        hasPackagingProcedure = aggregate.procedures.hasPackagingProcedure
      ),
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      createdBy = aggregate.createdBy.value.toString,
      version = aggregate.version
    )
  }

  // Mock data pour les tests (gardé pour compatibilité tests existants)
  private def createMockRecipe(id: String = UUID.randomUUID().toString): RecipeResponseDTO = {
    RecipeResponseDTO(
      id = id,
      name = "American IPA",
      description = Some("Une IPA houblonnée avec des notes d'agrumes"),
      style = BeerStyleDTO("21A", "American IPA", "IPA"),
      batchSize = BatchSizeDTO(20.0, "LITERS", 20.0),
      status = "DRAFT",
      hops = List(
        RecipeHopDTO("cascade", 50.0, 60, "BOIL"),
        RecipeHopDTO("centennial", 30.0, 10, "AROMA")
      ),
      malts = List(
        RecipeMaltDTO("pale-ale", 4.5, Some(85.0)),
        RecipeMaltDTO("crystal-40", 0.5, Some(15.0))
      ),
      yeast = Some(RecipeYeastDTO("us-05", 11.5, None)),
      otherIngredients = List.empty,
      calculations = RecipeCalculationsDTO(
        originalGravity = Some(1.060),
        finalGravity = Some(1.010),
        abv = Some(6.5),
        ibu = Some(45.0),
        srm = Some(8.0),
        efficiency = Some(75.0),
        calculatedAt = Some(Instant.now())
      ),
      procedures = BrewingProceduresDTO(
        hasMashProfile = true,
        hasBoilProcedure = true,
        hasFermentationProfile = true,
        hasPackagingProcedure = false
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      createdBy = UUID.randomUUID().toString,
      version = 1
    )
  }

  private def createMockRecipes(): Seq[RecipeResponseDTO] = {
    Seq(
      createMockRecipe(),
      createMockRecipe().copy(
        name = "Wheat Beer",
        style = BeerStyleDTO("1D", "American Wheat Beer", "Standard American Beer"),
        status = "PUBLISHED"
      ),
      createMockRecipe().copy(
        name = "Stout Imperial",
        style = BeerStyleDTO("20B", "American Stout", "American Porter and Stout"),
        status = "ARCHIVED"
      )
    )
  }
}