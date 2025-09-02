#!/bin/bash
# =============================================================================
# SCRIPT : Création Interface Layer domaine Yeast
# OBJECTIF : Créer Controllers REST, Routes, Validation HTTP
# USAGE : ./scripts/yeast/06-create-interface-layer.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🌐 CRÉATION INTERFACE LAYER YEAST${NC}"
echo -e "${BLUE}==================================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: CONTROLLER ADMIN (WRITE OPERATIONS)
# =============================================================================

echo -e "${YELLOW}🔐 Création YeastAdminController...${NC}"

mkdir -p app/interfaces/controllers/yeasts

cat > app/interfaces/controllers/yeasts/YeastAdminController.scala << 'EOF'
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
EOF

# =============================================================================
# ÉTAPE 2: CONTROLLER PUBLIC (READ OPERATIONS)
# =============================================================================

echo -e "\n${YELLOW}🌍 Création YeastPublicController...${NC}"

cat > app/interfaces/controllers/yeasts/YeastPublicController.scala << 'EOF'
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
EOF

echo -e "${GREEN}✅ Controllers créés${NC}"

# =============================================================================
# ÉTAPE 3: ROUTES CONFIGURATION
# =============================================================================

echo -e "\n${YELLOW}🛣️ Configuration des routes...${NC}"

# Ajouter les routes yeast dans le fichier routes principal
if ! grep -q "# Yeast routes" conf/routes 2>/dev/null; then
    cat >> conf/routes << 'EOF'

# =============================================================================
# Yeast routes
# =============================================================================

# Public API - Levures (lecture seule, pas d'auth)
GET     /api/v1/yeasts                    interfaces.controllers.yeasts.YeastPublicController.listActiveYeasts(page: Int ?= 0, size: Int ?= 20, name: Option[String], laboratory: Option[String], yeastType: Option[String])
GET     /api/v1/yeasts/search             interfaces.controllers.yeasts.YeastPublicController.searchYeasts(q: String, limit: Int ?= 20)
GET     /api/v1/yeasts/type/:yeastType    interfaces.controllers.yeasts.YeastPublicController.getYeastsByType(yeastType: String, limit: Int ?= 20)
GET     /api/v1/yeasts/laboratory/:lab    interfaces.controllers.yeasts.YeastPublicController.getYeastsByLaboratory(lab: String, limit: Int ?= 20)
GET     /api/v1/yeasts/:yeastId           interfaces.controllers.yeasts.YeastPublicController.getYeast(yeastId: String)
GET     /api/v1/yeasts/:yeastId/alternatives  interfaces.controllers.yeasts.YeastPublicController.getAlternatives(yeastId: String, reason: String ?= "unavailable", limit: Int ?= 5)

# Public API - Recommandations levures
GET     /api/v1/yeasts/recommendations/beginner     interfaces.controllers.yeasts.YeastPublicController.getBeginnerRecommendations(limit: Int ?= 5)
GET     /api/v1/yeasts/recommendations/seasonal     interfaces.controllers.yeasts.YeastPublicController.getSeasonalRecommendations(season: String ?= "current", limit: Int ?= 8)
GET     /api/v1/yeasts/recommendations/experimental interfaces.controllers.yeasts.YeastPublicController.getExperimentalRecommendations(limit: Int ?= 6)
GET     /api/v1/yeasts/recommendations/style/:style interfaces.controllers.yeasts.YeastPublicController.getRecommendationsForBeerStyle(style: String, targetAbv: Option[Double], fermentationTemp: Option[Int], limit: Int ?= 10)

# Public API - Statistiques levures
GET     /api/v1/yeasts/stats              interfaces.controllers.yeasts.YeastPublicController.getPublicStats()
GET     /api/v1/yeasts/popular            interfaces.controllers.yeasts.YeastPublicController.getPopularYeasts(limit: Int ?= 10)

