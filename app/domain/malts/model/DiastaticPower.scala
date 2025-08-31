package domain.malts.model

import play.api.libs.json._

/**
 * Value Object DiastaticPower - Pouvoir diastasique avec validation métier
 */
final case class DiastaticPower private (value: Double) extends AnyVal {
  def canConvertAdjuncts: Boolean = value >= 30
  def isHighEnzyme: Boolean = value >= 100
  def isLowEnzyme: Boolean = value <= 20
  def enzymePowerCategory: String = DiastaticPower.getCategory(value)
}

object DiastaticPower {
  
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value < 0) {
      Left(s"Le pouvoir diastasique ne peut être négatif: $value")
    } else if (value > 200) {
      Left(s"Le pouvoir diastasique semble irréaliste: $value (max recommandé: 200)")
    } else {
      Right(new DiastaticPower(value))
    }
  }
  
  def getCategory(power: Double): String = power match {
    case v if v >= 100 => "Très élevé"
    case v if v >= 50  => "Élevé"
    case v if v >= 30  => "Moyen"
    case v if v >= 10  => "Faible"
    case _             => "Négligeable"
  }
  
  // Constantes typiques
  def BASE_MALT_HIGH: DiastaticPower = DiastaticPower(140.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def BASE_MALT_STANDARD: DiastaticPower = DiastaticPower(100.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def SELF_CONVERTING: DiastaticPower = DiastaticPower(30.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def CRYSTAL_TYPICAL: DiastaticPower = DiastaticPower(0.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def ROASTED_TYPICAL: DiastaticPower = DiastaticPower(0.0).getOrElse(throw new Exception("Invalid diastatic power"))
  
  implicit val format: Format[DiastaticPower] = new Format[DiastaticPower] {
    def reads(json: JsValue): JsResult[DiastaticPower] = {
      json.validate[Double].flatMap { value =>
        DiastaticPower(value) match {
          case Right(power) => JsSuccess(power)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(power: DiastaticPower): JsValue = JsNumber(power.value)
  }
}
