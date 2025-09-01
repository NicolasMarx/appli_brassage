#!/bin/bash

# =============================================================================
# SCRIPT D'ALIGNEMENT HOPS SUR MALTS
# =============================================================================
# Aligne la structure et les APIs du domaine Hops sur le domaine Malts
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 ALIGNEMENT DOMAINE HOPS SUR MALTS${NC}"
echo -e "${BLUE}===================================${NC}"

# =============================================================================
# ÉTAPE 1 : CRÉATION DE LA NOUVELLE STRUCTURE API HOPS
# =============================================================================

echo -e "\n${YELLOW}📁 ÉTAPE 1 : Création nouvelle structure API Hops${NC}"

# Créer la nouvelle structure
mkdir -p app/interfaces/http/api/v1/hops
mkdir -p app/application/queries/public/hops/readmodels
mkdir -p app/application/queries/public/hops/handlers

echo -e "✅ ${GREEN}Structure créée${NC}"

# =============================================================================
# ÉTAPE 2 : CRÉATION DU READMODEL HOPS (BASÉ SUR MALTS)
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 2 : Création HopReadModel${NC}"

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
  region: Option[String],
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
        region = hop.origin.region,
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

echo -e "✅ ${GREEN}HopReadModel créé${NC}"

# =============================================================================
# ÉTAPE 3 : CRÉATION DES QUERIES HOPS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 3 : Création Queries Hops${NC}"

# HopListQuery
cat > app/application/queries/public/hops/HopListQuery.scala << 'EOF'
package application.queries.public.hops

import domain.hops.model.HopUsage

case class HopListQuery(
  usage: Option[HopUsage] = None,
  page: Int = 0,
  size: Int = 20
)
EOF

# HopDetailQuery  
cat > app/application/queries/public/hops/HopDetailQuery.scala << 'EOF'
package application.queries.public.hops

case class HopDetailQuery(
  hopId: String
)
EOF

# HopSearchQuery
cat > app/application/queries/public/hops/HopSearchQuery.scala << 'EOF'
package application.queries.public.hops

import domain.hops.model.HopUsage

case class HopSearchQuery(
  name: Option[String] = None,
  usage: Option[HopUsage] = None,
  minAlphaAcid: Option[Double] = None,
  maxAlphaAcid: Option[Double] = None,
  originCode: Option[String] = None,
  page: Int = 0,
  size: Int = 20
)
EOF

echo -e "✅ ${GREEN}Queries Hops créées${NC}"

# =============================================================================
# ÉTAPE 4 : CRÉATION DES HANDLERS HOPS ALIGNÉS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 4 : Création Handlers Hops alignés${NC}"

