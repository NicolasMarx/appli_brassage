#!/bin/bash

# =============================================================================
# SCRIPT DE CONFIGURATION AUTHENTIFICATION ADMIN
# =============================================================================
# Configure et teste l'authentification admin pour l'API sécurisée
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
API_BASE="http://localhost:9000"
ADMIN_API_BASE="$API_BASE/api/admin"
DB_CONTAINER="my-brew-app-v2-db-1"

echo -e "${BLUE}"
echo "🔐 =============================================================================="
echo "   CONFIGURATION ET TEST AUTHENTIFICATION ADMIN"
echo "=============================================================================="
echo -e "${NC}"

# Fonctions utilitaires
echo_step() { echo -e "${BLUE}📋 $1${NC}"; }
echo_success() { echo -e "${GREEN}✅ $1${NC}"; }
echo_error() { echo -e "${RED}❌ $1${NC}"; }
echo_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# =============================================================================
# ÉTAPE 1: VÉRIFICATION INFRASTRUCTURE
# =============================================================================

echo_step "Vérification de l'infrastructure"

if ! docker ps | grep -q postgres; then
    echo_error "PostgreSQL non démarré"
    echo "Lancez: docker-compose up -d"
    exit 1
fi
echo_success "PostgreSQL actif"

if ! curl -s "$API_BASE" > /dev/null; then
    echo_error "Application Play inaccessible"
    echo "Lancez: sbt run"
    exit 1
fi
echo_success "Application Play accessible"

echo ""

# =============================================================================
# ÉTAPE 2: CRÉATION DES TABLES ADMIN (SI NÉCESSAIRE)
# =============================================================================

echo_step "Vérification/création des tables admin"

# Vérifier si la table admins existe
ADMIN_TABLE_EXISTS=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_name = 'admins'
);" | xargs)

if [ "$ADMIN_TABLE_EXISTS" = "t" ]; then
    echo_success "Table admins existe déjà"
else
    echo_warning "Table admins n'existe pas, création..."
    
    # Créer la table admins
    docker exec $DB_CONTAINER psql -U postgres -d postgres -c "
    CREATE TABLE IF NOT EXISTS admins (
        id VARCHAR(255) PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
        permissions TEXT[] DEFAULT '{}',
        is_active BOOLEAN DEFAULT true,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
    );
    
    CREATE INDEX IF NOT EXISTS idx_admins_email ON admins(email);
    CREATE INDEX IF NOT EXISTS idx_admins_active ON admins(is_active);
    " > /dev/null 2>&1
    
    echo_success "Table admins créée"
fi

# Vérifier/créer table sessions admin
ADMIN_SESSIONS_EXISTS=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_name = 'admin_sessions'
);" | xargs)

if [ "$ADMIN_SESSIONS_EXISTS" = "t" ]; then
    echo_success "Table admin_sessions existe déjà"
else
    echo_warning "Table admin_sessions n'existe pas, création..."
    
    docker exec $DB_CONTAINER psql -U postgres -d postgres -c "
    CREATE TABLE IF NOT EXISTS admin_sessions (
        session_id VARCHAR(255) PRIMARY KEY,
        admin_id VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT NOW(),
        expires_at TIMESTAMP NOT NULL,
        ip_address VARCHAR(45),
        user_agent TEXT,
        FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE
    );
    
    CREATE INDEX IF NOT EXISTS idx_admin_sessions_admin_id ON admin_sessions(admin_id);
    CREATE INDEX IF NOT EXISTS idx_admin_sessions_expires ON admin_sessions(expires_at);
    " > /dev/null 2>&1
    
    echo_success "Table admin_sessions créée"
fi

echo ""

# =============================================================================
# ÉTAPE 3: CRÉATION D'UN ADMIN DE TEST
# =============================================================================

echo_step "Création d'un administrateur de test"

# Générer un mot de passe aléaoire sécurisé
TEST_PASSWORD=$(openssl rand -base64 12)
TEST_EMAIL="admin@brewery-test.com"
TEST_ADMIN_ID="admin-test-$(date +%s)"

echo_warning "Création d'un admin de test:"
echo "   Email: $TEST_EMAIL"
echo "   Mot de passe: $TEST_PASSWORD"
echo "   ID: $TEST_ADMIN_ID"

# Générer le hash du mot de passe (bcrypt-like, simplified for demo)
# En production, utilisez bcrypt proper
PASSWORD_HASH=$(echo -n "$TEST_PASSWORD" | sha256sum | cut -d' ' -f1)

