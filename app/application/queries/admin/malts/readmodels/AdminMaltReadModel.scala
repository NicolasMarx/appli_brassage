package application.queries.admin.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

case class AdminMaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: String,
  isActive: Boolean,
  credibilityScore: Double,
  qualityScore: Double,
  needsReview: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
)

case class AdminMaltDetailResult(
  malt: AdminMaltReadModel,
  substitutes: List[AdminSubstituteReadModel],
  beerStyleCompatibilities: List[BeerStyleCompatibility],
  statistics: MaltUsageStatistics,
  qualityAnalysis: QualityAnalysis
)

case class AdminSubstituteReadModel(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String],
  qualityScore: Double
)

case class BeerStyleCompatibility(
  styleId: String,
  styleName: String,
  compatibilityScore: Double,
  usageNotes: String
)

case class MaltUsageStatistics(
  recipeCount: Int,
  avgUsagePercent: Double,
  popularityScore: Double
)

case class QualityAnalysis(
  dataCompleteness: Double,
  sourceReliability: String,
  reviewStatus: String,
  lastValidated: Option[Instant]
)

case class AdminMaltSearchResult(
  malts: List[AdminMaltReadModel],
  totalCount: Long,
  currentPage: Int,
  pageSize: Int,
  hasNext: Boolean,
  filters: AdminSearchFilters
)

case class AdminSearchFilters(
  maltType: Option[String],
  status: Option[String],
  source: Option[String],
  minCredibility: Option[Double],
  needsReview: Boolean,
  searchTerm: Option[String]
)

object AdminMaltReadModel {
  def fromAggregate(malt: MaltAggregate): AdminMaltReadModel = {
    AdminMaltReadModel(
      id = malt.id.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      source = malt.source.name,
      isActive = malt.isActive,
      credibilityScore = malt.credibilityScore,
      qualityScore = malt.qualityScore,
      needsReview = malt.needsReview,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt,
      version = malt.version
    )
  }
  
  implicit val format: Format[AdminMaltReadModel] = Json.format[AdminMaltReadModel]
  implicit val substituteFormat: Format[AdminSubstituteReadModel] = Json.format[AdminSubstituteReadModel]
  implicit val compatibilityFormat: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
  implicit val statisticsFormat: Format[MaltUsageStatistics] = Json.format[MaltUsageStatistics]
  implicit val qualityFormat: Format[QualityAnalysis] = Json.format[QualityAnalysis]
  implicit val detailFormat: Format[AdminMaltDetailResult] = Json.format[AdminMaltDetailResult]
  implicit val searchFiltersFormat: Format[AdminSearchFilters] = Json.format[AdminSearchFilters]
  implicit val searchResultFormat: Format[AdminMaltSearchResult] = Json.format[AdminMaltSearchResult]
}
