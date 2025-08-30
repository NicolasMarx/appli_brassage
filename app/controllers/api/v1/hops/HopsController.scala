package controllers.api.v1.hops

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import domain.hops.model._
import domain.hops.repositories.HopReadRepository

// DTO pour les réponses de l'API
case class HopResponse(
                        id: String,
                        name: String,
                        alphaAcid: Double,
                        betaAcid: Option[Double],
                        origin: HopOriginResponse,
                        usage: String,
                        description: Option[String],
                        status: String,
                        source: String,
                        credibilityScore: Int,
                        aromaProfile: List[String],
                        createdAt: String,
                        updatedAt: String,
                        version: Int
                      )

case class HopOriginResponse(
                              code: String,
                              name: String,
                              region: String,
                              isNoble: Boolean,
                              isNewWorld: Boolean
                            )

case class HopSearchParams(
                            name: Option[String] = None,
                            origin: Option[String] = None,
                            usage: Option[String] = None,
                            minAlphaAcid: Option[Double] = None,
                            maxAlphaAcid: Option[Double] = None
                          )

// JSON formatters
object HopResponse {
  implicit val hopOriginResponseWrites: OWrites[HopOriginResponse] = Json.writes[HopOriginResponse]
  implicit val hopResponseWrites: OWrites[HopResponse] = Json.writes[HopResponse]
  implicit val hopSearchParamsReads: Reads[HopSearchParams] = Json.reads[HopSearchParams]
}

@Singleton
class HopsController @Inject()(
                                cc: ControllerComponents,
                                hopRepository: HopReadRepository
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  import HopResponse._

  def list(page: Int = 0, size: Int = 20) = Action.async { _ =>
    hopRepository.findActiveHops(page, size).map { case (hops, totalCount) =>
      val hopResponses = hops.map(convertToResponse)
      Ok(Json.obj(
        "items" -> hopResponses,
        "currentPage" -> page,
        "pageSize" -> size,
        "totalCount" -> totalCount,
        "hasNext" -> ((page + 1) * size < totalCount)
      ))
    }.recover {
      case ex: Exception =>
        InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération des houblons: ${ex.getMessage}"))
    }
  }

  def detail(id: String) = Action.async { _ =>
    val hopId = HopId(id)
    hopRepository.findById(hopId).map {
      case Some(hop) => Ok(Json.toJson(convertToResponse(hop)))
      case None => NotFound(Json.obj("error" -> s"Houblon $id non trouvé"))
    }.recover {
      case ex: Exception =>
        InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération du houblon: ${ex.getMessage}"))
    }
  }

  def search = Action(parse.json).async { implicit request =>
    val searchParams = extractSearchParams(request.body)

    hopRepository.searchHops(
      name = searchParams.name,
      originCode = searchParams.origin,
      usage = searchParams.usage,
      minAlphaAcid = searchParams.minAlphaAcid,
      maxAlphaAcid = searchParams.maxAlphaAcid,
      activeOnly = true
    ).map { hops =>
      val hopResponses = hops.map(convertToResponse)
      Ok(Json.obj(
        "items" -> hopResponses,
        "totalCount" -> hops.length
      ))
    }.recover {
      case ex: Exception =>
        InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche: ${ex.getMessage}"))
    }
  }

  /**
   * Convertit un HopAggregate en HopResponse pour l'API
   */
  private def convertToResponse(hop: HopAggregate): HopResponse = {
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
      aromaProfile = hop.aromaProfile,
      createdAt = hop.createdAt.toString,
      updatedAt = hop.updatedAt.toString,
      version = hop.version
    )
  }

  /**
   * Extrait les paramètres de recherche du JSON de la requête
   */
  private def extractSearchParams(json: JsValue): HopSearchParams = {
    json.validate[HopSearchParams].getOrElse(HopSearchParams())
  }
}