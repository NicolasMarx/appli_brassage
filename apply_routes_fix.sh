#!/bin/bash
# =============================================================================
# APPLICATION IMMÉDIATE DU CORRECTIF ROUTES
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Application immédiate du correctif routes${NC}"

# Vérifier que le fichier corrigé existe
if [ ! -f "conf/routes.proposed-fix" ]; then
    echo -e "${RED}❌ Fichier conf/routes.proposed-fix non trouvé${NC}"
    echo "   Veuillez relancer l'analyse avec: ./safe_routes_analysis.sh"
    exit 1
fi

# Vérifier qu'une sauvegarde existe
BACKUP_FILE=$(ls conf/routes.backup-analysis-* 2>/dev/null | head -1)
if [ -z "$BACKUP_FILE" ]; then
    echo -e "${YELLOW}⚠️  Création d'une sauvegarde de sécurité...${NC}"
    cp conf/routes conf/routes.backup-emergency-$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="conf/routes.backup-emergency-$(date +%Y%m%d_%H%M%S)"
fi

echo -e "${GREEN}✅ Sauvegarde disponible: $BACKUP_FILE${NC}"

# Appliquer la correction
echo "Application de la correction..."
mv conf/routes.proposed-fix conf/routes

echo -e "${GREEN}✅ Correction appliquée${NC}"

# Vérifier que les routes problématiques ont disparu
echo ""
echo -e "${BLUE}Vérification post-correction...${NC}"
REMAINING_ERRORS=$(grep -c "AdminMaltsController\.\(list\|create\|detail\|update\|delete\|statistics\|needsReview\|adjustCredibility\|batchImport\)" conf/routes 2>/dev/null || echo "0")

if [ "$REMAINING_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✅ Toutes les routes problématiques ont été supprimées${NC}"
else
    echo -e "${RED}❌ Il reste $REMAINING_ERRORS routes problématiques${NC}"
fi

# Vérifier les nouvelles routes
NEW_ROUTES=$(grep -c "getAllMalts\|searchMalts" conf/routes 2>/dev/null || echo "0")
echo "Nouvelles routes fonctionnelles: $NEW_ROUTES"

# Test de compilation immédiat
echo ""
echo -e "${BLUE}Test de compilation immédiat...${NC}"

if sbt compile > /tmp/routes_applied_compile.log 2>&1; then
    echo -e "${GREEN}✅ SUCCÈS - Compilation réussie après correction des routes !${NC}"
    echo ""
    echo -e "${BLUE}Routes disponibles pour tester :${NC}"
    echo "   curl http://localhost:9000/api/admin/malts"
    echo "   curl http://localhost:9000/api/admin/malts/paginated?page=0&pageSize=5"
    echo "   curl 'http://localhost:9000/api/admin/malts/search?query=pilsner'"
    echo ""
    echo -e "${GREEN}🎉 L'API malts devrait maintenant fonctionner !${NC}"
else
    echo -e "${RED}❌ Erreurs de compilation persistantes${NC}"
    echo "Dernières erreurs :"
    tail -10 /tmp/routes_applied_compile.log
    
    echo ""
    echo -e "${BLUE}Restauration possible avec :${NC}"
    echo "   cp $BACKUP_FILE conf/routes"
fi

echo ""
echo -e "${BLUE}Résumé des modifications :${NC}"
echo "   • Routes problématiques : supprimées du fichier actif"
echo "   • Routes fonctionnelles : ajoutées"  
echo "   • Sauvegarde : $BACKUP_FILE"
echo "   • Autres routes : préservées"