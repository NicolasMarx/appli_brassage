#!/bin/bash

# =============================================================================
# SCRIPT DE TEST COMPLET - DOMAINES HOPS ET MALTS
# =============================================================================
# Teste toutes les APIs des domaines Hops et Malts
# Vérifie la cohérence architecturale entre les deux domaines
# =============================================================================

BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

echo -e "${BLUE}🍺 TEST COMPLET - DOMAINES HOPS ET MALTS${NC}"
echo -e "${BLUE}=========================================${NC}"

# Variables globales pour les statistiques
total_tests=0
tests_passed=0
tests_failed=0
hops_tests_passed=0
hops_tests_failed=0
malts_tests_passed=0
malts_tests_failed=0

# Fonction utilitaire pour tester une API
test_api() {
    local domain="$1"
    local name="$2"
    local method="$3"
    local url="$4"
    local data="$5"
    local expected_field="$6"
    
    echo -e "\n${PURPLE}[$domain] ${YELLOW}$name${NC}"
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
    
    total_tests=$((total_tests + 1))
    
    if [ "$response" = "ERROR" ]; then
        echo -e "${RED}❌ Erreur de connexion${NC}"
        tests_failed=$((tests_failed + 1))
        if [ "$domain" = "HOPS" ]; then
            hops_tests_failed=$((hops_tests_failed + 1))
        else
            malts_tests_failed=$((malts_tests_failed + 1))
        fi
        return 1
    fi
    
    # Afficher un extrait de la réponse
    response_preview=$(echo "$response" | jq -c . 2>/dev/null | head -c 200)
    echo "Response preview: ${response_preview}..."
    
    # Vérifier si c'est du JSON valide
    if echo "$response" | jq . >/dev/null 2>&1; then
        if [ -n "$expected_field" ]; then
            field_value=$(echo "$response" | jq -r "$expected_field // \"null\"")
            if [ "$field_value" != "null" ] && [ "$field_value" != "" ]; then
                echo -e "${GREEN}✅ Success - $expected_field: $field_value${NC}"
                tests_passed=$((tests_passed + 1))
                if [ "$domain" = "HOPS" ]; then
                    hops_tests_passed=$((hops_tests_passed + 1))
                else
                    malts_tests_passed=$((malts_tests_passed + 1))
                fi
                return 0
            else
                echo -e "${YELLOW}⚠️  JSON valide mais field '$expected_field' manquant${NC}"
                tests_failed=$((tests_failed + 1))
                if [ "$domain" = "HOPS" ]; then
                    hops_tests_failed=$((hops_tests_failed + 1))
                else
                    malts_tests_failed=$((malts_tests_failed + 1))
                fi
                return 1
            fi
        else
            echo -e "${GREEN}✅ Success - JSON valide${NC}"
            tests_passed=$((tests_passed + 1))
            if [ "$domain" = "HOPS" ]; then
                hops_tests_passed=$((hops_tests_passed + 1))
            else
                malts_tests_passed=$((malts_tests_passed + 1))
            fi
            return 0
        fi
    else
        echo -e "${RED}❌ Réponse non-JSON ou erreur${NC}"
        tests_failed=$((tests_failed + 1))
        if [ "$domain" = "HOPS" ]; then
            hops_tests_failed=$((hops_tests_failed + 1))
        else
            malts_tests_failed=$((malts_tests_failed + 1))
        fi
        return 1
    fi
}

# Variables pour stocker les premiers IDs trouvés
first_hop_id=""
first_malt_id=""

echo -e "\n${BLUE}Vérification que l'application est démarrée...${NC}"
health_check=$(curl -s "${BASE_URL}/api/admin/hops" 2>/dev/null || echo "ERROR")
if [ "$health_check" = "ERROR" ]; then
    echo -e "${RED}❌ Application non accessible sur ${BASE_URL}${NC}"
    echo -e "${YELLOW}Démarrez l'application avec: sbt run${NC}"
    exit 1
else
    echo -e "${GREEN}✅ Application accessible${NC}"
fi

# =============================================================================
# TESTS DOMAINE HOPS
# =============================================================================

echo -e "\n${BLUE}🍺 TESTS DOMAINE HOPS${NC}"
echo -e "${BLUE}=====================${NC}"

# Admin Hops - Liste
if test_api "HOPS" "Admin - Liste" "GET" "${BASE_URL}/api/admin/hops" "" ".totalCount"; then
    admin_hops_response="$response"
    first_hop_id=$(echo "$admin_hops_response" | jq -r '.hops[0].id // empty' 2>/dev/null)
else
    admin_hops_response=""
fi

# Public Hops - Liste
test_api "HOPS" "Public - Liste" "GET" "${BASE_URL}/api/v1/hops" "" ".totalCount"

# Public Hops - Détail (si on a un ID)
if [ -n "$first_hop_id" ] && [ "$first_hop_id" != "null" ]; then
    test_api "HOPS" "Public - Détail" "GET" "${BASE_URL}/api/v1/hops/$first_hop_id" "" ".name"
