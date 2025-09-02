package domain.recipes.services

import domain.recipes.model._
import play.api.libs.json._
import scala.math._

/**
 * Calculateur avancé de chimie de l'eau pour le brassage
 * Implémente les calculs de pH de maische, ajustements minéraux, et profils d'eau
 */
class WaterChemistryCalculator {

  /**
   * Calcule le profil d'eau complet pour une recette
   */
  def calculateWaterProfile(
    sourceWater: WaterProfile,
    targetProfile: Option[WaterProfile],
    mashBill: List[MaltCalculationInput],
    volumeLiters: Double
  ): WaterChemistryResult = {
    
    val mashPH = calculateMashPH(sourceWater, mashBill)
    val mineralAdjustments = targetProfile.map(target => 
      calculateMineralAdjustments(sourceWater, target, volumeLiters)
    ).getOrElse(List.empty)
    
    val adjustedWater = applyMineralAdjustments(sourceWater, mineralAdjustments)
    val finalMashPH = calculateMashPH(adjustedWater, mashBill)
    
    val acidAdditions = if (finalMashPH > 5.6) {
      calculateAcidAdditions(adjustedWater, mashBill, volumeLiters, targetPH = 5.4)
    } else List.empty
    
    val phResult = PHResult(
      value = finalMashPH,
      confidence = 0.8, // Estimation basée sur la formule Palmer
      method = "Palmer formula",
      isOptimal = finalMashPH >= 5.2 && finalMashPH <= 5.6
    )
    
    val warnings = validateWaterProfile(adjustedWater)
    
    WaterChemistryResult(
      estimatedMashPH = phResult,
      acidAddition = acidAdditions.headOption.map(convertToAcidAddition),
      sourceWaterProfile = sourceWater,
      targetWaterProfile = targetProfile.getOrElse(sourceWater),
      adjustedWaterProfile = adjustedWater,
      mineralAdditions = mineralAdjustments.map(convertToMineralAddition),
      waterQualityScore = calculateWaterQualityScore(adjustedWater, warnings),
      warnings = warnings
    )
  }

  /**
   * Convertit AcidAddition du service vers AcidAddition du modèle
   */
  private def convertToAcidAddition(serviceAcid: AcidAddition): domain.recipes.model.AcidAddition = {
    domain.recipes.model.AcidAddition(
      acidType = serviceAcid.acidType,
      amountMl = serviceAcid.quantity, // Assume quantities are in ml
      targetPH = serviceAcid.phReduction, // Use phReduction as target
      estimatedPH = serviceAcid.phReduction - 0.1 // Estimated result pH
    )
  }
  
  /**
   * Convertit MineralAdjustment du service vers MineralAddition du modèle
   */
  private def convertToMineralAddition(adjustment: MineralAdjustment): MineralAddition = {
    val contribution = Map(
      "calcium" -> adjustment.calciumContribution,
      "magnesium" -> adjustment.magnesiumContribution,
      "sodium" -> adjustment.sodiumContribution,
      "chloride" -> adjustment.chlorideContribution,
      "sulfate" -> adjustment.sulfateContribution,
      "bicarbonate" -> adjustment.bicarbonateContribution
    )
    
    MineralAddition(
      mineralName = adjustment.mineralName,
      amountGrams = adjustment.quantity,
      contribution = contribution,
      purpose = "Water profile adjustment",
      estimatedCost = 0.0 // Coût non calculé pour l'instant
    )
  }
  
  /**
   * Calcule un score de qualité d'eau (0.0 - 1.0)
   */
  private def calculateWaterQualityScore(water: WaterProfile, warnings: List[String]): Double = {
    var score = 1.0
    
    // Pénalités basées sur les avertissements
    score -= warnings.length * 0.2
    
    // Bonus pour équilibre minéral
    val hardness = water.totalHardness
    if (hardness >= 50 && hardness <= 300) score += 0.1
    
    // Bonus pour ratio sulfate/chloride équilibré
    val ratio = water.sulfateChlorideRatio
    if (ratio >= 0.5 && ratio <= 2.0) score += 0.1
    
    math.max(0.0, math.min(1.0, score))
  }

