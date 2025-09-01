package domain.shared

import play.api.libs.json._

/**
 * Value Object pour chaînes non vides
 * Version corrigée sans imports inutilisés
 */
case class NonEmptyString private(value: String) extends AnyVal {
  override def toString: String = value
}

object NonEmptyString {
  
  def create(value: String): Either[String, NonEmptyString] = {
    if (value == null || value.trim.isEmpty) {
      Left("La chaîne ne peut pas être vide")
    } else {
      Right(new NonEmptyString(value.trim))
    }
  }
  
  def apply(value: String): NonEmptyString = {
    create(value).getOrElse(
      throw new IllegalArgumentException(s"Chaîne vide non autorisée : '$value'")
    )
  }
  
  // Méthode unsafe pour bypasser validation (debug uniquement)
  def unsafe(value: String): NonEmptyString = new NonEmptyString(
    if (value == null || value.trim.isEmpty) "DEFAULT_VALUE" else value.trim
  )
  
  def fromString(value: String): Option[NonEmptyString] = create(value).toOption
  
  implicit val format: Format[NonEmptyString] = Json.valueFormat[NonEmptyString]
}
