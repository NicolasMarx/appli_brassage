package interfaces.controllers.yeasts

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos._
import play.api.mvc._
import play.api.libs.json._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import play.api.cache.AsyncCacheApi
import scala.concurrent.duration._

/**
 * Controller public pour les levures (lecture seule)
 * Aucune authentification requise, cache activé
 */
@Singleton
class YeastPublicController @Inject()(
  yeastApplicationService: YeastApplicationService,
  cache: AsyncCacheApi,
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  import YeastDTOs._

  // ==========================================================================
  // OPÉRATIONS DE LECTURE PUBLIQUE
  // ==========================================================================

  /**
   * Liste publique des levures actives avec pagination
   */
  def listActiveYeasts(
    page: Int = 0,
    size: Int = 20,
    name: Option[String] = None,
    laboratory: Option[String] = None,
    yeastType: Option[String] = None
  ): Action[AnyContent] = Action.async { implicit request =>
    
    val cacheKey = s"yeasts:public:$page:$size:${name.getOrElse("")}:${laboratory.getOrElse("")}:${yeastType.getOrElse("")}"
    
    cache.getOrElseUpdate(cacheKey, 5.minutes) {
      val searchRequest = YeastSearchRequestDTO(
        name = name,
        laboratory = laboratory,
        yeastType = yeastType,
        status = List("ACTIVE"), // Seulement les levures actives
        page = page,
        size = math.min(size, 50) // Cap à 50 pour public
      )
      
      yeastApplicationService.findYeasts(searchRequest)
    }.map {
      case Right(result) => Ok(Json.toJson(result))
      case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
    }
  }

  /**
   * Détails d'une levure publique
   */
  def getYeast(yeastId: String): Action[AnyContent] = Action.async { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        
        cache.getOrElseUpdate(s"yeast:public:$yeastId", 10.minutes) {
          yeastApplicationService.getYeastById(id)
        }.map {
          case Some(yeast) if yeast.status == "ACTIVE" => Ok(Json.toJson(yeast))
          case Some(_) => NotFound(Json.obj("error" -> "Levure non disponible"))
          case None => NotFound(Json.obj("error" -> s"Levure non trouvée: $yeastId"))
        }
    }
  }

  /**
   * Recherche textuelle publique
   */
  def searchYeasts(
    q: String,
    limit: Int = 20
  ): Action[AnyContent] = Action.async { implicit request =>
    
    if (q.trim.length < 2) {
      Future.successful(BadRequest(Json.toJson(
        YeastApplicationUtils.buildErrorResponse("Terme de recherche trop court (minimum 2 caractères)")
      )))
    } else {
      val cacheKey = s"yeasts:search:${q.toLowerCase}:$limit"
      
      cache.getOrElseUpdate(cacheKey, 3.minutes) {
        yeastApplicationService.searchYeasts(q, Some(math.min(limit, 50)))
      }.map {
        case Right(yeasts) => Ok(Json.toJson(yeasts))
        case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
      }
    }
  }

  /**
   * Levures par type
   */
  def getYeastsByType(
    yeastType: String,
    limit: Int = 20
  ): Action[AnyContent] = Action.async { implicit request =>
    
    val cacheKey = s"yeasts:type:$yeastType:$limit"
    
    cache.getOrElseUpdate(cacheKey, 10.minutes) {
      yeastApplicationService.findYeastsByType(yeastType, Some(math.min(limit, 50)))
    }.map {
      case Right(yeasts) => Ok(Json.toJson(yeasts))
      case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
    }
  }

  /**
   * Levures par laboratoire
   */
  def getYeastsByLaboratory(
    laboratory: String,
    limit: Int = 20
  ): Action[AnyContent] = Action.async { implicit request =>
    
    val cacheKey = s"yeasts:lab:$laboratory:$limit"
    
    cache.getOrElseUpdate(cacheKey, 10.minutes) {
      yeastApplicationService.findYeastsByLaboratory(laboratory, Some(math.min(limit, 50)))
    }.map {
      case Right(yeasts) => Ok(Json.toJson(yeasts))
      case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
    }
  }

  // ==========================================================================
  // RECOMMANDATIONS PUBLIQUES
  // ==========================================================================

  /**
   * Recommandations pour débutants
   */
  def getBeginnerRecommendations(limit: Int = 5): Action[AnyContent] = Action.async { implicit request =>
    
    cache.getOrElseUpdate(s"yeasts:recommendations:beginner:$limit", 30.minutes) {
      yeastApplicationService.getBeginnerRecommendations(math.min(limit, 10))
    }.map { recommendations =>
      Ok(Json.toJson(recommendations))
    }
  }

  /**
   * Recommandations saisonnières
   */
  def getSeasonalRecommendations(
    season: String = "current",
    limit: Int = 8
  ): Action[AnyContent] = Action.async { implicit request =>
    
    val actualSeason = if (season == "current") getCurrentSeason() else season
    val cacheKey = s"yeasts:recommendations:seasonal:$actualSeason:$limit"
    
    cache.getOrElseUpdate(cacheKey, 60.minutes) {
      yeastApplicationService.getSeasonalRecommendations(actualSeason, math.min(limit, 15))
    }.map { recommendations =>
      Ok(Json.toJson(recommendations))
    }
  }

  /**
   * Recommandations pour expérimentateurs
   */
  def getExperimentalRecommendations(limit: Int = 6): Action[AnyContent] = Action.async { implicit request =>
    
    cache.getOrElseUpdate(s"yeasts:recommendations:experimental:$limit", 45.minutes) {
      yeastApplicationService.getExperimentalRecommendations(math.min(limit, 10))
    }.map { recommendations =>
      Ok(Json.toJson(recommendations))
    }
  }

  /**
   * Recommandations par style de bière
   */
  def getRecommendationsForBeerStyle(
    beerStyle: String,
    targetAbv: Option[Double] = None,
    fermentationTemp: Option[Int] = None,
    limit: Int = 10
  ): Action[AnyContent] = Action.async { implicit request =>
    
    val cacheKey = s"yeasts:recommendations:style:$beerStyle:${targetAbv.getOrElse("")}:${fermentationTemp.getOrElse("")}:$limit"
    
    cache.getOrElseUpdate(cacheKey, 20.minutes) {
      yeastApplicationService.getYeastRecommendations(
        beerStyle = Some(beerStyle),
        targetAbv = targetAbv,
        fermentationTemp = fermentationTemp,
        limit = math.min(limit, 15)
      )
    }.map {
      case Right(recommendations) => Ok(Json.toJson(recommendations))
      case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
    }
  }

  /**
   * Alternatives à une levure
   */
  def getAlternatives(
    yeastId: String,
    reason: String = "unavailable",
    limit: Int = 5
  ): Action[AnyContent] = Action.async { implicit request =>
    
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        
        val cacheKey = s"yeasts:alternatives:$yeastId:$reason:$limit"
        
        cache.getOrElseUpdate(cacheKey, 15.minutes) {
          yeastApplicationService.getYeastAlternatives(id, reason, math.min(limit, 10))
        }.map {
          case Right(alternatives) => Ok(Json.toJson(alternatives))
          case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
        }
    }
  }

  // ==========================================================================
  // STATISTIQUES PUBLIQUES
  // ==========================================================================

  /**
   * Statistiques publiques simplifiées
   */
  def getPublicStats(): Action[AnyContent] = Action.async { implicit request =>
    
    cache.getOrElseUpdate("yeasts:stats:public", 60.minutes) {
      yeastApplicationService.getYeastStatistics().map { stats =>
        Json.obj(
          "totalActiveYeasts" -> stats.byStatus.getOrElse("ACTIVE", 0L),
          "laboratoriesCount" -> stats.byLaboratory.size,
          "typesCount" -> stats.byType.size,
          "byType" -> Json.toJson(stats.byType),
          "byLaboratory" -> Json.toJson(stats.byLaboratory.take(10)) // Top 10
        )
      }
    }.map(Ok(_))
  }

  /**
   * Levures populaires
   */
  def getPopularYeasts(limit: Int = 10): Action[AnyContent] = Action.async { implicit request =>
    
    cache.getOrElseUpdate(s"yeasts:popular:$limit", 120.minutes) {
      // Pour l'instant, retourne les plus récentes
      val searchRequest = YeastSearchRequestDTO(
        status = List("ACTIVE"),
        size = math.min(limit, 20)
      )
      yeastApplicationService.findYeasts(searchRequest)
    }.map {
      case Right(result) => Ok(Json.toJson(result.yeasts))
      case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
    }
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def getCurrentSeason(): String = {
    import java.time.LocalDate
    val now = LocalDate.now()
    val month = now.getMonthValue
    
    month match {
      case 3 | 4 | 5 => "spring"
      case 6 | 7 | 8 => "summer"
      case 9 | 10 | 11 => "autumn"
      case _ => "winter"
    }
  }
}
