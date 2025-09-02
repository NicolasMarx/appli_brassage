package application.recipes.dtos

import play.api.libs.json._
import domain.recipes.model._
import java.time.Instant

case class RecipeResponseDTO(
  id: String,
  name: String,
  description: Option[String],
  style: BeerStyleDTO,
  batchSize: BatchSizeDTO,
  status: String,
  hops: List[RecipeHopDTO],
  malts: List[RecipeMaltDTO],
  yeast: Option[RecipeYeastDTO],
  otherIngredients: List[OtherIngredientDTO],
  calculations: RecipeCalculationsDTO,
  procedures: BrewingProceduresDTO,
  createdAt: Instant,
  updatedAt: Instant,
  createdBy: String,
  version: Int
)

object RecipeResponseDTO {
  def fromAggregate(recipe: RecipeAggregate): RecipeResponseDTO = {
    RecipeResponseDTO(
      id = recipe.id.value.toString,
      name = recipe.name.value,
      description = recipe.description.map(_.value),
      style = BeerStyleDTO.fromDomain(recipe.style),
      batchSize = BatchSizeDTO.fromDomain(recipe.batchSize),
      status = recipe.status.value,
      hops = recipe.hops.map(RecipeHopDTO.fromDomain),
      malts = recipe.malts.map(RecipeMaltDTO.fromDomain),
      yeast = recipe.yeast.map(RecipeYeastDTO.fromDomain),
      otherIngredients = recipe.otherIngredients.map(OtherIngredientDTO.fromDomain),
      calculations = RecipeCalculationsDTO.fromDomain(recipe.calculations),
      procedures = BrewingProceduresDTO.fromDomain(recipe.procedures),
      createdAt = recipe.createdAt,
      updatedAt = recipe.updatedAt,
      createdBy = recipe.createdBy.value.toString,
      version = recipe.version
    )
  }
  
  implicit val format: Format[RecipeResponseDTO] = Json.format[RecipeResponseDTO]
}

case class RecipeListResponseDTO(
  recipes: Seq[RecipeResponseDTO],
  totalCount: Long,
  page: Int,
  size: Int,
  hasNext: Boolean
)

object RecipeListResponseDTO {
  implicit val format: Format[RecipeListResponseDTO] = Json.format[RecipeListResponseDTO]
}

case class RecipeSearchRequestDTO(
  name: Option[String] = None,
  style: Option[String] = None,
  status: Option[String] = None,
  createdBy: Option[String] = None,
  minAbv: Option[Double] = None,
  maxAbv: Option[Double] = None,
  minIbu: Option[Double] = None,
  maxIbu: Option[Double] = None,
  minSrm: Option[Double] = None,
  maxSrm: Option[Double] = None,
  hasHop: Option[String] = None,
  hasMalt: Option[String] = None,
  hasYeast: Option[String] = None,
  page: Int = 0,
  size: Int = 20
)

object RecipeSearchRequestDTO {
  implicit val format: Format[RecipeSearchRequestDTO] = Json.format[RecipeSearchRequestDTO]
}

// DTOs pour les sous-entités
case class BeerStyleDTO(
  id: String,
  name: String,
  category: String
)

object BeerStyleDTO {
  def fromDomain(style: BeerStyle): BeerStyleDTO = {
    BeerStyleDTO(style.id, style.name, style.category)
  }
  
  implicit val format: Format[BeerStyleDTO] = Json.format[BeerStyleDTO]
}

case class BatchSizeDTO(
  value: Double,
  unit: String,
  liters: Double
)

object BatchSizeDTO {
  def fromDomain(batchSize: BatchSize): BatchSizeDTO = {
    BatchSizeDTO(
      value = batchSize.value,
      unit = batchSize.unit.value,
      liters = batchSize.toLiters
    )
  }
  
  implicit val format: Format[BatchSizeDTO] = Json.format[BatchSizeDTO]
}

case class RecipeHopDTO(
  hopId: String,
  quantityGrams: Double,
  additionTime: Int,
  usage: String
)

object RecipeHopDTO {
  def fromDomain(hop: RecipeHop): RecipeHopDTO = {
    RecipeHopDTO(
      hopId = hop.hopId.value.toString,
      quantityGrams = hop.quantityGrams,
      additionTime = hop.additionTime,
      usage = hop.usage.value
    )
  }
  
  implicit val format: Format[RecipeHopDTO] = Json.format[RecipeHopDTO]
}

case class RecipeMaltDTO(
  maltId: String,
  quantityKg: Double,
  percentage: Option[Double]
)

