package application.commands.admin.yeasts

import java.util.UUID

case class DeactivateYeastCommand(
  yeastId: UUID,
  reason: Option[String],
  deactivatedBy: UUID
)
