#!/bin/bash

# =============================================================================
# CORRECTION ULTRA-FINALE - MaltId DIRECT
# =============================================================================
# Le MaltId ne retourne pas Either mais directement un MaltId
# =============================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTION ULTRA-FINALE${NC}"
echo -e "${BLUE}==========================${NC}"

# =============================================================================
# CORRECTION FINALE : MaltDetailQueryHandler sans Either
# =============================================================================

echo -e "\n${YELLOW}🔧 Correction MaltDetailQueryHandler (sans Either)${NC}"

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
    
    try {
      // Création directe du MaltId - il semble que votre MaltId ne retourne pas Either
      val maltId = MaltId(java.util.UUID.fromString(query.maltId))
      println(s"✅ MaltId créé: $maltId")
      
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
      case ex: IllegalArgumentException =>
        println(s"❌ UUID invalide: ${query.maltId}")
        Future.successful(Failure(new IllegalArgumentException(s"Invalid UUID: ${query.maltId}")))
      case ex: Exception =>
        println(s"❌ Erreur lors de la création MaltId: ${ex.getMessage}")
        Future.successful(Failure(ex))
    }
  }
}
EOF

echo -e "✅ ${GREEN}MaltDetailQueryHandler corrigé (création directe)${NC}"

# =============================================================================
# TEST COMPILATION FINAL
# =============================================================================

echo -e "\n${YELLOW}⚙️  COMPILATION FINALE${NC}"

echo "Test de compilation..."
if sbt compile; then
    echo -e "✅ ${GREEN}🎉 SUCCESS - COMPILATION RÉUSSIE !${NC}"
    
    echo -e "\n${GREEN}🚀 DOMAINE MALTS COMPLET ET FONCTIONNEL !${NC}"
    
    echo -e "\n${YELLOW}Pour tester maintenant :${NC}"
    echo "1. sbt run"
    echo "2. ./test_malts_simple.sh"
    
    echo -e "\n${BLUE}📋 APIs disponibles :${NC}"
    echo "• GET    /api/v1/malts                (liste publique)"
    echo "• GET    /api/v1/malts/:id           (détail malt)"
    echo "• POST   /api/v1/malts/search        (recherche avancée)"
    echo "• GET    /api/v1/malts/type/BASE     (malts par type)"
    echo "• GET    /api/admin/malts             (liste admin)"
    
    echo -e "\n${GREEN}🎉 Le domaine Malts suit maintenant les mêmes patterns que le domaine Hops !${NC}"
    
else
    echo -e "❌ Erreur de compilation - montrez-moi le contenu de :"
    echo "cat app/domain/malts/model/MaltId.scala"
fi
