package application.commands.admin.malts

import domain.common.DomainError
import play.api.libs.json._

/**
 * Commande pour mettre à jour un malt
 */
case class UpdateMaltCommand(
  id: String,
  name: Option[String] = None,
  maltType: Option[String] = None,
  ebcColor: Option[Double] = None,
  extractionRate: Option[Double] = None,
  diastaticPower: Option[Double] = None,
  originCode: Option[String] = None,
  description: Option[String] = None,
  flavorProfiles: Option[List[String]] = None
) {

  def validate(): Either[DomainError, UpdateMaltCommand] = {
    if (id.trim.isEmpty) {
      Left(DomainError.validation("ID ne peut pas être vide"))
    } else if (name.exists(_.trim.isEmpty)) {
      Left(DomainError.validation("Le nom ne peut pas être vide"))
    } else if (ebcColor.exists(color => color < 0 || color > 1000)) {
      Left(DomainError.validation("La couleur EBC doit être entre 0 et 1000"))
    } else if (extractionRate.exists(rate => rate < 0 || rate > 100)) {
      Left(DomainError.validation("Le taux d'extraction doit être entre 0 et 100%"))
    } else if (diastaticPower.exists(power => power < 0 || power > 200)) {
      Left(DomainError.validation("Le pouvoir diastasique doit être entre 0 et 200"))
    } else {
      Right(this)
    }
  }
}

object UpdateMaltCommand {
  implicit val format: Format[UpdateMaltCommand] = Json.format[UpdateMaltCommand]
}
