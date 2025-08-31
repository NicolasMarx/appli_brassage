package domain.shared

import play.api.libs.json._

case class NonEmptyString(value: String) extends AnyVal

object NonEmptyString {
  def apply(value: String): NonEmptyString = {
    val trimmed = value.trim
    require(trimmed.nonEmpty, "String cannot be empty")
    new NonEmptyString(trimmed)
  }

  def create(value: String): Either[String, NonEmptyString] = {
    val trimmed = value.trim
    if (trimmed.nonEmpty) Right(NonEmptyString(trimmed))
    else Left("String cannot be empty")
  }
  
  // ❗ Format JSON manquant ajouté
  implicit val format: Format[NonEmptyString] = new Format[NonEmptyString] {
    def reads(json: JsValue): JsResult[NonEmptyString] = {
      json.validate[String].flatMap { str =>
        create(str) match {
          case Right(nonEmptyString) => JsSuccess(nonEmptyString)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(nonEmptyString: NonEmptyString): JsValue = JsString(nonEmptyString.value)
  }
}
