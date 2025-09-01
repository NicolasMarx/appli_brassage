package application.queries.public.yeasts

case class SearchYeastsQuery(
  searchTerm: String,
  limit: Option[Int]
)
