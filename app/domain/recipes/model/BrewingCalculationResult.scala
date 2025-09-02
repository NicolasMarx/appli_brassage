package domain.recipes.model

import play.api.libs.json._
import java.time.Instant

/**
 * Résultat complet des calculs de brassage
 * Contient tous les calculs effectués avec détails et méthodes utilisées
 */
case class BrewingCalculationResult(
  // Calculs principaux
  originalGravity: GravityResult,
  finalGravity: GravityResult,
  alcoholByVolume: ABVResult,
  internationalBitteringUnits: IBUResult,
  standardReferenceMethod: SRMResult,
  
  // Calculs auxiliaires
  efficiency: EfficiencyResult,
  attenuation: AttenuationResult,
  calories: CaloriesResult,
  
  // Volumes et corrections
  preboilVolume: Double,
  postboilVolume: Double,
  fermentorVolume: Double,
  finalVolume: Double,
  
  // Ratios importants - CORRECTION propriétés manquantes
  bitternessTogravigtRatio: Double, // BU:GU ratio
  maltToHopRatio: Double,
  balanceRatio: Option[Double] = None, // Ratio d'équilibre général
  bitternessRatio: Option[Double] = None, // Ratio amertume spécifique
  
  // Détails par ingrédient
  maltContributions: List[MaltContribution],
  hopContributions: List[HopContribution],
  yeastContribution: Option[YeastContribution],
  
  // Métadonnées
  calculationMethod: String = "Advanced_Professional_V2",
  calculatedAt: Instant = Instant.now(),
  parametersHash: String, // Hash des paramètres pour détecter les changements
  warnings: List[String] = List.empty,
  
  // Validations
  isWithinStyleGuidelines: Option[Boolean] = None,
  styleDeviations: List[String] = List.empty
) {
  
  def ibuToOg: Double = internationalBitteringUnits.total / ((originalGravity.value - 1.000) * 1000)
  
  def isBalanced: Boolean = {
    val ratio = bitternessTogravigtRatio
    ratio >= 0.5 && ratio <= 1.2 // Généralement équilibré
  }
  
  def strengthCategory: String = alcoholByVolume.primary match {
    case abv if abv < 3.5 => "Session"
    case abv if abv < 5.0 => "Standard"
    case abv if abv < 7.0 => "Strong"
    case abv if abv < 9.0 => "Very Strong"
    case abv if abv < 12.0 => "Imperial"
    case _ => "Extreme"
  }
  
  def colorCategory: String = standardReferenceMethod.value match {
    case srm if srm < 4 => "Pale"
    case srm if srm < 6 => "Gold"
    case srm if srm < 9 => "Amber"
    case srm if srm < 15 => "Brown"
    case srm if srm < 20 => "Dark Brown"
    case srm if srm < 30 => "Dark"
    case _ => "Black"
  }
  
  def bitternessCategory: String = internationalBitteringUnits.total match {
    case ibu if ibu < 20 => "Low"
    case ibu if ibu < 40 => "Moderate"
    case ibu if ibu < 60 => "High"
    case ibu if ibu < 80 => "Very High"
    case _ => "Extreme"
  }
  
  def hasWarnings: Boolean = warnings.nonEmpty
  def isComplete: Boolean = !hasWarnings && originalGravity.isValid && finalGravity.isValid
  
  def summary: String = {
    s"OG: ${originalGravity.formatted} | FG: ${finalGravity.formatted} | " +
    s"ABV: ${alcoholByVolume.formatted} | IBU: ${internationalBitteringUnits.formatted} | " +
    s"SRM: ${standardReferenceMethod.formatted} (${colorCategory})"
  }
}

case class GravityResult(
  value: Double,
  correctedValue: Double, // Corrigé pour la température
  points: Double, // Gravity points (value - 1.000) * 1000
  method: String,
  temperature: Double,
  isValid: Boolean = true
) {
  def formatted: String = f"$value%.3f"
  def pointsFormatted: String = f"$points%.0f"
}

case class ABVResult(
  primary: Double, // Méthode principale (généralement Balling)
  secondary: Double, // Méthode de vérification
  average: Double, // Moyenne des méthodes
  method: String,
  secondaryMethod: String,
  isValid: Boolean = true
) {
  def formatted: String = f"$primary%.1f%%"
  def rangeFormatted: String = f"$primary%.1f%% - $secondary%.1f%%"
}

case class IBUResult(
  total: Double,
  rager: Double, // Formule Rager
  tinseth: Double, // Formule Tinseth
  garetz: Double, // Formule Garetz
  method: String, // Méthode principale utilisée
  isValid: Boolean = true
) {
  def formatted: String = f"$total%.0f IBU"
  def detailFormatted: String = f"$total%.0f IBU (R:${rager%.0f}, T:${tinseth%.0f}, G:${garetz%.0f})"
}

