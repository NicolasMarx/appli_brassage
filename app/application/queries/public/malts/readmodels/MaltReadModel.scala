package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

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
  characteristics: MaltCharacteristics,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class MaltCharacteristics(
  colorName: String,
  extractionCategory: String,
  enzymaticCategory: String,
  maxRecommendedPercent: Option[Double],
  isBaseMalt: Boolean,
  canSelfConvert: Boolean
)

case class MaltDetailResult(
  malt: MaltReadModel,
  substitutes: List[SubstituteReadModel]
)

case class SubstituteReadModel(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String]
)

case class MaltSearchResult(
  malts: List[MaltReadModel],
  totalCount: Long,
  currentPage: Int,
  pageSize: Int,
  hasNext: Boolean
)

object MaltReadModel {
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      characteristics = MaltCharacteristics(
        colorName = malt.ebcColor.colorName,
        extractionCategory = malt.extractionRate.extractionCategory,
        enzymaticCategory = malt.diastaticPower.enzymaticCategory,
        maxRecommendedPercent = malt.maxRecommendedPercent,
        isBaseMalt = malt.isBaseMalt,
        canSelfConvert = malt.canSelfConvert
      ),
      isActive = malt.isActive,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
  
  implicit val characteristicsFormat: Format[MaltCharacteristics] = Json.format[MaltCharacteristics]
  implicit val substituteFormat: Format[SubstituteReadModel] = Json.format[SubstituteReadModel]
  implicit val detailFormat: Format[MaltDetailResult] = Json.format[MaltDetailResult]
  implicit val searchFormat: Format[MaltSearchResult] = Json.format[MaltSearchResult]
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
