#!/bin/bash

echo "🧪 Tests API Malts après correction"
echo "=================================="

API_BASE="http://localhost:9000"

echo ""
echo "1. Test API Admin - Liste malts:"
curl -s "$API_BASE/api/admin/malts" | jq . || curl -s "$API_BASE/api/admin/malts"

echo ""
echo "2. Test API Admin - Count:"  
curl -s "$API_BASE/api/admin/malts" | jq '.totalCount // "Pas de totalCount"' || echo "Erreur API"

echo ""
echo "3. Vérification logs (dernières 20 lignes avec DEBUG):"
echo "Lancez: docker logs [container-app] | tail -20 | grep DEBUG"

echo ""
echo "4. Test direct en base:"
docker exec [container-db] psql -U postgres -d postgres -c "SELECT id, name, malt_type, source FROM malts LIMIT 3;" 2>/dev/null || echo "DB non accessible"

echo ""
echo "✅ Tests terminés"
