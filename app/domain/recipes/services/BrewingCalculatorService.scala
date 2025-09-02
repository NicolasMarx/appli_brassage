package domain.recipes.services

import domain.recipes.model._
import java.security.MessageDigest
import scala.math._
import scala.util.{Success, Try}

/**
 * Service principal pour tous les calculs de brassage
 * Implémente les formules industrielles standard et avancées
 */
class BrewingCalculatorService {
  
  /**
   * Calcule tous les paramètres de brassage pour une recette
   */
  def calculateRecipe(params: BrewingCalculationParameters): Either[List[String], BrewingCalculationResult] = {
    // Validation des paramètres
    val validationErrors = params.validateParameters()
    if (validationErrors.nonEmpty) {
      return Left(validationErrors)
    }
    
    try {
      val hash = generateParametersHash(params)
      val warnings = scala.collection.mutable.ListBuffer[String]()
      
      // Calculs des volumes
      val volumes = calculateVolumes(params)
      
      // Calcul de la gravité originale
      val originalGravity = calculateOriginalGravity(params, volumes.preboilVolume)
      
      // Calcul de la gravité finale
      val finalGravity = calculateFinalGravity(originalGravity, params)
      
      // Calcul de l'ABV
      val abv = calculateABV(originalGravity, finalGravity)
      
      // Calcul des IBUs
      val ibu = calculateIBU(params, originalGravity.value, volumes.postboilVolume)
      
      // Calcul de la couleur SRM
      val srm = calculateSRM(params, volumes.postboilVolume)
      
      // Calculs auxiliaires
      val efficiency = calculateEfficiency(params, originalGravity)
      val attenuation = calculateAttenuation(originalGravity, finalGravity, params)
      val calories = calculateCalories(originalGravity, finalGravity)
      
      // Contributions par ingrédient
      val maltContributions = calculateMaltContributions(params, originalGravity, volumes.preboilVolume)
      val hopContributions = calculateHopContributions(params, originalGravity.value, ibu)
      val yeastContribution = calculateYeastContribution(params, finalGravity)
      
      // Ratios et équilibre
      val buGuRatio = ibu.total / ((originalGravity.value - 1.000) * 1000)
      val maltHopRatio = params.totalMaltWeightKg / (params.totalHopWeightGrams / 1000.0)
      
      // Vérifications et warnings
      checkCalculationWarnings(params, originalGravity, finalGravity, ibu, warnings)
      
      val result = BrewingCalculationResult(
        originalGravity = originalGravity,
        finalGravity = finalGravity,
        alcoholByVolume = abv,
        internationalBitteringUnits = ibu,
        standardReferenceMethod = srm,
        efficiency = efficiency,
        attenuation = attenuation,
        calories = calories,
        preboilVolume = volumes.preboilVolume,
        postboilVolume = volumes.postboilVolume,
        fermentorVolume = volumes.fermentorVolume,
        finalVolume = volumes.finalVolume,
        bitternessTogravigtRatio = buGuRatio,
        maltToHopRatio = maltHopRatio,
        maltContributions = maltContributions,
        hopContributions = hopContributions,
        yeastContribution = yeastContribution,
        parametersHash = hash,
        warnings = warnings.toList
      )
      
      Right(result)
      
    } catch {
      case ex: Exception =>
        Left(List(s"Erreur de calcul: ${ex.getMessage}"))
    }
  }
  
  // ========================================
  // CALCULS DE VOLUMES
  // ========================================
  
  private case class VolumeResults(
    preboilVolume: Double,
    postboilVolume: Double,
    fermentorVolume: Double,
    finalVolume: Double
  )
  
  private def calculateVolumes(params: BrewingCalculationParameters): VolumeResults = {
    val finalVolume = params.batchSizeLiters
    val fermentorVolume = finalVolume + params.trubLossLiters
    val postboilVolume = fermentorVolume + params.equipmentLossLiters
    
    // Volume pre-ébullition = volume post-ébullition + évaporation
    val evaporationLiters = postboilVolume * (params.boilOffRate / 100.0) * (params.boilTimeMins / 60.0)
    val preboilVolume = postboilVolume + evaporationLiters
    
    VolumeResults(preboilVolume, postboilVolume, fermentorVolume, finalVolume)
  }
  
