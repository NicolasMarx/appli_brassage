#!/bin/bash

# =============================================================================
# SCRIPT CRÉATION DOMAINE MALTS - Structure complète
# =============================================================================
# Crée tous les dossiers et fichiers vides pour le domaine Malts
# Suit les patterns du domaine Hops pour cohérence architecturale
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🌾 =============================================================================="
echo "   CRÉATION DOMAINE MALTS - Structure DDD/CQRS complète"
echo "==============================================================================${NC}"

# =============================================================================
# ÉTAPE 1 : CRÉATION BRANCHE GIT
# =============================================================================

echo ""
echo -e "${YELLOW}📋 ÉTAPE 1 : Création branche Git${NC}"

CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "master")
NEW_BRANCH="feature/malts-domain"

echo "Branche actuelle : ${CURRENT_BRANCH}"
echo "Nouvelle branche : ${NEW_BRANCH}"

# Vérifier si branche existe déjà
if git show-ref --verify --quiet refs/heads/$NEW_BRANCH; then
    echo -e "${YELLOW}⚠️  Branche ${NEW_BRANCH} existe déjà${NC}"
    echo -e "${RED}Voulez-vous la supprimer et recréer ? (y/n)${NC}"
    read -r RECREATE_BRANCH
    if [ "$RECREATE_BRANCH" = "y" ]; then
        git branch -D $NEW_BRANCH
        echo -e "${GREEN}✅ Branche supprimée${NC}"
    else
        echo -e "${RED}❌ Abandon - utilisez la branche existante${NC}"
        exit 1
    fi
fi

# Créer et basculer sur nouvelle branche
git checkout -b $NEW_BRANCH
echo -e "${GREEN}✅ Branche ${NEW_BRANCH} créée et active${NC}"

# =============================================================================
# ÉTAPE 2 : CRÉATION STRUCTURE DOSSIERS
# =============================================================================

echo ""
echo -e "${YELLOW}📁 ÉTAPE 2 : Création structure dossiers${NC}"

# Fonction pour créer dossiers avec affichage
create_directory() {
    local dir_path="$1"
    mkdir -p "$dir_path"
    echo -e "   ${GREEN}✅${NC} $dir_path"
}

echo "Création dossiers domaine..."
create_directory "app/domain/malts/model"
create_directory "app/domain/malts/services"
create_directory "app/domain/malts/repositories"

echo "Création dossiers application..."
create_directory "app/application/commands/admin/malts/handlers"
create_directory "app/application/queries/public/malts/readmodels"
create_directory "app/application/queries/public/malts/handlers"
create_directory "app/application/queries/admin/malts/handlers"

echo "Création dossiers infrastructure..."
create_directory "app/infrastructure/persistence/slick/repositories/malts"

echo "Création dossiers interfaces..."
create_directory "app/interfaces/http/api/v1/malts"
create_directory "app/interfaces/http/api/admin/malts"
create_directory "app/interfaces/http/dto/requests/malts"
create_directory "app/interfaces/http/dto/responses/malts"

echo "Création dossiers tests..."
create_directory "test/domain/malts"
create_directory "test/application/commands/malts"
create_directory "test/application/queries/malts"
create_directory "test/infrastructure/persistence/malts"
create_directory "test/interfaces/http/malts"

# =============================================================================
# ÉTAPE 3 : CRÉATION FICHIERS AVEC HEADERS
# =============================================================================

echo ""
echo -e "${YELLOW}📄 ÉTAPE 3 : Création fichiers avec headers${NC}"

# Fonction pour créer un fichier avec header approprié
create_file() {
    local file_path="$1"
    local description="$2"
    local package_name="$3"
    
    case "${file_path##*.}" in
        scala)
            cat > "$file_path" << EOF
// $description
package $package_name

// TODO: Implémenter selon l'architecture DDD/CQRS
// Pattern: Suivre l'exemple du domaine Hops
// Références: HopAggregate, HopId, etc.

EOF
            ;;
        sql)
            cat > "$file_path" << EOF
-- $description
-- Pattern: Suivre l'évolution des Hops
-- TODO: Définir le schéma Malts selon DDD

EOF
            ;;
        *)
            echo "// $description" > "$file_path"
            echo "// TODO: Implémenter" >> "$file_path"
            ;;
    esac
    
    echo -e "   ${GREEN}✅${NC} $file_path"
}

