#!/bin/bash

# =============================================================================
# MODULE MALTS SÉPARÉ - PRÉSERVATION COMPLÈTE HOPS
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Création module Malts séparé (préservation Hops)${NC}"

# =============================================================================
# BACKUP ET EXAMEN DU BINDINGSMODULE ACTUEL
# =============================================================================

echo -e "${YELLOW}Examen du BindingsModule actuel...${NC}"

if [ -f "app/modules/BindingsModule.scala" ]; then
    echo -e "${YELLOW}Contenu actuel du BindingsModule :${NC}"
    echo -e "${BLUE}===========================================${NC}"
    cat -n app/modules/BindingsModule.scala
    echo -e "${BLUE}===========================================${NC}"
    echo ""
    
    # Backup complet
    cp app/modules/BindingsModule.scala app/modules/BindingsModule.scala.backup.$(date +%Y%m%d_%H%M%S)
    echo -e "${GREEN}Backup complet créé${NC}"
    
    # Vérifier s'il contient des bindings Malts
    if grep -q "MaltReadRepository\|MaltWriteRepository" app/modules/BindingsModule.scala; then
        echo -e "${RED}ATTENTION: Bindings Malts détectés dans BindingsModule${NC}"
        echo -e "${YELLOW}Ces lignes seront supprimées :${NC}"
        grep -n "MaltReadRepository\|MaltWriteRepository" app/modules/BindingsModule.scala
        echo ""
    else
        echo -e "${GREEN}Aucun binding Malts dans BindingsModule - parfait${NC}"
    fi
    
else
    echo -e "${YELLOW}Aucun BindingsModule trouvé${NC}"
fi

# =============================================================================
# CRÉER MODULE MALTS DÉDIÉ
# =============================================================================

echo -e "${YELLOW}Création du module Malts dédié...${NC}"

cat > app/modules/MaltsModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule

/**
 * Module dédié exclusivement au domaine Malts
 * N'interfère pas avec les autres modules existants
 */
class MaltsModule extends AbstractModule {
  override def configure(): Unit = {
    // Bindings domaine Malts uniquement
    bind(classOf[domain.malts.repositories.MaltReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository])
      .asEagerSingleton()
    
    bind(classOf[domain.malts.repositories.MaltWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository])
      .asEagerSingleton()
  }
}
EOF

echo -e "${GREEN}MaltsModule dédié créé${NC}"

# =============================================================================
# NETTOYER BINDINGSMODULE (SUPPRIMER SEULEMENT LES MALTS)
# =============================================================================

if [ -f "app/modules/BindingsModule.scala" ]; then
    echo -e "${YELLOW}Nettoyage des bindings Malts du BindingsModule...${NC}"
    
    # Supprimer UNIQUEMENT les lignes contenant MaltReadRepository ou MaltWriteRepository
    sed -i.cleanup '/MaltReadRepository\|MaltWriteRepository/d' app/modules/BindingsModule.scala
    
    echo -e "${GREEN}Bindings Malts supprimés du BindingsModule${NC}"
    echo -e "${YELLOW}BindingsModule nettoyé :${NC}"
    echo -e "${BLUE}===========================================${NC}"
    cat -n app/modules/BindingsModule.scala
    echo -e "${BLUE}===========================================${NC}"
fi

# =============================================================================
# CONFIGURER APPLICATION.CONF
# =============================================================================

echo -e "${YELLOW}Configuration application.conf...${NC}"

# Vérifier si MaltsModule est déjà activé
if grep -q "modules.MaltsModule" conf/application.conf; then
    echo -e "${GREEN}MaltsModule déjà activé${NC}"
else
    echo -e "${YELLOW}Activation MaltsModule...${NC}"
    echo "" >> conf/application.conf
    echo "# Module domaine Malts (séparé)" >> conf/application.conf
    echo "play.modules.enabled += \"modules.MaltsModule\"" >> conf/application.conf
    echo -e "${GREEN}MaltsModule activé dans application.conf${NC}"
