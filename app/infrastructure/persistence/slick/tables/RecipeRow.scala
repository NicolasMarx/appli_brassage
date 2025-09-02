package infrastructure.persistence.slick.tables

import domain.recipes.model._
import play.api.libs.json._
import java.time.Instant
import java.util.UUID

/**
 * Row class pour mapping Slick Recipes
 * Suit exactement le pattern YeastRow pour cohérence architecturale
 */
case class RecipeRow(
  id: UUID,
  name: String,
  description: Option[String],
  styleId: String,
  styleName: String,
  styleCategory: String,
  batchSizeValue: Double,
  batchSizeUnit: String,
  batchSizeLiters: Double,
  hops: String, // JSON serialized
  malts: String, // JSON serialized
  yeast: Option[String], // JSON serialized
  otherIngredients: String, // JSON serialized
  originalGravity: Option[Double],
  finalGravity: Option[Double],
  abv: Option[Double],
  ibu: Option[Double],
  srm: Option[Double],
  efficiency: Option[Double],
  calculationsAt: Option[Instant],
  hasMashProfile: Boolean,
  hasBoilProcedure: Boolean,
  hasFermentationProfile: Boolean,
  hasPackagingProcedure: Boolean,
  status: String,
  createdBy: UUID,
  version: Int,
  createdAt: Instant,
  updatedAt: Instant
)

object RecipeRow {
  
  /**
   * Conversion RecipeAggregate -> RecipeRow
   * Suit exactement le pattern YeastRow.fromAggregate
   */
  def fromAggregate(recipe: RecipeAggregate): RecipeRow = {
    RecipeRow(
      id = recipe.id.value,
      name = recipe.name.value,
      description = recipe.description.map(_.value),
      styleId = recipe.style.id,
      styleName = recipe.style.name,
      styleCategory = recipe.style.category,
      batchSizeValue = recipe.batchSize.value,
      batchSizeUnit = recipe.batchSize.unit.name,
      batchSizeLiters = recipe.batchSize.toLiters,
      hops = Json.toJson(recipe.hops.map(recipeHopToJson)).toString(),
      malts = Json.toJson(recipe.malts.map(recipeMaltToJson)).toString(),
      yeast = recipe.yeast.map(yeast => Json.toJson(recipeYeastToJson(yeast)).toString()),
      otherIngredients = Json.toJson(recipe.otherIngredients.map(ingredientToJson)).toString(),
      originalGravity = recipe.calculations.originalGravity,
      finalGravity = recipe.calculations.finalGravity,
      abv = recipe.calculations.abv,
      ibu = recipe.calculations.ibu,
      srm = recipe.calculations.srm,
      efficiency = recipe.calculations.efficiency,
      calculationsAt = recipe.calculations.calculatedAt,
      hasMashProfile = recipe.procedures.hasMashProfile,
      hasBoilProcedure = recipe.procedures.hasBoilProcedure,
      hasFermentationProfile = recipe.procedures.hasFermentationProfile,
      hasPackagingProcedure = recipe.procedures.hasPackagingProcedure,
      status = recipe.status.name,
      createdBy = recipe.createdBy.value,
      version = recipe.version,
      createdAt = recipe.createdAt,
      updatedAt = recipe.updatedAt
    )
  }
  
  /**
   * Conversion RecipeRow -> RecipeAggregate
   * Suit exactement le pattern YeastRow.toAggregate
   */
  def toAggregate(row: RecipeRow): Either[String, RecipeAggregate] = {
    for {
      recipeId <- Right(RecipeId(row.id))
      recipeName <- RecipeName.create(row.name)
      description <- Right(row.description.flatMap(d => if (d.trim.nonEmpty) Some(RecipeDescription.unsafe(d)) else None))
      beerStyle <- Right(BeerStyle(row.styleId, row.styleName, row.styleCategory))
      volumeUnit <- VolumeUnit.fromName(row.batchSizeUnit)
        .toRight(s"Unité de volume inconnue: ${row.batchSizeUnit}")
      batchSize <- BatchSize.create(row.batchSizeValue, volumeUnit)
      hops <- parseHops(row.hops)
      malts <- parseMalts(row.malts)
      yeast <- parseYeast(row.yeast)
      otherIngredients <- parseOtherIngredients(row.otherIngredients)
      calculations <- Right(RecipeCalculations(
        originalGravity = row.originalGravity,
        finalGravity = row.finalGravity,
        abv = row.abv,
        ibu = row.ibu,
        srm = row.srm,
        efficiency = row.efficiency,
        calculatedAt = row.calculationsAt
      ))
      procedures <- Right(BrewingProcedures(
        mashProfile = if (row.hasMashProfile) Some(MashProfile(60.0, 60, MashType.SingleInfusion)) else None,
        boilProcedure = if (row.hasBoilProcedure) Some(BoilProcedure(60)) else None,
        fermentationProfile = if (row.hasFermentationProfile) Some(FermentationProfile(18.0, 14)) else None,
        packagingProcedure = if (row.hasPackagingProcedure) Some(PackagingProcedure(PackagingType.Bottle)) else None
      ))
      status <- RecipeStatus.fromName(row.status)
        .toRight(s"Statut inconnu: ${row.status}")
      createdBy <- Right(UserId(row.createdBy))
    } yield {
      RecipeAggregate(
        id = recipeId,
        name = recipeName,
        description = description,
        style = beerStyle,
        batchSize = batchSize,
        hops = hops,
        malts = malts,
        yeast = yeast,
        otherIngredients = otherIngredients,
        calculations = calculations,
        procedures = procedures,
        status = status,
        createdBy = createdBy,
        aggregateVersion = 1, // Default version
        createdAt = row.createdAt,
        updatedAt = row.updatedAt
      )
    }
  }
  
