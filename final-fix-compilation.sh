#!/bin/bash

# =============================================================================
# CORRECTION FINALE - 3 ERREURS RESTANTES
# =============================================================================
# Corrige les erreurs de signature des méthodes
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTION FINALE - 3 ERREURS${NC}"
echo -e "${BLUE}================================${NC}"

# =============================================================================
# CORRECTION 1 : MaltDetailQueryHandler - MaltId n'a pas fromString
# =============================================================================

echo -e "\n${YELLOW}🔧 Correction 1 : MaltDetailQueryHandler${NC}"

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
    
    // Création MaltId avec gestion d'erreur
    MaltId(query.maltId) match {
      case Right(maltId) =>
        println(s"✅ MaltId validé: ${maltId}")
        
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
        
      case Left(error) =>
        println(s"❌ MaltId invalide: $error")
        Future.successful(Failure(new IllegalArgumentException(error)))
    }
  }
}
EOF

echo -e "✅ ${GREEN}MaltDetailQueryHandler corrigé${NC}"

# =============================================================================
# CORRECTION 2 : MaltListQueryHandler - findAll a des paramètres obligatoires
# =============================================================================

echo -e "\n${YELLOW}🔧 Correction 2 : MaltListQueryHandler${NC}"

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
    
    // Appel avec les paramètres requis : page, pageSize, activeOnly
    maltReadRepository.findAll(
      page = query.page,
      pageSize = query.size,
      activeOnly = true  // On veut seulement les malts actifs pour l'API publique
    ).map { malts =>
      try {
        println(s"📊 Repository retourné: ${malts.length} malts")
        
        val maltReadModels = malts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = malts.length.toLong, // Approximation - pas de count séparé
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

echo -e "✅ ${GREEN}MaltListQueryHandler corrigé${NC}"

# =============================================================================
# CORRECTION 3 : MaltSearchQueryHandler - findAll a des paramètres obligatoires
# =============================================================================

echo -e "\n${YELLOW}🔧 Correction 3 : MaltSearchQueryHandler${NC}"

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
    
    // Récupère tous les malts actifs avec pagination large pour le filtrage
    maltReadRepository.findAll(
      page = 0,
      pageSize = 1000, // Grande page pour récupérer plus de malts à filtrer
      activeOnly = true
    ).map { allMalts =>
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
        
        // Pagination manuelle
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

echo -e "✅ ${GREEN}MaltSearchQueryHandler corrigé${NC}"

# =============================================================================
# TEST DE COMPILATION FINAL
# =============================================================================

echo -e "\n${YELLOW}⚙️  TEST DE COMPILATION FINAL${NC}"

echo "Compilation du projet..."
if sbt compile; then
    echo -e "✅ ${GREEN}🎉 COMPILATION RÉUSSIE !${NC}"
    
    echo -e "\n${GREEN}✅ Toutes les erreurs corrigées :${NC}"
    echo "• MaltId(query.maltId) au lieu de MaltId.fromString()"
    echo "• findAll(page, pageSize, activeOnly) avec paramètres requis"
    echo "• Pagination et filtrage manuels dans les handlers"
    
    echo -e "\n${YELLOW}🚀 Pour tester maintenant :${NC}"
    echo "1. sbt run"
    echo "2. ./test_malts_simple.sh"
    
    echo -e "\n${BLUE}📋 APIs disponibles :${NC}"
    echo "• GET /api/v1/malts                (liste publique)"
    echo "• GET /api/v1/malts/:id           (détail)"
    echo "• POST /api/v1/malts/search       (recherche)"
    echo "• GET /api/v1/malts/type/:type    (par type)"
    echo "• GET /api/admin/malts             (admin)"
    
else
    echo -e "❌ ${RED}Erreurs de compilation persistantes${NC}"
    echo "Veuillez montrer les nouvelles erreurs pour continuer."
fi
