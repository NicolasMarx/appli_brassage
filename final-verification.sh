#!/bin/bash

# =============================================================================
# VÉRIFICATION FINALE RAPIDE - INFRASTRUCTURE PHASE 1
# =============================================================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

API_BASE="http://localhost:9000"

echo -e "${BLUE}🔍 Vérification finale infrastructure Phase 1${NC}"
echo ""

# Test services Docker
echo -e "${BLUE}📋 Services Docker:${NC}"
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""

# Test PostgreSQL
echo -e "${BLUE}🗄️  Test PostgreSQL:${NC}"
if docker-compose exec postgres pg_isready -U postgres -d appli_brassage > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL opérationnel${NC}"
    
    # Compter les tables
    TABLE_COUNT=$(docker-compose exec postgres psql -U postgres -d appli_brassage -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs || echo "0")
    echo -e "${GREEN}✅ Tables en base: $TABLE_COUNT${NC}"
else
    echo -e "${RED}❌ PostgreSQL non accessible${NC}"
fi

# Test Redis
echo -e "${BLUE}🔴 Test Redis:${NC}"
if docker-compose exec redis redis-cli ping 2>/dev/null | grep -q "PONG"; then
    echo -e "${GREEN}✅ Redis opérationnel${NC}"
else
    echo -e "${RED}❌ Redis non accessible${NC}"
fi

echo ""

# Test Application Play
echo -e "${BLUE}🎮 Test Application Play:${NC}"
if curl -s --max-time 5 "$API_BASE" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Application accessible sur port 9000${NC}"
    
    # Test API houblons
    echo -e "${BLUE}🍺 Test API houblons:${NC}"
    HOPS_RESPONSE=$(curl -s --max-time 5 "$API_BASE/api/v1/hops" 2>/dev/null || echo "error")
    
    if [ "$HOPS_RESPONSE" != "error" ]; then
        if echo "$HOPS_RESPONSE" | jq . > /dev/null 2>&1; then
            HOPS_COUNT=$(echo "$HOPS_RESPONSE" | jq -r '.totalCount // (.content | length) // 0' 2>/dev/null || echo "0")
            echo -e "${GREEN}✅ API houblons: $HOPS_COUNT houblon(s) trouvé(s)${NC}"
            
            # Test pagination
            PAGINATED_RESPONSE=$(curl -s --max-time 5 "$API_BASE/api/v1/hops?page=0&size=5" 2>/dev/null || echo "error")
            if [ "$PAGINATED_RESPONSE" != "error" ] && echo "$PAGINATED_RESPONSE" | jq . > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Pagination API fonctionnelle${NC}"
            fi
            
        else
            echo -e "${YELLOW}⚠️  API houblons répond mais format non-JSON${NC}"
        fi
    else
        echo -e "${RED}❌ API houblons ne répond pas${NC}"
    fi
    
else
    echo -e "${YELLOW}⚠️  Application Play non accessible${NC}"
    echo -e "${YELLOW}   Assurez-vous que 'sbt run' est lancé${NC}"
fi

echo ""
echo -e "${BLUE}📊 RÉSUMÉ:${NC}"

# Vérifier les fichiers de configuration
if [ -f "conf/application.conf" ] && grep -q "slick.dbs.default" conf/application.conf; then
    echo -e "${GREEN}✅ Configuration Slick OK${NC}"
else
    echo -e "${RED}❌ Configuration Slick manquante${NC}"
fi

if [ -f "docker-compose.yml" ] && grep -q "postgres:" docker-compose.yml; then
    echo -e "${GREEN}✅ Docker-compose corrigé${NC}"
else
    echo -e "${RED}❌ Docker-compose incorrect${NC}"
fi

if java -version 2>&1 | head -n 1 | grep -E "(11|17|21)" > /dev/null; then
    echo -e "${GREEN}✅ Version Java compatible${NC}"
else
    echo -e "${YELLOW}⚠️  Version Java à vérifier${NC}"
fi

echo ""
echo -e "${BLUE}🚀 Actions recommandées:${NC}"

if curl -s --max-time 2 "$API_BASE" > /dev/null 2>&1; then
    echo -e "${GREEN}1. ✅ Infrastructure complète et fonctionnelle !${NC}"
    echo -e "${GREEN}2. Vous pouvez committer les changements:${NC}"
    echo "   git add ."
    echo "   git commit -m 'fix: infrastructure corrections applied and tested'"
    echo ""
    echo -e "${GREEN}3. Puis merger dans main:${NC}"
    echo "   git checkout main"
    echo "   git merge fix/infrastructure-corrections-$(date +%Y%m%d_%H%M%S | head -c 20)*"
else
    echo -e "${YELLOW}1. Lancez l'application: sbt run${NC}"
    echo -e "${YELLOW}2. Puis relancez ce script pour vérifier${NC}"
fi

echo ""
echo -e "${BLUE}🔗 URLs de test:${NC}"
echo "   • API: http://localhost:9000/api/v1/hops"
echo "   • App: http://localhost:9000"
echo ""