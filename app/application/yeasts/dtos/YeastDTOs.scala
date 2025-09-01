package application.yeasts.dtos

// CORRECTION: Suppression des imports inexistants
import domain.yeasts.model._
import play.api.libs.json._
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

/**
 * DTOs pour le domaine Yeasts
 * CORRECTION: Suppression imports YeastResponseDTOs/YeastRequestDTOs inexistants
 */

// DTOs de requête
case class CreateYeastRequest(
  name: String,
  strain: String,
  yeastType: String,
  laboratory: String,
  attenuationMin: Double,
  attenuationMax: Double,
  temperatureMin: Double,
  temperatureMax: Double,
  flocculation: String,
  alcoholTolerance: Double,
  description: Option[String] = None
)

case class UpdateYeastRequest(
  name: Option[String] = None,
  strain: Option[String] = None,
  yeastType: Option[String] = None,
  laboratory: Option[String] = None,
  attenuationMin: Option[Double] = None,
  attenuationMax: Option[Double] = None,
  temperatureMin: Option[Double] = None,
  temperatureMax: Option[Double] = None,
  flocculation: Option[String] = None,
  alcoholTolerance: Option[Double] = None,
  description: Option[String] = None
)

// DTOs de réponse
case class YeastResponse(
  id: String,
  name: String,
  strain: String,
  yeastType: String,
  laboratory: String,
  attenuationRange: String,
  temperatureRange: String,
  flocculation: String,
  alcoholTolerance: Double,
  description: Option[String],
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class YeastListResponse(
  yeasts: List[YeastResponse],
  totalCount: Long,
  page: Int,
  pageSize: Int
)

// CORRECTION: Classe sans ExecutionContext inutilisé
class YeastDTOs {

  def fromAggregate(yeast: YeastAggregate): YeastResponse = {
    YeastResponse(
      id = yeast.id.asString,
      name = yeast.name.value,
      strain = yeast.strain.value,
      yeastType = yeast.yeastType.name,
      laboratory = yeast.laboratory.name,
      attenuationRange = s"${yeast.attenuationRange.min}%-${yeast.attenuationRange.max}%",
      temperatureRange = s"${yeast.fermentationTemp.min}°C-${yeast.fermentationTemp.max}°C",
      flocculation = yeast.flocculation.name,
      alcoholTolerance = yeast.alcoholTolerance.value,
      description = yeast.description,
      isActive = yeast.isActive,
      createdAt = yeast.createdAt,
      updatedAt = yeast.updatedAt
    )
  }

  def toListResponse(yeasts: List[YeastAggregate], totalCount: Long, page: Int, pageSize: Int): YeastListResponse = {
    YeastListResponse(
      yeasts = yeasts.map(fromAggregate),
      totalCount = totalCount,
      page = page,
      pageSize = pageSize
    )
  }
}

object YeastDTOs {
  implicit val createRequestFormat: Format[CreateYeastRequest] = Json.format[CreateYeastRequest]
  implicit val updateRequestFormat: Format[UpdateYeastRequest] = Json.format[UpdateYeastRequest]
  implicit val responseFormat: Format[YeastResponse] = Json.format[YeastResponse]
  implicit val listResponseFormat: Format[YeastListResponse] = Json.format[YeastListResponse]
}
