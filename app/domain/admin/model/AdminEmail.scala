package domain.admin.model

import domain.shared.ValueObject
import play.api.libs.json._
import scala.util.matching.Regex

/**
 * Value Object pour l'email des administrateurs
 * Validation de format email avec contraintes métier
 */
case class AdminEmail private(value: String) extends ValueObject {
  override def toString: String = value
}

object AdminEmail {
  
  private val EmailRegex: Regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".r
  
  /**
   * Création avec validation
   */
  def fromString(email: String): Either[String, AdminEmail] = {
    if (email == null || email.trim.isEmpty) {
      Left("L'email ne peut pas être vide")
    } else {
      val trimmed = email.trim.toLowerCase
      EmailRegex.findFirstIn(trimmed) match {
        case Some(_) => Right(AdminEmail(trimmed))
        case None => Left(s"Format d'email invalide: $email")
      }
    }
  }
  
  /**
   * Création unsafe pour les tests
   */
  def unsafe(email: String): AdminEmail = AdminEmail(email)
  
  implicit val format: Format[AdminEmail] = Format(
    Reads(json => json.validate[String].flatMap { str =>
      fromString(str).fold(JsError(_), JsSuccess(_))
    }),
    Writes(email => JsString(email.value))
  )
}