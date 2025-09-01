package interfaces.controllers.yeasts

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

import application.yeasts.services.YeastApplicationService
import application.yeasts.utils.YeastApplicationUtils
import application.yeasts.dtos.YeastDTOs._
import application.yeasts.dtos.YeastSearchRequestDTO
import domain.yeasts.services.{YeastRecommendationService, Season, AlternativeReason}
import domain.yeasts.model.YeastId
import application.yeasts.dtos.{YeastRecommendationResponse, RecommendationListResponse}

@Singleton
class YeastPublicController @Inject()(
  cc: ControllerComponents,
  yeastApplicationService: YeastApplicationService,
  yeastRecommendationService: YeastRecommendationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def listActiveYeasts(
    page: Int = 0,
    size: Int = 20, 
    name: Option[String] = None,
    laboratory: Option[String] = None,
    yeastType: Option[String] = None
  ): Action[AnyContent] = Action.async { request =>
    val searchRequest = YeastSearchRequestDTO(
      name = name,
      laboratory = laboratory,
      yeastType = yeastType,
      minAttenuation = None,
      maxAttenuation = None,
      minTemperature = None,
      maxTemperature = None,
      minAlcoholTolerance = None,
      maxAlcoholTolerance = None,
      flocculation = None,
      characteristics = None,
      status = None,
      page = page,
      size = size
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(errors) => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors.mkString(", "))))
    }
  }

  def searchYeasts(q: String, limit: Int = 20): Action[AnyContent] = Action.async { request =>
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

  def getYeastsByType(yeastType: String, limit: Int = 20): Action[AnyContent] = Action.async { request =>
    val searchRequest = YeastSearchRequestDTO(
      name = None,
      laboratory = None,
      yeastType = Some(yeastType),
      minAttenuation = None,
      maxAttenuation = None,
      minTemperature = None,
      maxTemperature = None,
      minAlcoholTolerance = None,
      maxAlcoholTolerance = None,
      flocculation = None,
      characteristics = None,
      status = None,
      page = 0,
      size = limit
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(response) => Ok(Json.toJson(response.yeasts))
      case Left(errors) => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors.mkString(", "))))
    }
  }

  def getYeastsByLaboratory(lab: String, limit: Int = 20): Action[AnyContent] = Action.async { request =>
    val searchRequest = YeastSearchRequestDTO(
      name = None,
      laboratory = Some(lab),
      yeastType = None,
      minAttenuation = None,
      maxAttenuation = None,
      minTemperature = None,
      maxTemperature = None,
      minAlcoholTolerance = None,
      maxAlcoholTolerance = None,
      flocculation = None,
      characteristics = None,
      status = None,
      page = 0,
      size = limit
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(response) => Ok(Json.toJson(response.yeasts))
      case Left(errors) => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors.mkString(", "))))
    }
  }

  def getYeast(yeastId: String): Action[AnyContent] = Action.async { request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(uuid) => 
        yeastApplicationService.getYeastById(uuid).map {
          case Some(yeast) => Ok(Json.toJson(yeast))
          case None => NotFound(Json.obj("error" -> "Yeast not found"))
        }
    }
  }

  def getAlternatives(yeastId: String, reason: String = "unavailable", limit: Int = 5): Action[AnyContent] = Action.async { request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(uuid) => 
        YeastId.fromString(uuid.toString) match {
          case Left(idError) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(idError))))
          case Right(yeastIdVO) =>
            val alternativeReason = reason.toLowerCase match {
              case "unavailable" => AlternativeReason.Unavailable
              case "expensive" => AlternativeReason.TooExpensive
              case "experiment" => AlternativeReason.Experiment
              case _ => AlternativeReason.Unavailable
            }
            
            yeastRecommendationService.findAlternatives(yeastIdVO, alternativeReason, limit).map { recommendations =>
              val response = RecommendationListResponse(
                recommendations = recommendations.map(YeastRecommendationResponse.fromDomainRecommendation),
                totalCount = recommendations.length,
                requestType = "alternatives",
                parameters = Map("originalYeastId" -> yeastId, "reason" -> reason, "limit" -> limit.toString)
              )
              Ok(Json.toJson(response))
            }.recover {
              case ex => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(ex.getMessage)))
            }
        }
    }
  }

  def getBeginnerRecommendations(limit: Int = 5): Action[AnyContent] = Action.async { request =>
    yeastRecommendationService.getBeginnerFriendlyYeasts(limit).map { recommendations =>
      val response = RecommendationListResponse(
        recommendations = recommendations.map(YeastRecommendationResponse.fromDomainRecommendation),
        totalCount = recommendations.length,
        requestType = "beginner",
        parameters = Map("limit" -> limit.toString)
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(ex.getMessage)))
    }
  }

  def getSeasonalRecommendations(season: String = "current", limit: Int = 8): Action[AnyContent] = Action.async { request =>
    val seasonEnum = season.toLowerCase match {
      case "spring" => Season.Spring
      case "summer" => Season.Summer  
      case "autumn" => Season.Autumn
      case "winter" => Season.Winter
      case _ => Season.Summer // Default current season
    }
    
    yeastRecommendationService.getSeasonalRecommendations(seasonEnum, limit).map { recommendations =>
      val response = RecommendationListResponse(
        recommendations = recommendations.map(YeastRecommendationResponse.fromDomainRecommendation),
        totalCount = recommendations.length,
        requestType = "seasonal",
        parameters = Map("season" -> season, "limit" -> limit.toString)
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(ex.getMessage)))
    }
  }

  def getExperimentalRecommendations(limit: Int = 6): Action[AnyContent] = Action.async { request =>
    yeastRecommendationService.getExperimentalYeasts(limit).map { recommendations =>
      val response = RecommendationListResponse(
        recommendations = recommendations.map(YeastRecommendationResponse.fromDomainRecommendation),
        totalCount = recommendations.length,
        requestType = "experimental",
        parameters = Map("limit" -> limit.toString)
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(ex.getMessage)))
    }
  }

  def getRecommendationsForBeerStyle(
    style: String, 
    targetAbv: Option[Double] = None,
    fermentationTemp: Option[Int] = None,
    limit: Int = 10
  ): Action[AnyContent] = Action.async { request =>
    Future.successful(Ok(Json.obj("message" -> "Style recommendations not implemented yet")))
  }

  def getPublicStats(): Action[AnyContent] = Action.async { request =>
    Future.successful(Ok(Json.obj("message" -> "Public stats not implemented yet")))
  }

  def getPopularYeasts(limit: Int = 10): Action[AnyContent] = Action.async { request =>
    val searchRequest = YeastSearchRequestDTO(
      name = None,
      laboratory = None,
      yeastType = None,
      minAttenuation = None,
      maxAttenuation = None,
      minTemperature = None,
      maxTemperature = None,
      minAlcoholTolerance = None,
      maxAlcoholTolerance = None,
      flocculation = None,
      characteristics = None,
      status = None,
      page = 0,
      size = limit
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(response) => Ok(Json.toJson(response.yeasts))
      case Left(errors) => InternalServerError(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors.mkString(", "))))
    }
  }
}
