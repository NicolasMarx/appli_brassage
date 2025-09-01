package application.queries.admin.malts

import application.queries.admin.malts.readmodels.AdminMaltReadModel
import play.api.libs.json._

/**
 * Response pour la liste des malts (interface admin)
 */
case class AdminMaltListResponse(
  malts: List[AdminMaltReadModel],
  totalCount: Long,
  page: Int = 0,
  pageSize: Int = 20
)

object AdminMaltListResponse {
  implicit val format: Format[AdminMaltListResponse] = Json.format[AdminMaltListResponse]
}
