package application.commands.admin.yeasts

import java.util.UUID

case class ActivateYeastCommand(
  yeastId: UUID,
  activatedBy: UUID
)
