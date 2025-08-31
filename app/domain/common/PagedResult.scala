package domain.common

import play.api.libs.json._

/**
 * Wrapper générique pour les résultats paginés
 */
case class PagedResult[T](
  items: List[T],
  currentPage: Int,
  pageSize: Int,
  totalCount: Long,
  hasNext: Boolean
) {
  val totalPages: Int = Math.ceil(totalCount.toDouble / pageSize).toInt
  val hasPrevious: Boolean = currentPage > 0
}

object PagedResult {
  
  def empty[T]: PagedResult[T] = PagedResult(
    items = List.empty,
    currentPage = 0,
    pageSize = 20,
    totalCount = 0,
    hasNext = false
  )
  
  def single[T](item: T): PagedResult[T] = PagedResult(
    items = List(item),
    currentPage = 0,
    pageSize = 1,
    totalCount = 1,
    hasNext = false
  )
  
  implicit def format[T: Format]: Format[PagedResult[T]] = Json.format[PagedResult[T]]
}