# HopListQueryHandler aligné sur MaltListQueryHandler
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
    
    // Appel au repository avec les mêmes paramètres que Malts
    hopReadRepository.findAll(
      page = query.page,
      pageSize = query.size,
      activeOnly = true
    ).map { hops =>
      try {
        println(s"📊 Repository retourné: ${hops.length} hops")
        
        val hopReadModels = hops.map(HopReadModel.fromAggregate)
        val response = HopListResponse.create(
          hops = hopReadModels,
          totalCount = hops.length.toLong,
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

# HopSearchQueryHandler aligné  
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
    
    // Récupère tous les hops actifs avec pagination large pour le filtrage
    hopReadRepository.findAll(
      page = 0,
      pageSize = 1000,
      activeOnly = true
    ).map { allHops =>
      try {
        // Filtrage basique en mémoire (même pattern que Malts)
        val filteredHops = allHops.filter { hop =>
          val nameMatch = query.name.forall(n => 
            hop.name.value.toLowerCase.contains(n.toLowerCase)
          )
          val usageMatch = query.usage.forall(u => 
            hop.usage == u
          )
          val alphaMatch = (query.minAlphaAcid, query.maxAlphaAcid) match {
            case (Some(min), Some(max)) => hop.alphaAcid.value >= min && hop.alphaAcid.value <= max
            case (Some(min), None) => hop.alphaAcid.value >= min
            case (None, Some(max)) => hop.alphaAcid.value <= max
            case (None, None) => true
          }
          
          nameMatch && usageMatch && alphaMatch
        }
        
        // Pagination manuelle
        val offset = query.page * query.size
        val paginatedHops = filteredHops.drop(offset).take(query.size)
        
        val hopReadModels = paginatedHops.map(HopReadModel.fromAggregate)
        val response = HopListResponse.create(
          hops = hopReadModels,
          totalCount = filteredHops.length.toLong,
          page = query.page,
          size = query.size
        )
        
        println(s"📊 Recherche: ${filteredHops.length} résultats, ${paginatedHops.length} retournés")
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

echo -e "✅ ${GREEN}Handlers Hops alignés créés${NC}"

# =============================================================================
# ÉTAPE 5 : NOUVEAU CONTRÔLEUR HOPS ALIGNÉ SUR MALTS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 5 : Nouveau contrôleur Hops${NC}"

cat > app/interfaces/http/api/v1/hops/HopsController.scala << 'EOF'
package interfaces.http.api.v1.hops

import application.queries.public.hops._
import application.queries.public.hops.handlers._
import domain.hops.model.{HopId, HopUsage}
import domain.hops.repositories.HopReadRepository
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
        val query = HopSearchQuery(
          name = searchRequest.name,
          usage = searchRequest.usage.flatMap(HopUsage.fromString),
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

echo -e "✅ ${GREEN}Nouveau contrôleur Hops créé (aligné sur Malts)${NC}"

# =============================================================================
# ÉTAPE 6 : MISE À JOUR DES ROUTES
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 6 : Mise à jour des routes${NC}"

# Backup des routes actuelles
cp conf/routes conf/routes.backup.pre-alignment

# Remplacer les routes Hops pour pointer vers la nouvelle structure
sed -i '' 's|controllers.api.v1.hops.HopsController|interfaces.http.api.v1.hops.HopsController|g' conf/routes

echo -e "✅ ${GREEN}Routes mises à jour${NC}"

# =============================================================================
# ÉTAPE 7 : CORRECTION DU HANDLER DETAIL EXISTANT
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 7 : Correction handler Detail Hops existant${NC}"

# Backup du handler existant
if [ -f "app/application/queries/public/hops/handlers/HopDetailQueryHandler.scala" ]; then
    cp app/application/queries/public/hops/handlers/HopDetailQueryHandler.scala \
       app/application/queries/public/hops/handlers/HopDetailQueryHandler.scala.backup
fi

# Remplacer par une version alignée sur Malts
cat > app/application/queries/public/hops/handlers/HopDetailQueryHandler.scala << 'EOF'
package application.queries.public.hops.handlers

import application.queries.public.hops.HopDetailQuery
import application.queries.public.hops.readmodels.HopReadModel
import domain.hops.model.HopId
import domain.hops.repositories.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class HopDetailQueryHandler @Inject()(
    hopReadRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: HopDetailQuery): Future[Try[Option[HopReadModel]]] = {
    println(s"🍺 HopDetailQueryHandler - ID: ${query.hopId}")
    
    try {
      val hopId = HopId(query.hopId)
      println(s"✅ HopId créé: $hopId")
      
      hopReadRepository.findById(hopId).map { hopOpt =>
        try {
          val readModelOpt = hopOpt.map { hop =>
            println(s"✅ Hop trouvé: ${hop.name.value}")
            HopReadModel.fromAggregate(hop)
          }
          if (readModelOpt.isEmpty) {
            println(s"⚠️ Aucun hop trouvé pour l'ID: ${query.hopId}")
          }
          Success(readModelOpt)
        } catch {
          case ex: Exception =>
            println(s"❌ Erreur lors de la conversion: ${ex.getMessage}")
            Failure(ex)
        }
      }.recover {
        case ex: Exception => 
          println(s"❌ Erreur repository: ${ex.getMessage}")
          Failure(ex)
      }
      
    } catch {
      case ex: Exception =>
        println(s"❌ Erreur lors de la création HopId: ${ex.getMessage}")
        Future.successful(Failure(ex))
    }
  }
}
EOF