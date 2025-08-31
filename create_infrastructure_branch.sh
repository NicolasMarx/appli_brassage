#!/bin/bash

# =============================================================================
# SCRIPT DE CRÉATION BRANCHE INFRASTRUCTURE
# =============================================================================
# Crée une nouvelle branche pour les corrections d'infrastructure
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🌿 =============================================================================="
echo "   CRÉATION BRANCHE POUR CORRECTIONS INFRASTRUCTURE"
echo "=============================================================================="
echo -e "${NC}"

echo_step() { echo -e "${BLUE}📋 $1${NC}"; }
echo_success() { echo -e "${GREEN}✅ $1${NC}"; }
echo_error() { echo -e "${RED}❌ $1${NC}"; }
echo_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# Variables
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BRANCH_NAME="fix/infrastructure-corrections-$TIMESTAMP"
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")

# =============================================================================
# ÉTAPE 1: VÉRIFICATIONS PRÉLIMINAIRES
# =============================================================================

echo_step "Vérifications préliminaires Git"

# Vérifier que Git est initialisé
if [ ! -d ".git" ]; then
    echo_error "Ce répertoire n'est pas un dépôt Git"
    echo "Initialisation du dépôt Git..."
    git init
    echo_success "Dépôt Git initialisé"
fi

# Afficher la branche actuelle
echo_success "Branche actuelle: $CURRENT_BRANCH"
echo_success "Nouvelle branche: $BRANCH_NAME"

# Vérifier l'état du working directory
if ! git diff --quiet 2>/dev/null; then
    echo_warning "Vous avez des modifications non commitées"
    echo ""
    echo -e "${YELLOW}Modifications en cours:${NC}"
    git status --porcelain | head -10
    if [ $(git status --porcelain | wc -l) -gt 10 ]; then
        echo "... et $(( $(git status --porcelain | wc -l) - 10 )) autres fichiers"
    fi
    echo ""
    
    echo -e "${BLUE}Options:${NC}"
    echo "  1. Committer les modifications actuelles"
    echo "  2. Stasher les modifications et les récupérer après"
    echo "  3. Continuer sans commit (modifications seront dans la nouvelle branche)"
    echo "  4. Annuler"
    echo ""
    read -p "Votre choix (1-4): " choice
    
    case $choice in
        1)
            echo_step "Commit des modifications actuelles"
            git add .
            commit_message="chore: sauvegarde avant corrections infrastructure - $(date)"
            git commit -m "$commit_message"
            echo_success "Modifications commitées"
            ;;
        2)
            echo_step "Stash des modifications"
            git stash push -m "Modifications avant corrections infrastructure - $(date)"
            echo_success "Modifications stashées"
            STASHED=true
            ;;
        3)
            echo_warning "Les modifications seront incluses dans la nouvelle branche"
            ;;
        4)
            echo_error "Annulation par l'utilisateur"
            exit 1
            ;;
        *)
            echo_error "Choix invalide, annulation"
            exit 1
            ;;
    esac
fi

echo ""

# =============================================================================
# ÉTAPE 2: CRÉATION DE LA BRANCHE
# =============================================================================

echo_step "Création de la nouvelle branche"

# Créer et basculer sur la nouvelle branche
if git checkout -b "$BRANCH_NAME" 2>/dev/null; then
    echo_success "Branche '$BRANCH_NAME' créée et activée"
else
    echo_error "Erreur lors de la création de la branche"
    exit 1
fi

# =============================================================================
# ÉTAPE 3: DOCUMENTATION DE LA BRANCHE
# =============================================================================

echo_step "Documentation de la branche"

# Créer un fichier de documentation pour cette branche
cat > "BRANCH_INFRASTRUCTURE_FIXES.md" << 'BRANCH_DOC_EOF'
# Branche de Corrections Infrastructure

## 🎯 Objectif de cette branche
Cette branche contient les corrections nécessaires pour résoudre les problèmes d'infrastructure détectés lors de la vérification de la Phase 1.

## 🐛 Problèmes identifiés à corriger
1. **Docker-compose incorrect** : Service `db` au lieu de `postgres`, `redis` manquant
2. **Version Java incompatible** : Java 24 non supporté par Play Framework
3. **Configuration Slick manquante** : Configuration base de données incomplète
4. **Évolutions manquantes** : Schema de base de données à créer