  // ========================================
  // CALCULS DE GRAVITÉ
  // ========================================
  
  def calculateOriginalGravity(params: BrewingCalculationParameters, preboilVolume: Double): GravityResult = {
    val totalPoints = params.maltIngredients.map { malt =>
      val potentialPoints = malt.extractPotential * malt.quantityKg
      val extractedPoints = potentialPoints * (params.efficiency / 100.0)
      extractedPoints
    }.sum
    
    // Conversion en gravité spécifique
    val gravityPoints = totalPoints / (preboilVolume * 0.264172) // Conversion L vers gallons US
    val specificGravity = 1.000 + (gravityPoints / 1000.0)
    
    // Correction de température (standard 20°C)
    val correctedGravity = temperatureCorrection(specificGravity, params.originalGravityTemp, 20.0)
    
    GravityResult(
      value = specificGravity,
      correctedValue = correctedGravity,
      points = gravityPoints,
      method = "Standard Extract Potential",
      temperature = params.originalGravityTemp
    )
  }
  
  def calculateFinalGravity(og: GravityResult, params: BrewingCalculationParameters): GravityResult = {
    val yeastAttenuation = params.yeastInput.map(_.averageAttenuation).getOrElse(75.0)
    val adjustedAttenuation = yeastAttenuation + params.yeastAttenuationAdjustment
    
    // Atténuation apparente (méthode Balling)
    val apparentExtract = og.points * (1.0 - adjustedAttenuation / 100.0)
    val finalGravity = 1.000 + (apparentExtract / 1000.0)
    
    // Correction de température
    val correctedFG = temperatureCorrection(finalGravity, params.originalGravityTemp, 20.0)
    
    GravityResult(
      value = finalGravity,
      correctedValue = correctedFG,
      points = apparentExtract,
      method = "Yeast Attenuation",
      temperature = params.originalGravityTemp
    )
  }
  
  private def temperatureCorrection(gravity: Double, measuredTemp: Double, standardTemp: Double): Double = {
    // Correction standard pour la densité en fonction de la température
    val tempDiff = measuredTemp - standardTemp
    val correction = tempDiff * 0.00009
    gravity + correction
  }
  
  // ========================================
  // CALCULS ABV
  // ========================================
  
  def calculateABV(og: GravityResult, fg: GravityResult): ABVResult = {
    // Méthode 1: Formule Balling (recommandée)
    val abvBalling = (og.points - fg.points) / (2.0665 - 0.010665 * og.points)
    
    // Méthode 2: Formule Standard
    val abvStandard = (og.value - fg.value) * 131.25
    
    // Méthode 3: Formule Alternative pour vérification
    val abvAlternative = ((1.05 * (og.value - fg.value)) / fg.value) / 0.79 * 100
    
    val average = (abvBalling + abvStandard) / 2.0
    
    ABVResult(
      primary = abvBalling,
      secondary = abvStandard,
      average = average,
      method = "Balling",
      secondaryMethod = "Standard"
    )
  }
  
  // ========================================
  // CALCULS IBU - FORMULES MULTIPLES
  // ========================================
  
  def calculateIBU(params: BrewingCalculationParameters, og: Double, postboilVolume: Double): IBUResult = {
    val boilHops = params.hopIngredients.filter(_.isBoilHop)
    
    val ragerTotal = boilHops.map(hop => calculateRagerIBU(hop, og, postboilVolume, params.hopUtilizationFactor)).sum
    val tinsethTotal = boilHops.map(hop => calculateTinsethIBU(hop, og, postboilVolume, params.hopUtilizationFactor)).sum  
    val garetzTotal = boilHops.map(hop => calculateGaretzIBU(hop, og, postboilVolume, params.hopUtilizationFactor)).sum
    
    // Utilisation de la formule Tinseth comme référence (plus précise)
    val primaryTotal = tinsethTotal
    
    IBUResult(
      total = primaryTotal,
      rager = ragerTotal,
      tinseth = tinsethTotal,
      garetz = garetzTotal,
      method = "Tinseth"
    )
  }
  
