#!/bin/bash

# =============================================================================
# SCRIPT 1 : TEST RAPIDE D'ALIGNEMENT
# =============================================================================

cat > test_aligned_domains.sh << 'EOF'
#!/bin/bash

# Test rapide pour vérifier l'alignement Hops/Malts
BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔍 TEST RAPIDE - ALIGNEMENT DOMAINES${NC}"
echo "===================================="

# Test 1: Format Hops aligné
echo -e "\n${YELLOW}Test 1: Format Hops aligné${NC}"
hops_response=$(curl -s "${BASE_URL}/api/v1/hops" 2>/dev/null || echo "ERROR")

if [ "$hops_response" != "ERROR" ]; then
    # Vérifier nouveau format
    if echo "$hops_response" | jq '.hops' >/dev/null 2>&1; then
        hops_count=$(echo "$hops_response" | jq '.hops | length')
        echo -e "${GREEN}✅ Nouveau format - .hops contient $hops_count houblons${NC}"
        NEW_FORMAT=true
    elif echo "$hops_response" | jq '.items' >/dev/null 2>&1; then
        items_count=$(echo "$hops_response" | jq '.items | length')
        echo -e "${YELLOW}⚠️  Ancien format - .items contient $items_count houblons${NC}"
        NEW_FORMAT=false
    else
        echo -e "${RED}❌ Format JSON non reconnu${NC}"
        NEW_FORMAT=false
    fi
else
    echo -e "${RED}❌ API Hops inaccessible${NC}"
    NEW_FORMAT=false
fi

# Test 2: Recherche Hops
echo -e "\n${YELLOW}Test 2: Recherche Hops${NC}"
search_data='{"name": "cascade", "page": 0, "size": 5}'
search_response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "$search_data" \
    "${BASE_URL}/api/v1/hops/search" 2>/dev/null || echo "ERROR")

if [ "$search_response" != "ERROR" ]; then
    if echo "$search_response" | jq '.hops' >/dev/null 2>&1; then
        search_count=$(echo "$search_response" | jq '.hops | length')
        echo -e "${GREEN}✅ Recherche alignée - .hops contient $search_count résultats${NC}"
    elif echo "$search_response" | jq '.items' >/dev/null 2>&1; then
        items_count=$(echo "$search_response" | jq '.items | length')
        echo -e "${YELLOW}⚠️  Recherche ancien format - .items contient $items_count résultats${NC}"
    else
        echo -e "${RED}❌ Recherche format incorrect${NC}"
    fi
else
    echo -e "${RED}❌ Recherche Hops inaccessible${NC}"
fi

# Test 3: Cohérence avec Malts
echo -e "\n${YELLOW}Test 3: Cohérence avec Malts${NC}"
malts_response=$(curl -s "${BASE_URL}/api/v1/malts" 2>/dev/null || echo "ERROR")

if [ "$malts_response" != "ERROR" ] && [ "$hops_response" != "ERROR" ]; then
    # Vérifier structure pagination
    hops_has_total=$(echo "$hops_response" | jq 'has("totalCount")' 2>/dev/null)
    malts_has_total=$(echo "$malts_response" | jq 'has("totalCount")' 2>/dev/null)
    hops_has_page=$(echo "$hops_response" | jq 'has("page")' 2>/dev/null)
    malts_has_page=$(echo "$malts_response" | jq 'has("page")' 2>/dev/null)
    
    if [ "$hops_has_total" = "true" ] && [ "$malts_has_total" = "true" ] && 
       [ "$hops_has_page" = "true" ] && [ "$malts_has_page" = "true" ]; then
        echo -e "${GREEN}✅ Pagination cohérente${NC}"
        
        # Afficher structures pour comparaison
        echo -e "\n${BLUE}Structures de pagination:${NC}"
        hops_total=$(echo "$hops_response" | jq '.totalCount')
        malts_total=$(echo "$malts_response" | jq '.totalCount')
        hops_page=$(echo "$hops_response" | jq '.page')
        malts_page=$(echo "$malts_response" | jq '.page')
        
        echo "Hops:  totalCount=$hops_total, page=$hops_page"
        echo "Malts: totalCount=$malts_total, page=$malts_page"
    else
        echo -e "${RED}❌ Structures pagination différentes${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Impossible de comparer - APIs non accessibles${NC}"
