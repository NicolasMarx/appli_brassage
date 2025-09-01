package interfaces.controllers.yeasts

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

import application.yeasts.services.YeastApplicationService
import application.yeasts.utils.YeastApplicationUtils

@Singleton
class YeastPublicController @Inject()(
  cc: ControllerComponents,
  yeastApplicationService: YeastApplicationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def listActiveYeasts(
    page: Int = 0,
    size: Int = 20, 
    name: Option[String] = None,
    laboratory: Option[String] = None,
    yeastType: Option[String] = None
  ): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "List yeasts not implemented yet")))
  }

  def searchYeasts(q: String, limit: Int = 20): Action[AnyContent] = Action.async { implicit request =>
    if (q.length < 2) {
      Future.successful(BadRequest(Json.toJson(
        YeastApplicationUtils.buildErrorResponse("Search term too short (minimum 2 characters)")
      )))
    } else {
      yeastApplicationService.searchYeasts(q, Some(limit)).map {
        case Right(yeasts) => Ok(Json.toJson(yeasts))
        case Left(error) => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
      }
    }
  }

  def getYeastsByType(yeastType: String, limit: Int = 20): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Get yeasts by type not implemented yet")))
  }

  def getYeastsByLaboratory(lab: String, limit: Int = 20): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Get yeasts by laboratory not implemented yet")))
  }

  def getYeast(yeastId: String): Action[AnyContent] = Action.async { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(uuid) => 
        yeastApplicationService.getYeastById(uuid).map {
          case Some(yeast) => Ok(Json.toJson(yeast))
          case None => NotFound(Json.obj("error" -> "Yeast not found"))
        }
    }
  }

  def getAlternatives(yeastId: String, reason: String = "unavailable", limit: Int = 5): Action[AnyContent] = Action.async { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(uuid) => Future.successful(Ok(Json.obj("message" -> "Alternatives not implemented yet")))
    }
  }

  def getBeginnerRecommendations(limit: Int = 5): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Beginner recommendations not implemented yet")))
  }

  def getSeasonalRecommendations(season: String = "current", limit: Int = 8): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Seasonal recommendations not implemented yet")))
  }

  def getExperimentalRecommendations(limit: Int = 6): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Experimental recommendations not implemented yet")))
  }

  def getRecommendationsForBeerStyle(
    style: String, 
    targetAbv: Option[Double] = None,
    fermentationTemp: Option[Int] = None,
    limit: Int = 10
  ): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Style recommendations not implemented yet")))
  }

  def getPublicStats(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Public stats not implemented yet")))
  }

  def getPopularYeasts(limit: Int = 10): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("message" -> "Popular yeasts not implemented yet")))
  }
}
