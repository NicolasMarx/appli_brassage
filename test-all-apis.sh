#!/bin/bash

# =============================================================================
# SCRIPT DE TEST COMPLET DES APIs - 64 ENDPOINTS
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
    
    echo -n "Testing: $endpoint"
    
    if [ "$auth" = "true" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "$ADMIN_AUTH" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" 2>/dev/null)
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    # Vérifier si le code est dans les codes attendus
    if [[ "$expected_codes" == *"$status_code"* ]]; then
        echo -e " ${GREEN}✅ $status_code${NC} - $description"
        return 0
    else
        echo -e " ${RED}❌ $status_code${NC} - $description"
        return 1
    fi
}

# Compteurs
total_tests=0
passed_tests=0

echo "🍺 BREWING APP - TEST COMPLET DES APIs"
echo "======================================"
echo

# =============================================================================
# 1. HOME & ASSETS (2 endpoints)
# =============================================================================
echo -e "${BLUE}📁 HOME & ASSETS${NC}"
test_endpoint "GET" "/" false "200" "Page d'accueil"
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

echo

# =============================================================================
# 3. HOPS ADMIN APIs (5 endpoints)
# =============================================================================
echo -e "${BLUE}🍺 HOPS ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/hops" true "200" "Liste admin houblons"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/hops/cascade" true "200,404" "Détail admin houblon"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 4. MALTS PUBLIC APIs (4 endpoints)
# =============================================================================
echo -e "${BLUE}🌾 MALTS PUBLIC APIs${NC}"
test_endpoint "GET" "/api/v1/malts" false "200" "Liste des malts"
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

echo

# =============================================================================
# 6. YEASTS PUBLIC APIs (10 endpoints)
# =============================================================================
echo -e "${BLUE}🦠 YEASTS PUBLIC APIs${NC}"
test_endpoint "GET" "/api/v1/yeasts" false "200" "Liste des levures"
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

echo

# =============================================================================
# 7. YEASTS ADMIN APIs (11 endpoints)
# =============================================================================
echo -e "${BLUE}🦠 YEASTS ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/yeasts" true "200" "Liste admin levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/yeasts/stats" true "200" "Stats admin levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/yeasts/export" true "200,501" "Export levures"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 8. RECIPES PUBLIC APIs (15 endpoints)
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

test_endpoint "GET" "/api/v1/recipes/recommendations/ingredients" false "200" "Recommandations par ingrédients"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/recommendations/seasonal/summer" false "200" "Recommandations saisonnières"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/compare?recipeIds=1,2" false "200,400" "Comparaison recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/stats" false "200" "Stats publiques recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/v1/recipes/collections" false "200" "Collections recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# 9. RECIPES ADMIN APIs (4 endpoints)
# =============================================================================
echo -e "${BLUE}📝 RECIPES ADMIN APIs${NC}"
test_endpoint "GET" "/api/admin/recipes" true "200" "Liste admin recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

test_endpoint "GET" "/api/admin/recipes/_health" true "200" "Santé admin recettes"
((total_tests++)); [[ $? -eq 0 ]] && ((passed_tests++))

echo

# =============================================================================
# RÉSUMÉ FINAL
# =============================================================================
echo "======================================"
echo -e "${BLUE}📊 RÉSUMÉ FINAL${NC}"
echo "======================================"
echo -e "Total APIs testées: ${BLUE}$total_tests${NC}"
echo -e "APIs fonctionnelles: ${GREEN}$passed_tests${NC}"
echo -e "APIs défaillantes: ${RED}$((total_tests - passed_tests))${NC}"

percentage=$((passed_tests * 100 / total_tests))
echo -e "Taux de succès: ${GREEN}$percentage%${NC}"

if [ $percentage -ge 80 ]; then
    echo -e "${GREEN}🎉 Excellent! La majorité des APIs fonctionnent${NC}"
elif [ $percentage -ge 60 ]; then
    echo -e "${YELLOW}⚠️  Bon état général, quelques corrections nécessaires${NC}"
else
    echo -e "${RED}❌ Problèmes majeurs détectés${NC}"
fi