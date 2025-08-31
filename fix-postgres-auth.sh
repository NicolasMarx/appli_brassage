#!/bin/bash

# =============================================================================
# CORRECTION AUTHENTIFICATION POSTGRESQL
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🔐 =============================================================================="
echo "   CORRECTION AUTHENTIFICATION POSTGRESQL"
echo "=============================================================================="
echo -e "${NC}"

echo_step() { echo -e "${BLUE}📋 $1${NC}"; }
echo_success() { echo -e "${GREEN}✅ $1${NC}"; }
echo_error() { echo -e "${RED}❌ $1${NC}"; }
echo_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# =============================================================================
# DIAGNOSTIC DU PROBLÈME
# =============================================================================

echo_step "Diagnostic des configurations"

# Vérifier la configuration docker-compose
echo "🔍 Configuration Docker-compose:"
if grep -A 10 "postgres:" docker-compose.yml | grep -E "(POSTGRES_USER|POSTGRES_PASSWORD)"; then
    DOCKER_USER=$(grep -A 10 "postgres:" docker-compose.yml | grep "POSTGRES_USER" | cut -d':' -f2 | xargs)
    DOCKER_PASS=$(grep -A 10 "postgres:" docker-compose.yml | grep "POSTGRES_PASSWORD" | cut -d':' -f2 | xargs)
    DOCKER_DB=$(grep -A 10 "postgres:" docker-compose.yml | grep "POSTGRES_DB" | cut -d':' -f2 | xargs)
    
    echo "   User: $DOCKER_USER"
    echo "   Password: $DOCKER_PASS"
    echo "   Database: $DOCKER_DB"
else
    echo_error "Configuration PostgreSQL non trouvée dans docker-compose.yml"
fi

echo ""
echo "🔍 Configuration application.conf:"
if grep -A 5 "slick.dbs.default" conf/application.conf | grep -E "(user|password|url)"; then
    echo "Configuration actuelle dans application.conf:"
    grep -A 10 "slick.dbs.default" conf/application.conf | grep -E "(user|password|url)"
else
    echo_warning "Configuration Slick non trouvée"
fi

echo ""

# =============================================================================
# CORRECTION AUTOMATIQUE
# =============================================================================

echo_step "Correction de la configuration"

# Sauvegarder la configuration actuelle
cp conf/application.conf conf/application.conf.backup
echo_success "Configuration actuelle sauvegardée"

# Corriger la configuration Slick pour correspondre au Docker
cat > temp_slick_config << 'SLICK_EOF'

# =============================================================================
# CONFIGURATION BASE DE DONNÉES SLICK - CORRIGÉE
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
    connectionTimeout = 30000
    idleTimeout = 600000
    maxLifetime = 1800000
  }
}

# Configuration Play pour évolutions
play.evolutions.db.default {
  enabled = true
  autoApply = true
  autoApplyDowns = false
}
SLICK_EOF

# Supprimer l'ancienne configuration Slick et ajouter la nouvelle
if grep -q "slick.dbs.default" conf/application.conf; then
    # Supprimer la section slick existante
    sed -i.bak '/^slick\.dbs\.default/,/^}/d' conf/application.conf 2>/dev/null || \
    sed -i '/^slick\.dbs\.default/,/^}/d' conf/application.conf
    
    # Supprimer la section play.evolutions si elle existe
    sed -i.bak '/^play\.evolutions\.db\.default/,/^}/d' conf/application.conf 2>/dev/null || \
    sed -i '/^play\.evolutions\.db\.default/,/^}/d' conf/application.conf
    
    echo_success "Ancienne configuration Slick supprimée"
fi

# Ajouter la nouvelle configuration
cat temp_slick_config >> conf/application.conf
rm temp_slick_config
rm -f conf/application.conf.bak 2>/dev/null || true

echo_success "Nouvelle configuration Slick ajoutée"

# =============================================================================
# TEST DE CONNEXION
# =============================================================================

echo_step "Test de connexion directe à PostgreSQL"

# Tester la connexion avec les credentials Docker
if docker-compose exec postgres psql -U postgres -d appli_brassage -c "SELECT 'Connection OK' as status;" > /dev/null 2>&1; then
    echo_success "Connexion PostgreSQL réussie avec user 'postgres'"
else
    echo_error "Connexion PostgreSQL échoue toujours"
    
    # Essayer de diagnostiquer plus
    echo "🔍 Diagnostic approfondi:"
    
    # Vérifier que le container est vraiment up
    if docker-compose ps postgres | grep -q "Up"; then
        echo_success "Container PostgreSQL actif"
    else
        echo_error "Container PostgreSQL non actif"
        echo "Redémarrage du container..."
        docker-compose restart postgres
        sleep 5
    fi
    
    # Tester avec différents users
    echo "Test différents utilisateurs..."
    
    # Test 1: postgres/postgres
    if docker-compose exec postgres psql -U postgres -d postgres -c "SELECT version();" > /dev/null 2>&1; then
        echo_success "Connexion avec postgres/postgres sur DB 'postgres' OK"
        
        # Créer la base appli_brassage si elle n'existe pas
        echo "Création base 'appli_brassage' si nécessaire..."
        docker-compose exec postgres psql -U postgres -c "CREATE DATABASE appli_brassage;" 2>/dev/null || echo "Base existe déjà"
        
        # Test sur la bonne base
        if docker-compose exec postgres psql -U postgres -d appli_brassage -c "SELECT 'OK';" > /dev/null 2>&1; then
            echo_success "Connexion sur base 'appli_brassage' OK"
        fi
    fi
fi

# =============================================================================
# REDÉMARRAGE DES SERVICES
# =============================================================================

echo_step "Redémarrage des services pour appliquer la configuration"

docker-compose restart postgres
echo "⏳ Attente redémarrage PostgreSQL..."

for i in {1..20}; do
    if docker-compose exec postgres pg_isready -U postgres -d appli_brassage > /dev/null 2>&1; then
        echo_success "PostgreSQL redémarré et prêt après ${i}s"
        break
    fi
    sleep 1
done

# Test final de connexion
if docker-compose exec postgres psql -U postgres -d appli_brassage -c "SELECT 'Final test OK' as status;" > /dev/null 2>&1; then
    echo_success "Test final de connexion PostgreSQL réussi"
else
    echo_error "Test final de connexion échoue"
fi

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo_step "Configuration finale"

echo -e "${GREEN}Configuration corrigée:${NC}"
echo "   • User: postgres"
echo "   • Password: postgres" 
echo "   • Database: appli_brassage"
echo "   • URL: jdbc:postgresql://localhost:5432/appli_brassage"
echo ""

echo -e "${BLUE}Fichiers modifiés:${NC}"
echo "   • conf/application.conf (sauvegarde: conf/application.conf.backup)"
echo ""

echo -e "${BLUE}Prochaines étapes:${NC}"
echo "1. Redémarrez sbt si il était lancé:"
echo "   Appuyez sur Entrée dans sbt pour l'arrêter"
echo "   Puis: sbt run"
echo ""
echo "2. L'application devrait maintenant se connecter à PostgreSQL"
echo ""
echo "3. Testez avec:"
echo "   curl http://localhost:9000/api/v1/hops"
echo ""

# Créer un petit script de test
cat > test-connection.sh << 'TEST_EOF'
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
TEST_EOF

chmod +x test-connection.sh
echo_success "Script de test créé: ./test-connection.sh"