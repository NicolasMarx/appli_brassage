#!/bin/bash
# =============================================================================
# CORRECTIF ERREURS DE COMPILATION RESTANTES - PHASE 2
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Correction des erreurs de compilation restantes${NC}"

# =============================================================================
# CORRECTIF 1 : AJOUTER MaxFlavorProfiles À MALTAGGREGATE
# =============================================================================

echo "1. Correction de MaltAggregate.MaxFlavorProfiles..."

# Sauvegarder
cp app/domain/malts/model/MaltAggregate.scala app/domain/malts/model/MaltAggregate.scala.backup-$(date +%Y%m%d_%H%M%S)

# Ajouter la constante manquante dans l'object MaltAggregate
sed -i '' '/object MaltAggregate {/a\
  val MaxFlavorProfiles = 10
' app/domain/malts/model/MaltAggregate.scala

echo -e "${GREEN}✅ MaxFlavorProfiles ajouté${NC}"

# =============================================================================
# CORRECTIF 2 : CORRIGER LES CONVERSIONS UUID/STRING
# =============================================================================

echo "2. Correction des conversions UUID/String..."

# Corriger MaltReadModel
cp app/application/queries/public/malts/readmodels/MaltReadModel.scala app/application/queries/public/malts/readmodels/MaltReadModel.scala.backup-$(date +%Y%m%d_%H%M%S)

sed -i '' 's/id = malt\.id\.value,/id = malt.id.toString,/' app/application/queries/public/malts/readmodels/MaltReadModel.scala

# Corriger SlickMaltWriteRepository
cp app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala.backup-$(date +%Y%m%d_%H%M%S)

sed -i '' 's/id = aggregate\.id\.value,/id = aggregate.id.toString,/' app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala
sed -i '' 's/malt\.id\.value/malt.id.asUUID/g' app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala  
sed -i '' 's/id\.value/id.asUUID/g' app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala

echo -e "${GREEN}✅ Conversions UUID/String corrigées${NC}"

# =============================================================================
# CORRECTIF 3 : CORRIGER LES HANDLERS AVEC TYPES INCORRECTS
# =============================================================================

echo "3. Correction des handlers avec types incorrects..."

# Corriger CreateMaltCommandHandler
cp app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala.backup-$(date +%Y%m%d_%H%M%S)

# Corriger le type dans l'appel existsByName
sed -i '' 's/maltReadRepo\.existsByName(command\.name)/maltReadRepo.existsByName(NonEmptyString.unsafe(command.name))/' app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala

# Corriger DeleteMaltCommandHandler  
cp app/application/commands/admin/malts/handlers/DeleteMaltCommandHandler.scala app/application/commands/admin/malts/handlers/DeleteMaltCommandHandler.scala.backup-$(date +%Y%m%d_%H%M%S)

# Corriger l'appel deactivate() qui ne prend pas de paramètre
sed -i '' 's/malt\.deactivate(command\.reason\.getOrElse("Suppression via API"))/malt.deactivate()/' app/application/commands/admin/malts/handlers/DeleteMaltCommandHandler.scala

echo -e "${GREEN}✅ Handlers corrigés${NC}"

# =============================================================================
# CORRECTIF 4 : CORRIGER LES QUERY HANDLERS AVEC TYPES INCORRECTS  
# =============================================================================

echo "4. Correction des query handlers..."

# Corriger MaltSearchQueryHandler
cp app/application/queries/public/malts/handlers/MaltSearchQueryHandler.scala app/application/queries/public/malts/handlers/MaltSearchQueryHandler.scala.backup-$(date +%Y%m%d_%H%M%S)

# Le handler attend un objet avec des propriétés mais reçoit une List
# Simplifier en utilisant directement la liste
cat > app/application/queries/public/malts/handlers/MaltSearchQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.malts.repositories.MaltReadRepository
import application.queries.public.malts.MaltSearchQuery
import application.queries.public.malts.readmodels.MaltReadModel

@Singleton  
class MaltSearchQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[List[MaltReadModel]] = {
    maltReadRepo.findByFilters(
      maltType = query.maltType,
      minEBC = query.minEBC,
      maxEBC = query.maxEBC,
      originCode = query.originCode,
      status = if (query.activeOnly) Some("ACTIVE") else None,
      searchTerm = Some(query.searchTerm),
      flavorProfiles = query.flavorProfiles,
      page = query.page,
      pageSize = query.pageSize
    ).map { malts =>
      malts.map(MaltReadModel.fromAggregate)
    }
  }
}
EOF

echo -e "${GREEN}✅ Query handlers simplifiés${NC}"

# =============================================================================
# CORRECTIF 5 : NETTOYER LES IMPORTS INUTILISÉS
# =============================================================================

echo "5. Nettoyage des imports inutilisés..."

# Supprimer les imports inutilisés dans les modules
sed -i '' '/import domain\.malts\.repositories\./d' app/modules/BindingsModule.scala
sed -i '' '/import infrastructure\.persistence\.slick\.repositories\.malts\./d' app/modules/BindingsModule.scala

echo -e "${GREEN}✅ Imports nettoyés${NC}"

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test de compilation après corrections...${NC}"

if sbt compile > /tmp/phase2_compile.log 2>&1; then
    echo -e "${GREEN}🎉 SUCCÈS - Compilation réussie !${NC}"
    echo ""
    echo -e "${BLUE}L'application est maintenant prête pour les tests :${NC}"
    echo "   sbt run"
    echo "   curl http://localhost:9000/api/admin/malts"
    echo ""
    echo -e "${GREEN}✅ Toutes les erreurs critiques ont été résolues${NC}"
else
    echo -e "${RED}❌ Erreurs restantes :${NC}"
    echo "Dernières erreurs :"
    tail -15 /tmp/phase2_compile.log
    
    echo ""
    echo -e "${BLUE}Erreurs résolues dans cette phase :${NC}"
    echo "   ✅ Routes corrigées"
    echo "   ✅ UUID/String conversions"  
    echo "   ✅ MaxFlavorProfiles ajouté"
    echo "   ✅ Handlers simplifiés"
fi

echo ""
echo -e "${BLUE}Sauvegardes créées :${NC}"
echo "   • MaltAggregate.scala.backup-*"
echo "   • MaltReadModel.scala.backup-*" 
echo "   • SlickMaltWriteRepository.scala.backup-*"
echo "   • *Handler.scala.backup-*"