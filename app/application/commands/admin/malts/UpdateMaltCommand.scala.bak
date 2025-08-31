package application.commands.admin.malts

import domain.common.DomainError
import domain.shared.NonEmptyString

case class UpdateMaltCommand(
  id: String,
  name: Option[String] = None,
  description: Option[String] = None,
  ebcColor: Option[Double] = None,
  extractionRate: Option[Double] = None,
  diastaticPower: Option[Double] = None,
  flavorProfiles: Option[List[String]] = None,
  status: Option[String] = None
) {
  
  def validate(): Either[DomainError, UpdateMaltCommand] = {
    // Validation basique
    if (id.trim.isEmpty) {
      Left(DomainError.validation("ID ne peut pas être vide", "id"))
    } else {
      Right(this)
    }
  }
}
