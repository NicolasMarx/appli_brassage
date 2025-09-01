package domain.malts.model

import play.api.libs.json._
import scala.util.Try

/**
 * Value Object pour couleur EBC des malts
 * Version complète avec toutes les propriétés requises
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
  
  // NOUVELLE PROPRIÉTÉ requise par MaltAggregate
  def isMaltCanBeUsed: Boolean = {
    // Logique métier : un malt peut être utilisé s'il a une couleur valide
    value >= 0 && value <= 1000 && value.isFinite
  }
}

object EBCColor {
  
  def apply(value: Double): Either[String, EBCColor] = {
    if (value < 0) {
      Left("La couleur EBC ne peut pas être négative")
    } else if (value > 1000) {
      Left("La couleur EBC ne peut pas dépasser 1000")
    } else if (!value.isFinite) {
      Left("La couleur EBC doit être un nombre valide")
    } else {
      Right(new EBCColor(value))
    }
  }
  
  def fromDouble(value: Double): Option[EBCColor] = apply(value).toOption
  
  def fromString(value: String): Option[EBCColor] = {
    Try(value.toDouble).toOption.flatMap(fromDouble)
  }
  
  // Méthode unsafe pour bypasser validation
  def unsafe(value: Any): EBCColor = {
    val doubleValue = value match {
      case d: Double if d.isFinite => d
      case f: Float if f.isFinite => f.toDouble
      case n: Number => n.doubleValue()
      case s: String => Try(s.toDouble).getOrElse(3.0)
      case _ => 3.0
    }
    
    val clampedValue = math.max(0, math.min(1000, doubleValue))
    if (clampedValue != doubleValue) {
      println(s"⚠️  EBCColor.unsafe: valeur '$value' corrigée en $clampedValue")
    }
    new EBCColor(clampedValue)
  }
  
  val toOption: Either[String, EBCColor] => Option[EBCColor] = _.toOption
  
  implicit val format: Format[EBCColor] = Json.valueFormat[EBCColor]
}