fi

# Résumé
echo -e "\n${BLUE}📊 RÉSUMÉ${NC}"
echo "=========="

if [ "$NEW_FORMAT" = true ]; then
    echo -e "${GREEN}✅ Domaine Hops aligné sur format Malts${NC}"
    echo -e "  → Utilise .hops au lieu de .items"
    echo -e "  → Structure pagination cohérente"
else
    echo -e "${YELLOW}⚠️  Domaine Hops pas encore aligné${NC}"
    echo -e "  → Utilise encore .items"
    echo -e "  → Nécessite correction"
fi

echo -e "\n${YELLOW}💡 Instructions:${NC}"
echo "• Démarrez l'app: sbt run"
echo "• Si pas aligné, corrigez avec: ./fix-hops-alignment-errors.sh"
echo "• Test complet: ./test-hops-malts-complete.sh"
EOF

chmod +x test_aligned_domains.sh

# =============================================================================
# SCRIPT 2 : TEST COMPLET DOMAINES ALIGNÉS
# =============================================================================

cat > test-hops-malts-complete.sh << 'EOF'
#!/bin/bash

# Test complet des domaines Hops et Malts alignés
BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

echo -e "${BLUE}🎯 TEST COMPLET - DOMAINES ALIGNÉS${NC}"
echo -e "${BLUE}==================================${NC}"

# Variables globales
total_tests=0
tests_passed=0
tests_failed=0
hops_tests_passed=0
hops_tests_failed=0
malts_tests_passed=0
malts_tests_failed=0

# Fonction de test
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
        response=$(curl -s -X POST -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null || echo "ERROR")
    else
        response=$(curl -s "$url" 2>/dev/null || echo "ERROR")
    fi
    
    total_tests=$((total_tests + 1))
    
    if [ "$response" = "ERROR" ]; then
        echo -e "${RED}❌ Connexion échouée${NC}"
        tests_failed=$((tests_failed + 1))
        if [ "$domain" = "HOPS" ]; then hops_tests_failed=$((hops_tests_failed + 1)); else malts_tests_failed=$((malts_tests_failed + 1)); fi
        return 1
    fi
    
    # Preview réponse
    response_preview=$(echo "$response" | jq -c . 2>/dev/null | head -c 150)
    echo "Response: ${response_preview}..."
    
    # Validation JSON
    if echo "$response" | jq . >/dev/null 2>&1; then
        if [ -n "$expected_field" ]; then
            field_value=$(echo "$response" | jq -r "$expected_field // \"null\"")
            if [ "$field_value" != "null" ] && [ "$field_value" != "" ]; then
                echo -e "${GREEN}✅ Success - $expected_field: $field_value${NC}"
                tests_passed=$((tests_passed + 1))
                if [ "$domain" = "HOPS" ]; then hops_tests_passed=$((hops_tests_passed + 1)); else malts_tests_passed=$((malts_tests_passed + 1)); fi
                return 0
            fi
        else
            echo -e "${GREEN}✅ Success - JSON valide${NC}"
            tests_passed=$((tests_passed + 1))
            if [ "$domain" = "HOPS" ]; then hops_tests_passed=$((hops_tests_passed + 1)); else malts_tests_passed=$((malts_tests_passed + 1)); fi
            return 0
        fi
    fi
    
    echo -e "${RED}❌ Échec${NC}"
    tests_failed=$((tests_failed + 1))
    if [ "$domain" = "HOPS" ]; then hops_tests_failed=$((hops_tests_failed + 1)); else malts_tests_failed=$((malts_tests_failed + 1)); fi
    return 1
}

# Vérification application
echo -e "\n${BLUE}Vérification application...${NC}"
health=$(curl -s "${BASE_URL}/api/admin/hops" 2>/dev/null || echo "ERROR")
if [ "$health" = "ERROR" ]; then
    echo -e "${RED}❌ Application inaccessible${NC}"
    echo -e "${YELLOW}Démarrez avec: sbt run${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Application accessible${NC}"

# TESTS HOPS
echo -e "\n${BLUE}🍺 TESTS DOMAINE HOPS${NC}"
echo -e "${BLUE}=====================${NC}"

# Variables pour IDs
first_hop_id=""
first_malt_id=""

