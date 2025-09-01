package application.commands.admin.yeasts

import java.util.UUID

case class ArchiveYeastCommand(
  yeastId: UUID,
  reason: Option[String],
  archivedBy: UUID
)
