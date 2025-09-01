package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour taux d'extraction des malts avec catégories
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
    } else {
      Right(new ExtractionRate(value))
    }
  }
  
  def unsafe(value: Double): ExtractionRate = new ExtractionRate(value)
  
  implicit val format: Format[ExtractionRate] = Json.valueFormat[ExtractionRate]
}
