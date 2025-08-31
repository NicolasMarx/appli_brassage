package domain.malts.model

import play.api.libs.json._
import scala.util.Try

/**
 * Value Object pour pouvoir diastasique des malts
 * Version complète avec toutes les propriétés requises
 */
case class DiastaticPower private(value: Double) extends AnyVal {
  
  def canConvertAdjuncts: Boolean = value >= 35
  
  def enzymaticCategory: String = {
    if (value >= 140) "Très élevé"
    else if (value >= 100) "Élevé"
    else if (value >= 50) "Moyen"
    else if (value >= 20) "Faible"
    else "Très faible"
  }
  
  // ALIAS pour compatibility avec le code existant
  def enzymePowerCategory: String = enzymaticCategory
}

object DiastaticPower {
  
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value < 0) {
      Left("Le pouvoir diastasique ne peut pas être négatif")
    } else if (value > 200) {
      Left("Le pouvoir diastasique ne peut pas dépasser 200")
    } else if (!value.isFinite) {
      Left("Le pouvoir diastasique doit être un nombre valide")
    } else {
      Right(new DiastaticPower(value))
    }
  }
  
  def fromDouble(value: Double): Option[DiastaticPower] = apply(value).toOption
  
  def fromString(value: String): Option[DiastaticPower] = {
    Try(value.toDouble).toOption.flatMap(fromDouble)
  }
  
  // Méthode unsafe pour bypasser validation
  def unsafe(value: Any): DiastaticPower = {
    val doubleValue = value match {
      case d: Double if d.isFinite => d
      case f: Float if f.isFinite => f.toDouble
      case n: Number => n.doubleValue()
      case s: String => Try(s.toDouble).getOrElse(100.0)
      case _ => 100.0
    }
    
    val clampedValue = math.max(0, math.min(200, doubleValue))
    if (clampedValue != doubleValue) {
      println(s"⚠️  DiastaticPower.unsafe: valeur '$value' corrigée en $clampedValue")
    }
    new DiastaticPower(clampedValue)
  }
  
  val toOption: Either[String, DiastaticPower] => Option[DiastaticPower] = _.toOption
  
  implicit val format: Format[DiastaticPower] = Json.valueFormat[DiastaticPower]
}
