package application.queries.admin.malts

/**
 * Query pour liste malts avec informations admin (crédibilité, source, etc.)
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
    } else if (minCredibility.exists(c => c < 0 || c > 100)) {
      Left("Le score de crédibilité doit être entre 0 et 100")
    } else if (!List("name", "createdAt", "updatedAt", "credibilityScore", "ebcColor").contains(sortBy)) {
      Left("Critère de tri invalide")
    } else if (!List("asc", "desc").contains(sortOrder)) {
      Left("Ordre de tri invalide (asc ou desc)")
    } else {
      Right(this)
    }
  }
}