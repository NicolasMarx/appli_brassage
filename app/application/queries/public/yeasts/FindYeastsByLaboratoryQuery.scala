package application.queries.public.yeasts

case class FindYeastsByLaboratoryQuery(
  laboratory: String,
  limit: Option[Int]
)
