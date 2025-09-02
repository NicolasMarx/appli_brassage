package application.commands.admin.yeasts

import java.util.UUID

case class CreateYeastCommand(
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
  notes: Option[String],
  createdBy: UUID
)
