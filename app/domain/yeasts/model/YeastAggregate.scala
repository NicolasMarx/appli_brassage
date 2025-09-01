package domain.yeasts.model

import domain.shared.{AggregateRoot, DomainError}
import YeastEvent._
import java.time.Instant
import java.util.UUID

/**
 * Agrégat Yeast - Root du domaine levures
 * Encapsule la logique métier complète des levures de brassage
 */
case class YeastAggregate private(
  id: YeastId,
  name: YeastName,
  laboratory: YeastLaboratory,
  strain: YeastStrain,
  yeastType: YeastType,
  attenuation: AttenuationRange,
  temperature: FermentationTemp,
  alcoholTolerance: AlcoholTolerance,
  flocculation: FlocculationLevel,
  characteristics: YeastCharacteristics,
  status: YeastStatus,
  version: Long,
  createdAt: Instant,
  updatedAt: Instant,
  uncommittedEvents: List[YeastEvent] = List.empty
) extends AggregateRoot[YeastEvent] {

  // ==========================================================================
  // BUSINESS LOGIC - CRÉATION ET MODIFICATION
  // ==========================================================================

  /**
   * Met à jour le nom de la levure
   */
  def updateName(newName: YeastName, updatedBy: UUID): Either[DomainError, YeastAggregate] = {
    if (newName == name) {
      Left(DomainError.ValidationError("Le nouveau nom est identique à l'actuel"))
    } else if (status == YeastStatus.Archived) {
      Left(DomainError.BusinessRuleViolation("Impossible de modifier une levure archivée"))
    } else {
      val event = YeastUpdated(
        yeastId = id,
        changes = Map("name" -> newName.value),
        updatedBy = updatedBy,
        occurredAt = Instant.now(),
        version = version + 1
      )
      Right(copy(
        name = newName,
        version = version + 1,
        updatedAt = event.occurredAt,
        uncommittedEvents = uncommittedEvents :+ event
      ))
    }
  }

  /**
   * Met à jour les données techniques
   */
  def updateTechnicalData(
    newAttenuation: Option[AttenuationRange] = None,
    newTemperature: Option[FermentationTemp] = None,
    newAlcoholTolerance: Option[AlcoholTolerance] = None,
    newFlocculation: Option[FlocculationLevel] = None,
    newCharacteristics: Option[YeastCharacteristics] = None,
    updatedBy: UUID
  ): Either[DomainError, YeastAggregate] = {

    if (status == YeastStatus.Archived) {
      return Left(DomainError.BusinessRuleViolation("Impossible de modifier une levure archivée"))
    }

    val hasChanges = newAttenuation.exists(_ != attenuation) ||
                    newTemperature.exists(_ != temperature) ||
                    newAlcoholTolerance.exists(_ != alcoholTolerance) ||
                    newFlocculation.exists(_ != flocculation) ||
                    newCharacteristics.exists(_ != characteristics)

    if (!hasChanges) {
      Left(DomainError.ValidationError("Aucune modification détectée"))
    } else {
      val event = YeastTechnicalDataUpdated(
        yeastId = id,
        attenuation = newAttenuation,
        temperature = newTemperature,
        alcoholTolerance = newAlcoholTolerance,
        flocculation = newFlocculation,
        characteristics = newCharacteristics,
        updatedBy = updatedBy,
        occurredAt = Instant.now(),
        version = version + 1
      )

      Right(copy(
        attenuation = newAttenuation.getOrElse(attenuation),
        temperature = newTemperature.getOrElse(temperature),
        alcoholTolerance = newAlcoholTolerance.getOrElse(alcoholTolerance),
        flocculation = newFlocculation.getOrElse(flocculation),
        characteristics = newCharacteristics.getOrElse(characteristics),
        version = version + 1,
        updatedAt = event.occurredAt,
        uncommittedEvents = uncommittedEvents :+ event
      ))
    }
  }

  /**
   * Met à jour les informations laboratoire
   */
  def updateLaboratoryInfo(
    newLaboratory: YeastLaboratory,
    newStrain: YeastStrain,
    updatedBy: UUID
  ): Either[DomainError, YeastAggregate] = {

    if (status == YeastStatus.Archived) {
      return Left(DomainError.BusinessRuleViolation("Impossible de modifier une levure archivée"))
    }

    if (newLaboratory == laboratory && newStrain == strain) {
      Left(DomainError.ValidationError("Aucune modification détectée"))
    } else {
      val event = YeastLaboratoryInfoUpdated(
        yeastId = id,
        laboratory = newLaboratory,
        strain = newStrain,
        updatedBy = updatedBy,
        occurredAt = Instant.now(),
        version = version + 1
      )

      Right(copy(
        laboratory = newLaboratory,
        strain = newStrain,
        version = version + 1,
        updatedAt = event.occurredAt,
        uncommittedEvents = uncommittedEvents :+ event
      ))
    }
  }

  // ==========================================================================
  // BUSINESS LOGIC - GESTION STATUT
  // ==========================================================================

  /**
   * Active la levure
   */
  def activate(activatedBy: UUID): Either[DomainError, YeastAggregate] = {
    status match {
      case YeastStatus.Active =>
        Left(DomainError.ValidationError("La levure est déjà active"))
      case YeastStatus.Archived =>
        Left(DomainError.BusinessRuleViolation("Impossible d'activer une levure archivée"))
      case _ =>
        val event = YeastActivated(
          yeastId = id,
          activatedBy = activatedBy,
          occurredAt = Instant.now(),
          version = version + 1
        )
        Right(copy(
          status = YeastStatus.Active,
          version = version + 1,
          updatedAt = event.occurredAt,
          uncommittedEvents = uncommittedEvents :+ event
        ))
    }
  }

  /**
   * Désactive la levure
   */
  def deactivate(reason: Option[String], deactivatedBy: UUID): Either[DomainError, YeastAggregate] = {
    status match {
      case YeastStatus.Inactive =>
        Left(DomainError.ValidationError("La levure est déjà inactive"))
      case YeastStatus.Archived =>
        Left(DomainError.BusinessRuleViolation("Impossible de désactiver une levure archivée"))
      case _ =>
        val event = YeastDeactivated(
          yeastId = id,
          reason = reason,
          deactivatedBy = deactivatedBy,
          occurredAt = Instant.now(),
          version = version + 1
        )
        Right(copy(
          status = YeastStatus.Inactive,
          version = version + 1,
          updatedAt = event.occurredAt,
          uncommittedEvents = uncommittedEvents :+ event
        ))
    }
  }

  /**
   * Archive la levure
   */
  def archive(reason: Option[String], archivedBy: UUID): Either[DomainError, YeastAggregate] = {
    if (status == YeastStatus.Archived) {
      Left(DomainError.ValidationError("La levure est déjà archivée"))
    } else {
      val event = YeastArchived(
        yeastId = id,
        reason = reason,
        archivedBy = archivedBy,
        occurredAt = Instant.now(),
        version = version + 1
      )
      Right(copy(
        status = YeastStatus.Archived,
        version = version + 1,
        updatedAt = event.occurredAt,
        uncommittedEvents = uncommittedEvents :+ event
      ))
    }
  }

  // ==========================================================================
  // BUSINESS LOGIC - VALIDATION ET RÈGLES MÉTIER
  // ==========================================================================

  /**
   * Valide la cohérence des données techniques
   */
  def validateTechnicalCoherence: List[String] = {
    var warnings = List.empty[String]

    // Cohérence température/type
    (yeastType, temperature) match {
      case (YeastType.Lager, temp) if temp.min > 15 =>
        warnings = "Température élevée pour une levure lager" :: warnings
      case (YeastType.Ale, temp) if temp.max < 15 =>
        warnings = "Température basse pour une levure ale" :: warnings
      case (YeastType.Kveik, temp) if temp.max < 25 =>
        warnings = "Température basse pour une levure kveik" :: warnings
      case _ => // OK
    }

    // Cohérence atténuation/type
    (yeastType, attenuation) match {
      case (YeastType.Champagne, att) if att.max < 85 =>
        warnings = "Atténuation faible pour une levure champagne" :: warnings
      case _ => // OK
    }

    // Cohérence floculation/type
    (yeastType, flocculation) match {
      case (YeastType.Lager, FlocculationLevel.Low) =>
        warnings = "Floculation faible inhabituelle pour lager" :: warnings
      case _ => // OK
    }

    warnings.reverse
  }

  /**
   * Vérifie si la levure est compatible avec un style de bière
   */
  def isCompatibleWith(beerStyle: String): Boolean = {
    val styleLC = beerStyle.toLowerCase
    yeastType match {
      case YeastType.Ale => styleLC.contains("ale") || styleLC.contains("ipa") || styleLC.contains("porter")
      case YeastType.Lager => styleLC.contains("lager") || styleLC.contains("pilsner")
      case YeastType.Wheat => styleLC.contains("wheat") || styleLC.contains("weizen")
      case YeastType.Saison => styleLC.contains("saison") || styleLC.contains("farmhouse")
      case YeastType.Wild => styleLC.contains("wild") || styleLC.contains("brett")
      case YeastType.Sour => styleLC.contains("sour") || styleLC.contains("gose")
      case _ => true // Types polyvalents
    }
  }

  /**
   * Recommande des conditions de fermentation optimales
   */
  def recommendedConditions: Map[String, String] = {
    Map(
      "température" -> s"${temperature.min}-${temperature.max}°C",
      "atténuation_attendue" -> s"${attenuation.min}-${attenuation.max}%",
      "tolerance_alcool" -> s"jusqu'à ${alcoholTolerance.percentage}%",
      "floculation" -> flocculation.name,
      "temps_clarification" -> flocculation.clarificationTime,
      "recommandation_soutirage" -> flocculation.rackingRecommendation
    )
  }

  // ==========================================================================
  // AGGREGATE ROOT IMPLEMENTATION
  // ==========================================================================

  override def markEventsAsCommitted: YeastAggregate = copy(uncommittedEvents = List.empty)

  override def getUncommittedEvents: List[YeastEvent] = uncommittedEvents

  override def toString: String =
    s"YeastAggregate($id, $name, ${laboratory.name} $strain, $yeastType, $status)"
}

