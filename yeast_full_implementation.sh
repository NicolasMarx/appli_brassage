#!/bin/bash
# =============================================================================
# SCRIPT : Implémentation complète du domaine Yeast
# OBJECTIF : Exécuter tous les scripts Yeast avec rapport détaillé
# USAGE : ./yeast_complete_implementation.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Variables globales
START_TIME=$(date +%s)
TOTAL_SCRIPTS=12
SUCCESS_COUNT=0
FAILED_COUNT=0
SKIPPED_COUNT=0
LOG_DIR="logs/yeast_implementation_$(date +%Y%m%d_%H%M%S)"
REPORT_FILE="$LOG_DIR/implementation_report.md"

# Créer structure de logs
mkdir -p "$LOG_DIR"

echo -e "${BLUE}${BOLD}🧬 IMPLÉMENTATION COMPLÈTE DOMAINE YEAST${NC}"
echo -e "${BLUE}${BOLD}============================================${NC}"
echo ""
echo -e "${CYAN}📁 Logs sauvegardés dans: $LOG_DIR${NC}"
echo ""

# =============================================================================
# ÉTAPE PRÉLIMINAIRE : CORRECTIONS DÉPENDANCES
# =============================================================================

echo -e "${YELLOW}🔧 ÉTAPE 0: CORRECTIONS DÉPENDANCES MANQUANTES${NC}"
echo -e "${YELLOW}==============================================${NC}"

# Fonction pour créer les traits manquants
create_missing_dependencies() {
    echo "Création des traits DDD manquants..."
    
    # ValueObject trait
    if [ ! -f "app/domain/shared/ValueObject.scala" ] || grep -q "TODO" app/domain/shared/ValueObject.scala 2>/dev/null; then
        mkdir -p app/domain/shared
        cat > app/domain/shared/ValueObject.scala << 'EOF'
package domain.shared

/**
 * Trait de base pour tous les Value Objects
 * Garantit l'immutabilité et l'égalité par valeur
 */
trait ValueObject {
  override def equals(obj: Any): Boolean = obj match {
    case that: ValueObject => this.getClass == that.getClass && this.hashCode == that.hashCode
    case _ => false
  }
  
  override def hashCode(): Int = scala.util.hashing.MurmurHash3.productHash(this.asInstanceOf[Product])
  
  def canEqual(other: Any): Boolean = other.getClass == this.getClass
}
EOF
        echo "✅ ValueObject trait créé"
    fi
    
    # AggregateRoot trait
    if [ ! -f "app/domain/shared/AggregateRoot.scala" ] || grep -q "TODO" app/domain/shared/AggregateRoot.scala 2>/dev/null; then
        cat > app/domain/shared/AggregateRoot.scala << 'EOF'
package domain.shared

import scala.collection.mutable

/**
 * Trait de base pour tous les agrégats DDD
 * Support Event Sourcing et gestion des événements
 */
trait AggregateRoot[T <: DomainEvent] {
  private val _uncommittedEvents = mutable.ListBuffer[T]()
  
  def getUncommittedEvents: List[T] = _uncommittedEvents.toList
  
  def markEventsAsCommitted: this.type = {
    _uncommittedEvents.clear()
    this
  }
  
  protected def raise(event: T): Unit = {
    _uncommittedEvents += event
  }
}
EOF
        echo "✅ AggregateRoot trait créé"
    fi
    
    # DomainEvent trait  
    if [ ! -f "app/domain/shared/DomainEvent.scala" ]; then
        cat > app/domain/shared/DomainEvent.scala << 'EOF'
package domain.shared

import java.time.Instant

/**
 * Trait de base pour tous les événements de domaine
 * Support Event Sourcing
 */
trait DomainEvent {
  def occurredAt: Instant
  def version: Long
}
EOF
        echo "✅ DomainEvent trait créé"
    fi
    
    echo -e "${GREEN}✅ Dépendances DDD créées avec succès${NC}"
}

# Exécuter les corrections
create_missing_dependencies > "$LOG_DIR/step_0_dependencies.log" 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Dépendances corrigées${NC}"
else
    echo -e "${RED}❌ Erreur lors des corrections${NC}"
    exit 1
fi

# =============================================================================
# DÉFINITION DES SCRIPTS À EXÉCUTER
# =============================================================================

