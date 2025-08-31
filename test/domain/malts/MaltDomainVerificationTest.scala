package test.domain.malts

import domain.malts.model._
import domain.shared._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/**
 * Test de vérification du domaine Malts
 * Valide les Value Objects et l'agrégat principal
 */
class MaltDomainVerificationSpec extends AnyWordSpec with Matchers {

  "Domaine Malts" should {

    "créer des Value Objects valides" in {
      // Test MaltId
      val maltId = MaltId.generate()
      maltId.value should not be empty

      // Test EBCColor
      val ebcColor = EBCColor(15.0)
      ebcColor.isRight shouldBe true
      ebcColor.map(_.colorName) shouldBe Right("Pâle")

      // Test ExtractionRate
      val extractionRate = ExtractionRate(80.0)
      extractionRate.isRight shouldBe true
      extractionRate.map(_.canBeBaseMalt) shouldBe Right(true)

      // Test DiastaticPower
      val diastaticPower = DiastaticPower(100.0)
      diastaticPower.isRight shouldBe true
      diastaticPower.map(_.canConvertAdjuncts) shouldBe Right(true)
    }

    "créer un MaltAggregate valide" in {
      val name = NonEmptyString.create("Pilsner Malt").toOption.get
      val ebcColor = EBCColor(3.5).toOption.get
      val extractionRate = ExtractionRate(82.0).toOption.get
      val diastaticPower = DiastaticPower(105.0).toOption.get
      val origin = Origin("DE", "Germany", "Europe", isNoble = true, isNewWorld = false)

      val malt = MaltAggregate.create(
        name = name,
        maltType = MaltType.BASE,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        origin = origin,
        source = MaltSource.MANUAL
      )

      malt.isRight shouldBe true
      malt.map(_.isBaseMalt) shouldBe Right(true)
      malt.map(_.canSelfConvert) shouldBe Right(true)
      malt.map(_.qualityScore) should be('right)
    }

    "valider les business rules" in {
      val name = NonEmptyString.create("Invalid Dark Base").toOption.get
      val darkColor = EBCColor(500.0).toOption.get // Trop foncé pour un malt de base
      val extractionRate = ExtractionRate(80.0).toOption.get
      val diastaticPower = DiastaticPower(50.0).toOption.get
      val origin = Origin("DE", "Germany", "Europe", isNoble = false, isNewWorld = false)

      val invalidMalt = MaltAggregate.create(
        name = name,
        maltType = MaltType.BASE, // Base mais trop foncé
        ebcColor = darkColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        origin = origin,
        source = MaltSource.MANUAL
      )

      invalidMalt.isLeft shouldBe true // Doit échouer sur business rule
    }

    "gérer l'Event Sourcing" in {
      val malt = createValidMalt()
      malt.isRight shouldBe true

      val aggregate = malt.toOption.get
      aggregate.uncommittedEvents should not be empty
      aggregate.version shouldBe 1

      // Test mise à jour
      val updatedResult = aggregate.updateBasicInfo(
        NonEmptyString.create("Updated Name").toOption.get,
        Some("Updated description")
      )

      updatedResult.isRight shouldBe true
      val updatedAggregate = updatedResult.toOption.get
      updatedAggregate.version shouldBe 2
      updatedAggregate.uncommittedEvents.length shouldBe 2 // Création + mise à jour
    }
  }

  private def createValidMalt(): Either[domain.common.DomainError, MaltAggregate] = {
    val name = NonEmptyString.create("Test Malt").toOption.get
    val ebcColor = EBCColor(15.0).toOption.get
    val extractionRate = ExtractionRate(80.0).toOption.get
    val diastaticPower = DiastaticPower(100.0).toOption.get
    val origin = Origin("DE", "Germany", "Europe", isNoble = false, isNewWorld = false)

    MaltAggregate.create(
      name = name,
      maltType = MaltType.BASE,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      origin = origin,
      source = MaltSource.MANUAL
    )
  }
}