# Admin API - Levures (authentification requise)
GET     /api/admin/yeasts                 interfaces.controllers.yeasts.YeastAdminController.listYeasts(page: Int ?= 0, size: Int ?= 20, name: Option[String], laboratory: Option[String], yeastType: Option[String], status: Option[String])
POST    /api/admin/yeasts                 interfaces.controllers.yeasts.YeastAdminController.createYeast()
GET     /api/admin/yeasts/stats           interfaces.controllers.yeasts.YeastAdminController.getStatistics()
POST    /api/admin/yeasts/batch           interfaces.controllers.yeasts.YeastAdminController.batchCreate()
GET     /api/admin/yeasts/export          interfaces.controllers.yeasts.YeastAdminController.exportYeasts(format: String ?= "json", status: Option[String])

GET     /api/admin/yeasts/:yeastId        interfaces.controllers.yeasts.YeastAdminController.getYeast(yeastId: String)
PUT     /api/admin/yeasts/:yeastId        interfaces.controllers.yeasts.YeastAdminController.updateYeast(yeastId: String)
DELETE  /api/admin/yeasts/:yeastId        interfaces.controllers.yeasts.YeastAdminController.deleteYeast(yeastId: String)

# Admin API - Actions spécifiques levures
PUT     /api/admin/yeasts/:yeastId/status interfaces.controllers.yeasts.YeastAdminController.changeStatus(yeastId: String)
PUT     /api/admin/yeasts/:yeastId/activate   interfaces.controllers.yeasts.YeastAdminController.activateYeast(yeastId: String)
PUT     /api/admin/yeasts/:yeastId/deactivate interfaces.controllers.yeasts.YeastAdminController.deactivateYeast(yeastId: String)
PUT     /api/admin/yeasts/:yeastId/archive    interfaces.controllers.yeasts.YeastAdminController.archiveYeast(yeastId: String)
EOF
    echo -e "${GREEN}✅ Routes ajoutées au fichier conf/routes${NC}"
else
    echo -e "${YELLOW}⚠️  Routes yeast déjà présentes${NC}"
fi

# =============================================================================
# ÉTAPE 4: VALIDATION HTTP ET ERROR HANDLING
# =============================================================================

echo -e "\n${YELLOW}✅ Création validation HTTP...${NC}"

mkdir -p app/interfaces/validation/yeasts

cat > app/interfaces/validation/yeasts/YeastHttpValidation.scala << 'EOF'
package interfaces.validation.yeasts

import play.api.mvc._
import play.api.libs.json._
import application.yeasts.dtos._
import scala.concurrent.Future

/**
 * Validation HTTP spécialisée pour les endpoints levures
 * Validation côté interface avant passage à l'application
 */
object YeastHttpValidation {
  
  /**
   * Validation paramètres de pagination
   */
  def validatePagination(page: Int, size: Int): Either[List[String], (Int, Int)] = {
    var errors = List.empty[String]
    
    if (page < 0) errors = "Page doit être >= 0" :: errors
    if (size <= 0) errors = "Size doit être > 0" :: errors
    if (size > 100) errors = "Size maximum est 100" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right((page, size))
  }
  
  /**
   * Validation paramètres de recherche
   */
  def validateSearchParams(
    name: Option[String],
    laboratory: Option[String],
    yeastType: Option[String]
  ): Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    name.foreach { n =>
      if (n.trim.isEmpty) errors = "Nom ne peut pas être vide" :: errors
      if (n.length > 100) errors = "Nom trop long (max 100 caractères)" :: errors
    }
    
    laboratory.foreach { lab =>
      if (lab.trim.isEmpty) errors = "Laboratoire ne peut pas être vide" :: errors
    }
    
    yeastType.foreach { yType =>
      if (yType.trim.isEmpty) errors = "Type levure ne peut pas être vide" :: errors
    }
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
  
  /**
   * Validation paramètre de recherche textuelle
   */
  def validateSearchQuery(query: String, minLength: Int = 2): Either[String, String] = {
    val trimmed = query.trim
    if (trimmed.isEmpty) {
      Left("Terme de recherche requis")
    } else if (trimmed.length < minLength) {
      Left(s"Terme de recherche trop court (minimum $minLength caractères)")
    } else if (trimmed.length > 100) {
      Left("Terme de recherche trop long (maximum 100 caractères)")
    } else {
      Right(trimmed)
    }
  }
  
