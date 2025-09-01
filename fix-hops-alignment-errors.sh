#!/bin/bash

# =============================================================================
# CORRECTIF ERREURS ALIGNEMENT HOPS
# =============================================================================
# Corrige les erreurs de compilation en s'adaptant à l'interface Hops existante
# =============================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTIF ERREURS ALIGNEMENT HOPS${NC}"
echo -e "${BLUE}===================================${NC}"

# D'abord, regardons l'interface HopReadRepository pour s'adapter
echo -e "\n${YELLOW}🔍 Vérification interface HopReadRepository${NC}"
if [ -f "app/domain/hops/repositories/HopReadRepository.scala" ]; then
    echo "Méthodes disponibles dans HopReadRepository:"
    grep -E "def \w+" app/domain/hops/repositories/HopReadRepository.scala | head -10
else
    echo "Interface HopReadRepository non trouvée"
fi

# =============================================================================
# CORRECTION 1 : HopReadModel - Région Optional
# =============================================================================

echo -e "\n${YELLOW}🔧 CORRECTION 1 : HopReadModel (région)${NC}"

cat > app/application/queries/public/hops/readmodels/HopReadModel.scala << 'EOF'
package application.queries.public.hops.readmodels

import domain.hops.model.HopAggregate
import play.api.libs.json._

case class HopReadModel(
  id: String,
  name: String,
  alphaAcid: Double,
  betaAcid: Option[Double],
  origin: HopOriginResponse,
  usage: String,
  description: Option[String],
  aromaProfile: List[String],
  status: String,
  source: String,
  credibilityScore: Int,
  isActive: Boolean
)

case class HopOriginResponse(
  code: String,
  name: String,
  region: Option[String], // Corrigé: Option[String]
  isNoble: Boolean,
  isNewWorld: Boolean
)

object HopReadModel {
  
  def fromAggregate(hop: HopAggregate): HopReadModel = {
    HopReadModel(
      id = hop.id.value,
      name = hop.name.value,
      alphaAcid = hop.alphaAcid.value,
      betaAcid = hop.betaAcid.map(_.value),
      origin = HopOriginResponse(
        code = hop.origin.code,
        name = hop.origin.name,
        region = Some(hop.origin.region), // Corrigé: wrap dans Some()
        isNoble = hop.origin.isNoble,
        isNewWorld = hop.origin.isNewWorld
      ),
      usage = hop.usage match {
        case domain.hops.model.HopUsage.Bittering => "BITTERING"
        case domain.hops.model.HopUsage.Aroma => "AROMA"
        case domain.hops.model.HopUsage.DualPurpose => "DUAL_PURPOSE"
        case domain.hops.model.HopUsage.NobleHop => "NOBLE_HOP"
      },
      description = hop.description.map(_.value),
      aromaProfile = hop.aromaProfile,
      status = hop.status match {
        case domain.hops.model.HopStatus.Active => "ACTIVE"
        case domain.hops.model.HopStatus.Discontinued => "DISCONTINUED"
        case domain.hops.model.HopStatus.Limited => "LIMITED"
      },
      source = hop.source match {
        case domain.hops.model.HopSource.Manual => "MANUAL"
        case domain.hops.model.HopSource.AI_Discovery => "AI_DISCOVERED"
        case domain.hops.model.HopSource.Import => "IMPORT"
      },
      credibilityScore = hop.credibilityScore,
      isActive = hop.status == domain.hops.model.HopStatus.Active
    )
  }
  
  implicit val originFormat: Format[HopOriginResponse] = Json.format[HopOriginResponse]
  implicit val format: Format[HopReadModel] = Json.format[HopReadModel]
}

case class HopListResponse(
  hops: List[HopReadModel],
  totalCount: Long,
  page: Int,
  size: Int,
  hasNext: Boolean
)

object HopListResponse {
  
  def create(
    hops: List[HopReadModel],
    totalCount: Long,
    page: Int,
    size: Int
  ): HopListResponse = {
    HopListResponse(
      hops = hops,
      totalCount = totalCount,
      page = page,
      size = size,
      hasNext = (page + 1) * size < totalCount
    )
  }
  
  implicit val format: Format[HopListResponse] = Json.format[HopListResponse]
}
EOF

echo -e "✅ ${GREEN}HopReadModel corrigé${NC}"

# =============================================================================
# CORRECTION 2 : HopListQueryHandler - Interface compatible
# =============================================================================

echo -e "\n${YELLOW}🔧 CORRECTION 2 : HopListQueryHandler compatible${NC}"

cat > app/application/queries/public/hops/handlers/HopListQueryHandler.scala << 'EOF'
package application.queries.public.hops.handlers

