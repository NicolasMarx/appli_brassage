package application.commands.admin.yeasts

import java.util.UUID

case class ChangeYeastStatusCommand(
  yeastId: UUID,
  newStatus: String,
  reason: Option[String],
  changedBy: UUID
)
