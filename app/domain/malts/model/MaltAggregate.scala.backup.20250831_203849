package domain.malts.model

import domain.shared.NonEmptyString
import domain.common.DomainError
import java.time.Instant
import play.api.libs.json._

/**
 * Agrégat Malt complet avec toutes les propriétés requises
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
    case MaltType.BASE => Some(100)
    case MaltType.CRYSTAL => Some(20)
    case MaltType.ROASTED => Some(10)
    case _ => Some(30)
  }
  def isBaseMalt: Boolean = maltType == MaltType.BASE

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
  
  def updateInfo(
    name: Option[NonEmptyString] = None,
    maltType: Option[MaltType] = None,
    ebcColor: Option[EBCColor] = None,
    extractionRate: Option[ExtractionRate] = None,
    description: Option[String] = None
  ): MaltAggregate = {
    this.copy(
      name = name.getOrElse(this.name),
      maltType = maltType.getOrElse(this.maltType),
      ebcColor = ebcColor.getOrElse(this.ebcColor),
      extractionRate = extractionRate.getOrElse(this.extractionRate),
      description = description.orElse(this.description),
      updatedAt = Instant.now(),
      version = version + 1
    )
  }
}

object MaltAggregate {
  
  val MaxFlavorProfiles = 10
  
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
    
    if (flavorProfiles.length > MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum $MaxFlavorProfiles profils arômes autorisés"))
    } else {
      val now = Instant.now()
      Right(MaltAggregate(
        id = MaltId.generate(),
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = originCode,
        description = description,
        flavorProfiles = flavorProfiles.filter(_.trim.nonEmpty).distinct,
        source = source,
        isActive = true,
        credibilityScore = if (source == MaltSource.Manual) 1.0 else 0.7,
        createdAt = now,
        updatedAt = now,
        version = 1
      ))
    }
  }
  
  // Format JSON requis pour compilation (sera corrigé après ajout NonEmptyString format)
  // implicit val format: Format[MaltAggregate] = Json.format[MaltAggregate]
}
