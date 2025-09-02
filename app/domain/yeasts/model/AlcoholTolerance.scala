package domain.yeasts.model

import play.api.libs.json._

case class AlcoholTolerance private(value: Double) extends AnyVal {
  def canFerment(targetAbv: Double): Boolean = value >= targetAbv
  def percentage: Double = value  // Propriété manquante
}

object AlcoholTolerance {
  def fromDouble(value: Double): Either[String, AlcoholTolerance] = {
    if (value < 0 || value > 25) Left("Alcohol tolerance must be between 0 and 25%")
    else Right(AlcoholTolerance(value))
  }
  
  def unsafe(value: Double): AlcoholTolerance = AlcoholTolerance(value)
  
  implicit val format: Format[AlcoholTolerance] = Format(
    Reads(json => json.validate[Double].flatMap(d => 
      fromDouble(d).fold(JsError(_), JsSuccess(_))
    )),
    Writes(at => JsNumber(at.value))
  )
}