  // ==========================================================================
  // MÉTHODES PRIVÉES DE PARSING JSON
  // ==========================================================================
  
  private def parseHops(json: String): Either[String, List[RecipeHop]] = {
    try {
      val parsed = Json.parse(json)
      val hopsResult = parsed.as[List[JsValue]].map { hopJson =>
        for {
          hopId <- (hopJson \ "hopId").asOpt[String].toRight("hopId manquant")
          quantityGrams <- (hopJson \ "quantityGrams").asOpt[Double].toRight("quantityGrams manquant")
          additionTime <- (hopJson \ "additionTime").asOpt[Int].toRight("additionTime manquant")
          usage <- (hopJson \ "usage").asOpt[String].toRight("usage manquant")
          hopUsage <- HopUsage.fromName(usage).toRight(s"Usage inconnu: $usage")
        } yield {
          RecipeHop(domain.hops.model.HopId.unsafe(hopId), quantityGrams, additionTime, hopUsage)
        }
      }
      
      // Convertir List[Either[String, RecipeHop]] -> Either[String, List[RecipeHop]]
      val (errors, hops) = hopsResult.partitionMap(identity)
      if (errors.nonEmpty) Left(s"Erreurs parsing hops: ${errors.mkString(", ")}")
      else Right(hops)
    } catch {
      case ex: Exception => Left(s"Impossible de parser les hops: ${ex.getMessage}")
    }
  }
  
  private def parseMalts(json: String): Either[String, List[RecipeMalt]] = {
    try {
      val parsed = Json.parse(json)
      val maltsResult = parsed.as[List[JsValue]].map { maltJson =>
        for {
          maltId <- (maltJson \ "maltId").asOpt[String].toRight("maltId manquant")
          quantityKg <- (maltJson \ "quantityKg").asOpt[Double].toRight("quantityKg manquant")
          percentage <- Right((maltJson \ "percentage").asOpt[Double])
        } yield {
          RecipeMalt(domain.malts.model.MaltId.unsafe(maltId), quantityKg, percentage)
        }
      }
      
      val (errors, malts) = maltsResult.partitionMap(identity)
      if (errors.nonEmpty) Left(s"Erreurs parsing malts: ${errors.mkString(", ")}")
      else Right(malts)
    } catch {
      case ex: Exception => Left(s"Impossible de parser les malts: ${ex.getMessage}")
    }
  }
  
  private def parseYeast(jsonOpt: Option[String]): Either[String, Option[RecipeYeast]] = {
    jsonOpt match {
      case None => Right(None)
      case Some(json) => 
        try {
          val parsed = Json.parse(json)
          for {
            yeastId <- (parsed \ "yeastId").asOpt[String].toRight("yeastId manquant")
            quantityGrams <- (parsed \ "quantityGrams").asOpt[Double].toRight("quantityGrams manquant")
            starterSize <- Right((parsed \ "starterSize").asOpt[Double])
          } yield {
            Some(RecipeYeast(domain.yeasts.model.YeastId.unsafe(yeastId), quantityGrams, starterSize))
          }
        } catch {
          case ex: Exception => Left(s"Impossible de parser la levure: ${ex.getMessage}")
        }
    }
  }
  
  private def parseOtherIngredients(json: String): Either[String, List[OtherIngredient]] = {
    try {
      val parsed = Json.parse(json)
      val ingredientsResult = parsed.as[List[JsValue]].map { ingredientJson =>
        for {
          name <- (ingredientJson \ "name").asOpt[String].toRight("name manquant")
          quantityGrams <- (ingredientJson \ "quantityGrams").asOpt[Double].toRight("quantityGrams manquant")
          additionTime <- (ingredientJson \ "additionTime").asOpt[Int].toRight("additionTime manquant")
          ingredientType <- (ingredientJson \ "ingredientType").asOpt[String].toRight("ingredientType manquant")
          otherIngredientType <- OtherIngredientType.fromString(ingredientType)
        } yield {
          OtherIngredient(name, quantityGrams, additionTime, otherIngredientType)
        }
      }
      
      val (errors, ingredients) = ingredientsResult.partitionMap(identity)
      if (errors.nonEmpty) Left(s"Erreurs parsing autres ingrédients: ${errors.mkString(", ")}")
      else Right(ingredients)
    } catch {
      case ex: Exception => Left(s"Impossible de parser les autres ingrédients: ${ex.getMessage}")
    }
  }
  
  // ==========================================================================
  // MÉTHODES PRIVÉES DE SÉRIALISATION JSON
  // ==========================================================================
  
  private def recipeHopToJson(hop: RecipeHop): JsValue = {
    Json.obj(
      "hopId" -> hop.hopId.value,
      "quantityGrams" -> hop.quantityGrams,
      "additionTime" -> hop.additionTime,
      "usage" -> hop.usage.name
    )
  }
  
  private def recipeMaltToJson(malt: RecipeMalt): JsValue = {
    Json.obj(
      "maltId" -> malt.maltId.value,
      "quantityKg" -> malt.quantityKg,
      "percentage" -> malt.percentage
    )
  }
  
  private def recipeYeastToJson(yeast: RecipeYeast): JsValue = {
    Json.obj(
      "yeastId" -> yeast.yeastId.value,
      "quantityGrams" -> yeast.quantityGrams,
      "starterSize" -> yeast.starterSize
    )
  }
  
  private def ingredientToJson(ingredient: OtherIngredient): JsValue = {
    Json.obj(
      "name" -> ingredient.name,
      "quantityGrams" -> ingredient.quantityGrams,
      "additionTime" -> ingredient.additionTime,
      "ingredientType" -> ingredient.ingredientType.value
    )
  }
}