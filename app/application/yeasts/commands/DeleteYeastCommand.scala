package application.yeasts.commands

import play.api.libs.json._

/**
 * Command pour supprimer une levure (soft delete = archive)
 */
case class DeleteYeastCommand(
  yeastId: String,
  reason: Option[String] = None,
  deletedBy: Option[String] = None
)

object DeleteYeastCommand {
  implicit val format: Format[DeleteYeastCommand] = Json.format[DeleteYeastCommand]
}