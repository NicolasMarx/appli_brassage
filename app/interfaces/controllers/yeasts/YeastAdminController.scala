package interfaces.controllers.yeasts

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos._
import play.api.mvc._
import play.api.libs.json._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import actions.AdminAction
import domain.admin.model.{AdminId, Permission}

/**
 * Controller pour les opérations admin sur les levures
 * Authentification et permissions requises
 */
@Singleton
class YeastAdminController @Inject()(
  yeastApplicationService: YeastApplicationService,
  adminAction: AdminAction,
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  import YeastDTOs._

  // ==========================================================================
  // OPÉRATIONS CRUD ADMIN
  // ==========================================================================

  /**
   * Liste des levures pour admin avec filtres avancés
   */
  def listYeasts(
    page: Int = 0,
    size: Int = 20,
    name: Option[String] = None,
    laboratory: Option[String] = None,
    yeastType: Option[String] = None,
    status: Option[String] = None
  ): Action[AnyContent] = adminAction.async(Permission.MANAGE_INGREDIENTS) { implicit request =>
    
    val searchRequest = YeastSearchRequestDTO(
      name = name,
      laboratory = laboratory,
      yeastType = yeastType,
      status = status.map(List(_)).getOrElse(List.empty),
      page = page,
      size = math.min(size, 100) // Cap à 100
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(result) => Ok(Json.toJson(result))
      case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
    }
  }

  /**
   * Détails d'une levure pour admin
   */
  def getYeast(yeastId: String): Action[AnyContent] = adminAction.async(Permission.MANAGE_INGREDIENTS) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        yeastApplicationService.getYeastById(id).map {
          case Some(yeast) => Ok(Json.toJson(yeast))
          case None => NotFound(Json.obj("error" -> s"Levure non trouvée: $yeastId"))
        }
    }
  }

  /**
   * Création d'une nouvelle levure
   */
  def createYeast(): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    request.body.validate[CreateYeastRequestDTO] match {
      case JsSuccess(createRequest, _) =>
        val createdBy = request.admin.id.value
        
        yeastApplicationService.createYeast(createRequest, createdBy).map {
          case Right(yeast) => Created(Json.toJson(yeast))
          case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
        }
        
      case JsError(errors) => 
        val errorMessages = errors.flatMap(_._2).map(_.message).toList
        Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errorMessages))))
    }
  }

  /**
   * Mise à jour d'une levure existante
   */
  def updateYeast(yeastId: String): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        request.body.validate[UpdateYeastRequestDTO] match {
          case JsSuccess(updateRequest, _) =>
            val updatedBy = request.admin.id.value
            
            yeastApplicationService.updateYeast(id, updateRequest, updatedBy).map {
              case Right(yeast) => Ok(Json.toJson(yeast))
              case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
            }
            
          case JsError(errors) => 
            val errorMessages = errors.flatMap(_._2).map(_.message).toList
            Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errorMessages))))
        }
    }
  }

  /**
   * Changement de statut d'une levure
   */
  def changeStatus(yeastId: String): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        request.body.validate[ChangeStatusRequestDTO] match {
          case JsSuccess(statusRequest, _) =>
            val changedBy = request.admin.id.value
            
            yeastApplicationService.changeYeastStatus(id, statusRequest, changedBy).map {
              case Right(_) => Ok(Json.obj("message" -> "Statut modifié avec succès"))
              case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
            }
            
          case JsError(errors) => 
            val errorMessages = errors.flatMap(_._2).map(_.message).toList
            Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errorMessages))))
        }
    }
  }

  /**
   * Activation d'une levure
   */
  def activateYeast(yeastId: String): Action[AnyContent] = adminAction.async(Permission.MANAGE_INGREDIENTS) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        val activatedBy = request.admin.id.value
        
        yeastApplicationService.activateYeast(id, activatedBy).map {
          case Right(yeast) => Ok(Json.toJson(yeast))
          case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
        }
    }
  }

  /**
   * Désactivation d'une levure
   */
  def deactivateYeast(yeastId: String): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        val reason = (request.body \ "reason").asOpt[String]
        val deactivatedBy = request.admin.id.value
        
        yeastApplicationService.deactivateYeast(id, reason, deactivatedBy).map {
          case Right(yeast) => Ok(Json.toJson(yeast))
          case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
        }
    }
  }

  /**
   * Archivage d'une levure
   */
  def archiveYeast(yeastId: String): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        val reason = (request.body \ "reason").asOpt[String]
        val archivedBy = request.admin.id.value
        
        yeastApplicationService.archiveYeast(id, reason, archivedBy).map {
          case Right(yeast) => Ok(Json.toJson(yeast))
          case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
        }
    }
  }

  /**
   * Suppression d'une levure (soft delete vers archive)
   */
  def deleteYeast(yeastId: String): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    YeastApplicationUtils.parseUUID(yeastId) match {
      case Left(error) => Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error))))
      case Right(id) =>
        val reason = (request.body \ "reason").asOpt[String].getOrElse("Suppression demandée")
        val deletedBy = request.admin.id.value
        
        yeastApplicationService.deleteYeast(id, reason, deletedBy).map {
          case Right(_) => Ok(Json.obj("message" -> "Levure supprimée avec succès"))
          case Left(error) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(error)))
        }
    }
  }

  // ==========================================================================
  // OPÉRATIONS BATCH ET UTILITAIRES ADMIN
  // ==========================================================================

  /**
   * Import batch de levures
   */
  def batchCreate(): Action[JsValue] = adminAction.async(Permission.MANAGE_INGREDIENTS, parse.json) { implicit request =>
    request.body.validate[List[CreateYeastRequestDTO]] match {
      case JsSuccess(requests, _) =>
        if (requests.size > 50) {
          Future.successful(BadRequest(Json.toJson(
            YeastApplicationUtils.buildErrorResponse("Maximum 50 levures par batch")
          )))
        } else {
          val createdBy = request.admin.id.value
          
          yeastApplicationService.createYeastsBatch(requests, createdBy).map {
            case Right(yeasts) => 
              Ok(Json.obj(
                "message" -> s"${yeasts.size} levures créées avec succès",
                "yeasts" -> Json.toJson(yeasts)
              ))
            case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
          }
        }
        
      case JsError(errors) => 
        val errorMessages = errors.flatMap(_._2).map(_.message).toList
        Future.successful(BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errorMessages))))
    }
  }

  /**
   * Statistiques complètes pour admin
   */
  def getStatistics(): Action[AnyContent] = adminAction.async(Permission.VIEW_ANALYTICS) { implicit request =>
    yeastApplicationService.getYeastStatistics().map { stats =>
      Ok(Json.toJson(stats))
    }
  }

  /**
   * Export des levures (format JSON)
   */
  def exportYeasts(
    format: String = "json",
    status: Option[String] = None
  ): Action[AnyContent] = adminAction.async(Permission.MANAGE_INGREDIENTS) { implicit request =>
    
    val searchRequest = YeastSearchRequestDTO(
      status = status.map(List(_)).getOrElse(List("ACTIVE", "INACTIVE")),
      size = 1000 // Export large
    )
    
    yeastApplicationService.findYeasts(searchRequest).map {
      case Right(result) =>
        format.toLowerCase match {
          case "json" => 
            Ok(Json.toJson(result.yeasts))
              .withHeaders("Content-Disposition" -> "attachment; filename=yeasts-export.json")
          case "csv" =>
            val csvContent = generateCSVExport(result.yeasts)
            Ok(csvContent)
              .withHeaders(
                "Content-Type" -> "text/csv",
                "Content-Disposition" -> "attachment; filename=yeasts-export.csv"
              )
          case _ => 
            BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse("Format non supporté (json, csv)")))
        }
      case Left(errors) => BadRequest(Json.toJson(YeastApplicationUtils.buildErrorResponse(errors)))
    }
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def generateCSVExport(yeasts: List[YeastSummaryDTO]): String = {
    val header = "ID,Name,Laboratory,Strain,Type,Attenuation,Temperature,Alcohol_Tolerance,Flocculation,Status"
    val rows = yeasts.map { yeast =>
      s"${yeast.id},${yeast.name},${yeast.laboratory},${yeast.strain},${yeast.yeastType}," +
      s"${yeast.attenuationRange},${yeast.temperatureRange},${yeast.alcoholTolerance}," +
      s"${yeast.flocculation},${yeast.status}"
    }
    (header :: rows).mkString("\n")
  }
}
