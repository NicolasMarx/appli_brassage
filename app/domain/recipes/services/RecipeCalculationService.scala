package domain.recipes.services

import domain.recipes.model._
import domain.recipes.repositories.{RecipeRepository, RecipeReadRepository}
import domain.common.DomainError
import domain.malts.repositories.MaltReadRepository
import domain.hops.repositories.HopReadRepository
import domain.yeasts.repositories.YeastReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

/**
 * Service de domaine pour l'intégration des calculs de brassage avec les recettes
 * Orchestrate la récupération des données et l'exécution des calculs
 */
@Singleton
class RecipeCalculationService @Inject()(
  brewingCalculator: BrewingCalculatorService,
  waterChemistryCalculator: WaterChemistryCalculator,
  recipeRepository: RecipeRepository,
  recipeReadRepository: RecipeReadRepository,
  maltReadRepository: MaltReadRepository,
  hopReadRepository: HopReadRepository,
  yeastReadRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Recalcule tous les paramètres de brassage pour une recette
   */
  def calculateRecipeBrewingParameters(
    recipe: RecipeAggregate,
    waterProfile: Option[WaterProfile] = None,
    targetWaterProfile: Option[WaterProfile] = None,
    forceRecalculation: Boolean = false
  ): Future[Either[DomainError, RecipeAggregate]] = {
    
    // Construire les paramètres de calcul
    buildCalculationParameters(recipe).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(parameters) =>
        
        val currentHash = generateParametersHash(parameters)
        
        // Vérifier si recalcul nécessaire
        if (!forceRecalculation && !recipe.calculations.needsAdvancedRecalculation(currentHash)) {
          Future.successful(Right(recipe))
        } else {
          performCalculations(recipe, parameters, waterProfile, targetWaterProfile)
        }
    }
  }

  /**
   * Recalcule une recette de manière asynchrone et persiste le résultat
   */
  def calculateAndUpdateRecipe(recipeId: RecipeId): Future[Either[DomainError, RecipeAggregate]] = {
    recipeReadRepository.findById(recipeId).flatMap {
      case None => Future.successful(Left(DomainError.notFound(s"Recette non trouvée: ${recipeId.asString}", "RECIPE_NOT_FOUND")))
      case Some(recipe) =>
        calculateRecipeBrewingParameters(recipe, forceRecalculation = true).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(updatedRecipe) =>
            // Sauvegarder la recette mise à jour
            recipeRepository.save(updatedRecipe).map { _ =>
              Right(updatedRecipe)
            }.recover {
              case ex => Left(DomainError.technical(s"Erreur de sauvegarde: ${ex.getMessage}", ex))
            }
        }
    }
  }

  /**
   * Calcule seulement les paramètres sans persister (pour prévisualisation)
   */
  def previewCalculations(recipe: RecipeAggregate): Future[Either[DomainError, BrewingCalculationResult]] = {
    buildCalculationParameters(recipe).map {
      case Left(error) => Left(error)
      case Right(parameters) =>
        brewingCalculator.calculateRecipe(parameters) match {
          case Left(errors) => Left(DomainError.businessRule(errors.mkString(", "), "CALCULATION_ERROR"))
          case Right(result) => Right(result)
        }
    }
  }

  /**
   * Met à jour uniquement les calculs d'une recette sans modifier les ingrédients
   */
  def updateRecipeCalculations(
    recipe: RecipeAggregate,
    calculations: RecipeCalculations
  ): Either[DomainError, RecipeAggregate] = {
    Right(recipe.copy(
      calculations = calculations,
      updatedAt = java.time.Instant.now(),
      aggregateVersion = recipe.aggregateVersion + 1
    ))
  }

  /**
   * Valide qu'une recette peut être calculée correctement
   */
  def validateRecipeForCalculation(recipe: RecipeAggregate): Future[Either[List[String], Unit]] = {
    buildCalculationParameters(recipe).map {
      case Left(error) => Left(List(error.message))
      case Right(parameters) => 
        val validationErrors = parameters.validateParameters()
        if (validationErrors.isEmpty) Right(()) else Left(validationErrors)
    }
  }

  /**
   * Obtient les recommandations d'amélioration pour une recette
   */
  def getRecipeRecommendations(recipe: RecipeAggregate): Future[Either[DomainError, List[RecipeRecommendation]]] = {
    calculateRecipeBrewingParameters(recipe).map {
      case Left(error) => Left(error)
      case Right(calculatedRecipe) =>
        val recommendations = generateRecommendations(calculatedRecipe)
        Right(recommendations)
    }
  }

  // ========================================
  // MÉTHODES PRIVÉES
  // ========================================

  private def buildCalculationParameters(recipe: RecipeAggregate): Future[Either[DomainError, BrewingCalculationParameters]] = {
    val maltsFuture = Future.traverse(recipe.malts)(malt => 
      maltReadRepository.findById(malt.maltId).map(_.map(m => (malt, m)))
    ).map(_.flatten)

    val hopsFuture = Future.traverse(recipe.hops)(hop =>
      hopReadRepository.findById(hop.hopId).map(_.map(h => (hop, h)))
    ).map(_.flatten)

    val yeastFuture = recipe.yeast.map(yeast =>
      yeastReadRepository.findById(yeast.yeastId).map(_.map(y => (yeast, y)))
    ).getOrElse(Future.successful(None))

    for {
      malts <- maltsFuture
      hops <- hopsFuture  
      yeast <- yeastFuture
    } yield {
      try {
        val maltInputs = malts.map { case (recipeMalt, maltAggregate) =>
          MaltCalculationInput(
            name = maltAggregate.name.value,
            quantityKg = recipeMalt.quantityKg,
            extractionRate = maltAggregate.extractionRate,
            colorEBC = maltAggregate.ebcColor,
            isBaseMalt = maltAggregate.isBaseMalt,
            diastaticPower = maltAggregate.diastaticPower.value,
            percentage = recipeMalt.percentage.getOrElse(0.0)
          )
        }

        val hopInputs = hops.map { case (recipeHop, hopAggregate) =>
          val hopUsage = recipeHop.usage match {
            case domain.recipes.model.HopUsage.Boil => HopUsageType.Boil
            case domain.recipes.model.HopUsage.Aroma => HopUsageType.Aroma
            case domain.recipes.model.HopUsage.DryHop => HopUsageType.DryHop
            case domain.recipes.model.HopUsage.Whirlpool => HopUsageType.Whirlpool
          }
          
          HopCalculationInput(
            name = hopAggregate.name.value,
            quantityGrams = recipeHop.quantityGrams,
            alphaAcidPercent = hopAggregate.alphaAcid,
            boilTimeMins = recipeHop.additionTime,
            hopUsage = hopUsage
          )
        }

        val yeastInput = yeast.map { case (recipeYeast, yeastAggregate) =>
          YeastCalculationInput(
            name = yeastAggregate.name.value,
            attenuationRange = yeastAggregate.attenuationRange,
            alcoholTolerance = yeastAggregate.alcoholTolerance.value
          )
        }

        val parameters = BrewingCalculationParameters(
          batchSizeLiters = recipe.batchSize.value,
          boilTimeMins = 60, // Default, could be configurable
          efficiency = 75.0, // Default, could be user-configurable
          maltIngredients = maltInputs,
          hopIngredients = hopInputs,
          yeastInput = yeastInput
        )

        Right(parameters)

      } catch {
        case ex: Exception =>
          Left(DomainError.technical(s"Erreur construction paramètres: ${ex.getMessage}", ex))
      }
    }
  }

  private def performCalculations(
    recipe: RecipeAggregate,
    parameters: BrewingCalculationParameters,
    waterProfile: Option[WaterProfile],
    targetWaterProfile: Option[WaterProfile]
  ): Future[Either[DomainError, RecipeAggregate]] = {
    
    Future {
      // Calculs de brassage principaux
      brewingCalculator.calculateRecipe(parameters) match {
        case Left(errors) => 
          Left(DomainError.businessRule(errors.mkString(", "), "CALCULATION_ERROR"))
          
        case Right(brewingResult) =>
          // Calculs de chimie de l'eau si profil fourni
          val waterResult = for {
            source <- waterProfile
            target <- targetWaterProfile
          } yield {
            waterChemistryCalculator.calculateWaterProfile(
              source, 
              Some(target), 
              parameters.maltIngredients,
              parameters.batchSizeLiters
            )
          }

          // Mise à jour des calculs de la recette
          val updatedCalculations = recipe.calculations.withAdvancedCalculations(brewingResult, waterResult)
          
          Right(recipe.copy(
            calculations = updatedCalculations,
            updatedAt = java.time.Instant.now(),
            aggregateVersion = recipe.aggregateVersion + 1
          ))
      }
    }
  }

  private def generateRecommendations(recipe: RecipeAggregate): List[RecipeRecommendation] = {
    val recommendations = scala.collection.mutable.ListBuffer[RecipeRecommendation]()
    
    recipe.calculations.brewingCalculationResult.foreach { result =>
      // Recommandations basées sur l'équilibre
      if (!result.isBalanced) {
        if (result.bitternessTogravigtRatio < 0.5) {
          recommendations += RecipeRecommendation(
            category = "Balance",
            message = "La bière semble très maltée. Considérez augmenter les houblons amers.",
            severity = "INFO",
            suggestedAction = Some("Augmenter les ajouts de houblons en début d'ébullition")
          )
        } else if (result.bitternessTogravigtRatio > 1.2) {
          recommendations += RecipeRecommendation(
            category = "Balance", 
            message = "La bière semble très amère. Considérez réduire les houblons ou augmenter les malts.",
            severity = "WARNING",
            suggestedAction = Some("Réduire les houblons amers ou ajouter des malts caramel")
          )
        }
      }

      // Recommandations sur la force
      result.alcoholByVolume.primary match {
        case abv if abv > 12.0 =>
          recommendations += RecipeRecommendation(
            category = "Alcool",
            message = "Taux d'alcool très élevé. Vérifiez la tolérance de la levure.",
            severity = "WARNING",
            suggestedAction = Some("Considérez une levure haute tolérance ou réduire les malts")
          )
        case abv if abv < 2.0 =>
          recommendations += RecipeRecommendation(
            category = "Alcool",
            message = "Taux d'alcool très faible pour une bière.",
            severity = "INFO",
            suggestedAction = Some("Augmenter la quantité de malts de base")
          )
        case _ => // OK
      }

      // Recommandations sur l'efficacité
      if (!result.efficiency.isAcceptable) {
        recommendations += RecipeRecommendation(
          category = "Efficacité",
          message = f"Efficacité théorique ${result.efficiency.calculated}%.1f%% différente de l'attendu.",
          severity = "INFO",
          suggestedAction = Some("Vérifiez vos paramètres d'efficacité ou votre processus de brassage")
        )
      }
    }

    // Recommandations sur la water chemistry si disponible
    recipe.calculations.waterChemistryResult.foreach { waterResult =>
      if (waterResult.warnings.nonEmpty) {
        recommendations += RecipeRecommendation(
          category = "Eau",
          message = "Profil d'eau avec avertissements: " + waterResult.warnings.mkString(", "),
          severity = "INFO",
          suggestedAction = waterResult.mineralAdditions.headOption.map { addition => s"${addition.mineralName}: ${addition.amountGrams}g" }
        )
      }
    }

    recommendations.toList
  }

  private def generateParametersHash(parameters: BrewingCalculationParameters): String = {
    import java.security.MessageDigest
    val content = s"${parameters.batchSizeLiters}_${parameters.efficiency}_${parameters.maltIngredients.map(m => s"${m.name}_${m.quantityKg}").mkString("_")}_${parameters.hopIngredients.map(h => s"${h.name}_${h.quantityGrams}_${h.boilTimeMins}").mkString("_")}"
    MessageDigest.getInstance("MD5").digest(content.getBytes).map("%02x".format(_)).mkString.take(16)
  }
}

/**
 * Recommandation d'amélioration pour une recette
 */
case class RecipeRecommendation(
  category: String,
  message: String,
  severity: String, // INFO, WARNING, ERROR
  suggestedAction: Option[String] = None
) {
  def isWarning: Boolean = severity == "WARNING"
  def isError: Boolean = severity == "ERROR"
}

object RecipeRecommendation {
  implicit val format = play.api.libs.json.Json.format[RecipeRecommendation]
}