echo "Création fichiers domaine..."
create_file "app/domain/malts/model/MaltId.scala" "Value Object MaltId - Identifiant unique malts" "domain.malts.model"
create_file "app/domain/malts/model/MaltAggregate.scala" "Agrégat Malt principal - Racine d'agrégat" "domain.malts.model"
create_file "app/domain/malts/model/MaltType.scala" "Énumération types malts (BASE, CRYSTAL, ROASTED, SPECIALTY)" "domain.malts.model"
create_file "app/domain/malts/model/EBCColor.scala" "Value Object couleur EBC (0-1500+)" "domain.malts.model"
create_file "app/domain/malts/model/ExtractionRate.scala" "Value Object taux extraction (65-85%)" "domain.malts.model"
create_file "app/domain/malts/model/DiastaticPower.scala" "Value Object pouvoir diastasique" "domain.malts.model"
create_file "app/domain/malts/model/MaltStatus.scala" "Énumération statuts malts" "domain.malts.model"
create_file "app/domain/malts/model/MaltSource.scala" "Énumération sources malts (MANUAL, AI_DISCOVERED, IMPORT)" "domain.malts.model"
create_file "app/domain/malts/model/MaltCredibility.scala" "Value Object score crédibilité" "domain.malts.model"
create_file "app/domain/malts/model/MaltEvents.scala" "Domain Events pour agrégat Malt" "domain.malts.model"

echo "Création services domaine..."
create_file "app/domain/malts/services/MaltDomainService.scala" "Services domaine Malt" "domain.malts.services"

echo "Création repositories domaine..."
create_file "app/domain/malts/repositories/MaltReadRepository.scala" "Repository lecture Malt (interface)" "domain.malts.repositories"
create_file "app/domain/malts/repositories/MaltWriteRepository.scala" "Repository écriture Malt (interface)" "domain.malts.repositories"

echo "Création commandes application..."
create_file "app/application/commands/admin/malts/CreateMaltCommand.scala" "Commande création malt" "application.commands.admin.malts"
create_file "app/application/commands/admin/malts/UpdateMaltCommand.scala" "Commande mise à jour malt" "application.commands.admin.malts"
create_file "app/application/commands/admin/malts/DeleteMaltCommand.scala" "Commande suppression malt" "application.commands.admin.malts"
create_file "app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala" "Handler création malt" "application.commands.admin.malts.handlers"
create_file "app/application/commands/admin/malts/handlers/UpdateMaltCommandHandler.scala" "Handler mise à jour malt" "application.commands.admin.malts.handlers"
create_file "app/application/commands/admin/malts/handlers/DeleteMaltCommandHandler.scala" "Handler suppression malt" "application.commands.admin.malts.handlers"

echo "Création queries application..."
create_file "app/application/queries/public/malts/MaltListQuery.scala" "Query liste malts publique" "application.queries.public.malts"
create_file "app/application/queries/public/malts/MaltDetailQuery.scala" "Query détail malt publique" "application.queries.public.malts"
create_file "app/application/queries/public/malts/MaltSearchQuery.scala" "Query recherche malts publique" "application.queries.public.malts"
create_file "app/application/queries/public/malts/readmodels/MaltReadModel.scala" "ReadModel malt pour API publique" "application.queries.public.malts.readmodels"
create_file "app/application/queries/public/malts/handlers/MaltListQueryHandler.scala" "Handler liste malts" "application.queries.public.malts.handlers"
create_file "app/application/queries/public/malts/handlers/MaltDetailQueryHandler.scala" "Handler détail malt" "application.queries.public.malts.handlers"
create_file "app/application/queries/public/malts/handlers/MaltSearchQueryHandler.scala" "Handler recherche malts" "application.queries.public.malts.handlers"

create_file "app/application/queries/admin/malts/AdminMaltListQuery.scala" "Query liste malts admin" "application.queries.admin.malts"
create_file "app/application/queries/admin/malts/AdminMaltDetailQuery.scala" "Query détail malt admin" "application.queries.admin.malts"
create_file "app/application/queries/admin/malts/handlers/AdminMaltListQueryHandler.scala" "Handler liste malts admin" "application.queries.admin.malts.handlers"
create_file "app/application/queries/admin/malts/handlers/AdminMaltDetailQueryHandler.scala" "Handler détail malt admin" "application.queries.admin.malts.handlers"

echo "Création infrastructure persistence..."
create_file "app/infrastructure/persistence/slick/tables/MaltTables.scala" "Définitions tables Slick pour malts" "infrastructure.persistence.slick.tables"
create_file "app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala" "Repository lecture Slick malts" "infrastructure.persistence.slick.repositories.malts"
create_file "app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala" "Repository écriture Slick malts" "infrastructure.persistence.slick.repositories.malts"