# Hops - Admin Liste
if test_api "HOPS" "Admin - Liste" "GET" "${BASE_URL}/api/admin/hops" "" ".totalCount"; then
    admin_hops_response="$response"
    first_hop_id=$(echo "$admin_hops_response" | jq -r '.hops[0].id // empty' 2>/dev/null)
fi

# Hops - Public Liste (NOUVEAU FORMAT)
if test_api "HOPS" "Public - Liste (aligné)" "GET" "${BASE_URL}/api/v1/hops" "" ".totalCount"; then
    public_hops_response="$response"
    
    # Vérifier format aligné
    if echo "$public_hops_response" | jq '.hops' >/dev/null 2>&1; then
        echo -e "${GREEN}    → Format aligné détecté (.hops)${NC}"
    elif echo "$public_hops_response" | jq '.items' >/dev/null 2>&1; then
        echo -e "${YELLOW}    → Ancien format détecté (.items)${NC}"
    fi
fi

# Hops - Détail
if [ -n "$first_hop_id" ]; then
    test_api "HOPS" "Public - Détail" "GET" "${BASE_URL}/api/v1/hops/$first_hop_id" "" ".name"
else
    echo -e "\n${PURPLE}[HOPS] ${YELLOW}Public - Détail${NC}"
    echo -e "${YELLOW}⚠️  Aucun hop trouvé${NC}"
    total_tests=$((total_tests + 1))
    tests_failed=$((tests_failed + 1))
    hops_tests_failed=$((hops_tests_failed + 1))
fi

# Hops - Recherche (NOUVEAU FORMAT)
search_hops='{"name": "cascade", "page": 0, "size": 5}'
if test_api "HOPS" "Public - Recherche (alignée)" "POST" "${BASE_URL}/api/v1/hops/search" "$search_hops" ".totalCount"; then
    # Vérifier format aligné dans recherche
    if echo "$response" | jq '.hops' >/dev/null 2>&1; then
        echo -e "${GREEN}    → Recherche format aligné (.hops)${NC}"
    elif echo "$response" | jq '.items' >/dev/null 2>&1; then
        echo -e "${YELLOW}    → Recherche ancien format (.items)${NC}"
    fi
fi

# TESTS MALTS
echo -e "\n${BLUE}🌾 TESTS DOMAINE MALTS${NC}"
echo -e "${BLUE}======================${NC}"

# Malts - Admin Liste
if test_api "MALTS" "Admin - Liste" "GET" "${BASE_URL}/api/admin/malts" "" ".totalCount"; then
    admin_malts_response="$response"
    first_malt_id=$(echo "$admin_malts_response" | jq -r '.malts[0].id // empty' 2>/dev/null)
fi

# Malts - Public Liste
test_api "MALTS" "Public - Liste" "GET" "${BASE_URL}/api/v1/malts" "" ".totalCount"

# Malts - Détail
if [ -n "$first_malt_id" ]; then
    test_api "MALTS" "Public - Détail" "GET" "${BASE_URL}/api/v1/malts/$first_malt_id" "" ".name"
fi

# Malts - Recherche
search_malts='{"name": "malt", "minEbc": 0, "maxEbc": 20}'
test_api "MALTS" "Public - Recherche" "POST" "${BASE_URL}/api/v1/malts/search" "$search_malts" ".malts"

# Malts - Type
test_api "MALTS" "Public - Type BASE" "GET" "${BASE_URL}/api/v1/malts/type/BASE" "" ".malts"

# TESTS D'ALIGNEMENT
echo -e "\n${BLUE}🔍 TESTS D'ALIGNEMENT${NC}"
echo -e "${BLUE}=====================${NC}"

# Test cohérence structure
echo -e "\n${PURPLE}[ALIGN] ${YELLOW}Cohérence structure JSON${NC}"
total_tests=$((total_tests + 1))

