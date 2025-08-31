package domain.malts.model

import domain.shared.NonEmptyString
import domain.common.DomainError
import java.time.Instant
import play.api.libs.json._

/**
 * Agrégat Malt - Représente un malt dans le système de brassage
 * Suit le pattern DDD avec Event Sourcing
 */
case class MaltAggregate private(
  id: MaltId,
  name: NonEmptyString,
  maltType: MaltType,
  ebcColor: EBCColor,
  extractionRate: ExtractionRate,
  diastaticPower: DiastaticPower,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: MaltSource,
  isActive: Boolean,
  credibilityScore: Double,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
) {

  // Propriétés calculées pour compatibility avec les handlers existants
  def needsReview: Boolean = credibilityScore < 0.8
  def canSelfConvert: Boolean = diastaticPower.canConvertAdjuncts
  def qualityScore: Double = credibilityScore * 100
  def maxRecommendedPercent: Option[Double] = maltType match {
    case MaltType.BASE => Some(100.0)
    case MaltType.CRYSTAL => Some(20.0)
    case MaltType.ROASTED => Some(10.0)
    case _ => Some(30.0)
  }
  def isBaseMalt: Boolean = maltType == MaltType.BASE
  
  // Propriétés métier calculées
  def colorCategory: String = ebcColor.colorName
  def extractionCategory: String = extractionRate.extractionCategory
  def enzymePowerCategory: String = diastaticPower.enzymePowerCategory
  
  // Validation métier
  def isValidForBrewing: Boolean = {
    isActive && credibilityScore >= 0.5 && 
    ebcColor.isMaltCanBeUsed && 
    extractionRate.value >= 0.6
  }

  /**
   * Désactivation du malt avec validation métier
   */
  def deactivate(reason: String): Either[DomainError, MaltAggregate] = {
    if (!isActive) {
      Left(DomainError.businessRule("Le malt est déjà désactivé", "ALREADY_DEACTIVATED"))
    } else {
      Right(this.copy(
        isActive = false,
        updatedAt = Instant.now(),
        version = version + 1
      ))
    }
  }
  
  /**
   * Mise à jour des informations du malt avec versioning
   */
  def updateInfo(
    name: Option[NonEmptyString] = None,
    maltType: Option[MaltType] = None,
    ebcColor: Option[EBCColor] = None,
    extractionRate: Option[ExtractionRate] = None,
    diastaticPower: Option[DiastaticPower] = None,
    originCode: Option[String] = None,
    description: Option[Option[String]] = None,
    flavorProfiles: Option[List[String]] = None
  ): Either[DomainError, MaltAggregate] = {
    
    val updatedFlavorProfiles = flavorProfiles.getOrElse(this.flavorProfiles)
    
    if (updatedFlavorProfiles.length > MaltAggregate.MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum ${MaltAggregate.MaxFlavorProfiles} profils arômes autorisés"))
    } else {
      Right(this.copy(
        name = name.getOrElse(this.name),
        maltType = maltType.getOrElse(this.maltType),
        ebcColor = ebcColor.getOrElse(this.ebcColor),
        extractionRate = extractionRate.getOrElse(this.extractionRate),
        diastaticPower = diastaticPower.getOrElse(this.diastaticPower),
        originCode = originCode.getOrElse(this.originCode),
        description = description.getOrElse(this.description),
        flavorProfiles = updatedFlavorProfiles.filter(_.trim.nonEmpty).distinct,
        updatedAt = Instant.now(),
        version = version + 1
      ))
    }
  }
}

object MaltAggregate {
  
  val MaxFlavorProfiles = 10
  
  /**
   * Création d'un nouveau malt avec validation complète
   */
  def create(
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    originCode: String,
    source: MaltSource,
    description: Option[String] = None,
    flavorProfiles: List[String] = List.empty
  ): Either[DomainError, MaltAggregate] = {
    
    // Validation des profils aromatiques
    val cleanedProfiles = flavorProfiles.filter(_.trim.nonEmpty).distinct
    if (cleanedProfiles.length > MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum $MaxFlavorProfiles profils arômes autorisés"))
    } else if (originCode.trim.isEmpty) {
      Left(DomainError.validation("Le code origine ne peut pas être vide"))
    } else if (originCode.length > 10) {
      Left(DomainError.validation("Le code origine ne peut excéder 10 caractères"))
    } else {
      val now = Instant.now()
      Right(MaltAggregate(
        id = MaltId.generate(),
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = originCode.trim.toUpperCase,
        description = description.map(_.trim).filter(_.nonEmpty),
        flavorProfiles = cleanedProfiles,
        source = source,
        isActive = true,
        credibilityScore = calculateInitialCredibility(source),
        createdAt = now,
        updatedAt = now,
        version = 1L
      ))
    }
  }
  
  /**
   * ❗ Constructeur direct pour la persistence (ne pas utiliser dans la logique métier)
   * Utilisé par les repositories pour reconstituer l'agrégat depuis la DB
   */
  def apply(
    id: MaltId,
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    originCode: String,
    description: Option[String],
    flavorProfiles: List[String],
    source: MaltSource,
    isActive: Boolean,
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  ): MaltAggregate = {
    new MaltAggregate(
      id, name, maltType, ebcColor, extractionRate, diastaticPower,
      originCode, description, flavorProfiles, source, isActive,
      credibilityScore, createdAt, updatedAt, version
    )
  }
  
  /**
   * Calcule le score de crédibilité initial selon la source
   */
  private def calculateInitialCredibility(source: MaltSource): Double = source match {
    case MaltSource.Manual => 1.0
    case MaltSource.AI_Discovery => 0.7
    case MaltSource.Import => 0.8
  }
}
