package application.queries.public.yeasts

import java.util.UUID

case class GetYeastAlternativesQuery(
  originalYeastId: UUID,
  reason: String,
  limit: Int
)
