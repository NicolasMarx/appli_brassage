package application.yeasts.dtos

import play.api.libs.json._

case class YeastSearchRequestDTO(
  name: Option[String],
  laboratory: Option[String],
  yeastType: Option[String],
  minAttenuation: Option[Int],
  maxAttenuation: Option[Int],
  minTemperature: Option[Int],
  maxTemperature: Option[Int],
  minAlcoholTolerance: Option[Double],
  maxAlcoholTolerance: Option[Double],
  flocculation: Option[String],
  characteristics: Option[List[String]],
  status: Option[String],
  page: Int = 0,
  size: Int = 20
)

case class CreateYeastRequestDTO(
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
  aromaProfile: List[String],
  flavorProfile: List[String],
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String]
)

case class UpdateYeastRequestDTO(
  name: String,
  laboratory: String,
  strain: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String],
  flavorProfile: List[String],
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String]
)

case class ChangeStatusRequestDTO(
  status: String,
  reason: Option[String]
)

object YeastSearchRequestDTO {
  implicit val format: Format[YeastSearchRequestDTO] = Json.format[YeastSearchRequestDTO]
}

object CreateYeastRequestDTO {
  implicit val format: Format[CreateYeastRequestDTO] = Json.format[CreateYeastRequestDTO]
}

object UpdateYeastRequestDTO {
  implicit val format: Format[UpdateYeastRequestDTO] = Json.format[UpdateYeastRequestDTO]
}

object ChangeStatusRequestDTO {
  implicit val format: Format[ChangeStatusRequestDTO] = Json.format[ChangeStatusRequestDTO]
}
