#!/bin/bash

# =============================================================================
# DIAGNOSTIC APPROFONDI POSTGRESQL
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔬 Diagnostic approfondi PostgreSQL${NC}"
echo ""

# Test 1: Connexion depuis l'intérieur du container
echo -e "${BLUE}🧪 Test 1: Connexion interne au container${NC}"
if docker-compose exec postgres psql -U postgres -c "\l" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Connexion interne OK${NC}"
    
    # Lister les bases de données
    echo "📋 Bases de données disponibles:"
    docker-compose exec postgres psql -U postgres -c "\l" | grep -E "(Name|----|\s+appli_|postgres)" | head -10
    
    # Vérifier si la base appli_brassage existe
    DB_EXISTS=$(docker-compose exec postgres psql -U postgres -t -c "SELECT 1 FROM pg_database WHERE datname = 'appli_brassage';" | xargs)
    if [ "$DB_EXISTS" = "1" ]; then
        echo -e "${GREEN}✅ Base 'appli_brassage' existe${NC}"
        
        # Test connexion à cette base
        if docker-compose exec postgres psql -U postgres -d appli_brassage -c "SELECT current_database();" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Connexion à 'appli_brassage' OK${NC}"
        else
            echo -e "${RED}❌ Connexion à 'appli_brassage' échoue${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Base 'appli_brassage' n'existe pas${NC}"
        
        # Créer la base
        echo "🔧 Création de la base 'appli_brassage'..."
        docker-compose exec postgres psql -U postgres -c "CREATE DATABASE appli_brassage;" || echo "Erreur création base"
    fi
else
    echo -e "${RED}❌ Connexion interne échoue${NC}"
    
    # Vérifier les logs PostgreSQL
    echo "📋 Logs PostgreSQL (dernières lignes):"
    docker-compose logs postgres | tail -20
fi

echo ""

# Test 2: Configuration PostgreSQL interne
echo -e "${BLUE}🧪 Test 2: Configuration PostgreSQL${NC}"

# Vérifier pg_hba.conf (authentification)
echo "📋 Configuration authentification (pg_hba.conf):"
if docker-compose exec postgres cat /var/lib/postgresql/data/pg_hba.conf | grep -v "^#" | grep -v "^$"; then
    echo -e "${GREEN}✅ pg_hba.conf accessible${NC}"
else
    echo -e "${YELLOW}⚠️  pg_hba.conf non accessible${NC}"
fi

# Vérifier postgresql.conf
echo ""
echo "📋 Configuration écoute (postgresql.conf):"
LISTEN_CONFIG=$(docker-compose exec postgres grep "listen_addresses" /var/lib/postgresql/data/postgresql.conf || echo "non trouvé")
echo "Listen addresses: $LISTEN_CONFIG"

echo ""

# Test 3: Connexion réseau depuis l'hôte
echo -e "${BLUE}🧪 Test 3: Connexion réseau depuis l'hôte${NC}"

# Vérifier que le port est ouvert
if nc -z localhost 5432; then
    echo -e "${GREEN}✅ Port 5432 accessible${NC}"
else
    echo -e "${RED}❌ Port 5432 non accessible${NC}"
fi

# Test avec psql depuis l'hôte (si installé)
if command -v psql > /dev/null; then
    echo "🔧 Test psql depuis l'hôte..."
    if PGPASSWORD=postgres psql -h localhost -U postgres -d appli_brassage -c "SELECT 'Host connection OK';" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Connexion depuis l'hôte OK${NC}"
    else
        echo -e "${RED}❌ Connexion depuis l'hôte échoue${NC}"
        
        # Essayer sur la base postgres
        if PGPASSWORD=postgres psql -h localhost -U postgres -d postgres -c "SELECT 'Host connection OK';" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Connexion à 'postgres' depuis l'hôte OK${NC}"
        else
            echo -e "${RED}❌ Connexion à 'postgres' depuis l'hôte échoue aussi${NC}"
        fi
    fi
else
    echo -e "${YELLOW}⚠️  psql non installé sur l'hôte, test impossible${NC}"
fi

echo ""

# Test 4: Vérification variables d'environnement
echo -e "${BLUE}🧪 Test 4: Variables d'environnement container${NC}"
echo "📋 Variables PostgreSQL dans le container:"
docker-compose exec postgres env | grep "POSTGRES" || echo "Aucune variable POSTGRES trouvée"

echo ""

# Test 5: Processus PostgreSQL
echo -e "${BLUE}🧪 Test 5: Processus PostgreSQL${NC}"
echo "📋 Processus postgres dans le container:"
docker-compose exec postgres ps aux | grep postgres | head -5

echo ""

# Test 6: Reconstruction complète
echo -e "${BLUE}🔧 Solution: Reconstruction complète PostgreSQL${NC}"
echo ""
echo -e "${YELLOW}Cette solution va:${NC}"
echo "   1. Arrêter complètement PostgreSQL"
echo "   2. Supprimer les données existantes"
echo "   3. Redémarrer avec une base propre"
echo ""
read -p "Voulez-vous procéder ? (y/N) " -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}🔄 Reconstruction PostgreSQL...${NC}"
    
    # Arrêter les services
    echo "🛑 Arrêt des services..."
    docker-compose down
    
    # Supprimer les volumes
    echo "🗑️  Suppression des volumes PostgreSQL..."
    docker volume rm my-brew-app-v2_postgres_data 2>/dev/null || true
    
    # Redémarrer
    echo "🚀 Redémarrage avec base propre..."
    docker-compose up -d postgres
    
    # Attendre le démarrage
    echo "⏳ Attente démarrage PostgreSQL..."
    for i in {1..30}; do
        if docker-compose exec postgres pg_isready -U postgres > /dev/null 2>&1; then
            echo -e "${GREEN}✅ PostgreSQL redémarré après ${i}s${NC}"
            break
        fi
        sleep 1
    done
    
    # Créer la base appli_brassage
    echo "🏗️  Création base 'appli_brassage'..."
    docker-compose exec postgres psql -U postgres -c "CREATE DATABASE appli_brassage;" 2>/dev/null || echo "Base déjà existante"
    
    # Test final
    if docker-compose exec postgres psql -U postgres -d appli_brassage -c "SELECT 'Reconstruction réussie' as status;" > /dev/null 2>&1; then
        echo -e "${GREEN}🎉 Reconstruction PostgreSQL réussie !${NC}"
        echo ""
        echo -e "${GREEN}Vous pouvez maintenant relancer:${NC}"
        echo "   sbt run"
    else
        echo -e "${RED}❌ Reconstruction échouée${NC}"
    fi
    
    # Redémarrer Redis aussi
    docker-compose up -d redis
else
    echo ""
    echo -e "${BLUE}💡 Alternatives à essayer:${NC}"
    echo ""
    echo "1. Vérifier les logs détaillés:"
    echo "   docker-compose logs postgres | tail -50"
    echo ""
    echo "2. Se connecter au container pour debug:"
    echo "   docker-compose exec postgres bash"
    echo "   psql -U postgres"
    echo ""
    echo "3. Utiliser une base de données différente temporairement:"
    echo "   Modifier application.conf pour pointer vers 'postgres' au lieu de 'appli_brassage'"
fi