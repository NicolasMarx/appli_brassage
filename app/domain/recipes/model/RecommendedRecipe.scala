package domain.recipes.model

import play.api.libs.json._

/**
 * Représente une recette recommandée avec son score et ses explications
 * Modèle du domaine pour les recommandations intelligentes
 */
case class RecommendedRecipe(
  recipe: RecipeAggregate,
  score: Double, // 0.0 - 1.0
  reason: String,
  tips: List[String],
  matchFactors: List[String] = List.empty
) {
  def id: RecipeId = recipe.id
  def name: String = recipe.name.value
  def style: String = recipe.style.name
  // Note: difficulty would need to be calculated from recipe complexity
  def abv: Option[Double] = recipe.calculations.abv
  def ibu: Option[Double] = recipe.calculations.ibu
  def srm: Option[Double] = recipe.calculations.srm
}

object RecommendedRecipe {
  implicit val format: Format[RecommendedRecipe] = Json.format[RecommendedRecipe]
}

/**
 * Critères de recommandation pour différents contextes
 */
case class RecommendationCriteria(
  style: Option[String] = None,
  maxDifficulty: Option[DifficultyLevel] = None,
  maxBrewingTimeHours: Option[Int] = None,
  maxFermentationWeeks: Option[Int] = None,
  availableIngredients: List[String] = List.empty,
  equipmentLimitations: List[String] = List.empty,
  preferredProfiles: List[String] = List.empty,
  context: RecommendationContext = RecommendationContext.General
)

sealed trait RecommendationContext {
  def value: String
}

object RecommendationContext {
  case object General extends RecommendationContext { val value = "GENERAL" }
  case object Beginner extends RecommendationContext { val value = "BEGINNER" }
  case object Seasonal extends RecommendationContext { val value = "SEASONAL" }
  case object Advanced extends RecommendationContext { val value = "ADVANCED" }
  case object Experimental extends RecommendationContext { val value = "EXPERIMENTAL" }
  case object QuickBrew extends RecommendationContext { val value = "QUICK_BREW" }
  case object LowAlcohol extends RecommendationContext { val value = "LOW_ALCOHOL" }
  case object HighAlcohol extends RecommendationContext { val value = "HIGH_ALCOHOL" }
  
  def fromString(str: String): RecommendationContext = str.toUpperCase.trim match {
    case "GENERAL" => General
    case "BEGINNER" => Beginner
    case "SEASONAL" => Seasonal
    case "ADVANCED" => Advanced
    case "EXPERIMENTAL" => Experimental
    case "QUICK_BREW" => QuickBrew
    case "LOW_ALCOHOL" => LowAlcohol
    case "HIGH_ALCOHOL" => HighAlcohol
    case _ => General
  }
  
  implicit val format: Format[RecommendationContext] = new Format[RecommendationContext] {
    def reads(json: JsValue): JsResult[RecommendationContext] = json match {
      case JsString(value) => JsSuccess(fromString(value))
      case _ => JsError("String expected")
    }
    
    def writes(context: RecommendationContext): JsValue = JsString(context.value)
  }
}

object RecommendationCriteria {
  implicit val format: Format[RecommendationCriteria] = Json.format[RecommendationCriteria]
}

/**
 * Résultat d'analyse de recette pour recommandations
 */
case class RecipeAnalysis(
  recipe: RecipeAggregate,
  complexity: ComplexityLevel,
  timeRequirements: TimeRequirements,
  ingredientComplexity: IngredientComplexity,
  styleCompliance: StyleCompliance,
  equipmentRequirements: List[String],
  skillsRequired: List[String],
  commonMistakes: List[String],
  successFactors: List[String]
)

case class ComplexityLevel(
  overall: DifficultyLevel,
  brewing: Int, // 1-10
  fermentation: Int, // 1-10
  ingredients: Int, // 1-10
  equipment: Int // 1-10
)

case class TimeRequirements(
  brewDayHours: Int,
  fermentationWeeks: Int,
  conditioningWeeks: Int,
  totalWeeks: Int,
  activeTimeHours: Int
)

case class IngredientComplexity(
  totalIngredients: Int,
  rareIngredients: Int,
  specialTechniques: List[String],
  substitutions: Map[String, List[String]] = Map.empty
)

case class StyleCompliance(
  targetStyle: String,
  complianceScore: Double, // 0.0 - 1.0
  deviations: List[String] = List.empty,
  improvements: List[String] = List.empty
)

object StyleCompliance {
  implicit val format: Format[StyleCompliance] = Json.format[StyleCompliance]
}