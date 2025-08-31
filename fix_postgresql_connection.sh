#!/bin/bash

# =============================================================================
# SCRIPT DE DIAGNOSTIC ET RÉPARATION - PostgreSQL
# Diagnostique et résout les problèmes de connexion PostgreSQL
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo_step() { echo -e "${BLUE}🔧 $1${NC}"; echo "═══════════════════════════════════════"; }
echo_success() { echo -e "${GREEN}✅ $1${NC}"; }
echo_error() { echo -e "${RED}❌ $1${NC}"; }
echo_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
echo_info() { echo -e "${CYAN}ℹ️  $1${NC}"; }

# =============================================================================
# ÉTAPE 1 : DIAGNOSTIC INITIAL
# =============================================================================

echo_step "DIAGNOSTIC POSTGRESQL"

# Vérifier les containers PostgreSQL
echo_info "Containers PostgreSQL actifs:"
POSTGRES_CONTAINERS=$(docker ps --filter "name=postgres" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}")
echo "$POSTGRES_CONTAINERS"

if [ -z "$POSTGRES_CONTAINERS" ] || [ "$POSTGRES_CONTAINERS" = "NAMES	STATUS	PORTS" ]; then
    echo_warning "Aucun container PostgreSQL trouvé avec le nom 'postgres'"
    echo_info "Recherche de containers avec PostgreSQL dans l'image..."
    docker ps --filter "ancestor=postgres" --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
fi

# Lister tous les containers actifs
echo_info "Tous les containers actifs:"
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

# Identifier le bon container
echo ""
echo_warning "Quel est le nom de votre container PostgreSQL ?"
echo_info "Options probables basées sur les containers actifs:"
docker ps --format "{{.Names}}" | grep -E "(postgres|db|database)" || echo "Aucun container avec nom typique trouvé"

echo ""
read -p "Entrez le nom du container PostgreSQL: " DB_CONTAINER

if [ -z "$DB_CONTAINER" ]; then
    echo_error "Nom de container requis"
    exit 1
fi

# =============================================================================
# ÉTAPE 2 : DIAGNOSTIC APPROFONDI
# =============================================================================

echo_step "DIAGNOSTIC APPROFONDI - Container: $DB_CONTAINER"

# Vérifier que le container existe et tourne
if ! docker ps | grep -q "$DB_CONTAINER"; then
    echo_error "Container $DB_CONTAINER non trouvé ou arrêté"
    
    # Vérifier s'il existe mais est arrêté
    if docker ps -a | grep -q "$DB_CONTAINER"; then
        echo_warning "Container $DB_CONTAINER existe mais est arrêté"
        echo_info "Tentative de démarrage..."
        docker start "$DB_CONTAINER"
        sleep 5
    else
        echo_error "Container $DB_CONTAINER n'existe pas"
        echo_info "Lancez: docker-compose up -d"
        exit 1
    fi
fi

# Vérifier les logs du container
echo_info "Derniers logs du container PostgreSQL:"
docker logs --tail 20 "$DB_CONTAINER"

# Vérifier le status du processus PostgreSQL
echo_info "Status du processus PostgreSQL dans le container:"
docker exec "$DB_CONTAINER" ps aux | grep postgres || echo "Erreur: impossible d'exécuter ps dans le container"

# Vérifier les ports exposés
echo_info "Ports exposés par le container:"
docker port "$DB_CONTAINER" || echo "Aucun port exposé trouvé"

# Vérifier la connectivité réseau
echo_info "Test connectivité réseau vers le container:"
docker exec "$DB_CONTAINER" netstat -ln | grep :5432 || echo "Port 5432 non en écoute"

# =============================================================================
# ÉTAPE 3 : TESTS DE CONNEXION
# =============================================================================

echo_step "TESTS DE CONNEXION"

# Test pg_isready avec différents utilisateurs
echo_info "Test pg_isready avec différents utilisateurs:"

for user in "postgres" "root" "admin"; do
    echo -n "  Testing user $user: "
    if docker exec "$DB_CONTAINER" pg_isready -U "$user" >/dev/null 2>&1; then
        echo_success "OK"
        WORKING_USER="$user"
        break
    else
        echo_error "Failed"
    fi
done

# Si pg_isready ne fonctionne avec aucun utilisateur
if [ -z "$WORKING_USER" ]; then
    echo_warning "pg_isready échoue avec tous les utilisateurs"
    
    # Vérifier si PostgreSQL est démarré dans le container
    echo_info "Vérification démarrage PostgreSQL..."
    if docker exec "$DB_CONTAINER" pgrep postgres >/dev/null 2>&1; then
        echo_success "Processus PostgreSQL détecté"
    else
        echo_error "Aucun processus PostgreSQL trouvé"
        echo_info "PostgreSQL semble ne pas être démarré dans le container"
    fi
else
    echo_success "Utilisateur fonctionnel trouvé: $WORKING_USER"
fi

# =============================================================================
# ÉTAPE 4 : DIAGNOSTIC BASE DE DONNÉES
# =============================================================================

