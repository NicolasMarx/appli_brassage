#!/bin/bash

# =============================================================================
# SCRIPT DE TEST COMPLET - APIs MALTS
# =============================================================================
# Teste toutes les APIs du domaine Malts
# =============================================================================

BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🌾 TEST COMPLET APIs MALTS${NC}"
echo -e "${BLUE}===========================${NC}"

# Fonction utilitaire pour tester une API
test_api() {
    local name="$1"
    local method="$2"
    local url="$3"
    local data="$4"
    local expected_field="$5"
    
    echo -e "\n${YELLOW}🔍 Test: $name${NC}"
    echo "URL: $method $url"
    
    if [ "$method" = "POST" ]; then
        echo "Data: $data"
        response=$(curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url" 2>/dev/null || echo "ERROR")
    else
        response=$(curl -s "$url" 2>/dev/null || echo "ERROR")
    fi
    
    if [ "$response" = "ERROR" ]; then
        echo -e "${RED}❌ Erreur de connexion${NC}"
        return 1
    fi
    
    echo "Response: $response"
    
    # Vérifier si c'est du JSON valide
    if echo "$response" | jq . >/dev/null 2>&1; then
        if [ -n "$expected_field" ]; then
            field_value=$(echo "$response" | jq -r "$expected_field // \"null\"")
            if [ "$field_value" != "null" ] && [ "$field_value" != "" ]; then
                echo -e "${GREEN}✅ Success - $expected_field: $field_value${NC}"
                return 0
            else
                echo -e "${YELLOW}⚠️  JSON valide mais field '$expected_field' manquant${NC}"
                return 1
            fi
        else
            echo -e "${GREEN}✅ Success - JSON valide${NC}"
            return 0
        fi
    else
        echo -e "${RED}❌ Réponse non-JSON ou erreur${NC}"
        return 1
    fi
}

# Variables pour stocker les résultats
tests_passed=0
tests_failed=0
first_malt_id=""

echo -e "\n${BLUE}Vérification que l'application est démarrée...${NC}"
health_check=$(curl -s "${BASE_URL}/api/admin/malts" 2>/dev/null || echo "ERROR")
if [ "$health_check" = "ERROR" ]; then
    echo -e "${RED}❌ Application non accessible sur ${BASE_URL}${NC}"
    echo -e "${YELLOW}Démarrez l'application avec: sbt run${NC}"
    exit 1
else
    echo -e "${GREEN}✅ Application accessible${NC}"
fi

# =============================================================================
# TEST 1 : API ADMIN - LISTE
# =============================================================================

if test_api "API Admin - Liste" "GET" "${BASE_URL}/api/admin/malts" "" ".totalCount"; then
    tests_passed=$((tests_passed + 1))
    admin_response="$response"
else
    tests_failed=$((tests_failed + 1))
    admin_response=""
fi

# =============================================================================
# TEST 2 : API PUBLIQUE - LISTE
# =============================================================================

if test_api "API Publique - Liste" "GET" "${BASE_URL}/api/v1/malts" "" ".totalCount"; then
    tests_passed=$((tests_passed + 1))
    public_response="$response"
    # Extraire le premier malt ID pour les tests suivants
    if [ -n "$public_response" ]; then
        first_malt_id=$(echo "$public_response" | jq -r '.malts[0].id // empty' 2>/dev/null)
    fi
else
    tests_failed=$((tests_failed + 1))
    public_response=""
fi

# =============================================================================
# TEST 3 : API PUBLIQUE - DÉTAIL (si on a trouvé un ID)
# =============================================================================

if [ -n "$first_malt_id" ] && [ "$first_malt_id" != "null" ] && [ "$first_malt_id" != "" ]; then
    if test_api "API Publique - Détail" "GET" "${BASE_URL}/api/v1/malts/$first_malt_id" "" ".name"; then
        tests_passed=$((tests_passed + 1))
    else
        tests_failed=$((tests_failed + 1))
    fi
else
    echo -e "\n${YELLOW}🔍 Test: API Publique - Détail${NC}"
    echo -e "${YELLOW}⚠️  Aucun malt trouvé pour tester le détail${NC}"
    tests_failed=$((tests_failed + 1))
fi

# =============================================================================
# TEST 4 : API PUBLIQUE - RECHERCHE
# =============================================================================

search_data='{
  "name": "malt",
  "minEbc": 0,
  "maxEbc": 100,
  "page": 0,
  "size": 10
}'

if test_api "API Publique - Recherche" "POST" "${BASE_URL}/api/v1/malts/search" "$search_data" ".malts"; then
    tests_passed=$((tests_passed + 1))
