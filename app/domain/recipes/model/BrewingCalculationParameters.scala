package domain.recipes.model

import play.api.libs.json._
import domain.hops.model.AlphaAcidPercentage
import domain.malts.model.{EBCColor, ExtractionRate}
import domain.yeasts.model.AttenuationRange

/**
 * Paramètres d'entrée pour les calculs de brassage
 * Contient toutes les valeurs nécessaires pour effectuer les calculs précis
 */
case class BrewingCalculationParameters(
  batchSizeLiters: Double,
  boilTimeMins: Int = 60,
  efficiency: Double = 75.0, // Pourcentage d'efficacité du brasseur
  
  // Ingrédients avec leurs propriétés
  maltIngredients: List[MaltCalculationInput],
  hopIngredients: List[HopCalculationInput],
  yeastInput: Option[YeastCalculationInput],
  
  // Paramètres environnementaux
  boilOffRate: Double = 10.0, // Pourcentage d'évaporation par heure
  trubLossLiters: Double = 1.0, // Pertes en trub et hop matter
  equipmentLossLiters: Double = 0.5, // Pertes équipement
  
  // Température pour corrections
  mashTemp: Double = 67.0,
  originalGravityTemp: Double = 20.0, // Température de mesure de densité
  
  // Facteurs d'ajustement
  hopUtilizationFactor: Double = 1.0, // Facteur d'ajustement utilisation houblon
  yeastAttenuationAdjustment: Double = 0.0 // Ajustement atténuation levure (+/-%)
) {
  
  def totalMaltWeightKg: Double = maltIngredients.map(_.quantityKg).sum
  def totalHopWeightGrams: Double = hopIngredients.map(_.quantityGrams).sum
  
  def validateParameters(): List[String] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    if (batchSizeLiters <= 0) errors += "Le volume de batch doit être positif"
    if (boilTimeMins < 0) errors += "Le temps d'ébullition ne peut être négatif"
    if (efficiency <= 0 || efficiency > 100) errors += "L'efficacité doit être entre 0 et 100%"
    if (maltIngredients.isEmpty) errors += "Au moins un malt est requis"
    if (hopIngredients.isEmpty) errors += "Au moins un houblon est requis"
    if (yeastInput.isEmpty) errors += "Une levure est requise"
    if (boilOffRate < 0 || boilOffRate > 50) errors += "Le taux d'évaporation doit être entre 0 et 50% par heure"
    
    maltIngredients.zipWithIndex.foreach { case (malt, index) =>
      if (malt.quantityKg <= 0) errors += s"Quantité du malt ${index + 1} doit être positive"
    }
    
    hopIngredients.zipWithIndex.foreach { case (hop, index) =>
      if (hop.quantityGrams <= 0) errors += s"Quantité du houblon ${index + 1} doit être positive"
      if (hop.boilTimeMins < 0) errors += s"Temps d'ébullition du houblon ${index + 1} ne peut être négatif"
    }
    
    errors.toList
  }
  
  def isValid: Boolean = validateParameters().isEmpty
}

case class MaltCalculationInput(
  name: String,
  quantityKg: Double,
  extractionRate: ExtractionRate, // Taux d'extraction (%)
  colorEBC: EBCColor, // Couleur EBC
  isBaseMalt: Boolean,
  diastaticPower: Double = 0.0, // Lintner degrees
  percentage: Double // Pourcentage dans le grain bill
) {
  def extractPotential: Double = extractionRate.value / 100.0 * 384.0 // Points par livre par gallon
  def colorSRM: Double = colorEBC.value * 0.508 // Conversion EBC vers SRM
  def contributionPoints(efficiency: Double): Double = {
    quantityKg * extractPotential * (efficiency / 100.0)
  }
}

case class HopCalculationInput(
  name: String,
  quantityGrams: Double,
  alphaAcidPercent: AlphaAcidPercentage,
  boilTimeMins: Int,
  hopUsage: HopUsageType,
  hopForm: HopFormType = HopFormType.Pellets
) {
  def alphaAcidDecimal: Double = alphaAcidPercent.value / 100.0
  def isBoilHop: Boolean = hopUsage == HopUsageType.Boil
  def isDryHop: Boolean = hopUsage == HopUsageType.DryHop
}

case class YeastCalculationInput(
  name: String,
  attenuationRange: AttenuationRange,
  alcoholTolerance: Double,
  optimalTemp: Double = 20.0
) {
  def averageAttenuation: Double = (attenuationRange.min + attenuationRange.max) / 2.0
  def attenuationPercent: Double = averageAttenuation / 100.0
}

sealed trait HopUsageType {
  def name: String
}
object HopUsageType {
  case object Boil extends HopUsageType { val name = "BOIL" }
  case object Aroma extends HopUsageType { val name = "AROMA" }
  case object DryHop extends HopUsageType { val name = "DRY_HOP" }
  case object Whirlpool extends HopUsageType { val name = "WHIRLPOOL" }
  
  def fromString(usage: String): Either[String, HopUsageType] = usage.toUpperCase match {
    case "BOIL" => Right(Boil)
    case "AROMA" => Right(Aroma)
    case "DRY_HOP" => Right(DryHop)
    case "WHIRLPOOL" => Right(Whirlpool)
    case _ => Left(s"Usage de houblon non valide: $usage")
  }
}

sealed trait HopFormType {
  def name: String
  def utilizationMultiplier: Double
}
object HopFormType {
  case object Pellets extends HopFormType { 
    val name = "PELLETS"
    val utilizationMultiplier = 1.0
  }
  case object WholeLeaf extends HopFormType { 
    val name = "WHOLE_LEAF"
    val utilizationMultiplier = 0.9
  }
  case object PlugWhole extends HopFormType { 
    val name = "PLUG_WHOLE"
    val utilizationMultiplier = 0.85
  }
}

object BrewingCalculationParameters {
  implicit val hopUsageTypeFormat: Format[HopUsageType] = new Format[HopUsageType] {
    def reads(json: JsValue): JsResult[HopUsageType] = json match {
      case JsString(value) => HopUsageType.fromString(value) match {
        case Right(usage) => JsSuccess(usage)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu pour HopUsageType")
    }
    def writes(usage: HopUsageType): JsValue = JsString(usage.name)
  }
  
  implicit val hopFormTypeFormat: Format[HopFormType] = new Format[HopFormType] {
    def reads(json: JsValue): JsResult[HopFormType] = json match {
      case JsString("PELLETS") => JsSuccess(HopFormType.Pellets)
      case JsString("WHOLE_LEAF") => JsSuccess(HopFormType.WholeLeaf)
      case JsString("PLUG_WHOLE") => JsSuccess(HopFormType.PlugWhole)
      case _ => JsError("Type de houblon non reconnu")
    }
    def writes(form: HopFormType): JsValue = JsString(form.name)
  }
  
  implicit val maltInputFormat: Format[MaltCalculationInput] = Json.format[MaltCalculationInput]
  implicit val hopInputFormat: Format[HopCalculationInput] = Json.format[HopCalculationInput]
  implicit val yeastInputFormat: Format[YeastCalculationInput] = Json.format[YeastCalculationInput]
  implicit val format: Format[BrewingCalculationParameters] = Json.format[BrewingCalculationParameters]
}