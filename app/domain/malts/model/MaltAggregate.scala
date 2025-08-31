package domain.malts.model

import domain.common._
import domain.shared._
import java.time.Instant

/**
 * Agrégat MaltAggregate - Racine d'agrégat pour la gestion des malts
 * Encapsule la logique métier et maintient la cohérence des données malt
 * Suit le pattern HopAggregate pour cohérence architecturale
 */
case class MaltAggregate private (
                                   id: MaltId,
                                   name: NonEmptyString,
                                   maltType: MaltType,
                                   ebcColor: EBCColor,
                                   extractionRate: ExtractionRate,
                                   diastaticPower: DiastaticPower,
                                   origin: Origin,
                                   description: Option[String],
                                   status: MaltStatus,
                                   source: MaltSource,
                                   credibilityScore: MaltCredibility,
                                   flavorProfiles: List[String], // Profiles arômes (chocolate, biscuit, etc.)
                                   createdAt: Instant,
                                   updatedAt: Instant,
                                   override val version: Int
                                 ) extends EventSourced {

  // Méthodes de lecture (queries)
  def isActive: Boolean = status == MaltStatus.ACTIVE || status == MaltStatus.SEASONAL
  def isAvailable: Boolean = status.isAvailable
  def isBaseMalt: Boolean = maltType == MaltType.BASE && ebcColor.isBaseMalt
  def isSpecialtyMalt: Boolean = maltType != MaltType.BASE
  def needsReview: Boolean = credibilityScore.needsHumanReview
  def canSelfConvert: Boolean = diastaticPower.isSelfConverting
  def canConvertAdjuncts: Boolean = diastaticPower.canConvertAdjuncts

  /**
   * Calcul du pourcentage maximum recommandé dans une recette
   */
  def maxRecommendedPercent: Double = maltType match {
    case MaltType.BASE => 100.0
    case MaltType.SPECIALTY =>
      if (ebcColor.value <= 50) 40.0 else 20.0
    case MaltType.CRYSTAL => 15.0
    case MaltType.ROASTED => 5.0
    case MaltType.ADJUNCT => 30.0
  }

  /**
   * Vérification de compatibilité avec un style de bière
   */
  def isCompatibleWithStyle(beerStyleEBC: (Double, Double)): Boolean = {
    val (minEBC, maxEBC) = beerStyleEBC
    ebcColor.value >= minEBC && ebcColor.value <= maxEBC
  }

  // Méthodes métier (commands)
  def updateBasicInfo(
                       newName: NonEmptyString,
                       newDescription: Option[String]
                     ): Either[DomainError, MaltAggregate] = {
    if (newName == name && newDescription == description) {
      Left(DomainError.validation("Aucune modification détectée"))
    } else {
      val updatedMalt = this.copy(
        name = newName,
        description = newDescription,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltBasicInfoUpdated(id.value, newName.value, newDescription, version + 1))
      Right(updatedMalt)
    }
  }

  def updateMaltingSpecs(
                          newEbcColor: EBCColor,
                          newExtractionRate: ExtractionRate,
                          newDiastaticPower: DiastaticPower
                        ): Either[DomainError, MaltAggregate] = {
    // Validation cohérence maltage
    if (newEbcColor.value > 300 && newDiastaticPower.value > 0) {
      Left(DomainError.businessRule(
        "Un malt très foncé (>300 EBC) ne peut pas avoir de pouvoir diastasique",
        "DARK_MALT_WITH_ENZYMES"
      ))
    } else if (maltType == MaltType.BASE && !newEbcColor.isBaseMalt) {
      Left(DomainError.businessRule(
        "Un malt de base doit avoir une couleur compatible (<25 EBC)",
        "BASE_MALT_TOO_DARK"
      ))
    } else {
      val updatedMalt = this.copy(
        ebcColor = newEbcColor,
        extractionRate = newExtractionRate,
        diastaticPower = newDiastaticPower,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltSpecsUpdated(
        id.value,
        newEbcColor.value,
        newExtractionRate.value,
        newDiastaticPower.value,
        version + 1
      ))
      Right(updatedMalt)
    }
  }

  def updateFlavorProfiles(newProfiles: List[String]): Either[DomainError, MaltAggregate] = {
    val cleanProfiles = newProfiles.filter(_.trim.nonEmpty).distinct
    if (cleanProfiles == flavorProfiles) {
      Left(DomainError.validation("Aucune modification des profils arômes"))
    } else if (cleanProfiles.length > 10) {
      Left(DomainError.validation("Maximum 10 profils arômes par malt"))
    } else {
      val updatedMalt = this.copy(
        flavorProfiles = cleanProfiles,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltFlavorProfilesUpdated(id.value, cleanProfiles, version + 1))
      Right(updatedMalt)
    }
  }

  def changeStatus(newStatus: MaltStatus, reason: String): Either[DomainError, MaltAggregate] = {
    if (newStatus == status) {
      Left(DomainError.validation("Le statut est déjà celui spécifié"))
    } else if (reason.trim.isEmpty) {
      Left(DomainError.validation("Une raison est requise pour changer le statut"))
    } else {
      val updatedMalt = this.copy(
        status = newStatus,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltStatusChanged(id.value, status.name, newStatus.name, reason, version + 1))
      Right(updatedMalt)
    }
  }

  def adjustCredibilityScore(
                              newScore: MaltCredibility,
                              adjustmentReason: String
                            ): Either[DomainError, MaltAggregate] = {
    if (newScore == credibilityScore) {
      Left(DomainError.validation("Le score de crédibilité est identique"))
    } else if (adjustmentReason.trim.isEmpty) {
      Left(DomainError.validation("Une raison est requise pour ajuster le score"))
    } else {
      val updatedMalt = this.copy(
        credibilityScore = newScore,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltCredibilityAdjusted(
        id.value,
        credibilityScore.value,
        newScore.value,
        adjustmentReason,
        version + 1
      ))
      Right(updatedMalt)
    }
  }

  def deactivate(reason: String): Either[DomainError, MaltAggregate] = {
    if (status == MaltStatus.INACTIVE) {
      Left(DomainError.validation("Le malt est déjà inactif"))
    } else if (reason.trim.isEmpty) {
      Left(DomainError.validation("Une raison est requise pour désactiver le malt"))
    } else {
      val updatedMalt = this.copy(
        status = MaltStatus.INACTIVE,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltDeactivated(id.value, reason, version + 1))
      Right(updatedMalt)
    }
  }

  def activate(): Either[DomainError, MaltAggregate] = {
    if (status == MaltStatus.ACTIVE) {
      Left(DomainError.validation("Le malt est déjà actif"))
    } else {
      val updatedMalt = this.copy(
        status = MaltStatus.ACTIVE,
        updatedAt = Instant.now(),
        version = version + 1
      )
      updatedMalt.raise(MaltActivated(id.value, version + 1))
      Right(updatedMalt)
    }
  }

  /**
   * Calcul du score de qualité global du malt
   * Basé sur crédibilité + cohérence des données + complétude
   */
  def qualityScore: Int = {
    val credibilityWeight = credibilityScore.value * 0.6
    val completenessWeight = {
      val hasDescription = description.exists(_.trim.nonEmpty)
      val hasFlavorProfiles = flavorProfiles.nonEmpty
      val hasOrigin = origin.name.nonEmpty
      val completeness = List(hasDescription, hasFlavorProfiles, hasOrigin).count(identity) / 3.0
      completeness * 20
    }
    val consistencyWeight = {
      val specConsistency = if (isBaseMalt && canSelfConvert) 20 else 10
      specConsistency
    }

    math.round(credibilityWeight + completenessWeight + consistencyWeight).toInt
  }
}