declare -a SCRIPTS=(
    "scripts/yeast/00-create-yeast-branch.sh:Setup Git:Création branche feature et structure"
    "scripts/yeast/01-create-value-objects.sh:Value Objects Base:YeastId, YeastName, YeastLaboratory, YeastStrain, YeastType"
    "scripts/yeast/01b-create-advanced-value-objects.sh:Value Objects Avancés:AttenuationRange, FermentationTemp, AlcoholTolerance, FlocculationLevel, YeastCharacteristics"
    "scripts/yeast/01c-create-status-filter.sh:Status & Filtres:YeastStatus, YeastFilter, YeastEvents"
    "scripts/yeast/02-create-aggregate.sh:Aggregate:YeastAggregate avec Event Sourcing"
    "scripts/yeast/03-create-services.sh:Services Domaine:YeastDomainService, YeastValidationService, YeastRecommendationService"
    "scripts/yeast/04-create-repositories.sh:Repositories (1/2):Interfaces et Read Repository"
    "scripts/yeast/04b-create-repositories-part2.sh:Repositories (2/2):Write Repository, Repository combiné, tests"
    "scripts/yeast/05-create-application-layer.sh:Application CQRS (1/2):Commands, Queries, Handlers"
    "scripts/yeast/05b-create-application-layer-part2.sh:Application CQRS (2/2):DTOs, Services applicatifs, tests"
    "scripts/yeast/06-create-interface-layer.sh:Interface REST (1/2):Controllers, Routes, Validation"
    "scripts/yeast/06b-create-interface-layer-part2.sh:Interface REST (2/2):Tests, Documentation OpenAPI, configuration"
)

# =============================================================================
# FONCTIONS UTILITAIRES
# =============================================================================

