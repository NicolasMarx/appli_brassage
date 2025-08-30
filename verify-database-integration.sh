#!/bin/bash

echo "🔍 =============================================================================="
echo "   VÉRIFICATION INTÉGRATION BASE DE DONNÉES                                    "
echo "   Validation que l'API lit PostgreSQL et non des données mockées              "
echo "=============================================================================="

# Variables
API_BASE="http://localhost:9000"
DB_CONTAINER="my-brew-app-v2-db-1"  # Ajustez selon votre container name

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_step() { echo -e "${BLUE}📋 $1${NC}"; }
echo_success() { echo -e "${GREEN}✅ $1${NC}"; }
echo_error() { echo -e "${RED}❌ $1${NC}"; }
echo_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# =============================================================================
# ÉTAPE 1 : VÉRIFICATION INFRASTRUCTURE
# =============================================================================

echo_step "Vérification infrastructure base de données"

# Vérifier que PostgreSQL est actif
if ! docker ps | grep -q postgres; then
    echo_error "PostgreSQL n'est pas en cours d'exécution"
    echo "Lancez: docker-compose up -d"
    exit 1
fi
echo_success "PostgreSQL container actif"

# Vérifier connexion DB
if ! docker exec $DB_CONTAINER pg_isready > /dev/null 2>&1; then
    echo_error "PostgreSQL n'accepte pas les connexions"
    exit 1
fi
echo_success "PostgreSQL accepte les connexions"

# Vérifier que l'application est lancée
if ! curl -s "$API_BASE/api/v1/hops" > /dev/null; then
    echo_error "Application Play n'est pas accessible sur $API_BASE"
    echo "Lancez: sbt run"
    exit 1
fi
echo_success "Application Play accessible"

# =============================================================================
# ÉTAPE 2 : INSPECTION DIRECTE BASE DE DONNÉES
# =============================================================================

echo_step "Inspection directe des tables PostgreSQL"

# Compter les houblons en base
HOP_COUNT_DB=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM hops;" | xargs)
echo "🗄️  Nombre de houblons en base PostgreSQL: $HOP_COUNT_DB"

# Lister les premiers houblons en base
echo "📋 Premiers houblons en base de données:"
docker exec $DB_CONTAINER psql -U postgres -d postgres -c "
SELECT id, name, alpha_acid, usage, created_at 
FROM hops 
ORDER BY created_at 
LIMIT 5;
" 2>/dev/null

# Vérifier les autres tables
AROMA_COUNT=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM aromas;" | xargs)
ORIGIN_COUNT=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM origins;" | xargs)
echo "🗄️  Aromes: $AROMA_COUNT, Origins: $ORIGIN_COUNT"

# =============================================================================
# ÉTAPE 3 : VÉRIFICATION API VS BASE DE DONNÉES
# =============================================================================

echo_step "Comparaison données API vs base de données"

# Compter via API publique
API_RESPONSE=$(curl -s "$API_BASE/api/v1/hops")
HOP_COUNT_API=$(echo "$API_RESPONSE" | jq -r '.totalCount // 0' 2>/dev/null || echo "0")
echo "🔌 Nombre de houblons via API: $HOP_COUNT_API"

# Vérification cohérence
if [ "$HOP_COUNT_DB" -eq "$HOP_COUNT_API" ]; then
    echo_success "✅ COHÉRENCE: API et DB retournent le même nombre ($HOP_COUNT_DB)"
else
    echo_error "❌ INCOHÉRENCE: DB=$HOP_COUNT_DB vs API=$HOP_COUNT_API"
fi

# =============================================================================
# ÉTAPE 4 : TEST DE MODIFICATION EN BASE
# =============================================================================

echo_step "Test de modification directe en base de données"

# Créer un houblon test directement en base
TEST_HOP_ID="test-db-$(date +%s)"
TEST_HOP_NAME="Test-DB-Direct-$(date +%s)"

echo "🔧 Insertion d'un houblon test directement en base..."
docker exec $DB_CONTAINER psql -U postgres -d postgres -c "
INSERT INTO hops (id, name, alpha_acid, beta_acid, usage, status, source, credibility_score, created_at, updated_at) 
VALUES (
  '$TEST_HOP_ID',
  '$TEST_HOP_NAME', 
  7.5, 
  4.2, 
  'AROMA', 
  'ACTIVE', 
  'MANUAL', 
  100, 
  NOW(), 
  NOW()
);
" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo_success "Houblon test inséré en base: $TEST_HOP_NAME"
    
    # Attendre un moment pour la cohérence
    sleep 2
    
    # Vérifier que l'API le récupère
    echo "🔍 Recherche du houblon test via API..."
    API_TEST_RESPONSE=$(curl -s "$API_BASE/api/v1/hops/$TEST_HOP_ID")
    
    if echo "$API_TEST_RESPONSE" | jq -e '.name' > /dev/null 2>&1; then
        RETRIEVED_NAME=$(echo "$API_TEST_RESPONSE" | jq -r '.name')
        if [ "$RETRIEVED_NAME" = "$TEST_HOP_NAME" ]; then
            echo_success "✅ API LECTURE DB: Houblon récupéré correctement via API"
            echo "   Nom en base: $TEST_HOP_NAME"
            echo "   Nom via API: $RETRIEVED_NAME"
        else
            echo_error "❌ PROBLÈME: Nom différent (DB: $TEST_HOP_NAME vs API: $RETRIEVED_NAME)"
        fi
    else
        echo_error "❌ PROBLÈME: API ne trouve pas le houblon inséré en base"
        echo "Response: $API_TEST_RESPONSE"
    fi
    
    # Nettoyer le houblon test
    echo "🧹 Nettoyage du houblon test..."
    docker exec $DB_CONTAINER psql -U postgres -d postgres -c "DELETE FROM hops WHERE id = '$TEST_HOP_ID';" > /dev/null
