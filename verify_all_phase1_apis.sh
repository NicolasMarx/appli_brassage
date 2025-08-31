#!/bin/bash

# =============================================================================
# SCRIPT DE VÉRIFICATION COMPLÈTE DES APIS PHASE 1
# =============================================================================
# Vérifie toutes les APIs et l'infrastructure de la Phase 1 DDD/CQRS
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Configuration
API_BASE="http://localhost:9000"
ADMIN_API_BASE="$API_BASE/api/admin"
PUBLIC_API_BASE="$API_BASE/api/v1"
DB_CONTAINER="my-brew-app-v2-db-1"  # Ajustez selon votre container

echo -e "${BLUE}"
echo "🔍 =============================================================================="
echo "   VÉRIFICATION COMPLÈTE DES APIS PHASE 1 - DDD/CQRS"
echo "=============================================================================="
echo -e "${NC}"

ERRORS=0
WARNINGS=0
TESTS_RUN=0

# Fonctions d'affichage
test_start() {
    echo -e "${PURPLE}🧪 Test: $1${NC}"
    ((TESTS_RUN++))
}

test_ok() {
    echo -e "   ${GREEN}✅ $1${NC}"
}

test_error() {
    echo -e "   ${RED}❌ $1${NC}"
    ((ERRORS++))
}

test_warning() {
    echo -e "   ${YELLOW}⚠️  $1${NC}"
    ((WARNINGS++))
}

# Fonction pour tester un endpoint
test_endpoint() {
    local method=$1
    local url=$2
    local expected_status=$3
    local description=$4
    local data=${5:-""}
    local headers=${6:-""}
    
    test_start "$description"
    
    if [ -n "$headers" ]; then
        if [ -n "$data" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" -H "$headers" -d "$data" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" -H "$headers" 2>/dev/null)
        fi
    else
        if [ -n "$data" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" -d "$data" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" 2>/dev/null)
        fi
    fi
    
    # Séparer le body et le status code
    body=$(echo "$response" | sed '$d')
    status=$(echo "$response" | tail -n1)
    
    if [ "$status" = "$expected_status" ]; then
        test_ok "Status $status (attendu $expected_status)"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            # Essayer de parser le JSON pour vérifier sa validité
            if echo "$body" | jq . > /dev/null 2>&1; then
                test_ok "JSON valide retourné"
            else
                test_warning "Réponse non-JSON: ${body:0:100}..."
            fi
        fi
    else
        if [ "$status" = "000" ]; then
            test_error "Endpoint inaccessible (connexion impossible)"
        else
            test_error "Status $status (attendu $expected_status)"
            test_warning "Réponse: ${body:0:200}..."
        fi
    fi
    
    echo ""
}

# =============================================================================
# ÉTAPE 1: VÉRIFICATION INFRASTRUCTURE
# =============================================================================

echo -e "${BLUE}🏗️  ÉTAPE 1: Vérification de l'infrastructure${NC}"
echo ""

test_start "Vérification PostgreSQL"
if docker ps | grep -q postgres; then
    test_ok "Container PostgreSQL actif"
    
    if docker exec $DB_CONTAINER pg_isready > /dev/null 2>&1; then
        test_ok "PostgreSQL accepte les connexions"
    else
        test_error "PostgreSQL ne répond pas"
    fi
else
    test_error "Container PostgreSQL non démarré"
fi
echo ""

test_start "Vérification Play Framework"
if curl -s "$API_BASE" > /dev/null; then
    test_ok "Application Play accessible"
else
    test_error "Application Play inaccessible"
    echo -e "${RED}⚠️  Lancez 'sbt run' avant de continuer${NC}"
    exit 1
fi
echo ""

# =============================================================================
# ÉTAPE 2: VÉRIFICATION API PUBLIQUE (v1)
# =============================================================================

echo -e "${BLUE}🌐 ÉTAPE 2: Vérification API publique (v1)${NC}"
echo ""

