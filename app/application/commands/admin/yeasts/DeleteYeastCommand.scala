package application.commands.admin.yeasts

import java.util.UUID

case class DeleteYeastCommand(
  yeastId: UUID,
  reason: Option[String],
  deletedBy: UUID
)