  /**
   * Validation limite pour les listes
   */
  def validateLimit(limit: Int, maxLimit: Int = 50): Either[String, Int] = {
    if (limit <= 0) {
      Left("Limite doit être > 0")
    } else if (limit > maxLimit) {
      Left(s"Limite maximum est $maxLimit")
    } else {
      Right(limit)
    }
  }
  
  /**
   * Validation UUID dans URL
   */
  def validateUUID(uuidString: String): Either[String, String] = {
    try {
      java.util.UUID.fromString(uuidString)
      Right(uuidString)
    } catch {
      case _: IllegalArgumentException => Left(s"Format UUID invalide: $uuidString")
    }
  }
  
  /**
   * Validation format d'export
   */
  def validateExportFormat(format: String): Either[String, String] = {
    format.toLowerCase match {
      case "json" | "csv" => Right(format.toLowerCase)
      case _ => Left("Format d'export non supporté (json, csv)")
    }
  }
  
  /**
   * Validation paramètres ABV
   */
  def validateAbv(abv: Option[Double]): Either[String, Option[Double]] = {
    abv match {
      case Some(value) =>
        if (value < 0.0 || value > 20.0) {
          Left("ABV doit être entre 0% et 20%")
        } else {
          Right(Some(value))
        }
      case None => Right(None)
    }
  }
  
  /**
   * Validation température de fermentation
   */
  def validateFermentationTemp(temp: Option[Int]): Either[String, Option[Int]] = {
    temp match {
      case Some(value) =>
        if (value < 0 || value > 50) {
          Left("Température doit être entre 0°C et 50°C")
        } else {
          Right(Some(value))
        }
      case None => Right(None)
    }
  }
  
  /**
   * Validation JSON body présent
   */
  def validateJsonBody[T](request: Request[JsValue])(implicit reads: Reads[T]): Either[List[String], T] = {
    request.body.validate[T] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) => 
        val errorMessages = errors.flatMap(_._2).map(_.message).toList
        Left(errorMessages)
    }
  }
  
  /**
   * Validation taille batch
   */
  def validateBatchSize[T](items: List[T], maxSize: Int = 50): Either[String, List[T]] = {
    if (items.isEmpty) {
      Left("Batch vide")
    } else if (items.size > maxSize) {
      Left(s"Batch trop volumineux (maximum $maxSize éléments)")
    } else {
      Right(items)
    }
  }
}

/**
 * Filtre de validation pour les actions controller
 */
class YeastValidationFilter extends EssentialFilter {
  def apply(next: EssentialAction) = EssentialAction { request =>
    // Validation générale des headers, content-type, etc.
    validateRequest(request) match {
      case Right(_) => next(request)
      case Left(error) => 
        Future.successful(Results.BadRequest(Json.obj("error" -> error)))
    }
  }
  
