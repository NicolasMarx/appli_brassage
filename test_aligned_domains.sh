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
