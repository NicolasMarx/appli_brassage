package application.commands.admin.malts

case class DeleteMaltCommand(
  id: String,
  reason: Option[String] = None
)
