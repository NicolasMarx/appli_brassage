package domain.admin.model

import domain.shared.ValueObject
import play.api.libs.json._

/**
 * Value Object pour le nom des administrateurs
 * Validation de format nom avec contraintes métier
 */
case class AdminName private(value: String) extends ValueObject {
  override def toString: String = value
}

object AdminName {
  
  /**
   * Création avec validation
   */
  def fromString(name: String): Either[String, AdminName] = {
    if (name == null || name.trim.isEmpty) {
      Left("Le nom ne peut pas être vide")
    } else {
      val trimmed = name.trim
      if (trimmed.length < 2) {
        Left("Le nom doit contenir au moins 2 caractères")
      } else if (trimmed.length > 100) {
        Left("Le nom ne peut pas dépasser 100 caractères")
      } else {
        Right(AdminName(trimmed))
      }
    }
  }
  
  /**
   * Création unsafe pour les tests
   */
  def unsafe(name: String): AdminName = AdminName(name)
  
  implicit val format: Format[AdminName] = Format(
    Reads(json => json.validate[String].flatMap { str =>
      fromString(str).fold(JsError(_), JsSuccess(_))
    }),
    Writes(name => JsString(name.value))
  )
}