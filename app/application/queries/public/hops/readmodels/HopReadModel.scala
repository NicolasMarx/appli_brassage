package application.queries.public.hops.readmodels

import domain.hops.model.HopAggregate
import play.api.libs.json._

case class HopReadModel(
  id: String,
  name: String,
  alphaAcid: Double,
  betaAcid: Option[Double],
  origin: HopOriginResponse,
  usage: String,
  description: Option[String],
  aromaProfile: List[String],
  status: String,
  source: String,
  credibilityScore: Int,
  isActive: Boolean
)

case class HopOriginResponse(
  code: String,
  name: String,
  region: Option[String], // Corrigé: Option[String]
  isNoble: Boolean,
  isNewWorld: Boolean
)

object HopReadModel {
  
  def fromAggregate(hop: HopAggregate): HopReadModel = {
    HopReadModel(
      id = hop.id.value,
      name = hop.name.value,
      alphaAcid = hop.alphaAcid.value,
      betaAcid = hop.betaAcid.map(_.value),
      origin = HopOriginResponse(
        code = hop.origin.code,
        name = hop.origin.name,
        region = Some(hop.origin.region), // Corrigé: wrap dans Some()
        isNoble = hop.origin.isNoble,
        isNewWorld = hop.origin.isNewWorld
      ),
      usage = hop.usage match {
        case domain.hops.model.HopUsage.Bittering => "BITTERING"
        case domain.hops.model.HopUsage.Aroma => "AROMA"
        case domain.hops.model.HopUsage.DualPurpose => "DUAL_PURPOSE"
        case domain.hops.model.HopUsage.NobleHop => "NOBLE_HOP"
      },
      description = hop.description.map(_.value),
      aromaProfile = hop.aromaProfile,
      status = hop.status match {
        case domain.hops.model.HopStatus.Active => "ACTIVE"
        case domain.hops.model.HopStatus.Discontinued => "DISCONTINUED"
        case domain.hops.model.HopStatus.Limited => "LIMITED"
      },
      source = hop.source match {
        case domain.hops.model.HopSource.Manual => "MANUAL"
        case domain.hops.model.HopSource.AI_Discovery => "AI_DISCOVERED"
        case domain.hops.model.HopSource.Import => "IMPORT"
      },
      credibilityScore = hop.credibilityScore,
      isActive = hop.status == domain.hops.model.HopStatus.Active
    )
  }
  
  implicit val originFormat: Format[HopOriginResponse] = Json.format[HopOriginResponse]
  implicit val format: Format[HopReadModel] = Json.format[HopReadModel]
}

case class HopListResponse(
  hops: List[HopReadModel],
  totalCount: Long,
  page: Int,
  size: Int,
  hasNext: Boolean
)

object HopListResponse {
  
  def create(
    hops: List[HopReadModel],
    totalCount: Long,
    page: Int,
    size: Int
  ): HopListResponse = {
    HopListResponse(
      hops = hops,
      totalCount = totalCount,
      page = page,
      size = size,
      hasNext = (page + 1) * size < totalCount
    )
  }
  
  implicit val format: Format[HopListResponse] = Json.format[HopListResponse]
}
