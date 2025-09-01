package application.yeasts.dtos

import play.api.libs.json._
import domain.yeasts.services.RecommendedYeast
import java.time.Instant

/**
 * DTOs pour les recommandations de levures
 * Suivant les principes DDD/CQRS du projet
 */

case class YeastRecommendationResponse(
  id: String,
  name: String,
  strain: String,
  yeastType: String,
  laboratory: String,
  score: Double,
  reason: String,
  tips: List[String],
  attenuationRange: String,
  temperatureRange: String,
  flocculation: String,
  alcoholTolerance: Double,
  isActive: Boolean
)

case class RecommendationListResponse(
  recommendations: List[YeastRecommendationResponse],
  totalCount: Int,
  requestType: String,
  parameters: Map[String, String]
)

object YeastRecommendationResponse {
  implicit val format: Format[YeastRecommendationResponse] = Json.format[YeastRecommendationResponse]
  
  def fromDomainRecommendation(rec: RecommendedYeast): YeastRecommendationResponse = {
    YeastRecommendationResponse(
      id = rec.yeast.id.asString,
      name = rec.yeast.name.value,
      strain = rec.yeast.strain.value,
      yeastType = rec.yeast.yeastType.name,
      laboratory = rec.yeast.laboratory.name,
      score = rec.score,
      reason = rec.reason,
      tips = rec.tips,
      attenuationRange = s"${rec.yeast.attenuationRange.min}-${rec.yeast.attenuationRange.max}%",
      temperatureRange = s"${rec.yeast.fermentationTemp.min}°C-${rec.yeast.fermentationTemp.max}°C",
      flocculation = rec.yeast.flocculation.name,
      alcoholTolerance = rec.yeast.alcoholTolerance.value,
      isActive = rec.yeast.isActive
    )
  }
}

object RecommendationListResponse {
  implicit val format: Format[RecommendationListResponse] = Json.format[RecommendationListResponse]
}