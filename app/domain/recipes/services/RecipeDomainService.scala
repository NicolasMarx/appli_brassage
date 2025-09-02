package domain.recipes.services

import domain.recipes.model._
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service de domaine principal pour les recettes avec calculs avancés
 * Orchestrate tous les aspects du domaine Recipe incluant les calculs de brassage
 */
@Singleton
class RecipeDomainService @Inject()(
  calculationService: RecipeCalculationService,
  brewingCalculator: BrewingCalculatorService,
  waterChemistryCalculator: WaterChemistryCalculator
)(implicit ec: ExecutionContext) {

  /**
   * Crée une nouvelle recette avec calculs automatiques
   */
  def createRecipeWithCalculations(
    name: RecipeName,
    description: Option[RecipeDescription],
    style: BeerStyle,
    batchSize: BatchSize,
    createdBy: UserId,
    autoCalculate: Boolean = true
  ): Future[Either[DomainError, RecipeAggregate]] = {
    
    val recipe = RecipeAggregate.create(name, description, style, batchSize, createdBy)
    
    if (autoCalculate && recipe.malts.nonEmpty && recipe.hops.nonEmpty) {
      calculationService.calculateRecipeBrewingParameters(recipe).map {
        case Left(error) => Right(recipe) // Retourne la recette même si calcul échoue
        case Right(calculatedRecipe) => Right(calculatedRecipe)
      }
    } else {
      Future.successful(Right(recipe))
    }
  }

  /**
   * Ajoute un ingrédient et recalcule automatiquement si nécessaire
   */
  def addIngredientAndCalculate(
    recipe: RecipeAggregate,
    ingredient: RecipeIngredient,
    autoRecalculate: Boolean = true
  ): Future[Either[DomainError, RecipeAggregate]] = {
    
    val updatedRecipe = ingredient match {
      case hop: RecipeHop => recipe.addHop(hop) match {
        case Left(error) => return Future.successful(Left(DomainError.businessRule(error, "ADD_HOP_ERROR")))
        case Right(updated) => updated
      }
      case malt: RecipeMalt => recipe.addMalt(malt) match {
        case Left(error) => return Future.successful(Left(DomainError.businessRule(error, "ADD_MALT_ERROR")))
        case Right(updated) => updated
      }
      case yeast: RecipeYeast => recipe.setYeast(yeast)
      case other => recipe // Autres ingrédients non supportés pour l'instant
    }
    
    if (autoRecalculate && updatedRecipe.malts.nonEmpty && updatedRecipe.hops.nonEmpty) {
      calculationService.calculateRecipeBrewingParameters(updatedRecipe, forceRecalculation = true)
    } else {
      Future.successful(Right(updatedRecipe))
    }
  }

  /**
   * Optimise une recette pour atteindre des valeurs cibles
   */
  def optimizeRecipe(
    recipe: RecipeAggregate,
    targetOG: Option[Double] = None,
    targetIBU: Option[Double] = None,
    targetSRM: Option[Double] = None,
    targetABV: Option[Double] = None
  ): Future[Either[DomainError, RecipeOptimizationResult]] = {
    
    calculationService.previewCalculations(recipe).map {
      case Left(error) => Left(error)
      case Right(current) =>
        val suggestions = generateOptimizationSuggestions(recipe, current, targetOG, targetIBU, targetSRM, targetABV)
        Right(RecipeOptimizationResult(
          originalCalculations = current,
          targetValues = RecipeTargets(targetOG, targetIBU, targetSRM, targetABV),
          optimizationSuggestions = suggestions,
          feasibilityScore = calculateFeasibilityScore(suggestions)
        ))
    }
  }

  /**
   * Compare deux recettes et génère un rapport de différences
   */
  def compareRecipes(
    recipe1: RecipeAggregate,
    recipe2: RecipeAggregate
  ): Future[Either[DomainError, RecipeComparisonResult]] = {
    
    for {
      calc1Result <- calculationService.previewCalculations(recipe1)
      calc2Result <- calculationService.previewCalculations(recipe2)
    } yield {
      (calc1Result, calc2Result) match {
        case (Right(calc1), Right(calc2)) =>
          Right(RecipeComparisonResult(
            recipe1 = recipe1,
            recipe2 = recipe2,
            calculations1 = calc1,
            calculations2 = calc2,
            differences = calculateDifferences(calc1, calc2),
            similarity = calculateSimilarityScore(recipe1, recipe2)
          ))
        case (Left(error), _) => Left(error)
        case (_, Left(error)) => Left(error)
      }
    }
  }

  /**
   * Scale une recette pour un nouveau volume de batch
   */
  def scaleRecipe(
    recipe: RecipeAggregate,
    newBatchSize: BatchSize,
    maintainRatios: Boolean = true
  ): Future[Either[DomainError, RecipeAggregate]] = {
    
    val scaleFactor = newBatchSize.value / recipe.batchSize.value
    
    try {
      // Scale les ingrédients
      val scaledMalts = recipe.malts.map(malt => 
        malt.copy(quantityKg = malt.quantityKg * scaleFactor)
      )
      
      val scaledHops = recipe.hops.map(hop =>
        hop.copy(quantityGrams = hop.quantityGrams * scaleFactor)
      )
      
      val scaledYeast = recipe.yeast.map(yeast =>
        yeast.copy(quantityGrams = yeast.quantityGrams * scaleFactor)
      )
      
      val scaledOtherIngredients = recipe.otherIngredients.map(other =>
        other.copy(quantityGrams = other.quantityGrams * scaleFactor)
      )
      
      val scaledRecipe = recipe.copy(
        batchSize = newBatchSize,
        malts = scaledMalts,
        hops = scaledHops,
        yeast = scaledYeast,
        otherIngredients = scaledOtherIngredients,
        updatedAt = java.time.Instant.now(),
        aggregateVersion = recipe.aggregateVersion + 1
      )
      
      // Recalculer avec les nouvelles quantités
      calculationService.calculateRecipeBrewingParameters(scaledRecipe, forceRecalculation = true)
      
    } catch {
      case ex: Exception =>
        Future.successful(Left(DomainError.technical(s"Erreur lors du scaling: ${ex.getMessage}", ex)))
    }
  }

  /**
   * Génère un rapport de faisabilité pour une recette
   */
  def generateFeasibilityReport(recipe: RecipeAggregate): Future[Either[DomainError, RecipeFeasibilityReport]] = {
    
    calculationService.validateRecipeForCalculation(recipe).flatMap {
      case Left(errors) =>
        Future.successful(Right(RecipeFeasibilityReport(
          recipe = recipe,
          isFeasible = false,
          validationErrors = errors,
          warnings = List.empty,
          recommendations = List.empty,
          estimatedCost = None,
          estimatedTime = None
        )))
        
      case Right(_) =>
        calculationService.previewCalculations(recipe).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(calculations) =>
            calculationService.getRecipeRecommendations(recipe).map {
              case Left(error) => Left(error)
              case Right(recommendations) =>
                Right(RecipeFeasibilityReport(
                  recipe = recipe,
                  isFeasible = true,
                  validationErrors = List.empty,
                  warnings = calculations.warnings,
                  recommendations = recommendations,
                  estimatedCost = Some(estimateRecipeCost(recipe)),
                  estimatedTime = Some(estimateBrewingTime(recipe))
                ))
            }
        }
    }
  }

  /**
   * Convertit une recette d'extrait vers all-grain ou vice-versa
   */
  def convertRecipeType(
    recipe: RecipeAggregate,
    targetType: RecipeType,
    efficiency: Double = 75.0
  ): Future[Either[DomainError, RecipeAggregate]] = {
    
    targetType match {
      case RecipeType.AllGrain => convertToAllGrain(recipe, efficiency)
      case RecipeType.Extract => convertToExtract(recipe)
      case RecipeType.PartialMash => convertToPartialMash(recipe, efficiency)
    }
  }

  // ========================================
  // MÉTHODES PRIVÉES
  // ========================================

  private def generateOptimizationSuggestions(
    recipe: RecipeAggregate,
    current: BrewingCalculationResult,
    targetOG: Option[Double],
    targetIBU: Option[Double],
    targetSRM: Option[Double],
    targetABV: Option[Double]
  ): List[OptimizationSuggestion] = {
    
    val suggestions = scala.collection.mutable.ListBuffer[OptimizationSuggestion]()
    
    // Suggestions pour OG
    targetOG.foreach { target =>
      val currentOG = current.originalGravity.value
      if (math.abs(currentOG - target) > 0.005) {
        val factor = target / currentOG
        val maltAdjustment = recipe.malts.map(_.quantityKg).sum * (factor - 1)
        suggestions += OptimizationSuggestion(
          parameter = "Original Gravity",
          currentValue = f"$currentOG%.3f",
          targetValue = f"$target%.3f",
          suggestion = f"${if (maltAdjustment > 0) "Ajouter" else "Réduire"} ${math.abs(maltAdjustment)%.2f}kg de malts de base",
          impact = if (math.abs(maltAdjustment) > 1) "HIGH" else "MEDIUM",
          feasibility = "EASY"
        )
      }
    }
    
    // Suggestions pour IBU
    targetIBU.foreach { target =>
      val currentIBU = current.internationalBitteringUnits.total
      if (math.abs(currentIBU - target) > 2) {
        val hopAdjustmentFactor = target / currentIBU
        val totalHops = recipe.hops.map(_.quantityGrams).sum
        val hopAdjustment = totalHops * (hopAdjustmentFactor - 1)
        suggestions += OptimizationSuggestion(
          parameter = "IBU",
          currentValue = f"$currentIBU%.0f",
          targetValue = f"$target%.0f",
          suggestion = f"${if (hopAdjustment > 0) "Ajouter" else "Réduire"} ${math.abs(hopAdjustment)%.0f}g de houblons amers",
          impact = "MEDIUM",
          feasibility = "EASY"
        )
      }
    }
    
    // Suggestions pour SRM
    targetSRM.foreach { target =>
      val currentSRM = current.standardReferenceMethod.value
      if (math.abs(currentSRM - target) > 2) {
        if (target > currentSRM) {
          suggestions += OptimizationSuggestion(
            parameter = "SRM Color",
            currentValue = f"$currentSRM%.0f",
            targetValue = f"$target%.0f", 
            suggestion = "Ajouter des malts colorés (crystal, caramel) ou réduire les malts pâles",
            impact = "MEDIUM",
            feasibility = "MEDIUM"
          )
        } else {
          suggestions += OptimizationSuggestion(
            parameter = "SRM Color",
            currentValue = f"$currentSRM%.0f",
            targetValue = f"$target%.0f",
            suggestion = "Réduire les malts foncés et augmenter les malts pâles",
            impact = "MEDIUM",
            feasibility = "MEDIUM"
          )
        }
      }
    }
    
    suggestions.toList
  }

  private def calculateDifferences(calc1: BrewingCalculationResult, calc2: BrewingCalculationResult): List[RecipeDifference] = {
    List(
      RecipeDifference("Original Gravity", 
        f"${calc1.originalGravity.value}%.3f", 
        f"${calc2.originalGravity.value}%.3f",
        math.abs(calc1.originalGravity.value - calc2.originalGravity.value) * 1000 // en points
      ),
      RecipeDifference("Final Gravity",
        f"${calc1.finalGravity.value}%.3f",
        f"${calc2.finalGravity.value}%.3f", 
        math.abs(calc1.finalGravity.value - calc2.finalGravity.value) * 1000
      ),
      RecipeDifference("ABV",
        f"${calc1.alcoholByVolume.primary}%.1f%%",
        f"${calc2.alcoholByVolume.primary}%.1f%%",
        math.abs(calc1.alcoholByVolume.primary - calc2.alcoholByVolume.primary)
      ),
      RecipeDifference("IBU",
        f"${calc1.internationalBitteringUnits.total}%.0f",
        f"${calc2.internationalBitteringUnits.total}%.0f",
        math.abs(calc1.internationalBitteringUnits.total - calc2.internationalBitteringUnits.total)
      ),
      RecipeDifference("SRM",
        f"${calc1.standardReferenceMethod.value}%.0f",
        f"${calc2.standardReferenceMethod.value}%.0f",
        math.abs(calc1.standardReferenceMethod.value - calc2.standardReferenceMethod.value)
      )
    )
  }

  private def calculateSimilarityScore(recipe1: RecipeAggregate, recipe2: RecipeAggregate): Double = {
    // Score basé sur les ingrédients communs et les proportions
    val malts1 = recipe1.malts.map(m => m.maltId.value.toString -> m.quantityKg).toMap
    val malts2 = recipe2.malts.map(m => m.maltId.value.toString -> m.quantityKg).toMap
    val maltSimilarity = calculateIngredientSimilarity(malts1, malts2)
    
    val hops1 = recipe1.hops.map(h => h.hopId.value.toString -> h.quantityGrams).toMap  
    val hops2 = recipe2.hops.map(h => h.hopId.value.toString -> h.quantityGrams).toMap
    val hopSimilarity = calculateIngredientSimilarity(hops1, hops2)
    
    val yeastSimilarity = (recipe1.yeast, recipe2.yeast) match {
      case (Some(y1), Some(y2)) if y1.yeastId == y2.yeastId => 1.0
      case (None, None) => 1.0
      case _ => 0.0
    }
    
    (maltSimilarity * 0.5) + (hopSimilarity * 0.3) + (yeastSimilarity * 0.2)
  }

  private def calculateIngredientSimilarity(ingredients1: Map[String, Double], ingredients2: Map[String, Double]): Double = {
    val allIngredients = ingredients1.keySet ++ ingredients2.keySet
    if (allIngredients.isEmpty) return 1.0
    
    val similarities = allIngredients.map { ingredient =>
      val qty1 = ingredients1.getOrElse(ingredient, 0.0)
      val qty2 = ingredients2.getOrElse(ingredient, 0.0)
      val maxQty = math.max(qty1, qty2)
      if (maxQty == 0) 1.0 else 1.0 - (math.abs(qty1 - qty2) / maxQty)
    }
    
    similarities.sum / allIngredients.size
  }

  private def calculateFeasibilityScore(suggestions: List[OptimizationSuggestion]): Double = {
    if (suggestions.isEmpty) return 1.0
    
    val scores = suggestions.map { suggestion =>
      val feasibilityScore = suggestion.feasibility match {
        case "EASY" => 1.0
        case "MEDIUM" => 0.7
        case "HARD" => 0.4
        case "VERY_HARD" => 0.2
        case _ => 0.5
      }
      
      val impactPenalty = suggestion.impact match {
        case "HIGH" => 0.1
        case "MEDIUM" => 0.05
        case _ => 0.0
      }
      
      feasibilityScore - impactPenalty
    }
    
    scores.sum / suggestions.length
  }

  private def estimateRecipeCost(recipe: RecipeAggregate): Double = {
    // Estimation très approximative des coûts
    val maltCost = recipe.malts.map(_.quantityKg * 3.5).sum // 3.5€/kg en moyenne
    val hopCost = recipe.hops.map(_.quantityGrams * 0.02).sum // 0.02€/g en moyenne  
    val yeastCost = recipe.yeast.map(_ => 4.0).getOrElse(0.0) // 4€ par sachet
    val otherCost = recipe.otherIngredients.map(_.quantityGrams * 0.01).sum // 0.01€/g en moyenne
    
    maltCost + hopCost + yeastCost + otherCost
  }

  private def estimateBrewingTime(recipe: RecipeAggregate): Int = {
    // Estimation en minutes
    val baseTime = 300 // 5h de base
    val complexityBonus = recipe.malts.length * 15 + recipe.hops.length * 10
    val procedureTime = 60 // Temps procédures spéciales
    
    baseTime + complexityBonus + procedureTime
  }

  private def convertToAllGrain(recipe: RecipeAggregate, efficiency: Double): Future[Either[DomainError, RecipeAggregate]] = {
    // Conversion logique extrait -> all-grain
    Future.successful(Left(DomainError.businessRule("Conversion all-grain non implémentée", "NOT_IMPLEMENTED")))
  }

  private def convertToExtract(recipe: RecipeAggregate): Future[Either[DomainError, RecipeAggregate]] = {
    // Conversion logique all-grain -> extrait  
    Future.successful(Left(DomainError.businessRule("Conversion extrait non implémentée", "NOT_IMPLEMENTED")))
  }

  private def convertToPartialMash(recipe: RecipeAggregate, efficiency: Double): Future[Either[DomainError, RecipeAggregate]] = {
    // Conversion logique vers partial mash
    Future.successful(Left(DomainError.businessRule("Conversion partial mash non implémentée", "NOT_IMPLEMENTED")))
  }
}