object MaltAggregate {
  val MaxFlavorProfiles = 10
  val MinCredibilityForProduction = 50

  def create(
              name: NonEmptyString,
              maltType: MaltType,
              ebcColor: EBCColor,
              extractionRate: ExtractionRate,
              diastaticPower: DiastaticPower,
              origin: Origin,
              source: MaltSource,
              description: Option[String] = None,
              flavorProfiles: List[String] = List.empty
            ): Either[DomainError, MaltAggregate] = {

    // Validation cohérence création
    if (maltType == MaltType.BASE && !ebcColor.isBaseMalt) {
      Left(DomainError.businessRule(
        "Un malt de base doit avoir une couleur compatible (<25 EBC)",
        "BASE_MALT_COLOR_INCOMPATIBLE"
      ))
    } else if (ebcColor.value > 300 && diastaticPower.value > 0) {
      Left(DomainError.businessRule(
        "Un malt très foncé (>300 EBC) ne peut pas avoir de pouvoir diastasique",
        "DARK_MALT_WITH_ENZYMES"
      ))
    } else if (flavorProfiles.length > MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum $MaxFlavorProfiles profils arômes par malt"))
    } else {
      val maltId = MaltId.generate()
      val now = Instant.now()
      val cleanProfiles = flavorProfiles.filter(_.trim.nonEmpty).distinct

      val malt = MaltAggregate(
        id = maltId,
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        origin = origin,
        description = description,
        status = MaltStatus.ACTIVE,
        source = source,
        credibilityScore = MaltCredibility.fromSource(source),
        flavorProfiles = cleanProfiles,
        createdAt = now,
        updatedAt = now,
        version = 1
      )

      malt.raise(MaltCreated(
        maltId.value,
        name.value,
        maltType.name,
        ebcColor.value,
        extractionRate.value,
        diastaticPower.value,
        origin.code,
        source.name,
        now,
        1
      ))
      Right(malt)
    }
  }
}