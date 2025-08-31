package domain.malts.model

import play.api.libs.json._
import scala.util.Try

/**
 * Value Object pour taux d'extraction des malts
 * Version corrigée avec validation étendue
 */
case class ExtractionRate private(value: Double) extends AnyVal {
  def asPercentage: String = f"${value}%.1f%%"
  def extractionCategory: String = {
    if (value >= 82) "Très élevé"
    else if (value >= 79) "Élevé"  
    else if (value >= 75) "Moyen"
    else if (value >= 70) "Faible"
    else "Très faible"
  }
}

object ExtractionRate {
  
  def apply(value: Double): Either[String, ExtractionRate] = {
    if (value < 0) {
      Left("Le taux d'extraction ne peut pas être négatif")
    } else if (value > 100) {
      Left("Le taux d'extraction ne peut pas dépasser 100%")
    } else if (!value.isFinite) {
      Left("Le taux d'extraction doit être un nombre valide")
    } else {
      Right(new ExtractionRate(value))
    }
  }
  
  def fromDouble(value: Double): Option[ExtractionRate] = apply(value).toOption
  
  def fromString(value: String): Option[ExtractionRate] = {
    Try(value.toDouble).toOption.flatMap(fromDouble)
  }
  
  // Méthode unsafe pour bypasser validation  
  def unsafe(value: Any): ExtractionRate = {
    val doubleValue = value match {
      case d: Double if d.isFinite => d
      case f: Float if f.isFinite => f.toDouble
      case n: Number => n.doubleValue()
      case s: String => Try(s.toDouble).getOrElse(80.0)
      case _ => 80.0
    }
    
    val clampedValue = math.max(0, math.min(100, doubleValue))
    if (clampedValue != doubleValue) {
      println(s"⚠️  ExtractionRate.unsafe: valeur '$value' corrigée en $clampedValue")
    }
    new ExtractionRate(clampedValue)
  }
  
  val toOption: Either[String, ExtractionRate] => Option[ExtractionRate] = _.toOption
  
  implicit val format: Format[ExtractionRate] = Json.valueFormat[ExtractionRate]
}