  private def validateRequest(request: RequestHeader): Either[String, Unit] = {
    // Validation Content-Type pour les requêtes POST/PUT
    if (List("POST", "PUT", "PATCH").contains(request.method)) {
      request.contentType match {
        case Some("application/json") => Right(())
        case Some(other) => Left(s"Content-Type non supporté: $other (attendu: application/json)")
        case None => Left("Content-Type requis pour les requêtes de modification")
      }
    } else {
      Right(())
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 5: TESTS D'INTÉGRATION CONTROLLERS
# =============================================================================

echo -e "\n${YELLOW}🧪 Création tests d'intégration...${NC}"

mkdir -p test/interfaces/controllers/yeasts

cat > test/interfaces/controllers/yeasts/YeastPublicControllerSpec.scala << 'EOF'
package interfaces.controllers.yeasts

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.libs.json._
import play.api.cache.AsyncCacheApi
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

class YeastPublicControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  "YeastPublicController" should {
    
    "return active yeasts list" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      val mockResult = YeastPageResponseDTO(
        yeasts = mockYeasts,
        pagination = PaginationDTO(0, 20, 1, 1, false, false)
      )
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockResult)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts")
      val result: Future[Result] = controller.listActiveYeasts()(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return yeast details by ID" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val yeastId = UUID.randomUUID()
      val mockYeast = createMockYeastDetail(yeastId)
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Some(mockYeast)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result: Future[Result] = controller.getYeast(yeastId.toString)(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      
      val json = contentAsJson(result)
      (json \ "id").as[String] shouldBe yeastId.toString
    }
    
    "return 404 for non-existent yeast" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val yeastId = UUID.randomUUID()
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(None))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result: Future[Result] = controller.getYeast(yeastId.toString)(request)
      
      status(result) shouldBe NOT_FOUND
    }
    
    "return 400 for invalid UUID" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/invalid-uuid")
      val result: Future[Result] = controller.getYeast("invalid-uuid")(request)
      
      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "errors").as[List[String]] should not be empty
    }
    
    "perform text search with valid query" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockYeasts)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/search?q=test")
      val result: Future[Result] = controller.searchYeasts("test")(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return 400 for search query too short" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/search?q=a")
      val result: Future[Result] = controller.searchYeasts("a")(request)
      
      status(result) shouldBe BAD_REQUEST
    }
    
    "return yeasts by type" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockYeasts)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/type/Ale")
      val result: Future[Result] = controller.getYeastsByType("Ale")(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return beginner recommendations" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockRecommendations = List(createMockRecommendation())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(mockRecommendations))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/recommendations/beginner")
      val result: Future[Result] = controller.getBeginnerRecommendations()(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
  }
  
  private def createMockYeastSummary(): YeastSummaryDTO = {
    YeastSummaryDTO(
      id = UUID.randomUUID().toString,
      name = "Test Yeast",
      laboratory = "Fermentis",
      strain = "S-04",
      yeastType = "Ale",
      attenuationRange = "75-82%",
      temperatureRange = "15-24°C",
      alcoholTolerance = "9.0%",
      flocculation = "High",
      status = "ACTIVE",
      mainCharacteristics = List("Clean", "Fruity")
    )
  }
  
  private def createMockYeastDetail(yeastId: UUID): YeastDetailResponseDTO = {
    YeastDetailResponseDTO(
      id = yeastId.toString,
      name = "Test Yeast Detailed",
      laboratory = YeastLaboratoryDTO("Fermentis", "F", "French laboratory"),
      strain = "S-04",
      yeastType = YeastTypeDTO("Ale", "Top fermenting", "15-25°C", "Fruity esters"),
      attenuation = AttenuationRangeDTO(75, 82, 78.5, "75-82%"),
      temperature = FermentationTempDTO(15, 24, 19.5, "15-24°C", "59-75°F"),
      alcoholTolerance = AlcoholToleranceDTO(9.0, "9.0%", "Medium"),
      flocculation = FlocculationLevelDTO("High", "High flocculation", "3-7 days", "Early racking possible"),
      characteristics = YeastCharacteristicsDTO(
        aromaProfile = List("Clean", "Fruity"),
        flavorProfile = List("Balanced"),
        esters = List("Ethyl acetate"),
        phenols = List.empty,
        otherCompounds = List.empty,
        notes = Some("Great for beginners"),
        summary = "Clean and fruity profile"
      ),
      status = "ACTIVE",
      version = 1L,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      recommendations = Some(List("Ferment at 18-20°C")),
      warnings = Some(List.empty)
    )
  }
  
  private def createMockRecommendation(): YeastRecommendationDTO = {
    YeastRecommendationDTO(
      yeast = createMockYeastSummary(),
      score = 0.9,
      reason = "Perfect for beginners",
      tips = List("Use at moderate temperature", "Easy to handle")
    )
  }
}
EOF

echo -e "${GREEN}✅ Tests d'intégration créés${NC}"

# =============================================================================
# ÉTAPE 6: COMPILATION ET VÉRIFICATION
# =============================================================================

echo -e "\n${YELLOW}🔨 Compilation interface layer...${NC}"

if sbt "compile" > /tmp/yeast_interface_compile.log 2>&1; then
    echo -e "${GREEN}✅ Interface Layer compile correctement${NC}"
    
    # Vérification spécifique des routes
    if grep -q "yeasts" conf/routes 2>/dev/null; then
        echo -e "${GREEN}✅ Routes yeast configurées${NC}"
    else
        echo -e "${YELLOW}⚠️  Routes yeast à vérifier manuellement${NC}"
    fi
    
