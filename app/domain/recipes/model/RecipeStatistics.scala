package domain.recipes.model

import play.api.libs.json._

/**
 * Statistiques publiques des recettes - Modèle du domaine
 */
case class RecipePublicStatistics(
  totalRecipes: Int,
  totalStyles: Int,
  averageAbv: Double,
  averageIbu: Double,
  popularStyles: List[StylePopularity],
  popularIngredients: PopularIngredients,
  trends: TrendStatistics,
  communityMetrics: CommunityMetrics
)

case class StylePopularity(
  styleName: String,
  category: String,
  count: Int,
  percentage: Double,
  averageRating: Double
)

case class PopularIngredients(
  hops: List[IngredientPopularity],
  malts: List[IngredientPopularity],
  yeasts: List[IngredientPopularity]
)

case class IngredientPopularity(
  name: String,
  category: Option[String] = None,
  usageCount: Int,
  usagePercentage: Double,
  averageQuantity: Option[Double] = None,
  trendingScore: Double = 0.0
)

case class TrendStatistics(
  emergingStyles: List[StyleTrend],
  popularIngredients: List[IngredientTrend],
  seasonalTrends: List[SeasonalTrend]
)

case class StyleTrend(
  styleName: String,
  growthPercentage: Double,
  currentRank: Int,
  previousRank: Option[Int] = None,
  timeframe: String
)

case class IngredientTrend(
  ingredientName: String,
  ingredientType: IngredientType,
  growthPercentage: Double,
  popularityScore: Double,
  timeframe: String
)

sealed trait IngredientType {
  def name: String
}

object IngredientType {
  case object Hop extends IngredientType { val name = "hop" }
  case object Malt extends IngredientType { val name = "malt" }
  case object Yeast extends IngredientType { val name = "yeast" }
  case object Other extends IngredientType { val name = "other" }
  
  def fromString(s: String): Option[IngredientType] = s.toLowerCase match {
    case "hop" => Some(Hop)
    case "malt" => Some(Malt)
    case "yeast" => Some(Yeast)
    case "other" => Some(Other)
    case _ => None
  }
}

case class SeasonalTrend(
  season: Season,
  topStyles: List[String],
  characteristicIngredients: List[String],
  averageAbv: Double,
  popularityScore: Double
)

case class CommunityMetrics(
  averageRating: Double,
  totalRatings: Int,
  activeBrewers: Int,
  averageRecipesPerBrewer: Double
)

/**
 * Collections thématiques de recettes - Modèle du domaine
 */
case class RecipeCollectionListResponse(
  collections: List[RecipeCollection],
  totalCount: Int,
  page: Int,
  size: Int,
  hasNext: Boolean,
  categories: List[String]
)

case class RecipeCollection(
  id: String,
  name: String,
  description: String,
  category: CollectionCategory,
  recipes: List[RecipeAggregate],
  totalRecipes: Int,
  curator: Option[String] = None,
  tags: List[String] = List.empty,
  difficulty: Option[DifficultyLevel] = None,
  averageRating: Option[Double] = None,
  createdAt: java.time.Instant
)

sealed trait CollectionCategory {
  def name: String
  def description: String
}

object CollectionCategory {
  case object Seasonal extends CollectionCategory {
    val name = "seasonal"
    val description = "Collections saisonnières"
  }
  case object Style extends CollectionCategory {
    val name = "style"
    val description = "Collections par style de bière"
  }
  case object Difficulty extends CollectionCategory {
    val name = "difficulty"
    val description = "Collections par niveau de difficulté"
  }
  case object Ingredients extends CollectionCategory {
    val name = "ingredients"
    val description = "Collections par ingrédients spécifiques"
  }
  case object Technique extends CollectionCategory {
    val name = "technique"
    val description = "Collections par techniques de brassage"
  }
  case object Regional extends CollectionCategory {
    val name = "regional"
    val description = "Collections régionales"
  }
  case object Historical extends CollectionCategory {
    val name = "historical"
    val description = "Collections historiques"
  }
  
  val all = List(Seasonal, Style, Difficulty, Ingredients, Technique, Regional, Historical)
  
  def fromString(s: String): Option[CollectionCategory] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
}

object RecipePublicStatistics {
  implicit val stylePopularityFormat: Format[StylePopularity] = Json.format[StylePopularity]
  implicit val ingredientPopularityFormat: Format[IngredientPopularity] = Json.format[IngredientPopularity]
  implicit val popularIngredientsFormat: Format[PopularIngredients] = Json.format[PopularIngredients]
  
  implicit val ingredientTypeFormat: Format[IngredientType] = Format(
    Reads(js => js.validate[String].flatMap(s => IngredientType.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid ingredient type")))),
    Writes(iType => JsString(iType.name))
  )
  
  implicit val styleTrendFormat: Format[StyleTrend] = Json.format[StyleTrend]
  implicit val ingredientTrendFormat: Format[IngredientTrend] = Json.format[IngredientTrend]
  implicit val seasonalTrendFormat: Format[SeasonalTrend] = Json.format[SeasonalTrend]
  implicit val trendStatisticsFormat: Format[TrendStatistics] = Json.format[TrendStatistics]
  implicit val communityMetricsFormat: Format[CommunityMetrics] = Json.format[CommunityMetrics]
  
  implicit val format: Format[RecipePublicStatistics] = Json.format[RecipePublicStatistics]
}

object RecipeCollectionListResponse {
  implicit val collectionCategoryFormat: Format[CollectionCategory] = Format(
    Reads(js => js.validate[String].flatMap(s => CollectionCategory.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid category")))),
    Writes(cat => JsString(cat.name))
  )
  implicit val recipeCollectionFormat: Format[RecipeCollection] = Json.format[RecipeCollection]
  implicit val format: Format[RecipeCollectionListResponse] = Json.format[RecipeCollectionListResponse]
}