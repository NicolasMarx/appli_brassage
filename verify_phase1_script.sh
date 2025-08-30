#!/bin/bash

# =============================================================================
# SCRIPT DE VÉRIFICATION PHASE 1 - FONDATIONS DDD/CQRS
# =============================================================================
# Vérifie que tous les éléments critiques de la Phase 1 sont en place
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🔍 =============================================================================="
echo "   VÉRIFICATION PHASE 1 - FONDATIONS DDD/CQRS"
echo "==============================================================================${NC}"

ERRORS=0
WARNINGS=0

# Fonction d'affichage des résultats
check_ok() {
    echo -e "   ${GREEN}✅ $1${NC}"
}

check_error() {
    echo -e "   ${RED}❌ $1${NC}"
    ((ERRORS++))
}

check_warning() {
    echo -e "   ${YELLOW}⚠️  $1${NC}"
    ((WARNINGS++))
}

# =============================================================================
# VÉRIFICATION STRUCTURE DE FICHIERS
# =============================================================================

echo ""
echo -e "${BLUE}📁 Vérification structure de fichiers${NC}"

# Value Objects
if [ -f "app/domain/shared/Email.scala" ]; then
    check_ok "Email.scala présent"
else
    check_error "Email.scala manquant"
fi

if [ -f "app/domain/shared/NonEmptyString.scala" ]; then
    check_ok "NonEmptyString.scala présent"
else
    check_error "NonEmptyString.scala manquant"
fi

if [ -f "app/domain/shared/Percentage.scala" ]; then
    check_ok "Percentage.scala présent"
else
    check_error "Percentage.scala manquant"
fi

# Domaine commun
if [ -f "app/domain/common/DomainError.scala" ]; then
    check_ok "DomainError.scala présent"
else
    check_error "DomainError.scala manquant"
fi

if [ -f "app/domain/common/DomainEvent.scala" ]; then
    check_ok "DomainEvent.scala présent"
else
    check_error "DomainEvent.scala manquant"
fi

# Admin
if [ -f "app/domain/admin/model/AdminId.scala" ]; then
    check_ok "AdminId.scala présent"
else
    check_error "AdminId.scala manquant"
fi

if [ -f "app/domain/admin/model/AdminRole.scala" ]; then
    check_ok "AdminRole.scala présent"
else
    check_error "AdminRole.scala manquant"
fi

if [ -f "app/domain/admin/model/AdminAggregate.scala" ]; then
    check_ok "AdminAggregate.scala présent"
else
    check_error "AdminAggregate.scala manquant"
fi

if [ -f "app/domain/admin/model/AdminEvents.scala" ]; then
    check_ok "AdminEvents.scala présent"
else
    check_error "AdminEvents.scala manquant"
fi

# Services
if [ -f "app/domain/admin/services/PasswordService.scala" ]; then
    check_ok "PasswordService.scala présent"
else
    check_error "PasswordService.scala manquant"
fi

# Configuration
if [ -f "conf/evolutions/default/1.sql" ]; then
    check_ok "Évolution 1.sql présente"
else
    check_error "Évolution 1.sql manquante"
fi

# =============================================================================
# VÉRIFICATION COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}🔨 Vérification compilation${NC}"

echo "   Compilation en cours..."
if sbt compile > /tmp/compile.log 2>&1; then
    check_ok "Compilation réussie"
else
    check_error "Erreurs de compilation détectées"
    echo -e "${RED}   Détails des erreurs de compilation :${NC}"
    tail -20 /tmp/compile.log | sed 's/^/     /'
fi

# =============================================================================
# VÉRIFICATION BASE DE DONNÉES
# =============================================================================

echo ""
echo -e "${BLUE}🗄️  Vérification base de données${NC}"

