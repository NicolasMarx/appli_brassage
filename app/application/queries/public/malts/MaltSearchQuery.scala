package application.queries.public.malts

/**
 * Query de recherche avancée pour malts (API publique)
 */
case class MaltSearchQuery(
                            searchTerm: String,
                            page: Int = 0,
                            pageSize: Int = 20,
                            maltType: Option[String] = None,
                            minEBC: Option[Double] = None,
                            maxEBC: Option[Double] = None,
                            minExtraction: Option[Double] = None,
                            minDiastaticPower: Option[Double] = None,
                            flavorProfiles: List[String] = List.empty,
                            originCode: Option[String] = None,
                            activeOnly: Boolean = true
                          ) {

  def validate(): Either[String, MaltSearchQuery] = {
    if (searchTerm.trim.length < 2) {
      Left("Le terme de recherche doit contenir au moins 2 caractères")
    } else if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else {
      Right(this)
    }
  }
}