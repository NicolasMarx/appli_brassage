package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour couleur EBC des malts avec noms de couleurs
 */
case class EBCColor private(value: Double) extends AnyVal {
  def toSRM: Double = value * 0.508
  
  def colorName: String = {
    if (value <= 3) "Très pâle"
    else if (value <= 8) "Pâle"
    else if (value <= 16) "Doré"
    else if (value <= 33) "Ambré"
    else if (value <= 66) "Brun clair"
    else if (value <= 138) "Brun foncé"
    else "Noir"
  }
}

object EBCColor {
  
  def apply(value: Double): Either[String, EBCColor] = {
    if (value < 0) {
      Left("La couleur EBC ne peut pas être négative")
    } else if (value > 1000) {
      Left("La couleur EBC ne peut pas dépasser 1000")
    } else {
      Right(new EBCColor(value))
    }
  }
  
  def unsafe(value: Double): EBCColor = new EBCColor(value)
  
  implicit val format: Format[EBCColor] = Json.valueFormat[EBCColor]
}