case class SRMResult(
  value: Double,
  ebc: Double, // Équivalent EBC
  method: String,
  isValid: Boolean = true
) {
  def formatted: String = f"$value%.0f SRM"
  def ebcFormatted: String = f"$ebc%.0f EBC"
}

case class EfficiencyResult(
  calculated: Double,
  expected: Double,
  variance: Double,
  isAcceptable: Boolean
) {
  def formatted: String = f"$calculated%.1f%% (expected: $expected%.1f%%)"
}

case class AttenuationResult(
  apparent: Double, // Atténuation apparente
  real: Double, // Atténuation réelle
  expected: Double, // Atténuation attendue de la levure
  variance: Double
) {
  def formatted: String = f"$apparent%.1f%% apparent, $real%.1f%% real"
}

case class CaloriesResult(
  per100ml: Double,
  perServingMl: Double,
  servingSizeMl: Double = 330.0,
  method: String = "Balling"
) {
  def formatted: String = f"${per100ml.toInt} cal/100ml, ${perServingMl.toInt} cal/serving"
}

case class MaltContribution(
  name: String,
  quantityKg: Double,
  percentage: Double,
  gravityContribution: Double, // Points de gravité contribués
  colorContribution: Double, // Contribution couleur SRM
  extractionAchieved: Double, // Efficacité d'extraction réalisée
  potentialPoints: Double // Points potentiels maximum
)

case class HopContribution(
  name: String,
  quantityGrams: Double,
  alphaAcidPercent: Double,
  boilTime: Int,
  ibuContribution: Double,
  utilizationPercent: Double, // Pourcentage d'utilisation des alpha acids
  method: String // Formule utilisée pour ce houblon
)

case class YeastContribution(
  name: String,
  expectedAttenuation: Double,
  attenuationRange: String,
  alcoholTolerance: Double,
  estimatedFinalGravity: Double
)

object BrewingCalculationResult {
  
  def empty(parametersHash: String): BrewingCalculationResult = {
    BrewingCalculationResult(
      originalGravity = GravityResult(1.000, 1.000, 0.0, "none", 20.0, false),
      finalGravity = GravityResult(1.000, 1.000, 0.0, "none", 20.0, false),
      alcoholByVolume = ABVResult(0.0, 0.0, 0.0, "none", "none", false),
      internationalBitteringUnits = IBUResult(0.0, 0.0, 0.0, 0.0, "none", false),
      standardReferenceMethod = SRMResult(0.0, 0.0, "none", false),
      efficiency = EfficiencyResult(0.0, 0.0, 0.0, false),
      attenuation = AttenuationResult(0.0, 0.0, 0.0, 0.0),
      calories = CaloriesResult(0.0, 0.0),
      preboilVolume = 0.0,
      postboilVolume = 0.0,
      fermentorVolume = 0.0,
      finalVolume = 0.0,
      bitternessTogravigtRatio = 0.0,
      maltToHopRatio = 0.0,
      balanceRatio = Some(0.0),
      bitternessRatio = Some(0.0),
      maltContributions = List.empty,
      hopContributions = List.empty,
      yeastContribution = None,
      parametersHash = parametersHash,
      warnings = List("Calculs non effectués")
    )
  }
  
