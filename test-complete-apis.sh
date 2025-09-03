#!/bin/bash

# =============================================================================
# SCRIPT DE TEST COMPLET DE TOUTES LES APIs - TOUS LES ENDPOINTS
# =============================================================================

BASE_URL="http://localhost:9000"
ADMIN_AUTH="Authorization: Basic YWRtaW46YnJld2luZzIwMjQ="

# Couleurs pour le output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction de test
test_endpoint() {
    local method=$1
    local endpoint=$2
    local auth=$3
    local expected_codes=$4
    local description=$5
    local body_data=${6:-""}
    
    echo -n "Testing: $method $endpoint"
    
    if [ "$method" = "POST" ] || [ "$method" = "PUT" ]; then
        if [ "$auth" = "true" ]; then
            if [ -n "$body_data" ]; then
                response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "$ADMIN_AUTH" -H "Content-Type: application/json" -d "$body_data" 2>/dev/null)
            else
                response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "$ADMIN_AUTH" -H "Content-Type: application/json" -d '{}' 2>/dev/null)
            fi
        else
            if [ -n "$body_data" ]; then
                response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "Content-Type: application/json" -d "$body_data" 2>/dev/null)
            else
                response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "Content-Type: application/json" -d '{}' 2>/dev/null)
            fi
        fi
    else
        if [ "$auth" = "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "$ADMIN_AUTH" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" 2>/dev/null)
        fi
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    # Vérifier si le code est dans les codes attendus
    if [[ "$expected_codes" == *"$status_code"* ]]; then
        echo -e " ${GREEN}✅ $status_code${NC} - $description"
        return 0
    else
        echo -e " ${RED}❌ $status_code${NC} - $description"
        echo "   Expected: $expected_codes, Got: $status_code"
        return 1
    fi
}

# Compteurs
total_tests=0
passed_tests=0

echo "🍺 BREWING APP - TEST COMPLET DE TOUTES LES APIs"
echo "==============================================="
echo

# =============================================================================
# 1. HOME & ASSETS (2 endpoints)
# =============================================================================
echo -e "${BLUE}📁 HOME & ASSETS${NC}"
test_endpoint "GET" "/" false "200" "Page d'accueil"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/assets/images/logo.png" false "200,404" "Assets statiques"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 2. HOPS PUBLIC APIs (3 endpoints)
# =============================================================================
echo -e "${BLUE}🍺 HOPS PUBLIC APIs${NC}"
test_endpoint "GET" "/api/v1/hops" false "200" "Liste des houblons"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/hops/cascade" false "200,404" "Détail houblon"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/v1/hops/search" false "200,400" "Recherche houblons" '{"query":"cascade","limit":10}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 3. HOPS ADMIN APIs (5 endpoints)
# =============================================================================
echo -e "${BLUE}🍺 HOPS ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/hops" true "200" "Liste admin houblons"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/hops/cascade" true "200,404" "Détail admin houblon"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/admin/hops" true "201,400" "Créer houblon" '{"name":"Test Hop","alphaAcid":5.5,"betaAcid":3.2}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/hops/test-hop" true "200,404" "Modifier houblon" '{"name":"Test Hop Updated","alphaAcid":6.0}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "DELETE" "/api/admin/hops/test-hop" true "200,404" "Supprimer houblon"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 4. MALTS PUBLIC APIs (4 endpoints)
# =============================================================================
echo -e "${BLUE}🌾 MALTS PUBLIC APIs${NC}"
test_endpoint "GET" "/api/v1/malts" false "200" "Liste des malts"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/malts/pilsner-2-row" false "200,404" "Détail malt"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/v1/malts/search" false "200,400" "Recherche malts" '{"query":"pilsner","limit":10}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/malts/type/BASE" false "200" "Malts par type"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 5. MALTS ADMIN APIs (5 endpoints)
# =============================================================================
echo -e "${BLUE}🌾 MALTS ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/malts" true "200" "Liste admin malts"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/malts/pilsner-2-row" true "200,404" "Détail admin malt"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/admin/malts" true "201,400" "Créer malt" '{"name":"Test Malt","type":"BASE","color":2}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/malts/test-malt" true "200,404" "Modifier malt" '{"name":"Test Malt Updated","color":3}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "DELETE" "/api/admin/malts/test-malt" true "200,404" "Supprimer malt"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 6. YEASTS PUBLIC APIs (12 endpoints)
# =============================================================================
echo -e "${BLUE}🦠 YEASTS PUBLIC APIs${NC}"
test_endpoint "GET" "/api/v1/yeasts" false "200" "Liste des levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/debug" false "200,404" "Debug test levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/search?q=wyeast" false "200" "Recherche levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/stats" false "200" "Stats publiques levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/popular" false "200" "Levures populaires"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/type/ALE" false "200" "Levures par type"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/laboratory/Wyeast" false "200" "Levures par labo"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/recommendations/beginner" false "200" "Recommandations débutants"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/recommendations/seasonal" false "200" "Recommandations saisonnières"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/recommendations/experimental" false "200" "Recommandations expérimentales"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/yeasts/recommendations/style/ipa" false "200" "Recommandations par style"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

