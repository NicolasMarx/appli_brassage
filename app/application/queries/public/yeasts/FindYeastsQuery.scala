package application.queries.public.yeasts

case class FindYeastsQuery(
  name: Option[String],
  laboratory: Option[String],
  yeastType: Option[String],
  minAttenuation: Option[Int],
  maxAttenuation: Option[Int],
  minTemperature: Option[Int],
  maxTemperature: Option[Int],
  minAlcoholTolerance: Option[Double],
  maxAlcoholTolerance: Option[Double],
  flocculation: Option[String],
  characteristics: Option[List[String]],
  status: Option[String],
  page: Int,
  size: Int
)