else
    tests_failed=$((tests_failed + 1))
fi

# =============================================================================
# TEST 5 : API PUBLIQUE - MALTS PAR TYPE BASE
# =============================================================================

if test_api "API Publique - Type BASE" "GET" "${BASE_URL}/api/v1/malts/type/BASE" "" ".malts"; then
    tests_passed=$((tests_passed + 1))
else
    tests_failed=$((tests_failed + 1))
fi

# =============================================================================
# TEST 6 : API PUBLIQUE - MALTS PAR TYPE SPECIALTY
# =============================================================================

if test_api "API Publique - Type SPECIALTY" "GET" "${BASE_URL}/api/v1/malts/type/SPECIALTY" "" ".malts"; then
    tests_passed=$((tests_passed + 1))
else
    tests_failed=$((tests_failed + 1))
fi

# =============================================================================
# TEST 7 : API PUBLIQUE - TYPE INVALIDE (doit échouer)
# =============================================================================

echo -e "\n${YELLOW}🔍 Test: API Publique - Type Invalide (doit échouer)${NC}"
echo "URL: GET ${BASE_URL}/api/v1/malts/type/INVALID_TYPE"

invalid_response=$(curl -s "${BASE_URL}/api/v1/malts/type/INVALID_TYPE" 2>/dev/null || echo "ERROR")
echo "Response: $invalid_response"

if [ "$invalid_response" != "ERROR" ] && echo "$invalid_response" | jq . >/dev/null 2>&1; then
    error_field=$(echo "$invalid_response" | jq -r '.error // empty')
    if [ "$error_field" = "invalid_type" ]; then
        echo -e "${GREEN}✅ Success - Erreur attendue pour type invalide${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}❌ Devrait retourner une erreur invalid_type${NC}"
        tests_failed=$((tests_failed + 1))
    fi
else
    echo -e "${RED}❌ Erreur de connexion ou réponse invalide${NC}"
    tests_failed=$((tests_failed + 1))
fi

# =============================================================================
# RÉSUMÉ DES TESTS
# =============================================================================

echo -e "\n${BLUE}📊 RÉSUMÉ DES TESTS${NC}"
echo "=================="

total_tests=$((tests_passed + tests_failed))
echo "Total tests: $total_tests"
echo -e "Réussis: ${GREEN}$tests_passed${NC}"
echo -e "Échoués: ${RED}$tests_failed${NC}"

if [ $tests_failed -eq 0 ]; then
    echo -e "\n${GREEN}🎉 TOUS LES TESTS SONT PASSÉS !${NC}"
    echo -e "${GREEN}Le domaine Malts est complètement fonctionnel !${NC}"
    
    # Afficher quelques statistiques si on a des données
    if [ -n "$public_response" ]; then
        malt_count=$(echo "$public_response" | jq -r '.totalCount // 0')
        malts_in_page=$(echo "$public_response" | jq -r '.malts | length // 0')
        echo -e "\n${BLUE}📈 Statistiques:${NC}"
        echo "• Malts actifs: $malt_count"
        echo "• Malts dans cette page: $malts_in_page"
        
        # Afficher le nom du premier malt si disponible
        if [ -n "$first_malt_id" ]; then
            first_malt_name=$(echo "$public_response" | jq -r '.malts[0].name // "Unknown"')
            echo "• Premier malt: $first_malt_name (ID: $first_malt_id)"
        fi
    fi
    
else
    echo -e "\n${RED}⚠️  CERTAINS TESTS ONT ÉCHOUÉ${NC}"
    echo -e "${YELLOW}Vérifiez les logs de l'application (console sbt run) pour plus de détails${NC}"
    
    if [ $tests_passed -gt 0 ]; then
        echo -e "${BLUE}Les APIs qui fonctionnent peuvent être utilisées normalement.${NC}"
    fi
fi

echo -e "\n${BLUE}🔍 URLs testées:${NC}"
echo "• GET    /api/admin/malts              (API admin)"
echo "• GET    /api/v1/malts                 (liste publique)"
echo "• GET    /api/v1/malts/:id             (détail)"
echo "• POST   /api/v1/malts/search          (recherche)"
echo "• GET    /api/v1/malts/type/BASE       (par type)"
echo "• GET    /api/v1/malts/type/SPECIALTY  (par type)"

echo -e "\n${YELLOW}💡 Pour débugger:${NC}"
echo "• Regardez les logs dans la console où 'sbt run' tourne"
echo "• Les logs commencent par 🌾, 🔍, ✅, ou ❌"
echo "• Vérifiez que les 3 malts de test sont bien en base"

exit $tests_failed
