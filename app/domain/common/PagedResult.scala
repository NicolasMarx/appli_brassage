package domain.common

import play.api.libs.json._

/**
 * Résultat paginé générique pour toutes les requêtes
 * Utilisé par tous les domaines pour la cohérence
 */
case class PagedResult[T](
  items: List[T],
  currentPage: Int,
  pageSize: Int,
  totalCount: Long,
  hasNext: Boolean
) {
  def hasPrevious: Boolean = currentPage > 0
  def totalPages: Long = Math.ceil(totalCount.toDouble / pageSize).toLong
  def isLastPage: Boolean = currentPage >= totalPages - 1
}

object PagedResult {
  def empty[T]: PagedResult[T] = PagedResult(List.empty, 0, 20, 0, hasNext = false)
  
  def apply[T](items: List[T], page: Int, pageSize: Int, totalCount: Long): PagedResult[T] = {
    val hasNext = (page + 1) * pageSize < totalCount
    PagedResult(items, page, pageSize, totalCount, hasNext)
  }
  
  implicit def format[T: Format]: Format[PagedResult[T]] = Json.format[PagedResult[T]]
}