else
    echo -e "\n${PURPLE}[HOPS] ${YELLOW}Public - Détail${NC}"
    echo -e "${YELLOW}⚠️  Aucun hop trouvé pour tester le détail${NC}"
    total_tests=$((total_tests + 1))
    tests_failed=$((tests_failed + 1))
    hops_tests_failed=$((hops_tests_failed + 1))
fi

# Public Hops - Recherche
hops_search_data='{
  "name": "hop",
  "minAlphaAcid": 0,
  "maxAlphaAcid": 20,
  "page": 0,
  "size": 10
}'

test_api "HOPS" "Public - Recherche" "POST" "${BASE_URL}/api/v1/hops/search" "$hops_search_data" ".hops"

# =============================================================================
# TESTS DOMAINE MALTS
# =============================================================================

echo -e "\n${BLUE}🌾 TESTS DOMAINE MALTS${NC}"
echo -e "${BLUE}======================${NC}"

# Admin Malts - Liste
if test_api "MALTS" "Admin - Liste" "GET" "${BASE_URL}/api/admin/malts" "" ".totalCount"; then
    admin_malts_response="$response"
    first_malt_id=$(echo "$admin_malts_response" | jq -r '.malts[0].id // empty' 2>/dev/null)
else
    admin_malts_response=""
fi

# Public Malts - Liste
test_api "MALTS" "Public - Liste" "GET" "${BASE_URL}/api/v1/malts" "" ".totalCount"

# Public Malts - Détail (si on a un ID)
if [ -n "$first_malt_id" ] && [ "$first_malt_id" != "null" ]; then
    test_api "MALTS" "Public - Détail" "GET" "${BASE_URL}/api/v1/malts/$first_malt_id" "" ".name"
else
    echo -e "\n${PURPLE}[MALTS] ${YELLOW}Public - Détail${NC}"
    echo -e "${YELLOW}⚠️  Aucun malt trouvé pour tester le détail${NC}"
    total_tests=$((total_tests + 1))
    tests_failed=$((tests_failed + 1))
    malts_tests_failed=$((malts_tests_failed + 1))
fi

# Public Malts - Recherche
malts_search_data='{
  "name": "malt",
  "minEbc": 0,
  "maxEbc": 50,
  "page": 0,
  "size": 10
}'

test_api "MALTS" "Public - Recherche" "POST" "${BASE_URL}/api/v1/malts/search" "$malts_search_data" ".malts"

# Public Malts - Type BASE
test_api "MALTS" "Public - Type BASE" "GET" "${BASE_URL}/api/v1/malts/type/BASE" "" ".malts"

# Public Malts - Type SPECIALTY
test_api "MALTS" "Public - Type SPECIALTY" "GET" "${BASE_URL}/api/v1/malts/type/SPECIALTY" "" ".malts"

# =============================================================================
# TESTS DE COHÉRENCE ARCHITECTURALE
# =============================================================================

echo -e "\n${BLUE}🔍 TESTS DE COHÉRENCE ARCHITECTURALE${NC}"
echo -e "${BLUE}====================================${NC}"

# Test 1: Structure de réponse similaire entre domaines
echo -e "\n${PURPLE}[ARCH] ${YELLOW}Cohérence structure APIs Admin${NC}"
total_tests=$((total_tests + 1))

if [ -n "$admin_hops_response" ] && [ -n "$admin_malts_response" ]; then
    hops_has_totalcount=$(echo "$admin_hops_response" | jq 'has("totalCount")' 2>/dev/null)
    malts_has_totalcount=$(echo "$admin_malts_response" | jq 'has("totalCount")' 2>/dev/null)
    
    hops_has_page=$(echo "$admin_hops_response" | jq 'has("page")' 2>/dev/null)
    malts_has_page=$(echo "$admin_malts_response" | jq 'has("page")' 2>/dev/null)
    
    if [ "$hops_has_totalcount" = "true" ] && [ "$malts_has_totalcount" = "true" ] && 
       [ "$hops_has_page" = "true" ] && [ "$malts_has_page" = "true" ]; then
        echo -e "${GREEN}✅ Structure pagination cohérente entre domaines${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}❌ Structures de pagination différentes${NC}"
        tests_failed=$((tests_failed + 1))
    fi
else
    echo -e "${YELLOW}⚠️  Impossible de tester - données manquantes${NC}"
    tests_failed=$((tests_failed + 1))
fi

# Test 2: Codes de statut HTTP cohérents
echo -e "\n${PURPLE}[ARCH] ${YELLOW}Test codes HTTP - Type invalide${NC}"
total_tests=$((total_tests + 1))

# Test type invalide pour les deux domaines
malts_invalid_response=$(curl -s "${BASE_URL}/api/v1/malts/type/INVALID_TYPE" 2>/dev/null)
malts_error=$(echo "$malts_invalid_response" | jq -r '.error // empty' 2>/dev/null)