  // JSON formatters
  implicit val gravityResultFormat: Format[GravityResult] = Json.format[GravityResult]
  implicit val abvResultFormat: Format[ABVResult] = Json.format[ABVResult]
  implicit val ibuResultFormat: Format[IBUResult] = Json.format[IBUResult]
  implicit val srmResultFormat: Format[SRMResult] = Json.format[SRMResult]
  implicit val efficiencyResultFormat: Format[EfficiencyResult] = Json.format[EfficiencyResult]
  implicit val attenuationResultFormat: Format[AttenuationResult] = Json.format[AttenuationResult]
  implicit val caloriesResultFormat: Format[CaloriesResult] = Json.format[CaloriesResult]
  implicit val maltContributionFormat: Format[MaltContribution] = Json.format[MaltContribution]
  implicit val hopContributionFormat: Format[HopContribution] = Json.format[HopContribution]
  implicit val yeastContributionFormat: Format[YeastContribution] = Json.format[YeastContribution]
  implicit val format: Format[BrewingCalculationResult] = new Format[BrewingCalculationResult] {
    def reads(json: JsValue): JsResult[BrewingCalculationResult] = {
      for {
        originalGravity <- (json \ "originalGravity").validate[GravityResult]
        finalGravity <- (json \ "finalGravity").validate[GravityResult]
        alcoholByVolume <- (json \ "alcoholByVolume").validate[ABVResult]
        internationalBitteringUnits <- (json \ "internationalBitteringUnits").validate[IBUResult]
        standardReferenceMethod <- (json \ "standardReferenceMethod").validate[SRMResult]
        efficiency <- (json \ "efficiency").validate[EfficiencyResult]
        attenuation <- (json \ "attenuation").validate[AttenuationResult]
        calories <- (json \ "calories").validate[CaloriesResult]
        preboilVolume <- (json \ "preboilVolume").validate[Double]
        postboilVolume <- (json \ "postboilVolume").validate[Double]
        fermentorVolume <- (json \ "fermentorVolume").validate[Double]
        finalVolume <- (json \ "finalVolume").validate[Double]
        bitternessTogravigtRatio <- (json \ "bitternessTogravigtRatio").validate[Double]
        maltToHopRatio <- (json \ "maltToHopRatio").validate[Double]
        balanceRatio <- (json \ "balanceRatio").validateOpt[Double]
        bitternessRatio <- (json \ "bitternessRatio").validateOpt[Double]
        maltContributions <- (json \ "maltContributions").validate[List[MaltContribution]]
        hopContributions <- (json \ "hopContributions").validate[List[HopContribution]]
        yeastContribution <- (json \ "yeastContribution").validateOpt[YeastContribution]
        calculationMethod <- (json \ "calculationMethod").validateOpt[String]
        calculatedAt <- (json \ "calculatedAt").validateOpt[Instant]
        parametersHash <- (json \ "parametersHash").validate[String]
        warnings <- (json \ "warnings").validateOpt[List[String]]
        isWithinStyleGuidelines <- (json \ "isWithinStyleGuidelines").validateOpt[Boolean]
        styleDeviations <- (json \ "styleDeviations").validateOpt[List[String]]
      } yield BrewingCalculationResult(
        originalGravity = originalGravity,
        finalGravity = finalGravity,
        alcoholByVolume = alcoholByVolume,
        internationalBitteringUnits = internationalBitteringUnits,
        standardReferenceMethod = standardReferenceMethod,
        efficiency = efficiency,
        attenuation = attenuation,
        calories = calories,
        preboilVolume = preboilVolume,
        postboilVolume = postboilVolume,
        fermentorVolume = fermentorVolume,
        finalVolume = finalVolume,
        bitternessTogravigtRatio = bitternessTogravigtRatio,
        maltToHopRatio = maltToHopRatio,
        balanceRatio = balanceRatio,
        bitternessRatio = bitternessRatio,
        maltContributions = maltContributions,
        hopContributions = hopContributions,
        yeastContribution = yeastContribution,
        calculationMethod = calculationMethod.getOrElse("Advanced_Professional_V2"),
        calculatedAt = calculatedAt.getOrElse(Instant.now()),
        parametersHash = parametersHash,
        warnings = warnings.getOrElse(List.empty),
        isWithinStyleGuidelines = isWithinStyleGuidelines,
        styleDeviations = styleDeviations.getOrElse(List.empty)
      )
    }
    
    def writes(result: BrewingCalculationResult): JsValue = Json.obj(
      "originalGravity" -> Json.toJson(result.originalGravity),
      "finalGravity" -> Json.toJson(result.finalGravity),
      "alcoholByVolume" -> Json.toJson(result.alcoholByVolume),
      "internationalBitteringUnits" -> Json.toJson(result.internationalBitteringUnits),
      "standardReferenceMethod" -> Json.toJson(result.standardReferenceMethod),
      "efficiency" -> Json.toJson(result.efficiency),
      "attenuation" -> Json.toJson(result.attenuation),
      "calories" -> Json.toJson(result.calories),
      "preboilVolume" -> result.preboilVolume,
      "postboilVolume" -> result.postboilVolume,
      "fermentorVolume" -> result.fermentorVolume,
      "finalVolume" -> result.finalVolume,
      "bitternessTogravigtRatio" -> result.bitternessTogravigtRatio,
      "maltToHopRatio" -> result.maltToHopRatio,
      "balanceRatio" -> Json.toJson(result.balanceRatio),
      "bitternessRatio" -> Json.toJson(result.bitternessRatio),
      "maltContributions" -> Json.toJson(result.maltContributions),
      "hopContributions" -> Json.toJson(result.hopContributions),
      "yeastContribution" -> Json.toJson(result.yeastContribution),
      "calculationMethod" -> result.calculationMethod,
      "calculatedAt" -> Json.toJson(result.calculatedAt),
      "parametersHash" -> result.parametersHash,
      "warnings" -> Json.toJson(result.warnings),
      "isWithinStyleGuidelines" -> Json.toJson(result.isWithinStyleGuidelines),
      "styleDeviations" -> Json.toJson(result.styleDeviations)
    )
  }
}