object RecipeMaltDTO {
  def fromDomain(malt: RecipeMalt): RecipeMaltDTO = {
    RecipeMaltDTO(
      maltId = malt.maltId.value.toString,
      quantityKg = malt.quantityKg,
      percentage = malt.percentage
    )
  }
  
  implicit val format: Format[RecipeMaltDTO] = Json.format[RecipeMaltDTO]
}

case class RecipeYeastDTO(
  yeastId: String,
  quantityGrams: Double,
  starterSize: Option[Double]
)

object RecipeYeastDTO {
  def fromDomain(yeast: RecipeYeast): RecipeYeastDTO = {
    RecipeYeastDTO(
      yeastId = yeast.yeastId.value.toString,
      quantityGrams = yeast.quantityGrams,
      starterSize = yeast.starterSize
    )
  }
  
  implicit val format: Format[RecipeYeastDTO] = Json.format[RecipeYeastDTO]
}

case class OtherIngredientDTO(
  name: String,
  quantityGrams: Double,
  additionTime: Int,
  ingredientType: String
)

object OtherIngredientDTO {
  def fromDomain(ingredient: OtherIngredient): OtherIngredientDTO = {
    OtherIngredientDTO(
      name = ingredient.name,
      quantityGrams = ingredient.quantityGrams,
      additionTime = ingredient.additionTime,
      ingredientType = ingredient.ingredientType.value
    )
  }
  
  implicit val format: Format[OtherIngredientDTO] = Json.format[OtherIngredientDTO]
}

case class RecipeCalculationsDTO(
  originalGravity: Option[Double],
  finalGravity: Option[Double],
  abv: Option[Double],
  ibu: Option[Double],
  srm: Option[Double],
  efficiency: Option[Double],
  calculatedAt: Option[Instant]
)

object RecipeCalculationsDTO {
  def fromDomain(calculations: RecipeCalculations): RecipeCalculationsDTO = {
    RecipeCalculationsDTO(
      originalGravity = calculations.originalGravity,
      finalGravity = calculations.finalGravity,
      abv = calculations.abv,
      ibu = calculations.ibu,
      srm = calculations.srm,
      efficiency = calculations.efficiency,
      calculatedAt = calculations.calculatedAt
    )
  }
  
  implicit val format: Format[RecipeCalculationsDTO] = Json.format[RecipeCalculationsDTO]
}

case class BrewingProceduresDTO(
  hasMashProfile: Boolean,
  hasBoilProcedure: Boolean,
  hasFermentationProfile: Boolean,
  hasPackagingProcedure: Boolean
)

object BrewingProceduresDTO {
  def fromDomain(procedures: BrewingProcedures): BrewingProceduresDTO = {
    BrewingProceduresDTO(
      hasMashProfile = procedures.mashProfile.isDefined,
      hasBoilProcedure = procedures.boilProcedure.isDefined,
      hasFermentationProfile = procedures.fermentationProfile.isDefined,
      hasPackagingProcedure = procedures.packagingProcedure.isDefined
    )
  }
  
  implicit val format: Format[BrewingProceduresDTO] = Json.format[BrewingProceduresDTO]
}

// Object principal avec tous les formats
object RecipeDTOs {
  implicit val responseFormat: Format[RecipeResponseDTO] = RecipeResponseDTO.format
  implicit val listResponseFormat: Format[RecipeListResponseDTO] = RecipeListResponseDTO.format
  implicit val searchRequestFormat: Format[RecipeSearchRequestDTO] = RecipeSearchRequestDTO.format
  implicit val beerStyleFormat: Format[BeerStyleDTO] = BeerStyleDTO.format
  implicit val batchSizeFormat: Format[BatchSizeDTO] = BatchSizeDTO.format
  implicit val recipeHopFormat: Format[RecipeHopDTO] = RecipeHopDTO.format
  implicit val recipeMaltFormat: Format[RecipeMaltDTO] = RecipeMaltDTO.format
  implicit val recipeYeastFormat: Format[RecipeYeastDTO] = RecipeYeastDTO.format
  implicit val otherIngredientFormat: Format[OtherIngredientDTO] = OtherIngredientDTO.format
  implicit val calculationsFormat: Format[RecipeCalculationsDTO] = RecipeCalculationsDTO.format
  implicit val proceduresFormat: Format[BrewingProceduresDTO] = BrewingProceduresDTO.format
  
  def fromAggregate(recipe: RecipeAggregate): RecipeResponseDTO = {
    RecipeResponseDTO.fromAggregate(recipe)
  }
}