import application.queries.public.hops.HopListQuery
import application.queries.public.hops.readmodels.{HopReadModel, HopListResponse}
import domain.hops.repositories.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class HopListQueryHandler @Inject()(
    hopReadRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: HopListQuery): Future[Try[HopListResponse]] = {
    println(s"🍺 HopListQueryHandler - page: ${query.page}, size: ${query.size}")
    
    // Utilise la méthode existante de l'interface Hops
    hopReadRepository.findActiveHops(query.page, query.size).map { case (hops, totalCount) =>
      try {
        println(s"📊 Repository retourné: ${hops.length} hops, total: $totalCount")
        
        val hopReadModels = hops.map(HopReadModel.fromAggregate)
        val response = HopListResponse.create(
          hops = hopReadModels,
          totalCount = totalCount,
          page = query.page,
          size = query.size
        )
        println(s"✅ Response créé: ${response.hops.length} hops dans la réponse")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans HopListQueryHandler: ${ex.getMessage}")
          ex.printStackTrace()
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans HopListQueryHandler: ${ex.getMessage}")
        ex.printStackTrace()
        Failure(ex)
    }
  }
}
EOF

echo -e "✅ ${GREEN}HopListQueryHandler corrigé${NC}"

# =============================================================================
# CORRECTION 3 : HopSearchQueryHandler - Interface compatible
# =============================================================================

echo -e "\n${YELLOW}🔧 CORRECTION 3 : HopSearchQueryHandler compatible${NC}"

cat > app/application/queries/public/hops/handlers/HopSearchQueryHandler.scala << 'EOF'
package application.queries.public.hops.handlers

import application.queries.public.hops.HopSearchQuery
import application.queries.public.hops.readmodels.{HopReadModel, HopListResponse}
import domain.hops.repositories.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class HopSearchQueryHandler @Inject()(
    hopReadRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: HopSearchQuery): Future[Try[HopListResponse]] = {
    println(s"🍺 HopSearchQueryHandler - recherche: ${query.name}")
    
    // Utilise la méthode searchHops existante de l'interface Hops
    hopReadRepository.searchHops(
      name = query.name,
      originCode = query.originCode,
      usage = query.usage.map(_.toString),
      minAlphaAcid = query.minAlphaAcid,
      maxAlphaAcid = query.maxAlphaAcid,
      activeOnly = true
    ).map { hops =>
      try {
        // Pagination manuelle sur les résultats
        val offset = query.page * query.size
        val paginatedHops = hops.drop(offset).take(query.size)
        
        val hopReadModels = paginatedHops.map(HopReadModel.fromAggregate)
        val response = HopListResponse.create(
          hops = hopReadModels,
          totalCount = hops.length.toLong,
          page = query.page,
          size = query.size
        )
        
        println(s"📊 Recherche: ${hops.length} résultats totaux, ${paginatedHops.length} retournés")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans HopSearchQueryHandler: ${ex.getMessage}")
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans HopSearchQueryHandler: ${ex.getMessage}")
        Failure(ex)
    }
  }
}
EOF

echo -e "✅ ${GREEN}HopSearchQueryHandler corrigé${NC}"

# =============================================================================
# CORRECTION 4 : HopsController - HopUsage.fromString
# =============================================================================

echo -e "\n${YELLOW}🔧 CORRECTION 4 : HopsController (HopUsage)${NC}"

cat > app/interfaces/http/api/v1/hops/HopsController.scala << 'EOF'
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
EOF

echo -e "✅ ${GREEN}HopsController corrigé${NC}"

# =============================================================================
# TEST COMPILATION
# =============================================================================

echo -e "\n${YELLOW}⚙️  Test de compilation${NC}"

echo "Compilation du projet..."
if sbt compile; then
    echo -e "✅ ${GREEN}🎉 COMPILATION RÉUSSIE !${NC}"
    
    echo -e "\n${GREEN}🔄 Corrections appliquées:${NC}"
    echo "• HopReadModel: région en Option[String]"
    echo "• HopListQueryHandler: utilise findActiveHops existant"
    echo "• HopSearchQueryHandler: utilise searchHops existant"
    echo "• HopsController: conversion manuelle String → HopUsage"
    
    echo -e "\n${YELLOW}🧪 Pour tester:${NC}"
    echo "1. sbt run"
    echo "2. ./test_aligned_domains.sh"
    echo "3. ./test-hops-malts-domains.sh"
    
    echo -e "\n${GREEN}Le format sera maintenant:${NC}"
    echo 'GET /api/v1/hops: {"hops": [...], "totalCount": X, "page": 0, "size": 20}'
    
else
    echo -e "❌ ${RED}Erreurs de compilation persistantes${NC}"
    echo "Montrez-moi l'interface HopReadRepository:"
    echo "cat app/domain/hops/repositories/HopReadRepository.scala"