package domain.malts.model

import play.api.libs.json._

/**
 * Value Object DiastaticPower - Pouvoir diastasique des malts
 * Mesure la capacité enzymatique à convertir l'amidon en sucres fermentescibles
 * Unité : degrés Lintner (°L) - 0 à 160+ typiquement
 */
final case class DiastaticPower private (value: Double) extends AnyVal {
  override def toString: String = f"$value%.0f°L"

  /**
   * Conversion vers unités WK (Windisch-Kolbach)
   * Formule: WK ≈ Lintner × 3.5
   */
  def toWK: Double = value * 3.5

  /**
   * Classification du pouvoir enzymatique
   */
  def enzymaticCategory: String = value match {
    case v if v == 0    => "Aucun"         // Malts kilned, caramel, roasted
    case v if v <= 35   => "Très Faible"   // Malts Munich foncés, Vienna
    case v if v <= 50   => "Faible"        // Munich clair, certains spéciaux
    case v if v <= 80   => "Modéré"        // Pale Ale, certains blés
    case v if v <= 120  => "Bon"           // Pilsner standards
    case v if v <= 160  => "Très Bon"      // Pilsner premium, 6-row
    case _              => "Exceptionnel"  // Malts enzymatiques spéciaux
  }

  /**
   * Indique si ce malt peut convertir des adjuvants (riz, maïs)
   * Nécessite généralement >100°L pour conversion efficace
   */
  def canConvertAdjuncts: Boolean = value >= 100.0

  /**
   * Indique si ce malt a suffisamment d'enzymes pour l'autoconversion
   * Minimum ~35°L pour conversion complète
   */
  def isSelfConverting: Boolean = value >= 35.0

  /**
   * Calcul du ratio malt enzymatique nécessaire dans un mélange
   * Pour convertir des malts sans pouvoir diastasique
   */
  def requiredRatioFor(totalMash: Double, targetPower: Double = 40.0): Double = {
    if (value <= 0) return 1.0 // Impossible de convertir
    math.min(1.0, targetPower / value)
  }

  /**
   * Pouvoir diastasique combiné avec un autre malt
   * Weighted average basé sur les proportions
   */
  def combinedWith(other: DiastaticPower, thisRatio: Double): DiastaticPower = {
    val otherRatio = 1.0 - thisRatio
    val combinedValue = (this.value * thisRatio) + (other.value * otherRatio)
    new DiastaticPower(combinedValue)
  }
}

object DiastaticPower {
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value.isNaN || value.isInfinite) {
      Left("Pouvoir diastasique invalide (NaN ou infini)")
    } else if (value < 0.0) {
      Left("Le pouvoir diastasique ne peut pas être négatif")
    } else if (value > 200.0) {
      Left("Le pouvoir diastasique ne peut pas dépasser 200°L (limite pratique)")
    } else {
      Right(new DiastaticPower(value))
    }
  }

  /**
   * Création depuis unités WK (Windisch-Kolbach)
   * Formule: Lintner ≈ WK / 3.5
   */
  def fromWK(wk: Double): Either[String, DiastaticPower] = {
    val lintnerValue = wk / 3.5
    apply(lintnerValue)
  }

  def fromString(str: String): Either[String, DiastaticPower] = {
    try {
      val cleanStr = str.trim.toUpperCase
        .replace("°L", "")
        .replace("LINTNER", "")
        .replace("L", "")
        .trim
      val value = cleanStr.toDouble
      apply(value)
    } catch {
      case _: NumberFormatException =>
        Left(s"Format de pouvoir diastasique invalide: '$str'")
    }
  }

  // Constantes typiques par type de malt
  val PILSNER_MALT = new DiastaticPower(105.0)       // Pilsner malt - bon pouvoir
  val SIX_ROW_MALT = new DiastaticPower(140.0)       // 6-row - pouvoir élevé
  val TWO_ROW_MALT = new DiastaticPower(100.0)       // 2-row standard
  val PALE_ALE_MALT = new DiastaticPower(85.0)       // Pale Ale malt
  val MUNICH_LIGHT = new DiastaticPower(45.0)        // Munich clair
  val MUNICH_DARK = new DiastaticPower(25.0)         // Munich foncé
  val VIENNA_MALT = new DiastaticPower(35.0)         // Vienna malt
  val WHEAT_MALT = new DiastaticPower(120.0)         // Wheat malt - pouvoir élevé
  val CRYSTAL_MALT = new DiastaticPower(0.0)         // Crystal - aucun pouvoir
  val CHOCOLATE_MALT = new DiastaticPower(0.0)       // Chocolate - aucun pouvoir
  val ROASTED_BARLEY = new DiastaticPower(0.0)       // Roasted - aucun pouvoir

  // Seuils critiques pour formulation recettes
  val MINIMUM_SELF_CONVERSION = new DiastaticPower(35.0)
  val MINIMUM_ADJUNCT_CONVERSION = new DiastaticPower(100.0)
  val OPTIMAL_ALL_GRAIN = new DiastaticPower(80.0)

  implicit val diastaticPowerFormat: Format[DiastaticPower] = new Format[DiastaticPower] {
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