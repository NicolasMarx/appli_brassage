package domain.common

/**
 * Résultat paginé générique utilisé dans tous les domaines
 * COPIE EXACTE du pattern Yeast pour cohérence architecturale
 */
case class PaginatedResult[T](
  items: List[T],
  totalCount: Long,
  page: Int,
  size: Int
) {
  def hasNext: Boolean = (page + 1) * size < totalCount
  def hasPrevious: Boolean = page > 0
  def totalPages: Long = math.ceil(totalCount.toDouble / size).toLong
  def offset: Int = page * size
}