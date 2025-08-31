package application.queries.admin.malts

/**
 * Query détail malt avec toutes informations admin (audit, etc.)
 */
case class AdminMaltDetailQuery(
                                 id: String,
                                 includeAuditLog: Boolean = false,
                                 includeSubstitutes: Boolean = true,
                                 includeBeerStyles: Boolean = true,
                                 includeStatistics: Boolean = false
                               ) {

  def validate(): Either[String, AdminMaltDetailQuery] = {
    if (id.trim.isEmpty) {
      Left("L'ID du malt est requis")
    } else {
      Right(this)
    }
  }
}