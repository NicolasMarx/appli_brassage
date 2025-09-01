package application.queries.public.malts

/**
 * Query pour la liste des malts (API publique)
 */
case class MaltListQuery(
  page: Int = 0,
  pageSize: Int = 20
)
