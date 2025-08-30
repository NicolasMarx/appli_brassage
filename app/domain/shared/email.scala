// app/domain/shared/Email.scala
package domain.shared

import play.api.libs.json._

/**
 * Value Object Email avec validation stricte
 * Respecte les principes DDD : immutable, auto-validant, comparaison par valeur
 */
final case class Email private (value: String) extends AnyVal {
  override def toString: String = value
}

object Email {
  private val EmailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".r
  
  def apply(value: String): Either[String, Email] = {
    val trimmed = value.trim.toLowerCase
    
    if (trimmed.isEmpty) {
      Left("Email ne peut pas être vide")
    } else if (trimmed.length > 254) { // RFC 5321
      Left("Email trop long (max 254 caractères)")
    } else if (!EmailRegex.matches(trimmed)) {
      Left("Format email invalide")
    } else {
      Right(new Email(trimmed))
    }
  }
  
  def fromString(value: String): Email = {
    apply(value) match {
      case Right(email) => email
      case Left(error) => throw new IllegalArgumentException(s"Email invalide: $error")
    }
  }
  
  implicit val emailFormat: Format[Email] = new Format[Email] {
    def reads(json: JsValue): JsResult[Email] = {
      json.validate[String].flatMap { str =>
        Email(str) match {
          case Right(email) => JsSuccess(email)
          case Left(error) => JsError(s"Email invalide: $error")
        }
      }
    }
    
    def writes(email: Email): JsValue = JsString(email.value)
  }
}