  /**
   * Calcule le pH de maische estimé
   * Basé sur la méthode Troester modifiée
   */
  def calculateMashPH(water: WaterProfile, mashBill: List[MaltCalculationInput]): Double = {
    // pH de base de l'eau distillée avec les malts
    val baseMashPH = 5.8
    
    // Effet des minéraux sur le pH
    val mineralEffect = calculateMineralPHEffect(water)
    
    // Effet des malts sur le pH (malts foncés abaissent le pH)
    val maltEffect = mashBill.map { malt =>
      val colorSRM = malt.colorEBC.value * 0.508
      val acidification = colorSRM match {
        case c if c <= 3 => 0.0 // Malts très pâles
        case c if c <= 10 => -0.02 * (c - 3) * malt.percentage / 100.0
        case c if c <= 50 => -0.05 * sqrt(c - 10) * malt.percentage / 100.0
        case c => -0.15 * log(c / 50.0) * malt.percentage / 100.0
      }
      acidification
    }.sum
    
    val estimatedPH = baseMashPH + mineralEffect + maltEffect
    math.max(4.0, math.min(7.0, estimatedPH)) // Limites réalistes
  }

  private def calculateMineralPHEffect(water: WaterProfile): Double = {
    // Alcalinité résiduelle (RA) en ppm CaCO3
    val residualAlkalinity = water.bicarbonate - (water.calcium / 3.5) - (water.magnesium / 7.0)
    
    // Conversion RA vers effet pH (approximation)
    val phEffect = residualAlkalinity / 50.0 * 0.3
    math.max(-1.0, math.min(1.0, phEffect))
  }

  /**
   * Calcule les ajustements minéraux nécessaires
   */
  def calculateMineralAdjustments(
    source: WaterProfile,
    target: WaterProfile,
    volumeLiters: Double
  ): List[MineralAdjustment] = {
    val adjustments = scala.collection.mutable.ListBuffer[MineralAdjustment]()
    
    // Calcium (CaCl2 et CaSO4)
    val calciumDiff = target.calcium - source.calcium
    if (calciumDiff > 0) {
      val gypsum = calculateGypsumAddition(calciumDiff, target.sulfate - source.sulfate, volumeLiters)
      val calciumChloride = calculateCalciumChlorideAddition(
        calciumDiff - gypsum.calciumContribution, 
        target.chloride - source.chloride,
        volumeLiters
      )
      if (gypsum.quantity > 0) adjustments += gypsum
      if (calciumChloride.quantity > 0) adjustments += calciumChloride
    }
    
    // Magnésium (MgSO4)
    val magnesiumDiff = target.magnesium - source.magnesium
    if (magnesiumDiff > 0) {
      val epsom = calculateEpsomSaltAddition(magnesiumDiff, volumeLiters)
      if (epsom.quantity > 0) adjustments += epsom
    }
    
    // Sodium (NaCl)
    val sodiumDiff = target.sodium - source.sodium
    if (sodiumDiff > 0) {
      val salt = calculateSaltAddition(sodiumDiff, volumeLiters)
      if (salt.quantity > 0) adjustments += salt
    }
    
    adjustments.toList
  }

  private def calculateGypsumAddition(calciumNeeded: Double, sulfateNeeded: Double, volumeLiters: Double): MineralAdjustment = {
    // CaSO4·2H2O: 1g dans 1L ajoute 232mg Ca et 558mg SO4
    val calciumFromGypsum = if (calciumNeeded > 0) calciumNeeded else 0.0
    val sulfateFromGypsum = calciumFromGypsum * 558.0 / 232.0
    
    val quantity = (calciumFromGypsum * volumeLiters) / 232.0
    
    MineralAdjustment(
      mineralName = "Gypsum (CaSO4·2H2O)",
      quantity = quantity,
      unit = "g",
      calciumContribution = calciumFromGypsum,
      magnesiumContribution = 0.0,
      sodiumContribution = 0.0,
      chlorideContribution = 0.0,
      sulfateContribution = sulfateFromGypsum,
      bicarbonateContribution = 0.0
    )
  }