## 🔧 Corrections prévues
- [ ] Correction docker-compose.yml avec services `postgres` et `redis`
- [ ] Configuration application.conf pour Slick/PostgreSQL
- [ ] Création évolutions de base de données
- [ ] Scripts de vérification infrastructure
- [ ] Documentation mise à jour

## 📋 Checklist avant merge
- [ ] Services Docker démarrent correctement
- [ ] Application Play se lance sans erreur
- [ ] API publique accessible (GET /api/v1/hops)
- [ ] Base de données PostgreSQL connectée
- [ ] Cache Redis opérationnel
- [ ] Tests de vérification passent

## 🔄 Comment tester
```bash
# Démarrer services
docker-compose up -d postgres redis

# Démarrer application
sbt run

# Vérifier APIs
./verify-all-phase1-apis.sh
```

## 🔙 Retour à la branche précédente
Si des problèmes surviennent :
```bash
git checkout BRANCH_PRECEDENTE
docker-compose down
```

## 📝 Notes
Créée le: DATE_CREATION
Branche parente: BRANCHE_PARENTE
Auteur: USER_NAME
BRANCH_DOC_EOF

# Remplacer les placeholders
sed -i.bak "s/DATE_CREATION/$(date)/" "BRANCH_INFRASTRUCTURE_FIXES.md" 2>/dev/null || \
    sed -i "s/DATE_CREATION/$(date)/" "BRANCH_INFRASTRUCTURE_FIXES.md"
sed -i.bak "s/BRANCHE_PARENTE/$CURRENT_BRANCH/" "BRANCH_INFRASTRUCTURE_FIXES.md" 2>/dev/null || \
    sed -i "s/BRANCHE_PARENTE/$CURRENT_BRANCH/" "BRANCH_INFRASTRUCTURE_FIXES.md"
sed -i.bak "s/USER_NAME/$(whoami)/" "BRANCH_INFRASTRUCTURE_FIXES.md" 2>/dev/null || \
    sed -i "s/USER_NAME/$(whoami)/" "BRANCH_INFRASTRUCTURE_FIXES.md"

# Supprimer les fichiers .bak créés par sed sur macOS
rm -f "BRANCH_INFRASTRUCTURE_FIXES.md.bak"

echo_success "Documentation de branche créée"

# =============================================================================
# ÉTAPE 4: PRÉPARATION DES SCRIPTS
# =============================================================================

echo_step "Préparation des scripts de correction"

# Créer un script wrapper qui combine toutes les corrections
cat > "apply-infrastructure-fixes.sh" << 'WRAPPER_SCRIPT_EOF'
#!/bin/bash

# =============================================================================
# SCRIPT WRAPPER - APPLICATION DES CORRECTIONS INFRASTRUCTURE
# =============================================================================
# Applique toutes les corrections d'infrastructure en séquence
# =============================================================================

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🔧 =============================================================================="
echo "   APPLICATION DES CORRECTIONS INFRASTRUCTURE"
echo "=============================================================================="
echo -e "${NC}"

echo -e "${GREEN}📋 Ce script va appliquer toutes les corrections nécessaires:${NC}"
echo "   1. Correction docker-compose.yml"
echo "   2. Configuration application.conf"  
echo "   3. Démarrage services PostgreSQL et Redis"
echo "   4. Création évolutions base de données"
echo "   5. Vérification finale"
echo ""

read -p "Continuer avec les corrections ? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Annulation"
    exit 1
fi

echo ""
echo -e "${GREEN}🚀 Application des corrections...${NC}"

# Étape 1: Corrections infrastructure
if [ -f "fix-infrastructure.sh" ]; then
    echo "📝 Application des corrections infrastructure..."
    chmod +x fix-infrastructure.sh
    ./fix-infrastructure.sh
    echo ""
else
    echo "❌ Script fix-infrastructure.sh non trouvé"
    echo "Veuillez d'abord créer les scripts de correction"
    exit 1
fi

