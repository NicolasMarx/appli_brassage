package application.queries.public.malts

/**
 * Query pour obtenir le détail d'un malt spécifique
 */
case class MaltDetailQuery(
  maltId: String  // ✅ Propriété maltId requise par le handler
)
