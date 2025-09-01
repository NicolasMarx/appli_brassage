#!/bin/bash

# =============================================================================
# SCRIPT FINAL - COMPLÉTION APIs MALTS 
# =============================================================================
# Complète uniquement ce qui manque sur votre branche feature/malts-domain
# Basé sur l'audit de votre structure existante
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🎯 COMPLÉTION FINALE APIs MALTS${NC}"
echo -e "${BLUE}==============================${NC}"

# =============================================================================
# ÉTAPE 1 : CRÉATION DES QUERIES PUBLIQUES MANQUANTES
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 1 : Création des Queries publiques${NC}"

# Créer les dossiers manquants
mkdir -p app/application/queries/public/malts/{handlers,readmodels}

# MaltListQuery
cat > app/application/queries/public/malts/MaltListQuery.scala << 'EOF'
package application.queries.public.malts

import domain.malts.model.MaltType

case class MaltListQuery(
  maltType: Option[MaltType] = None,
  page: Int = 0,
  size: Int = 20
)
EOF

# MaltDetailQuery
cat > app/application/queries/public/malts/MaltDetailQuery.scala << 'EOF'
package application.queries.public.malts

case class MaltDetailQuery(
  maltId: String
)
EOF

# MaltSearchQuery
cat > app/application/queries/public/malts/MaltSearchQuery.scala << 'EOF'
package application.queries.public.malts

import domain.malts.model.MaltType

case class MaltSearchQuery(
  name: Option[String] = None,
  maltType: Option[MaltType] = None,
  minEbc: Option[Double] = None,
  maxEbc: Option[Double] = None,
  minExtraction: Option[Double] = None,
  maxExtraction: Option[Double] = None,
  page: Int = 0,
  size: Int = 20
)
EOF

echo -e "✅ ${GREEN}Queries créées${NC}"

# =============================================================================
# ÉTAPE 2 : CRÉATION DES READMODELS
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 2 : Création des ReadModels${NC}"

# MaltReadModel
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
      id = malt.id.value,
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

echo -e "✅ ${GREEN}ReadModels créés${NC}"

# =============================================================================
# ÉTAPE 3 : CRÉATION DES HANDLERS PUBLIQUES
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 3 : Création des Handlers publiques${NC}"

# MaltListQueryHandler
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
    println(s"🔍 MaltListQueryHandler - page: ${query.page}, size: ${query.size}, type: ${query.maltType.map(_.name)}")
    
    val repositoryCall = query.maltType match {
      case Some(maltType) => 
        println(s"🔍 Recherche par type: ${maltType.name}")
        maltReadRepository.findByType(maltType, query.page, query.size)
      case None => 
        println(s"🔍 Recherche tous malts actifs")
        maltReadRepository.findActive(query.page, query.size)
    }
    
    repositoryCall.map { case (malts, totalCount) =>
      try {
        println(s"📊 Repository retourné: ${malts.length} malts, total: $totalCount")
        val maltReadModels = malts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = totalCount,
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

# MaltDetailQueryHandler
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
    
    MaltId(query.maltId) match {
      case Right(maltId) =>
        println(s"🔍 MaltId validé, recherche en cours...")
        maltReadRepository.byId(maltId).map { maltOpt =>
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
              ex.printStackTrace()
              Failure(ex)
          }
        }.recover {
          case ex: Exception => 
            println(s"❌ Erreur repository: ${ex.getMessage}")
            ex.printStackTrace()
            Failure(ex)
        }
      
      case Left(error) =>
        println(s"❌ MaltId invalide: $error")
        Future.successful(Failure(new IllegalArgumentException(error)))
    }
  }
}
EOF

# MaltSearchQueryHandler
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
    println(s"🔍 MaltSearchQueryHandler - name: ${query.name}, type: ${query.maltType.map(_.name)}")
    
    maltReadRepository.search(
      name = query.name,
      maltType = query.maltType,
      minEbc = query.minEbc,
      maxEbc = query.maxEbc,
      minExtraction = query.minExtraction,
      maxExtraction = query.maxExtraction,
      page = query.page,
      size = query.size
    ).map { case (malts, totalCount) =>
      try {
        println(s"📊 Recherche retourné: ${malts.length} malts, total: $totalCount")
        val maltReadModels = malts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = totalCount,
          page = query.page,
          size = query.size
        )
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans MaltSearchQueryHandler: ${ex.getMessage}")
          ex.printStackTrace()
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans MaltSearchQueryHandler: ${ex.getMessage}")
        ex.printStackTrace()
        Failure(ex)
    }
  }
}
EOF

