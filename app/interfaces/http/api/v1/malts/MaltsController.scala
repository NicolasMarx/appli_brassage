package interfaces.http.api.v1.malts

import application.queries.public.malts._
import application.queries.public.malts.handlers._
import application.queries.public.malts.readmodels._
import interfaces.http.common.BaseController
import play.api.libs.json._
import play.api.mvc._
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
 * Contrôleur API publique pour les malts
 * Endpoints lecture seule, sans authentification requise
 * Suit le pattern HopsController pour cohérence
 */
@Singleton
class MaltsController @Inject()(
                                 cc: ControllerComponents,
                                 maltListHandler: MaltListQueryHandler,
                                 maltDetailHandler: MaltDetailQueryHandler,
                                 maltSearchHandler: MaltSearchQueryHandler
                               )(implicit ec: ExecutionContext) extends BaseController(cc) {

  /**
   * GET /api/v1/malts
   * Liste paginée des malts avec filtres optionnels
   */
  def list(
            page: Int = 0,
            pageSize: Int = 20,
            maltType: Option[String] = None,
            minEBC: Option[Double] = None,
            maxEBC: Option[Double] = None,
            originCode: Option[String] = None,
            activeOnly: Boolean = true
          ): Action[AnyContent] = Action.async { implicit request =>

    val query = MaltListQuery(
      page = page,
      pageSize = pageSize,
      maltType = maltType,
      minEBC = minEBC,
      maxEBC = maxEBC,
      originCode = originCode,
      activeOnly = activeOnly
    )

    maltListHandler.handle(query).map {
      case Left(error) =>
        BadRequest(Json.obj(
          "error" -> "Paramètres invalides",
          "details" -> error
        ))

      case Right(pagedResult) =>
        Ok(Json.obj(
          "items" -> pagedResult.items.map(maltToJson),
          "currentPage" -> pagedResult.currentPage,
          "pageSize" -> pagedResult.pageSize,
          "totalCount" -> pagedResult.totalCount,
          "hasNext" -> pagedResult.hasNext
        ))
    }
  }

  /**
   * GET /api/v1/malts/:id
   * Détail d'un malt spécifique
   */
  def detail(
              id: String,
              includeSubstitutes: Boolean = false,
              includeBeerStyles: Boolean = false
            ): Action[AnyContent] = Action.async { implicit request =>

    val query = MaltDetailQuery(
      id = id,
      includeSubstitutes = includeSubstitutes,
      includeBeerStyles = includeBeerStyles
    )

    maltDetailHandler.handle(query).map {
      case Left(error) =>
        NotFound(Json.obj(
          "error" -> s"Malt $id non trouvé",
          "details" -> error
        ))

      case Right(detailResult) =>
        Ok(maltDetailToJson(detailResult))
    }
  }

  /**
   * POST /api/v1/malts/search
   * Recherche avancée de malts
   */
  def search(): Action[JsValue] = Action.async(parse.json) { implicit request =>

    request.body.validate[MaltSearchRequest] match {
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Données de recherche invalides",
          "details" -> JsError.toJson(errors)
        )))

      case JsSuccess(searchRequest, _) =>
        val query = MaltSearchQuery(
          searchTerm = searchRequest.searchTerm,
          page = searchRequest.page,
          pageSize = searchRequest.pageSize,
          maltType = searchRequest.maltType,
          minEBC = searchRequest.minEBC,
          maxEBC = searchRequest.maxEBC,
          minExtraction = searchRequest.minExtraction,
          minDiastaticPower = searchRequest.minDiastaticPower,
          flavorProfiles = searchRequest.flavorProfiles,
          originCode = searchRequest.originCode,
          activeOnly = searchRequest.activeOnly
        )

        maltSearchHandler.handle(query).map {
          case Left(error) =>
            BadRequest(Json.obj(
              "error" -> "Recherche invalide",
              "details" -> error
            ))

          case Right(pagedResult) =>
            Ok(Json.obj(
              "items" -> pagedResult.items.map(maltToJson),
              "currentPage" -> pagedResult.currentPage,
              "pageSize" -> pagedResult.pageSize,
              "totalCount" -> pagedResult.totalCount,
              "hasNext" -> pagedResult.hasNext,
              "searchTerm" -> searchRequest.searchTerm
            ))
        }
    }
  }

  /**
   * GET /api/v1/malts/types
   * Liste des types de malts disponibles
   */
  def types(): Action[AnyContent] = Action { implicit request =>
    val maltTypes = List(
      Json.obj(
        "name" -> "BASE",
        "description" -> "Malts de base - foundation de la recette",
        "typicalUsagePercent" -> Json.obj("min" -> 50.0, "max" -> 100.0),
        "typicalEBCRange" -> Json.obj("min" -> 2.0, "max" -> 25.0)
      ),
      Json.obj(
        "name" -> "CRYSTAL",
        "description" -> "Malts crystal/caramel - sucres caramélisés",
        "typicalUsagePercent" -> Json.obj("min" -> 1.0, "max" -> 20.0),
        "typicalEBCRange" -> Json.obj("min" -> 40.0, "max" -> 300.0)
      ),
      Json.obj(
        "name" -> "ROASTED",
        "description" -> "Malts torréfiés - saveurs grillées/café",
        "typicalUsagePercent" -> Json.obj("min" -> 0.5, "max" -> 10.0),
        "typicalEBCRange" -> Json.obj("min" -> 500.0, "max" -> 1500.0)
      ),
      Json.obj(
        "name" -> "SPECIALTY",
        "description" -> "Malts spéciaux - arômes/textures spécifiques",
        "typicalUsagePercent" -> Json.obj("min" -> 1.0, "max" -> 40.0),
        "typicalEBCRange" -> Json.obj("min" -> 3.0, "max" -> 80.0)
      ),
      Json.obj(
        "name" -> "ADJUNCT",
        "description" -> "Adjuvants - riz, maïs, avoine non maltée",
        "typicalUsagePercent" -> Json.obj("min" -> 5.0, "max" -> 40.0),
        "typicalEBCRange" -> Json.obj("min" -> 0.0, "max" -> 10.0)
      )
    )

    Ok(Json.obj("maltTypes" -> maltTypes))
  }

  /**
   * GET /api/v1/malts/colors
   * Guide des couleurs EBC avec exemples
   */
  def colors(): Action[AnyContent] = Action { implicit request =>
    val colorRanges = List(
      Json.obj(
        "name" -> "Très Pâle",
        "ebcRange" -> Json.obj("min" -> 0, "max" -> 8),
        "description" -> "Pilsner, Wheat",
        "examples" -> List("Pilsner Malt", "Wheat Malt")
      ),
      Json.obj(
        "name" -> "Pâle",
        "ebcRange" -> Json.obj("min" -> 8, "max" -> 16),
        "description" -> "Pale Ale, Munich Light",
        "examples" -> List("Pale Ale Malt", "Munich Light")
      ),
      Json.obj(
        "name" -> "Ambré",
        "ebcRange" -> Json.obj("min" -> 16, "max" -> 33),
        "description" -> "Vienna, Munich Dark",
        "examples" -> List("Vienna Malt", "Munich Dark")
      ),
      Json.obj(
        "name" -> "Cuivré",
        "ebcRange" -> Json.obj("min" -> 33, "max" -> 66),
        "description" -> "Crystal/Caramel malts",
        "examples" -> List("Crystal 40", "Crystal 60")
      ),
      Json.obj(
        "name" -> "Brun",
        "ebcRange" -> Json.obj("min" -> 66, "max" -> 130),
        "description" -> "Chocolate, Brown malts",
        "examples" -> List("Chocolate Malt", "Brown Malt")
      ),
      Json.obj(
        "name" -> "Brun Foncé",
        "ebcRange" -> Json.obj("min" -> 130, "max" -> 300),
        "description" -> "Roasted malts",
        "examples" -> List("Roasted Barley", "Black Malt")
      ),
      Json.obj(
        "name" -> "Noir",
        "ebcRange" -> Json.obj("min" -> 300, "max" -> 1500),
        "description" -> "Black malts, Roasted Barley",
        "examples" -> List("Patent Black", "Carafa Special III")
      )
    )

    Ok(Json.obj("colorRanges" -> colorRanges))
  }

  // JSON serializers
  private def maltToJson(malt: MaltReadModel): JsValue = {
    Json.obj(
      "id" -> malt.id,
      "name" -> malt.name,
      "maltType" -> malt.maltType,
      "ebcColor" -> malt.ebcColor,
      "extractionRate" -> malt.extractionRate,
      "diastaticPower" -> malt.diastaticPower,
      "origin" -> Json.obj(
        "code" -> malt.origin.code,
        "name" -> malt.origin.name,
        "region" -> malt.origin.region,
        "isNoble" -> malt.origin.isNoble,
        "isNewWorld" -> malt.origin.isNewWorld
      ),
      "description" -> malt.description,
      "flavorProfiles" -> malt.flavorProfiles,
      "colorName" -> malt.colorName,
      "extractionCategory" -> malt.extractionCategory,
      "enzymaticCategory" -> malt.enzymaticCategory,
      "maxRecommendedPercent" -> malt.maxRecommendedPercent,
      "isBaseMalt" -> malt.isBaseMalt,
      "canSelfConvert" -> malt.canSelfConvert,
      "createdAt" -> malt.createdAt,
      "updatedAt" -> malt.updatedAt
    )
  }

  private def maltDetailToJson(result: MaltDetailResult): JsValue = {
    Json.obj(
      "malt" -> maltToJson(result.malt),
      "substitutes" -> result.substitutes.map { substitute =>
        Json.obj(
          "malt" -> maltToJson(substitute.malt),
          "substitutionRatio" -> substitute.substitutionRatio,
          "notes" -> substitute.notes
        )
      },
      "compatibleBeerStyles" -> result.compatibleBeerStyles,
      "qualityScore" -> result.qualityScore
    )
  }
}

// DTO pour requête de recherche
case class MaltSearchRequest(
                              searchTerm: String,
                              page: Int = 0,
                              pageSize: Int = 20,
                              maltType: Option[String] = None,
                              minEBC: Option[Double] = None,
                              maxEBC: Option[Double] = None,
                              minExtraction: Option[Double] = None,
                              minDiastaticPower: Option[Double] = None,
                              flavorProfiles: List[String] = List.empty,
                              originCode: Option[String] = None,
                              activeOnly: Boolean = true
                            )

object MaltSearchRequest {
  implicit val format: Format[MaltSearchRequest] = Json.format[MaltSearchRequest]
}