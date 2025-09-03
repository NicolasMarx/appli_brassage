package domain.yeasts.model

import play.api.libs.json._

case class FermentationTemp private(min: Int, max: Int) {
  def range: Int = max - min
  def contains(temp: Int): Boolean = temp >= min && temp <= max
  def optimal: Int = (min + max) / 2
}

object FermentationTemp {
  def create(min: Int, max: Int): Either[String, FermentationTemp] = {
    if (min < 0 || max > 50) Left("Temperature must be between 0°C and 50°C")
    else if (min > max) Left("Min temperature cannot be greater than max")
    else Right(new FermentationTemp(min, max))
  }
  
  def apply(min: Int, max: Int): FermentationTemp = new FermentationTemp(min, max)
  def unsafe(min: Int, max: Int): FermentationTemp = new FermentationTemp(min, max)
  
  implicit val format: Format[FermentationTemp] = Json.format[FermentationTemp]
}
