package domain.yeasts.model

import domain.shared.DomainEvent
import java.time.Instant
import java.util.UUID

/**
 * Events du domaine Yeast
 * Implémentation Event Sourcing pour traçabilité complète
 */
sealed trait YeastEvent extends DomainEvent {
  def yeastId: YeastId
  def occurredAt: Instant
  def version: Long
}

object YeastEvent {
  
  /**
   * Event : Création d'une nouvelle levure
   */
  case class YeastCreated(
    yeastId: YeastId,
    name: YeastName,
    laboratory: YeastLaboratory,
    strain: YeastStrain,
    yeastType: YeastType,
    attenuation: AttenuationRange,
    temperature: FermentationTemp,
    alcoholTolerance: AlcoholTolerance,
    flocculation: FlocculationLevel,
    characteristics: YeastCharacteristics,
    createdBy: UUID,
    occurredAt: Instant,
    version: Long = 1
  ) extends YeastEvent
  
  /**
   * Event : Mise à jour d'une levure
   */
  case class YeastUpdated(
    yeastId: YeastId,
    changes: Map[String, Any],
    updatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Changement de statut
   */
  case class YeastStatusChanged(
    yeastId: YeastId,
    oldStatus: YeastStatus,
    newStatus: YeastStatus,
    reason: Option[String],
    changedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Levure activée
   */
  case class YeastActivated(
    yeastId: YeastId,
    activatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Levure désactivée
   */
  case class YeastDeactivated(
    yeastId: YeastId,
    reason: Option[String],
    deactivatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Levure archivée
   */
  case class YeastArchived(
    yeastId: YeastId,
    reason: Option[String],
    archivedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Données techniques mises à jour
   */
  case class YeastTechnicalDataUpdated(
    yeastId: YeastId,
    attenuation: Option[AttenuationRange],
    temperature: Option[FermentationTemp],
    alcoholTolerance: Option[AlcoholTolerance],
    flocculation: Option[FlocculationLevel],
    characteristics: Option[YeastCharacteristics],
    updatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Information laboratoire mise à jour
   */
  case class YeastLaboratoryInfoUpdated(
    yeastId: YeastId,
    laboratory: YeastLaboratory,
    strain: YeastStrain,
    updatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
}