  /**
   * Formule Rager - Simple et directe
   */
  def calculateRagerIBU(hop: HopCalculationInput, og: Double, volumeLiters: Double, utilizationFactor: Double): Double = {
    val utilization = ragerUtilization(hop.boilTimeMins)
    val gravityFactor = if (og > 1.050) (og - 1.050) / 0.2 else 0.0
    val boilGravityAdjustment = 1.0 + (gravityFactor * 0.25)
    
    val ibu = (hop.quantityGrams * utilization * hop.alphaAcidDecimal * 100 * utilizationFactor) / 
              (volumeLiters * boilGravityAdjustment) * hop.hopForm.utilizationMultiplier
              
    math.max(0, ibu)
  }
  
  private def ragerUtilization(boilTimeMins: Int): Double = {
    val time = math.max(0, boilTimeMins)
    if (time == 0) 0.0
    else if (time <= 5) 0.05
    else if (time <= 10) 0.12
    else if (time <= 15) 0.15
    else if (time <= 20) 0.19
    else if (time <= 25) 0.24
    else if (time <= 30) 0.27
    else if (time <= 35) 0.30
    else if (time <= 40) 0.34
    else if (time <= 45) 0.37
    else if (time <= 50) 0.38
    else if (time <= 60) 0.40
    else 0.40
  }
  
  /**
   * Formule Tinseth - Plus précise, prend en compte la gravité de façon continue
   */
  def calculateTinsethIBU(hop: HopCalculationInput, og: Double, volumeLiters: Double, utilizationFactor: Double): Double = {
    val utilization = tinsethUtilization(hop.boilTimeMins, og)
    
    val ibu = (hop.quantityGrams * hop.alphaAcidDecimal * utilization * 100 * utilizationFactor) / 
              volumeLiters * hop.hopForm.utilizationMultiplier
              
    math.max(0, ibu)
  }
  
  private def tinsethUtilization(boilTimeMins: Int, og: Double): Double = {
    val time = math.max(0, boilTimeMins)
    val timeUtil = (1.0 - exp(-0.04 * time)) / 4.15
    val gravityUtil = 1.65 * pow(0.000125, og - 1.0)
    timeUtil * gravityUtil
  }
  
  /**
   * Formule Garetz - Prend en compte l'altitude, le pH et d'autres facteurs
   */
  def calculateGaretzIBU(hop: HopCalculationInput, og: Double, volumeLiters: Double, utilizationFactor: Double): Double = {
    val baseUtilization = garetzUtilization(hop.boilTimeMins)
    
    // Facteurs de correction Garetz
    val gravityFactor = ((og - 1.050) / 0.2) + 1.0
    val hopRateFactor = ((hop.quantityGrams / volumeLiters) / 2.0) + 1.0
    val temperatureFactor = 1.0 // Assumé standard (pas d'altitude)
    
    val combinedFactor = gravityFactor * hopRateFactor * temperatureFactor
    val adjustedUtilization = baseUtilization / combinedFactor
    
    val ibu = (hop.quantityGrams * hop.alphaAcidDecimal * adjustedUtilization * 100 * utilizationFactor) / 
              volumeLiters * hop.hopForm.utilizationMultiplier
              
    math.max(0, ibu)
  }
  
  private def garetzUtilization(boilTimeMins: Int): Double = {
    val time = math.max(0, boilTimeMins)
    7.2994 + 15.0746 * tanh((time - 21.86) / 24.71) / 100.0
  }
  
  // ========================================
  // CALCULS SRM (COULEUR)
  // ========================================
  
