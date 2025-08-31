#!/bin/bash

# =============================================================================
# SCRIPT DE CORRECTION DE L'INFRASTRUCTURE
# =============================================================================
# Corrige les problèmes d'infrastructure détectés
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🔧 =============================================================================="
echo "   CORRECTION DE L'INFRASTRUCTURE"
echo "=============================================================================="
echo -e "${NC}"

echo_step() { echo -e "${BLUE}📋 $1${NC}"; }
echo_success() { echo -e "${GREEN}✅ $1${NC}"; }
echo_error() { echo -e "${RED}❌ $1${NC}"; }
echo_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# =============================================================================
# ÉTAPE 1: CORRECTION DOCKER-COMPOSE
# =============================================================================

echo_step "Correction du fichier docker-compose.yml"

# Sauvegarder l'ancien fichier s'il existe
if [ -f "docker-compose.yml" ]; then
    cp docker-compose.yml docker-compose.yml.backup
    echo_success "Ancien docker-compose sauvegardé"
fi

# Créer le nouveau docker-compose avec les bons noms de services
cat > docker-compose.yml << 'DOCKER_EOF'
version: '3.8'

services:
  # =============================================================================
  # POSTGRESQL (nom: postgres pour compatibilité avec les scripts)
  # =============================================================================
  postgres:
    image: postgres:15-alpine
    container_name: my-brew-app-v2-db-1
    environment:
      POSTGRES_DB: appli_brassage
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_INITDB_ARGS: --encoding=UTF-8
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - brewing-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d appli_brassage"]
      interval: 10s
      timeout: 5s
      retries: 5

  # =============================================================================
  # REDIS POUR CACHE ET SESSIONS
  # =============================================================================
  redis:
    image: redis:7-alpine
    container_name: my-brew-app-v2-redis-1
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - brewing-network
    restart: unless-stopped
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  # =============================================================================
  # PGADMIN POUR ADMINISTRATION BDD (OPTIONNEL)
  # =============================================================================
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: my-brew-app-v2-pgadmin-1
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@brewery.com
      PGADMIN_DEFAULT_PASSWORD: admin123
    ports:
      - "5050:80"
    networks:
      - brewing-network
    depends_on:
      - postgres
    restart: unless-stopped

# =============================================================================
# VOLUMES ET RÉSEAUX
# =============================================================================
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local

networks:
  brewing-network:
    driver: bridge
DOCKER_EOF

echo_success "Nouveau docker-compose.yml créé avec services 'postgres' et 'redis'"

# =============================================================================
# ÉTAPE 2: VÉRIFICATION/CORRECTION CONFIGURATION APPLICATION
# =============================================================================

echo_step "Vérification configuration application"

if [ -f "conf/application.conf" ]; then
    echo_success "Fichier application.conf existe"
    
    # Vérifier si la configuration Slick pointe vers la bonne base
    if grep -q "slick.dbs.default" conf/application.conf; then
        echo_success "Configuration Slick trouvée"
    else
        echo_warning "Configuration Slick manquante, ajout..."
        
        # Ajouter configuration Slick basique
        cat >> conf/application.conf << 'SLICK_CONFIG'

# =============================================================================
# CONFIGURATION BASE DE DONNÉES SLICK
# =============================================================================
slick.dbs.default {
  profile = "slick.jdbc.PostgresProfile$"
  db {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/appli_brassage"
    user = "postgres"
    password = "postgres"
    numThreads = 10
    maxConnections = 10
    minConnections = 1
  }
}

# Configuration Play pour évolutions
play.evolutions.db.default {
  enabled = true
  autoApply = true
  autoApplyDowns = false
}
SLICK_CONFIG
        
        echo_success "Configuration Slick ajoutée"
    fi
else
    echo_warning "Fichier application.conf manquant, création..."
    
    mkdir -p conf
    cat > conf/application.conf << 'APP_CONFIG'
# =============================================================================
# CONFIGURATION PLAY FRAMEWORK
# =============================================================================
play {
  http {
    secret.key = "changeme-in-production-this-should-be-very-long-and-random"
    secret.key = ${?APPLICATION_SECRET}
  }
  
  # Configuration filtres
  filters.enabled += "play.filters.cors.CORSFilter"
  filters.enabled += "play.filters.csrf.CSRFFilter"
}