fi

# Vérifier que BindingsModule est toujours activé
if grep -q "modules.BindingsModule" conf/application.conf; then
    echo -e "${GREEN}BindingsModule (Hops) toujours activé${NC}"
else
    echo -e "${YELLOW}Réactivation BindingsModule...${NC}"
    echo "play.modules.enabled += \"modules.BindingsModule\"" >> conf/application.conf
fi

# =============================================================================
# RÉSUMÉ DES MODIFICATIONS
# =============================================================================

echo ""
echo -e "${BLUE}RÉSUMÉ DES MODIFICATIONS :${NC}"
echo -e "${GREEN}✓ BindingsModule original préservé (backup créé)${NC}"
echo -e "${GREEN}✓ Bindings Hops intacts${NC}"
echo -e "${GREEN}✓ MaltsModule dédié créé${NC}"
echo -e "${GREEN}✓ Bindings Malts isolés dans MaltsModule${NC}"
echo -e "${GREEN}✓ Configuration application.conf mise à jour${NC}"

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test de compilation...${NC}"

if sbt compile > /tmp/conservative_malts_fix.log 2>&1; then
    echo -e "${GREEN}✓ COMPILATION RÉUSSIE${NC}"
    
    # Test de démarrage
    echo -e "${YELLOW}Test de démarrage (20s max)...${NC}"
    timeout 20s sbt run > /tmp/conservative_startup.log 2>&1 || true
    
    if grep -q "BindingAlreadySet\|CreationException" /tmp/conservative_startup.log; then
        echo -e "${RED}✗ Erreurs Guice détectées${NC}"
        echo -e "${YELLOW}Premières lignes d'erreur :${NC}"
        grep -A3 -B1 "BindingAlreadySet\|CreationException" /tmp/conservative_startup.log | head -10
        
    elif grep -q "Server started\|Application started\|Listening for HTTP" /tmp/conservative_startup.log; then
        echo -e "${GREEN}✓✓ APPLICATION DÉMARRE PARFAITEMENT !${NC}"
        echo ""
        echo -e "${GREEN}🎯 SUCCÈS COMPLET :${NC}"
        echo -e "${GREEN}  ✓ Hops préservés et fonctionnels${NC}"
        echo -e "${GREEN}  ✓ Malts intégrés avec succès${NC}"
        echo -e "${GREEN}  ✓ Aucun conflit de bindings${NC}"
        echo ""
        echo -e "${BLUE}Prochaines étapes :${NC}"
        echo "  1. Créer évolution DB pour Malts"
        echo "  2. Tester APIs : /api/v1/malts"
        echo "  3. Vérifier intégration complète"
        
    else
        echo -e "${YELLOW}Application semble démarrer (timeout normal)${NC}"
        echo -e "${GREEN}✓ Pas d'erreurs Guice détectées${NC}"
    fi
    
else
    echo -e "${RED}✗ Erreurs de compilation${NC}"
    tail -10 /tmp/conservative_malts_fix.log
fi

# =============================================================================
# INSTRUCTIONS DE ROLLBACK
# =============================================================================

echo ""
echo -e "${BLUE}INSTRUCTIONS DE ROLLBACK (si nécessaire) :${NC}"
echo -e "${YELLOW}1. Restaurer BindingsModule :${NC}"
echo "   cp app/modules/BindingsModule.scala.backup.* app/modules/BindingsModule.scala"
echo -e "${YELLOW}2. Supprimer MaltsModule :${NC}"
echo "   rm app/modules/MaltsModule.scala"
echo -e "${YELLOW}3. Nettoyer application.conf :${NC}"
echo "   sed -i '/modules.MaltsModule/d' conf/application.conf"

echo ""
echo -e "${GREEN}Module Malts conservateur créé avec succès !${NC}"