# Insérer l'admin de test
INSERT_RESULT=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -c "
INSERT INTO admins (id, email, password_hash, role, permissions, is_active, created_at, updated_at)
VALUES (
    '$TEST_ADMIN_ID',
    '$TEST_EMAIL',
    '\$2b\$10\$dummyhashfor${PASSWORD_HASH:0:20}',
    'ADMIN',
    '{\"MANAGE_INGREDIENTS\", \"MANAGE_REFERENTIALS\", \"APPROVE_AI_PROPOSALS\", \"VIEW_ANALYTICS\"}',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    updated_at = NOW();
" 2>&1)

if echo "$INSERT_RESULT" | grep -q "INSERT\|UPDATE"; then
    echo_success "Admin de test créé/mis à jour"
else
    echo_error "Erreur lors de la création de l'admin"
    echo "$INSERT_RESULT"
fi

echo ""

# =============================================================================
# ÉTAPE 4: TEST AUTHENTIFICATION BASIQUE
# =============================================================================

echo_step "Test d'authentification basique"

# Test 1: Accès sans authentification (doit échouer)
echo "🧪 Test 1: Accès API admin sans authentification"
RESPONSE=$(curl -s -w "\n%{http_code}" "$ADMIN_API_BASE/hops" 2>/dev/null)
STATUS=$(echo "$RESPONSE" | tail -n1)

if [ "$STATUS" = "401" ]; then
    echo_success "✅ Sécurité OK: Accès refusé sans auth (401)"
else
    echo_error "❌ Problème sécurité: Status $STATUS (attendu 401)"
fi

echo ""

# Test 2: Endpoint de login (si implémenté)
echo "🧪 Test 2: Tentative de connexion admin"

LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/api/admin/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}" 2>/dev/null)

LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
LOGIN_STATUS=$(echo "$LOGIN_RESPONSE" | tail -n1)

if [ "$LOGIN_STATUS" = "200" ]; then
    echo_success "✅ Login réussi (200)"
    
    # Essayer d'extraire le token/session
    if echo "$LOGIN_BODY" | jq -e '.token' > /dev/null 2>&1; then
        AUTH_TOKEN=$(echo "$LOGIN_BODY" | jq -r '.token')
        echo_success "Token d'authentification récupéré"
        
        # Test avec le token
        echo "🧪 Test 3: Accès API avec token"
        AUTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$ADMIN_API_BASE/hops" \
            -H "Authorization: Bearer $AUTH_TOKEN" 2>/dev/null)
        
        AUTH_STATUS=$(echo "$AUTH_RESPONSE" | tail -n1)
        
        if [ "$AUTH_STATUS" = "200" ]; then
            echo_success "✅ API admin accessible avec token"
        else
            echo_warning "⚠️  API admin retourne $AUTH_STATUS avec token"
        fi
        
    elif echo "$LOGIN_BODY" | jq -e '.sessionId' > /dev/null 2>&1; then
        SESSION_ID=$(echo "$LOGIN_BODY" | jq -r '.sessionId')
        echo_success "Session ID récupéré"
        
        # Test avec session cookie
        echo "🧪 Test 3: Accès API avec session"
        AUTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$ADMIN_API_BASE/hops" \
            -H "Cookie: PLAY_SESSION=$SESSION_ID" 2>/dev/null)
        
        AUTH_STATUS=$(echo "$AUTH_RESPONSE" | tail -n1)
        
        if [ "$AUTH_STATUS" = "200" ]; then
            echo_success "✅ API admin accessible avec session"
        else
            echo_warning "⚠️  API admin retourne $AUTH_STATUS avec session"
        fi
    else
        echo_warning "Format de réponse de login inattendu"
        echo "Réponse: $LOGIN_BODY"
    fi
    
elif [ "$LOGIN_STATUS" = "404" ]; then
    echo_warning "⚠️  Endpoint de login non implémenté (404)"
    echo "Vous devrez implémenter l'authentification admin"
elif [ "$LOGIN_STATUS" = "401" ]; then
    echo_warning "⚠️  Échec d'authentification (401)"
    echo "Vérifiez l'implémentation du login ou les credentials"
else
    echo_warning "⚠️  Login retourne status $LOGIN_STATUS"
    echo "Réponse: $LOGIN_BODY"
fi

echo ""

# =============================================================================
# ÉTAPE 5: TESTS MANUELS AVEC CURL
# =============================================================================

echo_step "Génération de commandes de test manuelles"

