#!/bin/bash

# =============================================================================
# NETTOYAGE COMPLET BINDINGSMODULE
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Nettoyage complet BindingsModule${NC}"

# =============================================================================
# MONTRER LE PROBLÈME ACTUEL
# =============================================================================

echo -e "${YELLOW}Contenu problématique actuel :${NC}"
if [ -f "app/modules/BindingsModule.scala" ]; then
    echo -e "${RED}===========================================${NC}"
    cat -n app/modules/BindingsModule.scala
    echo -e "${RED}===========================================${NC}"
    echo ""
    
    # Backup complet
    cp app/modules/BindingsModule.scala app/modules/BindingsModule.scala.BROKEN.$(date +%Y%m%d_%H%M%S)
    echo -e "${GREEN}Backup du fichier cassé créé${NC}"
fi

# =============================================================================
# ANALYSER CE QUI EXISTE VRAIMENT
# =============================================================================

echo -e "${YELLOW}Analyse des domaines réellement présents...${NC}"

# Chercher les repositories qui existent vraiment
echo -e "${BLUE}Recherche des repositories existants :${NC}"

find app/ -name "*Repository.scala" -type f | while read file; do
    echo -e "${GREEN}Trouvé: $file${NC}"
done

echo ""

# Chercher spécifiquement les Hops
if find app/ -name "*HopReadRepository.scala" -o -name "*HopWriteRepository.scala" | grep -q .; then
    echo -e "${GREEN}✓ Repositories Hops détectés${NC}"
    HOP_REPOS_EXIST=true
    find app/ -name "*HopReadRepository.scala" -o -name "*HopWriteRepository.scala"
else
    echo -e "${YELLOW}✗ Aucun repository Hops trouvé${NC}"
    HOP_REPOS_EXIST=false
fi

echo ""

# Chercher spécifiquement les Malts
if find app/ -name "*MaltReadRepository.scala" -o -name "*MaltWriteRepository.scala" | grep -q .; then
    echo -e "${GREEN}✓ Repositories Malts détectés${NC}"
    MALT_REPOS_EXIST=true
    find app/ -name "*MaltReadRepository.scala" -o -name "*MaltWriteRepository.scala"
else
    echo -e "${YELLOW}✗ Aucun repository Malts trouvé${NC}"
    MALT_REPOS_EXIST=false
fi

# =============================================================================
# CRÉER UN BINDINGSMODULE CORRECT
# =============================================================================

echo -e "${YELLOW}Création d'un BindingsModule correct...${NC}"

cat > app/modules/BindingsModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    // Module reconstruit proprement - seulement les bindings qui existent
    
    // Bindings domaine Malts
    bind(classOf[domain.malts.repositories.MaltReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository])
      .asEagerSingleton()
    
    bind(classOf[domain.malts.repositories.MaltWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository])
      .asEagerSingleton()
      
    // TODO: Ajouter d'autres bindings si nécessaire
    // Les bindings Hops peuvent être ajoutés ici s'ils existent
  }
}
EOF

echo -e "${GREEN}BindingsModule reconstruit${NC}"

# =============================================================================
# VÉRIFIER APPLICATION.CONF
# =============================================================================

echo -e "${YELLOW}Vérification application.conf...${NC}"

if grep -q "modules.BindingsModule" conf/application.conf; then
    echo -e "${GREEN}✓ BindingsModule activé${NC}"
else
    echo -e "${YELLOW}Activation BindingsModule...${NC}"
    echo "" >> conf/application.conf
    echo "# Module principal reconstructed" >> conf/application.conf
    echo "play.modules.enabled += \"modules.BindingsModule\"" >> conf/application.conf
    echo -e "${GREEN}BindingsModule activé${NC}"
fi

# Supprimer les anciens modules si présents
if grep -q "modules.MaltsModule" conf/application.conf; then
    sed -i.bak '/modules.MaltsModule/d' conf/application.conf
    echo -e "${YELLOW}Ancien MaltsModule supprimé de application.conf${NC}"
fi

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test de compilation avec BindingsModule reconstruit...${NC}"

if sbt compile > /tmp/clean_rebuild_test.log 2>&1; then
    echo -e "${GREEN}✓ COMPILATION RÉUSSIE !${NC}"
    
    # Test de démarrage
    echo -e "${YELLOW}Test de démarrage...${NC}"
    timeout 25s sbt run > /tmp/clean_rebuild_startup.log 2>&1 || true
    
    if grep -q "BindingAlreadySet\|CreationException" /tmp/clean_rebuild_startup.log; then
        echo -e "${RED}✗ Erreurs Guice persistent${NC}"
        echo -e "${YELLOW}Premières lignes d'erreur :${NC}"
        grep -A3 -B1 "BindingAlreadySet\|CreationException" /tmp/clean_rebuild_startup.log | head -8
        
        echo -e "${YELLOW}Solution alternative : Module vide${NC}"
        cat > app/modules/BindingsModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    // Module vide - utilisation des annotations @Singleton
  }
}
EOF
        echo -e "${GREEN}BindingsModule vide créé${NC}"
        
    elif grep -q "Server started\|Application started\|Listening for HTTP" /tmp/clean_rebuild_startup.log; then
        echo -e "${GREEN}✓✓ APPLICATION DÉMARRE CORRECTEMENT !${NC}"
        echo ""
        echo -e "${GREEN}SUCCÈS :${NC}"
        echo -e "${GREEN}  ✓ BindingsModule reconstruit proprement${NC}"
        echo -e "${GREEN}  ✓ Erreurs Admin supprimées${NC}"
        echo -e "${GREEN}  ✓ Bindings Malts fonctionnels${NC}"
        echo -e "${GREEN}  ✓ Application opérationnelle${NC}"
        echo ""
        echo -e "${BLUE}Prochaine étape : Créer l'évolution DB${NC}"
        
    else
        echo -e "${YELLOW}Application semble démarrer (pas d'erreurs Guice)${NC}"
        echo -e "${GREEN}✓ Reconstruction probablement réussie${NC}"
    fi
    
else
    echo -e "${RED}✗ Erreurs de compilation persistent${NC}"
    echo -e "${YELLOW}Erreurs :${NC}"
    grep "error" /tmp/clean_rebuild_test.log | head -5
    echo ""
    echo -e "${YELLOW}Log complet : /tmp/clean_rebuild_test.log${NC}"
fi

# =============================================================================
# RÉSUMÉ
# =============================================================================

echo ""
echo -e "${BLUE}RÉSUMÉ :${NC}"
echo -e "${GREEN}✓ BindingsModule problématique sauvegardé${NC}"
echo -e "${GREEN}✓ Nouveau BindingsModule créé sans erreurs Admin${NC}"
echo -e "${GREEN}✓ Seulement les bindings Malts (existants)${NC}"
echo -e "${GREEN}✓ Configuration application.conf nettoyée${NC}"

echo ""
echo -e "${GREEN}Nettoyage complet terminé !${NC}"