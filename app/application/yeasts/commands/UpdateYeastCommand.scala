package application.yeasts.commands

import play.api.libs.json._

/**
 * Command pour mettre à jour une levure existante
 */
case class UpdateYeastCommand(
  yeastId: String,
  name: String,
  strain: String,
  laboratory: String,
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
  notes: Option[String],
  updatedBy: Option[String] = None
)

object UpdateYeastCommand {
  implicit val format: Format[UpdateYeastCommand] = Json.format[UpdateYeastCommand]
}