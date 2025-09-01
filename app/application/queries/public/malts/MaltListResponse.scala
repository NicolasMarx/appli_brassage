package application.queries.public.malts

import application.queries.public.malts.readmodels.MaltReadModel
import play.api.libs.json._

/**
 * Response pour la liste des malts (API publique)
 */
case class MaltListResponse(
  malts: List[MaltReadModel],
  totalCount: Long,
  page: Int = 0,
  pageSize: Int = 20
)

object MaltListResponse {
  implicit val format: Format[MaltListResponse] = Json.format[MaltListResponse]
}