// ========================================
// MODÈLES DE DONNÉES AUXILIAIRES
// ========================================

sealed trait RecipeType {
  def name: String
}
object RecipeType {
  case object AllGrain extends RecipeType { val name = "ALL_GRAIN" }
  case object Extract extends RecipeType { val name = "EXTRACT" }
  case object PartialMash extends RecipeType { val name = "PARTIAL_MASH" }
}

case class RecipeTargets(
  originalGravity: Option[Double],
  ibu: Option[Double], 
  srm: Option[Double],
  abv: Option[Double]
)

case class OptimizationSuggestion(
  parameter: String,
  currentValue: String,
  targetValue: String,
  suggestion: String,
  impact: String, // LOW, MEDIUM, HIGH
  feasibility: String // EASY, MEDIUM, HARD, VERY_HARD
)

case class RecipeOptimizationResult(
  originalCalculations: BrewingCalculationResult,
  targetValues: RecipeTargets,
  optimizationSuggestions: List[OptimizationSuggestion],
  feasibilityScore: Double
)

case class RecipeDifference(
  parameter: String,
  value1: String,
  value2: String,
  absoluteDifference: Double
)

case class RecipeComparisonResult(
  recipe1: RecipeAggregate,
  recipe2: RecipeAggregate,
  calculations1: BrewingCalculationResult,
  calculations2: BrewingCalculationResult,
  differences: List[RecipeDifference],
  similarity: Double
)

