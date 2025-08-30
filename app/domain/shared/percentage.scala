// app/domain/shared/Percentage.scala
package domain.shared

import play.api.libs.json._

/**
 * Value Object Percentage avec validation des bornes
 * Utilisé pour alpha/beta acids, etc. (0.0 à 100.0)
 */
final case class Percentage private (value: Double) extends AnyVal {
  override def toString: String = f"$value%.2f%%"
  
  def asDecimal: Double = value / 100.0
  def +(other: Percentage): Percentage = new Percentage(value + other.value)
  def -(other: Percentage): Percentage = new Percentage(value - other.value)
  def isZero: Boolean = value == 0.0
}

object Percentage {
  def apply(value: Double): Either[String, Percentage] = {
    if (value.isNaN || value.isInfinite) {
      Left("Pourcentage invalide (NaN ou infini)")
    } else if (value < 0.0) {
      Left("Le pourcentage ne peut pas être négatif")
    } else if (value > 100.0) {
      Left("Le pourcentage ne peut pas dépasser 100%")
    } else {
      Right(new Percentage(value))
    }
  }
  
  def fromDecimal(decimal: Double): Either[String, Percentage] = {
    apply(decimal * 100.0)
  }
  
  def zero: Percentage = new Percentage(0.0)
  
  def fromString(str: String): Either[String, Percentage] = {
    try {
      val cleanStr = str.trim.replace("%", "")
      val value = cleanStr.toDouble
      apply(value)
    } catch {
      case _: NumberFormatException =>
        Left(s"Format de pourcentage invalide: '$str'")
    }
  }
  
  implicit val percentageFormat: Format[Percentage] = new Format[Percentage] {
    def reads(json: JsValue): JsResult[Percentage] = {
      json.validate[Double].flatMap { value =>
        Percentage(value) match {
          case Right(percentage) => JsSuccess(percentage)
          case Left(error) => JsError(error)
        }
      }
    }
    
    def writes(percentage: Percentage): JsValue = JsNumber(percentage.value)
  }
}