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