echo "Création interfaces HTTP..."
create_file "app/interfaces/http/api/v1/malts/MaltsController.scala" "Contrôleur API publique malts" "interfaces.http.api.v1.malts"
create_file "app/interfaces/http/api/admin/malts/AdminMaltsController.scala" "Contrôleur API admin malts" "interfaces.http.api.admin.malts"

echo "Création DTOs..."
create_file "app/interfaces/http/dto/requests/malts/CreateMaltRequest.scala" "DTO création malt" "interfaces.http.dto.requests.malts"
create_file "app/interfaces/http/dto/requests/malts/UpdateMaltRequest.scala" "DTO mise à jour malt" "interfaces.http.dto.requests.malts"
create_file "app/interfaces/http/dto/requests/malts/MaltSearchRequest.scala" "DTO recherche malts" "interfaces.http.dto.requests.malts"
create_file "app/interfaces/http/dto/responses/malts/MaltResponse.scala" "DTO réponse malt" "interfaces.http.dto.responses.malts"
create_file "app/interfaces/http/dto/responses/malts/MaltListResponse.scala" "DTO liste malts" "interfaces.http.dto.responses.malts"

echo "Création évolution base de données..."
create_file "conf/evolutions/default/10.sql" "Évolution base de données - Tables malts" ""

echo "Création tests..."
create_file "test/domain/malts/MaltAggregateSpec.scala" "Tests agrégat Malt" "domain.malts"
create_file "test/application/commands/malts/CreateMaltCommandHandlerSpec.scala" "Tests handler création malt" "application.commands.malts"
create_file "test/application/queries/malts/MaltListQueryHandlerSpec.scala" "Tests handler liste malts" "application.queries.malts"
create_file "test/infrastructure/persistence/malts/SlickMaltRepositorySpec.scala" "Tests repository Slick malts" "infrastructure.persistence.malts"
create_file "test/interfaces/http/malts/MaltsControllerSpec.scala" "Tests contrôleur public malts" "interfaces.http.malts"
create_file "test/interfaces/http/malts/AdminMaltsControllerSpec.scala" "Tests contrôleur admin malts" "interfaces.http.malts"

# =============================================================================
# ÉTAPE 4 : RÉSUMÉ ET PROCHAINES ACTIONS
# =============================================================================

echo ""
echo -e "${BLUE}📊 RÉSUMÉ CRÉATION DOMAINE MALTS${NC}"
echo ""
echo -e "${GREEN}✅ Branche créée :${NC} $NEW_BRANCH"
echo -e "${GREEN}✅ Dossiers créés :${NC} 15 dossiers"
echo -e "${GREEN}✅ Fichiers créés :${NC} 38 fichiers"
echo ""

echo -e "${YELLOW}🚀 PROCHAINES ACTIONS :${NC}"
echo ""
echo "1. 📝 Implémenter Value Objects :"
echo "   - MaltId.scala (UUID + validation)"
echo "   - EBCColor.scala (0-1500+ EBC)" 
echo "   - ExtractionRate.scala (65-85%)"
echo "   - DiastaticPower.scala (Lintner)"
echo ""
echo "2. 🏗️  Créer MaltAggregate :"
echo "   - Logique métier + Event Sourcing"
echo "   - Business rules validation"
echo ""
echo "3. 🗄️  Base de données :"
echo "   - Évolution 10.sql (tables + indexes)"
echo "   - Repositories Slick"
echo ""
echo "4. 🚀 API Layer :"
echo "   - Commands + Queries handlers"
echo "   - Controllers Admin + Public"
echo ""
echo -e "${BLUE}💡 Conseil : Commencer par MaltId.scala puis EBCColor.scala${NC}"
echo ""
echo -e "${GREEN}🎯 Structure domaine Malts prête pour implémentation !${NC}"

# Vérification finale
echo ""
echo -e "${BLUE}🔍 Vérification structure créée :${NC}"
find app/domain/malts -type f -name "*.scala" | wc -l | xargs echo "   Domain files:"
find app/application -path "*/malts/*" -name "*.scala" | wc -l | xargs echo "   Application files:"
find app/infrastructure -path "*/malts/*" -name "*.scala" | wc -l | xargs echo "   Infrastructure files:"
find app/interfaces -path "*/malts/*" -name "*.scala" | wc -l | xargs echo "   Interface files:"
find test -path "*/malts/*" -name "*.scala" | wc -l | xargs echo "   Test files:"

echo ""
echo -e "${GREEN}✅ Domaine Malts prêt pour le développement !${NC}"