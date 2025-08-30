package domain.shared

/**
 * Classe de pagination générique pour encapsuler les résultats paginés.
 * Utilisée dans les repositories et contrôleurs pour standardiser la pagination.
 */
case class Page[T](
                    items: List[T],
                    currentPage: Int,
                    pageSize: Int,
                    totalItems: Int,
                    totalPages: Int
                  ) {

  /**
   * Indique s'il y a une page suivante
   */
  def hasNext: Boolean = currentPage < totalPages - 1

  /**
   * Indique s'il y a une page précédente
   */
  def hasPrevious: Boolean = currentPage > 0

  /**
   * Retourne le numéro de la page suivante (si elle existe)
   */
  def nextPage: Option[Int] = if (hasNext) Some(currentPage + 1) else None

  /**
   * Retourne le numéro de la page précédente (si elle existe)
   */
  def previousPage: Option[Int] = if (hasPrevious) Some(currentPage - 1) else None

  /**
   * Indique si la page est vide
   */
  def isEmpty: Boolean = items.isEmpty

  /**
   * Indique si la page contient des éléments
   */
  def nonEmpty: Boolean = items.nonEmpty

  /**
   * Nombre d'éléments dans cette page
   */
  def size: Int = items.size

  /**
   * Transforme les éléments de la page avec une fonction
   */
  def map[U](f: T => U): Page[U] = copy(items = items.map(f))

  /**
   * Filtre les éléments de la page (attention: peut changer la taille)
   */
  def filter(predicate: T => Boolean): Page[T] = copy(items = items.filter(predicate))

  /**
   * Crée une nouvelle page avec des métadonnées mises à jour
   */
  def withMetadata(newCurrentPage: Int, newPageSize: Int, newTotalItems: Int): Page[T] = {
    val newTotalPages = if (newPageSize > 0) (newTotalItems + newPageSize - 1) / newPageSize else 0
    copy(
      currentPage = newCurrentPage,
      pageSize = newPageSize,
      totalItems = newTotalItems,
      totalPages = newTotalPages
    )
  }
}

object Page {

  /**
   * Crée une page vide
   */
  def empty[T]: Page[T] = Page(
    items = List.empty[T],
    currentPage = 0,
    pageSize = 0,
    totalItems = 0,
    totalPages = 0
  )

  /**
   * Crée une page à partir d'une liste complète et des paramètres de pagination
   */
  def from[T](allItems: List[T], page: Int, pageSize: Int): Page[T] = {
    if (pageSize <= 0) {
      empty[T]
    } else {
      val totalItems = allItems.length
      val totalPages = (totalItems + pageSize - 1) / pageSize
      val safePage = math.max(0, math.min(page, totalPages - 1))
      val start = safePage * pageSize
      val end = math.min(start + pageSize, totalItems)
      val pageItems = if (start < totalItems) allItems.slice(start, end) else List.empty[T]

      Page(
        items = pageItems,
        currentPage = safePage,
        pageSize = pageSize,
        totalItems = totalItems,
        totalPages = totalPages
      )
    }
  }

  /**
   * Crée une page directement avec les éléments et métadonnées
   * Utile quand la pagination est faite côté base de données
   */
  def of[T](items: List[T], currentPage: Int, pageSize: Int, totalItems: Int): Page[T] = {
    val totalPages = if (pageSize > 0) (totalItems + pageSize - 1) / pageSize else 0

    Page(
      items = items,
      currentPage = currentPage,
      pageSize = pageSize,
      totalItems = totalItems,
      totalPages = totalPages
    )
  }

  /**
   * Crée une page avec un seul élément
   */
  def single[T](item: T): Page[T] = Page(
    items = List(item),
    currentPage = 0,
    pageSize = 1,
    totalItems = 1,
    totalPages = 1
  )

  /**
   * Crée une page à partir d'un tuple (items, totalCount) - format couramment utilisé
   */
  def fromTuple[T](data: (List[T], Int), currentPage: Int, pageSize: Int): Page[T] = {
    val (items, totalItems) = data
    of(items, currentPage, pageSize, totalItems)
  }
}

/**
 * Paramètres de pagination pour les requêtes
 */
case class PageRequest(page: Int, size: Int) {
  require(page >= 0, "Page number must be non-negative")
  require(size > 0, "Page size must be positive")

  /**
   * Offset pour les requêtes SQL
   */
  def offset: Int = page * size

  /**
   * Limite pour les requêtes SQL
   */
  def limit: Int = size
}

object PageRequest {

  /**
   * Paramètres par défaut
   */
  val default: PageRequest = PageRequest(0, 20)

  /**
   * Crée un PageRequest avec validation
   */
  def of(page: Int, size: Int): PageRequest = {
    val safePage = math.max(0, page)
    val safeSize = math.max(1, math.min(size, 1000)) // limite max à 1000 éléments
    PageRequest(safePage, safeSize)
  }

  /**
   * Crée un PageRequest à partir d'options (pour les paramètres HTTP)
   */
  def fromOptions(page: Option[Int], size: Option[Int]): PageRequest = {
    of(page.getOrElse(0), size.getOrElse(20))
  }
}