package domain.recipes.services

import domain.recipes.model._
import domain.malts.model.{EBCColor, ExtractionRate}
import domain.hops.model.AlphaAcidPercentage
import domain.yeasts.model.AttenuationRange
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Tests pour le système de calculs de brassage avancés
 * Vérifie la précision des formules et la cohérence des résultats
 */
class BrewingCalculatorServiceSpec extends AnyWordSpec with Matchers {

  val calculator = new BrewingCalculatorService()

  "BrewingCalculatorService" should {
    
    "calculate a simple pale ale recipe correctly" in {
      val parameters = createSimplePaleAleParameters()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      result.right.get.isComplete shouldBe true
      
      val calculations = result.right.get
      
      // Vérifications des ranges attendus pour une pale ale
      calculations.originalGravity.value should be >= 1.045
      calculations.originalGravity.value should be <= 1.060
      
      calculations.finalGravity.value should be >= 1.008
      calculations.finalGravity.value should be <= 1.015
      
      calculations.alcoholByVolume.primary should be >= 4.0
      calculations.alcoholByVolume.primary should be <= 6.5
      
      calculations.internationalBitteringUnits.total should be >= 30.0
      calculations.internationalBitteringUnits.total should be <= 50.0
    }
    
    "calculate IBU using multiple formulas" in {
      val parameters = createHoppyIPAParameters()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val ibu = result.right.get.internationalBitteringUnits
      
      // Les trois formules doivent donner des résultats dans un range raisonnable
      ibu.rager should be > 50.0
      ibu.tinseth should be > 50.0
      ibu.garetz should be > 50.0
      
      // Tinseth généralement plus conservateur que Rager
      ibu.tinseth should be <= ibu.rager
      
      // Garetz prend en compte plus de facteurs, peut varier
      math.abs(ibu.garetz - ibu.tinseth) should be <= 20.0
    }
    
    "calculate SRM color accurately" in {
      val parameters = createDarkStoutParameters()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val srm = result.right.get.standardReferenceMethod
      
      // Un stout doit être très foncé
      srm.value should be >= 35.0
      srm.ebc should be >= 60.0 // Équivalent EBC
      
      val color = result.right.get.colorCategory
      color should (equal("Dark") or equal("Black"))
    }
    
    "handle multiple ABV calculation methods" in {
      val parameters = createStrongBeerParameters()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val abv = result.right.get.alcoholByVolume
      
      // Méthodes primaire et secondaire doivent être cohérentes
      math.abs(abv.primary - abv.secondary) should be <= 1.0
      
      // ABV élevé pour une strong beer
      abv.primary should be >= 8.0
      abv.average should be >= 8.0
      
      val strength = result.right.get.strengthCategory
      strength should (equal("Very Strong") or equal("Imperial"))
    }
    
    "validate recipe balance (BU:GU ratio)" in {
      val parameters = createBalancedBeerParameters()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val calculations = result.right.get
      
      calculations.bitternessTogravigtRatio should be >= 0.5
      calculations.bitternessTogravigtRatio should be <= 1.2
      calculations.isBalanced shouldBe true
    }
    
    "calculate ingredient contributions correctly" in {
      val parameters = createComplexGrainBillParameters()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val calculations = result.right.get
      
      // Vérifications des contributions par malt
      calculations.maltContributions should have length 4
      val totalGravityPoints = calculations.maltContributions.map(_.gravityContribution).sum
      totalGravityPoints should be >= calculations.originalGravity.points * 0.9 // Tolérance d'erreur
      
      // Le malt de base doit contribuer le plus
      val baseMaltContribution = calculations.maltContributions.maxBy(_.gravityContribution)
      baseMaltContribution.percentage should be >= 50.0
      
      // Vérifications des contributions par houblon
      calculations.hopContributions should have length 3
      val totalIBU = calculations.hopContributions.map(_.ibuContribution).sum
      totalIBU should be >= calculations.internationalBitteringUnits.total * 0.9
    }
    
    "generate appropriate warnings" in {
      val parameters = createExtremeParametersWithIssues()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val calculations = result.right.get
      
      calculations.warnings should not be empty
      calculations.hasWarnings shouldBe true
      
      // Doit détecter les problèmes courants
      val warningText = calculations.warnings.mkString(" ")
      (warningText should (include("très élevé") or include("très faible") or include("amère")))
    }
    
    "calculate efficiency and attenuation correctly" in {
      val parameters = createParametersWithKnownEfficiency()
      val result = calculator.calculateRecipe(parameters)
      
      result shouldBe a[Right[_, _]]
      val calculations = result.right.get
      
      // L'efficacité calculée doit être proche de l'efficacité donnée
      math.abs(calculations.efficiency.calculated - parameters.efficiency) should be <= 5.0
      calculations.efficiency.isAcceptable shouldBe true
      
      // L'atténuation doit être cohérente avec la levure
      val expectedAttenuation = parameters.yeastInput.get.averageAttenuation
      math.abs(calculations.attenuation.apparent - expectedAttenuation) should be <= 10.0
    }
  }

