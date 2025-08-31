package application.queries.public.malts

/**
 * Query pour récupérer la liste paginée des malts (API publique)
 */
case class MaltListQuery(
                          page: Int = 0,
                          pageSize: Int = 20,
                          maltType: Option[String] = None,
                          minEBC: Option[Double] = None,
                          maxEBC: Option[Double] = None,
                          originCode: Option[String] = None,
                          activeOnly: Boolean = true
                        ) {

  def validate(): Either[String, MaltListQuery] = {
    if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else if (minEBC.exists(_ < 0) || maxEBC.exists(_ < 0)) {
      Left("Les valeurs EBC ne peuvent pas être négatives")
    } else if (minEBC.isDefined && maxEBC.isDefined && minEBC.get > maxEBC.get) {
      Left("EBC minimum ne peut pas être supérieur à EBC maximum")
    } else {
      Right(this)
    }
  }
}