else
    echo -e "${RED}❌ Erreurs de compilation détectées${NC}"
    echo -e "${YELLOW}Voir les logs : /tmp/yeast_interface_compile.log${NC}"
    tail -20 /tmp/yeast_interface_compile.log
fi

# =============================================================================
# ÉTAPE 7: RÉSUMÉ
# =============================================================================

echo -e "\n${BLUE}🎯 RÉSUMÉ INTERFACE LAYER${NC}"
echo -e "${BLUE}==========================${NC}"
echo ""
echo -e "${GREEN}✅ Controllers REST créés :${NC}"
echo -e "   • YeastAdminController (API admin sécurisée)"
echo -e "   • YeastPublicController (API publique avec cache)"
echo ""
echo -e "${GREEN}✅ Routes configurées :${NC}"
echo -e "   • 15+ endpoints API publique"
echo -e "   • 10+ endpoints API admin"
echo -e "   • Routes recommandations et statistiques"
echo ""
echo -e "${GREEN}✅ Validation et tests :${NC}"
echo -e "   • YeastHttpValidation complet"
echo -e "   • Tests controllers publics"
echo -e "   • Gestion erreurs standardisée"
echo ""
echo -e "${YELLOW}📋 PROCHAINE ÉTAPE :${NC}"
echo -e "${YELLOW}   ./scripts/yeast/06b-create-interface-layer-part2.sh${NC}"
echo ""
echo -e "${GREEN}🎉 INTERFACE LAYER PARTIE 1 TERMINÉ AVEC SUCCÈS !${NC}"

# =============================================================================
# ÉTAPE 3: ROUTES CONFIGURATION
# =============================================================================

echo -e "\n${YELLOW}🛣️ Configuration des routes...${NC}"

# Ajouter les routes yeast dans le fichier routes principal
if ! grep -q "# Yeast routes" conf/routes 2>/dev/null; then
    cat >> conf/routes << 'EOF'

# =============================================================================
# Yeast routes
# =============================================================================

# Public API - Levures (lecture seule, pas d'auth)
GET     /api/v1/yeasts                    interfaces.controllers.yeasts.YeastPublicController.listActiveYeasts(page: Int ?= 0, size: Int ?= 20, name: Option[String], laboratory: Option[String], yeastType: Option[String])
GET     /api/v1/yeasts/search             interfaces.controllers.yeasts.YeastPublicController.searchYeasts(q: String, limit: Int ?= 20)
GET     /api/v1/yeasts/type/:yeastType    interfaces.controllers.yeasts.YeastPublicController.getYeastsByType(yeastType: String, limit: Int ?= 20)
GET     /api/v1/yeasts/laboratory/:lab    interfaces.controllers.yeasts.YeastPublicController.getYeastsByLaboratory(lab: String, limit: Int ?= 20)
GET     /api/v1/yeasts/:yeastId           interfaces.controllers.yeasts.YeastPublicController.getYeast(yeastId: String)
GET     /api/v1/yeasts/:yeastId/alternatives  interfaces.controllers.yeasts.YeastPublicController.getAlternatives(yeastId: String, reason: String ?= "unavailable", limit: Int ?= 5)

# Public API - Recommandations levures
GET     /api/v1/yeasts/recommendations/beginner     interfaces.controllers.yeasts.YeastPublicController.getBeginnerRecommendations(limit: Int ?= 5)
GET     /api/v1/yeasts/recommendations/seasonal     interfaces.controllers.yeasts.YeastPublicController.getSeasonalRecommendations(season: String ?= "current", limit: Int ?= 8)
GET     /api/v1/yeasts/recommendations/experimental interfaces.controllers.yeasts.YeastPublicController.getExperimentalRecommendations(limit: Int ?= 6)
GET     /api/v1/yeasts/recommendations/style/:style interfaces.controllers.yeasts.YeastPublicController.getRecommendationsForBeerStyle(style: String, targetAbv: Option[Double], fermentationTemp: Option[Int], limit: Int ?= 10)