object YeastAggregate {

  /**
   * Crée un nouvel agrégat Yeast
   */
  def create(
    name: YeastName,
    laboratory: YeastLaboratory,
    strain: YeastStrain,
    yeastType: YeastType,
    attenuation: AttenuationRange,
    temperature: FermentationTemp,
    alcoholTolerance: AlcoholTolerance,
    flocculation: FlocculationLevel,
    characteristics: YeastCharacteristics,
    createdBy: UUID
  ): Either[DomainError, YeastAggregate] = {

    // Validation de cohérence à la création
    val tempYeast = YeastAggregate(
      id = YeastId.generate(),
      name = name,
      laboratory = laboratory,
      strain = strain,
      yeastType = yeastType,
      attenuation = attenuation,
      temperature = temperature,
      alcoholTolerance = alcoholTolerance,
      flocculation = flocculation,
      characteristics = characteristics,
      status = YeastStatus.Draft,
      version = 1,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val warnings = tempYeast.validateTechnicalCoherence
    if (warnings.nonEmpty) {
      // Log warnings but don't block creation
      println(s"Warnings for new yeast ${name.value}: ${warnings.mkString(", ")}")
    }

    val event = YeastCreated(
      yeastId = tempYeast.id,
      name = name,
      laboratory = laboratory,
      strain = strain,
      yeastType = yeastType,
      attenuation = attenuation,
      temperature = temperature,
      alcoholTolerance = alcoholTolerance,
      flocculation = flocculation,
      characteristics = characteristics,
      createdBy = createdBy,
      occurredAt = tempYeast.createdAt,
      version = 1
    )

    Right(tempYeast.copy(uncommittedEvents = List(event)))
  }

  /**
   * Reconstruit l'agrégat depuis les événements (Event Sourcing)
   */
  def fromEvents(events: List[YeastEvent]): Either[DomainError, YeastAggregate] = {
    events.headOption match {
      case Some(YeastCreated(id, name, laboratory, strain, yeastType, attenuation,
                            temperature, alcoholTolerance, flocculation, characteristics,
                            _, occurredAt, _)) =>

        val initial = YeastAggregate(
          id = id,
          name = name,
          laboratory = laboratory,
          strain = strain,
          yeastType = yeastType,
          attenuation = attenuation,
          temperature = temperature,
          alcoholTolerance = alcoholTolerance,
          flocculation = flocculation,
          characteristics = characteristics,
          status = YeastStatus.Draft,
          version = 1,
          createdAt = occurredAt,
          updatedAt = occurredAt
        )

        // Appliquer les événements suivants
        val finalAggregate = events.tail.foldLeft(initial) { (aggregate, event) =>
          applyEvent(aggregate, event)
        }

        Right(finalAggregate)

      case Some(other) =>
        Left(DomainError.InvalidState(s"Premier événement doit être YeastCreated, reçu: ${other.getClass.getSimpleName}"))
      case None =>
        Left(DomainError.InvalidState("Aucun événement fourni"))
    }
  }

  /**
   * Applique un événement à l'agrégat
   */
  private def applyEvent(aggregate: YeastAggregate, event: YeastEvent): YeastAggregate = {
    event match {
      case e: YeastUpdated =>
        // Appliquer les changements depuis la map
        var updated = aggregate
        e.changes.foreach {
          case ("name", value: String) =>
            YeastName.fromString(value).foreach(newName => updated = updated.copy(name = newName))
          case _ => // Ignorer les changements non reconnus
        }
        updated.copy(version = e.version, updatedAt = e.occurredAt)

      case e: YeastStatusChanged =>
        aggregate.copy(status = e.newStatus, version = e.version, updatedAt = e.occurredAt)

      case e: YeastActivated =>
        aggregate.copy(status = YeastStatus.Active, version = e.version, updatedAt = e.occurredAt)

      case e: YeastDeactivated =>
        aggregate.copy(status = YeastStatus.Inactive, version = e.version, updatedAt = e.occurredAt)

      case e: YeastArchived =>
        aggregate.copy(status = YeastStatus.Archived, version = e.version, updatedAt = e.occurredAt)

      case e: YeastTechnicalDataUpdated =>
        aggregate.copy(
          attenuation = e.attenuation.getOrElse(aggregate.attenuation),
          temperature = e.temperature.getOrElse(aggregate.temperature),
          alcoholTolerance = e.alcoholTolerance.getOrElse(aggregate.alcoholTolerance),
          flocculation = e.flocculation.getOrElse(aggregate.flocculation),
          characteristics = e.characteristics.getOrElse(aggregate.characteristics),
          version = e.version,
          updatedAt = e.occurredAt
        )

      case e: YeastLaboratoryInfoUpdated =>
        aggregate.copy(
          laboratory = e.laboratory,
          strain = e.strain,
          version = e.version,
          updatedAt = e.occurredAt
        )

      case _: YeastCreated =>
        // Événement de création déjà traité
        aggregate
    }
  }
}