echo -e "✅ ${GREEN}Handlers publiques créés${NC}"

# =============================================================================
# ÉTAPE 4 : AMÉLIORATION DE L'API PUBLIQUE EXISTANTE
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 4 : Amélioration de l'API publique${NC}"

# Remplacer le MaltsController existant par une version complète
cat > app/interfaces/http/api/v1/malts/MaltsController.scala << 'EOF'
package interfaces.http.api.v1.malts

import application.queries.public.malts._
import application.queries.public.malts.handlers._
import application.queries.public.malts.readmodels.MaltReadModel
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

  // ===============================
  // LISTE DES MALTS (PUBLIQUE)
  // ===============================
  
  def list(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts - page: $page, size: $size")
    
    val query = MaltListQuery(page = page, size = size)
    
    maltListQueryHandler.handle(query).map { result =>
      result match {
        case Success(response) =>
          println(s"✅ API publique - Malts récupérés: ${response.malts.length}/${response.totalCount}")
          Ok(Json.toJson(response))
        case Failure(ex) =>
          println(s"❌ Erreur API publique lors de la récupération des malts: ${ex.getMessage}")
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite",
            "details" -> ex.getMessage
          ))
      }
    }.recover {
      case ex =>
        println(s"❌ Erreur critique dans list API publique: ${ex.getMessage}")
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "critical_error",
          "message" -> "Erreur critique du serveur"
        ))
    }
  }

  // ===============================
  // DÉTAIL D'UN MALT
  // ===============================
  
  def detail(id: String): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts/$id")
    
    val query = MaltDetailQuery(maltId = id)
    
    maltDetailQueryHandler.handle(query).map { result =>
      result match {
        case Success(Some(malt)) =>
          println(s"✅ API publique - Malt trouvé: ${malt.name}")
          Ok(Json.toJson(malt))
        case Success(None) =>
          println(s"⚠️ API publique - Malt non trouvé: $id")
          NotFound(Json.obj(
            "error" -> "malt_not_found",
            "message" -> s"Malt avec l'ID '$id' non trouvé"
          ))
        case Failure(ex) =>
          println(s"❌ Erreur API publique lors de la récupération du malt $id: ${ex.getMessage}")
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite",
            "details" -> ex.getMessage
          ))
      }
    }.recover {
      case ex =>
        println(s"❌ Erreur critique dans detail API publique: ${ex.getMessage}")
        BadRequest(Json.obj(
          "error" -> "invalid_id",
          "message" -> "ID de malt invalide"
        ))
    }
  }

  // ===============================
  // RECHERCHE AVANCÉE DE MALTS
  // ===============================
  
  def search(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    println(s"🌾 POST /api/v1/malts/search")
    println(s"🔍 Body reçu: ${Json.prettyPrint(request.body)}")
    
    request.body.validate[MaltSearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        println(s"✅ Requête de recherche validée: ${searchRequest}")
        
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
              println(s"✅ API publique - Recherche réussie: ${response.malts.length} malts trouvés")
              Ok(Json.toJson(response))
            case Failure(ex) =>
              println(s"❌ Erreur API publique lors de la recherche: ${ex.getMessage}")
              ex.printStackTrace()
              InternalServerError(Json.obj(
                "error" -> "search_error",
                "message" -> "Erreur lors de la recherche",
                "details" -> ex.getMessage
              ))
          }
        }

      case JsError(errors) =>
        println(s"❌ Validation JSON échouée: $errors")
        Future.successful(BadRequest(Json.obj(
          "error" -> "validation_error",
          "message" -> "Format de requête invalide",
          "details" -> JsError.toJson(errors)
        )))
    }
  }

  // ===============================
  // MALTS PAR TYPE
  // ===============================
  
  def byType(maltType: String, page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts/type/$maltType - page: $page, size: $size")
    
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
              println(s"✅ API publique - Malts de type $maltType récupérés: ${response.malts.length}")
              Ok(Json.toJson(response))
            case Failure(ex) =>
              println(s"❌ Erreur API publique lors de la récupération des malts de type $maltType: ${ex.getMessage}")
              InternalServerError(Json.obj(
                "error" -> "internal_error",
                "message" -> "Une erreur interne s'est produite"
              ))
          }
        }
        
      case None =>
        println(s"❌ Type de malt invalide: $maltType")
        Future.successful(BadRequest(Json.obj(
          "error" -> "invalid_type",
          "message" -> s"Type de malt '$maltType' invalide",
          "validTypes" -> MaltType.values.map(_.name)
        )))
    }
  }
}

