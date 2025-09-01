package interfaces.http.api.v1.hops

import application.queries.public.hops._
import application.queries.public.hops.handlers._
import domain.hops.model.{HopUsage}
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class HopsController @Inject()(
    cc: ControllerComponents,
    hopListQueryHandler: HopListQueryHandler,
    hopDetailQueryHandler: HopDetailQueryHandler,
    hopSearchQueryHandler: HopSearchQueryHandler
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def list(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🍺 GET /api/v1/hops - page: $page, size: $size")
    
    val query = HopListQuery(page = page, size = size)
    
    hopListQueryHandler.handle(query).map { result =>
      result match {
        case Success(response) =>
          println(s"✅ API publique Hops - Hops récupérés: ${response.hops.length}/${response.totalCount}")
          Ok(Json.toJson(response))
        case Failure(ex) =>
          println(s"❌ Erreur API publique Hops: ${ex.getMessage}")
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite"
          ))
      }
    }.recover {
      case ex =>
        println(s"❌ Erreur critique Hops: ${ex.getMessage}")
        InternalServerError(Json.obj("error" -> "critical_error"))
    }
  }

  def detail(id: String): Action[AnyContent] = Action.async { implicit request =>
    println(s"🍺 GET /api/v1/hops/$id")
    
    val query = HopDetailQuery(hopId = id)
    
    hopDetailQueryHandler.handle(query).map { result =>
      result match {
        case Success(Some(hop)) =>
          println(s"✅ API publique Hops - Hop trouvé: ${hop.name}")
          Ok(Json.toJson(hop))
        case Success(None) =>
          NotFound(Json.obj("error" -> "hop_not_found"))
        case Failure(ex) =>
          println(s"❌ Erreur API publique Hops: ${ex.getMessage}")
          InternalServerError(Json.obj("error" -> "internal_error"))
      }
    }.recover {
      case ex =>
        BadRequest(Json.obj("error" -> "invalid_id"))
    }
  }

  def search(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    println(s"🍺 POST /api/v1/hops/search")
    
    request.body.validate[HopSearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        // Conversion String vers HopUsage
        val usage = searchRequest.usage.flatMap { usageStr =>
          usageStr match {
            case "BITTERING" => Some(HopUsage.Bittering)
            case "AROMA" => Some(HopUsage.Aroma)
            case "DUAL_PURPOSE" => Some(HopUsage.DualPurpose)
            case "NOBLE_HOP" => Some(HopUsage.NobleHop)
            case _ => None
          }
        }
        
        val query = HopSearchQuery(
          name = searchRequest.name,
          usage = usage,
          minAlphaAcid = searchRequest.minAlphaAcid,
          maxAlphaAcid = searchRequest.maxAlphaAcid,
          originCode = searchRequest.originCode,
          page = searchRequest.page.getOrElse(0),
          size = searchRequest.size.getOrElse(20)
        )

        hopSearchQueryHandler.handle(query).map { result =>
          result match {
            case Success(response) =>
              Ok(Json.toJson(response))
            case Failure(ex) =>
              InternalServerError(Json.obj("error" -> "search_error"))
          }
        }

      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "validation_error",
          "details" -> JsError.toJson(errors)
        )))
    }
  }
}

case class HopSearchRequest(
  name: Option[String] = None,
  usage: Option[String] = None,
  minAlphaAcid: Option[Double] = None,
  maxAlphaAcid: Option[Double] = None,
  originCode: Option[String] = None,
  page: Option[Int] = None,
  size: Option[Int] = None
)

object HopSearchRequest {
  implicit val format: Format[HopSearchRequest] = Json.format[HopSearchRequest]
}
