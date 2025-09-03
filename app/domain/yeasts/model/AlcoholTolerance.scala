package domain.yeasts.model

import play.api.libs.json._

case class AlcoholTolerance private(value: Double) extends AnyVal {
  def canFerment(targetAbv: Double): Boolean = value >= targetAbv
  def percentage: Double = value  // Propriété manquante
  def category: String = value match {
    case v if v < 6 => "Low"
    case v if v < 10 => "Medium" 
    case v if v < 15 => "High"
    case _ => "Very High"
  }
}

object AlcoholTolerance {
  def fromDouble(value: Double): Either[String, AlcoholTolerance] = {
    if (value < 0 || value > 25) Left("Alcohol tolerance must be between 0 and 25%")
    else Right(new AlcoholTolerance(value))
  }
  
  def apply(value: Int): AlcoholTolerance = new AlcoholTolerance(value.toDouble)
  def apply(value: Double): AlcoholTolerance = new AlcoholTolerance(value)
  
  def unsafe(value: Double): AlcoholTolerance = new AlcoholTolerance(value)
  
  implicit val format: Format[AlcoholTolerance] = Format(
    Reads(json => json.validate[Double].flatMap(d => 
      fromDouble(d).fold(JsError(_), JsSuccess(_))
    )),
    Writes(at => JsNumber(at.value))
  )
}