# =============================================================================
# CONFIGURATION BASE DE DONNÉES SLICK
# =============================================================================
slick.dbs.default {
  profile = "slick.jdbc.PostgresProfile$"
  db {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/appli_brassage"
    user = "postgres"
    password = "postgres"
    numThreads = 10
    maxConnections = 10
    minConnections = 1
  }
}

# Configuration Play pour évolutions
play.evolutions.db.default {
  enabled = true
  autoApply = true
  autoApplyDowns = false
}

# =============================================================================
# CONFIGURATION CACHE REDIS
# =============================================================================
play.cache.redis {
  host = "localhost"
  port = 6379
  database = 0
}
APP_CONFIG

    echo_success "Configuration application.conf créée"
fi

# =============================================================================
# ÉTAPE 3: VÉRIFICATION DÉPENDANCES BUILD.SBT
# =============================================================================

echo_step "Vérification des dépendances build.sbt"

if [ -f "build.sbt" ]; then
    # Vérifier si les dépendances PostgreSQL et Slick sont présentes
    if grep -q "postgresql" build.sbt && grep -q "play-slick" build.sbt; then
        echo_success "Dépendances PostgreSQL et Slick présentes"
    else
        echo_warning "Dépendances manquantes, vérification détaillée..."
        
        if ! grep -q "postgresql" build.sbt; then
            echo_error "Dépendance PostgreSQL manquante dans build.sbt"
        fi
        
        if ! grep -q "play-slick" build.sbt; then
            echo_error "Dépendance play-slick manquante dans build.sbt"
        fi
        
        echo_warning "Veuillez ajouter ces dépendances à build.sbt :"
        echo '  "com.typesafe.play" %% "play-slick" % "5.1.0",'
        echo '  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",'
        echo '  "org.postgresql" % "postgresql" % "42.7.0",'
    fi
else
    echo_error "Fichier build.sbt manquant !"
fi

# =============================================================================
# ÉTAPE 4: INFORMATION JAVA VERSION
# =============================================================================

echo_step "Information version Java"

java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
echo_warning "Version Java détectée: $java_version"

if [ "$java_version" -eq 24 ]; then
    echo_error "Java 24 n'est pas supporté par Play Framework"
    echo_warning "Versions supportées: Java 11, 17, ou 21"
    echo ""
    echo_warning "Solutions possibles:"
    echo "  1. Installer Java 21 via SDKMAN:"
    echo "     curl -s \"https://get.sdkman.io\" | bash"
    echo "     sdk install java 21.0.1-tem"
    echo "     sdk use java 21.0.1-tem"
    echo ""
    echo "  2. Ou via Homebrew:"
    echo "     brew install openjdk@21"
    echo "     export JAVA_HOME=\$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
    echo ""
    echo "  3. Redémarrer sbt après changement de Java"
elif [ "$java_version" -ge 11 ] && [ "$java_version" -le 21 ]; then
    echo_success "Version Java compatible"
else
    echo_warning "Version Java possiblement incompatible"
fi

# =============================================================================
# ÉTAPE 5: DÉMARRAGE DES SERVICES
# =============================================================================

echo_step "Démarrage des services Docker"

# Arrêter les services existants
echo "🛑 Arrêt des services existants..."
docker-compose down 2>/dev/null || true

# Démarrer les nouveaux services
echo "🚀 Démarrage des nouveaux services..."
if docker-compose up -d postgres redis; then
    echo_success "Services PostgreSQL et Redis démarrés"
    
    # Attendre que PostgreSQL soit prêt
    echo "⏳ Attente PostgreSQL..."
    for i in {1..30}; do
        if docker-compose exec postgres pg_isready -U postgres -d appli_brassage > /dev/null 2>&1; then
            echo_success "PostgreSQL prêt après ${i}s"
            break
        fi
        sleep 1
    done
    
    # Vérifier Redis
    if docker-compose exec redis redis-cli ping | grep -q "PONG"; then
        echo_success "Redis opérationnel"
    else
        echo_warning "Redis pourrait ne pas être prêt"
    fi
    
else
    echo_error "Erreur lors du démarrage des services"
fi

# =============================================================================
# ÉTAPE 6: CRÉATION ÉVOLUTIONS BASIQUES SI NÉCESSAIRES
# =============================================================================

