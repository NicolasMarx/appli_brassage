// app/controllers/admin/AdminHopsController.scala
package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import interfaces.http.dto.responses.public.HopResponse._
import interfaces.http.dto.responses.public._
import java.time.Instant

@Singleton
class AdminHopsController @Inject()(
                                     val controllerComponents: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends BaseController {

  def list(page: Int = 0, size: Int = 20) = Action.async { implicit request =>
    Future {
      val mockHops = getMockAdminHops()
      val totalCount = mockHops.length
      val startIndex = page * size
      val endIndex = math.min(startIndex + size, totalCount)
      val hopsPage = mockHops.slice(startIndex, endIndex)

      val response = AdminHopListResponse(
        hops = hopsPage,
        totalCount = totalCount,
        page = page,
        pageSize = size,
        hasNext = endIndex < totalCount
      )

      Ok(Json.toJson(response))
    }
  }

  def detail(id: String) = Action.async { implicit request =>
    Future {
      getMockAdminHops().find(_.id == id) match {
        case Some(hop) => Ok(Json.toJson(hop))
        case None => NotFound(Json.obj("error" -> "Houblon non trouvé"))
      }
    }
  }

  def create() = Action.async(parse.json) { implicit request =>
    Future {
      request.body.validate[CreateHopRequest] match {
        case JsSuccess(hopRequest, _) =>
          val newHop = AdminHopResponse(
            id = java.util.UUID.randomUUID().toString,
            name = hopRequest.name,
            alphaAcid = hopRequest.alphaAcid,
            betaAcid = hopRequest.betaAcid,
            origin = HopOriginResponse(
              hopRequest.originCode,
              getOriginName(hopRequest.originCode),
              getOriginRegion(hopRequest.originCode),
              isNoble = Set("DE", "CZ", "BE").contains(hopRequest.originCode),
              isNewWorld = Set("US", "AU", "NZ", "CA").contains(hopRequest.originCode)
            ),
            usage = hopRequest.usage,
            description = hopRequest.description,
            status = "DRAFT",
            source = "MANUAL",
            credibilityScore = 95,
            aromaProfiles = List.empty,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
          )

          Created(Json.toJson(newHop))

        case JsError(errors) =>
          BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      }
    }
  }

  def update(id: String) = Action.async(parse.json) { implicit request =>
    Future {
      request.body.validate[UpdateHopRequest] match {
        case JsSuccess(hopRequest, _) =>
          getMockAdminHops().find(_.id == id) match {
            case Some(existingHop) =>
              val updatedHop = existingHop.copy(
                name = hopRequest.name.getOrElse(existingHop.name),
                alphaAcid = hopRequest.alphaAcid.getOrElse(existingHop.alphaAcid),
                betaAcid = hopRequest.betaAcid.orElse(existingHop.betaAcid),
                description = hopRequest.description.orElse(existingHop.description),
                updatedAt = Instant.now()
              )
              Ok(Json.toJson(updatedHop))
            case None =>
              NotFound(Json.obj("error" -> "Houblon non trouvé"))
          }
        case JsError(errors) =>
          BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      }
    }
  }

  def delete(id: String) = Action.async { implicit request =>
    Future {
      getMockAdminHops().find(_.id == id) match {
        case Some(_) =>
          Ok(Json.obj("message" -> "Houblon supprimé avec succès"))
        case None =>
          NotFound(Json.obj("error" -> "Houblon non trouvé"))
      }
    }
  }

  private def getMockAdminHops(): List[AdminHopResponse] = List(
    AdminHopResponse(
      id = "cascade-001",
      name = "Cascade",
      alphaAcid = 5.5,
      betaAcid = Some(4.8),
      origin = HopOriginResponse("US", "États-Unis", "Amérique du Nord", false, true),
      usage = "AROMA",
      description = Some("Houblon américain iconique aux arômes d'agrumes et floraux"),
      status = "ACTIVE",
      source = "MANUAL",
      credibilityScore = 98,
      aromaProfiles = List(
        AromaProfileResponse("citrus", "Agrume", "fruity", Some(9)),
        AromaProfileResponse("floral", "Floral", "floral", Some(7))
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    ),
    AdminHopResponse(
      id = "galaxy-004",
      name = "Galaxy",
      alphaAcid = 14.2,
      betaAcid = Some(5.8),
      origin = HopOriginResponse("AU", "Australie", "Océanie", false, true),
      usage = "DUAL_PURPOSE",
      description = Some("Houblon australien aux arômes tropicaux intenses"),
      status = "ACTIVE",
      source = "MANUAL",
      credibilityScore = 97,
      aromaProfiles = List(
        AromaProfileResponse("tropical", "Fruits tropicaux", "fruity", Some(10)),
        AromaProfileResponse("citrus", "Agrume", "fruity", Some(8))
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  )

  private def getOriginName(code: String): String = code match {
    case "US" => "États-Unis"
    case "DE" => "Allemagne"
    case "CZ" => "République Tchèque"
    case "UK" => "Royaume-Uni"
    case "AU" => "Australie"
    case "NZ" => "Nouvelle-Zélande"
    case "BE" => "Belgique"
    case "FR" => "France"
    case "CA" => "Canada"
    case "JP" => "Japon"
    case _ => "Inconnu"
  }

  private def getOriginRegion(code: String): String = code match {
    case "US" | "CA" => "Amérique du Nord"
    case "DE" | "CZ" | "UK" | "BE" | "FR" => "Europe"
    case "AU" | "NZ" => "Océanie"
    case "JP" => "Asie"
    case _ => "Inconnu"
  }
}