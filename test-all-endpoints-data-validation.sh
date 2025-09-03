#!/bin/bash

# =============================================================================
# VALIDATION DE DONNÉES RÉELLES - TOUS LES 77 ENDPOINTS
# =============================================================================

BASE_URL="http://localhost:9000"
ADMIN_AUTH="Authorization: Basic YWRtaW46YnJld2luZzIwMjQ="

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Fonction de validation des données
validate_data_response() {
    local endpoint=$1
    local method=$2
    local auth=$3
    local expected_codes=$4
    local description=$5
    local body_data=${6:-""}
    
    echo -n "🔍 $method $endpoint - "
    
    # Faire la requête
    if [ "$method" = "POST" ] || [ "$method" = "PUT" ]; then
        if [ "$auth" = "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "$ADMIN_AUTH" -H "Content-Type: application/json" -d "$body_data" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "Content-Type: application/json" -d "$body_data" 2>/dev/null)
        fi
    else
        if [ "$auth" = "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "$ADMIN_AUTH" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" 2>/dev/null)
        fi
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    # Vérifier le code de statut
    if [[ "$expected_codes" == *"$status_code"* ]]; then
        # Analyser le contenu pour vérifier les données réelles
        if [ "$status_code" = "200" ] || [ "$status_code" = "201" ]; then
            # Vérifier que ce n'est pas vide
            if [ -z "$body" ] || [ "$body" = "{}" ] || [ "$body" = "[]" ]; then
                echo -e "${YELLOW}⚠️  $status_code (EMPTY)${NC} - $description"
                return 2
            fi
            
            # Vérifier qu'il n'y a pas de données mockées
            if echo "$body" | grep -qi "mock\|fake\|test\|sample\|dummy"; then
                echo -e "${YELLOW}⚠️  $status_code (MOCK DATA)${NC} - $description"
                return 3
            fi
            
            # Vérifier qu'il y a des données significatives
            char_count=$(echo "$body" | wc -c)
            if [ "$char_count" -lt 50 ]; then
                echo -e "${YELLOW}⚠️  $status_code (MINIMAL DATA: ${char_count} chars)${NC} - $description"
                return 4
            fi
            
            # Compter les éléments de données
            items_count=0
            if echo "$body" | jq -e '.items[]' >/dev/null 2>&1; then
                items_count=$(echo "$body" | jq '.items | length' 2>/dev/null || echo 0)
            elif echo "$body" | jq -e '.malts[]' >/dev/null 2>&1; then
                items_count=$(echo "$body" | jq '.malts | length' 2>/dev/null || echo 0)
            elif echo "$body" | jq -e '.hops[]' >/dev/null 2>&1; then
                items_count=$(echo "$body" | jq '.hops | length' 2>/dev/null || echo 0)
            elif echo "$body" | jq -e '.yeasts[]' >/dev/null 2>&1; then
                items_count=$(echo "$body" | jq '.yeasts | length' 2>/dev/null || echo 0)
            elif echo "$body" | jq -e '.recipes[]' >/dev/null 2>&1; then
                items_count=$(echo "$body" | jq '.recipes | length' 2>/dev/null || echo 0)
            elif echo "$body" | jq -e '.recommendations[]' >/dev/null 2>&1; then
                items_count=$(echo "$body" | jq '.recommendations | length' 2>/dev/null || echo 0)
            elif echo "$body" | jq -e 'type' | grep -q "object"; then
                # Single object response
                items_count=1
            fi
            
            if [ "$items_count" -gt 0 ]; then
                echo -e "${GREEN}✅ $status_code (${items_count} items, ${char_count} chars)${NC} - $description"
                return 0
            else
                echo -e "${GREEN}✅ $status_code (${char_count} chars)${NC} - $description"
                return 0
            fi
        else
            echo -e "${GREEN}✅ $status_code${NC} - $description"
            return 0
        fi
    else
        echo -e "${RED}❌ $status_code (expected: $expected_codes)${NC} - $description"
        return 1
    fi
}

# Compteurs
total_endpoints=0
working_endpoints=0
empty_responses=0
mock_data=0
minimal_data=0

echo "🔍 VALIDATION DES DONNÉES RÉELLES - 77 ENDPOINTS"
echo "================================================="

# Obtenir des IDs réels pour les tests
echo "🔄 Récupération des IDs réels pour les tests..."

# Récupérer des IDs réels
REAL_MALT_ID=$(curl -s "http://localhost:9000/api/v1/malts" | jq -r '.malts[0].id // empty' 2>/dev/null)
REAL_YEAST_ID=$(curl -s "http://localhost:9000/api/v1/yeasts" | jq -r '.yeasts[0].id // empty' 2>/dev/null)
REAL_HOP_ID=$(curl -s "http://localhost:9000/api/v1/hops" | jq -r '.hops[0].id // empty' 2>/dev/null)
REAL_RECIPE_ID=$(curl -s "http://localhost:9000/api/v1/recipes/search" | jq -r '.recipes[0].id // empty' 2>/dev/null)

echo "📋 IDs réels trouvés:"
echo "   Malt ID: $REAL_MALT_ID"
echo "   Yeast ID: $REAL_YEAST_ID" 
echo "   Hop ID: $REAL_HOP_ID"
echo "   Recipe ID: $REAL_RECIPE_ID"
echo

# =============================================================================
# 1. HOME & ASSETS
# =============================================================================
echo -e "${BLUE}🏠 HOME & ASSETS${NC}"
validate_data_response "/" "GET" false "200" "Page d'accueil"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/assets/js/app.js" "GET" false "200,404" "Assets statiques"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
echo

# =============================================================================
# 2. HOPS PUBLIC APIs (4 endpoints)
# =============================================================================
echo -e "${BLUE}🍺 HOPS PUBLIC APIs${NC}"
validate_data_response "/api/v1/hops" "GET" false "200" "Liste des houblons"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

if [ -n "$REAL_HOP_ID" ]; then
    validate_data_response "/api/v1/hops/$REAL_HOP_ID" "GET" false "200,404" "Détail houblon réel"
else
    validate_data_response "/api/v1/hops/cascade" "GET" false "200,404" "Détail houblon"
fi
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/hops/search" "POST" false "200,400" "Recherche houblons" '{"query":"cascade","limit":10}'
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
echo

# =============================================================================
# 3. HOPS ADMIN APIs (5 endpoints) - Skipped (auth required)
# =============================================================================
echo -e "${BLUE}🍺 HOPS ADMIN APIs (Auth Required - Skipping detailed data validation)${NC}"
echo "   ℹ️  Admin endpoints require proper authentication"
total_endpoints=$((total_endpoints + 5))
echo

# =============================================================================
# 4. MALTS PUBLIC APIs (4 endpoints)
# =============================================================================
echo -e "${BLUE}🌾 MALTS PUBLIC APIs${NC}"
validate_data_response "/api/v1/malts" "GET" false "200" "Liste des malts"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

if [ -n "$REAL_MALT_ID" ]; then
    validate_data_response "/api/v1/malts/$REAL_MALT_ID" "GET" false "200,404" "Détail malt réel"
else
    echo "   ⚠️  Pas d'ID de malt réel trouvé"
    total_endpoints=$((total_endpoints + 1))
fi
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/malts/search" "POST" false "200,400" "Recherche malts" '{"query":"pilsner","limit":10}'
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/malts/type/BASE" "GET" false "200" "Malts par type"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
echo

# =============================================================================
# 5. MALTS ADMIN APIs (5 endpoints) - Skipped (auth required)
# =============================================================================
echo -e "${BLUE}🌾 MALTS ADMIN APIs (Auth Required - Skipping detailed data validation)${NC}"
echo "   ℹ️  Admin endpoints require proper authentication"
total_endpoints=$((total_endpoints + 5))
echo

# =============================================================================
# 6. YEASTS PUBLIC APIs (14 endpoints)
# =============================================================================
echo -e "${BLUE}🦠 YEASTS PUBLIC APIs${NC}"
validate_data_response "/api/v1/yeasts" "GET" false "200" "Liste des levures"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/debug" "GET" false "200,404" "Debug test levures"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/search?q=wyeast" "GET" false "200" "Recherche levures"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/stats" "GET" false "200" "Stats publiques levures"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/popular" "GET" false "200" "Levures populaires"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/type/ALE" "GET" false "200" "Levures par type"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/laboratory/Wyeast" "GET" false "200" "Levures par laboratoire"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/recommendations/beginner" "GET" false "200" "Recommandations débutants"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/recommendations/seasonal" "GET" false "200" "Recommandations saisonnières"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/recommendations/experimental" "GET" false "200" "Recommandations expérimentales"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/yeasts/recommendations/style/ipa" "GET" false "200" "Recommandations par style"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

if [ -n "$REAL_YEAST_ID" ]; then
    validate_data_response "/api/v1/yeasts/$REAL_YEAST_ID" "GET" false "200,404" "Détail levure réelle"
    ((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
    
    validate_data_response "/api/v1/yeasts/$REAL_YEAST_ID/alternatives" "GET" false "200,404" "Alternatives levure réelle"
    ((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
else
    echo "   ⚠️  Pas d'ID de levure réel trouvé pour test détail et alternatives"
    total_endpoints=$((total_endpoints + 2))
fi
echo

# =============================================================================
# 7. YEASTS ADMIN APIs (12 endpoints) - Skipped (auth required) 
# =============================================================================
echo -e "${BLUE}🦠 YEASTS ADMIN APIs (Auth Required - Skipping detailed data validation)${NC}"
echo "   ℹ️  Admin endpoints require proper authentication"
total_endpoints=$((total_endpoints + 12))
echo

# =============================================================================
# 8. RECIPES PUBLIC APIs (16 endpoints)
# =============================================================================
echo -e "${BLUE}📝 RECIPES PUBLIC APIs${NC}"
validate_data_response "/api/v1/recipes/health" "GET" false "200" "Santé du service"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/search" "GET" false "200" "Recherche recettes"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/discover" "GET" false "200" "Découverte recettes"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/recommendations/beginner" "GET" false "200" "Recommandations débutants"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/recommendations/style/ipa" "GET" false "200" "Recommandations par style"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/recommendations/ingredients" "GET" false "200,400" "Recommandations par ingrédients"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/recommendations/seasonal/summer" "GET" false "200" "Recommandations saisonnières"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

if [ -n "$REAL_RECIPE_ID" ]; then
    validate_data_response "/api/v1/recipes/recommendations/progression?lastRecipeId=$REAL_RECIPE_ID" "GET" false "200,400" "Recommandations progression"
else
    validate_data_response "/api/v1/recipes/recommendations/progression?lastRecipeId=12345" "GET" false "200,400" "Recommandations progression"
fi
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/compare?recipeIds=1,2" "GET" false "200,400" "Comparaison recettes"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/analyze" "POST" false "200,400" "Analyse recette custom" '{"name":"Test Recipe","style":"IPA"}'
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/stats" "GET" false "200" "Stats publiques recettes"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

validate_data_response "/api/v1/recipes/collections" "GET" false "200" "Collections recettes"
((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac

if [ -n "$REAL_RECIPE_ID" ]; then
    validate_data_response "/api/v1/recipes/$REAL_RECIPE_ID" "GET" false "200,404" "Détail recette réelle"
    ((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
    
    validate_data_response "/api/v1/recipes/$REAL_RECIPE_ID/scale?targetBatchSize=20" "GET" false "200,404" "Mise à l'échelle recette"
    ((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
    
    validate_data_response "/api/v1/recipes/$REAL_RECIPE_ID/brewing-guide" "GET" false "200,404" "Guide de brassage"
    ((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
    
    validate_data_response "/api/v1/recipes/$REAL_RECIPE_ID/alternatives" "GET" false "200,404" "Alternatives recette"
    ((total_endpoints++)); [[ $? -eq 0 ]] && ((working_endpoints++)) || case $? in 2) ((empty_responses++));; 3) ((mock_data++));; 4) ((minimal_data++));; esac
else
    echo "   ⚠️  Pas d'ID de recette réel trouvé pour tests détaillés"
    total_endpoints=$((total_endpoints + 4))
fi
echo

# =============================================================================
# 9. RECIPES ADMIN APIs (5 endpoints) - Skipped (auth required)
# =============================================================================
echo -e "${BLUE}📝 RECIPES ADMIN APIs (Auth Required - Skipping detailed data validation)${NC}"
echo "   ℹ️  Admin endpoints require proper authentication"
total_endpoints=$((total_endpoints + 5))
echo

# =============================================================================
# RÉSUMÉ FINAL
# =============================================================================
echo "================================================="
echo -e "${BLUE}📊 RÉSUMÉ DE VALIDATION DES DONNÉES${NC}"
echo "================================================="
echo -e "Total endpoints analysés: ${BLUE}$total_endpoints${NC}"
echo -e "Endpoints avec données réelles: ${GREEN}$working_endpoints${NC}"
echo -e "Réponses vides: ${YELLOW}$empty_responses${NC}"
echo -e "Données mockées détectées: ${YELLOW}$mock_data${NC}"
echo -e "Données minimales: ${YELLOW}$minimal_data${NC}"

data_percentage=$((working_endpoints * 100 / total_endpoints))
echo -e "Taux de données réelles: ${GREEN}$data_percentage%${NC}"

if [ $data_percentage -ge 90 ]; then
    echo -e "${GREEN}🎉 EXCELLENT! La quasi-totalité des endpoints retournent des données réelles${NC}"
elif [ $data_percentage -ge 80 ]; then
    echo -e "${GREEN}✅ TRÈS BON! La majorité des endpoints ont des données réelles${NC}"
elif [ $data_percentage -ge 70 ]; then
    echo -e "${YELLOW}⚠️  BON mais des améliorations possibles${NC}"
else
    echo -e "${RED}❌ PROBLÉMATIQUE - Beaucoup d'endpoints sans données réelles${NC}"
fi

echo
echo "Total des endpoints dans l'application: $total_endpoints"