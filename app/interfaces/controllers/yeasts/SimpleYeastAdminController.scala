package interfaces.controllers.yeasts

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos.{YeastSearchRequestDTO, YeastDTOs}
import application.yeasts.commands.{CreateYeastCommand, DeleteYeastCommand}
import infrastructure.auth.SimpleAuth

/**
 * VERSION PRODUCTION SIMPLE - YeastAdminController
 * Minimaliste, robuste, sécurisé
 */
@Singleton
class SimpleYeastAdminController @Inject()(
  cc: ControllerComponents,
  yeastApplicationService: YeastApplicationService,
  auth: SimpleAuth,
  yeastDTOs: YeastDTOs
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // READ - Liste avec authentification
  def list(page: Int = 0, size: Int = 20): Action[AnyContent] = auth.secure {
    Action.async { request =>
      val searchRequest = YeastSearchRequestDTO(
        name = None,
        laboratory = None,
        yeastType = None,
        status = None,
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
        case Right(response) => Ok(Json.toJson(response)(YeastDTOs.listResponseFormat))
        case Left(errors) => InternalServerError(Json.obj("errors" -> errors))
      }
    }
  }

  // READ - Détail avec authentification
  def get(yeastId: String): Action[AnyContent] = auth.secure {
    Action.async { request =>
      try {
        val uuid = UUID.fromString(yeastId)
        yeastApplicationService.getYeastById(uuid).map {
          case Some(yeast) => 
            val yeastResponse = yeastDTOs.fromAggregate(yeast)
            Ok(Json.toJson(yeastResponse)(YeastDTOs.responseFormat))
          case None => NotFound(Json.obj("error" -> "Levure non trouvée"))
        }
      } catch {
        case _: IllegalArgumentException =>
          Future.successful(BadRequest(Json.obj("error" -> "ID invalide")))
      }
    }
  }

  // CREATE - Avec authentification JSON
  def create(): Action[JsValue] = auth.secure {
    Action.async(parse.json) { request =>
      request.body.validate[CreateYeastCommand] match {
        case JsSuccess(command, _) =>
          yeastApplicationService.createYeast(command).map {
            case Left(errors) => BadRequest(Json.obj("errors" -> errors))
            case Right(yeast) => 
              val yeastResponse = yeastDTOs.fromAggregate(yeast)
              Created(Json.toJson(yeastResponse)(YeastDTOs.responseFormat))
          }
        case JsError(errors) =>
          Future.successful(BadRequest(Json.obj("errors" -> JsError.toJson(errors))))
      }
    }
  }

  // DELETE - Avec authentification
  def delete(yeastId: String): Action[AnyContent] = auth.secure {
    Action.async { request =>
      val command = DeleteYeastCommand(yeastId = yeastId)
      yeastApplicationService.deleteYeast(command).map {
        case Left(errors) => BadRequest(Json.obj("errors" -> errors))
        case Right(_) => Ok(Json.obj("message" -> "Supprimé"))
      }
    }
  }

  // STATUS - Actions rapides
  def activate(yeastId: String): Action[AnyContent] = auth.secure {
    Action.async { request =>
      yeastApplicationService.changeYeastStatus(yeastId, "ACTIVE").map {
        case Left(errors) => BadRequest(Json.obj("errors" -> errors))
        case Right(_) => Ok(Json.obj("message" -> "Activé"))
      }
    }
  }

  def deactivate(yeastId: String): Action[AnyContent] = auth.secure {
    Action.async { request =>
      yeastApplicationService.changeYeastStatus(yeastId, "INACTIVE").map {
        case Left(errors) => BadRequest(Json.obj("errors" -> errors))
        case Right(_) => Ok(Json.obj("message" -> "Désactivé"))
      }
    }
  }

  // HEALTH - Endpoint de santé sécurisé
  def health(): Action[AnyContent] = auth.secure {
    Action { request =>
      Ok(Json.obj(
        "status" -> "healthy",
        "service" -> "YeastAdmin",
        "version" -> "production-simple",
        "timestamp" -> System.currentTimeMillis()
      ))
    }
  }
}