// ===============================
// DTO POUR RECHERCHE
// ===============================

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

echo -e "✅ ${GREEN}API publique Malts complétée${NC}"

# =============================================================================
# ÉTAPE 5 : AJOUT DES ROUTES DANS conf/routes
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 5 : Ajout des routes APIs Malts${NC}"

# Vérifier si les routes malts existent déjà
if ! grep -q "api/v1/malts" conf/routes 2>/dev/null; then
    echo -e "\n# ==============================================================================" >> conf/routes
    echo "# API MALTS - INTERFACE PUBLIQUE" >> conf/routes
    echo "# ==============================================================================" >> conf/routes
    echo "" >> conf/routes
    echo "# Liste des malts (publique)" >> conf/routes
    echo "GET     /api/v1/malts                   interfaces.http.api.v1.malts.MaltsController.list(page: Int ?= 0, size: Int ?= 20)" >> conf/routes
    echo "GET     /api/v1/malts/:id               interfaces.http.api.v1.malts.MaltsController.detail(id: String)" >> conf/routes
    echo "POST    /api/v1/malts/search            interfaces.http.api.v1.malts.MaltsController.search()" >> conf/routes
    echo "GET     /api/v1/malts/type/:maltType    interfaces.http.api.v1.malts.MaltsController.byType(maltType: String, page: Int ?= 0, size: Int ?= 20)" >> conf/routes
    echo "" >> conf/routes
    
    echo -e "✅ ${GREEN}Routes API publique Malts ajoutées${NC}"
else
    echo -e "✅ ${GREEN}Routes API Malts déjà présentes${NC}"
fi

# =============================================================================
# ÉTAPE 6 : CRÉATION D'UN SCRIPT DE TEST
# =============================================================================

echo -e "\n${YELLOW}🧪 ÉTAPE 6 : Création du script de test${NC}"

cat > test_malts_apis.sh << 'EOF'
#!/bin/bash

# Script de test des APIs Malts complètes
BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🌾 TEST COMPLET APIs MALTS${NC}"
echo "=============================="

# Test 1 : API Admin - Liste
echo -e "\n${BLUE}1. Test API Admin - Liste${NC}"
admin_response=$(curl -s "${BASE_URL}/api/admin/malts" || echo "ERROR")
echo "Admin Response: $admin_response"

