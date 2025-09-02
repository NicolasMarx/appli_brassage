package domain.yeasts.model

import domain.common.{DomainError, EventSourced}
import domain.shared.NonEmptyString
import java.time.Instant
import play.api.libs.json._

/**
 * Agrégat Yeast - Racine d'agrégat pour la gestion des levures
 * CORRECTION: Ajout des méthodes manquantes et propriétés manquantes
 */
case class YeastAggregate private (
  id: YeastId,
  name: NonEmptyString,
  strain: YeastStrain,
  yeastType: YeastType,
  laboratory: YeastLaboratory,
  attenuationRange: AttenuationRange,
  fermentationTemp: FermentationTemp,
  flocculation: FlocculationLevel,
  alcoholTolerance: AlcoholTolerance,
  description: Option[String],
  characteristics: YeastCharacteristics,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
  aggregateVersion: Int = 1
) extends EventSourced {

  def getUncommittedEvents: List[YeastEvent] = uncommittedEvents.asInstanceOf[List[YeastEvent]]
  
  override def version: Int = aggregateVersion

  // Méthodes métier
  def activate(): Either[DomainError, YeastAggregate] = {
    if (isActive) {
      Left(DomainError.businessRule("La levure est déjà active", "ALREADY_ACTIVE"))
    } else {
      val activated = this.copy(isActive = true, updatedAt = Instant.now(), aggregateVersion = aggregateVersion + 1)
      activated.raise(YeastActivated(id.asString, aggregateVersion + 1))
      Right(activated)
    }
  }

  def deactivate(): Either[DomainError, YeastAggregate] = {
    if (!isActive) {
      Left(DomainError.businessRule("La levure est déjà inactive", "ALREADY_INACTIVE"))
    } else {
      val deactivated = this.copy(isActive = false, updatedAt = Instant.now(), aggregateVersion = aggregateVersion + 1)
      deactivated.raise(YeastDeactivated(id.asString, aggregateVersion + 1))
      Right(deactivated)
    }
  }

  def updateBasicInfo(
    name: Option[NonEmptyString] = None,
    strain: Option[YeastStrain] = None,
    description: Option[String] = None
  ): Either[DomainError, YeastAggregate] = {
    val updated = this.copy(
      name = name.getOrElse(this.name),
      strain = strain.getOrElse(this.strain),
      description = description.orElse(this.description),
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
    updated.raise(YeastBasicInfoUpdated(id.asString, updated.name.value, updated.strain.value, aggregateVersion + 1))
    Right(updated)
  }

  // Méthodes pour serialization JSON (utilisées dans YeastAggregate ligne 285)
  def toJson: JsValue = Json.obj(
    "id" -> id.asString,
    "nom" -> name.value,
    "souche" -> strain.value,
    "type" -> yeastType.name,
    "laboratoire" -> laboratory.name,
    "attenuation_min" -> attenuationRange.min,
    "attenuation_max" -> attenuationRange.max,
    "temp_fermentation_min" -> fermentationTemp.min,
    "temp_fermentation_max" -> fermentationTemp.max,
    "floculation" -> flocculation.name,
    "temps_clarification" -> flocculation.clarificationTime, // CORRECTION: propriété manquante ajoutée
    "tolerance_alcool" -> alcoholTolerance.value,
    "description" -> description,
    "caractéristiques" -> characteristics.toJson,
    "actif" -> isActive,
    "créé_le" -> createdAt.toString,
    "modifié_le" -> updatedAt.toString,
    "version" -> aggregateVersion
  )
}

object YeastAggregate {
  
  def create(
    id: YeastId,
    name: NonEmptyString,
    strain: YeastStrain,
    yeastType: YeastType,
    laboratory: YeastLaboratory,
    attenuationRange: AttenuationRange,
    fermentationTemp: FermentationTemp,
    flocculation: FlocculationLevel,
    alcoholTolerance: AlcoholTolerance,
    description: Option[String] = None,
    characteristics: YeastCharacteristics
  ): YeastAggregate = {
    val now = Instant.now()
    val yeast = YeastAggregate(
      id = id,
      name = name,
      strain = strain,
      yeastType = yeastType,
      laboratory = laboratory,
      attenuationRange = attenuationRange,
      fermentationTemp = fermentationTemp,
      flocculation = flocculation,
      alcoholTolerance = alcoholTolerance,
      description = description,
      characteristics = characteristics,
      isActive = true,
      createdAt = now,
      updatedAt = now,
      aggregateVersion = 1
    )
    yeast.raise(YeastCreated(id.asString, name.value, yeastType.name, laboratory.name, 1))
    yeast
  }

  implicit val format: Format[YeastAggregate] = Json.format[YeastAggregate]
}