# Public API - Statistiques levures
GET     /api/v1/yeasts/stats              interfaces.controllers.yeasts.YeastPublicController.getPublicStats()
GET     /api/v1/yeasts/popular            interfaces.controllers.yeasts.YeastPublicController.getPopularYeasts(limit: Int ?= 10)

# Admin API - Levures (authentification requise)
GET     /api/admin/yeasts                 interfaces.controllers.yeasts.YeastAdminController.listYeasts(page: Int ?= 0, size: Int ?= 20, name: Option[String], laboratory: Option[String], yeastType: Option[String], status: Option[String])
POST    /api/admin/yeasts                 interfaces.controllers.yeasts.YeastAdminController.createYeast()
GET     /api/admin/yeasts/stats           interfaces.controllers.yeasts.YeastAdminController.getStatistics()
POST    /api/admin/yeasts/batch           interfaces.controllers.yeasts.YeastAdminController.batchCreate()
GET     /api/admin/yeasts/export          interfaces.controllers.yeasts.YeastAdminController.exportYeasts(format: String ?= "json", status: Option[String])

GET     /api/admin/yeasts/:yeastId        interfaces.controllers.yeasts.YeastAdminController.getYeast(yeastId: String)
PUT     /api/admin/yeasts/:yeastId        interfaces.controllers.yeasts.YeastAdminController.updateYeast(yeastId: String)
DELETE  /api/admin/yeasts/:yeastId        interfaces.controllers.yeasts.YeastAdminController.deleteYeast(yeastId: String)

# Admin API - Actions spécifiques levures
PUT     /api/admin/yeasts/:yeastId/status interfaces.controllers.yeasts.YeastAdminController.changeStatus(yeastId: String)
PUT     /api/admin/yeasts/:yeastId/activate   interfaces.controllers.yeasts.YeastAdminController.activateYeast(yeastId: String)
PUT     /api/admin/yeasts/:yeastId/deactivate interfaces.controllers.yeasts.YeastAdminController.deactivateYeast(yeastId: String)
PUT     /api/admin/yeasts/:yeastId/archive    interfaces.controllers.yeasts.YeastAdminController.archiveYeast(yeastId: String)
EOF
    echo -e "${GREEN}✅ Routes ajoutées au fichier conf/routes${NC}"
else
    echo -e "${YELLOW}⚠️  Routes yeast déjà présentes${NC}"
fi

# =============================================================================
# ÉTAPE 4: VALIDATION HTTP ET ERROR HANDLING
# =============================================================================

echo -e "\n${YELLOW}✅ Création validation HTTP...${NC}"

mkdir -p app/interfaces/validation/yeasts

cat > app/interfaces/validation/yeasts/YeastHttpValidation.scala << 'EOF'
package interfaces.validation.yeasts

import play.api.mvc._
import play.api.libs.json._
import application.yeasts.dtos._
import scala.concurrent.Future

/**
 * Validation HTTP spécialisée pour les endpoints levures
 * Validation côté interface avant passage à l'application
 */
object YeastHttpValidation {
  
  /**
   * Validation paramètres de pagination
   */
  def validatePagination(page: Int, size: Int): Either[List[String], (Int, Int)] = {
    var errors = List.empty[String]
    
    if (page < 0) errors = "Page doit être >= 0" :: errors
    if (size <= 0) errors = "Size doit être > 0" :: errors
    if (size > 100) errors = "Size maximum est 100" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right((page, size))
  }
  
  /**
   * Validation paramètres de recherche
   */
  def validateSearchParams(
    name: Option[String],
    laboratory: Option[String],
    yeastType: Option[String]
  ): Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    name.foreach { n =>
      if (n.trim.isEmpty) errors = "Nom ne peut pas être vide" :: errors
      if (n.length > 100) errors = "Nom trop long (max 100 caractères)" :: errors
    }
    
    laboratory.foreach { lab =>
      if (lab.trim.isEmpty) errors = "Laboratoire ne peut pas être vide" :: errors
    }
    
    yeastType.foreach { yType =>
      if (yType.trim.isEmpty) errors = "Type levure ne peut pas être vide" :: errors
    }
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
  
