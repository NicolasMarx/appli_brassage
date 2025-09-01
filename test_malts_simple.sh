#!/bin/bash

BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🌾 TEST SIMPLE APIs MALTS${NC}"
echo "========================="

echo -e "\n${BLUE}Test API Admin${NC}"
admin_response=$(curl -s "${BASE_URL}/api/admin/malts" 2>/dev/null || echo "ERROR")
if [ "$admin_response" != "ERROR" ]; then
    echo -e "✅ ${GREEN}Admin API accessible${NC}"
    echo "Response: $admin_response"
else
    echo -e "❌ ${RED}Admin API inaccessible${NC}"
fi

echo -e "\n${BLUE}Test API Publique${NC}"
public_response=$(curl -s "${BASE_URL}/api/v1/malts" 2>/dev/null || echo "ERROR")
if [ "$public_response" != "ERROR" ]; then
    echo -e "✅ ${GREEN}Public API accessible${NC}"
    echo "Response: $public_response"
else
    echo -e "❌ ${RED}Public API inaccessible${NC}"
fi

echo -e "\n${BLUE}Instructions:${NC}"
echo "1. Démarrez l'app avec: sbt run"
echo "2. Relancez ce script pour tester les APIs"
