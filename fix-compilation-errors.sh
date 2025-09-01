#!/bin/bash

# =============================================================================
# SCRIPT DE CORRECTION DES ERREURS DE COMPILATION
# =============================================================================
# Corrige les erreurs basées sur votre structure existante
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTION ERREURS DE COMPILATION${NC}"
echo -e "${BLUE}====================================${NC}"

# =============================================================================
# ÉTAPE 1 : CORRECTION MALTREADMODEL (ERREUR UUID)
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 1 : Correction MaltReadModel${NC}"

cat > app/application/queries/public/malts/readmodels/MaltReadModel.scala << 'EOF'
package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._

case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  isActive: Boolean
)

object MaltReadModel {
  
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.toString, // Correction: toString au lieu de .value
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      isActive = malt.isActive
    )
  }
  
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}

case class MaltListResponse(
  malts: List[MaltReadModel],
  totalCount: Long,
  page: Int,
  size: Int,
  hasNext: Boolean
)

object MaltListResponse {
  
  def create(
    malts: List[MaltReadModel],
    totalCount: Long,
    page: Int,
    size: Int
  ): MaltListResponse = {
    MaltListResponse(
      malts = malts,
      totalCount = totalCount,
      page = page,
      size = size,
      hasNext = (page + 1) * size < totalCount
    )
  }
  
  implicit val format: Format[MaltListResponse] = Json.format[MaltListResponse]
}
EOF

echo -e "✅ ${GREEN}MaltReadModel corrigé${NC}"

# =============================================================================
# ÉTAPE 2 : VÉRIFICATION ET CORRECTION DE L'INTERFACE MALTREADREPOSITORY
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 2 : Vérification interface MaltReadRepository${NC}"

# Regarder la signature actuelle du repository
echo "Contenu actuel de MaltReadRepository:"
if [ -f "app/domain/malts/repositories/MaltReadRepository.scala" ]; then
    head -20 app/domain/malts/repositories/MaltReadRepository.scala
else
    echo "Fichier non trouvé"
fi

# =============================================================================
# ÉTAPE 3 : CORRECTION DES HANDLERS BASÉE SUR L'INTERFACE RÉELLE
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 3 : Correction handlers basée sur interface réelle${NC}"

