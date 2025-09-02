package domain.recipes.model

import play.api.libs.json._

/**
 * Demande d'analyse de recette personnalisée - Modèle du domaine
 */
case class CustomRecipeAnalysisRequest(
  name: String,
  style: String,
  batchSize: Double,
  batchUnit: String,
  hops: List[AnalysisIngredient],
  malts: List[AnalysisIngredient],
  yeasts: List[AnalysisIngredient],
  otherIngredients: List[AnalysisIngredient] = List.empty,
  procedures: Option[AnalysisProcedures] = None,
  targetParameters: Option[TargetParameters] = None
)

case class AnalysisIngredient(
  name: String,
  quantity: Double,
  unit: String,
  timing: Option[String] = None, // Pour houblon
  alpha: Option[Double] = None, // Alpha acids pour houblon
  lovibond: Option[Double] = None, // Pour malt
  extract: Option[Double] = None // Pour malt
)

case class AnalysisProcedures(
  mashTemp: Option[Double] = None,
  mashTime: Option[Int] = None,
  boilTime: Option[Int] = None,
  fermentationTemp: Option[Double] = None,
  fermentationTime: Option[Int] = None
)

case class TargetParameters(
  targetAbv: Option[Double] = None,
  targetIbu: Option[Double] = None,
  targetSrm: Option[Double] = None,
  targetOg: Option[Double] = None,
  targetFg: Option[Double] = None
)

/**
 * Réponse d'analyse de recette - Modèle du domaine
 */
case class RecipeAnalysisResponse(
  calculatedParameters: CalculatedParameters,
  styleCompliance: StyleCompliance,
  recommendations: List[String],
  warnings: List[String],
  improvements: List[ImprovementSuggestion]
)

case class CalculatedParameters(
  abv: Option[Double] = None,
  ibu: Option[Double] = None,
  srm: Option[Double] = None,
  og: Option[Double] = None,
  fg: Option[Double] = None,
  attenuation: Option[Double] = None,
  calories: Option[Double] = None
)

case class ComplianceItem(
  isCompliant: Boolean,
  actual: Double,
  min: Double,
  max: Double,
  score: Double
)

case class ImprovementSuggestion(
  category: ImprovementCategory,
  title: String,
  description: String,
  impact: ImprovementImpact,
  difficulty: ImprovementDifficulty
)

sealed trait ImprovementCategory {
  def name: String
}

object ImprovementCategory {
  case object Ingredients extends ImprovementCategory { val name = "ingredients" }
  case object Process extends ImprovementCategory { val name = "process" }
  case object Timing extends ImprovementCategory { val name = "timing" }
  case object Temperature extends ImprovementCategory { val name = "temperature" }
  case object Balance extends ImprovementCategory { val name = "balance" }
  case object Style extends ImprovementCategory { val name = "style" }
  
  def fromString(s: String): Option[ImprovementCategory] = s.toLowerCase match {
    case "ingredients" => Some(Ingredients)
    case "process" => Some(Process)
    case "timing" => Some(Timing)
    case "temperature" => Some(Temperature)
    case "balance" => Some(Balance)
    case "style" => Some(Style)
    case _ => None
  }
}

sealed trait ImprovementImpact {
  def name: String
  def level: Int
}

object ImprovementImpact {
  case object High extends ImprovementImpact { val name = "high"; val level = 3 }
  case object Medium extends ImprovementImpact { val name = "medium"; val level = 2 }
  case object Low extends ImprovementImpact { val name = "low"; val level = 1 }
  
  def fromString(s: String): Option[ImprovementImpact] = s.toLowerCase match {
    case "high" => Some(High)
    case "medium" => Some(Medium)
    case "low" => Some(Low)
    case _ => None
  }
}

sealed trait ImprovementDifficulty {
  def name: String
  def level: Int
}

object ImprovementDifficulty {
  case object Easy extends ImprovementDifficulty { val name = "easy"; val level = 1 }
  case object Medium extends ImprovementDifficulty { val name = "medium"; val level = 2 }
  case object Hard extends ImprovementDifficulty { val name = "hard"; val level = 3 }
  
  def fromString(s: String): Option[ImprovementDifficulty] = s.toLowerCase match {
    case "easy" => Some(Easy)
    case "medium" => Some(Medium)
    case "hard" => Some(Hard)
    case _ => None
  }
}

object CustomRecipeAnalysisRequest {
  implicit val analysisIngredientFormat: Format[AnalysisIngredient] = Json.format[AnalysisIngredient]
  implicit val analysisProceduresFormat: Format[AnalysisProcedures] = Json.format[AnalysisProcedures]
  implicit val targetParametersFormat: Format[TargetParameters] = Json.format[TargetParameters]
  implicit val format: Format[CustomRecipeAnalysisRequest] = Json.format[CustomRecipeAnalysisRequest]
}

object RecipeAnalysisResponse {
  implicit val calculatedParametersFormat: Format[CalculatedParameters] = Json.format[CalculatedParameters]
  implicit val complianceItemFormat: Format[ComplianceItem] = Json.format[ComplianceItem]
  implicit val styleComplianceFormat: Format[StyleCompliance] = StyleCompliance.format
  
  implicit val improvementCategoryFormat: Format[ImprovementCategory] = Format(
    Reads(js => js.validate[String].flatMap(s => ImprovementCategory.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid category")))),
    Writes(cat => JsString(cat.name))
  )
  implicit val improvementImpactFormat: Format[ImprovementImpact] = Format(
    Reads(js => js.validate[String].flatMap(s => ImprovementImpact.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid impact")))),
    Writes(impact => JsString(impact.name))
  )
  implicit val improvementDifficultyFormat: Format[ImprovementDifficulty] = Format(
    Reads(js => js.validate[String].flatMap(s => ImprovementDifficulty.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid difficulty")))),
    Writes(diff => JsString(diff.name))
  )
  implicit val improvementSuggestionFormat: Format[ImprovementSuggestion] = Json.format[ImprovementSuggestion]
  
  implicit val format: Format[RecipeAnalysisResponse] = Json.format[RecipeAnalysisResponse]
}