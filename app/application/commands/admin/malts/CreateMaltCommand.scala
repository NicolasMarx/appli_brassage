package application.commands.admin.malts

case class CreateMaltCommand(
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String] = None,
  source: String = "MANUAL",
  flavorProfiles: List[String] = List.empty
)
