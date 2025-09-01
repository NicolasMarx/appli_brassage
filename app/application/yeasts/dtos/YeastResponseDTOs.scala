package application.yeasts.dtos

import play.api.libs.json._
import java.util.UUID

case class YeastDetailResponseDTO(
  id: UUID,
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  characteristics: Map[String, List[String]],
  status: String,
  version: Long,
  createdAt: String,
  updatedAt: String
)

case class YeastSummaryDTO(
  id: UUID,
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  status: String
)

case class YeastPageResponseDTO(
  yeasts: List[YeastSummaryDTO],
  totalCount: Long,
  page: Int,
  pageSize: Int,
  hasNext: Boolean
)

case class YeastRecommendationDTO(
  yeast: YeastSummaryDTO,
  score: Double,
  reason: String,
  tips: List[String]
)

case class YeastStatsDTO(
  totalCount: Long,
  activeCount: Long,
  inactiveCount: Long,
  topLaboratories: List[String],
  topYeastTypes: List[String],
  recentlyAdded: List[YeastSummaryDTO]
)

object YeastDetailResponseDTO {
  implicit val format: Format[YeastDetailResponseDTO] = Json.format[YeastDetailResponseDTO]
}

object YeastSummaryDTO {
  implicit val format: Format[YeastSummaryDTO] = Json.format[YeastSummaryDTO]
}

object YeastPageResponseDTO {
  implicit val format: Format[YeastPageResponseDTO] = Json.format[YeastPageResponseDTO]
}

object YeastRecommendationDTO {
  implicit val format: Format[YeastRecommendationDTO] = Json.format[YeastRecommendationDTO]
}

object YeastStatsDTO {
  implicit val format: Format[YeastStatsDTO] = Json.format[YeastStatsDTO]
}