if [ "$admin_response" != "ERROR" ] && echo "$admin_response" | jq . >/dev/null 2>&1; then
    admin_count=$(echo "$admin_response" | jq -r '.totalCount // 0')
    admin_length=$(echo "$admin_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ API Admin OK - $admin_length/$admin_count malts${NC}"
else
    echo -e "${RED}❌ API Admin KO${NC}"
fi

# Test 2 : API Publique - Liste
echo -e "\n${BLUE}2. Test API Publique - Liste${NC}"
public_response=$(curl -s "${BASE_URL}/api/v1/malts" || echo "ERROR")
echo "Public Response: $public_response"

if [ "$public_response" != "ERROR" ] && echo "$public_response" | jq . >/dev/null 2>&1; then
    public_count=$(echo "$public_response" | jq -r '.totalCount // 0')
    public_length=$(echo "$public_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ API Publique OK - $public_length/$public_count malts${NC}"
else
    echo -e "${RED}❌ API Publique KO${NC}"
fi

# Test 3 : Recherche Avancée
echo -e "\n${BLUE}3. Test Recherche Avancée${NC}"
search_data='{
  "name": "malt",
  "minEbc": 0,
  "maxEbc": 50,
  "page": 0,
  "size": 10
}'

search_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$search_data" \
  "${BASE_URL}/api/v1/malts/search" || echo "ERROR")

echo "Search Response: $search_response"

if [ "$search_response" != "ERROR" ] && echo "$search_response" | jq . >/dev/null 2>&1; then
    search_length=$(echo "$search_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ Recherche OK - $search_length résultats${NC}"
else
    echo -e "${RED}❌ Recherche KO${NC}"
fi

# Test 4 : Malts par type
echo -e "\n${BLUE}4. Test Malts par Type BASE${NC}"
type_response=$(curl -s "${BASE_URL}/api/v1/malts/type/BASE" || echo "ERROR")
echo "Type BASE Response: $type_response"

if [ "$type_response" != "ERROR" ] && echo "$type_response" | jq . >/dev/null 2>&1; then
    type_length=$(echo "$type_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ Type BASE OK - $type_length malts${NC}"
else
    echo -e "${RED}❌ Type BASE KO${NC}"
fi

# Test 5 : Détail d'un malt (si on en a trouvé un)
if [ "$public_response" != "ERROR" ] && echo "$public_response" | jq . >/dev/null 2>&1; then
    first_malt_id=$(echo "$public_response" | jq -r '.malts[0].id // empty')
    if [ -n "$first_malt_id" ] && [ "$first_malt_id" != "null" ]; then
        echo -e "\n${BLUE}5. Test Détail Malt - ID: $first_malt_id${NC}"
        detail_response=$(curl -s "${BASE_URL}/api/v1/malts/$first_malt_id" || echo "ERROR")
        echo "Detail Response: $detail_response"
        
        if [ "$detail_response" != "ERROR" ] && echo "$detail_response" | jq . >/dev/null 2>&1; then
            malt_name=$(echo "$detail_response" | jq -r '.name // "Unknown"')
            echo -e "${GREEN}✅ Détail OK - Malt: $malt_name${NC}"
        else
            echo -e "${RED}❌ Détail KO${NC}"
        fi
    else
        echo -e "\n${YELLOW}⚠️ Aucun malt trouvé pour test détail${NC}"
    fi
fi

echo -e "\n${BLUE}Tests terminés${NC}"
echo "=================="

# Résumé
echo -e "\n${YELLOW}📋 Résumé:${NC}"
echo -e "• API Admin:    $([ "$admin_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"
echo -e "• API Publique: $([ "$public_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"
echo -e "• Recherche:    $([ "$search_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"
echo -e "• Type BASE:    $([ "$type_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"

if [ "$admin_response" != "ERROR" ] && [ "$public_response" != "ERROR" ]; then
    echo -e "\n${GREEN}🎉 Domaine Malts fonctionnel !${NC}"
else
    echo -e "\n${RED}⚠️ Vérifiez les logs SBT pour plus de détails${NC}"
fi
EOF

chmod +x test_malts_apis.sh

echo -e "✅ ${GREEN}Script de test créé: ./test_malts_apis.sh${NC}"

# =============================================================================
# ÉTAPE 7 : TEST DE COMPILATION
# =============================================================================

echo -e "\n${YELLOW}⚙️  ÉTAPE 7 : Test de compilation${NC}"

echo "Compilation du projet..."
if sbt compile; then
    echo -e "✅ ${GREEN}Compilation réussie !${NC}"
else
    echo -e "❌ ${RED}Erreurs de compilation${NC}"
    echo "Vérifiez les imports et dépendances manquantes."
fi

# =============================================================================
# INSTRUCTIONS FINALES
# =============================================================================

echo -e "\n${BLUE}🎉 COMPLÉTION TERMINÉE !${NC}"
echo "========================="

echo -e "\n${GREEN}✅ Réalisations:${NC}"
echo "• Queries publiques créées (MaltListQuery, MaltDetailQuery, MaltSearchQuery)"
echo "• ReadModels publiques créés (MaltReadModel, MaltListResponse)"
echo "• Handlers publiques créés (avec debug détaillé)"
echo "• API publique complétée (MaltsController)"
echo "• Routes ajoutées dans conf/routes"
echo "• Script de test généré (test_malts_apis.sh)"

echo -e "\n${YELLOW}📋 Pour tester:${NC}"
echo "1. Démarrer l'application: sbt run"
echo "2. Dans un autre terminal: ./test_malts_apis.sh"
echo "3. Vérifier les logs de debug dans la console SBT"

echo -e "\n${YELLOW}🔍 URLs disponibles:${NC}"
echo "• GET    /api/v1/malts                    (liste publique)"
echo "• GET    /api/v1/malts/:id               (détail public)"
echo "• POST   /api/v1/malts/search            (recherche avancée)"
echo "• GET    /api/v1/malts/type/:type        (malts par type)"
echo "• GET    /api/admin/malts                (liste admin)"

echo -e "\n${GREEN}Le domaine Malts est maintenant complet avec APIs publiques et admin !${NC}"
echo -e "${BLUE}Si le SlickMaltReadRepository était corrigé précédemment, tout devrait fonctionner.${NC}"