# Étape 2: Vérification
if [ -f "verify-all-phase1-apis.sh" ]; then
    echo "🔍 Vérification des corrections..."
    chmod +x verify-all-phase1-apis.sh
    if ./verify-all-phase1-apis.sh; then
        echo -e "${GREEN}✅ Toutes les vérifications sont passées !${NC}"
    else
        echo -e "⚠️  Certaines vérifications ont échoué, mais les corrections de base sont appliquées"
    fi
else
    echo "⚠️  Script de vérification non trouvé, corrections appliquées sans vérification finale"
fi

echo ""
echo -e "${GREEN}🎉 CORRECTIONS INFRASTRUCTURE TERMINÉES !${NC}"
echo ""
echo -e "${BLUE}Prochaines étapes:${NC}"
echo "   1. Testez manuellement l'application"
echo "   2. Si tout fonctionne, commitez les changements:"
echo "      git add ."
echo "      git commit -m 'fix: corrections infrastructure Docker/Java/Config'"
echo "   3. Mergez dans la branche principale si satisfait"
echo ""
WRAPPER_SCRIPT_EOF

chmod +x "apply-infrastructure-fixes.sh"
echo_success "Script wrapper créé"

# =============================================================================
# ÉTAPE 5: COMMIT INITIAL DE LA BRANCHE
# =============================================================================

echo_step "Commit initial de la branche"

# Ajouter les fichiers de documentation
git add "BRANCH_INFRASTRUCTURE_FIXES.md" "apply-infrastructure-fixes.sh"

# Commit initial
initial_commit_msg="feat: création branche corrections infrastructure

- Branche créée pour corriger les problèmes d'infrastructure
- Documentation et scripts de correction préparés  
- Problèmes identifiés: Docker-compose, Java version, config Slick
- Prêt pour application des corrections"

git commit -m "$initial_commit_msg"
echo_success "Commit initial effectué"

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 =============================================================================="
echo "   BRANCHE INFRASTRUCTURE CRÉÉE AVEC SUCCÈS"
echo "==============================================================================${NC}"
echo ""

echo -e "${GREEN}✅ Actions effectuées:${NC}"
echo "   • Branche '$BRANCH_NAME' créée et activée"
echo "   • Documentation de branche ajoutée"
echo "   • Script wrapper de corrections préparé"
echo "   • Commit initial effectué"
echo ""

echo -e "${BLUE}📋 État actuel:${NC}"
echo "   • Branche active: $(git branch --show-current)"
echo "   • Branche parente: $CURRENT_BRANCH"
echo "   • Fichiers ajoutés: BRANCH_INFRASTRUCTURE_FIXES.md, apply-infrastructure-fixes.sh"
echo ""

if [ "${STASHED:-}" = "true" ]; then
    echo -e "${YELLOW}📦 Modifications stashées disponibles:${NC}"
    echo "   • Récupérer avec: git stash pop"
    echo ""
fi

echo -e "${BLUE}🚀 Prochaines étapes:${NC}"
echo ""
echo -e "${GREEN}1. Appliquer les corrections infrastructure:${NC}"
echo "   ./apply-infrastructure-fixes.sh"
echo ""
echo -e "${GREEN}2. Ou étape par étape:${NC}"
echo "   chmod +x fix-infrastructure.sh"
echo "   ./fix-infrastructure.sh"
echo "   ./verify-all-phase1-apis.sh"
echo ""
echo -e "${GREEN}3. Si les corrections fonctionnent:${NC}"
echo "   git add ."
echo "   git commit -m 'fix: corrections infrastructure appliquées'"
echo ""
echo -e "${GREEN}4. Pour merger dans la branche principale:${NC}"
echo "   git checkout $CURRENT_BRANCH"
echo "   git merge $BRANCH_NAME"
echo ""
echo -e "${GREEN}5. En cas de problème, retourner à la branche précédente:${NC}"
echo "   git checkout $CURRENT_BRANCH"
echo ""

echo -e "${BLUE}📖 Documentation:${NC}"
echo "   • Lisez BRANCH_INFRASTRUCTURE_FIXES.md pour plus de détails"
echo "   • Tous les scripts sont documentés et prêts à utiliser"
echo ""