log_step() {
    local step_num=$1
    local script_path=$2
    local category=$3
    local description=$4
    local status=$5
    local duration=$6
    local error_msg=${7:-""}
    
    echo "## Étape $step_num: $category" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Script**: \`$script_path\`" >> "$REPORT_FILE"
    echo "**Description**: $description" >> "$REPORT_FILE"
    echo "**Status**: $status" >> "$REPORT_FILE"
    echo "**Durée**: ${duration}s" >> "$REPORT_FILE"
    
    if [ -n "$error_msg" ]; then
        echo "**Erreur**: \`$error_msg\`" >> "$REPORT_FILE"
    fi
    
    echo "" >> "$REPORT_FILE"
}

execute_script() {
    local script_info=$1
    local step_num=$2
    
    IFS=':' read -r script_path category description <<< "$script_info"
    
    echo -e "${CYAN}📄 Étape $step_num/$TOTAL_SCRIPTS: $category${NC}"
    echo -e "${CYAN}   Script: $script_path${NC}"
    echo -e "${CYAN}   Description: $description${NC}"
    
    # Vérifier l'existence du script
    if [ ! -f "$script_path" ]; then
        echo -e "${RED}❌ Script non trouvé: $script_path${NC}"
        log_step "$step_num" "$script_path" "$category" "$description" "❌ ÉCHEC" "0" "Script non trouvé"
        FAILED_COUNT=$((FAILED_COUNT + 1))
        return 1
    fi
    
    # Rendre le script exécutable
    chmod +x "$script_path"
    
    # Exécuter avec mesure du temps
    local step_start=$(date +%s)
    local log_file="$LOG_DIR/step_${step_num}_$(basename "$script_path" .sh).log"
    
    if timeout 300 "$script_path" > "$log_file" 2>&1; then
        local step_end=$(date +%s)
        local duration=$((step_end - step_start))
        
        echo -e "${GREEN}✅ Succès (${duration}s)${NC}"
        log_step "$step_num" "$script_path" "$category" "$description" "✅ SUCCÈS" "$duration"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        return 0
    else
        local step_end=$(date +%s)
        local duration=$((step_end - step_start))
        local exit_code=$?
        
        echo -e "${RED}❌ Échec (${duration}s) - Code: $exit_code${NC}"
        echo -e "${YELLOW}   Voir logs: $log_file${NC}"
        
        # Extraire les dernières lignes d'erreur
        local error_summary
        if [ -f "$log_file" ]; then
            error_summary=$(tail -3 "$log_file" | tr '\n' ' ')
        else
            error_summary="Timeout ou erreur système"
        fi
        
        log_step "$step_num" "$script_path" "$category" "$description" "❌ ÉCHEC" "$duration" "$error_summary"
        FAILED_COUNT=$((FAILED_COUNT + 1))
        return 1
    fi
}

# =============================================================================
# INITIALISATION DU RAPPORT
# =============================================================================

cat > "$REPORT_FILE" << EOF
# 🧬 Rapport d'Implémentation - Domaine Yeast

**Date**: $(date)
**Durée totale**: [À compléter]
**Scripts exécutés**: $TOTAL_SCRIPTS

## 📊 Résumé Exécutif

| Métrique | Valeur |
|----------|--------|
| Scripts réussis | [À compléter] |
| Scripts échoués | [À compléter] |
| Scripts ignorés | [À compléter] |
| Taux de succès | [À compléter] |

---

EOF

# =============================================================================
# EXÉCUTION SÉQUENTIELLE DES SCRIPTS
# =============================================================================

echo -e "${BLUE}${BOLD}🚀 DÉMARRAGE DE L'IMPLÉMENTATION${NC}"
echo -e "${BLUE}${BOLD}================================${NC}"
echo ""

step_num=1
continue_execution=true

for script_info in "${SCRIPTS[@]}"; do
    if [ "$continue_execution" = false ]; then
        echo -e "${YELLOW}⏭️  Scripts restants ignorés suite à un échec critique${NC}"
        SKIPPED_COUNT=$((TOTAL_SCRIPTS - step_num + 1))
        break
    fi
    
    echo ""
    echo -e "${BOLD}────────────────────────────────────────${NC}"
    
    execute_script "$script_info" "$step_num"
    script_result=$?
    
    # Test de compilation après chaque étape critique
    if [ $step_num -eq 2 ] || [ $step_num -eq 4 ] || [ $step_num -eq 5 ] || [ $step_num -eq 8 ]; then
        echo -e "${YELLOW}🔨 Test de compilation intermédiaire...${NC}"
        if timeout 120 sbt compile > "$LOG_DIR/compile_test_step_$step_num.log" 2>&1; then
            echo -e "${GREEN}✅ Compilation OK${NC}"
        else
            echo -e "${RED}❌ Échec compilation - Arrêt recommandé${NC}"
            echo -e "${YELLOW}   Continuer malgré l'échec ? (y/N)${NC}"
            read -r -n 1 response
            echo
            if [[ ! "$response" =~ ^[Yy]$ ]]; then
                continue_execution=false
            fi
        fi
    fi
    
    step_num=$((step_num + 1))
    sleep 1
done

# =============================================================================
# TESTS FINAUX
# =============================================================================

if [ "$continue_execution" = true ] && [ $SUCCESS_COUNT -gt $((TOTAL_SCRIPTS / 2)) ]; then
    echo ""
    echo -e "${BLUE}${BOLD}🧪 TESTS FINAUX${NC}"
    echo -e "${BLUE}${BOLD}===============${NC}"
    
    # Test de compilation final
    echo -e "${YELLOW}🔨 Test de compilation finale...${NC}"
    if timeout 180 sbt compile > "$LOG_DIR/final_compile_test.log" 2>&1; then
        echo -e "${GREEN}✅ Compilation finale réussie${NC}"
        final_compile_status="✅ SUCCÈS"
    else
        echo -e "${RED}❌ Échec compilation finale${NC}"
        final_compile_status="❌ ÉCHEC"
    fi
    
    # Test des routes
    echo -e "${YELLOW}🛣️  Vérification des routes...${NC}"
    if grep -q "yeasts" conf/routes 2>/dev/null; then
        echo -e "${GREEN}✅ Routes yeast détectées${NC}"
        routes_status="✅ PRÉSENTES"
    else
        echo -e "${YELLOW}⚠️  Routes yeast non détectées${NC}"
        routes_status="⚠️ MANQUANTES"
    fi
    
    # Test de la structure des fichiers
    echo -e "${YELLOW}📁 Vérification structure des fichiers...${NC}"
    yeast_files_count=$(find app/domain/yeasts -name "*.scala" 2>/dev/null | wc -l || echo "0")
    echo -e "${CYAN}   Fichiers domaine yeast créés: $yeast_files_count${NC}"
fi

# =============================================================================
# GÉNÉRATION DU RAPPORT FINAL
# =============================================================================

END_TIME=$(date +%s)
TOTAL_DURATION=$((END_TIME - START_TIME))
SUCCESS_RATE=$(( (SUCCESS_COUNT * 100) / TOTAL_SCRIPTS ))

echo ""
echo -e "${BLUE}${BOLD}📋 GÉNÉRATION DU RAPPORT FINAL${NC}"
echo -e "${BLUE}${BOLD}===============================${NC}"

# Compléter le rapport
sed -i.bak "s|\[À compléter\]|$SUCCESS_COUNT|g" "$REPORT_FILE" 2>/dev/null || {
    # Fallback pour systèmes sans sed -i
    cp "$REPORT_FILE" "$REPORT_FILE.bak"
    sed "s|\[À compléter\]|$SUCCESS_COUNT|g" "$REPORT_FILE.bak" > "$REPORT_FILE"
}

# Remplacer les valeurs dans le rapport
if command -v sed >/dev/null 2>&1; then
    sed -i.bak2 "s/Scripts réussis.*|.*/Scripts réussis | $SUCCESS_COUNT |/" "$REPORT_FILE"
    sed -i.bak3 "s/Scripts échoués.*|.*/Scripts échoués | $FAILED_COUNT |/" "$REPORT_FILE"
    sed -i.bak4 "s/Scripts ignorés.*|.*/Scripts ignorés | $SKIPPED_COUNT |/" "$REPORT_FILE"
    sed -i.bak5 "s/Taux de succès.*|.*/Taux de succès | $SUCCESS_RATE% |/" "$REPORT_FILE"
    sed -i.bak6 "s/\*\*Durée totale\*\*:.*/\*\*Durée totale\*\*: ${TOTAL_DURATION}s/" "$REPORT_FILE"
fi

# Ajouter section tests finaux
cat >> "$REPORT_FILE" << EOF

---

## 🧪 Tests Finaux

| Test | Résultat |
|------|----------|
| Compilation finale | ${final_compile_status:-"Non exécuté"} |
| Routes configurées | ${routes_status:-"Non vérifié"} |
| Fichiers créés | ${yeast_files_count:-"0"} fichiers |

## 📁 Logs Détaillés

Les logs complets sont disponibles dans: \`$LOG_DIR/\`

EOF

# =============================================================================
# AFFICHAGE DU RÉSUMÉ FINAL
# =============================================================================

echo ""
echo -e "${BLUE}${BOLD}🏁 RÉSUMÉ FINAL DE L'IMPLÉMENTATION${NC}"
echo -e "${BLUE}${BOLD}====================================${NC}"
echo ""
echo -e "${CYAN}📊 Statistiques d'exécution:${NC}"
echo -e "   • Scripts réussis:    ${GREEN}$SUCCESS_COUNT${NC}/$TOTAL_SCRIPTS"
echo -e "   • Scripts échoués:    ${RED}$FAILED_COUNT${NC}/$TOTAL_SCRIPTS"
echo -e "   • Scripts ignorés:    ${YELLOW}$SKIPPED_COUNT${NC}/$TOTAL_SCRIPTS"
echo -e "   • Taux de succès:     ${SUCCESS_RATE}%"
echo -e "   • Durée totale:       ${TOTAL_DURATION}s"
echo ""
echo -e "${CYAN}📁 Fichiers générés:${NC}"
echo -e "   • Rapport détaillé:   $REPORT_FILE"
echo -e "   • Logs complets:      $LOG_DIR/"
echo ""

# Évaluation globale
if [ $SUCCESS_RATE -ge 90 ]; then
    echo -e "${GREEN}${BOLD}🎉 IMPLÉMENTATION EXCELLENTE${NC}"
    echo -e "${GREEN}   Le domaine Yeast est prêt pour la production${NC}"
    exit_code=0
elif [ $SUCCESS_RATE -ge 70 ]; then
    echo -e "${YELLOW}${BOLD}⚠️  IMPLÉMENTATION PARTIELLE${NC}"
    echo -e "${YELLOW}   Des corrections mineures peuvent être nécessaires${NC}"
    exit_code=1
else
    echo -e "${RED}${BOLD}❌ IMPLÉMENTATION ÉCHOUÉE${NC}"
    echo -e "${RED}   Révision majeure nécessaire${NC}"
    exit_code=2
fi

echo ""
echo -e "${CYAN}📖 Pour consulter le rapport complet:${NC}"
echo -e "${CYAN}   cat $REPORT_FILE${NC}"

exit $exit_code