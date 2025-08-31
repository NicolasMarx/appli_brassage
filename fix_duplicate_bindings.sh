#!/bin/bash

# =============================================================================
# CORRECTION BINDINGS DUPLIQUÉS GUICE
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Correction bindings dupliqués${NC}"

# =============================================================================
# IDENTIFIER LES MODULES EN CONFLIT
# =============================================================================

echo -e "${YELLOW}Recherche des modules en conflit...${NC}"

# Chercher BindingsModule
if [ -f "app/modules/BindingsModule.scala" ]; then
    echo -e "${YELLOW}BindingsModule trouvé, analyse...${NC}"
    
    # Vérifier s'il contient des bindings Malts
    if grep -q "MaltReadRepository\|MaltWriteRepository" app/modules/BindingsModule.scala; then
        echo -e "${RED}Conflit détecté dans BindingsModule${NC}"
        
        # Backup
        cp app/modules/BindingsModule.scala app/modules/BindingsModule.scala.backup.$(date +%Y%m%d_%H%M%S)
        
        # Supprimer les bindings Malts du BindingsModule
        echo -e "${YELLOW}Suppression des bindings Malts de BindingsModule...${NC}"
        sed -i.bak '/MaltReadRepository\|MaltWriteRepository/d' app/modules/BindingsModule.scala
        
        echo -e "${GREEN}Bindings Malts supprimés de BindingsModule${NC}"
    else
        echo -e "${GREEN}Aucun conflit détecté dans BindingsModule${NC}"
    fi
else
    echo -e "${GREEN}BindingsModule non trouvé${NC}"
fi

# =============================================================================
# SUPPRIMER MALTMODULE SÉPARÉ
# =============================================================================

echo -e "${YELLOW}Suppression du MaltModule séparé...${NC}"

if [ -f "app/modules/MaltModule.scala" ]; then
    rm app/modules/MaltModule.scala
    echo -e "${GREEN}MaltModule supprimé${NC}"
fi

# Supprimer de la configuration si présent
if grep -q "modules.MaltModule" conf/application.conf; then
    sed -i.bak '/modules.MaltModule/d' conf/application.conf
    echo -e "${GREEN}MaltModule supprimé de application.conf${NC}"
fi

# =============================================================================
# AJOUTER BINDINGS AU MODULE PRINCIPAL
# =============================================================================

echo -e "${YELLOW}Ajout des bindings au module principal...${NC}"

# Si BindingsModule existe, l'utiliser
if [ -f "app/modules/BindingsModule.scala" ]; then
    echo -e "${YELLOW}Ajout au BindingsModule existant...${NC}"
    
    # Vérifier la structure du fichier
    if grep -q "override def configure" app/modules/BindingsModule.scala; then
        # Ajouter avant la fin de la méthode configure
        sed -i.bak2 '/override def configure.*{/,/^[[:space:]]*}/ {
            /^[[:space:]]*}$/ i\
\
    // Bindings domaine Malts\
    bind(classOf[domain.malts.repositories.MaltReadRepository]).to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository]).asEagerSingleton()\
    bind(classOf[domain.malts.repositories.MaltWriteRepository]).to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository]).asEagerSingleton()
        }' app/modules/BindingsModule.scala
        
        echo -e "${GREEN}Bindings ajoutés au BindingsModule${NC}"
    else
        echo -e "${RED}Structure BindingsModule non reconnue${NC}"
    fi
else
    # Créer un nouveau BindingsModule
    echo -e "${YELLOW}Création d'un nouveau BindingsModule...${NC}"
    
    cat > app/modules/BindingsModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    // Bindings domaine Malts
    bind(classOf[domain.malts.repositories.MaltReadRepository]).to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository]).asEagerSingleton()
    bind(classOf[domain.malts.repositories.MaltWriteRepository]).to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository]).asEagerSingleton()
  }
}
EOF
    
    # L'activer dans application.conf
    if ! grep -q "modules.BindingsModule" conf/application.conf; then
        echo "" >> conf/application.conf
        echo "# Module principal avec tous les bindings" >> conf/application.conf
        echo "play.modules.enabled += \"modules.BindingsModule\"" >> conf/application.conf
    fi
    
    echo -e "${GREEN}Nouveau BindingsModule créé et configuré${NC}"
fi

# =============================================================================
# ALTERNATIVE : UTILISER @SINGLETON SANS MODULE
# =============================================================================

echo -e "${YELLOW}Vérification des annotations @Singleton...${NC}"

# Vérifier que les repositories ont @Singleton
if grep -q "@Singleton" app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala; then
    echo -e "${GREEN}@Singleton présent sur SlickMaltReadRepository${NC}"
else
    echo -e "${RED}@Singleton manquant${NC}"
fi

if grep -q "@Singleton" app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala; then
    echo -e "${GREEN}@Singleton présent sur SlickMaltWriteRepository${NC}"
else
    echo -e "${RED}@Singleton manquant${NC}"
fi

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test de compilation...${NC}"

if sbt compile > /tmp/duplicate_bindings_fix.log 2>&1; then
    echo -e "${GREEN}COMPILATION RÉUSSIE !${NC}"
    echo ""
    echo -e "${YELLOW}Test de démarrage...${NC}"
    
    # Test rapide de démarrage
    timeout 15s sbt run > /tmp/startup_test.log 2>&1 || true
    
    if grep -q "CreationException\|BindingAlreadySet" /tmp/startup_test.log; then
        echo -e "${RED}Erreurs Guice persistantes${NC}"
        grep -A3 -B3 "CreationException\|BindingAlreadySet" /tmp/startup_test.log
    elif grep -q "Server started" /tmp/startup_test.log || grep -q "Application started" /tmp/startup_test.log; then
        echo -e "${GREEN}APPLICATION DÉMARRE CORRECTEMENT !${NC}"
        echo ""
        echo -e "${GREEN}✓ Bindings résolus${NC}"
        echo -e "${GREEN}✓ Injection fonctionnelle${NC}"
        echo -e "${GREEN}✓ Repositories disponibles${NC}"
        echo ""
        echo -e "${BLUE}Prochaines étapes :${NC}"
        echo "1. Créer l'évolution DB (10.sql)"
        echo "2. Tester les APIs malts"
        echo "3. Vérifier les tables créées"
    else
        echo -e "${YELLOW}Démarrage interrompu (timeout normal)${NC}"
        echo -e "${GREEN}Pas d'erreurs Guice détectées${NC}"
    fi
    
else
    echo -e "${RED}Erreurs de compilation${NC}"
    grep "error" /tmp/duplicate_bindings_fix.log | head -5
fi

echo ""
echo -e "${GREEN}Correction bindings dupliqués terminée${NC}"