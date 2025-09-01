package interfaces.controllers.yeasts

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import application.yeasts.dtos.YeastDTOs._

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos.YeastSearchRequestDTO
import application.yeasts.commands.{CreateYeastCommand, DeleteYeastCommand}
import infrastructure.auth.AuthAction

@Singleton
class YeastAdminController @Inject()(
  cc: ControllerComponents,
  yeastApplicationService: YeastApplicationService,
  authAction: AuthAction
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def listYeasts(
    page: Int = 0, 
    size: Int = 20,
    name: Option[String] = None,
    laboratory: Option[String] = None, 
    yeastType: Option[String] = None,
    status: Option[String] = None
  ): Action[AnyContent] = authAction.async { request =>
    
    val searchRequest = YeastSearchRequestDTO(
      name = name,
      laboratory = laboratory,
      yeastType = yeastType,
      status = status,
      page = page,
      size = size,
      minAttenuation = None,
      maxAttenuation = None,
      minTemperature = None,
      maxTemperature = None,
      minAlcoholTolerance = None,
      maxAlcoholTolerance = None,
      flocculation = None,
      characteristics = None
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(result) => Ok(Json.toJson(result))
      case Left(errors) => BadRequest(Json.obj("errors" -> errors))
    }
  }

  def createYeast(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    // Authentification manuelle
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Basic ") =>
        try {
          val encodedCredentials = authHeader.substring(6)
          val credentials = new String(java.util.Base64.getDecoder.decode(encodedCredentials), "UTF-8")
          val Array(username, password) = credentials.split(":", 2)
          if ((username == "admin" && password == "brewing2024") || (username == "editor" && password == "ingredients2024")) {
            // Utilisateur authentifié, procéder à la création
            request.body.validate[CreateYeastCommand] match {
              case JsSuccess(command, _) =>
                yeastApplicationService.createYeast(command).map {
                  case Left(errors) => BadRequest(Json.obj("errors" -> errors))
                  case Right(yeast) => Created(Json.toJson(yeast))
                }
              case JsError(errors) =>
                Future.successful(BadRequest(Json.obj("errors" -> JsError.toJson(errors))))
            }
          } else {
            Future.successful(Unauthorized("Invalid credentials"))
          }
        } catch {
          case _: Exception => Future.successful(BadRequest("Invalid Authorization header"))
        }
      case _ => Future.successful(Unauthorized("Authorization required"))
    }
  }

  def getYeast(yeastId: String): Action[AnyContent] = Action.async { request =>
    try {
      val uuid = UUID.fromString(yeastId)
      yeastApplicationService.getYeastById(uuid).map {
        case Some(yeast) => Ok(Json.toJson(yeast))
        case None => NotFound(Json.obj("error" -> "Levure non trouvée"))
      }
    } catch {
      case _: IllegalArgumentException =>
        Future.successful(BadRequest(Json.obj("error" -> "ID invalide")))
    }
  }

  def updateYeast(yeastId: String): Action[JsValue] = Action.async(parse.json) { _ =>
    Future.successful(Ok(Json.obj("message" -> "Update not fully implemented yet")))
  }

  def deleteYeast(yeastId: String): Action[AnyContent] = authAction.async { request =>
    val command = DeleteYeastCommand(yeastId = yeastId)
    yeastApplicationService.deleteYeast(command).map {
      case Left(errors) => BadRequest(Json.obj("errors" -> errors))
      case Right(_) => NoContent
    }
  }

  def changeStatus(yeastId: String): Action[JsValue] = Action.async(parse.json) { request =>
    (request.body \ "status").asOpt[String] match {
      case Some(status) =>
        val reason = (request.body \ "reason").asOpt[String]
        yeastApplicationService.changeYeastStatus(yeastId, status, reason).map {
          case Left(errors) => BadRequest(Json.obj("errors" -> errors))
          case Right(_) => Ok(Json.obj("message" -> "Statut mis à jour avec succès"))
        }
      case None =>
        Future.successful(BadRequest(Json.obj("error" -> "Statut requis")))
    }
  }

  def activateYeast(yeastId: String): Action[AnyContent] = authAction.async { request =>
    yeastApplicationService.changeYeastStatus(yeastId, "active").map {
      case Left(errors) => BadRequest(Json.obj("errors" -> errors))
      case Right(_) => Ok(Json.obj("message" -> "Levure activée avec succès"))
    }
  }

  def deactivateYeast(yeastId: String): Action[AnyContent] = authAction.async { request =>
    yeastApplicationService.changeYeastStatus(yeastId, "inactive").map {
      case Left(errors) => BadRequest(Json.obj("errors" -> errors))
      case Right(_) => Ok(Json.obj("message" -> "Levure désactivée avec succès"))
    }
  }

  def archiveYeast(yeastId: String): Action[AnyContent] = authAction.async { request =>
    yeastApplicationService.changeYeastStatus(yeastId, "archived").map {
      case Left(errors) => BadRequest(Json.obj("errors" -> errors))
      case Right(_) => Ok(Json.obj("message" -> "Levure archivée avec succès"))
    }
  }

  def getStatistics(): Action[AnyContent] = Action.async { request =>
    Future.successful(Ok(Json.obj("message" -> "Statistics not implemented yet")))
  }

  def batchCreate(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    // Authentification manuelle
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Basic ") =>
        try {
          val encodedCredentials = authHeader.substring(6)
          val credentials = new String(java.util.Base64.getDecoder.decode(encodedCredentials), "UTF-8")
          val Array(username, password) = credentials.split(":", 2)
          if ((username == "admin" && password == "brewing2024") || (username == "editor" && password == "ingredients2024")) {
            // Utilisateur authentifié, procéder à la création par batch
            Future.successful(NotImplemented(Json.obj("message" -> "Batch create not implemented yet")))
          } else {
            Future.successful(Unauthorized("Invalid credentials"))
          }
        } catch {
          case _: Exception => Future.successful(BadRequest("Invalid Authorization header"))
        }
      case _ => Future.successful(Unauthorized("Authorization required"))
    }
  }

  def exportYeasts(format: String = "json", status: Option[String] = None): Action[AnyContent] = Action.async { request =>
    Future.successful(NotImplemented(Json.obj("message" -> "Export not implemented yet")))
  }
}
