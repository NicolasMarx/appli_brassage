#!/bin/bash

# =============================================================================
# NETTOYAGE BINDINGSMODULE - SOLUTION DIRECTE
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Nettoyage complet BindingsModule${NC}"

# =============================================================================
# EXAMINER LE FICHIER ACTUEL
# =============================================================================

echo -e "${YELLOW}Examen du BindingsModule actuel...${NC}"

if [ -f "app/modules/BindingsModule.scala" ]; then
    echo -e "${YELLOW}Contenu actuel :${NC}"
    cat -n app/modules/BindingsModule.scala
    echo ""
    
    # Backup
    cp app/modules/BindingsModule.scala app/modules/BindingsModule.scala.backup.$(date +%Y%m%d_%H%M%S)
    echo -e "${GREEN}Backup créé${NC}"
    
    # Compter les occurrences de MaltReadRepository
    malt_read_count=$(grep -c "MaltReadRepository" app/modules/BindingsModule.scala || echo "0")
    malt_write_count=$(grep -c "MaltWriteRepository" app/modules/BindingsModule.scala || echo "0")
    
    echo -e "${YELLOW}Bindings MaltReadRepository trouvés : $malt_read_count${NC}"
    echo -e "${YELLOW}Bindings MaltWriteRepository trouvés : $malt_write_count${NC}"
    
else
    echo -e "${RED}BindingsModule non trouvé${NC}"
    exit 1
fi

# =============================================================================
# CRÉER UN NOUVEAU BINDINGSMODULE PROPRE
# =============================================================================

echo -e "${YELLOW}Création d'un BindingsModule propre...${NC}"

cat > app/modules/BindingsModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    // Bindings domaine Hops (existants)
    bind(classOf[domain.hops.repositories.HopReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.hops.SlickHopReadRepository])
      .asEagerSingleton()
    bind(classOf[domain.hops.repositories.HopWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.hops.SlickHopWriteRepository])
      .asEagerSingleton()

    // Bindings domaine Admin (existants)
    bind(classOf[domain.admin.repositories.AdminReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.admin.SlickAdminReadRepository])
      .asEagerSingleton()
    bind(classOf[domain.admin.repositories.AdminWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.admin.SlickAdminWriteRepository])
      .asEagerSingleton()

    // Bindings domaine Malts (UNIQUES)
    bind(classOf[domain.malts.repositories.MaltReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository])
      .asEagerSingleton()
    bind(classOf[domain.malts.repositories.MaltWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository])
      .asEagerSingleton()
  }
}
EOF

echo -e "${GREEN}BindingsModule propre créé${NC}"

# =============================================================================
# ALTERNATIVE : SUPPRIMER TOUS LES BINDINGS GUICE
# =============================================================================

echo -e "${YELLOW}Création d'une alternative sans bindings Guice...${NC}"

cat > app/modules/BindingsModule.scala.no_guice << 'EOF'
package modules

import com.google.inject.AbstractModule

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    // Aucun binding explicite - utilisation des annotations @Singleton
    // Les repositories avec @Singleton seront automatiquement découverts
  }
}
EOF

echo -e "${GREEN}Alternative sans bindings créée${NC}"

# =============================================================================
# VÉRIFIER LES ANNOTATIONS @SINGLETON
# =============================================================================

echo -e "${YELLOW}Vérification des annotations @Singleton...${NC}"

files_to_check=(
    "app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala"
    "app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala"
)

for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        if grep -q "@Singleton" "$file"; then
            echo -e "${GREEN}✓ @Singleton présent dans $(basename $file)${NC}"
        else
            echo -e "${RED}✗ @Singleton manquant dans $(basename $file)${NC}"
            # Ajouter @Singleton si manquant
            sed -i.bak 's/^class Slick\([A-Za-z]*\)Repository/@Singleton\nclass Slick\1Repository/' "$file"
            echo -e "${GREEN}@Singleton ajouté${NC}"
        fi
    else
        echo -e "${RED}Fichier non trouvé : $file${NC}"
    fi
done

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test 1 : Compilation avec BindingsModule propre${NC}"

if sbt compile > /tmp/clean_bindings_test1.log 2>&1; then
    echo -e "${GREEN}✓ Compilation réussie${NC}"
    
    # Test de démarrage
    echo -e "${YELLOW}Test de démarrage...${NC}"
    timeout 15s sbt run > /tmp/clean_bindings_startup1.log 2>&1 || true
    
    if grep -q "BindingAlreadySet\|CreationException" /tmp/clean_bindings_startup1.log; then
        echo -e "${RED}✗ Erreurs Guice persistantes${NC}"
        
        # Essayer l'alternative sans bindings
        echo -e "${YELLOW}Test alternative sans bindings Guice...${NC}"
        mv app/modules/BindingsModule.scala app/modules/BindingsModule.scala.with_bindings
        mv app/modules/BindingsModule.scala.no_guice app/modules/BindingsModule.scala
        
        timeout 15s sbt run > /tmp/clean_bindings_startup2.log 2>&1 || true
        
        if grep -q "BindingAlreadySet\|CreationException" /tmp/clean_bindings_startup2.log; then
            echo -e "${RED}✗ Erreurs persistantes même sans bindings${NC}"
            echo -e "${YELLOW}Logs disponibles dans /tmp/clean_bindings_startup*.log${NC}"
        else
            echo -e "${GREEN}✓ Solution : BindingsModule sans bindings explicites${NC}"
            echo -e "${GREEN}✓ Les repositories utilisent @Singleton automatiquement${NC}"
        fi
        
    else
        echo -e "${GREEN}✓ Application démarre correctement !${NC}"
        echo -e "${GREEN}✓ Bindings résolus${NC}"
        echo -e "${GREEN}✓ Prêt pour l'évolution de base de données${NC}"
    fi
    
else
    echo -e "${RED}✗ Erreurs de compilation${NC}"
    tail -10 /tmp/clean_bindings_test1.log
fi

# =============================================================================
# INSTRUCTIONS FINALES
# =============================================================================

echo ""
echo -e "${BLUE}Instructions :${NC}"
echo ""
echo "Si l'application démarre correctement :"
echo "  → Créer l'évolution DB : conf/evolutions/default/10.sql"
echo "  → Tester les APIs malts"
echo ""
echo "Si des erreurs persistent :"
echo "  → Vérifier les logs dans /tmp/clean_bindings_*.log"
echo "  → Considérer une approche différente (injection par constructeur)"
echo ""
echo -e "${GREEN}Nettoyage BindingsModule terminé${NC}"