package domain.malts.model

import play.api.libs.json._

/**
 * Value Object ExtractionRate - Taux d'extraction avec validation et catégorisation
 */
final case class ExtractionRate private (value: Double) extends AnyVal {
  def percentage: Double = value * 100
  def extractionCategory: String = ExtractionRate.getCategory(value)
  def isHighExtraction: Boolean = value >= 0.82
  def isLowExtraction: Boolean = value <= 0.75
}

object ExtractionRate {
  
  def apply(value: Double): Either[String, ExtractionRate] = {
    if (value < 0.5) {
      Left(s"Le taux d'extraction doit être au moins 50%: ${value * 100}%")
    } else if (value > 1.0) {
      Left(s"Le taux d'extraction ne peut excéder 100%: ${value * 100}%")
    } else {
      Right(new ExtractionRate(value))
    }
  }
  
  def fromPercentage(percentage: Double): Either[String, ExtractionRate] = {
    apply(percentage / 100.0)
  }
  
  def getCategory(rate: Double): String = rate match {
    case v if v >= 0.85 => "Très élevé"
    case v if v >= 0.82 => "Élevé"
    case v if v >= 0.78 => "Moyen"
    case v if v >= 0.75 => "Faible"
    case _              => "Très faible"
  }
  
  // Constantes typiques
  def HIGH_QUALITY_BASE: ExtractionRate = ExtractionRate(0.85).getOrElse(throw new Exception("Invalid extraction"))
  def STANDARD_BASE: ExtractionRate = ExtractionRate(0.82).getOrElse(throw new Exception("Invalid extraction"))
  def CRYSTAL_AVERAGE: ExtractionRate = ExtractionRate(0.75).getOrElse(throw new Exception("Invalid extraction"))
  def ROASTED_AVERAGE: ExtractionRate = ExtractionRate(0.70).getOrElse(throw new Exception("Invalid extraction"))
  
  implicit val format: Format[ExtractionRate] = new Format[ExtractionRate] {
    def reads(json: JsValue): JsResult[ExtractionRate] = {
      json.validate[Double].flatMap { value =>
        ExtractionRate(value) match {
          case Right(rate) => JsSuccess(rate)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(rate: ExtractionRate): JsValue = JsNumber(rate.value)
  }
}