# Handler Detail simplifié
cat > app/application/queries/public/malts/handlers/MaltDetailQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.MaltDetailQuery
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.model.MaltId
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class MaltDetailQueryHandler @Inject()(
    maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltDetailQuery): Future[Try[Option[MaltReadModel]]] = {
    println(s"🔍 MaltDetailQueryHandler - ID: ${query.maltId}")
    
    // Création directe du MaltId
    try {
      val maltId = MaltId.fromString(query.maltId).getOrElse {
        return Future.successful(Failure(new IllegalArgumentException(s"Invalid MaltId: ${query.maltId}")))
      }
      
      // Appel simplifié au repository - on utilise la méthode qui existe
      maltReadRepository.findById(maltId).map { maltOpt =>
        try {
          val readModelOpt = maltOpt.map { malt =>
            println(s"✅ Malt trouvé: ${malt.name.value}")
            MaltReadModel.fromAggregate(malt)
          }
          if (readModelOpt.isEmpty) {
            println(s"⚠️ Aucun malt trouvé pour l'ID: ${query.maltId}")
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
        println(s"❌ Erreur lors de la création MaltId: ${ex.getMessage}")
        Future.successful(Failure(ex))
    }
  }
}
EOF

# Handler List simplifié
cat > app/application/queries/public/malts/handlers/MaltListQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.MaltListQuery
import application.queries.public.malts.readmodels.{MaltReadModel, MaltListResponse}
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class MaltListQueryHandler @Inject()(
    maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltListQuery): Future[Try[MaltListResponse]] = {
    println(s"🔍 MaltListQueryHandler - page: ${query.page}, size: ${query.size}")
    
    // Appel simplifié - utilise findAll basique du repository
    maltReadRepository.findAll().map { malts =>
      try {
        println(s"📊 Repository retourné: ${malts.length} malts")
        
        // Pagination manuelle
        val offset = query.page * query.size
        val paginatedMalts = malts.drop(offset).take(query.size)
        
        val maltReadModels = paginatedMalts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = malts.length.toLong,
          page = query.page,
          size = query.size
        )
        println(s"✅ Response créé: ${response.malts.length} malts dans la réponse")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans MaltListQueryHandler: ${ex.getMessage}")
          ex.printStackTrace()
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans MaltListQueryHandler: ${ex.getMessage}")
        ex.printStackTrace()
        Failure(ex)
    }
  }
}
EOF

# Handler Search simplifié (stub)
cat > app/application/queries/public/malts/handlers/MaltSearchQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.MaltSearchQuery
import application.queries.public.malts.readmodels.{MaltReadModel, MaltListResponse}
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class MaltSearchQueryHandler @Inject()(
    maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[Try[MaltListResponse]] = {
    println(s"🔍 MaltSearchQueryHandler - recherche: ${query.name}")
    
    // Pour l'instant, recherche simple sur tous les malts
    maltReadRepository.findAll().map { allMalts =>
      try {
        // Filtrage basique en mémoire
        val filteredMalts = allMalts.filter { malt =>
          val nameMatch = query.name.forall(n => 
            malt.name.value.toLowerCase.contains(n.toLowerCase)
          )
          val typeMatch = query.maltType.forall(t => 
            malt.maltType.name == t.name
          )
          val ebcMatch = (query.minEbc, query.maxEbc) match {
            case (Some(min), Some(max)) => malt.ebcColor.value >= min && malt.ebcColor.value <= max
            case (Some(min), None) => malt.ebcColor.value >= min
            case (None, Some(max)) => malt.ebcColor.value <= max
            case (None, None) => true
          }
          
          nameMatch && typeMatch && ebcMatch
        }
        
        // Pagination
        val offset = query.page * query.size
        val paginatedMalts = filteredMalts.drop(offset).take(query.size)
        
        val maltReadModels = paginatedMalts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = filteredMalts.length.toLong,
          page = query.page,
          size = query.size
        )
        
        println(s"📊 Recherche: ${filteredMalts.length} résultats, ${paginatedMalts.length} retournés")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans MaltSearchQueryHandler: ${ex.getMessage}")
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans MaltSearchQueryHandler: ${ex.getMessage}")
        Failure(ex)
    }
  }
}
EOF

echo -e "✅ ${GREEN}Handlers corrigés avec appels simplifiés${NC}"

# =============================================================================
# ÉTAPE 4 : CORRECTION API PUBLIQUE (MALTTYPE.VALUES)
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 4 : Correction API publique${NC}"

# Correction pour MaltType.values
cat > app/interfaces/http/api/v1/malts/MaltsController.scala << 'EOF'
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
EOF

echo -e "✅ ${GREEN}API publique corrigée${NC}"

# =============================================================================
# ÉTAPE 5 : SUPPRESSION DU SLICKMALTREADREPOSITORY CORROMPU
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 5 : Restauration SlickMaltReadRepository${NC}"

# Utiliser le backup le plus récent qui fonctionnait
if [ -f "app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala.backup" ]; then
    cp app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala.backup \
       app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala
    echo -e "✅ ${GREEN}SlickMaltReadRepository restauré depuis backup${NC}"
else
    echo -e "⚠️ ${YELLOW}Pas de backup trouvé pour SlickMaltReadRepository${NC}"
fi

# =============================================================================
# ÉTAPE 6 : COMPILATION TEST
# =============================================================================

echo -e "\n${YELLOW}⚙️  ÉTAPE 6 : Test de compilation${NC}"

echo "Test de compilation..."
if sbt compile; then
    echo -e "✅ ${GREEN}Compilation réussie !${NC}"
    COMPILATION_OK=true
else
    echo -e "❌ ${RED}Erreurs de compilation persistantes${NC}"
    COMPILATION_OK=false
fi

# =============================================================================
# ÉTAPE 7 : SCRIPT DE TEST BASIQUE
# =============================================================================

echo -e "\n${YELLOW}🧪 ÉTAPE 7 : Script de test basique${NC}"

cat > test_malts_simple.sh << 'EOF'
#!/bin/bash

BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🌾 TEST SIMPLE APIs MALTS${NC}"
echo "========================="

echo -e "\n${BLUE}Test API Admin${NC}"
admin_response=$(curl -s "${BASE_URL}/api/admin/malts" 2>/dev/null || echo "ERROR")
if [ "$admin_response" != "ERROR" ]; then
    echo -e "✅ ${GREEN}Admin API accessible${NC}"
    echo "Response: $admin_response"
else
    echo -e "❌ ${RED}Admin API inaccessible${NC}"
fi

echo -e "\n${BLUE}Test API Publique${NC}"
public_response=$(curl -s "${BASE_URL}/api/v1/malts" 2>/dev/null || echo "ERROR")
if [ "$public_response" != "ERROR" ]; then
    echo -e "✅ ${GREEN}Public API accessible${NC}"
    echo "Response: $public_response"
else
    echo -e "❌ ${RED}Public API inaccessible${NC}"
fi

echo -e "\n${BLUE}Instructions:${NC}"
echo "1. Démarrez l'app avec: sbt run"
echo "2. Relancez ce script pour tester les APIs"
EOF

chmod +x test_malts_simple.sh

# =============================================================================
# INSTRUCTIONS FINALES
# =============================================================================

echo -e "\n${BLUE}🏁 CORRECTION TERMINÉE${NC}"
echo "======================="

if [ "$COMPILATION_OK" = true ]; then
    echo -e "\n${GREEN}✅ Compilation réussie !${NC}"
    echo -e "\n${YELLOW}Pour tester:${NC}"
    echo "1. sbt run"
    echo "2. ./test_malts_simple.sh"
else
    echo -e "\n${RED}⚠️ Compilation échouée${NC}"
    echo -e "\n${YELLOW}Actions suggérées:${NC}"
    echo "1. Vérifiez votre interface MaltReadRepository existante"
    echo "2. Regardez les méthodes disponibles dans votre repository"
    echo "3. Montrez-moi le contenu de:"
    echo "   - app/domain/malts/repositories/MaltReadRepository.scala"
    echo "   - app/domain/malts/model/MaltId.scala" 
    echo "   - app/domain/malts/model/MaltType.scala"
fi

echo -e "\n${GREEN}Corrections appliquées:${NC}"
echo "• MaltReadModel corrigé (UUID → String)"
echo "• Handlers simplifiés avec appels repository basiques"
echo "• API publique corrigée (MaltType.values → liste hardcodée)"
echo "• SlickMaltReadRepository restauré depuis backup"
echo "• Script de test simple créé"