else
    echo_error "Échec insertion houblon test en base"
fi

# =============================================================================
# ÉTAPE 5 : VÉRIFICATION RELATIONS ET JOINTURES
# =============================================================================

echo_step "Vérification des relations et jointures"

# Vérifier qu'un houblon avec aromes en base est bien retourné avec ses aromes via API
FIRST_HOP_WITH_AROMAS=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "
SELECT h.id 
FROM hops h 
JOIN hop_aromas ha ON h.id = ha.hop_id 
LIMIT 1;
" | xargs)

if [ -n "$FIRST_HOP_WITH_AROMAS" ]; then
    echo "🔍 Test houblon avec aromes: $FIRST_HOP_WITH_AROMAS"
    
    # Compter aromes en base pour ce houblon
    AROMAS_DB_COUNT=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "
    SELECT COUNT(*) FROM hop_aromas WHERE hop_id = '$FIRST_HOP_WITH_AROMAS';
    " | xargs)
    
    # Compter aromes via API pour ce houblon
    HOP_API_RESPONSE=$(curl -s "$API_BASE/api/v1/hops/$FIRST_HOP_WITH_AROMAS")
    AROMAS_API_COUNT=$(echo "$HOP_API_RESPONSE" | jq -r '.aromaProfiles | length' 2>/dev/null || echo "0")
    
    echo "🗄️  Aromes en base: $AROMAS_DB_COUNT"
    echo "🔌 Aromes via API: $AROMAS_API_COUNT"
    
    if [ "$AROMAS_DB_COUNT" -eq "$AROMAS_API_COUNT" ] && [ "$AROMAS_DB_COUNT" -gt 0 ]; then
        echo_success "✅ JOINTURES OK: Relations houblon-aromes fonctionnelles"
    else
        echo_error "❌ PROBLÈME JOINTURES: DB=$AROMAS_DB_COUNT vs API=$AROMAS_API_COUNT"
    fi
else
    echo_warning "Aucun houblon avec aromes trouvé pour tester les jointures"
fi

# =============================================================================
# ÉTAPE 6 : VÉRIFICATION DONNÉES SEED/RESEED
# =============================================================================

echo_step "Vérification mécanisme de seed des données"

# Vérifier si les données correspondent à des fichiers CSV seed
if [ -f "conf/reseed/hops.csv" ]; then
    CSV_COUNT=$(tail -n +2 conf/reseed/hops.csv | wc -l | xargs)
    echo "📄 Houblons dans CSV seed: $CSV_COUNT"
    
    if [ "$CSV_COUNT" -eq "$HOP_COUNT_DB" ]; then
        echo_warning "⚠️  ATTENTION: Nombre identique CSV et DB ($CSV_COUNT)"
        echo "   Ceci pourrait indiquer que les données viennent du seed CSV"
        echo "   Vérifiez si des données ont été ajoutées manuellement"
    else
        echo_success "✅ DONNÉES ÉVOLUÉES: DB ($HOP_COUNT_DB) ≠ CSV ($CSV_COUNT)"
    fi
fi

# Test endpoint reseed (si disponible)
echo "🔄 Test endpoint reseed..."
RESEED_RESPONSE=$(curl -s -X POST "$API_BASE/api/reseed" 2>/dev/null || echo "endpoint_not_found")
if [ "$RESEED_RESPONSE" != "endpoint_not_found" ]; then
    echo_warning "Endpoint reseed disponible - peut recharger données CSV"
else
    echo_success "Pas d'endpoint reseed public"
fi

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo "📊 =============================================================================="
echo "   RAPPORT FINAL - INTÉGRATION BASE DE DONNÉES                                "
echo "=============================================================================="

echo ""
echo "🗄️  ÉTAT BASE DE DONNÉES:"
echo "   • PostgreSQL actif: ✅"
echo "   • Houblons en base: $HOP_COUNT_DB"
echo "   • Aromes: $AROMA_COUNT, Origins: $ORIGIN_COUNT"

echo ""
echo "🔌 ÉTAT API:"
echo "   • Application accessible: ✅"  
echo "   • Houblons via API: $HOP_COUNT_API"
echo "   • Cohérence DB/API: $([ "$HOP_COUNT_DB" -eq "$HOP_COUNT_API" ] && echo "✅" || echo "❌")"

echo ""
if [ "$HOP_COUNT_DB" -eq "$HOP_COUNT_API" ] && [ "$HOP_COUNT_DB" -gt 0 ]; then
    echo_success "🎉 CONCLUSION: L'API lit correctement les données PostgreSQL"
    echo "   Les données ne sont PAS stubbées/mockées"
    echo "   L'intégration Slick/PostgreSQL fonctionne correctement"
    exit 0
else
    echo_error "⚠️  ATTENTION: Problème détecté dans l'intégration DB/API"
    echo "   Vérifiez la configuration Slick et les repositories"
    exit 1
fi