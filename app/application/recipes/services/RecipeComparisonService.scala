package application.recipes.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.recipes.model._
import domain.recipes.repositories.{RecipeReadRepository, RecipeWriteRepository}
import domain.common.DomainError
import play.api.libs.json._
import java.time.Instant

/**
 * Service pour la comparaison de recettes avec analyse approfondie
 * Endpoint: GET /api/v1/recipes/compare
 * 
 * Fonctionnalité: Comparaison multi-critères entre 2+ recettes
 * Features: Analyse nutritionnelle, coûts, complexité, similitudes
 */
@Singleton
class RecipeComparisonService @Inject()(
  recipeReadRepository: RecipeReadRepository,
  recipeWriteRepository: RecipeWriteRepository
)(implicit ec: ExecutionContext) {

  /**
   * Comparer plusieurs recettes sur différents critères
   */
  def compareRecipes(
    recipeIds: List[RecipeId],
    comparisonCriteria: ComparisonCriteria = ComparisonCriteria.all,
    aggregateId: Option[String] = None
  ): Future[Either[DomainError, RecipeComparisonResult]] = {
    
    if (recipeIds.length < 2) {
      return Future.successful(Left(DomainError.validation("Au moins 2 recettes requises pour comparaison", "INSUFFICIENT_RECIPES")))
    }
    
    for {
      // Étape 1: Récupérer toutes les recettes
      recipes <- Future.sequence(recipeIds.map(recipeReadRepository.findById))
      
      result <- {
        val validRecipes = recipes.flatten
        
        if (validRecipes.length != recipeIds.length) {
          Future.successful(Left(DomainError.notFound("Une ou plusieurs recettes non trouvées", "RECIPES_NOT_FOUND")))
        } else {
          
          // Étape 2: Effectuer toutes les analyses de comparaison
          val nutritionalComparison = compareNutritionalProfiles(validRecipes)
          val ingredientComparison = compareIngredients(validRecipes)
          val complexityComparison = compareComplexity(validRecipes)
          val costComparison = compareCosts(validRecipes)
          val styleComparison = compareStyles(validRecipes)
          val processComparison = compareProcesses(validRecipes)
          
          // Étape 3: Analyse de similitude globale
          val similarityMatrix = calculateSimilarityMatrix(validRecipes)
          val recommendations = generateComparisonRecommendations(validRecipes, similarityMatrix)
          
          // Étape 4: Créer le résultat avec toutes les métadonnées
          val comparisonResult = RecipeComparisonResult(
            comparedRecipes = validRecipes,
            nutritionalComparison = nutritionalComparison,
            ingredientComparison = ingredientComparison,
            complexityComparison = complexityComparison,
            costComparison = costComparison,
            styleComparison = styleComparison,
            processComparison = processComparison,
            similarityMatrix = similarityMatrix,
            recommendations = recommendations,
            metadata = ComparisonMetadata(
              totalRecipes = validRecipes.length,
              comparisonCriteria = comparisonCriteria,
              aggregateId = aggregateId,
              generatedAt = Instant.now()
            )
          )
          
          Future.successful(Right(comparisonResult))
        }
      }
    } yield result
  }

  /**
   * Comparer les profils nutritionnels des recettes
   */
  private def compareNutritionalProfiles(recipes: List[RecipeAggregate]): NutritionalComparison = {
    val profiles = recipes.map { recipe =>
      RecipeNutritionalProfile(
        recipeId = recipe.id,
        recipeName = recipe.name.value,
        abv = recipe.calculations.abv.getOrElse(0.0),
        ibu = recipe.calculations.ibu.getOrElse(0.0),
        og = recipe.calculations.originalGravity.getOrElse(1.040),
        fg = recipe.calculations.finalGravity.getOrElse(1.010),
        estimatedCalories = calculateEstimatedCalories(recipe),
        color = calculateEstimatedColor(recipe)
      )
    }
    
    NutritionalComparison(
      profiles = profiles,
      averageABV = profiles.map(_.abv).sum / profiles.length,
      averageIBU = profiles.map(_.ibu).sum / profiles.length,
      abvRange = (profiles.map(_.abv).min, profiles.map(_.abv).max),
      ibuRange = (profiles.map(_.ibu).min, profiles.map(_.ibu).max),
      strongestRecipe = profiles.maxBy(_.abv),
      mildestRecipe = profiles.minBy(_.abv),
      mostBitterRecipe = profiles.maxBy(_.ibu),
      leastBitterRecipe = profiles.minBy(_.ibu)
    )
  }

  /**
   * Comparer les ingrédients utilisés
   */
  private def compareIngredients(recipes: List[RecipeAggregate]): IngredientComparison = {
    val allHops = recipes.flatMap(_.hops).map(_.hopId.toString).distinct
    val allMalts = recipes.flatMap(_.malts).map(_.maltId.toString).distinct
    val allYeasts = recipes.flatMap(_.yeast.map(_.yeastId.toString)).distinct
    
    val commonHops = allHops.filter(hop => recipes.forall(_.hops.exists(_.hopId.toString == hop)))
    val commonMalts = allMalts.filter(malt => recipes.forall(_.malts.exists(_.maltId.toString == malt)))
    
    val uniqueIngredients = recipes.map { recipe =>
      val recipeHops = recipe.hops.map(_.hopId.toString).toSet
      val recipeMalts = recipe.malts.map(_.maltId.toString).toSet
      
      UniqueIngredients(
        recipeId = recipe.id,
        uniqueHops = recipeHops -- allHops.filter(hop => recipes.count(r => r.hops.exists(_.hopId.toString == hop)) > 1).toSet,
        uniqueMalts = recipeMalts -- allMalts.filter(malt => recipes.count(r => r.malts.exists(_.maltId.toString == malt)) > 1).toSet
      )
    }
    
    IngredientComparison(
      totalUniqueHops = allHops.length,
      totalUniqueMalts = allMalts.length,
      totalUniqueYeasts = allYeasts.length,
      commonHops = commonHops,
      commonMalts = commonMalts,
      uniqueIngredients = uniqueIngredients,
      ingredientDiversity = calculateIngredientDiversity(recipes)
    )
  }

  /**
   * Comparer la complexité des recettes
   */
  private def compareComplexity(recipes: List[RecipeAggregate]): ComplexityComparison = {
    val complexityScores = recipes.map { recipe =>
      val hopComplexity = recipe.hops.length * 2
      val maltComplexity = recipe.malts.length * 1
      val yeastComplexity = if (recipe.yeast.isDefined) 3 else 0
      val otherComplexity = recipe.otherIngredients.length * 1
      
      val totalScore = hopComplexity + maltComplexity + yeastComplexity + otherComplexity
      
      RecipeComplexityScore(
        recipeId = recipe.id,
        recipeName = recipe.name.value,
        totalScore = totalScore,
        hopComplexity = hopComplexity,
        maltComplexity = maltComplexity,
        yeastComplexity = yeastComplexity,
        processComplexity = calculateProcessComplexity(recipe),
        level = totalScore match {
          case s if s < 10 => "BEGINNER"
          case s if s < 20 => "INTERMEDIATE" 
          case s if s < 35 => "ADVANCED"
          case _ => "EXPERT"
        }
      )
    }
    
    ComplexityComparison(
      scores = complexityScores,
      averageComplexity = complexityScores.map(_.totalScore).sum.toDouble / complexityScores.length,
      simplestRecipe = complexityScores.minBy(_.totalScore),
      mostComplexRecipe = complexityScores.maxBy(_.totalScore),
      complexityDistribution = complexityScores.groupBy(_.level).mapValues(_.length).toMap
    )
  }

  /**
   * Comparer les coûts estimés
   */
  private def compareCosts(recipes: List[RecipeAggregate]): CostComparison = {
    val costAnalysis = recipes.map { recipe =>
      val hopCost = recipe.hops.map(hop => hop.quantity * 0.02).sum // 2€/100g
      val maltCost = recipe.malts.map(malt => malt.quantity * 0.003).sum // 0.3€/100g
      val yeastCost = if (recipe.yeast.isDefined) 3.50 else 0.0 // 3.50€ par levure
      val otherCost = recipe.otherIngredients.map(_.quantity * 0.01).sum // 1€/100g
      
      val totalCost = hopCost + maltCost + yeastCost + otherCost
      val costPerLiter = totalCost / recipe.batchSize.toLiters
      
      RecipeCostAnalysis(
        recipeId = recipe.id,
        recipeName = recipe.name.value,
        hopCost = hopCost,
        maltCost = maltCost,
        yeastCost = yeastCost,
        otherCost = otherCost,
        totalCost = totalCost,
        costPerLiter = costPerLiter,
        batchSize = recipe.batchSize.toLiters
      )
    }
    
    CostComparison(
      analyses = costAnalysis,
      averageCostPerLiter = costAnalysis.map(_.costPerLiter).sum / costAnalysis.length,
      cheapestRecipe = costAnalysis.minBy(_.costPerLiter),
      mostExpensiveRecipe = costAnalysis.maxBy(_.costPerLiter),
      costRange = (costAnalysis.map(_.costPerLiter).min, costAnalysis.map(_.costPerLiter).max)
    )
  }

  /**
   * Comparer les styles de bière
   */
  private def compareStyles(recipes: List[RecipeAggregate]): StyleComparison = {
    val styles = recipes.map(r => r.style.name).distinct
    val styleDistribution = recipes.groupBy(_.style.name).mapValues(_.length).toMap
    
    StyleComparison(
      totalStyles = styles.length,
      styleDistribution = styleDistribution,
      dominantStyle = styleDistribution.maxBy(_._2)._1,
      styleConformance = recipes.map { recipe =>
        StyleConformanceScore(
          recipeId = recipe.id,
          recipeName = recipe.name.value,
          style = recipe.style.name,
          conformanceScore = calculateStyleConformance(recipe),
          deviations = calculateStyleDeviations(recipe)
        )
      }
    )
  }

  /**
   * Comparer les processus de brassage
   */
  private def compareProcesses(recipes: List[RecipeAggregate]): ProcessComparison = {
    ProcessComparison(
      estimatedDurations = recipes.map { recipe =>
        ProcessDuration(
          recipeId = recipe.id,
          recipeName = recipe.name.value,
          estimatedMinutes = calculateEstimatedDuration(recipe),
          preparationMinutes = 30,
          brewingMinutes = 240,
          cleanupMinutes = 45
        )
      },
      averageDuration = recipes.map(calculateEstimatedDuration).sum / recipes.length,
      quickestRecipe = recipes.minBy(calculateEstimatedDuration),
      longestRecipe = recipes.maxBy(calculateEstimatedDuration)
    )
  }

  /**
   * Calculer la matrice de similitude entre recettes
   */
  private def calculateSimilarityMatrix(recipes: List[RecipeAggregate]): SimilarityMatrix = {
    val similarities = for {
      i <- recipes.indices
      j <- recipes.indices if i < j
    } yield {
      val recipe1 = recipes(i)
      val recipe2 = recipes(j)
      
      val styleSimilarity = if (recipe1.style.name == recipe2.style.name) 1.0 else 0.0
      val abvSimilarity = 1.0 - Math.abs(recipe1.calculations.abv.getOrElse(0.0) - recipe2.calculations.abv.getOrElse(0.0)) / 15.0
      val ibuSimilarity = 1.0 - Math.abs(recipe1.calculations.ibu.getOrElse(0.0) - recipe2.calculations.ibu.getOrElse(0.0)) / 100.0
      val hopSimilarity = calculateIngredientSimilarity(recipe1.hops.map(_.hopId.toString), recipe2.hops.map(_.hopId.toString))
      val maltSimilarity = calculateIngredientSimilarity(recipe1.malts.map(_.maltId.toString), recipe2.malts.map(_.maltId.toString))
      
      val overallSimilarity = (styleSimilarity * 0.3 + abvSimilarity * 0.2 + ibuSimilarity * 0.2 + 
                              hopSimilarity * 0.15 + maltSimilarity * 0.15).max(0.0).min(1.0)
      
      RecipeSimilarity(
        recipe1Id = recipe1.id,
        recipe2Id = recipe2.id,
        overallSimilarity = overallSimilarity,
        styleSimilarity = styleSimilarity,
        abvSimilarity = Math.max(0.0, abvSimilarity),
        ibuSimilarity = Math.max(0.0, ibuSimilarity),
        ingredientSimilarity = (hopSimilarity + maltSimilarity) / 2.0
      )
    }
    
    SimilarityMatrix(
      similarities = similarities.toList,
      mostSimilarPair = if (similarities.nonEmpty) Some(similarities.maxBy(_.overallSimilarity)) else None,
      leastSimilarPair = if (similarities.nonEmpty) Some(similarities.minBy(_.overallSimilarity)) else None,
      averageSimilarity = if (similarities.nonEmpty) similarities.map(_.overallSimilarity).sum / similarities.length else 0.0
    )
  }

  /**
   * Générer des recommandations basées sur la comparaison
   */
  private def generateComparisonRecommendations(
    recipes: List[RecipeAggregate], 
    similarityMatrix: SimilarityMatrix
  ): List[ComparisonRecommendation] = {
    val recommendations = scala.collection.mutable.ListBuffer[ComparisonRecommendation]()
    
    // Recommandation pour débutants
    val simplestRecipe = recipes.minBy(calculateComplexityScore)
    recommendations += ComparisonRecommendation(
      type_ = "BEGINNER_FRIENDLY",
      title = "Recette recommandée pour débuter",
      description = s"${simplestRecipe.name.value} est la recette la plus simple à réaliser",
      recipeIds = List(simplestRecipe.id),
      confidence = 0.9
    )
    
    // Recommandation pour économies
    val cheapestRecipe = recipes.minBy(calculateEstimatedCostPerLiter)
    recommendations += ComparisonRecommendation(
      type_ = "COST_EFFECTIVE",
      title = "Recette la plus économique",
      description = s"${cheapestRecipe.name.value} offre le meilleur rapport qualité-prix",
      recipeIds = List(cheapestRecipe.id),
      confidence = 0.85
    )
    
    // Recommandation de découverte
    if (similarityMatrix.leastSimilarPair.isDefined) {
      val leastSimilar = similarityMatrix.leastSimilarPair.get
      recommendations += ComparisonRecommendation(
        type_ = "VARIETY_DISCOVERY",
        title = "Pour découvrir des saveurs différentes",
        description = "Ces deux recettes offrent des profils gustatifs très contrastés",
        recipeIds = List(leastSimilar.recipe1Id, leastSimilar.recipe2Id),
        confidence = 0.8
      )
    }
    
    recommendations.toList
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def calculateEstimatedCalories(recipe: RecipeAggregate): Double = {
    val abv = recipe.calculations.abv.getOrElse(0.0)
    val og = recipe.calculations.originalGravity.getOrElse(1.040)
    (abv * 7.0) + ((og - 1.0) * 1000 * 0.5) // Approximation simplifiée
  }

  private def calculateEstimatedColor(recipe: RecipeAggregate): Double = {
    recipe.malts.map(malt => malt.quantity * 0.8).sum / recipe.batchSize.toLiters // Approximation EBC
  }

  private def calculateProcessComplexity(recipe: RecipeAggregate): Int = {
    var complexity = 0
    if (recipe.hops.exists(_.additionTime > 60)) complexity += 2 // Houblonnage tardif
    if (recipe.malts.length > 3) complexity += 2 // Mélange de malts complexe
    if (recipe.otherIngredients.nonEmpty) complexity += 1 // Ingrédients spéciaux
    complexity
  }

  private def calculateIngredientDiversity(recipes: List[RecipeAggregate]): Double = {
    val allIngredients = (recipes.flatMap(_.hops.map(_.hopId.toString)) ++ 
                         recipes.flatMap(_.malts.map(_.maltId.toString))).distinct
    val avgIngredientsPerRecipe = recipes.map(r => r.hops.length + r.malts.length).sum.toDouble / recipes.length
    allIngredients.length / avgIngredientsPerRecipe
  }

  private def calculateComplexityScore(recipe: RecipeAggregate): Int = {
    recipe.hops.length * 2 + recipe.malts.length + 
    (if (recipe.yeast.isDefined) 3 else 0) + recipe.otherIngredients.length
  }

  private def calculateEstimatedCostPerLiter(recipe: RecipeAggregate): Double = {
    val hopCost = recipe.hops.map(_.quantity * 0.02).sum
    val maltCost = recipe.malts.map(_.quantity * 0.003).sum  
    val yeastCost = if (recipe.yeast.isDefined) 3.50 else 0.0
    val otherCost = recipe.otherIngredients.map(_.quantity * 0.01).sum
    (hopCost + maltCost + yeastCost + otherCost) / recipe.batchSize.toLiters
  }

  private def calculateEstimatedDuration(recipe: RecipeAggregate): Int = {
    240 + (recipe.hops.length * 5) + (recipe.malts.length * 3) // Base + complexity
  }

  private def calculateStyleConformance(recipe: RecipeAggregate): Double = {
    // Simplification - dans la vraie implementation, vérifier contre les guidelines BJCP
    val abvInRange = recipe.calculations.abv.map(abv => if (abv >= 4.0 && abv <= 12.0) 0.8 else 0.4).getOrElse(0.5)
    val ibuInRange = recipe.calculations.ibu.map(ibu => if (ibu >= 10.0 && ibu <= 80.0) 0.8 else 0.4).getOrElse(0.5)
    (abvInRange + ibuInRange) / 2.0
  }

  private def calculateStyleDeviations(recipe: RecipeAggregate): List[String] = {
    val deviations = scala.collection.mutable.ListBuffer[String]()
    
    recipe.calculations.abv.foreach { abv =>
      if (abv > 12.0) deviations += "ABV élevé pour le style"
      if (abv < 3.0) deviations += "ABV faible pour le style" 
    }
    
    recipe.calculations.ibu.foreach { ibu =>
      if (ibu > 100.0) deviations += "Amertume très élevée"
      if (ibu < 5.0) deviations += "Manque d'amertume"
    }
    
    deviations.toList
  }

  private def calculateIngredientSimilarity(ingredients1: List[String], ingredients2: List[String]): Double = {
    val common = ingredients1.intersect(ingredients2).length
    val union = (ingredients1 ++ ingredients2).distinct.length
    if (union == 0) 1.0 else common.toDouble / union.toDouble
  }
}

// ==========================================================================
// TYPES DE SUPPORT POUR COMPARAISON
// ==========================================================================

case class ComparisonCriteria(
  nutritional: Boolean = true,
  ingredients: Boolean = true,
  complexity: Boolean = true,
  cost: Boolean = true,
  style: Boolean = true,
  process: Boolean = true
)

object ComparisonCriteria {
  val all = ComparisonCriteria()
  val basic = ComparisonCriteria(complexity = false, cost = false, process = false)
}

case class RecipeComparisonResult(
  comparedRecipes: List[RecipeAggregate],
  nutritionalComparison: NutritionalComparison,
  ingredientComparison: IngredientComparison,
  complexityComparison: ComplexityComparison,
  costComparison: CostComparison,
  styleComparison: StyleComparison,
  processComparison: ProcessComparison,
  similarityMatrix: SimilarityMatrix,
  recommendations: List[ComparisonRecommendation],
  metadata: ComparisonMetadata
)

case class ComparisonMetadata(
  totalRecipes: Int,
  comparisonCriteria: ComparisonCriteria,
  aggregateId: Option[String],
  generatedAt: Instant
)

case class NutritionalComparison(
  profiles: List[RecipeNutritionalProfile],
  averageABV: Double,
  averageIBU: Double,
  abvRange: (Double, Double),
  ibuRange: (Double, Double),
  strongestRecipe: RecipeNutritionalProfile,
  mildestRecipe: RecipeNutritionalProfile,
  mostBitterRecipe: RecipeNutritionalProfile,
  leastBitterRecipe: RecipeNutritionalProfile
)

case class RecipeNutritionalProfile(
  recipeId: RecipeId,
  recipeName: String,
  abv: Double,
  ibu: Double,
  og: Double,
  fg: Double,
  estimatedCalories: Double,
  color: Double
)

case class IngredientComparison(
  totalUniqueHops: Int,
  totalUniqueMalts: Int,
  totalUniqueYeasts: Int,
  commonHops: List[String],
  commonMalts: List[String],
  uniqueIngredients: List[UniqueIngredients],
  ingredientDiversity: Double
)

case class UniqueIngredients(
  recipeId: RecipeId,
  uniqueHops: Set[String],
  uniqueMalts: Set[String]
)

case class ComplexityComparison(
  scores: List[RecipeComplexityScore],
  averageComplexity: Double,
  simplestRecipe: RecipeComplexityScore,
  mostComplexRecipe: RecipeComplexityScore,
  complexityDistribution: Map[String, Int]
)

case class RecipeComplexityScore(
  recipeId: RecipeId,
  recipeName: String,
  totalScore: Int,
  hopComplexity: Int,
  maltComplexity: Int,
  yeastComplexity: Int,
  processComplexity: Int,
  level: String
)

case class CostComparison(
  analyses: List[RecipeCostAnalysis],
  averageCostPerLiter: Double,
  cheapestRecipe: RecipeCostAnalysis,
  mostExpensiveRecipe: RecipeCostAnalysis,
  costRange: (Double, Double)
)

case class RecipeCostAnalysis(
  recipeId: RecipeId,
  recipeName: String,
  hopCost: Double,
  maltCost: Double,
  yeastCost: Double,
  otherCost: Double,
  totalCost: Double,
  costPerLiter: Double,
  batchSize: Double
)

case class StyleComparison(
  totalStyles: Int,
  styleDistribution: Map[String, Int],
  dominantStyle: String,
  styleConformance: List[StyleConformanceScore]
)

case class StyleConformanceScore(
  recipeId: RecipeId,
  recipeName: String,
  style: String,
  conformanceScore: Double,
  deviations: List[String]
)

case class ProcessComparison(
  estimatedDurations: List[ProcessDuration],
  averageDuration: Int,
  quickestRecipe: RecipeAggregate,
  longestRecipe: RecipeAggregate
)

case class ProcessDuration(
  recipeId: RecipeId,
  recipeName: String,
  estimatedMinutes: Int,
  preparationMinutes: Int,
  brewingMinutes: Int,
  cleanupMinutes: Int
)

case class SimilarityMatrix(
  similarities: List[RecipeSimilarity],
  mostSimilarPair: Option[RecipeSimilarity],
  leastSimilarPair: Option[RecipeSimilarity],
  averageSimilarity: Double
)

case class RecipeSimilarity(
  recipe1Id: RecipeId,
  recipe2Id: RecipeId,
  overallSimilarity: Double,
  styleSimilarity: Double,
  abvSimilarity: Double,
  ibuSimilarity: Double,
  ingredientSimilarity: Double
)

case class ComparisonRecommendation(
  type_ : String,
  title: String,
  description: String,
  recipeIds: List[RecipeId],
  confidence: Double
)