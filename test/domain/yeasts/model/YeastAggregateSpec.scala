package domain.yeasts.model

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import java.time.Instant
import java.util.UUID
import domain.shared.DomainError

class YeastAggregateSpec extends AnyWordSpec with Matchers with EitherValues {

  "YeastAggregate" should {

    "be created successfully with valid data" in {
      val result = createValidYeast()
      result shouldBe a[Right[_, _]]

      val yeast = result.value
      yeast.name.value shouldBe "SafSpirit M-1"
      yeast.laboratory shouldBe YeastLaboratory.Fermentis
      yeast.status shouldBe YeastStatus.Draft
      yeast.version shouldBe 1
      yeast.getUncommittedEvents should have size 1
    }

    "validate technical coherence" in {
      val yeast = createValidYeast().value
      val warnings = yeast.validateTechnicalCoherence
      warnings shouldBe empty
    }

    "detect temperature/type inconsistency" in {
      val result = YeastAggregate.create(
        name = YeastName("Test Lager").value,
        laboratory = YeastLaboratory.Wyeast,
        strain = YeastStrain("2124").value,
        yeastType = YeastType.Lager,
        attenuation = AttenuationRange(75, 82),
        temperature = FermentationTemp(20, 25), // Trop chaud pour lager
        alcoholTolerance = AlcoholTolerance(10.0),
        flocculation = FlocculationLevel.Medium,
        characteristics = YeastCharacteristics.Clean,
        createdBy = UUID.randomUUID()
      )

      val yeast = result.value
      val warnings = yeast.validateTechnicalCoherence
      warnings should not be empty
      warnings.head should include("Température élevée pour une levure lager")
    }

    "update name successfully" in {
      val yeast = createValidYeast().value
      val newName = YeastName("Updated Name").value
      val updatedBy = UUID.randomUUID()

      val result = yeast.updateName(newName, updatedBy)
      result shouldBe a[Right[_, _]]

      val updated = result.value
      updated.name shouldBe newName
      updated.version shouldBe 2
      updated.getUncommittedEvents should have size 2
    }

    "reject name update with same name" in {
      val yeast = createValidYeast().value
      val result = yeast.updateName(yeast.name, UUID.randomUUID())

      result shouldBe a[Left[_, _]]
      result.left.value shouldBe a[DomainError.ValidationError]
    }

    "activate inactive yeast" in {
      val yeast = createValidYeast().value.copy(status = YeastStatus.Inactive)
      val result = yeast.activate(UUID.randomUUID())

      result shouldBe a[Right[_, _]]
      val activated = result.value
      activated.status shouldBe YeastStatus.Active
    }

    "reject activation of already active yeast" in {
      val yeast = createValidYeast().value.copy(status = YeastStatus.Active)
      val result = yeast.activate(UUID.randomUUID())

      result shouldBe a[Left[_, _]]
      result.left.value shouldBe a[DomainError.ValidationError]
    }

    "deactivate active yeast" in {
      val yeast = createValidYeast().value.copy(status = YeastStatus.Active)
      val result = yeast.deactivate(Some("Test reason"), UUID.randomUUID())

      result shouldBe a[Right[_, _]]
      val deactivated = result.value
      deactivated.status shouldBe YeastStatus.Inactive
    }

    "archive yeast with reason" in {
      val yeast = createValidYeast().value
      val result = yeast.archive(Some("Discontinued by lab"), UUID.randomUUID())

      result shouldBe a[Right[_, _]]
      val archived = result.value
      archived.status shouldBe YeastStatus.Archived
    }

    "reject modifications on archived yeast" in {
      val yeast = createValidYeast().value.copy(status = YeastStatus.Archived)
      val newName = YeastName("New Name").value
      val result = yeast.updateName(newName, UUID.randomUUID())

      result shouldBe a[Left[_, _]]
      result.left.value shouldBe a[DomainError.BusinessRuleViolation]
    }

    "check beer style compatibility" in {
      val aleYeast = createValidYeast().value.copy(yeastType = YeastType.Ale)
      val lagerYeast = createValidYeast().value.copy(yeastType = YeastType.Lager)

      aleYeast.isCompatibleWith("American IPA") shouldBe true
      aleYeast.isCompatibleWith("Czech Pilsner") shouldBe false

      lagerYeast.isCompatibleWith("Czech Pilsner") shouldBe true
      lagerYeast.isCompatibleWith("American IPA") shouldBe false
    }

    "provide recommended conditions" in {
      val yeast = createValidYeast().value
      val conditions = yeast.recommendedConditions

      conditions should contain key "température"
      conditions should contain key "atténuation_attendue"
      conditions should contain key "tolerance_alcool"
      conditions should contain key "floculation"
    }

    "update technical data successfully" in {
      val yeast = createValidYeast().value
      val newAttenuation = AttenuationRange(80, 85)
      val updatedBy = UUID.randomUUID()

      val result = yeast.updateTechnicalData(
        newAttenuation = Some(newAttenuation),
        updatedBy = updatedBy
      )

      result shouldBe a[Right[_, _]]
      val updated = result.value
      updated.attenuation shouldBe newAttenuation
      updated.version shouldBe 2
    }

    "update laboratory info successfully" in {
      val yeast = createValidYeast().value
      val newLab = YeastLaboratory.WhiteLabs
      val newStrain = YeastStrain("WLP001").value
      val updatedBy = UUID.randomUUID()

      val result = yeast.updateLaboratoryInfo(newLab, newStrain, updatedBy)

      result shouldBe a[Right[_, _]]
      val updated = result.value
      updated.laboratory shouldBe newLab
      updated.strain shouldBe newStrain
    }

    "reconstruct from events" in {
      val createdEvent = YeastEvent.YeastCreated(
        yeastId = YeastId.generate(),
        name = YeastName("Test Yeast").value,
        laboratory = YeastLaboratory.Fermentis,
        strain = YeastStrain("S-04").value,
        yeastType = YeastType.Ale,
        attenuation = AttenuationRange(75, 82),
        temperature = FermentationTemp(18, 22),
        alcoholTolerance = AlcoholTolerance(10.0),
        flocculation = FlocculationLevel.Medium,
        characteristics = YeastCharacteristics.Fruity,
        createdBy = UUID.randomUUID(),
        occurredAt = Instant.now(),
        version = 1
      )

      val result = YeastAggregate.fromEvents(List(createdEvent))
      result shouldBe a[Right[_, _]]

      val yeast = result.value
      yeast.name.value shouldBe "Test Yeast"
      yeast.version shouldBe 1
    }
  }

  private def createValidYeast(): Either[DomainError, YeastAggregate] = {
    YeastAggregate.create(
      name = YeastName("SafSpirit M-1").value,
      laboratory = YeastLaboratory.Fermentis,
      strain = YeastStrain("M-1").value,
      yeastType = YeastType.Ale,
      attenuation = AttenuationRange(78, 82),
      temperature = FermentationTemp(18, 22),
      alcoholTolerance = AlcoholTolerance(9.0),
      flocculation = FlocculationLevel.Medium,
      characteristics = YeastCharacteristics.Fruity,
      createdBy = UUID.randomUUID()
    )
  }
}