# Test endpoint principal des houblons
test_endpoint "GET" "$PUBLIC_API_BASE/hops" "200" "Liste des houblons publique"

# Test avec paramètres de pagination
test_endpoint "GET" "$PUBLIC_API_BASE/hops?page=0&size=5" "200" "Pagination des houblons"

# Test endpoint détail houblon (si des houblons existent)
# D'abord, récupérer un ID de houblon
hop_response=$(curl -s "$PUBLIC_API_BASE/hops" 2>/dev/null)
if echo "$hop_response" | jq -e '.content[0].id' > /dev/null 2>&1; then
    first_hop_id=$(echo "$hop_response" | jq -r '.content[0].id')
    test_endpoint "GET" "$PUBLIC_API_BASE/hops/$first_hop_id" "200" "Détail d'un houblon spécifique"
else
    test_warning "Aucun houblon trouvé pour tester l'endpoint de détail"
fi

# Test filtrage avancé
test_endpoint "POST" "$PUBLIC_API_BASE/hops/search" "200" "Filtrage avancé des houblons" '{"alphaAcidRange":{"min":4.0,"max":8.0},"usage":"AROMA"}' "Content-Type: application/json"

# Test endpoints qui devraient retourner 404
test_endpoint "GET" "$PUBLIC_API_BASE/hops/nonexistent-hop-id" "404" "Houblon inexistant (404 attendu)"

# =============================================================================
# ÉTAPE 3: VÉRIFICATION API ADMIN (SÉCURISÉE)
# =============================================================================

echo -e "${BLUE}🔒 ÉTAPE 3: Vérification API admin (sécurisée)${NC}"
echo ""

# Test accès sans authentification (devrait échouer)
test_endpoint "GET" "$ADMIN_API_BASE/hops" "401" "Accès admin sans auth (401 attendu)" "" ""

# Test création houblon sans auth (devrait échouer)
test_endpoint "POST" "$ADMIN_API_BASE/hops" "401" "Création houblon sans auth (401 attendu)" '{"name":"Test","alphaAcid":5.0,"usage":"AROMA"}' "Content-Type: application/json"

# Test propositions IA sans auth (devrait échouer)
test_endpoint "GET" "$ADMIN_API_BASE/proposals/pending" "401" "Propositions IA sans auth (401 attendu)"

# Note: Pour tester complètement l'API admin, il faudrait s'authentifier
test_warning "Tests API admin complets nécessitent une authentification"
test_warning "Créez d'abord un utilisateur admin pour tester complètement"

# =============================================================================
# ÉTAPE 4: VÉRIFICATION BASE DE DONNÉES
# =============================================================================

echo -e "${BLUE}🗄️  ÉTAPE 4: Vérification intégration base de données${NC}"
echo ""

test_start "Comptage des données en base"
if docker ps | grep -q postgres; then
    hop_count_db=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM hops;" 2>/dev/null | xargs || echo "0")
    aroma_count=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM aromas;" 2>/dev/null | xargs || echo "0")
    origin_count=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM origins;" 2>/dev/null | xargs || echo "0")
    
    test_ok "Houblons en base: $hop_count_db"
    test_ok "Aromes en base: $aroma_count"
    test_ok "Origines en base: $origin_count"
    
    # Vérifier cohérence API/DB
    hop_api_response=$(curl -s "$PUBLIC_API_BASE/hops" 2>/dev/null)
    hop_count_api=$(echo "$hop_api_response" | jq -r '.totalCount // (.content | length) // 0' 2>/dev/null || echo "0")
    
    test_ok "Houblons via API: $hop_count_api"
    
    if [ "$hop_count_db" = "$hop_count_api" ]; then
        test_ok "Cohérence DB/API: ✓"
    else
        test_warning "Incohérence DB ($hop_count_db) vs API ($hop_count_api)"
    fi
