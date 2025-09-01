package application.queries.public.yeasts

case class GetYeastRecommendationsQuery(
  beerStyle: Option[String],
  targetAbv: Option[Double],
  fermentationTemp: Option[Int],
  desiredCharacteristics: Option[List[String]],
  limit: Int
)
