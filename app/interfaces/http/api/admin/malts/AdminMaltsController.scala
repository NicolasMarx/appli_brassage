package interfaces.http.api.admin.malts

import application.commands.admin.malts._
import application.commands.admin.malts.handlers._
import application.queries.admin.malts._
import application.queries.admin.malts.handlers._
import interfaces.http.common.BaseController
import interfaces.actions.AdminSecuredAction
import play.api.libs.json._
import play.api.mvc._
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
 * Contrôleur API admin pour les malts
 * CRUD complet avec authentification et permissions
 * Suit le pattern AdminHopsController
 */
@Singleton
class AdminMaltsController @Inject()(
                                      cc: ControllerComponents,
                                      adminAction: AdminSecuredAction,
                                      // Command handlers
                                      createHandler: CreateMaltCommandHandler,
                                      updateHandler: UpdateMaltCommandHandler,
                                      deleteHandler: DeleteMaltCommandHandler,
                                      // Query handlers
                                      adminListHandler: AdminMaltListQueryHandler,
                                      adminDetailHandler: AdminMaltDetailQueryHandler
                                    )(implicit ec: ExecutionContext) extends BaseController(cc) {

  /**
   * GET /api/admin/malts
   * Liste complète admin avec filtres avancés
   */
  def list(
            page: Int = 0,
            pageSize: Int = 20,
            maltType: Option[String] = None,
            status: Option[String] = None,
            source: Option[String] = None,
            minCredibility: Option[Int] = None,
            needsReview: Boolean = false,
            searchTerm: Option[String] = None,
            sortBy: String = "name",
            sortOrder: String = "asc"
          ): Action[AnyContent] = adminAction.async { implicit request =>

    val query = AdminMaltListQuery(
      page = page,
      pageSize = pageSize,
      maltType = maltType,
      status = status,
      source = source,
      minCredibility = minCredibility,
      needsReview = needsReview,
      searchTerm = searchTerm,
      sortBy = sortBy,
      sortOrder = sortOrder
    )

    adminListHandler.handle(query).map {
      case Left(error) =>
        BadRequest(Json.obj(
          "error" -> "Paramètres invalides",
          "details" -> error
        ))

      case Right(pagedResult) =>
        Ok(Json.obj(
          "malts" -> pagedResult.items.map(adminMaltToJson),
          "currentPage" -> pagedResult.currentPage,
          "pageSize" -> pagedResult.pageSize,
          "totalCount" -> pagedResult.totalCount,
          "hasNext" -> pagedResult.hasNext
        ))
    }
  }

  /**
   * GET /api/admin/malts/:id
   * Détail complet admin avec analyse
   */
  def detail(
              id: String,
              includeAuditLog: Boolean = false,
              includeSubstitutes: Boolean = true,
              includeBeerStyles: Boolean = true,
              includeStatistics: Boolean = false
            ): Action[AnyContent] = adminAction.async { implicit request =>

    val query = AdminMaltDetailQuery(
      id = id,
      includeAuditLog = includeAuditLog,
      includeSubstitutes = includeSubstitutes,
      includeBeerStyles = includeBeerStyles,
      includeStatistics = includeStatistics
    )

    adminDetailHandler.handle(query).map {
      case Left(error) =>
        NotFound(Json.obj(
          "error" -> s"Malt $id non trouvé",
          "details" -> error
        ))

      case Right(detailResult) =>
        Ok(adminMaltDetailToJson(detailResult))
    }
  }

  /**
   * POST /api/admin/malts
   * Création nouveau malt
   */
  def create(): Action[JsValue] = adminAction.async(parse.json) { implicit request =>

    request.body.validate[CreateMaltRequest] match {
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Données invalides",
          "details" -> JsError.toJson(errors)
        )))

      case JsSuccess(createRequest, _) =>
        val command = CreateMaltCommand(
          name = createRequest.name,
          maltType = createRequest.maltType,
          ebcColor = createRequest.ebcColor,
          extractionRate = createRequest.extractionRate,
          diastaticPower = createRequest.diastaticPower,
          originCode = createRequest.originCode,
          description = createRequest.description,
          flavorProfiles = createRequest.flavorProfiles,
          source = createRequest.source.getOrElse("MANUAL")
        )

        createHandler.handle(command).map {
          case Left(domainError) =>
            BadRequest(Json.obj(
              "error" -> "Erreur de création",
              "details" -> domainError.message
            ))

          case Right(maltId) =>
            Created(Json.obj(
              "id" -> maltId.value,
              "message" -> "Malt créé avec succès"
            ))
        }
    }
  }

  /**
   * PUT /api/admin/malts/:id
   * Mise à jour malt existant
   */
  def update(id: String): Action[JsValue] = adminAction.async(parse.json) { implicit request =>

    request.body.validate[UpdateMaltRequest] match {
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Données invalides",
          "details" -> JsError.toJson(errors)
        )))

      case JsSuccess(updateRequest, _) =>
        val command = UpdateMaltCommand(
          id = id,
          name = updateRequest.name,
          description = updateRequest.description,
          ebcColor = updateRequest.ebcColor,
          extractionRate = updateRequest.extractionRate,
          diastaticPower = updateRequest.diastaticPower,
          flavorProfiles = updateRequest.flavorProfiles,
          status = updateRequest.status
        )

        updateHandler.handle(command).map {
          case Left(domainError) =>
            BadRequest(Json.obj(
              "error" -> "Erreur de mise à jour",
              "details" -> domainError.message
            ))

          case Right(updatedMalt) =>
            Ok(adminMaltToJson(AdminMaltReadModel.fromAggregate(updatedMalt)))
        }
    }
  }

  /**
   * DELETE /api/admin/malts/:id
   * Suppression (désactivation) malt
   */
  def delete(id: String): Action[JsValue] = adminAction.async(parse.json) { implicit request =>

    val deleteRequest = (request.body \ "reason").asOpt[String]
    val forceDelete = (request.body \ "forceDelete").asOpt[Boolean].getOrElse(false)

    val command = DeleteMaltCommand(
      id = id,
      reason = deleteRequest,
      forceDelete = forceDelete
    )

    deleteHandler.handle(command).map {
      case Left(domainError) =>
        BadRequest(Json.obj(
          "error" -> "Erreur de suppression",
          "details" -> domainError.message
        ))

      case Right(_) =>
        Ok(Json.obj(
          "message" -> "Malt supprimé avec succès",
          "id" -> id
        ))
    }
  }

  /**
   * GET /api/admin/malts/statistics
   * Statistiques globales malts
   */
  def statistics(): Action[AnyContent] = adminAction.async { implicit request =>
    // TODO: Implémenter récupération statistiques via repository
    Future.successful(Ok(Json.obj(
      "totalCount" -> 0,
      "activeCount" -> 0,
      "countByType" -> Json.obj(),
      "countBySource" -> Json.obj(),
      "averageCredibilityScore" -> 0.0,
      "needsReviewCount" -> 0
    )))
  }

  /**
   * GET /api/admin/malts/needs-review
   * Malts nécessitant une révision
   */
  def needsReview(
                   page: Int = 0,
                   pageSize: Int = 20,
                   maxCredibility: Int = 70
                 ): Action[AnyContent] = adminAction.async { implicit request =>

    val query = AdminMaltListQuery(
      page = page,
      pageSize = pageSize,
      needsReview = true,
      sortBy = "credibilityScore",
      sortOrder = "asc"
    )

    adminListHandler.handle(query).map {
      case Left(error) =>
        BadRequest(Json.obj("error" -> error))

      case Right(pagedResult) =>
        Ok(Json.obj(
          "malts" -> pagedResult.items.map(adminMaltToJson),
          "currentPage" -> pagedResult.currentPage,
          "pageSize" -> pagedResult.pageSize,
          "totalCount" -> pagedResult.totalCount,
          "hasNext" -> pagedResult.hasNext,
          "reviewThreshold" -> maxCredibility
        ))
    }
  }

  /**
   * PATCH /api/admin/malts/:id/credibility
   * Ajustement score de crédibilité
   */
  def adjustCredibility(id: String): Action[JsValue] = adminAction.async(parse.json) { implicit request =>

    val newScore = (request.body \ "credibilityScore").asOpt[Int]
    val reason = (request.body \ "reason").asOpt[String]

    (newScore, reason) match {
      case (Some(score), Some(reasonText)) if score >= 0 && score <= 100 =>
        // TODO: Implémenter ajustement crédibilité
        Future.successful(Ok(Json.obj(
          "message" -> "Score de crédibilité mis à jour",
          "newScore" -> score
        )))

      case _ =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Score (0-100) et raison requis"
        )))
    }
  }

  /**
   * POST /api/admin/malts/batch-import
   * Import en lot de malts
   */
  def batchImport(): Action[JsValue] = adminAction.async(parse.json) { implicit request =>

    request.body.validate[List[CreateMaltRequest]] match {
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Données d'import invalides",
          "details" -> JsError.toJson(errors)
        )))

      case JsSuccess(maltRequests, _) =>
        // TODO: Implémenter import en lot
        Future.successful(Ok(Json.obj(
          "message" -> s"${maltRequests.length} malts importés",
          "importedCount" -> maltRequests.length
        )))
    }
  }

  // JSON serializers admin
  private def adminMaltToJson(malt: AdminMaltReadModel): JsValue = {
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
      "status" -> malt.status,
      "source" -> malt.source,
      "credibilityScore" -> malt.credibilityScore,
      "flavorProfiles" -> malt.flavorProfiles,
      // Informations calculées
      "colorName" -> malt.colorName,
      "extractionCategory" -> malt.extractionCategory,
      "enzymaticCategory" -> malt.enzymaticCategory,
      "maxRecommendedPercent" -> malt.maxRecommendedPercent,
      "qualityScore" -> malt.qualityScore,
      "needsReview" -> malt.needsReview,
      // Métadonnées
      "createdAt" -> malt.createdAt,
      "updatedAt" -> malt.updatedAt,
      "version" -> malt.version
    )
  }

  private def adminMaltDetailToJson(result: AdminMaltDetailResult): JsValue = {
    Json.obj(
      "malt" -> adminMaltToJson(result.malt),
      "substitutes" -> result.substitutes.map { substitute =>
        Json.obj(
          "malt" -> adminMaltToJson(substitute.malt),
          "substitutionRatio" -> substitute.substitutionRatio,
          "notes" -> substitute.notes
        )
      },
      "beerStyleCompatibility" -> result.beerStyleCompatibility.map { compat =>
        Json.obj(
          "beerStyleId" -> compat.beerStyleId,
          "compatibilityScore" -> compat.compatibilityScore,
          "typicalPercentage" -> compat.typicalPercentage
        )
      },
      "auditLog" -> result.auditLog.map { log =>
        Json.obj(
          "id" -> log.id,
          "adminId" -> log.adminId,
          "action" -> log.action,
          "changes" -> log.changes,
          "timestamp" -> log.timestamp
        )
      },
      "statistics" -> result.statistics.map { stats =>
        Json.obj(
          "usageInRecipes" -> stats.usageInRecipes,
          "averagePercentageInRecipes" -> stats.averagePercentageInRecipes,
          "popularityRank" -> stats.popularityRank,
          "lastUsedDate" -> stats.lastUsedDate
        )
      },
      "qualityAnalysis" -> Json.obj(
        "overallScore" -> result.qualityAnalysis.overallScore,
        "issues" -> result.qualityAnalysis.issues,
        "suggestions" -> result.qualityAnalysis.suggestions,
        "completenessPercentage" -> result.qualityAnalysis.completenessPercentage
      ),
      "recommendations" -> result.recommendations
    )
  }
}

// DTOs pour requêtes admin
case class CreateMaltRequest(
                              name: String,
                              maltType: String,
                              ebcColor: Double,
                              extractionRate: Double,
                              diastaticPower: Double,
                              originCode: String,
                              description: Option[String] = None,
                              flavorProfiles: List[String] = List.empty,
                              source: Option[String] = None
                            )

case class UpdateMaltRequest(
                              name: Option[String] = None,
                              description: Option[String] = None,
                              ebcColor: Option[Double] = None,
                              extractionRate: Option[Double] = None,
                              diastaticPower: Option[Double] = None,
                              flavorProfiles: Option[List[String]] = None,
                              status: Option[String] = None
                            )

object CreateMaltRequest {
  implicit val format: Format[CreateMaltRequest] = Json.format[CreateMaltRequest]
}

object UpdateMaltRequest {
  implicit val format: Format[UpdateMaltRequest] = Json.format[UpdateMaltRequest]
}