else
    test_error "Impossible de vérifier la base de données"
fi
echo ""

# =============================================================================
# ÉTAPE 5: TEST D'INTÉGRATION LECTURE/ÉCRITURE
# =============================================================================

echo -e "${BLUE}💾 ÉTAPE 5: Test intégration lecture/écriture${NC}"
echo ""

test_start "Test insertion directe en base + lecture via API"
if docker ps | grep -q postgres; then
    # Créer un houblon test directement en base
    test_hop_id="test-integration-$(date +%s)"
    test_hop_name="Test-Integration-$(date +%s)"
    
    # Insérer en base
    insert_result=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -c "
    INSERT INTO hops (id, name, alpha_acid, beta_acid, usage, status, source, credibility_score, created_at, updated_at) 
    VALUES (
      '$test_hop_id',
      '$test_hop_name', 
      7.5, 
      4.2, 
      'AROMA', 
      'ACTIVE', 
      'MANUAL', 
      100, 
      NOW(), 
      NOW()
    );
    " 2>&1)
    
    if echo "$insert_result" | grep -q "INSERT 0 1"; then
        test_ok "Houblon test inséré en base"
        
        # Attendre un moment pour la cohérence
        sleep 2
        
        # Tenter de le récupérer via API
        api_response=$(curl -s "$PUBLIC_API_BASE/hops/$test_hop_id" 2>/dev/null)
        
        if echo "$api_response" | jq -e '.name' > /dev/null 2>&1; then
            retrieved_name=$(echo "$api_response" | jq -r '.name')
            if [ "$retrieved_name" = "$test_hop_name" ]; then
                test_ok "Houblon récupéré via API avec le bon nom"
            else
                test_error "Nom différent: DB='$test_hop_name' vs API='$retrieved_name'"
            fi
        else
            test_error "API ne trouve pas le houblon inséré en base"
        fi
        
        # Nettoyer
        docker exec $DB_CONTAINER psql -U postgres -d postgres -c "DELETE FROM hops WHERE id = '$test_hop_id';" > /dev/null 2>&1
        test_ok "Nettoyage effectué"
    else
        test_error "Échec insertion en base"
    fi
else
    test_error "PostgreSQL non disponible pour le test"
fi
echo ""

# =============================================================================
# ÉTAPE 6: VÉRIFICATION CONFIGURATION
# =============================================================================

echo -e "${BLUE}⚙️  ÉTAPE 6: Vérification configuration${NC}"
echo ""

test_start "Vérification fichiers de configuration"

if [ -f "conf/application.conf" ]; then
    test_ok "Configuration application.conf présente"
    
    if grep -q "slick.dbs.default" conf/application.conf; then
        test_ok "Configuration Slick trouvée"
    else
        test_warning "Configuration Slick non trouvée"
    fi
    
    if grep -q "ai.monitoring" conf/application.conf; then
        test_ok "Configuration IA trouvée"
    else
        test_warning "Configuration IA non trouvée"
    fi
else
    test_error "Fichier application.conf manquant"
fi

if [ -f "conf/routes" ]; then
    test_ok "Fichier routes présent"
    
    if grep -q "/api/v1" conf/routes; then
        test_ok "Routes API publique trouvées"
    else
        test_warning "Routes API publique non trouvées"
    fi
    
    if grep -q "/api/admin" conf/routes; then
        test_ok "Routes API admin trouvées"
    else
        test_warning "Routes API admin non trouvées"
    fi
else
    test_error "Fichier routes manquant"
fi
echo ""

# =============================================================================
# ÉTAPE 7: VÉRIFICATION STRUCTURE DDD
# =============================================================================

echo -e "${BLUE}🏗️  ÉTAPE 7: Vérification structure DDD/CQRS${NC}"
echo ""

test_start "Vérification structure des dossiers DDD"

