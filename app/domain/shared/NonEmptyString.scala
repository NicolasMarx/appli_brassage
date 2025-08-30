package domain.shared

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
}