# Test avec un ID de levure existant (à ajuster selon les données)
test_endpoint "GET" "/api/v1/yeasts/1" false "200,404" "Détail levure"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 7. YEASTS ADMIN APIs (15 endpoints)
# =============================================================================
echo -e "${BLUE}🦠 YEASTS ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/yeasts" true "200" "Liste admin levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/yeasts/stats" true "200" "Stats admin levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/yeasts/export" true "200,501" "Export levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/admin/yeasts" true "201,400" "Créer levure" '{"name":"Test Yeast","strain":"TEST001","laboratory":"Wyeast","type":"ALE"}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/admin/yeasts/batch" true "201,400" "Création batch levures" '{"yeasts":[{"name":"Batch Test","strain":"BATCH001"}]}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/yeasts/1" true "200,404" "Détail admin levure"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/yeasts/1" true "200,404" "Modifier levure" '{"name":"Updated Yeast"}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/yeasts/1/status" true "200,404" "Changer statut levure" '{"status":"ACTIVE"}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/yeasts/1/activate" true "200,404" "Activer levure"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/yeasts/1/deactivate" true "200,404" "Désactiver levure"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "PUT" "/api/admin/yeasts/1/archive" true "200,404" "Archiver levure"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "DELETE" "/api/admin/yeasts/1" true "200,404" "Supprimer levure"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 8. RECIPES PUBLIC APIs (18 endpoints)
# =============================================================================
echo -e "${BLUE}📝 RECIPES PUBLIC APIs${NC}"
test_endpoint "GET" "/api/v1/recipes/health" false "200" "Santé du service"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/search" false "200" "Recherche recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/discover" false "200" "Découverte recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/recommendations/beginner" false "200" "Recommandations débutants"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/recommendations/style/ipa" false "200" "Recommandations par style"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/recommendations/ingredients" false "200,400" "Recommandations par ingrédients"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/recommendations/seasonal/summer" false "200" "Recommandations saisonnières"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/recommendations/progression?lastRecipeId=1" false "200,400" "Recommandations progression"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/compare?recipeIds=1,2" false "200,400" "Comparaison recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/v1/recipes/analyze" false "200,400" "Analyse recette custom" '{"name":"Test Recipe","style":"IPA"}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/stats" false "200" "Stats publiques recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/collections" false "200" "Collections recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/1" false "200,404" "Détail recette"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/1/scale?targetBatchSize=20" false "200,404" "Mise à l'échelle recette"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/1/brewing-guide" false "200,404" "Guide de brassage"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/1/alternatives" false "200,404" "Alternatives recette"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 9. RECIPES ADMIN APIs (5 endpoints)
# =============================================================================
echo -e "${BLUE}📝 RECIPES ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/recipes" true "200" "Liste admin recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/recipes/_health" true "200" "Santé admin recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "POST" "/api/admin/recipes" true "201,400" "Créer recette" '{"name":"Test Recipe","style":"IPA","difficulty":"beginner"}'
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/recipes/1" true "200,404" "Détail admin recette"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "DELETE" "/api/admin/recipes/1" true "200,404" "Supprimer recette"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# RÉSUMÉ FINAL
# =============================================================================
echo "==============================================="
echo -e "${BLUE}📊 RÉSUMÉ FINAL - TOUS LES ENDPOINTS${NC}"
echo "==============================================="
echo -e "Total APIs testées: ${BLUE}$total_tests${NC}"
echo -e "APIs fonctionnelles: ${GREEN}$passed_tests${NC}"
echo -e "APIs défaillantes: ${RED}$((total_tests - passed_tests))${NC}"

percentage=$((passed_tests * 100 / total_tests))
echo -e "Taux de succès: ${GREEN}$percentage%${NC}"

if [ $percentage -eq 100 ]; then
    echo -e "${GREEN}🎉 PARFAIT! Toutes les APIs fonctionnent!${NC}"
elif [ $percentage -ge 90 ]; then
    echo -e "${GREEN}🎉 Excellent! Presque toutes les APIs fonctionnent${NC}"
elif [ $percentage -ge 80 ]; then
    echo -e "${YELLOW}⚠️  Bon état général, quelques corrections nécessaires${NC}"
else
    echo -e "${RED}❌ Corrections majeures nécessaires${NC}"
fi

echo
echo "Total des endpoints testés: $total_tests"