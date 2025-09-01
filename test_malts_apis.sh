#!/bin/bash

# Script de test des APIs Malts complètes
BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🌾 TEST COMPLET APIs MALTS${NC}"
echo "=============================="

# Test 1 : API Admin - Liste
echo -e "\n${BLUE}1. Test API Admin - Liste${NC}"
admin_response=$(curl -s "${BASE_URL}/api/admin/malts" || echo "ERROR")
echo "Admin Response: $admin_response"

if [ "$admin_response" != "ERROR" ] && echo "$admin_response" | jq . >/dev/null 2>&1; then
    admin_count=$(echo "$admin_response" | jq -r '.totalCount // 0')
    admin_length=$(echo "$admin_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ API Admin OK - $admin_length/$admin_count malts${NC}"
else
    echo -e "${RED}❌ API Admin KO${NC}"
fi

# Test 2 : API Publique - Liste
echo -e "\n${BLUE}2. Test API Publique - Liste${NC}"
public_response=$(curl -s "${BASE_URL}/api/v1/malts" || echo "ERROR")
echo "Public Response: $public_response"

if [ "$public_response" != "ERROR" ] && echo "$public_response" | jq . >/dev/null 2>&1; then
    public_count=$(echo "$public_response" | jq -r '.totalCount // 0')
    public_length=$(echo "$public_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ API Publique OK - $public_length/$public_count malts${NC}"
else
    echo -e "${RED}❌ API Publique KO${NC}"
fi

# Test 3 : Recherche Avancée
echo -e "\n${BLUE}3. Test Recherche Avancée${NC}"
search_data='{
  "name": "malt",
  "minEbc": 0,
  "maxEbc": 50,
  "page": 0,
  "size": 10
}'

search_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$search_data" \
  "${BASE_URL}/api/v1/malts/search" || echo "ERROR")

echo "Search Response: $search_response"

if [ "$search_response" != "ERROR" ] && echo "$search_response" | jq . >/dev/null 2>&1; then
    search_length=$(echo "$search_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ Recherche OK - $search_length résultats${NC}"
else
    echo -e "${RED}❌ Recherche KO${NC}"
fi

# Test 4 : Malts par type
echo -e "\n${BLUE}4. Test Malts par Type BASE${NC}"
type_response=$(curl -s "${BASE_URL}/api/v1/malts/type/BASE" || echo "ERROR")
echo "Type BASE Response: $type_response"

if [ "$type_response" != "ERROR" ] && echo "$type_response" | jq . >/dev/null 2>&1; then
    type_length=$(echo "$type_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ Type BASE OK - $type_length malts${NC}"
else
    echo -e "${RED}❌ Type BASE KO${NC}"
fi

# Test 5 : Détail d'un malt (si on en a trouvé un)
if [ "$public_response" != "ERROR" ] && echo "$public_response" | jq . >/dev/null 2>&1; then
    first_malt_id=$(echo "$public_response" | jq -r '.malts[0].id // empty')
    if [ -n "$first_malt_id" ] && [ "$first_malt_id" != "null" ]; then
        echo -e "\n${BLUE}5. Test Détail Malt - ID: $first_malt_id${NC}"
        detail_response=$(curl -s "${BASE_URL}/api/v1/malts/$first_malt_id" || echo "ERROR")
        echo "Detail Response: $detail_response"
        
        if [ "$detail_response" != "ERROR" ] && echo "$detail_response" | jq . >/dev/null 2>&1; then
            malt_name=$(echo "$detail_response" | jq -r '.name // "Unknown"')
            echo -e "${GREEN}✅ Détail OK - Malt: $malt_name${NC}"
        else
            echo -e "${RED}❌ Détail KO${NC}"
        fi
    else
        echo -e "\n${YELLOW}⚠️ Aucun malt trouvé pour test détail${NC}"
    fi
fi

echo -e "\n${BLUE}Tests terminés${NC}"
echo "=================="

# Résumé
echo -e "\n${YELLOW}📋 Résumé:${NC}"
echo -e "• API Admin:    $([ "$admin_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"
echo -e "• API Publique: $([ "$public_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"
echo -e "• Recherche:    $([ "$search_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"
echo -e "• Type BASE:    $([ "$type_response" != "ERROR" ] && echo -e "${GREEN}✅" || echo -e "${RED}❌")${NC}"

if [ "$admin_response" != "ERROR" ] && [ "$public_response" != "ERROR" ]; then
    echo -e "\n${GREEN}🎉 Domaine Malts fonctionnel !${NC}"
else
    echo -e "\n${RED}⚠️ Vérifiez les logs SBT pour plus de détails${NC}"
fi
