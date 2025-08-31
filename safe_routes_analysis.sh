#!/bin/bash
# =============================================================================
# ANALYSE SÉCURISÉE DU FICHIER ROUTES - DIAGNOSTIC AVANT MODIFICATION
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔍 ANALYSE SÉCURISÉE DU FICHIER ROUTES ACTUEL${NC}"
echo ""

# =============================================================================
# ÉTAPE 1 : VÉRIFICATION EXISTENCE ET SAUVEGARDE
# =============================================================================

echo -e "${BLUE}1. Vérification et sauvegarde du fichier routes...${NC}"

if [ ! -f "conf/routes" ]; then
    echo -e "${RED}❌ ERREUR: Fichier conf/routes non trouvé !${NC}"
    exit 1
fi

# Créer sauvegarde avec timestamp
BACKUP_FILE="conf/routes.backup-analysis-$(date +%Y%m%d_%H%M%S)"
cp "conf/routes" "$BACKUP_FILE"
echo -e "${GREEN}✅ Sauvegarde créée: $BACKUP_FILE${NC}"

# =============================================================================
# ÉTAPE 2 : ANALYSE STRUCTURE ACTUELLE
# =============================================================================

echo ""
echo -e "${BLUE}2. Analyse de la structure actuelle...${NC}"

echo ""
echo -e "${YELLOW}📊 STATISTIQUES DU FICHIER:${NC}"
echo "   Lignes totales: $(wc -l < conf/routes)"
echo "   Lignes non vides: $(grep -c . conf/routes)"
echo "   Lignes de commentaires: $(grep -c '^#' conf/routes)"
echo "   Routes actives: $(grep -c '^[A-Z]' conf/routes)"

echo ""
echo -e "${YELLOW}🛣️  TYPES DE ROUTES DÉTECTÉES:${NC}"
echo "   GET routes: $(grep -c '^GET' conf/routes)"
echo "   POST routes: $(grep -c '^POST' conf/routes)"
echo "   PUT routes: $(grep -c '^PUT' conf/routes)"
echo "   DELETE routes: $(grep -c '^DELETE' conf/routes)"
echo "   PATCH routes: $(grep -c '^PATCH' conf/routes)"

echo ""
echo -e "${YELLOW}🎯 CONTROLLERS RÉFÉRENCÉS:${NC}"
# Extraire et compter les controllers uniques
grep '^[A-Z]' conf/routes | awk '{print $3}' | sed 's/\..*$//' | sort | uniq -c | sort -nr

echo ""
echo -e "${YELLOW}🌾 ROUTES MALTS ACTUELLES:${NC}"
echo "Routes contenant 'malt' (case insensitive):"
grep -i "malt" conf/routes | head -10 || echo "   Aucune route malt trouvée"

echo ""
echo -e "${YELLOW}🎛️  ROUTES ADMIN ACTUELLES:${NC}"
echo "Routes contenant '/admin':"
grep "/admin" conf/routes | head -5 || echo "   Aucune route admin trouvée"

# =============================================================================
# ÉTAPE 3 : IDENTIFICATION DES ROUTES PROBLÉMATIQUES
# =============================================================================

echo ""
echo -e "${BLUE}3. Identification des routes problématiques...${NC}"

echo ""
echo -e "${YELLOW}❌ ROUTES ADMINMALTSCONTROLLER PROBLÉMATIQUES:${NC}"
PROBLEMATIC_ROUTES=$(grep "AdminMaltsController" conf/routes | grep -v "getAllMalts\|searchMalts" || echo "")

if [ -n "$PROBLEMATIC_ROUTES" ]; then
    echo "$PROBLEMATIC_ROUTES" | nl
    PROBLEM_COUNT=$(echo "$PROBLEMATIC_ROUTES" | wc -l)
    echo ""
    echo -e "${RED}   Trouvé $PROBLEM_COUNT routes problématiques à corriger${NC}"
else
    echo -e "${GREEN}   Aucune route AdminMaltsController problématique trouvée${NC}"
fi

# =============================================================================
# ÉTAPE 4 : VÉRIFICATION MÉTHODES CONTROLLER EXISTANTES
# =============================================================================

echo ""
echo -e "${BLUE}4. Vérification des méthodes du controller existant...${NC}"

if [ -f "app/controllers/admin/AdminMaltsController.scala" ]; then
    echo ""
    echo -e "${YELLOW}📋 MÉTHODES DISPONIBLES DANS ADMINMALTSCONTROLLER:${NC}"
    grep "def " app/controllers/admin/AdminMaltsController.scala | sed 's/^[[:space:]]*/   /'
else
    echo -e "${RED}❌ Fichier AdminMaltsController.scala non trouvé !${NC}"
fi

# =============================================================================
# ÉTAPE 5 : GÉNÉRATION PLAN DE CORRECTION SÉCURISÉ
# =============================================================================

echo ""
echo -e "${BLUE}5. Plan de correction sécurisé...${NC}"

# Créer un fichier temporaire avec les corrections proposées
TEMP_ROUTES="conf/routes.proposed-fix"
cp "conf/routes" "$TEMP_ROUTES"