  def calculateSRM(params: BrewingCalculationParameters, postboilVolume: Double): SRMResult = {
    val totalMCU = params.maltIngredients.map { malt =>
      val colorSRM = malt.colorEBC.value * 0.508 // Conversion EBC vers SRM
      val lbs = malt.quantityKg * 2.20462 // kg vers livres
      val gallons = postboilVolume * 0.264172 // litres vers gallons US
      
      (colorSRM * lbs) / gallons
    }.sum
    
    // Formule Morey pour SRM
    val srm = 1.4922 * pow(totalMCU, 0.6859)
    val ebc = srm * 1.97 // Conversion approximative SRM vers EBC
    
    SRMResult(
      value = srm,
      ebc = ebc,
      method = "Morey"
    )
  }
  
  // ========================================
  // CALCULS AUXILIAIRES
  // ========================================
  
  def calculateEfficiency(params: BrewingCalculationParameters, og: GravityResult): EfficiencyResult = {
    val potentialPoints = params.maltIngredients.map { ingredient =>
      ingredient.contributionPoints(100.0) // Points potentiels à 100% d'efficacité
    }.sum
    val actualPoints = og.points * (params.batchSizeLiters * 0.264172) // Conversion en gallons
    val actualEfficiency = if (potentialPoints > 0) (actualPoints / potentialPoints) * 100.0 else 0.0
    
    val variance = actualEfficiency - params.efficiency
    val isAcceptable = math.abs(variance) <= 5.0 // +/- 5% acceptable
    
    EfficiencyResult(
      calculated = actualEfficiency,
      expected = params.efficiency,
      variance = variance,
      isAcceptable = isAcceptable
    )
  }
  
  def calculateAttenuation(og: GravityResult, fg: GravityResult, params: BrewingCalculationParameters): AttenuationResult = {
    val apparentAttenuation = ((og.points - fg.points) / og.points) * 100.0
    
    // Atténuation réelle (formule Balling)
    val realExtractOG = og.points / (258.6 - ((og.points / 258.2) * 227.1))
    val realExtractFG = fg.points / (258.6 - ((fg.points / 258.2) * 227.1))
    val realAttenuation = ((realExtractOG - realExtractFG) / realExtractOG) * 100.0
    
    val expectedAttenuation = params.yeastInput.map(_.averageAttenuation).getOrElse(75.0)
    val variance = apparentAttenuation - expectedAttenuation
    
    AttenuationResult(
      apparent = apparentAttenuation,
      real = realAttenuation,
      expected = expectedAttenuation,
      variance = variance
    )
  }
  
  def calculateCalories(og: GravityResult, fg: GravityResult): CaloriesResult = {
    // Formule approximative: Cal/12oz = 1881.22 * FG(P) + 3550 * (RE/100) 
    val realExtract = ((0.1808 * og.points) + (0.8192 * fg.points)) / 100.0
    val caloriesPer355ml = 1881.22 * (fg.points / 100.0) + 3550 * realExtract
    val caloriesPer100ml = caloriesPer355ml * (100.0 / 355.0)
    val caloriesPerServing = caloriesPer100ml * 3.3 // 330ml serving
    
    CaloriesResult(
      per100ml = caloriesPer100ml,
      perServingMl = caloriesPerServing,
      servingSizeMl = 330.0
    )
  }
  
  // ========================================
  // CONTRIBUTIONS PAR INGRÉDIENT
  // ========================================
  
  def calculateMaltContributions(params: BrewingCalculationParameters, og: GravityResult, preboilVolume: Double): List[MaltContribution] = {
    val gallons = preboilVolume * 0.264172
    
    params.maltIngredients.map { malt =>
      val potentialPoints = malt.extractPotential * malt.quantityKg
      val extractedPoints = potentialPoints * (params.efficiency / 100.0)
      val gravityContribution = extractedPoints / gallons
      val extractionAchieved = (extractedPoints / potentialPoints) * 100.0
      
      // Contribution couleur basée sur la proportion
      val colorContribution = (malt.colorEBC.value * 0.508 * malt.quantityKg * 2.20462) / gallons
      
      MaltContribution(
        name = malt.name,
        quantityKg = malt.quantityKg,
        percentage = malt.percentage,
        gravityContribution = gravityContribution,
        colorContribution = colorContribution,
        extractionAchieved = extractionAchieved,
        potentialPoints = potentialPoints
      )
    }
  }
  
