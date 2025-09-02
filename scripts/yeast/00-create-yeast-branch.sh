#!/bin/bash
# =============================================================================
# SCRIPT : Création branche Git pour domaine Yeast
# OBJECTIF : Procédure Git complète pour nouveau domaine
# USAGE : ./scripts/yeast/00-create-yeast-branch.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

BRANCH_NAME="feature/yeast-domain"
BACKUP_BRANCH="backup-before-yeast-$(date +%Y%m%d_%H%M%S)"

echo -e "${BLUE}🍺 CRÉATION BRANCHE DOMAINE YEAST${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: VÉRIFICATION PRÉREQUIS
# =============================================================================

echo -e "${YELLOW}📋 Vérification prérequis...${NC}"

# Vérifier que nous sommes dans un repo Git
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo -e "${RED}❌ Erreur : Pas dans un repository Git${NC}"
    exit 1
fi

# Vérifier branche courante
CURRENT_BRANCH=$(git branch --show-current)
echo -e "${BLUE}📍 Branche courante : ${CURRENT_BRANCH}${NC}"

# Vérifier status Git propre
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}⚠️  Modifications non commitées détectées${NC}"
    echo -e "${YELLOW}   Voulez-vous continuer ? (y/N)${NC}"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo -e "${RED}❌ Opération annulée${NC}"
        exit 1
    fi
fi

# =============================================================================
# ÉTAPE 2: SAUVEGARDE SÉCURITÉ
# =============================================================================

echo -e "\n${YELLOW}💾 Création sauvegarde sécurité...${NC}"

# Créer branche de sauvegarde
git branch "$BACKUP_BRANCH"
echo -e "${GREEN}✅ Branche sauvegarde créée : ${BACKUP_BRANCH}${NC}"

# Créer sauvegarde locale
BACKUP_DIR="backups/yeast-implementation-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Sauvegarder fichiers critiques
cp -r app/domain/hops "$BACKUP_DIR/reference-hops/" 2>/dev/null || true
cp -r app/domain/malts "$BACKUP_DIR/reference-malts/" 2>/dev/null || true
cp -r conf/evolutions "$BACKUP_DIR/evolutions/" 2>/dev/null || true
cp conf/routes "$BACKUP_DIR/routes" 2>/dev/null || true

echo -e "${GREEN}✅ Sauvegarde locale : ${BACKUP_DIR}${NC}"

# =============================================================================
# ÉTAPE 3: CRÉATION BRANCHE FEATURE
# =============================================================================

echo -e "\n${YELLOW}🌿 Création branche feature...${NC}"

# Vérifier si la branche existe déjà
if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    echo -e "${YELLOW}⚠️  La branche $BRANCH_NAME existe déjà${NC}"
    echo -e "${YELLOW}   Voulez-vous la supprimer et recréer ? (y/N)${NC}"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        git branch -D "$BRANCH_NAME"
        echo -e "${GREEN}✅ Ancienne branche supprimée${NC}"
    else
        git checkout "$BRANCH_NAME"
        echo -e "${GREEN}✅ Basculé sur branche existante${NC}"
        exit 0
    fi
fi

# Créer et basculer sur nouvelle branche
git checkout -b "$BRANCH_NAME"
echo -e "${GREEN}✅ Branche créée et active : ${BRANCH_NAME}${NC}"

# =============================================================================
# ÉTAPE 4: PRÉPARATION ENVIRONNEMENT
# =============================================================================

echo -e "\n${YELLOW}🔧 Préparation environnement...${NC}"

# Créer structure dossiers yeast
mkdir -p app/domain/yeasts/{model,services,repositories}
mkdir -p app/application/yeasts/{commands,queries,handlers}
mkdir -p app/infrastructure/persistence/slick/repositories/yeasts
mkdir -p app/interfaces/controllers/yeasts
mkdir -p test/domain/yeasts
mkdir -p test/integration/yeasts
mkdir -p scripts/yeast

echo -e "${GREEN}✅ Structure dossiers créée${NC}"