echo ""
echo -e "${YELLOW}🔧 PLAN DE CORRECTION PROPOSÉ:${NC}"

if [ -n "$PROBLEMATIC_ROUTES" ]; then
    echo "1. Commenter (ne pas supprimer) les routes problématiques:"
    echo "$PROBLEMATIC_ROUTES" | sed 's/^/   # DISABLED: /'
    
    echo ""
    echo "2. Ajouter les routes fonctionnelles à la fin du fichier:"
    echo "   # Routes Admin Malts fonctionnelles"
    echo "   GET /api/admin/malts                    controllers.admin.AdminMaltsController.getAllMaltsDefault()"
    echo "   GET /api/admin/malts/paginated          controllers.admin.AdminMaltsController.getAllMalts(page: Int ?= 0, pageSize: Int ?= 20, activeOnly: Boolean ?= false)"
    echo "   GET /api/admin/malts/search             controllers.admin.AdminMaltsController.searchMalts(query: String, page: Int ?= 0, pageSize: Int ?= 20)"
    
    # Appliquer les corrections au fichier temporaire
    
    # Commenter les routes problématiques
    echo "$PROBLEMATIC_ROUTES" | while read -r route; do
        if [ -n "$route" ]; then
            # Échapper les caractères spéciaux pour sed
            escaped_route=$(printf '%s\n' "$route" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
            sed -i.tmp "s/^$escaped_route$/# DISABLED: $route/" "$TEMP_ROUTES" 2>/dev/null || true
        fi
    done
    
    # Ajouter les routes fonctionnelles
    cat >> "$TEMP_ROUTES" << 'EOF'

# ============================================================================
# ROUTES ADMIN MALTS FONCTIONNELLES (ajoutées par correction automatique)
# ============================================================================
GET     /api/admin/malts                        controllers.admin.AdminMaltsController.getAllMaltsDefault()
GET     /api/admin/malts/paginated              controllers.admin.AdminMaltsController.getAllMalts(page: Int ?= 0, pageSize: Int ?= 20, activeOnly: Boolean ?= false)
GET     /api/admin/malts/search                 controllers.admin.AdminMaltsController.searchMalts(query: String, page: Int ?= 0, pageSize: Int ?= 20)

EOF
    
    echo ""
    echo -e "${GREEN}✅ Proposition de correction générée dans: $TEMP_ROUTES${NC}"
else
    echo -e "${GREEN}✅ Aucune correction nécessaire - routes déjà correctes${NC}"
    rm "$TEMP_ROUTES"
fi

# =============================================================================
# ÉTAPE 6 : VALIDATION AVANT APPLICATION
# =============================================================================

echo ""
echo -e "${BLUE}6. Validation et options d'application...${NC}"

if [ -f "$TEMP_ROUTES" ]; then
    echo ""
    echo -e "${YELLOW}🔍 APERÇU DES CHANGEMENTS (premières lignes différentes):${NC}"
    diff "conf/routes" "$TEMP_ROUTES" | head -20 || echo "   Fichiers identiques"
    
    echo ""
    echo -e "${YELLOW}📊 COMPARAISON AVANT/APRÈS:${NC}"
    echo "   Routes avant: $(grep -c '^[A-Z]' conf/routes)"
    echo "   Routes après: $(grep -c '^[A-Z]' "$TEMP_ROUTES")"
    echo "   Routes commentées: $(grep -c '^# DISABLED:' "$TEMP_ROUTES")"
    
    echo ""
    echo -e "${BLUE}OPTIONS D'APPLICATION:${NC}"
    echo "   1. Appliquer automatiquement: mv $TEMP_ROUTES conf/routes"
    echo "   2. Révision manuelle d'abord: diff conf/routes $TEMP_ROUTES"
    echo "   3. Annuler: rm $TEMP_ROUTES"
    echo ""
    echo -e "${GREEN}✅ Correction préparée et prête à appliquer de manière sécurisée${NC}"
else
    echo -e "${GREEN}✅ Aucune correction nécessaire${NC}"
fi

# =============================================================================
# RÉSUMÉ FINAL
# =============================================================================

echo ""
echo -e "${GREEN}🎯 ============================================================================="
echo "   ANALYSE TERMINÉE - ROUTES ANALYSÉES DE MANIÈRE SÉCURISÉE"
echo "=============================================================================${NC}"
echo ""

echo -e "${BLUE}📋 RÉSUMÉ:${NC}"
echo "   • Sauvegarde créée: $BACKUP_FILE"
if [ -f "$TEMP_ROUTES" ]; then
    echo "   • Correction proposée: $TEMP_ROUTES"
    echo "   • Prêt pour application sécurisée"
else
    echo "   • Aucune correction nécessaire"
fi

echo ""
echo -e "${YELLOW}⚠️  IMPORTANT:${NC}"
echo "   • Toutes les routes existantes sont préservées"
echo "   • Aucune suppression définitive - seulement commentaires"
echo "   • Sauvegarde complète disponible"
echo "   • Application manuelle recommandée pour validation finale"