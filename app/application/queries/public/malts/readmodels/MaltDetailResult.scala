package application.queries.public.malts.readmodels

import play.api.libs.json._

/**
 * Result détaillé pour une requête de malt spécifique
 * (Optionnel - pour usage futur avec substituts, compatibilités, etc.)
 */
case class MaltDetailResult(
  malt: MaltReadModel,
  substitutes: List[MaltSubstitute] = List.empty,
  compatibleBeerStyles: List[BeerStyleCompatibility] = List.empty,
  usageRecommendations: List[String] = List.empty
)

case class MaltSubstitute(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String]
)

case class BeerStyleCompatibility(
  styleId: String,
  styleName: String,
  compatibilityScore: Double,
  usageNotes: Option[String]
)

object MaltDetailResult {
  implicit val maltSubstituteFormat: Format[MaltSubstitute] = Json.format[MaltSubstitute]
  implicit val beerStyleCompatibilityFormat: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
  implicit val format: Format[MaltDetailResult] = Json.format[MaltDetailResult]
}
