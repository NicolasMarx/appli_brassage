package domain.malts.repositories

/**
 * Type PagedResult utilisé par MaltSearchQueryHandler
 * Résultat paginé générique
 */
case class PagedResult[T](
  items: List[T],
  currentPage: Int,
  pageSize: Int,
  totalCount: Long,
  hasNext: Boolean
)
