package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._

case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  isActive: Boolean
)

object MaltReadModel {
  
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.toString, // Correction: toString au lieu de .value
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      isActive = malt.isActive
    )
  }
  
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}

case class MaltListResponse(
  malts: List[MaltReadModel],
  totalCount: Long,
  page: Int,
  size: Int,
  hasNext: Boolean
)

object MaltListResponse {
  
  def create(
    malts: List[MaltReadModel],
    totalCount: Long,
    page: Int,
    size: Int
  ): MaltListResponse = {
    MaltListResponse(
      malts = malts,
      totalCount = totalCount,
      page = page,
      size = size,
      hasNext = (page + 1) * size < totalCount
    )
  }
  
  implicit val format: Format[MaltListResponse] = Json.format[MaltListResponse]
}
