package domain.yeasts.model

import play.api.libs.json._

case class YeastStrain private(value: String) extends AnyVal

object YeastStrain {
  def fromString(value: String): Either[String, YeastStrain] = {
    if (value.trim.isEmpty) Left("Strain cannot be empty")
    else Right(new YeastStrain(value.trim))
  }
  
  def apply(value: String): YeastStrain = new YeastStrain(value)
  def unsafe(value: String): YeastStrain = new YeastStrain(value)
  
  implicit val format: Format[YeastStrain] = Format(
    Reads(json => json.validate[String].flatMap(s => 
      fromString(s).fold(JsError(_), JsSuccess(_))
    )),
    Writes(ys => JsString(ys.value))
  )
}
