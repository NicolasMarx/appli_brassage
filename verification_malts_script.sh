#!/bin/bash

# =============================================================================
# SCRIPT DE VÉRIFICATION - DOMAINE MALTS
# Vérifie structure BDD, données, compilation et APIs
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DB_CONTAINER="my-brew-app-v2-db-1"  # ou le nom de votre container PostgreSQL
DB_NAME="appli_brassage" # ou votre nom de base
DB_USER="postgres"
API_BASE="http://localhost:9000"

# Fonctions utilitaires
echo_step() {
    echo -e "${BLUE}🔍 $1${NC}"
    echo "================================================="
}

echo_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

echo_error() {
    echo -e "${RED}❌ $1${NC}"
}

echo_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

check_prerequisite() {
    if ! command -v "$1" &> /dev/null; then
        echo_error "$1 n'est pas installé"
        exit 1
    fi
    echo_success "$1 est disponible"
}

# =============================================================================
# ÉTAPE 0 : VÉRIFICATION PRÉREQUIS
# =============================================================================

echo_step "Vérification des prérequis"

check_prerequisite "docker"
check_prerequisite "docker-compose"
check_prerequisite "curl"
check_prerequisite "jq"
check_prerequisite "sbt"

# =============================================================================
# ÉTAPE 1 : VÉRIFICATION SERVICES
# =============================================================================

echo_step "Vérification des services"

# PostgreSQL
if docker ps | grep -q postgres; then
    echo_success "PostgreSQL container actif"
    
    # Test connexion
    if docker exec $DB_CONTAINER pg_isready -U $DB_USER > /dev/null 2>&1; then
        echo_success "PostgreSQL accepte les connexions"
    else
        echo_error "PostgreSQL refuse les connexions"
        exit 1
    fi
else
    echo_error "PostgreSQL container non trouvé"
    echo_info "Lancez: docker-compose up -d postgres"
    exit 1
fi

# Application Play
if pgrep -f "sbt.*run" > /dev/null || curl -s "$API_BASE" > /dev/null 2>&1; then
    echo_success "Application Play semble active"
else
    echo_warning "Application Play peut ne pas être active"
    echo_info "Si nécessaire, lancez: sbt run"
fi

# =============================================================================
# ÉTAPE 2 : VÉRIFICATION STRUCTURE BASE DE DONNÉES
# =============================================================================

echo_step "Vérification structure base de données"

