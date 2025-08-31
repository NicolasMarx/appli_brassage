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