# Créer fichier de tracking de l'implémentation
cat > "YEAST_IMPLEMENTATION.md" << 'EOF'
# Implémentation Domaine Yeast

**Branche** : feature/yeast-domain  
**Démarré** : $(date)  
**Status** : 🚧 En cours

## Checklist Implementation

### Phase 1: Value Objects
- [ ] YeastId
- [ ] YeastName  
- [ ] YeastLaboratory
- [ ] YeastStrain
- [ ] YeastType
- [ ] AttenuationRange
- [ ] FermentationTemp
- [ ] AlcoholTolerance
- [ ] FlocculationLevel
- [ ] YeastCharacteristics

### Phase 2: Aggregate
- [ ] YeastAggregate
- [ ] YeastEvents
- [ ] YeastStatus

### Phase 3: Services
- [ ] YeastDomainService
- [ ] YeastValidationService
- [ ] YeastRecommendationService

### Phase 4: Repositories
- [ ] YeastRepository (trait)
- [ ] YeastReadRepository
- [ ] YeastWriteRepository
- [ ] SlickYeastRepository implem

### Phase 5: Application Layer
- [ ] Commands (Create, Update, Delete)
- [ ] Queries (List, Detail, Search)
- [ ] CommandHandlers
- [ ] QueryHandlers

### Phase 6: Interface Layer
- [ ] YeastController (admin)
- [ ] YeastPublicController
- [ ] Routes configuration
- [ ] DTOs et validation

### Phase 7: Infrastructure
- [ ] Database evolutions
- [ ] Slick table definition
- [ ] Repository implementations

### Phase 8: Tests
- [ ] Unit tests Value Objects
- [ ] Unit tests Aggregate
- [ ] Integration tests
- [ ] API tests

## Métriques Objectifs

- **Coverage** : ≥ 100%
- **Performance API** : < 100ms
- **Build time** : Sans régression
EOF

echo -e "${GREEN}✅ Fichier de tracking créé${NC}"

# =============================================================================
# ÉTAPE 5: COMMIT INITIAL
# =============================================================================

echo -e "\n${YELLOW}💾 Commit initial...${NC}"

git add .
git commit -m "🍺 feat(yeast): Initialize yeast domain implementation

- Create feature branch for yeast domain
- Setup directory structure following DDD/CQRS patterns
- Add implementation tracking document
- Prepare environment for yeast aggregate development

Ref: Brewing Platform expansion - Phase 3A"

echo -e "${GREEN}✅ Commit initial effectué${NC}"

# =============================================================================
# ÉTAPE 6: RÉSUMÉ ET INSTRUCTIONS
# =============================================================================

echo -e "\n${BLUE}🎯 RÉSUMÉ DE L'OPÉRATION${NC}"
echo -e "${BLUE}========================${NC}"
echo ""
echo -e "${GREEN}✅ Branche créée : ${BRANCH_NAME}${NC}"
echo -e "${GREEN}✅ Sauvegarde : ${BACKUP_BRANCH}${NC}"
echo -e "${GREEN}✅ Backup local : ${BACKUP_DIR}${NC}"
echo -e "${GREEN}✅ Structure prête pour implémentation${NC}"
echo ""
echo -e "${YELLOW}📋 PROCHAINES ÉTAPES :${NC}"
echo -e "${YELLOW}1.${NC} ./scripts/yeast/01-create-value-objects.sh"
echo -e "${YELLOW}2.${NC} ./scripts/yeast/02-create-aggregate.sh"
echo -e "${YELLOW}3.${NC} ./scripts/yeast/03-create-services.sh"
echo -e "${YELLOW}4.${NC} Continuer avec les autres scripts..."
echo ""
echo -e "${BLUE}🔄 Pour revenir à la branche précédente :${NC}"
echo -e "${BLUE}   git checkout ${CURRENT_BRANCH}${NC}"
echo ""
echo -e "${BLUE}🆘 En cas de problème :${NC}"
echo -e "${BLUE}   git checkout ${BACKUP_BRANCH}${NC}"
echo ""
echo -e "${GREEN}🚀 Prêt pour l'implémentation du domaine Yeast !${NC}"