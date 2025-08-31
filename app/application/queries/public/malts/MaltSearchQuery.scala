package application.queries.public.malts

/**
 * Query pour la recherche avancée de malts (API publique)
 */
case class MaltSearchQuery(
  searchTerm: String,
  maltType: Option[String] = None,
  minEBC: Option[Double] = None,
  maxEBC: Option[Double] = None,
  minExtraction: Option[Double] = None,
  minDiastaticPower: Option[Double] = None,
  originCode: Option[String] = None,
  flavorProfiles: List[String] = List.empty,
  activeOnly: Boolean = true,
  page: Int = 0,
  pageSize: Int = 20
) {

  def validate(): Either[String, MaltSearchQuery] = {
    if (searchTerm.trim.isEmpty) {
      Left("Le terme de recherche est requis")
    } else if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else {
      Right(this)
    }
  }
}