  def calculateHopContributions(params: BrewingCalculationParameters, og: Double, ibu: IBUResult): List[HopContribution] = {
    val postboilVolume = params.batchSizeLiters + params.trubLossLiters + params.equipmentLossLiters
    
    params.hopIngredients.filter(_.isBoilHop).map { hop =>
      val tinsethIBU = calculateTinsethIBU(hop, og, postboilVolume, params.hopUtilizationFactor)
      val utilization = tinsethUtilization(hop.boilTimeMins, og) * 100.0
      
      HopContribution(
        name = hop.name,
        quantityGrams = hop.quantityGrams,
        alphaAcidPercent = hop.alphaAcidPercent.value,
        boilTime = hop.boilTimeMins,
        ibuContribution = tinsethIBU,
        utilizationPercent = utilization,
        method = "Tinseth"
      )
    }
  }
  
  def calculateYeastContribution(params: BrewingCalculationParameters, fg: GravityResult): Option[YeastContribution] = {
    params.yeastInput.map { yeast =>
      YeastContribution(
        name = yeast.name,
        expectedAttenuation = yeast.averageAttenuation,
        attenuationRange = s"${yeast.attenuationRange.min}%-${yeast.attenuationRange.max}%",
        alcoholTolerance = yeast.alcoholTolerance,
        estimatedFinalGravity = fg.value
      )
    }
  }
  
  // ========================================
  // UTILITAIRES
  // ========================================
  
  private def checkCalculationWarnings(
    params: BrewingCalculationParameters,
    og: GravityResult,
    fg: GravityResult,
    ibu: IBUResult,
    warnings: scala.collection.mutable.ListBuffer[String]
  ): Unit = {
    
    if (og.value < 1.030) warnings += "Gravité originale très faible - vérifiez les malts"
    if (og.value > 1.120) warnings += "Gravité originale très élevée - risques de fermentation"
    if (fg.value < 1.000) warnings += "Gravité finale impossible (<1.000)"
    if (fg.value > 1.030) warnings += "Gravité finale élevée - fermentation incomplète possible"
    
    val abv = ((og.value - fg.value) * 131.25)
    if (abv > 15.0) warnings += "Taux d'alcool très élevé - vérifiez la tolérance de la levure"
    
    if (ibu.total > 100) warnings += "IBU très élevé - amertume intense"
    if (ibu.total < 5) warnings += "IBU très faible - bière peu houblonnée"
    
    val buGu = ibu.total / ((og.value - 1.000) * 1000)
    if (buGu < 0.3) warnings += "Ratio BU:GU faible - bière très maltée"
    if (buGu > 1.5) warnings += "Ratio BU:GU élevé - bière très amère"
    
    if (params.efficiency < 60.0) warnings += "Efficacité faible - vérifiez le processus de brassage"
    if (params.efficiency > 90.0) warnings += "Efficacité très élevée - vérifiez les calculs"
  }
  
  private def generateParametersHash(params: BrewingCalculationParameters): String = {
    val content = s"${params.batchSizeLiters}_${params.efficiency}_${params.maltIngredients.map(m => s"${m.name}_${m.quantityKg}").mkString("_")}_${params.hopIngredients.map(h => s"${h.name}_${h.quantityGrams}_${h.boilTimeMins}").mkString("_")}"
    MessageDigest.getInstance("MD5").digest(content.getBytes).map("%02x".format(_)).mkString.take(16)
  }
}

object BrewingCalculatorService {
  def apply(): BrewingCalculatorService = new BrewingCalculatorService()
}