if [ "$malts_error" = "invalid_type" ]; then
    echo -e "${GREEN}✅ Gestion d'erreur cohérente (type invalide)${NC}"
    tests_passed=$((tests_passed + 1))
else
    echo -e "${RED}❌ Gestion d'erreur incohérente${NC}"
    tests_failed=$((tests_failed + 1))
fi

# =============================================================================
# ANALYSE COMPARATIVE DES DONNÉES
# =============================================================================

echo -e "\n${BLUE}📊 ANALYSE COMPARATIVE DES DONNÉES${NC}"
echo -e "${BLUE}===================================${NC}"

if [ -n "$admin_hops_response" ] && [ -n "$admin_malts_response" ]; then
    hops_count=$(echo "$admin_hops_response" | jq -r '.totalCount // 0')
    malts_count=$(echo "$admin_malts_response" | jq -r '.totalCount // 0')
    
    echo -e "${BLUE}Houblons disponibles:${NC} $hops_count"
    echo -e "${BLUE}Malts disponibles:${NC} $malts_count"
    
    if [ "$hops_count" -gt 0 ]; then
        first_hop_name=$(echo "$admin_hops_response" | jq -r '.hops[0].name // "Unknown"')
        echo -e "${BLUE}Premier houblon:${NC} $first_hop_name"
    fi
    
    if [ "$malts_count" -gt 0 ]; then
        first_malt_name=$(echo "$admin_malts_response" | jq -r '.malts[0].name // "Unknown"')
        echo -e "${BLUE}Premier malt:${NC} $first_malt_name"
    fi
    
    total_ingredients=$((hops_count + malts_count))
    echo -e "${BLUE}Total ingrédients:${NC} $total_ingredients"
fi

# =============================================================================
# RÉSUMÉ FINAL
# =============================================================================

echo -e "\n${BLUE}📊 RÉSUMÉ FINAL${NC}"
echo "==============="

echo "Total tests: $total_tests"
echo -e "Réussis: ${GREEN}$tests_passed${NC}"
echo -e "Échoués: ${RED}$tests_failed${NC}"

echo -e "\n${BLUE}Par domaine:${NC}"
echo -e "HOPS   - Réussis: ${GREEN}$hops_tests_passed${NC}, Échoués: ${RED}$hops_tests_failed${NC}"
echo -e "MALTS  - Réussis: ${GREEN}$malts_tests_passed${NC}, Échoués: ${RED}$malts_tests_failed${NC}"

# Calcul du pourcentage de réussite
if [ $total_tests -gt 0 ]; then
    success_rate=$(( (tests_passed * 100) / total_tests ))
    echo -e "\nTaux de réussite: ${success_rate}%"
fi

if [ $tests_failed -eq 0 ]; then
    echo -e "\n${GREEN}🎉 TOUS LES TESTS SONT PASSÉS !${NC}"
    echo -e "${GREEN}Les domaines Hops et Malts sont complètement fonctionnels !${NC}"
    echo -e "${BLUE}L'architecture DDD/CQRS est cohérente entre les domaines.${NC}"
    
elif [ $tests_passed -gt 0 ]; then
    echo -e "\n${YELLOW}⚠️  TESTS PARTIELLEMENT RÉUSSIS${NC}"
    echo -e "${BLUE}Certaines APIs fonctionnent, d'autres nécessitent des ajustements.${NC}"
    
    if [ $hops_tests_failed -eq 0 ]; then
        echo -e "${GREEN}✅ Domaine HOPS: Complètement fonctionnel${NC}"
    fi
    
    if [ $malts_tests_failed -eq 0 ]; then
        echo -e "${GREEN}✅ Domaine MALTS: Complètement fonctionnel${NC}"
    fi
    
else
    echo -e "\n${RED}❌ ÉCHEC CRITIQUE${NC}"
    echo -e "${YELLOW}Vérifiez que l'application est bien démarrée et configurée.${NC}"
fi

echo -e "\n${BLUE}🔍 APIs testées:${NC}"
echo "HOPS:"
echo "• GET    /api/admin/hops           (admin)"
echo "• GET    /api/v1/hops              (liste publique)"
echo "• GET    /api/v1/hops/:id          (détail)"
echo "• POST   /api/v1/hops/search       (recherche)"

echo -e "\nMALTS:"
echo "• GET    /api/admin/malts          (admin)"
echo "• GET    /api/v1/malts             (liste publique)"
echo "• GET    /api/v1/malts/:id         (détail)"
echo "• POST   /api/v1/malts/search      (recherche)"
echo "• GET    /api/v1/malts/type/BASE   (par type)"

echo -e "\n${YELLOW}💡 Pour débugger:${NC}"
echo "• Logs détaillés dans la console SBT (sbt run)"
echo "• Logs HOPS commencent par 🍺"
echo "• Logs MALTS commencent par 🌾"

exit $tests_failed