if [ -n "$public_hops_response" ] && [ -n "$admin_malts_response" ]; then
    # Vérifier même structure de pagination
    hops_struct=$(echo "$public_hops_response" | jq 'keys | sort' 2>/dev/null)
    malts_struct=$(echo "$admin_malts_response" | jq 'keys | sort' 2>/dev/null)
    
    hops_has_total=$(echo "$public_hops_response" | jq 'has("totalCount")' 2>/dev/null)
    malts_has_total=$(echo "$admin_malts_response" | jq 'has("totalCount")' 2>/dev/null)
    
    if [ "$hops_has_total" = "true" ] && [ "$malts_has_total" = "true" ]; then
        echo -e "${GREEN}✅ Structures compatibles${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}❌ Structures incompatibles${NC}"
        tests_failed=$((tests_failed + 1))
    fi
else
    echo -e "${YELLOW}⚠️  Données insuffisantes pour comparaison${NC}"
    tests_failed=$((tests_failed + 1))
fi

# Test format de réponse
echo -e "\n${PURPLE}[ALIGN] ${YELLOW}Format de réponse Hops${NC}"
total_tests=$((total_tests + 1))

if [ -n "$public_hops_response" ]; then
    if echo "$public_hops_response" | jq '.hops' >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Format Hops aligné (.hops)${NC}"
        tests_passed=$((tests_passed + 1))
    elif echo "$public_hops_response" | jq '.items' >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  Format Hops ancien (.items)${NC}"
        tests_failed=$((tests_failed + 1))
    else
        echo -e "${RED}❌ Format Hops inconnu${NC}"
        tests_failed=$((tests_failed + 1))
    fi
else
    echo -e "${RED}❌ Pas de données Hops${NC}"
    tests_failed=$((tests_failed + 1))
fi

# ANALYSE COMPARATIVE
echo -e "\n${BLUE}📊 ANALYSE COMPARATIVE${NC}"
echo -e "${BLUE}======================${NC}"

if [ -n "$admin_hops_response" ] && [ -n "$admin_malts_response" ]; then
    hops_count=$(echo "$admin_hops_response" | jq -r '.totalCount // 0')
    malts_count=$(echo "$admin_malts_response" | jq -r '.totalCount // 0')
    total_ingredients=$((hops_count + malts_count))
    
    echo -e "${BLUE}📈 Statistiques ingrédients:${NC}"
    echo "• Houblons: $hops_count"
    echo "• Malts: $malts_count"
    echo "• Total: $total_ingredients ingrédients"
    
    if [ "$hops_count" -gt 0 ] && [ "$malts_count" -gt 0 ]; then
        echo -e "${GREEN}✅ Base de données opérationnelle${NC}"
    fi
fi

# RÉSUMÉ FINAL
echo -e "\n${BLUE}📊 RÉSUMÉ FINAL${NC}"
echo "==============="

echo "Total tests: $total_tests"
echo -e "Réussis: ${GREEN}$tests_passed${NC}"
echo -e "Échoués: ${RED}$tests_failed${NC}"

success_rate=$(( (tests_passed * 100) / total_tests ))
echo -e "\nTaux de réussite: ${success_rate}%"

echo -e "\n${BLUE}Par domaine:${NC}"
echo -e "HOPS   - Réussis: ${GREEN}$hops_tests_passed${NC}, Échoués: ${RED}$hops_tests_failed${NC}"
echo -e "MALTS  - Réussis: ${GREEN}$malts_tests_passed${NC}, Échoués: ${RED}$malts_tests_failed${NC}"

# Évaluation finale
if [ $tests_failed -eq 0 ]; then
    echo -e "\n${GREEN}🎉 PARFAIT - DOMAINES COMPLÈTEMENT ALIGNÉS !${NC}"
    echo -e "${GREEN}Architecture DDD/CQRS cohérente entre Hops et Malts${NC}"
elif [ $success_rate -ge 90 ]; then
    echo -e "\n${GREEN}🎯 EXCELLENT - Domaines quasiment alignés${NC}"
    echo -e "${YELLOW}Quelques ajustements mineurs nécessaires${NC}"
elif [ $success_rate -ge 75 ]; then
    echo -e "\n${YELLOW}⚡ BON - Alignement partiel réussi${NC}"
    echo -e "${BLUE}Certaines APIs nécessitent des corrections${NC}"
else
    echo -e "\n${RED}⚠️  NÉCESSITE CORRECTIONS${NC}"
    echo -e "${YELLOW}Plusieurs APIs nécessitent un alignement${NC}"
fi

