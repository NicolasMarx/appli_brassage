package application.commands.admin.malts

import domain.malts.model._
// import domain.shared._ // Unused import removed
import domain.common.DomainError

/**
 * Commande de création d'un nouveau malt
 * Pattern CQRS propre (sans handler mélangé)
 */
case class CreateMaltCommand(
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String] = None,
  flavorProfiles: List[String] = List.empty,
  source: String = "MANUAL"
) {

  def validate(): Either[DomainError, CreateMaltCommand] = {
    if (name.trim.isEmpty) {
      Left(DomainError.validation("Le nom du malt ne peut pas être vide"))
    } else if (originCode.trim.isEmpty) {
      Left(DomainError.validation("Le code d'origine est requis"))
    } else if (flavorProfiles.length > MaltAggregate.MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum ${MaltAggregate.MaxFlavorProfiles} profils arômes"))
    } else {
      Right(this)
    }
  }
}
