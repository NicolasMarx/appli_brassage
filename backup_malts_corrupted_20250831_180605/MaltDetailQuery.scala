package application.queries.public.malts

/**
 * Query pour récupérer le détail d'un malt (API publique)
 */
case class MaltDetailQuery(
                            id: String,
                            includeSubstitutes: Boolean = false,
                            includeBeerStyles: Boolean = false
                          ) {

  def validate(): Either[String, MaltDetailQuery] = {
    if (id.trim.isEmpty) {
      Left("L'ID du malt est requis")
    } else {
      Right(this)
    }
  }
}