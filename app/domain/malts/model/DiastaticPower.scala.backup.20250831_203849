package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour pouvoir diastasique (Lintner degrees)
 */
case class DiastaticPower private(value: Double) extends AnyVal {
  def canConvertAdjuncts: Boolean = value > 80
  def enzymaticCategory: String = {
    if (value > 80) "HIGH"
    else if (value > 0) "MEDIUM"
    else "NONE"
  }
}

object DiastaticPower {
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value < 0) {
      Left("Le pouvoir diastasique ne peut pas être négatif")
    } else {
      Right(new DiastaticPower(value))
    }
  }

  def unsafe(value: Double): DiastaticPower = new DiastaticPower(value)

  implicit val format: Format[DiastaticPower] = Json.valueFormat[DiastaticPower]
}
