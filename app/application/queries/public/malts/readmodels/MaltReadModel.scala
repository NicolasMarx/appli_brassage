package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel pour les malts (interface publique)
 * Projection optimisée pour les APIs publiques
 */
case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  characteristics: MaltCharacteristics,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  isActive: Boolean,
  qualityScore: Double,
  createdAt: Instant,
  updatedAt: Instant
)

case class MaltCharacteristics(
  ebcColor: Double,
  colorName: String,
  extractionRate: Double,
  extractionCategory: String,
  diastaticPower: Double,
  enzymaticCategory: String,  // ✅ Corrigé: utilisera enzymePowerCategory
  canSelfConvert: Boolean,
  isBaseMalt: Boolean,
  maxRecommendedPercent: Option[Double]
)

object MaltReadModel {
  
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.value,
      name = malt.name.value,
      maltType = malt.maltType.name,
      characteristics = MaltCharacteristics(
        ebcColor = malt.ebcColor.value,
        colorName = malt.ebcColor.colorName,
        extractionRate = malt.extractionRate.value,
        extractionCategory = malt.extractionRate.extractionCategory,
        diastaticPower = malt.diastaticPower.value,
        enzymaticCategory = malt.diastaticPower.enzymePowerCategory, // ✅ CORRIGÉ ICI
        canSelfConvert = malt.canSelfConvert,
        isBaseMalt = malt.isBaseMalt,
        maxRecommendedPercent = malt.maxRecommendedPercent
      ),
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      isActive = malt.isActive,
      qualityScore = malt.qualityScore,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
  
  implicit val characteristicsFormat: Format[MaltCharacteristics] = Json.format[MaltCharacteristics]
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
