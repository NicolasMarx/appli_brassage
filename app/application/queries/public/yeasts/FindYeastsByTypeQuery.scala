package application.queries.public.yeasts

case class FindYeastsByTypeQuery(
  yeastType: String,
  limit: Option[Int]
)