  // ========================================
  // FACTORY METHODS POUR LES TESTS
  // ========================================

  private def createSimplePaleAleParameters(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 75.0,
      maltIngredients = List(
        MaltCalculationInput("Pale Ale Malt", 4.0, ExtractionRate.unsafe(82), EBCColor.unsafe(5), true, 140, 90.0),
        MaltCalculationInput("Crystal 60", 0.5, ExtractionRate.unsafe(78), EBCColor.unsafe(120), false, 0, 10.0)
      ),
      hopIngredients = List(
        HopCalculationInput("Cascade", 25.0, AlphaAcidPercentage(5.5), 60, HopUsageType.Boil),
        HopCalculationInput("Cascade", 15.0, AlphaAcidPercentage(5.5), 10, HopUsageType.Aroma)
      ),
      yeastInput = Some(YeastCalculationInput("Safale US-05", AttenuationRange.unsafe(70, 85), 12.0))
    )
  }

  private def createHoppyIPAParameters(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 75.0,
      maltIngredients = List(
        MaltCalculationInput("Pale Ale Malt", 5.5, ExtractionRate.unsafe(82), EBCColor.unsafe(5), true, 140, 85.0),
        MaltCalculationInput("Munich", 0.8, ExtractionRate.unsafe(80), EBCColor.unsafe(15), false, 120, 12.5),
        MaltCalculationInput("Crystal 40", 0.16, ExtractionRate.unsafe(78), EBCColor.unsafe(80), false, 0, 2.5)
      ),
      hopIngredients = List(
        HopCalculationInput("Columbus", 30.0, AlphaAcidPercentage(15.0), 60, HopUsageType.Boil),
        HopCalculationInput("Centennial", 25.0, AlphaAcidPercentage(10.0), 20, HopUsageType.Boil),
        HopCalculationInput("Cascade", 40.0, AlphaAcidPercentage(5.5), 5, HopUsageType.Aroma)
      ),
      yeastInput = Some(YeastCalculationInput("Safale US-05", AttenuationRange.unsafe(75, 85), 12.0))
    )
  }

  private def createDarkStoutParameters(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 72.0,
      maltIngredients = List(
        MaltCalculationInput("Maris Otter", 4.5, ExtractionRate.unsafe(82), EBCColor.unsafe(6), true, 130, 70.0),
        MaltCalculationInput("Crystal 120", 0.5, ExtractionRate.unsafe(75), EBCColor.unsafe(240), false, 0, 8.0),
        MaltCalculationInput("Chocolate Malt", 0.3, ExtractionRate.unsafe(70), EBCColor.unsafe(800), false, 0, 5.0),
        MaltCalculationInput("Roasted Barley", 0.8, ExtractionRate.unsafe(65), EBCColor.unsafe(1100), false, 0, 12.0),
        MaltCalculationInput("Flaked Barley", 0.3, ExtractionRate.unsafe(65), EBCColor.unsafe(5), false, 0, 5.0)
      ),
      hopIngredients = List(
        HopCalculationInput("East Kent Golding", 30.0, AlphaAcidPercentage(5.0), 60, HopUsageType.Boil)
      ),
      yeastInput = Some(YeastCalculationInput("Safale S-04", AttenuationRange.unsafe(72, 82), 9.0))
    )
  }

  private def createStrongBeerParameters(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 90,
      efficiency = 78.0,
      maltIngredients = List(
        MaltCalculationInput("Pilsner Malt", 8.0, ExtractionRate.unsafe(83), EBCColor.unsafe(3), true, 150, 85.0),
        MaltCalculationInput("Munich", 1.5, ExtractionRate.unsafe(80), EBCColor.unsafe(15), false, 120, 15.0)
      ),
      hopIngredients = List(
        HopCalculationInput("Magnum", 35.0, AlphaAcidPercentage(14.0), 90, HopUsageType.Boil),
        HopCalculationInput("Hallertau", 20.0, AlphaAcidPercentage(4.0), 15, HopUsageType.Aroma)
      ),
      yeastInput = Some(YeastCalculationInput("Belgian Strong Ale", AttenuationRange.unsafe(80, 90), 16.0))
    )
  }

  private def createBalancedBeerParameters(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 75.0,
      maltIngredients = List(
        MaltCalculationInput("Vienna", 4.2, ExtractionRate.unsafe(81), EBCColor.unsafe(8), true, 130, 100.0)
      ),
      hopIngredients = List(
        HopCalculationInput("Saaz", 28.0, AlphaAcidPercentage(3.5), 60, HopUsageType.Boil),
        HopCalculationInput("Saaz", 14.0, AlphaAcidPercentage(3.5), 15, HopUsageType.Aroma)
      ),
      yeastInput = Some(YeastCalculationInput("Saflager W-34/70", AttenuationRange.unsafe(80, 90), 9.0))
    )
  }

  private def createComplexGrainBillParameters(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 75.0,
      maltIngredients = List(
        MaltCalculationInput("Pale Ale Malt", 3.5, ExtractionRate.unsafe(82), EBCColor.unsafe(5), true, 140, 60.0),
        MaltCalculationInput("Vienna", 1.0, ExtractionRate.unsafe(81), EBCColor.unsafe(8), false, 130, 17.0),
        MaltCalculationInput("Crystal 60", 0.8, ExtractionRate.unsafe(78), EBCColor.unsafe(120), false, 0, 14.0),
        MaltCalculationInput("Chocolate Wheat", 0.5, ExtractionRate.unsafe(75), EBCColor.unsafe(900), false, 0, 9.0)
      ),
      hopIngredients = List(
        HopCalculationInput("Chinook", 20.0, AlphaAcidPercentage(13.0), 60, HopUsageType.Boil),
        HopCalculationInput("Citra", 15.0, AlphaAcidPercentage(12.0), 10, HopUsageType.Aroma),
        HopCalculationInput("Mosaic", 25.0, AlphaAcidPercentage(11.5), 0, HopUsageType.Whirlpool)
      ),
      yeastInput = Some(YeastCalculationInput("Safale US-05", AttenuationRange.unsafe(75, 85), 12.0))
    )
  }

  private def createExtremeParametersWithIssues(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 95.0, // Efficacité irréaliste
      maltIngredients = List(
        MaltCalculationInput("Pilsner Malt", 12.0, ExtractionRate.unsafe(83), EBCColor.unsafe(3), true, 150, 100.0) // Beaucoup de malt
      ),
      hopIngredients = List(
        HopCalculationInput("Magnum", 100.0, AlphaAcidPercentage(14.0), 60, HopUsageType.Boil) // Beaucoup trop de houblon
      ),
      yeastInput = Some(YeastCalculationInput("Standard Ale", AttenuationRange.unsafe(75, 85), 8.0))
    )
  }

  private def createParametersWithKnownEfficiency(): BrewingCalculationParameters = {
    BrewingCalculationParameters(
      batchSizeLiters = 20.0,
      boilTimeMins = 60,
      efficiency = 76.0, // Efficacité précise
      maltIngredients = List(
        MaltCalculationInput("Golden Promise", 4.5, ExtractionRate.unsafe(82), EBCColor.unsafe(6), true, 140, 100.0)
      ),
      hopIngredients = List(
        HopCalculationInput("Fuggles", 25.0, AlphaAcidPercentage(4.5), 60, HopUsageType.Boil)
      ),
      yeastInput = Some(YeastCalculationInput("London Ale III", AttenuationRange.unsafe(70, 80), 10.0))
    )
  }
}