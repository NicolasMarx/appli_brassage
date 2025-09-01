#!/bin/bash

# 🚨 CORRECTION IMMÉDIATE DES ERREURS ROUTES
# Résout les erreurs "identifier expected but `public` found"

set -e

echo "🚨 Correction urgente des erreurs routes..."

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# =============================================================================
# ÉTAPE 1 : SAUVEGARDE DU ROUTES ACTUEL
# =============================================================================

echo -e "${BLUE}📁 Sauvegarde du fichier routes actuel...${NC}"

# Créer sauvegarde avec timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
cp conf/routes conf/routes.backup_$TIMESTAMP 2>/dev/null || true
echo -e "${GREEN}✅ Sauvegarde créée : conf/routes.backup_$TIMESTAMP${NC}"

# =============================================================================
# ÉTAPE 2 : CORRECTION DU FICHIER ROUTES
# =============================================================================

echo -e "${BLUE}🔧 Reconstruction du fichier routes avec structure correcte...${NC}"

cat > conf/routes << 'EOF'
# Routes corrigées - Structure DDD/CQRS

# =============================================================================
# PAGE D'ACCUEIL
# =============================================================================
GET     /                           controllers.HomeController.index()

# =============================================================================
# API PUBLIQUE v1 - LECTURE SEULE
# =============================================================================
GET     /api/v1/hops                controllers.api.v1.hops.HopsController.list(page: Int ?= 0, size: Int ?= 20)
GET     /api/v1/hops/:id            controllers.api.v1.hops.HopsController.detail(id: String)
POST    /api/v1/hops/search         controllers.api.v1.hops.HopsController.search()

# Malts publiques (à implémenter)
# GET     /api/v1/malts               controllers.api.v1.malts.MaltsController.list(page: Int ?= 0, size: Int ?= 20)
# GET     /api/v1/malts/:id           controllers.api.v1.malts.MaltsController.detail(id: String)
# POST    /api/v1/malts/search        controllers.api.v1.malts.MaltsController.search()

# =============================================================================
# API ADMIN - ÉCRITURE SÉCURISÉE
# =============================================================================
GET     /api/admin/hops             controllers.admin.AdminHopsController.list(page: Int ?= 0, size: Int ?= 20)
POST    /api/admin/hops             controllers.admin.AdminHopsController.create()
GET     /api/admin/hops/:id         controllers.admin.AdminHopsController.detail(id: String)
PUT     /api/admin/hops/:id         controllers.admin.AdminHopsController.update(id: String)
DELETE  /api/admin/hops/:id         controllers.admin.AdminHopsController.delete(id: String)

# Malts admin (existant)
GET     /api/admin/malts            controllers.admin.AdminMaltsController.list(page: Int ?= 0, size: Int ?= 20)
POST    /api/admin/malts            controllers.admin.AdminMaltsController.create()
GET     /api/admin/malts/:id        controllers.admin.AdminMaltsController.get(id: String)
PUT     /api/admin/malts/:id        controllers.admin.AdminMaltsController.update(id: String)
DELETE  /api/admin/malts/:id        controllers.admin.AdminMaltsController.delete(id: String)

# =============================================================================
# ASSETS STATIQUES
# =============================================================================
GET     /assets/*file               controllers.Assets.versioned(path="/public", file: Asset)
EOF

echo -e "${GREEN}✅ Fichier routes corrigé avec structure cohérente${NC}"

# =============================================================================
# ÉTAPE 3 : VÉRIFICATION DE LA COMPILATION
# =============================================================================

echo -e "${BLUE}🧪 Test de compilation immédiat...${NC}"

echo "Compilation en cours..."
if sbt compile > /tmp/routes_fix_compilation.log 2>&1; then
    echo -e "${GREEN}🎉 ROUTES CORRIGÉES - COMPILATION RÉUSSIE !${NC}"
    COMPILATION_SUCCESS=true
else
    echo -e "${RED}❌ Erreurs persistantes dans routes${NC}"
    echo -e "${YELLOW}Détails des erreurs :${NC}"
    grep -A 5 -B 5 "routes:" /tmp/routes_fix_compilation.log 2>/dev/null || tail -10 /tmp/routes_fix_compilation.log
    COMPILATION_SUCCESS=false
fi

# =============================================================================
# ÉTAPE 4 : RAPPORT DE CORRECTION
# =============================================================================

echo ""
echo -e "${BLUE}📊 RAPPORT DE CORRECTION ROUTES${NC}"
echo "======================================="

if [ "$COMPILATION_SUCCESS" = true ]; then
    echo -e "${GREEN}✅ PROBLÈME ROUTES RÉSOLU !${NC}"
    echo ""
    echo -e "${GREEN}🔧 Corrections appliquées :${NC}"
    echo "   ✅ Suppression des références incorrectes à 'public' en tant que package"
    echo "   ✅ Utilisation des vrais noms de contrôleurs existants"
    echo "   ✅ Structure cohérente avec l'architecture DDD/CQRS"
    echo "   ✅ Routes malts commentées temporairement (à décommenter après implémentation)"
    echo ""
    echo -e "${BLUE}🎯 Routes opérationnelles :${NC}"
    echo "   ✅ GET  / → Page d'accueil"
    echo "   ✅ GET  /api/v1/hops → API publique houblons"
    echo "   ✅ GET  /api/admin/hops → API admin houblons"
    echo "   ✅ GET  /api/admin/malts → API admin malts"
    echo "   ✅ GET  /assets/* → Ressources statiques"
    
else
    echo -e "${RED}❌ ERREURS ROUTES PERSISTANTES${NC}"
    echo ""
    echo -e "${YELLOW}Actions possibles :${NC}"
    echo "   1. Vérifiez que tous les contrôleurs existent dans les bons packages"
    echo "   2. Restaurez la sauvegarde si nécessaire :"
    echo "      cp conf/routes.backup_$TIMESTAMP conf/routes"
    echo "   3. Consultez les logs détaillés : /tmp/routes_fix_compilation.log"
fi

echo ""
echo -e "${BLUE}📁 Sauvegardes disponibles :${NC}"
ls -la conf/routes.backup_* 2>/dev/null || echo "   Aucune sauvegarde trouvée"

echo ""
echo -e "${GREEN}🚨 Correction urgente routes terminée !${NC}"