echo_step "Vérification évolutions base de données"

if [ ! -d "conf/evolutions/default" ]; then
    echo_warning "Répertoire évolutions manquant, création..."
    mkdir -p conf/evolutions/default
    
    # Créer une évolution basique si aucune n'existe
    if [ ! -f "conf/evolutions/default/1.sql" ]; then
        cat > conf/evolutions/default/1.sql << 'EVOLUTION_EOF'
-- Schema initial pour l'application de brassage

# --- !Ups

-- Table des houblons (existante ou à adapter)
CREATE TABLE IF NOT EXISTS hops (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    alpha_acid DECIMAL(4,2),
    beta_acid DECIMAL(4,2),
    usage VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    source VARCHAR(50) DEFAULT 'MANUAL',
    credibility_score INTEGER DEFAULT 100,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Table des arômes
CREATE TABLE IF NOT EXISTS aromas (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table des origines
CREATE TABLE IF NOT EXISTS origins (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100),
    region VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Relations houblon-arômes
CREATE TABLE IF NOT EXISTS hop_aromas (
    hop_id VARCHAR(255),
    aroma_id VARCHAR(255),
    intensity INTEGER DEFAULT 1,
    PRIMARY KEY (hop_id, aroma_id),
    FOREIGN KEY (hop_id) REFERENCES hops(id) ON DELETE CASCADE,
    FOREIGN KEY (aroma_id) REFERENCES aromas(id) ON DELETE CASCADE
);

-- Relations houblon-origines
CREATE TABLE IF NOT EXISTS hop_origins (
    hop_id VARCHAR(255),
    origin_id VARCHAR(255),
    PRIMARY KEY (hop_id, origin_id),
    FOREIGN KEY (hop_id) REFERENCES hops(id) ON DELETE CASCADE,
    FOREIGN KEY (origin_id) REFERENCES origins(id) ON DELETE CASCADE
);

-- Index pour les performances
CREATE INDEX IF NOT EXISTS idx_hops_name ON hops(name);
CREATE INDEX IF NOT EXISTS idx_hops_usage ON hops(usage);
CREATE INDEX IF NOT EXISTS idx_hops_status ON hops(status);

# --- !Downs

DROP TABLE IF EXISTS hop_origins;
DROP TABLE IF EXISTS hop_aromas;
DROP TABLE IF EXISTS origins;
DROP TABLE IF EXISTS aromas;
DROP TABLE IF EXISTS hops;
EVOLUTION_EOF

        echo_success "Évolution basique créée"
    fi
fi

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 =============================================================================="
echo "   RAPPORT DE CORRECTION DE L'INFRASTRUCTURE"
echo "==============================================================================${NC}"
echo ""

echo -e "${GREEN}✅ Actions effectuées:${NC}"
echo "   • Docker-compose corrigé avec services 'postgres' et 'redis'"
echo "   • Configuration application.conf vérifiée/créée"
echo "   • Services PostgreSQL et Redis démarrés"
echo "   • Évolutions base de données préparées"
echo ""

echo -e "${BLUE}🚀 Prochaines étapes:${NC}"
echo "   1. Corriger la version Java si nécessaire (Java 11, 17 ou 21)"
echo "   2. Vérifier les dépendances dans build.sbt"
echo "   3. Relancer Play Framework:"
echo "      sbt run"
echo ""
echo "   4. Tester l'infrastructure:"
echo "      chmod +x verify-all-phase1-apis.sh"
echo "      ./verify-all-phase1-apis.sh"
echo ""

echo -e "${BLUE}📋 Statut des services:${NC}"
docker-compose ps

echo ""
echo -e "${BLUE}🔗 URLs utiles:${NC}"
echo "   • Application: http://localhost:9000"
echo "   • PgAdmin: http://localhost:5050 (admin@brewery.com / admin123)"
echo "   • PostgreSQL: localhost:5432 (postgres/postgres)"
echo "   • Redis: localhost:6379"
echo ""

if [ "$java_version" -eq 24 ]; then
    echo -e "${RED}⚠️  ATTENTION: Java 24 non supporté par Play Framework${NC}"
    echo -e "${RED}   Installez Java 11, 17 ou 21 avant de continuer${NC}"
fi