# Lister toutes les tables
echo_info "Tables existantes:"
TABLES=$(docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    ORDER BY table_name;
" 2>/dev/null | grep -v "^$" | sed 's/^ *//' || echo "Erreur connexion")

if [ "$TABLES" = "Erreur connexion" ]; then
    echo_error "Impossible de se connecter à la base de données"
    echo_info "Vérifiez les paramètres : DB_NAME=$DB_NAME, DB_USER=$DB_USER"
    exit 1
fi

echo "$TABLES" | while read -r table; do
    if [ -n "$table" ]; then
        echo "  📋 $table"
    fi
done

# Vérifier tables spécifiques
echo ""
echo_info "Vérification tables critiques:"

check_table() {
    local table_name=$1
    local count
    count=$(docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = '$table_name';
    " 2>/dev/null | xargs)
    
    if [ "$count" -eq "1" ]; then
        # Compter les enregistrements
        local row_count
        row_count=$(docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "
            SELECT COUNT(*) FROM $table_name;
        " 2>/dev/null | xargs)
        echo_success "Table $table_name existe ($row_count enregistrements)"
        return 0
    else
        echo_error "Table $table_name manquante"
        return 1
    fi
}

# Tables attendues
MALTS_TABLE_EXISTS=false
if check_table "malts"; then
    MALTS_TABLE_EXISTS=true
fi

check_table "admins"
check_table "audit_logs"  
check_table "hops"
check_table "origins"
check_table "aroma_profiles" || check_table "aromas"
check_table "beer_styles"

# =============================================================================
# ÉTAPE 3 : VÉRIFICATION ÉVOLUTIONS APPLIQUÉES
# =============================================================================

echo_step "Vérification évolutions Play"

# Vérifier table play_evolutions
EVOLUTIONS=$(docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "
    SELECT id, applied_at 
    FROM play_evolutions 
    WHERE state = 'applied' 
    ORDER BY id;
" 2>/dev/null || echo "Table play_evolutions non trouvée")

if [ "$EVOLUTIONS" != "Table play_evolutions non trouvée" ]; then
    echo_success "Évolutions appliquées:"
    echo "$EVOLUTIONS" | while read -r line; do
        if [ -n "$line" ]; then
            echo "  🔄 Evolution $line"
        fi
    done
else
    echo_warning "Table play_evolutions non trouvée"
    echo_info "Cela peut indiquer que les évolutions n'ont pas été appliquées"
fi

# =============================================================================
# ÉTAPE 4 : CRÉATION ÉVOLUTION MALTS (SI NÉCESSAIRE)
# =============================================================================

if [ "$MALTS_TABLE_EXISTS" = false ]; then
    echo_step "Création évolution malts"
    
    echo_info "Table malts non trouvée - création de l'évolution"
    
    # Trouver le prochain numéro d'évolution
    NEXT_EVOLUTION=10
    if [ -f "conf/evolutions/default/10.sql" ]; then
        echo_warning "Évolution 10.sql existe déjà"
    else
        echo_info "Création de conf/evolutions/default/10.sql"
        
        mkdir -p conf/evolutions/default
        
        cat > conf/evolutions/default/10.sql << 'EOF'
# --- !Ups

-- =============================================================================
-- ÉVOLUTION 10 : TABLE MALTS - DOMAINE DDD/CQRS
-- =============================================================================

-- Extension pour UUID (si pas déjà créée)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table principale malts
CREATE TABLE malts (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                  VARCHAR(100) NOT NULL,
    malt_type             VARCHAR(30) NOT NULL CHECK (malt_type IN ('BASE', 'CRYSTAL', 'ROASTED', 'SPECIALTY', 'ADJUNCT')),
    ebc_color             NUMERIC(5,1) NOT NULL CHECK (ebc_color >= 0 AND ebc_color <= 1000),
    extraction_rate       NUMERIC(4,1) NOT NULL CHECK (extraction_rate >= 0 AND extraction_rate <= 100),
    diastatic_power       NUMERIC(6,1) NOT NULL DEFAULT 0 CHECK (diastatic_power >= 0),
    origin_code           VARCHAR(10) REFERENCES origins(id),
    description           TEXT,
    flavor_profiles       TEXT[] DEFAULT '{}',
    source                VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'AI', 'IMPORT')),
    is_active             BOOLEAN NOT NULL DEFAULT true,
    credibility_score     NUMERIC(3,2) NOT NULL DEFAULT 1.0 CHECK (credibility_score >= 0 AND credibility_score <= 1),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version               BIGINT NOT NULL DEFAULT 1
);

-- Index pour performances
CREATE INDEX idx_malts_name ON malts(name);
CREATE INDEX idx_malts_type ON malts(malt_type);
CREATE INDEX idx_malts_ebc_color ON malts(ebc_color);
CREATE INDEX idx_malts_extraction_rate ON malts(extraction_rate);
CREATE INDEX idx_malts_origin_code ON malts(origin_code);
CREATE INDEX idx_malts_is_active ON malts(is_active);
CREATE INDEX idx_malts_source ON malts(source);
CREATE INDEX idx_malts_credibility_score ON malts(credibility_score);
CREATE INDEX idx_malts_created_at ON malts(created_at);

-- Index composé pour recherche
CREATE INDEX idx_malts_search ON malts(is_active, malt_type, ebc_color);

-- Trigger pour mise à jour automatique updated_at
CREATE TRIGGER update_malts_updated_at 
    BEFORE UPDATE ON malts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Table des substitutions malts
CREATE TABLE malt_substitutions (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    malt_id           UUID NOT NULL REFERENCES malts(id) ON DELETE CASCADE,
    substitute_id     UUID NOT NULL REFERENCES malts(id) ON DELETE CASCADE,
    compatibility     NUMERIC(3,2) NOT NULL CHECK (compatibility >= 0 AND compatibility <= 1),
    notes             TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(malt_id, substitute_id)
);

-- Index pour substitutions
CREATE INDEX idx_malt_substitutions_malt_id ON malt_substitutions(malt_id);
CREATE INDEX idx_malt_substitutions_substitute_id ON malt_substitutions(substitute_id);

-- Table de relation malts-styles de bière
CREATE TABLE malt_beer_styles (
    malt_id           UUID NOT NULL REFERENCES malts(id) ON DELETE CASCADE,
    style_id          VARCHAR(50) NOT NULL REFERENCES beer_styles(id) ON DELETE CASCADE,
    suitability       NUMERIC(3,2) DEFAULT 1.0 CHECK (suitability >= 0 AND suitability <= 1),
    usage_notes       TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (malt_id, style_id)
);

-- Index pour relation malts-styles
CREATE INDEX idx_malt_beer_styles_malt_id ON malt_beer_styles(malt_id);
CREATE INDEX idx_malt_beer_styles_style_id ON malt_beer_styles(style_id);

-- =============================================================================
-- DONNÉES INITIALES - MALTS DE BASE
-- =============================================================================

-- Malts de base populaires
INSERT INTO malts (name, malt_type, ebc_color, extraction_rate, diastatic_power, origin_code, description, source) VALUES
('Pilsner Malt', 'BASE', 3.5, 82.0, 120.0, 'DE', 'Malt de base allemand classique, couleur très pâle, idéal pour lagers et bières claires', 'MANUAL'),
('Pale Ale Malt', 'BASE', 5.5, 81.0, 100.0, 'UK', 'Malt de base britannique, légèrement plus coloré que le Pilsner, parfait pour ales', 'MANUAL'),
('Munich Malt', 'SPECIALTY', 16.0, 80.0, 85.0, 'DE', 'Malt allemand apportant couleur dorée et saveurs maltées riches', 'MANUAL'),
('Crystal 60', 'CRYSTAL', 118.0, 75.0, 0.0, 'US', 'Malt crystal américain apportant couleurs ambrées et saveurs caramélisées', 'MANUAL'),
('Chocolate Malt', 'ROASTED', 750.0, 72.0, 0.0, 'UK', 'Malt torréfié apportant couleurs brunes et saveurs chocolatées', 'MANUAL'),
('Vienna Malt', 'SPECIALTY', 8.5, 80.5, 95.0, 'DE', 'Malt autrichien/allemand pour couleurs dorées et saveurs légèrement grillées', 'MANUAL'),
('Wheat Malt', 'BASE', 4.0, 82.5, 160.0, 'DE', 'Malt de blé pour bières blanches, apporte onctuosité et mousse', 'MANUAL');

-- Relations malts-styles populaires
INSERT INTO malt_beer_styles (malt_id, style_id, suitability, usage_notes) 
SELECT m.id, 'pils', 1.0, 'Malt de base principal'
FROM malts m WHERE m.name = 'Pilsner Malt';

INSERT INTO malt_beer_styles (malt_id, style_id, suitability, usage_notes)
SELECT m.id, 'pale-ale', 1.0, 'Malt de base principal'  
FROM malts m WHERE m.name = 'Pale Ale Malt';

INSERT INTO malt_beer_styles (malt_id, style_id, suitability, usage_notes)
SELECT m.id, 'ipa', 0.9, 'Excellent malt de base pour IPA'
FROM malts m WHERE m.name = 'Pale Ale Malt';

# --- !Downs

DROP TABLE IF EXISTS malt_beer_styles;
DROP TABLE IF EXISTS malt_substitutions;
DROP TRIGGER IF EXISTS update_malts_updated_at ON malts;
DROP TABLE IF EXISTS malts;
EOF
        
        echo_success "Évolution 10.sql créée"
        echo_info "Redémarrez l'application pour appliquer : sbt run"
    fi
fi

# =============================================================================
# ÉTAPE 5 : VÉRIFICATION COMPILATION
# =============================================================================

echo_step "Vérification compilation"

echo_info "Compilation du projet..."
if sbt compile > /tmp/malts_compile.log 2>&1; then
    echo_success "Compilation réussie"
else
    echo_error "Erreurs de compilation détectées"
    echo_info "Dernières erreurs:"
    tail -10 /tmp/malts_compile.log
    echo_warning "Continuons malgré les erreurs de compilation..."
fi

# =============================================================================
# ÉTAPE 6 : TEST DES APIS EXISTANTES
# =============================================================================

echo_step "Test des APIs existantes"

# Test API Hops (référence)
echo_info "Test API Hops (référence):"
HOPS_RESPONSE=$(curl -s "$API_BASE/api/v1/hops" || echo "ERROR")
if [ "$HOPS_RESPONSE" != "ERROR" ]; then
    HOPS_COUNT=$(echo "$HOPS_RESPONSE" | jq -r '.totalCount // 0' 2>/dev/null || echo "0")
    echo_success "API Hops répond : $HOPS_COUNT houblons"
else
    echo_warning "API Hops ne répond pas"
fi

# Test API Malts
echo_info "Test API Malts:"
MALTS_API_RESPONSE=$(curl -s "$API_BASE/api/v1/malts" || echo "ERROR")
if [ "$MALTS_API_RESPONSE" != "ERROR" ]; then
    if echo "$MALTS_API_RESPONSE" | jq . >/dev/null 2>&1; then
        MALTS_COUNT=$(echo "$MALTS_API_RESPONSE" | jq -r '.totalCount // 0' 2>/dev/null || echo "0")
        echo_success "API Malts répond : $MALTS_COUNT malts"
    else
        echo_warning "API Malts répond mais pas en JSON valide"
        echo_info "Réponse: ${MALTS_API_RESPONSE:0:200}..."
    fi
else
    echo_warning "API Malts ne répond pas (normal si pas encore implémentée)"
fi

# Test API Admin Malts
echo_info "Test API Admin Malts:"
ADMIN_MALTS_RESPONSE=$(curl -s "$API_BASE/api/admin/malts" -H "Accept: application/json" || echo "ERROR")
if [ "$ADMIN_MALTS_RESPONSE" != "ERROR" ]; then
    echo_success "API Admin Malts répond"
else
    echo_warning "API Admin Malts ne répond pas (normal si pas encore implémentée)"
fi

# =============================================================================
# ÉTAPE 7 : TEST CRUD MALTS (SI API DISPONIBLE)
# =============================================================================

if [ "$MALTS_API_RESPONSE" != "ERROR" ] && [ "$MALTS_API_RESPONSE" != "" ]; then
    echo_step "Test CRUD Malts"
    
    # Test création malt
    echo_info "Test création malt..."
    CREATE_PAYLOAD='{
        "name": "Test Malt Verification",
        "maltType": "BASE",
        "ebcColor": 5.5,
        "extractionRate": 81.0,
        "diastaticPower": 100.0,
        "originCode": "US",
        "description": "Malt créé pour test de vérification"
    }'
    
    CREATE_RESPONSE=$(curl -s -X POST "$API_BASE/api/admin/malts" \
        -H "Content-Type: application/json" \
        -d "$CREATE_PAYLOAD" || echo "ERROR")
    
    if [ "$CREATE_RESPONSE" != "ERROR" ] && echo "$CREATE_RESPONSE" | jq . >/dev/null 2>&1; then
        echo_success "Création malt réussie"
        CREATED_MALT_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id // ""')
        
        if [ -n "$CREATED_MALT_ID" ]; then
            # Test lecture
            echo_info "Test lecture malt créé..."
            READ_RESPONSE=$(curl -s "$API_BASE/api/v1/malts/$CREATED_MALT_ID" || echo "ERROR")
            if [ "$READ_RESPONSE" != "ERROR" ]; then
                echo_success "Lecture malt réussie"
            else
                echo_warning "Lecture malt échouée"
            fi
            
            # Test suppression (cleanup)
            echo_info "Nettoyage : suppression malt test..."
            DELETE_RESPONSE=$(curl -s -X DELETE "$API_BASE/api/admin/malts/$CREATED_MALT_ID" || echo "ERROR")
            if [ "$DELETE_RESPONSE" != "ERROR" ]; then
                echo_success "Suppression malt réussie"
            else
                echo_warning "Suppression malt échouée"
            fi
        fi
    else
        echo_warning "Création malt échouée ou API non disponible"
        echo_info "Réponse: ${CREATE_RESPONSE:0:200}..."
    fi
fi

# =============================================================================
# ÉTAPE 8 : INSPECTION DONNÉES EXISTANTES
# =============================================================================

echo_step "Inspection données existantes"

if [ "$MALTS_TABLE_EXISTS" = true ]; then
    echo_info "Analyse table malts:"
    
    # Statistiques générales
    docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "
        SELECT 
            COUNT(*) as total_malts,
            COUNT(*) FILTER (WHERE is_active = true) as active_malts,
            COUNT(DISTINCT malt_type) as types_count,
            COUNT(DISTINCT origin_code) as origins_count,
            AVG(ebc_color) as avg_ebc_color,
            AVG(extraction_rate) as avg_extraction_rate
        FROM malts;
    " 2>/dev/null || echo_warning "Erreur analyse malts"
    
    # Répartition par type
    echo_info "Répartition par type:"
    docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "
        SELECT malt_type, COUNT(*) as count 
        FROM malts 
        GROUP BY malt_type 
        ORDER BY count DESC;
    " 2>/dev/null || echo_warning "Erreur répartition types"
    
    # Premiers malts
    echo_info "Premiers malts en base:"
    docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "
        SELECT name, malt_type, ebc_color, extraction_rate, origin_code 
        FROM malts 
        WHERE is_active = true 
        ORDER BY created_at 
        LIMIT 5;
    " 2>/dev/null || echo_warning "Erreur lecture malts"
fi

# Vérifier autres données utiles
echo_info "Autres données utiles:"
if docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "SELECT COUNT(*) as hops_count FROM hops;" 2>/dev/null; then
    echo_success "Table hops accessible"
else
    echo_warning "Table hops non accessible"
fi

# =============================================================================
# ÉTAPE 9 : RECOMMENDATIONS
# =============================================================================

echo_step "Recommandations"

echo_info "État actuel du domaine malts:"

if [ "$MALTS_TABLE_EXISTS" = true ]; then
    echo_success "✓ Table malts existe"
else
    echo_warning "✗ Table malts manquante - évolution 10.sql créée"
fi

if [ "$MALTS_API_RESPONSE" != "ERROR" ] && [ "$MALTS_API_RESPONSE" != "" ]; then
    echo_success "✓ API publique malts répond"
else
    echo_warning "✗ API publique malts non disponible"
fi

if [ "$ADMIN_MALTS_RESPONSE" != "ERROR" ] && [ "$ADMIN_MALTS_RESPONSE" != "" ]; then
    echo_success "✓ API admin malts répond"
else
    echo_warning "✗ API admin malts non disponible"
fi

echo ""
echo_info "Actions recommandées:"

if [ "$MALTS_TABLE_EXISTS" = false ]; then
    echo "1. 🔄 Redémarrer l'application pour appliquer l'évolution 10.sql"
    echo "   → sbt run"
fi

if [ "$MALTS_API_RESPONSE" = "ERROR" ] || [ "$MALTS_API_RESPONSE" = "" ]; then
    echo "2. 🔧 Implémenter les contrôleurs malts manquants"
    echo "   → MaltsController.scala et AdminMaltsController.scala"
fi

echo "3. 🧪 Lancer les tests complets:"
echo "   → sbt \"testOnly *malts*\""

echo "4. 📊 Vérifier métriques de performance:"
echo "   → EXPLAIN ANALYZE des requêtes malts"

echo ""
echo_success "Script de vérification terminé !"
echo_info "Logs de compilation: /tmp/malts_compile.log"
echo_info "Pour plus de détails, consultez les logs de l'application"