echo_warning "Pour tester manuellement l'API admin, utilisez ces commandes:"
echo ""

if [ -n "${AUTH_TOKEN:-}" ]; then
    echo -e "${BLUE}# Avec token Bearer:${NC}"
    echo "curl -H \"Authorization: Bearer $AUTH_TOKEN\" \"$ADMIN_API_BASE/hops\""
    echo "curl -X POST -H \"Authorization: Bearer $AUTH_TOKEN\" -H \"Content-Type: application/json\" \\"
    echo "     -d '{\"name\":\"Test Hop\",\"alphaAcid\":5.5,\"usage\":\"AROMA\"}' \\"
    echo "     \"$ADMIN_API_BASE/hops\""
    echo ""
fi

if [ -n "${SESSION_ID:-}" ]; then
    echo -e "${BLUE}# Avec session cookie:${NC}"
    echo "curl -H \"Cookie: PLAY_SESSION=$SESSION_ID\" \"$ADMIN_API_BASE/hops\""
    echo "curl -X POST -H \"Cookie: PLAY_SESSION=$SESSION_ID\" -H \"Content-Type: application/json\" \\"
    echo "     -d '{\"name\":\"Test Hop\",\"alphaAcid\":5.5,\"usage\":\"AROMA\"}' \\"
    echo "     \"$ADMIN_API_BASE/hops\""
    echo ""
fi

echo -e "${BLUE}# Login:${NC}"
echo "curl -X POST -H \"Content-Type: application/json\" \\"
echo "     -d '{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}' \\"
echo "     \"$API_BASE/api/admin/login\""
echo ""

echo -e "${BLUE}# Test sécurité (doit retourner 401):${NC}"
echo "curl \"$ADMIN_API_BASE/hops\""
echo ""

# =============================================================================
# ÉTAPE 6: VÉRIFICATION PERMISSIONS
# =============================================================================

echo_step "Vérification des permissions admin"

ADMIN_COUNT=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "SELECT COUNT(*) FROM admins WHERE is_active = true;" | xargs)
echo_success "$ADMIN_COUNT administrateur(s) actif(s) en base"

# Lister les permissions de l'admin test
ADMIN_PERMISSIONS=$(docker exec $DB_CONTAINER psql -U postgres -d postgres -t -c "
SELECT permissions FROM admins WHERE email = '$TEST_EMAIL';
" | xargs)

echo_success "Permissions de l'admin test:"
echo "   $ADMIN_PERMISSIONS"

echo ""

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo -e "${BLUE}📊 =============================================================================="
echo "   RAPPORT CONFIGURATION AUTHENTIFICATION ADMIN"
echo "==============================================================================${NC}"
echo ""

echo -e "${GREEN}✅ Infrastructure vérifiée${NC}"
echo -e "${GREEN}✅ Tables admin créées${NC}"
echo -e "${GREEN}✅ Admin de test configuré${NC}"
echo ""

echo -e "${BLUE}📋 Informations admin de test:${NC}"
echo "   Email: $TEST_EMAIL"
echo "   Mot de passe: $TEST_PASSWORD"
echo "   Permissions: MANAGE_INGREDIENTS, MANAGE_REFERENTIALS, APPROVE_AI_PROPOSALS, VIEW_ANALYTICS"
echo ""

if [ -n "${AUTH_TOKEN:-}" ]; then
    echo -e "${GREEN}✅ Authentification token fonctionnelle${NC}"
    echo "   Token: ${AUTH_TOKEN:0:20}..."
elif [ -n "${SESSION_ID:-}" ]; then
    echo -e "${GREEN}✅ Authentification session fonctionnelle${NC}"
    echo "   Session: ${SESSION_ID:0:20}..."
else
    echo -e "${YELLOW}⚠️  Authentification à implémenter ou déboguer${NC}"
fi

echo ""
echo -e "${BLUE}🔄 Prochaines étapes:${NC}"
echo "   1. Relancez le script de vérification principal avec: ./verify-all-phase1-apis.sh"
echo "   2. Testez manuellement l'API admin avec les commandes ci-dessus"
echo "   3. Implémentez l'authentification complète si nécessaire"
echo "   4. Développez les tests d'intégration pour l'API admin"
echo ""

echo -e "${BLUE}🗑️  Nettoyage:${NC}"
echo "Pour supprimer l'admin de test:"
echo "docker exec $DB_CONTAINER psql -U postgres -d postgres -c \"DELETE FROM admins WHERE email = '$TEST_EMAIL';\""
echo ""