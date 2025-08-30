// app/domain/hops/model/AlphaAcidPercentage.scala
package domain.hops.model

import play.api.libs.json._

final case class AlphaAcidPercentage private (value: Double) extends AnyVal {
  override def toString: String = f"$value%.2f%% AA"

  def asDecimal: Double = value / 100.0
  def isHigh: Boolean = value > 12.0  // Considéré comme houblon amérisant
  def isMedium: Boolean = value >= 6.0 && value <= 12.0
  def isLow: Boolean = value < 6.0    // Considéré comme houblon aromatique

  def category: String = {
    if (isHigh) "High Alpha (Bittering)"
    else if (isMedium) "Medium Alpha (Dual Purpose)"
    else "Low Alpha (Aroma)"
  }
}

object AlphaAcidPercentage {
  val MinValue = 0.0
  val MaxValue = 25.0  // Maximum réaliste pour les houblons

  def apply(value: Double): Either[String, AlphaAcidPercentage] = {
    if (value.isNaN || value.isInfinite) {
      Left("Alpha acid percentage invalide (NaN ou infini)")
    } else if (value < MinValue) {
      Left("Alpha acid percentage ne peut pas être négatif")
    } else if (value > MaxValue) {
      Left(s"Alpha acid percentage trop élevé (max ${MaxValue}%)")
    } else {
      Right(new AlphaAcidPercentage(value))
    }
  }

  def fromString(str: String): Either[String, AlphaAcidPercentage] = {
    try {
      val cleanStr = str.trim.replace("%", "").replace("AA", "")
      val value = cleanStr.toDouble
      apply(value)
    } catch {
      case _: NumberFormatException =>
        Left(s"Format alpha acid invalide: '$str'")
    }
  }

  def zero: AlphaAcidPercentage = new AlphaAcidPercentage(0.0)
  def high(value: Double): Either[String, AlphaAcidPercentage] = apply(value)
  def medium(value: Double): Either[String, AlphaAcidPercentage] = apply(value)
  def low(value: Double): Either[String, AlphaAcidPercentage] = apply(value)

  implicit val alphaAcidFormat: Format[AlphaAcidPercentage] = new Format[AlphaAcidPercentage] {
    def reads(json: JsValue): JsResult[AlphaAcidPercentage] = {
      json.validate[Double].flatMap { value =>
        AlphaAcidPercentage(value) match {
          case Right(aa) => JsSuccess(aa)
          case Left(error) => JsError(error)
        }
      }
    }

    def writes(aa: AlphaAcidPercentage): JsValue = JsNumber(aa.value)
  }
}