package application.queries.admin.malts

/**
 * Query pour la liste admin des malts avec filtres avancés
 */
case class AdminMaltListQuery(
  page: Int = 0,
  pageSize: Int = 20,
  maltType: Option[String] = None,
  status: Option[String] = None,
  source: Option[String] = None,
  minCredibility: Option[Int] = None,
  needsReview: Boolean = false,
  searchTerm: Option[String] = None,
  sortBy: String = "name",
  sortOrder: String = "asc"
) {

  def validate(): Either[String, AdminMaltListQuery] = {
    if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else {
      Right(this)
    }
  }
}
