package domain.recipes.model

import play.api.libs.json._
import java.time.Instant

/**
 * Résultat des calculs de chimie de l'eau
 * Contient les analyses pH, minéraux et ajustements recommandés
 */
case class WaterChemistryResult(
  // pH et acidité
  estimatedMashPH: PHResult,
  acidAddition: Option[AcidAddition],
  
  // Profil minéral
  sourceWaterProfile: WaterProfile,
  targetWaterProfile: WaterProfile,
  adjustedWaterProfile: WaterProfile,
  
  // Ajustements recommandés
  mineralAdditions: List[MineralAddition],
  
  // Analyses et avertissements
  waterQualityScore: Double, // 0.0 - 1.0
  warnings: List[String] = List.empty,
  recommendations: List[String] = List.empty,
  
  // Métadonnées
  calculatedAt: Instant = Instant.now(),
  method: String = "Troester_Advanced"
) {
  
  def isOptimal: Boolean = waterQualityScore >= 0.8 && warnings.isEmpty
  def hasWarnings: Boolean = warnings.nonEmpty
  
  def totalMineralCost: Double = mineralAdditions.map(_.estimatedCost).sum
  
  def summary: String = {
    s"pH: ${estimatedMashPH.formatted} | Quality: ${(waterQualityScore * 100).toInt}% | " +
    s"Adjustments: ${mineralAdditions.size}"
  }
}

case class PHResult(
  value: Double,
  confidence: Double, // 0.0 - 1.0
  method: String,
  isOptimal: Boolean
) {
  def formatted: String = f"$value%.2f"
  def confidenceFormatted: String = f"${confidence * 100}%.0f%%"
}

case class AcidAddition(
  acidType: String, // "Lactic", "Phosphoric", "Citric"
  amountMl: Double,
  targetPH: Double,
  estimatedPH: Double
)

case class WaterProfile(
  name: String,
  calcium: Double,     // ppm Ca2+
  magnesium: Double,   // ppm Mg2+
  sodium: Double,      // ppm Na+
  sulfate: Double,     // ppm SO4-2
  chloride: Double,    // ppm Cl-
  bicarbonate: Double, // ppm HCO3-
  ph: Option[Double] = None
) {
  
  def hardness: Double = (calcium * 2.5) + (magnesium * 4.1) // ppm CaCO3 equivalent
  def totalHardness: Double = hardness // Alias for compatibility
  def alkalinity: Double = bicarbonate * 0.82 // ppm CaCO3 equivalent
  def sulfateToChlorideRatio: Double = if (chloride > 0) sulfate / chloride else sulfate
  def sulfateChlorideRatio: Double = sulfateToChlorideRatio // Alias for compatibility
  def residualAlkalinity: Double = bicarbonate - (calcium / 3.5) - (magnesium / 7.0)
  
  def style: String = sulfateToChlorideRatio match {
    case ratio if ratio > 2.0 => "Hoppy (sulfate dominant)"
    case ratio if ratio < 0.5 => "Malty (chloride dominant)" 
    case _ => "Balanced"
  }
}

object WaterProfile {
  // Famous water profiles
  val BURTON_ON_TRENT = WaterProfile("Burton-on-Trent", 295, 45, 55, 725, 25, 300)
  val PILSEN = WaterProfile("Pilsen", 7, 2, 2, 5, 5, 15)
  val DUBLIN = WaterProfile("Dublin", 115, 4, 12, 55, 19, 280)
  val MUNICH = WaterProfile("Munich", 75, 18, 2, 10, 2, 152)
  val DORTMUND = WaterProfile("Dortmund", 225, 40, 60, 120, 60, 180)
  val LONDON = WaterProfile("London", 52, 32, 86, 32, 34, 104)
  
  // Aliases for compatibility with WaterChemistryCalculator
  val pilsner = PILSEN
  val burton = BURTON_ON_TRENT  
  val dublin = DUBLIN
  val munich = MUNICH
  val balanced = WaterProfile("Balanced", 100, 15, 10, 150, 50, 120)
  
  implicit val format: Format[WaterProfile] = Json.format[WaterProfile]
}

case class MineralAddition(
  mineralName: String,
  amountGrams: Double,
  contribution: Map[String, Double], // Ion -> ppm added
  purpose: String,
  estimatedCost: Double = 0.0
)

object WaterChemistryResult {
  
  def empty: WaterChemistryResult = {
    val emptyProfile = WaterProfile("Unknown", 0, 0, 0, 0, 0, 0)
    WaterChemistryResult(
      estimatedMashPH = PHResult(5.4, 0.0, "none", false),
      acidAddition = None,
      sourceWaterProfile = emptyProfile,
      targetWaterProfile = emptyProfile,
      adjustedWaterProfile = emptyProfile,
      mineralAdditions = List.empty,
      waterQualityScore = 0.0,
      warnings = List("No water analysis performed")
    )
  }
  
  // JSON support
  implicit val phResultFormat: Format[PHResult] = Json.format[PHResult]
  implicit val acidAdditionFormat: Format[AcidAddition] = Json.format[AcidAddition]
  implicit val mineralAdditionFormat: Format[MineralAddition] = Json.format[MineralAddition]
  implicit val format: Format[WaterChemistryResult] = Json.format[WaterChemistryResult]
}