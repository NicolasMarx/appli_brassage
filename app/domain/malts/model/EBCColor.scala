package domain.malts.model

import play.api.libs.json._

/**
 * Value Object EBCColor - Couleur EBC avec validation et calcul automatique du nom
 */
final case class EBCColor private (value: Double) extends AnyVal {
  def colorName: String = EBCColor.getColorName(value)
  def isMaltCanBeUsed: Boolean = value >= 0 && value <= 1000
  def isBaseMaltColor: Boolean = value >= 2 && value <= 8
  def isCrystalMaltColor: Boolean = value >= 20 && value <= 300
  def isRoastedMaltColor: Boolean = value >= 300
}

object EBCColor {
  
  def apply(value: Double): Either[String, EBCColor] = {
    if (value < 0) {
      Left(s"La couleur EBC doit être positive: $value")
    } else if (value > 1000) {
      Left(s"La couleur EBC ne peut excéder 1000: $value")
    } else {
      Right(new EBCColor(value))
    }
  }
  
  def getColorName(ebc: Double): String = ebc match {
    case v if v <= 4   => "Très pâle"
    case v if v <= 8   => "Pâle"
    case v if v <= 12  => "Doré"
    case v if v <= 25  => "Ambre"
    case v if v <= 50  => "Cuivre"
    case v if v <= 100 => "Brun"
    case v if v <= 300 => "Brun foncé"
    case _             => "Noir"
  }
  
  // Constantes communes
  def PILSNER: EBCColor = EBCColor(3.5).getOrElse(throw new Exception("Invalid EBC"))
  def WHEAT: EBCColor = EBCColor(4.0).getOrElse(throw new Exception("Invalid EBC"))
  def MUNICH: EBCColor = EBCColor(15.0).getOrElse(throw new Exception("Invalid EBC"))
  def CRYSTAL_40: EBCColor = EBCColor(80.0).getOrElse(throw new Exception("Invalid EBC"))
  def CHOCOLATE: EBCColor = EBCColor(900.0).getOrElse(throw new Exception("Invalid EBC"))
  def ROASTED_BARLEY: EBCColor = EBCColor(1000.0).getOrElse(throw new Exception("Invalid EBC"))
  
  implicit val format: Format[EBCColor] = new Format[EBCColor] {
    def reads(json: JsValue): JsResult[EBCColor] = {
      json.validate[Double].flatMap { value =>
        EBCColor(value) match {
          case Right(ebc) => JsSuccess(ebc)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(ebc: EBCColor): JsValue = JsNumber(ebc.value)
  }
}
