package domain.yeasts.model

import play.api.libs.json._

case class AttenuationRange private(min: Int, max: Int) {
  def range: Int = max - min
  def contains(value: Int): Boolean = value >= min && value <= max
}

object AttenuationRange {
  def create(min: Int, max: Int): Either[String, AttenuationRange] = {
    if (min < 30 || max > 100) Left("Attenuation must be between 30% and 100%")
    else if (min > max) Left("Min attenuation cannot be greater than max")
    else Right(AttenuationRange(min, max))
  }
  
  def unsafe(min: Int, max: Int): AttenuationRange = AttenuationRange(min, max)
  
  implicit val format: Format[AttenuationRange] = Json.format[AttenuationRange]
}
