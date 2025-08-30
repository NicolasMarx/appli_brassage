// app/domain/hops/model/HopAggregate.scala
package domain.hops.model

import domain.common._
import domain.shared._
import java.time.Instant

case class HopAggregate private (
                                  id: HopId,
                                  name: NonEmptyString,
                                  alphaAcid: AlphaAcidPercentage,
                                  betaAcid: Option[AlphaAcidPercentage],
                                  origin: HopOrigin,
                                  usage: HopUsage,
                                  description: Option[NonEmptyString],
                                  aromaProfile: List[String], // IDs des arômes
                                  status: HopStatus,
                                  source: HopSource,
                                  credibilityScore: Int,
                                  createdAt: Instant,
                                  updatedAt: Instant,
                                  override val version: Int
                                ) extends EventSourced {

  // Méthodes de lecture (queries)
  def isActive: Boolean = status == HopStatus.Active
  def isDraft: Boolean = status == HopStatus.Draft
  def isHighAlpha: Boolean = alphaAcid.isHigh
  def isDualPurpose: Boolean = usage == HopUsage.DualPurpose
  def isNoble: Boolean = origin.isNoble
  def isFromAI: Boolean = source == HopSource.AI_Discovery
  def hasHighCredibility: Boolean = credibilityScore >= 80

  def getUsageCategory: String = usage.name
  def getAlphaCategory: String = alphaAcid.category

  // Méthodes métier (commands)
  def updateAlphaAcid(newAlphaAcid: AlphaAcidPercentage): Either[DomainError, HopAggregate] = {
    if (alphaAcid == newAlphaAcid) {
      Left(DomainError.validation("Le taux d'alpha acid est déjà celui spécifié"))
    } else {
      // Mise à jour automatique de l'usage si nécessaire
      val suggestedUsage = HopUsage.fromAlphaAcid(newAlphaAcid.value)
      val finalUsage = if (usage != suggestedUsage && usage == HopUsage.DualPurpose) usage else suggestedUsage

      val updatedHop = this.copy(
        alphaAcid = newAlphaAcid,
        usage = finalUsage,
        updatedAt = Instant.now(),
        version = version + 1
      )

      updatedHop.raise(HopAlphaAcidUpdated(
        id.value,
        alphaAcid.value,
        newAlphaAcid.value,
        finalUsage.name,
        version + 1
      ))
      Right(updatedHop)
    }
  }

  def updateDescription(newDescription: NonEmptyString): Either[DomainError, HopAggregate] = {
    if (description.contains(newDescription)) {
      Left(DomainError.validation("La description est déjà celle spécifiée"))
    } else {
      val updatedHop = this.copy(
        description = Some(newDescription),
        updatedAt = Instant.now(),
        version = version + 1
      )

      updatedHop.raise(HopDescriptionUpdated(id.value, newDescription.value, version + 1))
      Right(updatedHop)
    }
  }

  def activate(): Either[DomainError, HopAggregate] = {
    if (status == HopStatus.Active) {
      Left(DomainError.validation("Le houblon est déjà actif"))
    } else if (!hasMinimalRequiredData) {
      Left(DomainError.businessRule("Le houblon ne peut être activé : données minimales manquantes", "MINIMAL_DATA_REQUIRED"))
    } else {
      val updatedHop = this.copy(
        status = HopStatus.Active,
        updatedAt = Instant.now(),
        version = version + 1
      )

      updatedHop.raise(HopActivated(id.value, version + 1))
      Right(updatedHop)
    }
  }

  def deactivate(): Either[DomainError, HopAggregate] = {
    if (status == HopStatus.Inactive) {
      Left(DomainError.validation("Le houblon est déjà inactif"))
    } else {
      val updatedHop = this.copy(
        status = HopStatus.Inactive,
        updatedAt = Instant.now(),
        version = version + 1
      )

      updatedHop.raise(HopDeactivated(id.value, version + 1))
      Right(updatedHop)
    }
  }

  def updateCredibilityScore(newScore: Int): Either[DomainError, HopAggregate] = {
    if (newScore < 0 || newScore > 100) {
      Left(DomainError.validation("Le score de crédibilité doit être entre 0 et 100"))
    } else if (credibilityScore == newScore) {
      Left(DomainError.validation("Le score de crédibilité est déjà celui spécifié"))
    } else {
      val updatedHop = this.copy(
        credibilityScore = newScore,
        updatedAt = Instant.now(),
        version = version + 1
      )

      updatedHop.raise(HopCredibilityUpdated(id.value, credibilityScore, newScore, version + 1))
      Right(updatedHop)
    }
  }

  def addAromaProfile(aromaId: String): Either[DomainError, HopAggregate] = {
    if (aromaProfile.contains(aromaId)) {
      Left(DomainError.validation("Ce profil d'arôme est déjà associé au houblon"))
    } else {
      val updatedHop = this.copy(
        aromaProfile = aromaProfile :+ aromaId,
        updatedAt = Instant.now(),
        version = version + 1
      )

      updatedHop.raise(HopAromaAdded(id.value, aromaId, version + 1))
      Right(updatedHop)
    }
  }

  private def hasMinimalRequiredData: Boolean = {
    name.length > 0 && alphaAcid.value >= 0.0 && description.isDefined
  }
}

object HopAggregate {
  def create(
              name: NonEmptyString,
              alphaAcid: AlphaAcidPercentage,
              origin: HopOrigin,
              description: Option[NonEmptyString] = None,
              betaAcid: Option[AlphaAcidPercentage] = None,
              source: HopSource = HopSource.Manual
            ): Either[DomainError, HopAggregate] = {

    val hopId = HopId.generate()
    val now = Instant.now()
    val usage = HopUsage.fromAlphaAcid(alphaAcid.value)
    val initialCredibility = if (source == HopSource.AI_Discovery) 70 else 95

    val hop = HopAggregate(
      id = hopId,
      name = name,
      alphaAcid = alphaAcid,
      betaAcid = betaAcid,
      origin = origin,
      usage = usage,
      description = description,
      aromaProfile = List.empty,
      status = HopStatus.Draft,
      source = source,
      credibilityScore = initialCredibility,
      createdAt = now,
      updatedAt = now,
      version = 1
    )

    hop.raise(HopCreated(
      hopId.value,
      name.value,
      alphaAcid.value,
      origin.code,
      usage.name,
      source.name,
      now,
      1
    ))

    Right(hop)
  }
}

sealed trait HopStatus {
  def name: String
}

object HopStatus {
  case object Draft extends HopStatus { val name = "DRAFT" }
  case object Active extends HopStatus { val name = "ACTIVE" }
  case object Inactive extends HopStatus { val name = "INACTIVE" }
  case object PendingReview extends HopStatus { val name = "PENDING_REVIEW" }

  def fromName(name: String): Option[HopStatus] = name match {
    case "DRAFT" => Some(Draft)
    case "ACTIVE" => Some(Active)
    case "INACTIVE" => Some(Inactive)
    case "PENDING_REVIEW" => Some(PendingReview)
    case _ => None
  }
}

sealed trait HopSource {
  def name: String
}

object HopSource {
  case object Manual extends HopSource { val name = "MANUAL" }
  case object AI_Discovery extends HopSource { val name = "AI_DISCOVERY" }
  case object Import extends HopSource { val name = "IMPORT" }

  def fromName(name: String): Option[HopSource] = name match {
    case "MANUAL" => Some(Manual)
    case "AI_DISCOVERY" => Some(AI_Discovery)
    case "IMPORT" => Some(Import)
    case _ => None
  }
}