  private def calculateCalciumChlorideAddition(calciumNeeded: Double, chlorideNeeded: Double, volumeLiters: Double): MineralAdjustment = {
    // CaCl2·2H2O: 1g dans 1L ajoute 272mg Ca et 482mg Cl
    val calciumFromCaCl2 = if (calciumNeeded > 0) calciumNeeded else 0.0
    val chlorideFromCaCl2 = calciumFromCaCl2 * 482.0 / 272.0
    
    val quantity = (calciumFromCaCl2 * volumeLiters) / 272.0
    
    MineralAdjustment(
      mineralName = "Calcium Chloride (CaCl2·2H2O)",
      quantity = quantity,
      unit = "g",
      calciumContribution = calciumFromCaCl2,
      magnesiumContribution = 0.0,
      sodiumContribution = 0.0,
      chlorideContribution = chlorideFromCaCl2,
      sulfateContribution = 0.0,
      bicarbonateContribution = 0.0
    )
  }

  private def calculateEpsomSaltAddition(magnesiumNeeded: Double, volumeLiters: Double): MineralAdjustment = {
    // MgSO4·7H2O: 1g dans 1L ajoute 99mg Mg et 389mg SO4
    val quantity = (magnesiumNeeded * volumeLiters) / 99.0
    val sulfateContribution = magnesiumNeeded * 389.0 / 99.0
    
    MineralAdjustment(
      mineralName = "Epsom Salt (MgSO4·7H2O)",
      quantity = quantity,
      unit = "g",
      calciumContribution = 0.0,
      magnesiumContribution = magnesiumNeeded,
      sodiumContribution = 0.0,
      chlorideContribution = 0.0,
      sulfateContribution = sulfateContribution,
      bicarbonateContribution = 0.0
    )
  }

  private def calculateSaltAddition(sodiumNeeded: Double, volumeLiters: Double): MineralAdjustment = {
    // NaCl: 1g dans 1L ajoute 393mg Na et 607mg Cl
    val quantity = (sodiumNeeded * volumeLiters) / 393.0
    val chlorideContribution = sodiumNeeded * 607.0 / 393.0
    
    MineralAdjustment(
      mineralName = "Table Salt (NaCl)",
      quantity = quantity,
      unit = "g",
      calciumContribution = 0.0,
      magnesiumContribution = 0.0,
      sodiumContribution = sodiumNeeded,
      chlorideContribution = chlorideContribution,
      sulfateContribution = 0.0,
      bicarbonateContribution = 0.0
    )
  }

  /**
   * Applique les ajustements minéraux au profil d'eau
   */
  private def applyMineralAdjustments(source: WaterProfile, adjustments: List[MineralAdjustment]): WaterProfile = {
    val totalCa = source.calcium + adjustments.map(_.calciumContribution).sum
    val totalMg = source.magnesium + adjustments.map(_.magnesiumContribution).sum
    val totalNa = source.sodium + adjustments.map(_.sodiumContribution).sum
    val totalCl = source.chloride + adjustments.map(_.chlorideContribution).sum
    val totalSO4 = source.sulfate + adjustments.map(_.sulfateContribution).sum
    val totalHCO3 = source.bicarbonate + adjustments.map(_.bicarbonateContribution).sum
    
    source.copy(
      calcium = totalCa,
      magnesium = totalMg,
      sodium = totalNa,
      chloride = totalCl,
      sulfate = totalSO4,
      bicarbonate = totalHCO3
    )
  }