# Vérifier Docker Compose
if command -v docker-compose &> /dev/null; then
    check_ok "Docker Compose disponible"
    
    # Vérifier si PostgreSQL tourne
    if docker-compose ps | grep -q postgres.*Up; then
        check_ok "PostgreSQL en cours d'exécution"
        
        # Vérifier connectivité
       if docker-compose exec -T db psql -U postgres -d appli_brassage -c "SELECT 1;" > /dev/null 2>&1; then

            check_ok "Connexion PostgreSQL fonctionnelle"
            
            # Vérifier tables créées
           TABLES=$(docker-compose exec -T db psql -U postgres -d appli_brassage -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")
            if [ "$TABLES" -gt 5 ]; then
                check_ok "Tables créées ($TABLES tables détectées)"
            else
                check_warning "Peu de tables créées ($TABLES), évolutions peut-être non appliquées"
            fi
        else
            check_error "Impossible de se connecter à PostgreSQL"
        fi
    else
        check_warning "PostgreSQL n'est pas en cours d'exécution (docker-compose up -d postgres)"
    fi
else
    check_warning "Docker Compose non disponible"
fi

# =============================================================================
# VÉRIFICATION DÉPENDANCES
# =============================================================================

echo ""
echo -e "${BLUE}📦 Vérification dépendances${NC}"

# Vérifier build.sbt
if grep -q "jbcrypt" build.sbt 2>/dev/null; then
    check_ok "Dépendance jbcrypt présente"
else
    check_warning "Dépendance jbcrypt non trouvée dans build.sbt"
fi

if grep -q "play-slick" build.sbt 2>/dev/null; then
    check_ok "Dépendance play-slick présente"
else
    check_warning "Dépendance play-slick non trouvée dans build.sbt"
fi

if grep -q "postgresql" build.sbt 2>/dev/null; then
    check_ok "Driver PostgreSQL présent"
else
    check_warning "Driver PostgreSQL non trouvé dans build.sbt"
fi

# =============================================================================
# VÉRIFICATION CONFIGURATION
# =============================================================================

echo ""
echo -e "${BLUE}⚙️  Vérification configuration${NC}"

if [ -f "conf/application.conf" ]; then
    check_ok "Configuration application.conf présente"
    
    if grep -q "slick.dbs.default" conf/application.conf; then
        check_ok "Configuration Slick trouvée"
    else
        check_warning "Configuration Slick non trouvée"
    fi
    
    if grep -q "ai.monitoring" conf/application.conf; then
        check_ok "Configuration IA trouvée"
    else
        check_warning "Configuration IA non trouvée"
    fi
else
    check_error "Fichier application.conf manquant"
fi

if [ -f "conf/routes" ]; then
    check_ok "Fichier routes présent"
    
    if grep -q "/api/admin" conf/routes; then
        check_ok "Routes API admin trouvées"
    else
        check_warning "Routes API admin non trouvées"
    fi
else
    check_error "Fichier routes manquant"
fi

# =============================================================================
# VÉRIFICATION SÉCURITÉ
# =============================================================================

echo ""
echo -e "${BLUE}🔐 Vérification sécurité${NC}"

# Vérifier configuration sécurité
if grep -q "play.http.secret.key.*changeme" conf/application.conf 2>/dev/null; then
    check_warning "Clé secrète par défaut détectée (à changer en production)"
else
    check_ok "Clé secrète configurée"
fi

if grep -q "CSRF" conf/application.conf 2>/dev/null; then
    check_ok "Protection CSRF configurée"
else
    check_warning "Protection CSRF non trouvée"
fi

# =============================================================================
# TESTS UNITAIRES BASIQUES
# =============================================================================

echo ""
echo -e "${BLUE}🧪 Vérification tests${NC}"

if [ -d "test" ]; then
    check_ok "Répertoire test présent"
    
    echo "   Exécution tests unitaires de base..."
    if sbt "testOnly *Email*" > /tmp/test.log 2>&1; then
        check_ok "Tests Value Objects passent"
    else
        check_warning "Certains tests échouent (vérifiez /tmp/test.log)"
    fi
else
    check_warning "Répertoire test manquant"
fi

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 Rapport final Phase 1${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ PHASE 1 VALIDÉE - Fondations solides !${NC}"
    echo ""
    echo -e "${GREEN}Prêt pour la Phase 2 - Domaine Hops${NC}"
    echo ""
    echo -e "${BLUE}Prochaines étapes :${NC}"
    echo "   1. Implémenter HopAggregate"
    echo "   2. Créer API publique houblons"
    echo "   3. Créer interface admin houblons"
    echo "   4. Implémenter premiers tests d'intégration"
    
elif [ $ERRORS -le 2 ] && [ $WARNINGS -le 5 ]; then
    echo -e "${YELLOW}⚠️  PHASE 1 PARTIELLEMENT VALIDÉE${NC}"
    echo ""
    echo -e "${YELLOW}$ERRORS erreur(s) et $WARNINGS avertissement(s)${NC}"
    echo ""
    echo -e "${BLUE}Corrections recommandées avant Phase 2 :${NC}"
    echo "   - Corriger les erreurs de compilation"
    echo "   - Vérifier la base de données"
    echo "   - Compléter la configuration manquante"
    
else
    echo -e "${RED}❌ PHASE 1 NON VALIDÉE${NC}"
    echo ""
    echo -e "${RED}$ERRORS erreur(s) critique(s) et $WARNINGS avertissement(s)${NC}"
    echo ""
    echo -e "${RED}Actions requises :${NC}"
    echo "   - Corriger toutes les erreurs de compilation"
    echo "   - Implémenter les Value Objects manquants"
    echo "   - Configurer la base de données"
    echo "   - Revoir la structure des fichiers"
    echo ""
    echo -e "${RED}Ne pas passer à la Phase 2 avant validation complète${NC}"
fi

echo ""
echo -e "${BLUE}Logs détaillés disponibles :${NC}"
echo "   - Compilation : /tmp/compile.log"
echo "   - Tests : /tmp/test.log"
echo ""

exit $ERRORS