if [ -n "$WORKING_USER" ]; then
    echo_step "DIAGNOSTIC BASE DE DONNÉES"
    
    # Lister les bases de données disponibles
    echo_info "Bases de données disponibles:"
    docker exec "$DB_CONTAINER" psql -U "$WORKING_USER" -l 2>/dev/null | grep -E "^ [a-zA-Z]" || echo "Erreur listage bases de données"
    
    # Tester connexion à différentes bases
    for db in "appli_brassage" "postgres" "template1"; do
        echo -n "  Test connexion base $db: "
        if docker exec "$DB_CONTAINER" psql -U "$WORKING_USER" -d "$db" -c "SELECT 1;" >/dev/null 2>&1; then
            echo_success "OK"
            WORKING_DB="$db"
            break
        else
            echo_warning "Failed"
        fi
    done
    
    if [ -n "$WORKING_DB" ]; then
        echo_success "Base de données fonctionnelle: $WORKING_DB"
        
        # Test requête simple
        echo_info "Test requête simple:"
        docker exec "$DB_CONTAINER" psql -U "$WORKING_USER" -d "$WORKING_DB" -c "SELECT current_database(), current_user, version();" 2>/dev/null || echo "Erreur requête test"
    fi
fi

# =============================================================================
# ÉTAPE 5 : SOLUTIONS PROPOSÉES
# =============================================================================

echo_step "SOLUTIONS PROPOSÉES"

if [ -z "$WORKING_USER" ]; then
    echo_warning "PostgreSQL n'accepte aucune connexion"
    
    echo_info "Solutions à essayer:"
    echo "1. 🔄 Redémarrer le container PostgreSQL:"
    echo "   docker restart $DB_CONTAINER"
    echo ""
    echo "2. 🔄 Redémarrer complètement avec docker-compose:"
    echo "   docker-compose down"
    echo "   docker-compose up -d"
    echo ""
    echo "3. 🔍 Vérifier les logs complets:"
    echo "   docker logs $DB_CONTAINER"
    echo ""
    echo "4. 🗄️ Réinitialiser complètement PostgreSQL (ATTENTION: perte de données):"
    echo "   docker-compose down -v"
    echo "   docker-compose up -d"
    
    # Tentative de redémarrage automatique
    echo ""
    read -p "Voulez-vous que je redémarre automatiquement le container? (y/N): " RESTART_CHOICE
    if [[ "$RESTART_CHOICE" =~ ^[Yy]$ ]]; then
        echo_info "Redémarrage du container $DB_CONTAINER..."
        docker restart "$DB_CONTAINER"
        
        echo_info "Attente du redémarrage (30 secondes)..."
        sleep 30
        
        echo_info "Test après redémarrage:"
        if docker exec "$DB_CONTAINER" pg_isready -U postgres >/dev/null 2>&1; then
            echo_success "PostgreSQL fonctionne après redémarrage!"
        else
            echo_error "Problème persiste après redémarrage"
        fi
    fi
    
elif [ -n "$WORKING_USER" ] && [ -n "$WORKING_DB" ]; then
    echo_success "PostgreSQL fonctionne correctement!"
    
    echo_info "Configuration détectée:"
    echo "  🐘 Container: $DB_CONTAINER"
    echo "  👤 Utilisateur: $WORKING_USER"
    echo "  🗄️ Base de données: $WORKING_DB"
    
    echo_info "Mettez à jour vos scripts avec ces paramètres:"
    echo "  DB_CONTAINER=\"$DB_CONTAINER\""
    echo "  DB_USER=\"$WORKING_USER\""
    echo "  DB_NAME=\"$WORKING_DB\""
    
    # Proposer de mettre à jour le script de vérification
    echo ""
    read -p "Voulez-vous que je mette à jour le script de vérification avec ces paramètres? (y/N): " UPDATE_CHOICE
    if [[ "$UPDATE_CHOICE" =~ ^[Yy]$ ]]; then
        # Créer une version mise à jour du script
        sed -i.bak \
            -e "s/DB_CONTAINER=\"postgres\"/DB_CONTAINER=\"$DB_CONTAINER\"/" \
            -e "s/DB_USER=\"postgres\"/DB_USER=\"$WORKING_USER\"/" \
            -e "s/DB_NAME=\"appli_brassage\"/DB_NAME=\"$WORKING_DB\"/" \
            verification_malts_script.sh
        
        echo_success "Script verification_malts_script.sh mis à jour!"
        echo_info "Sauvegarde créée: verification_malts_script.sh.bak"
    fi
fi

# =============================================================================
# ÉTAPE 6 : VÉRIFICATION DOCKER-COMPOSE
# =============================================================================

echo_step "VÉRIFICATION DOCKER-COMPOSE"

if [ -f "docker-compose.yml" ]; then
    echo_success "docker-compose.yml trouvé"
    
    echo_info "Configuration PostgreSQL dans docker-compose.yml:"
    grep -A 10 -B 2 "postgres\|db:" docker-compose.yml | head -20
    
elif [ -f "docker-compose.new.yml" ]; then
    echo_warning "docker-compose.yml manquant mais docker-compose.new.yml trouvé"
    echo_info "Renommez probablement: mv docker-compose.new.yml docker-compose.yml"
    
else
    echo_error "Aucun fichier docker-compose trouvé"
    echo_info "Créez un docker-compose.yml avec la configuration PostgreSQL"
fi

echo ""
echo_step "DIAGNOSTIC TERMINÉ"
echo_info "Prochaines étapes selon le résultat:"
echo "  ✅ Si PostgreSQL fonctionne: relancez ./verification_malts_script.sh"
echo "  🔄 Si problème persiste: suivez les solutions proposées ci-dessus"
echo "  💬 Si besoin d'aide: partagez les logs affichés ci-dessus"