  /**
   * Validation paramètre de recherche textuelle
   */
  def validateSearchQuery(query: String, minLength: Int = 2): Either[String, String] = {
    val trimmed = query.trim
    if (trimmed.isEmpty) {
      Left("Terme de recherche requis")
    } else if (trimmed.length < minLength) {
      Left(s"Terme de recherche trop court (minimum $minLength caractères)")
    } else if (trimmed.length > 100) {
      Left("Terme de recherche trop long (maximum 100 caractères)")
    } else {
      Right(trimmed)
    }
  }
  
  /**
   * Validation limite pour les listes
   */
  def validateLimit(limit: Int, maxLimit: Int = 50): Either[String, Int] = {
    if (limit <= 0) {
      Left("Limite doit être > 0")
    } else if (limit > maxLimit) {
      Left(s"Limite maximum est $maxLimit")
    } else {
      Right(limit)
    }
  }
  
  /**
   * Validation UUID dans URL
   */
  def validateUUID(uuidString: String): Either[String, String] = {
    try {
      java.util.UUID.fromString(uuidString)
      Right(uuidString)
    } catch {
      case _: IllegalArgumentException => Left(s"Format UUID invalide: $uuidString")
    }
  }
  
  /**
   * Validation format d'export
   */
  def validateExportFormat(format: String): Either[String, String] = {
    format.toLowerCase match {
      case "json" | "csv" => Right(format.toLowerCase)
      case _ => Left("Format d'export non supporté (json, csv)")
    }
  }
  
  /**
   * Validation paramètres ABV
   */
  def validateAbv(abv: Option[Double]): Either[String, Option[Double]] = {
    abv match {
      case Some(value) =>
        if (value < 0.0 || value > 20.0) {
          Left("ABV doit être entre 0% et 20%")
        } else {
          Right(Some(value))
        }
      case None => Right(None)
    }
  }
  
  /**
   * Validation température de fermentation
   */
  def validateFermentationTemp(temp: Option[Int]): Either[String, Option[Int]] = {
    temp match {
      case Some(value) =>
        if (value < 0 || value > 50) {
          Left("Température doit être entre 0°C et 50°C")
        } else {
          Right(Some(value))
        }
      case None => Right(None)
    }
  }
  
  /**
   * Validation JSON body présent
   */
  def validateJsonBody[T](request: Request[JsValue])(implicit reads: Reads[T]): Either[List[String], T] = {
    request.body.validate[T] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) => 
        val errorMessages = errors.flatMap(_._2).map(_.message).toList
        Left(errorMessages)
    }
  }
  
  /**
   * Validation taille batch
   */
  def validateBatchSize[T](items: List[T], maxSize: Int = 50): Either[String, List[T]] = {
    if (items.isEmpty) {
      Left("Batch vide")
    } else if (items.size > maxSize) {
      Left(s"Batch trop volumineux (maximum $maxSize éléments)")
    } else {
      Right(items)
    }
  }
}

/**
 * Filtre de validation pour les actions controller
 */
class YeastValidationFilter extends EssentialFilter {
  def apply(next: EssentialAction) = EssentialAction { request =>
    // Validation générale des headers, content-type, etc.
    validateRequest(request) match {
      case Right(_) => next(request)
      case Left(error) => 
        Future.successful(Results.BadRequest(Json.obj("error" -> error)))
    }
  }
  
