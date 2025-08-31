package domain.malts.model

import play.api.libs.json._

/**
 * Value Object ExtractionRate - Taux d'extraction des malts
 * Indique le pourcentage de sucres fermentescibles extractibles
 * Typique : 75-85% pour malts de base, 65-75% pour malts spéciaux
 */
final case class ExtractionRate private (value: Double) extends AnyVal {
  override def toString: String = f"$value%.1f%%"

  /**
   * Valeur en décimal (0.75 pour 75%)
   */
  def asDecimal: Double = value / 100.0

  /**
   * Points d'extraction théoriques par kg et par litre
   * Formule: (taux/100) × 383 points/kg/L (constante malterie)
   */
  def extractionPoints: Double = (value / 100.0) * 383.0

  /**
   * Classification du pouvoir d'extraction
   */
  def extractionCategory: String = value match {
    case v if v < 70   => "Faible"      // Malts très spéciaux
    case v if v < 75   => "Modéré"      // Malts spéciaux
    case v if v < 80   => "Bon"         // Malts de base standards
    case v if v < 85   => "Très Bon"    // Excellents malts de base
    case _             => "Exceptionnel" // Malts premium
  }

  /**
   * Indique si ce malt peut servir de base (>75% extraction)
   */
  def canBeBaseMalt: Boolean = value >= 75.0

  /**
   * Calcul du rendement approximatif en gravité spécifique
   * Pour 1kg de malt dans 1L d'eau
   */
  def specificGravityContribution: Double = 1.000 + (extractionPoints / 1000.0)

  /**
   * Comparaison d'efficacité avec un autre malt
   */
  def efficiencyVs(other: ExtractionRate): Double = {
    (this.value / other.value - 1.0) * 100.0
  }
}

object ExtractionRate {
  def apply(value: Double): Either[String, ExtractionRate] = {
    if (value.isNaN || value.isInfinite) {
      Left("Taux d'extraction invalide (NaN ou infini)")
    } else if (value < 50.0) {
      Left("Le taux d'extraction ne peut pas être inférieur à 50% (limite pratique)")
    } else if (value > 90.0) {
      Left("Le taux d'extraction ne peut pas dépasser 90% (limite théorique)")
    } else {
      Right(new ExtractionRate(value))
    }
  }

  /**
   * Création depuis valeur décimale (0.75 → 75%)
   */
  def fromDecimal(decimal: Double): Either[String, ExtractionRate] = {
    apply(decimal * 100.0)
  }

  /**
   * Création depuis points d'extraction
   * Formule inverse: taux = (points / 383) × 100
   */
  def fromExtractionPoints(points: Double): Either[String, ExtractionRate] = {
    val percentage = (points / 383.0) * 100.0
    apply(percentage)
  }

  def fromString(str: String): Either[String, ExtractionRate] = {
    try {
      val cleanStr = str.trim
        .replace("%", "")
        .replace("percent", "")
        .trim
      val value = cleanStr.toDouble
      apply(value)
    } catch {
      case _: NumberFormatException =>
        Left(s"Format de taux d'extraction invalide: '$str'")
    }
  }

  // Constantes typiques par type de malt
  val PILSNER_MALT = new ExtractionRate(82.0)      // Pilsner malt premium
  val PALE_ALE_MALT = new ExtractionRate(81.0)     // Pale Ale malt
  val MUNICH_MALT = new ExtractionRate(80.0)       // Munich malt
  val VIENNA_MALT = new ExtractionRate(79.0)       // Vienna malt
  val WHEAT_MALT = new ExtractionRate(83.0)        // Wheat malt (très extractible)
  val CRYSTAL_MALT = new ExtractionRate(72.0)      // Crystal/Caramel malts
  val CHOCOLATE_MALT = new ExtractionRate(68.0)    // Chocolate malt
  val ROASTED_BARLEY = new ExtractionRate(65.0)    // Roasted barley (faible)

  implicit val extractionRateFormat: Format[ExtractionRate] = new Format[ExtractionRate] {
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