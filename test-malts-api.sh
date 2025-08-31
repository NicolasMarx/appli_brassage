#!/bin/bash

echo "🌾 Test API Domaine Malts"
echo "========================="

BASE_URL="http://localhost:9000"

# Test API publique
echo "1. Test liste malts publique..."
curl -s "$BASE_URL/api/v1/malts?page=0&pageSize=5" | jq '.items | length'

echo "2. Test types malts..."
curl -s "$BASE_URL/api/v1/malts/types" | jq '.maltTypes | length'

echo "3. Test couleurs malts..."
curl -s "$BASE_URL/api/v1/malts/colors" | jq '.colorRanges | length'

# Test création malt (nécessite authentification admin)
echo "4. Test création malt admin..."
curl -X POST "$BASE_URL/api/admin/malts" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Malt API",
    "maltType": "BASE",
    "ebcColor": 8.0,
    "extractionRate": 81.0,
    "diastaticPower": 95.0,
    "originCode": "US",
    "description": "Malt créé via test API",
    "flavorProfiles": ["Céréale", "Biscuit"],
    "source": "MANUAL"
  }' | jq '.id // .error'

echo ""
echo "✅ Tests API Malts terminés"
echo "Consultez la sortie JSON pour vérifier les résultats"