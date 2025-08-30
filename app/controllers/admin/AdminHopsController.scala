package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

import application.services.hops.{HopService, CreateHopData, UpdateHopData}
import domain.hops.model._
import interfaces.http.dto.responses.public.HopOriginResponse

/**
 * DTO pour les requêtes de création de houblon (admin)
 */
case class CreateHopRequest(
                             name: String,
                             alphaAcid: Double,
                             betaAcid: Option[Double],
                             originCode: String,
                             usage: String,
                             description: Option[String]
                           )

/**
 * DTO pour les requêtes de mise à jour de houblon (admin)
 */
case class UpdateHopRequest(
                             name: Option[String],
                             alphaAcid: Option[Double],
                             betaAcid: Option[Double],
                             description: Option[String]
                           )

/**
 * DTO de réponse pour l'admin (plus détaillé que l'API publique)
 */
case class AdminHopResponse(
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
                             aromaProfiles: List[String],
                             createdAt: Instant,
                             updatedAt: Instant,
                             version: Int
                           )

/**
 * DTO pour les listes paginées d'admin
 */
case class AdminHopListResponse(
                                 hops: List[AdminHopResponse],
                                 totalCount: Int,
                                 page: Int,
                                 pageSize: Int,
                                 hasNext: Boolean
                               )

/**
 * JSON formatters pour les DTOs admin
 */
object AdminHopDTOs {
  // Import nécessaire pour HopOriginResponse
  import interfaces.http.dto.responses.public.HopOriginResponse

  implicit val createHopRequestReads: Reads[CreateHopRequest] = Json.reads[CreateHopRequest]
  implicit val updateHopRequestReads: Reads[UpdateHopRequest] = Json.reads[UpdateHopRequest]

  // Formatter pour HopOriginResponse (si pas déjà défini ailleurs)
  implicit val hopOriginResponseWrites: Writes[HopOriginResponse] = Json.writes[HopOriginResponse]

  implicit val adminHopResponseWrites: Writes[AdminHopResponse] = Json.writes[AdminHopResponse]
  implicit val adminHopListResponseWrites: Writes[AdminHopListResponse] = Json.writes[AdminHopListResponse]
}

@Singleton
class AdminHopsController @Inject()(
                                     cc: ControllerComponents,
                                     hopService: HopService
                                   )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  import AdminHopDTOs._

  def list(page: Int = 0, size: Int = 20) = Action.async { _ =>
    hopService.getAllHops(page, size).map { case (hops, totalCount) =>
      val adminHops = hops.map(convertToAdminResponse)
      val response = AdminHopListResponse(
        hops = adminHops,
        totalCount = totalCount,
        page = page,
        pageSize = size,
        hasNext = (page + 1) * size < totalCount
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex =>
        InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération: ${ex.getMessage}"))
    }
  }

  def detail(id: String) = Action.async { _ =>
    hopService.getHopById(id).map {
      case Some(hop) => Ok(Json.toJson(convertToAdminResponse(hop)))
      case None => NotFound(Json.obj("error" -> s"Houblon $id non trouvé"))
    }.recover {
      case ex =>
        InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération: ${ex.getMessage}"))
    }
  }

  def create = Action(parse.json).async { implicit request =>
    request.body.validate[CreateHopRequest] match {
      case JsSuccess(hopRequest, _) =>
        try {
          val origin = HopOrigin.fromCode(hopRequest.originCode).getOrElse(
            HopOrigin(hopRequest.originCode, hopRequest.originCode, "Unknown")
          )

          val usage = HopUsage.fromString(hopRequest.usage)

          val createData = CreateHopData(
            id = UUID.randomUUID().toString,
            name = hopRequest.name,
            alphaAcid = hopRequest.alphaAcid,
            origin = origin,
            usage = usage,
            betaAcid = hopRequest.betaAcid,
            description = hopRequest.description
          )

          hopService.createHop(createData).map { newHop =>
            Created(Json.toJson(convertToAdminResponse(newHop)))
          }.recover {
            case ex =>
              InternalServerError(Json.obj("error" -> s"Erreur lors de la création: ${ex.getMessage}"))
          }
        } catch {
          case ex: Exception =>
            Future.successful(BadRequest(Json.obj("error" -> s"Données invalides: ${ex.getMessage}")))
        }

      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> "Données invalides", "details" -> JsError.toJson(errors))))
    }
  }

  def update(id: String) = Action(parse.json).async { implicit request =>
    request.body.validate[UpdateHopRequest] match {
      case JsSuccess(hopRequest, _) =>
        val updateData = UpdateHopData(
          alphaAcid = hopRequest.alphaAcid,
          betaAcid = hopRequest.betaAcid.map(Some(_)),
          description = hopRequest.description.map(Some(_))
        )

        hopService.updateHop(id, updateData).map {
          case Some(updatedHop) => Ok(Json.toJson(convertToAdminResponse(updatedHop)))
          case None => NotFound(Json.obj("error" -> s"Houblon $id non trouvé"))
        }.recover {
          case ex =>
            InternalServerError(Json.obj("error" -> s"Erreur lors de la mise à jour: ${ex.getMessage}"))
        }

      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> "Données invalides", "details" -> JsError.toJson(errors))))
    }
  }

  def delete(id: String) = Action.async { _ =>
    hopService.deleteHop(id).map { deleted =>
      if (deleted) {
        NoContent
      } else {
        NotFound(Json.obj("error" -> s"Houblon $id non trouvé"))
      }
    }.recover {
      case ex =>
        InternalServerError(Json.obj("error" -> s"Erreur lors de la suppression: ${ex.getMessage}"))
    }
  }

  /**
   * Convertit un HopAggregate vers AdminHopResponse
   */
  private def convertToAdminResponse(hop: HopAggregate): AdminHopResponse = {
    AdminHopResponse(
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
      aromaProfiles = hop.aromaProfile,
      createdAt = hop.createdAt,
      updatedAt = hop.updatedAt,
      version = hop.version
    )
  }
}