echo -e "\n${YELLOW}💡 Prochaines étapes:${NC}"
if echo "$public_hops_response" | jq '.items' >/dev/null 2>&1; then
    echo "• Hops utilise encore .items → Corriger avec ./fix-hops-alignment-errors.sh"
fi
echo "• Vérifier logs SBT pour détails techniques"
echo "• APIs fonctionnelles peuvent être utilisées en production"

exit $tests_failed
EOF

chmod +x test-hops-malts-complete.sh

# =============================================================================
# SCRIPT 3 : TEST DE RÉGRESSION
# =============================================================================

cat > test_regression.sh << 'EOF'
#!/bin/bash

# Test de régression pour vérifier que les corrections n'ont pas cassé l'existant
BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔄 TEST DE RÉGRESSION${NC}"
echo "===================="

# Test que les APIs existantes fonctionnent toujours
echo -e "\n${YELLOW}Test 1: APIs Admin existantes${NC}"

admin_hops=$(curl -s "${BASE_URL}/api/admin/hops" 2>/dev/null || echo "ERROR")
admin_malts=$(curl -s "${BASE_URL}/api/admin/malts" 2>/dev/null || echo "ERROR")

if [ "$admin_hops" != "ERROR" ] && [ "$admin_malts" != "ERROR" ]; then
    echo -e "${GREEN}✅ APIs Admin opérationnelles${NC}"
else
    echo -e "${RED}❌ APIs Admin défaillantes${NC}"
fi

# Test compatibilité données existantes
echo -e "\n${YELLOW}Test 2: Intégrité données${NC}"

if [ "$admin_hops" != "ERROR" ]; then
    hops_count=$(echo "$admin_hops" | jq '.totalCount // 0')
    if [ "$hops_count" -gt 0 ]; then
        echo -e "${GREEN}✅ Données Hops intègres ($hops_count houblons)${NC}"
    else
        echo -e "${RED}❌ Données Hops perdues${NC}"
    fi
fi

if [ "$admin_malts" != "ERROR" ]; then
    malts_count=$(echo "$admin_malts" | jq '.totalCount // 0')
    if [ "$malts_count" -gt 0 ]; then
        echo -e "${GREEN}✅ Données Malts intègres ($malts_count malts)${NC}"
    else
        echo -e "${RED}❌ Données Malts perdues${NC}"
    fi
fi

# Test performances
echo -e "\n${YELLOW}Test 3: Performance APIs${NC}"

start_time=$(date +%s%N)
curl -s "${BASE_URL}/api/v1/hops" >/dev/null 2>&1
end_time=$(date +%s%N)
hops_time=$(( (end_time - start_time) / 1000000 ))

start_time=$(date +%s%N)
curl -s "${BASE_URL}/api/v1/malts" >/dev/null 2>&1  
end_time=$(date +%s%N)
malts_time=$(( (end_time - start_time) / 1000000 ))

echo "Temps réponse Hops: ${hops_time}ms"
echo "Temps réponse Malts: ${malts_time}ms"

if [ "$hops_time" -lt 1000 ] && [ "$malts_time" -lt 1000 ]; then
    echo -e "${GREEN}✅ Performances acceptables${NC}"
else
    echo -e "${YELLOW}⚠️  Performances dégradées${NC}"
fi

echo -e "\n${BLUE}Test de régression terminé${NC}"
EOF

chmod +x test_regression.sh

echo -e "${BLUE}📋 SCRIPTS DE TEST CRÉÉS${NC}"
echo "=========================="

echo -e "\n${GREEN}✅ Scripts disponibles:${NC}"
echo "• ./test_aligned_domains.sh     (test rapide alignement)" 
echo "• ./test-hops-malts-complete.sh (test complet des 2 domaines)"
echo "• ./test_regression.sh          (test de régression)"

echo -e "\n${YELLOW}📖 Usage recommandé:${NC}"
echo "1. Démarrez: sbt run"
echo "2. Test rapide: ./test_aligned_domains.sh"
echo "3. Test complet: ./test-hops-malts-complete.sh" 
echo "4. Régression: ./test_regression.sh"

echo -e "\n${BLUE}Les scripts vérifieront automatiquement:${NC}"
echo "• Format JSON aligné (.hops vs .items)"
echo "• Structure de pagination cohérente"
echo "• Fonctionnement des APIs de recherche"
echo "• Performance et intégrité des données"