ddd_directories=(
    "app/domain"
    "app/application" 
    "app/infrastructure"
    "app/interfaces"
    "app/domain/shared"
    "app/domain/admin"
    "app/domain/hops"
)

for dir in "${ddd_directories[@]}"; do
    if [ -d "$dir" ]; then
        test_ok "Répertoire $dir présent"
    else
        test_error "Répertoire $dir manquant"
    fi
done
echo ""

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo -e "${BLUE}📊 =============================================================================="
echo "   RAPPORT FINAL DE VÉRIFICATION PHASE 1"
echo "==============================================================================${NC}"
echo ""

echo -e "${BLUE}📈 Statistiques:${NC}"
echo "   Tests exécutés: $TESTS_RUN"
echo "   Erreurs: $ERRORS"
echo "   Avertissements: $WARNINGS"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}🎉 PHASE 1 VALIDATION RÉUSSIE !${NC}"
    echo ""
    echo -e "${GREEN}✅ Infrastructure fonctionnelle${NC}"
    echo -e "${GREEN}✅ API publique opérationnelle${NC}" 
    echo -e "${GREEN}✅ Sécurité admin en place${NC}"
    echo -e "${GREEN}✅ Intégration base de données OK${NC}"
    echo ""
    echo -e "${GREEN}🚀 PRÊT POUR LA PHASE 2 - DOMAINE HOPS${NC}"
    echo ""
    echo -e "${BLUE}Prochaines étapes Phase 2:${NC}"
    echo "   1. Implémenter HopAggregate complet"
    echo "   2. Créer API admin authentifiée pour houblons"
    echo "   3. Développer tests d'intégration avancés"
    echo "   4. Implémenter validation métier"
    
elif [ $ERRORS -le 3 ] && [ $WARNINGS -le 8 ]; then
    echo -e "${YELLOW}⚠️  PHASE 1 PARTIELLEMENT VALIDÉE${NC}"
    echo ""
    echo -e "${YELLOW}$ERRORS erreur(s) mineure(s) détectée(s)${NC}"
    echo -e "${YELLOW}$WARNINGS avertissement(s)${NC}"
    echo ""
    echo -e "${BLUE}Actions recommandées:${NC}"
    echo "   - Corriger les erreurs mineures"
    echo "   - Vérifier la configuration"
    echo "   - Implémenter l'authentification admin"
    echo ""
    echo -e "${YELLOW}Vous pouvez continuer vers la Phase 2 avec précaution${NC}"
    
else
    echo -e "${RED}❌ PHASE 1 NON VALIDÉE${NC}"
    echo ""
    echo -e "${RED}$ERRORS erreur(s) critique(s) détectée(s)${NC}"
    echo -e "${RED}$WARNINGS avertissement(s)${NC}"
    echo ""
    echo -e "${RED}ACTIONS REQUISES AVANT PHASE 2:${NC}"
    echo "   - Corriger toutes les erreurs de compilation"
    echo "   - Réparer l'intégration base de données"
    echo "   - Vérifier la structure DDD/CQRS"
    echo "   - Tester l'API publique"
    echo ""
    echo -e "${RED}⚠️  NE PAS CONTINUER VERS PHASE 2 SANS VALIDATION${NC}"
fi

echo ""
echo -e "${BLUE}💡 Conseils:${NC}"
echo "   - Pour les erreurs d'authentification admin, créez d'abord un utilisateur admin"
echo "   - Vérifiez que 'sbt run' est lancé et que l'app écoute sur le port 9000"
echo "   - Assurez-vous que PostgreSQL est accessible via Docker"
echo ""

echo -e "${BLUE}🔧 Commandes utiles:${NC}"
echo "   # Relancer l'app:"
echo "   sbt run"
echo ""
echo "   # Vérifier PostgreSQL:"
echo "   docker-compose ps"
echo "   docker-compose logs postgres"
echo ""
echo "   # Vérifier compilation:"
echo "   sbt compile"
echo ""

exit $ERRORS