case class RecipeFeasibilityReport(
  recipe: RecipeAggregate,
  isFeasible: Boolean,
  validationErrors: List[String],
  warnings: List[String],
  recommendations: List[RecipeRecommendation],
  estimatedCost: Option[Double],
  estimatedTime: Option[Int] // minutes
)

object RecipeAggregateExtensions {
  implicit class RecipeAggregateExtensions(recipe: RecipeAggregate) {
  def canBeCalculated: Boolean = recipe.malts.nonEmpty && recipe.hops.nonEmpty && recipe.yeast.isDefined
  
  def totalMaltWeight: Double = recipe.malts.map(_.quantityKg).sum
  def totalHopWeight: Double = recipe.hops.map(_.quantityGrams).sum
  
  def estimatedOG: Option[Double] = recipe.calculations.getOriginalGravity
  def estimatedFG: Option[Double] = recipe.calculations.getFinalGravity
  def estimatedABV: Option[Double] = recipe.calculations.getABV
  def estimatedIBU: Option[Double] = recipe.calculations.getIBU
  def estimatedSRM: Option[Double] = recipe.calculations.getSRM
  }
}

object RecipeDomainService {
  // Formatters JSON pour les nouvelles structures
  import play.api.libs.json._
  
  implicit val recipeTargetsFormat: Format[RecipeTargets] = Json.format[RecipeTargets]
  implicit val optimizationSuggestionFormat: Format[OptimizationSuggestion] = Json.format[OptimizationSuggestion] 
  implicit val recipeOptimizationResultFormat: Format[RecipeOptimizationResult] = Json.format[RecipeOptimizationResult]
  implicit val recipeDifferenceFormat: Format[RecipeDifference] = Json.format[RecipeDifference]
  implicit val recipeComparisonResultFormat: Format[RecipeComparisonResult] = Json.format[RecipeComparisonResult]
  implicit val recipeFeasibilityReportFormat: Format[RecipeFeasibilityReport] = Json.format[RecipeFeasibilityReport]
}