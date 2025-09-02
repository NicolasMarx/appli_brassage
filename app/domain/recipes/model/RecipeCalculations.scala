package domain.recipes.model

import play.api.libs.json._
import java.time.Instant

/**
 * Calculs de recette étendus avec support des calculs avancés
 * Compatible avec l'ancien format mais supporte les nouveaux calculs complets
 */
case class RecipeCalculations(
  // Calculs de base (compatibilité)
  originalGravity: Option[Double] = None,
  finalGravity: Option[Double] = None,
  abv: Option[Double] = None,
  ibu: Option[Double] = None,
  srm: Option[Double] = None,
  efficiency: Option[Double] = None,
  calculatedAt: Option[java.time.Instant] = None,
  
  // Nouveaux calculs avancés
  brewingCalculationResult: Option[BrewingCalculationResult] = None,
  waterChemistryResult: Option[WaterChemistryResult] = None,
  
  // Métadonnées des calculs
  calculationMethod: Option[String] = None,
  parametersHash: Option[String] = None,
  hasWarnings: Boolean = false,
  warnings: List[String] = List.empty
) {
  
  def isComplete: Boolean = originalGravity.isDefined && finalGravity.isDefined && abv.isDefined
  
  def isAdvancedComplete: Boolean = brewingCalculationResult.exists(_.isComplete)
  
  def needsRecalculation: Boolean = calculatedAt.isEmpty || 
    calculatedAt.exists(_.isBefore(java.time.Instant.now().minusSeconds(300))) // 5 minutes
    
  def needsAdvancedRecalculation(currentParametersHash: String): Boolean = {
    brewingCalculationResult.isEmpty || 
    parametersHash.exists(_ != currentParametersHash) ||
    needsRecalculation
  }
  
  /**
   * Met à jour avec les résultats de calculs avancés
   */
  def withAdvancedCalculations(
    result: BrewingCalculationResult,
    waterResult: Option[WaterChemistryResult] = None
  ): RecipeCalculations = {
    this.copy(
      // Mise à jour des valeurs de base pour compatibilité
      originalGravity = Some(result.originalGravity.value),
      finalGravity = Some(result.finalGravity.value),
      abv = Some(result.alcoholByVolume.primary),
      ibu = Some(result.internationalBitteringUnits.total),
      srm = Some(result.standardReferenceMethod.value),
      efficiency = Some(result.efficiency.calculated),
      calculatedAt = Some(result.calculatedAt),
      
      // Nouveaux résultats complets
      brewingCalculationResult = Some(result),
      waterChemistryResult = waterResult,
      calculationMethod = Some(result.calculationMethod),
      parametersHash = Some(result.parametersHash),
      hasWarnings = result.hasWarnings,
      warnings = result.warnings
    )
  }
  
  /**
   * Résumé des calculs principaux
   */
  def summary: String = {
    brewingCalculationResult.map(_.summary).getOrElse {
      s"OG: ${originalGravity.map(g => f"$g%.3f").getOrElse("N/A")} | " +
      s"FG: ${finalGravity.map(g => f"$g%.3f").getOrElse("N/A")} | " +
      s"ABV: ${abv.map(a => f"$a%.1f%%").getOrElse("N/A")} | " +
      s"IBU: ${ibu.map(i => f"$i%.0f").getOrElse("N/A")} | " +
      s"SRM: ${srm.map(s => f"$s%.0f").getOrElse("N/A")}"
    }
  }
  
  /**
   * Détails des calculs si disponibles
   */
  def detailedSummary: String = {
    brewingCalculationResult.map { result =>
      val basic = result.summary
      val balance = if (result.isBalanced) "Équilibrée" else "Déséquilibrée"
      val strength = result.strengthCategory
      val color = result.colorCategory
      val bitterness = result.bitternessCategory
      
      s"$basic\nForce: $strength | Couleur: $color | Amertume: $bitterness | Balance: $balance"
    }.getOrElse(summary)
  }
  
  /**
   * Obtient les valeurs principales avec fallback sur les calculs de base
   */
  def getOriginalGravity: Option[Double] = brewingCalculationResult.map(_.originalGravity.value).orElse(originalGravity)
  def getFinalGravity: Option[Double] = brewingCalculationResult.map(_.finalGravity.value).orElse(finalGravity)
  def getABV: Option[Double] = brewingCalculationResult.map(_.alcoholByVolume.primary).orElse(abv)
  def getIBU: Option[Double] = brewingCalculationResult.map(_.internationalBitteringUnits.total).orElse(ibu)
  def getSRM: Option[Double] = brewingCalculationResult.map(_.standardReferenceMethod.value).orElse(srm)
  def getEfficiency: Option[Double] = brewingCalculationResult.map(_.efficiency.calculated).orElse(efficiency)
}

object RecipeCalculations {
  def empty: RecipeCalculations = RecipeCalculations()
  
  /**
   * Crée des calculs de base à partir de valeurs simples
   */
  def basic(
    og: Double,
    fg: Double,
    abv: Double,
    ibu: Double,
    srm: Double,
    efficiency: Double
  ): RecipeCalculations = RecipeCalculations(
    originalGravity = Some(og),
    finalGravity = Some(fg),
    abv = Some(abv),
    ibu = Some(ibu),
    srm = Some(srm),
    efficiency = Some(efficiency),
    calculatedAt = Some(Instant.now())
  )
  
  /**
   * Crée des calculs avancés à partir d'un résultat complet
   */
  def advanced(
    result: BrewingCalculationResult,
    waterResult: Option[WaterChemistryResult] = None
  ): RecipeCalculations = RecipeCalculations().withAdvancedCalculations(result, waterResult)
  
  implicit val format: Format[RecipeCalculations] = Json.format[RecipeCalculations]
}