package application.commands.admin.malts

import domain.common.DomainError

/**
 * Commande de suppression d'un malt
 */
case class DeleteMaltCommand(
                              id: String,
                              reason: Option[String] = None,
                              forceDelete: Boolean = false
                            ) {

  def validate(): Either[DomainError, DeleteMaltCommand] = {
    if (id.trim.isEmpty) {
      Left(DomainError.validation("L'ID du malt est requis"))
    } else {
      Right(this)
    }
  }
}