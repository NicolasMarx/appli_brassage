package interfaces.http.dto.responses.public

import play.api.libs.json._
import domain.hops.model._

/**
 * DTO de réponse pour l'API publique des houblons
 * Représente un houblon formaté pour les clients externes
 */
case class HopResponse(
                        id: String,
                        name: String,
                        alphaAcid: Double,
                        betaAcid: Option[Double],
                        origin: HopOriginResponse,
                        usage: String,
                        description: Option[String],
                        aromaProfile: List[String],
                        status: String,
                        source: String,
                        credibilityScore: Int,
                        characteristics: String,
                        isReliable: Boolean,
                        isNoble: Boolean,
                        isNewWorld: Boolean,
                        isDualPurpose: Boolean,
                        createdAt: String,
                        updatedAt: String,
                        version: Int
                      )

/**
 * DTO de réponse pour l'origine d'un houblon
 */
case class HopOriginResponse(
                              code: String,
                              name: String,
                              region: String,
                              isNoble: Boolean,
                              isNewWorld: Boolean
                            )

/**
 * Formatters JSON pour les réponses
 */
object HopResponse {

  implicit val hopOriginResponseWrites: OWrites[HopOriginResponse] = Json.writes[HopOriginResponse]
  implicit val hopResponseWrites: OWrites[HopResponse] = Json.writes[HopResponse]

  /**
   * Convertit un HopAggregate du domaine vers un HopResponse pour l'API
   */
  def fromAggregate(hop: HopAggregate): HopResponse = {
    HopResponse(
      id = hop.id.value,
      name = hop.name.value,
      alphaAcid = hop.alphaAcid.value,
      betaAcid = hop.betaAcid.map(_.value),
      origin = HopOriginResponse(
        code = hop.origin.code,
        name = hop.origin.name,
        region = hop.origin.region,
        isNoble = hop.origin.isNoble,
        isNewWorld = hop.origin.isNewWorld
      ),
      usage = hop.usage match {
        case HopUsage.Bittering => "BITTERING"
        case HopUsage.Aroma => "AROMA"
        case HopUsage.DualPurpose => "DUAL_PURPOSE"
        case HopUsage.NobleHop => "NOBLE_HOP"
      },
      description = hop.description.map(_.value),
      aromaProfile = hop.aromaProfile,
      status = hop.status match {
        case HopStatus.Active => "ACTIVE"
        case HopStatus.Discontinued => "DISCONTINUED"
        case HopStatus.Limited => "LIMITED"
      },
      source = hop.source match {
        case HopSource.Manual => "MANUAL"
        case HopSource.AI_Discovery => "AI_DISCOVERED"
        case HopSource.Import => "IMPORT"
      },
      credibilityScore = hop.credibilityScore,
      characteristics = hop.characteristics,
      isReliable = hop.isReliable,
      isNoble = hop.isNoble,
      isNewWorld = hop.isNewWorld,
      isDualPurpose = hop.isDualPurpose,
      createdAt = hop.createdAt.toString,
      updatedAt = hop.updatedAt.toString,
      version = hop.version
    )
  }

  /**
   * Convertit une liste de HopAggregate vers une liste de HopResponse
   */
  def fromAggregates(hops: List[HopAggregate]): List[HopResponse] = {
    hops.map(fromAggregate)
  }

  /**
   * Convertit une séquence de HopAggregate vers une liste de HopResponse
   */
  def fromAggregates(hops: Seq[HopAggregate]): List[HopResponse] = {
    hops.map(fromAggregate).toList
  }
}

/**
 * DTO pour les réponses paginées de houblons
 */
case class HopPageResponse(
                            items: List[HopResponse],
                            currentPage: Int,
                            pageSize: Int,
                            totalCount: Int,
                            totalPages: Int,
                            hasNext: Boolean,
                            hasPrevious: Boolean
                          )

object HopPageResponse {
  implicit val hopPageResponseWrites: OWrites[HopPageResponse] = Json.writes[HopPageResponse]

  /**
   * Crée une réponse paginée à partir d'une liste de houblons et des métadonnées de pagination
   */
  def create(
              hops: List[HopAggregate],
              currentPage: Int,
              pageSize: Int,
              totalCount: Int
            ): HopPageResponse = {
    val totalPages = if (pageSize > 0) (totalCount + pageSize - 1) / pageSize else 0

    HopPageResponse(
      items = HopResponse.fromAggregates(hops),
      currentPage = currentPage,
      pageSize = pageSize,
      totalCount = totalCount,
      totalPages = totalPages,
      hasNext = currentPage < totalPages - 1,
      hasPrevious = currentPage > 0
    )
  }
}

/**
 * DTO pour les statistiques sur les houblons
 */
case class HopStatsResponse(
                             totalHops: Int,
                             activeHops: Int,
                             discontinuedHops: Int,
                             aiDiscoveredHops: Int,
                             hopsByUsage: Map[String, Int],
                             hopsByOrigin: Map[String, Int],
                             avgCredibilityScore: Double,
                             reliableHopsCount: Int
                           )

object HopStatsResponse {
  implicit val hopStatsResponseWrites: OWrites[HopStatsResponse] = Json.writes[HopStatsResponse]

  /**
   * Calcule les statistiques à partir d'une liste de houblons
   */
  def fromAggregates(hops: List[HopAggregate]): HopStatsResponse = {
    val totalHops = hops.length
    val activeHops = hops.count(_.status == HopStatus.Active)
    val discontinuedHops = hops.count(_.status == HopStatus.Discontinued)
    val aiDiscoveredHops = hops.count(_.source == HopSource.AI_Discovery)
    val reliableHopsCount = hops.count(_.isReliable)

    val hopsByUsage = hops.groupBy(_.usage).map { case (usage, hopList) =>
      val usageString = usage match {
        case HopUsage.Bittering => "BITTERING"
        case HopUsage.Aroma => "AROMA"
        case HopUsage.DualPurpose => "DUAL_PURPOSE"
        case HopUsage.NobleHop => "NOBLE_HOP"
      }
      usageString -> hopList.length
    }

    val hopsByOrigin = hops.groupBy(_.origin.code).map { case (origin, hopList) =>
      origin -> hopList.length
    }

    val avgCredibilityScore = if (totalHops > 0) {
      hops.map(_.credibilityScore).sum.toDouble / totalHops
    } else 0.0

    HopStatsResponse(
      totalHops = totalHops,
      activeHops = activeHops,
      discontinuedHops = discontinuedHops,
      aiDiscoveredHops = aiDiscoveredHops,
      hopsByUsage = hopsByUsage,
      hopsByOrigin = hopsByOrigin,
      avgCredibilityScore = avgCredibilityScore,
      reliableHopsCount = reliableHopsCount
    )
  }
}