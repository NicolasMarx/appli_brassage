// app/domain/shared/NonEmptyString.scala
package domain.shared

import play.api.libs.json._

final case class NonEmptyString private (value: String) extends AnyVal {
  override def toString: String = value
  def length: Int = value.length
}

object NonEmptyString {
  def apply(value: String): Either[String, NonEmptyString] = {
    val trimmed = if (value == null) "" else value.trim

    if (trimmed.isEmpty) {
      Left("La valeur ne peut pas être vide")
    } else {
      Right(new NonEmptyString(trimmed))
    }
  }

  def fromString(value: String): NonEmptyString = {
    new NonEmptyString(if (value == null) "" else value.trim)
  }

  implicit val nonEmptyStringFormat: Format[NonEmptyString] = Json.valueFormat[NonEmptyString]
}