  /**
   * Calcule les ajouts d'acide nécessaires
   */
  def calculateAcidAdditions(
    water: WaterProfile,
    mashBill: List[MaltCalculationInput],
    volumeLiters: Double,
    targetPH: Double = 5.4
  ): List[AcidAddition] = {
    val currentPH = calculateMashPH(water, mashBill)
    val phDrop = currentPH - targetPH
    
    if (phDrop <= 0) return List.empty
    
    // Calcul approximatif basé sur l'alcalinité
    val alkalinity = water.bicarbonate * 0.82 // Conversion ppm HCO3 vers ppm CaCO3
    val acidRequired = phDrop * alkalinity * volumeLiters / 1000.0 // mL d'acide
    
    List(
      AcidAddition(
        acidType = "Phosphoric Acid (10%)",
        quantity = acidRequired * 1.2, // Ajustement pour acide phosphorique 10%
        unit = "mL",
        phReduction = phDrop,
        notes = "Acide phosphorique recommandé - n'ajoute pas de saveurs off"
      )
    )
  }

  /**
   * Valide le profil d'eau et génère des avertissements
   */
  private def validateWaterProfile(water: WaterProfile): List[String] = {
    val warnings = scala.collection.mutable.ListBuffer[String]()
    
    if (water.calcium < 50) warnings += "Calcium faible - peut affecter la clarification"
    if (water.calcium > 300) warnings += "Calcium très élevé - peut créer une astringence"
    
    if (water.magnesium > 30) warnings += "Magnésium élevé - peut créer une amertume métallique"
    
    if (water.sodium > 200) warnings += "Sodium élevé - peut créer un goût salé"
    
    val sulfateChlorideRatio = if (water.chloride > 0) water.sulfate / water.chloride else 100.0
    if (sulfateChlorideRatio > 9) warnings += "Ratio SO4/Cl très élevé - amertume sèche prononcée"
    if (sulfateChlorideRatio < 0.4) warnings += "Ratio SO4/Cl très faible - maltage prononcé"
    
    if (water.bicarbonate > 300) warnings += "Alcalinité élevée - pH de maische élevé probable"
    
    warnings.toList
  }

  /**
   * Recommande un profil d'eau basé sur le style de bière
   */
  def recommendWaterProfile(beerStyle: String, colorSRM: Double): WaterProfile = {
    beerStyle.toLowerCase match {
      case style if style.contains("pilsner") || style.contains("lager") =>
        WaterProfile.pilsner
      
      case style if style.contains("ipa") || style.contains("pale ale") =>
        WaterProfile.burton
      
      case style if style.contains("stout") || style.contains("porter") =>
        WaterProfile.dublin
      
      case style if style.contains("wheat") || style.contains("weizen") =>
        WaterProfile.munich
      
      case _ =>
        // Profil générique basé sur la couleur
        if (colorSRM < 10) WaterProfile.balanced
        else if (colorSRM < 30) WaterProfile.munich
        else WaterProfile.dublin
    }
  }
}

// ========================================
// MODÈLES DE DONNÉES
// ========================================

// WaterProfile moved to domain.recipes.model.WaterChemistryResult

case class MineralAdjustment(
  mineralName: String,
  quantity: Double,
  unit: String,
  calciumContribution: Double,
  magnesiumContribution: Double,
  sodiumContribution: Double,
  chlorideContribution: Double,
  sulfateContribution: Double,
  bicarbonateContribution: Double
) {
  def formatted: String = f"$quantity%.1f$unit $mineralName"
}

case class AcidAddition(
  acidType: String,
  quantity: Double,
  unit: String,
  phReduction: Double,
  notes: String
) {
  def formatted: String = f"$quantity%.1f$unit $acidType"
}

// WaterChemistryResult moved to domain.recipes.model.WaterChemistryResult

object WaterChemistryCalculator {
  def apply(): WaterChemistryCalculator = new WaterChemistryCalculator()
}