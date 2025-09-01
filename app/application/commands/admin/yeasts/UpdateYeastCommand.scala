package application.commands.admin.yeasts

import java.util.UUID

case class UpdateYeastCommand(
  yeastId: UUID,
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
  notes: Option[String],
  updatedBy: UUID
)
