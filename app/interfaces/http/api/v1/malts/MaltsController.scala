package interfaces.http.api.v1.malts

import application.queries.public.malts._
import application.queries.public.malts.handlers._
import domain.malts.model.MaltType
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MaltsController @Inject()(
    cc: ControllerComponents,
    maltListQueryHandler: MaltListQueryHandler,
    maltDetailQueryHandler: MaltDetailQueryHandler,
    maltSearchQueryHandler: MaltSearchQueryHandler
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def list(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts - page: $page, size: $size")
    
    val query = MaltListQuery(page = page, size = size)
    
    maltListQueryHandler.handle(query).map { result =>
      result match {
        case Success(response) =>
          println(s"✅ API publique - Malts récupérés: ${response.malts.length}/${response.totalCount}")
          Ok(Json.toJson(response))
        case Failure(ex) =>
          println(s"❌ Erreur API publique: ${ex.getMessage}")
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite"
          ))
      }
    }.recover {
      case ex =>
        println(s"❌ Erreur critique: ${ex.getMessage}")
        InternalServerError(Json.obj("error" -> "critical_error"))
    }
  }

  def detail(id: String): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts/$id")
    
    val query = MaltDetailQuery(maltId = id)
    
    maltDetailQueryHandler.handle(query).map { result =>
      result match {
        case Success(Some(malt)) =>
          println(s"✅ API publique - Malt trouvé: ${malt.name}")
          Ok(Json.toJson(malt))
        case Success(None) =>
          NotFound(Json.obj("error" -> "malt_not_found"))
        case Failure(ex) =>
          println(s"❌ Erreur API publique: ${ex.getMessage}")
          InternalServerError(Json.obj("error" -> "internal_error"))
      }
    }.recover {
      case ex =>
        BadRequest(Json.obj("error" -> "invalid_id"))
    }
  }

  def search(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    println(s"🌾 POST /api/v1/malts/search")
    
    request.body.validate[MaltSearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        val query = MaltSearchQuery(
          name = searchRequest.name,
          maltType = searchRequest.maltType.flatMap(MaltType.fromName),
          minEbc = searchRequest.minEbc,
          maxEbc = searchRequest.maxEbc,
          minExtraction = searchRequest.minExtraction,
          maxExtraction = searchRequest.maxExtraction,
          page = searchRequest.page.getOrElse(0),
          size = searchRequest.size.getOrElse(20)
        )

        maltSearchQueryHandler.handle(query).map { result =>
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

  def byType(maltType: String, page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts/type/$maltType")
    
    MaltType.fromName(maltType) match {
      case Some(validType) =>
        val query = MaltListQuery(
          maltType = Some(validType),
          page = page,
          size = size
        )
        
        maltListQueryHandler.handle(query).map { result =>
          result match {
            case Success(response) =>
              Ok(Json.toJson(response))
            case Failure(ex) =>
              InternalServerError(Json.obj("error" -> "internal_error"))
          }
        }
        
      case None =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "invalid_type",
          "message" -> s"Type de malt '$maltType' invalide",
          "validTypes" -> List("BASE", "SPECIALTY", "CRYSTAL", "CARAMEL", "ROASTED") // Liste hardcodée
        )))
    }
  }
}

case class MaltSearchRequest(
  name: Option[String] = None,
  maltType: Option[String] = None,
  minEbc: Option[Double] = None,
  maxEbc: Option[Double] = None,
  minExtraction: Option[Double] = None,
  maxExtraction: Option[Double] = None,
  page: Option[Int] = None,
  size: Option[Int] = None
)

object MaltSearchRequest {
  implicit val format: Format[MaltSearchRequest] = Json.format[MaltSearchRequest]
}
