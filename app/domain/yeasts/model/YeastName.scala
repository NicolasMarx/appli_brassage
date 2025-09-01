package domain.yeasts.model

import domain.shared.NonEmptyString
import play.api.libs.json._

case class YeastName private(value: String) extends AnyVal

object YeastName {
  def fromString(value: String): Either[String, YeastName] = {
    NonEmptyString.fromString(value) match {
      case Some(nes) => Right(YeastName(nes.value))
      case None => Left("Yeast name cannot be empty")
    }
  }
  
  def unsafe(value: String): YeastName = YeastName(value)
  
  implicit val format: Format[YeastName] = Format(
    Reads(json => json.validate[String].flatMap(s => 
      fromString(s).fold(JsError(_), JsSuccess(_))
    )),
    Writes(yn => JsString(yn.value))
  )
}