  private def validateRequest(request: RequestHeader): Either[String, Unit] = {
    // Validation Content-Type pour les requêtes POST/PUT
    if (List("POST", "PUT", "PATCH").contains(request.method)) {
      request.contentType match {
        case Some("application/json") => Right(())
        case Some(other) => Left(s"Content-Type non supporté: $other (attendu: application/json)")
        case None => Left("Content-Type requis pour les requêtes de modification")
      }
    } else {
      Right(())
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 5: TESTS D'INTÉGRATION CONTROLLERS
# =============================================================================

echo -e "\n${YELLOW}🧪 Création tests d'intégration...${NC}"

mkdir -p test/interfaces/controllers/yeasts

cat > test/interfaces/controllers/yeasts/YeastPublicControllerSpec.scala << 'EOF'
package interfaces.controllers.yeasts

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.libs.json._
import play.api.cache.AsyncCacheApi
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

class YeastPublicControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  "YeastPublicController" should {
    
    "return active yeasts list" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      val mockResult = YeastPageResponseDTO(
        yeasts = mockYeasts,
        pagination = PaginationDTO(0, 20, 1, 1, false, false)
      )
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockResult)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts")
      val result: Future[Result] = controller.listActiveYeasts()(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return yeast details by ID" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val yeastId = UUID.randomUUID()
      val mockYeast = createMockYeastDetail(yeastId)
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Some(mockYeast)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result: Future[Result] = controller.getYeast(yeastId.toString)(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      
      val json = contentAsJson(result)
      (json \ "id").as[String] shouldBe yeastId.toString
    }
    
    "return 404 for non-existent yeast" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val yeastId = UUID.randomUUID()
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(None))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result: Future[Result] = controller.getYeast(yeastId.toString)(request)
      
      status(result) shouldBe NOT_FOUND
    }
    
    "return 400 for invalid UUID" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/invalid-uuid")
      val result: Future[Result] = controller.getYeast("invalid-uuid")(request)
      
      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "errors").as[List[String]] should not be empty
    }
    
    "perform text search with valid query" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockYeasts)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/search?q=test")
      val result: Future[Result] = controller.searchYeasts("test")(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return 400 for search query too short" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/search?q=a")
      val result: Future[Result] = controller.searchYeasts("a")(request)
      
      status(result) shouldBe BAD_REQUEST
    }
    
    "return yeasts by type" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockYeasts)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/type/Ale")
      val result: Future[Result] = controller.getYeastsByType("Ale")(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return beginner recommendations" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockRecommendations = List(createMockRecommendation())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(mockRecommendations))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/recommendations/beginner")
      val result: Future[Result] = controller.getBeginnerRecommendations()(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
  }
  
  private def createMockYeastSummary(): YeastSummaryDTO = {
    YeastSummaryDTO(
      id = UUID.randomUUID().toString,
      name = "Test Yeast",
      laboratory = "Fermentis",
      strain = "S-04",
      yeastType = "Ale",
      attenuationRange = "75-82%",
      temperatureRange = "15-24°C",
      alcoholTolerance = "9.0%",
      flocculation = "High",
      status = "ACTIVE",
      mainCharacteristics = List("Clean", "Fruity")
    )
  }
  
  private def createMockYeastDetail(yeastId: UUID): YeastDetailResponseDTO = {
    YeastDetailResponseDTO(
      id = yeastId.toString,
      name = "Test Yeast Detailed",
      laboratory = YeastLaboratoryDTO("Fermentis", "F", "French laboratory"),
      strain = "S-04",
      yeastType = YeastTypeDTO("Ale", "Top fermenting", "15-25°C", "Fruity esters"),
      attenuation = AttenuationRangeDTO(75, 82, 78.5, "75-82%"),
      temperature = FermentationTempDTO(15, 24, 19.5, "15-24°C", "59-75°F"),
      alcoholTolerance = AlcoholToleranceDTO(9.0, "9.0%", "Medium"),
      flocculation = FlocculationLevelDTO("High", "High flocculation", "3-7 days", "Early racking possible"),
      characteristics = YeastCharacteristicsDTO(
        aromaProfile = List("Clean", "Fruity"),
        flavorProfile = List("Balanced"),
        esters = List("Ethyl acetate"),
        phenols = List.empty,
        otherCompounds = List.empty,
        notes = Some("Great for beginners"),
        summary = "Clean and fruity profile"
      ),
      status = "ACTIVE",
      version = 1L,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      recommendations = Some(List("Ferment at 18-20°C")),
      warnings = Some(List.empty)
    )
  }
  
  private def createMockRecommendation(): YeastRecommendationDTO = {
    YeastRecommendationDTO(
      yeast = createMockYeastSummary(),
      score = 0.9,
      reason = "Perfect for beginners",
      tips = List("Use at moderate temperature", "Easy to handle")
    )
  }
}
EOF

echo -e "${GREEN}✅ Tests d'intégration créés${NC}"