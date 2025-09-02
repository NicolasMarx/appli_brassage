package application.yeasts.commands

import play.api.libs.json._
import java.util.UUID

/**
 * Command pour créer une nouvelle levure
 * Suit les principes CQRS du projet
 */
case class CreateYeastCommand(
  name: String,
  strain: String,
  yeastType: String,
  laboratory: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String] = List.empty,
  flavorProfile: List[String] = List.empty,
  esters: List[String] = List.empty,
  phenols: List[String] = List.empty,
  otherCompounds: List[String] = List.empty,
  notes: Option[String] = None,
  createdBy: Option[String] = None
)

object CreateYeastCommand {
  implicit val format: Format[CreateYeastCommand] = Json.format[CreateYeastCommand]
}