#!/bin/bash
echo "🔍 Test connexion PostgreSQL..."
if docker-compose exec postgres psql -U postgres -d appli_brassage -c "SELECT 'Connection OK' as status;" > /dev/null 2>&1; then
    echo "✅ PostgreSQL: OK"
else
    echo "❌ PostgreSQL: Échec"
fi

echo "🔍 Test application Play..."
if curl -s --max-time 5 http://localhost:9000 > /dev/null 2>&1; then
    echo "✅ Play Framework: OK"
else
    echo "⚠️  Play Framework: Non accessible (normal si pas lancé)"
fi
