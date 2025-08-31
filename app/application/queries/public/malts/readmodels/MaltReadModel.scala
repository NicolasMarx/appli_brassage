package application.queries.public.malts.readmodels

import domain.malts.model._
import java.time.Instant

/**
 * ReadModel simplifié pour l'API publique
 * Masque les informations sensibles (crédibilité, source, etc.)
 */
case class MaltReadModel(
                          id: String,
                          name: String,
                          maltType: String,
                          ebcColor: Double,
                          extractionRate: Double,
                          diastaticPower: Double,
                          origin: OriginReadModel,
                          description: Option[String],
                          flavorProfiles: List[String],
                          colorName: String,
                          extractionCategory: String,
                          enzymaticCategory: String,
                          maxRecommendedPercent: Double,
                          isBaseMalt: Boolean,
                          canSelfConvert: Boolean,
                          createdAt: Instant,
                          updatedAt: Instant
                        )

case class OriginReadModel(
                            code: String,
                            name: String,
                            region: String,
                            isNoble: Boolean,
                            isNewWorld: Boolean
                          )

object MaltReadModel {
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.value,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      origin = OriginReadModel(
        code = malt.origin.code,
        name = malt.origin.name,
        region = malt.origin.region,
        isNoble = malt.origin.isNoble,
        isNewWorld = malt.origin.isNewWorld
      ),
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      colorName = malt.ebcColor.colorName,
      extractionCategory = malt.extractionRate.extractionCategory,
      enzymaticCategory = malt.diastaticPower.enzymaticCategory,
      maxRecommendedPercent = malt.maxRecommendedPercent,
      isBaseMalt = malt.isBaseMalt,
      canSelfConvert = malt.canSelfConvert,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
}
