package application.queries.admin.malts

/**
 * Query pour la liste des malts (interface admin)
 */
case class AdminMaltListQuery(
  page: Int = 0,
  pageSize: Int = 20,
  filterActive: Option[Boolean] = None
)
