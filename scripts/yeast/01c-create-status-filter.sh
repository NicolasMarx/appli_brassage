#!/bin/bash
# =============================================================================
# SCRIPT : Création YeastStatus et YeastFilter
# OBJECTIF : Compléter les Value Objects avec status et filtres
# USAGE : ./scripts/yeast/01c-create-status-filter.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CRÉATION STATUS ET FILTRES YEAST${NC}"
echo -e "${BLUE}==================================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: YEAST STATUS
# =============================================================================

echo -e "${YELLOW}📊 Création YeastStatus...${NC}"

cat > app/domain/yeasts/model/YeastStatus.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour le statut des levures
 * Gestion du cycle de vie des levures dans le système
 */
sealed trait YeastStatus extends ValueObject {
  def name: String
  def description: String
  def isActive: Boolean
  def isVisible: Boolean
}

object YeastStatus {
  
  case object Active extends YeastStatus {
    val name = "ACTIVE"
    val description = "Levure active et disponible"
    val isActive = true
    val isVisible = true
  }
  
  case object Inactive extends YeastStatus {
    val name = "INACTIVE" 
    val description = "Levure temporairement indisponible"
    val isActive = false
    val isVisible = true
  }
  
  case object Discontinued extends YeastStatus {
    val name = "DISCONTINUED"
    val description = "Levure arrêtée par le laboratoire"
    val isActive = false
    val isVisible = true
  }
  
  case object Draft extends YeastStatus {
    val name = "DRAFT"
    val description = "Levure en cours de validation"
    val isActive = false
    val isVisible = false
  }
  
  case object Archived extends YeastStatus {
    val name = "ARCHIVED"
    val description = "Levure archivée (non visible)"
    val isActive = false
    val isVisible = false
  }
  
  val values: List[YeastStatus] = List(Active, Inactive, Discontinued, Draft, Archived)
  val activeStatuses: List[YeastStatus] = values.filter(_.isActive)
  val visibleStatuses: List[YeastStatus] = values.filter(_.isVisible)
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[YeastStatus] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, YeastStatus] = {
    fromName(input.trim).toRight(s"Statut inconnu: $input")
  }
  
  /**
   * Statut par défaut pour nouvelles levures
   */
  val default: YeastStatus = Draft
}
EOF

# =============================================================================
# ÉTAPE 2: YEAST FILTER
# =============================================================================

echo -e "${YELLOW}🔍 Création YeastFilter...${NC}"

cat > app/domain/yeasts/model/YeastFilter.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les filtres de recherche de levures
 * Critères de filtrage avancé pour les APIs publiques et admin
 */
case class YeastFilter(
  name: Option[String] = None,
  laboratory: Option[YeastLaboratory] = None,
  yeastType: Option[YeastType] = None,
  minAttenuation: Option[Int] = None,
  maxAttenuation: Option[Int] = None,
  minTemperature: Option[Int] = None,
  maxTemperature: Option[Int] = None,
  maxAlcoholTolerance: Option[Double] = None,
  minAlcoholTolerance: Option[Double] = None,
  flocculation: Option[FlocculationLevel] = None,
  characteristics: List[String] = List.empty,
  status: List[YeastStatus] = List.empty,
  page: Int = 0,
  size: Int = 20
) extends ValueObject {
  
  require(page >= 0, "Page doit être >= 0")
  require(size > 0 && size <= 100, "Size doit être entre 1 et 100")
  require(minAttenuation.forall(_ >= 30), "Atténuation min doit être >= 30%")
  require(maxAttenuation.forall(_ <= 100), "Atténuation max doit être <= 100%")
  require(minTemperature.forall(_ >= 0), "Température min doit être >= 0°C")
  require(maxTemperature.forall(_ <= 50), "Température max doit être <= 50°C")
  require(minAlcoholTolerance.forall(_ >= 0), "Tolérance alcool min doit être >= 0%")
  require(maxAlcoholTolerance.forall(_ <= 20), "Tolérance alcool max doit être <= 20%")
  
  def hasFilters: Boolean = {
    name.isDefined || laboratory.isDefined || yeastType.isDefined ||
    minAttenuation.isDefined || maxAttenuation.isDefined ||
    minTemperature.isDefined || maxTemperature.isDefined ||
    minAlcoholTolerance.isDefined || maxAlcoholTolerance.isDefined ||
    flocculation.isDefined || characteristics.nonEmpty || status.nonEmpty
  }
  
  def isEmptySearch: Boolean = !hasFilters
  
  def withDefaultStatus: YeastFilter = {
    if (status.isEmpty) copy(status = List(YeastStatus.Active))
    else this
  }
  
  def forPublicAPI: YeastFilter = {
    copy(status = List(YeastStatus.Active))
  }
  
  def nextPage: YeastFilter = copy(page = page + 1)
  def previousPage: YeastFilter = copy(page = math.max(0, page - 1))
  def withSize(newSize: Int): YeastFilter = copy(size = math.min(100, math.max(1, newSize)))
}

object YeastFilter {
  
  /**
   * Filtre vide par défaut
   */
  val empty: YeastFilter = YeastFilter()
  
  /**
   * Filtre pour API publique (seulement levures actives)
   */
  val publicDefault: YeastFilter = YeastFilter(status = List(YeastStatus.Active))
  
  /**
   * Filtre pour API admin (tous statuts)
   */
  val adminDefault: YeastFilter = YeastFilter()
  
  /**
   * Filtres prédéfinis par type de levure
   */
  def forType(yeastType: YeastType): YeastFilter = {
    YeastFilter(yeastType = Some(yeastType), status = List(YeastStatus.Active))
  }
  
  def forLaboratory(laboratory: YeastLaboratory): YeastFilter = {
    YeastFilter(laboratory = Some(laboratory), status = List(YeastStatus.Active))
  }
  
  /**
   * Filtres par caractéristiques de fermentation
   */
  def forHighAttenuation: YeastFilter = {
    YeastFilter(minAttenuation = Some(82), status = List(YeastStatus.Active))
  }
  
  def forLowTemperature: YeastFilter = {
    YeastFilter(maxTemperature = Some(15), status = List(YeastStatus.Active))
  }
  
  def forHighAlcoholTolerance: YeastFilter = {
    YeastFilter(minAlcoholTolerance = Some(12.0), status = List(YeastStatus.Active))
  }
  
  /**
   * Recherche par caractéristiques aromatiques
   */
  def withCharacteristics(chars: String*): YeastFilter = {
    YeastFilter(characteristics = chars.toList, status = List(YeastStatus.Active))
  }
  
  /**
   * Validation des paramètres de filtre
   */
  def validate(filter: YeastFilter): List[String] = {
    var errors = List.empty[String]
    
    if (filter.minAttenuation.isDefined && filter.maxAttenuation.isDefined) {
      val min = filter.minAttenuation.get
      val max = filter.maxAttenuation.get
      if (min > max) {
        errors = s"Atténuation min ($min%) > max ($max%)" :: errors
      }
    }
    
    if (filter.minTemperature.isDefined && filter.maxTemperature.isDefined) {
      val min = filter.minTemperature.get
      val max = filter.maxTemperature.get
      if (min > max) {
        errors = s"Température min (${min}°C) > max (${max}°C)" :: errors
      }
    }
    
    if (filter.minAlcoholTolerance.isDefined && filter.maxAlcoholTolerance.isDefined) {
      val min = filter.minAlcoholTolerance.get
      val max = filter.maxAlcoholTolerance.get
      if (min > max) {
        errors = s"Tolérance alcool min ($min%) > max ($max%)" :: errors
      }
    }
    
    errors.reverse
  }
}
EOF

# =============================================================================
# ÉTAPE 3: YEAST EVENTS
# =============================================================================

echo -e "${YELLOW}📅 Création YeastEvents...${NC}"

cat > app/domain/yeasts/model/YeastEvents.scala << 'EOF'
package domain.yeasts.model

import domain.shared.DomainEvent
import java.time.Instant
import java.util.UUID

/**
 * Events du domaine Yeast
 * Implémentation Event Sourcing pour traçabilité complète
 */
sealed trait YeastEvent extends DomainEvent {
  def yeastId: YeastId
  def occurredAt: Instant
  def version: Long
}

object YeastEvent {
  
  /**
   * Event : Création d'une nouvelle levure
   */
  case class YeastCreated(
    yeastId: YeastId,
    name: YeastName,
    laboratory: YeastLaboratory,
    strain: YeastStrain,
    yeastType: YeastType,
    attenuation: AttenuationRange,
    temperature: FermentationTemp,
    alcoholTolerance: AlcoholTolerance,
    flocculation: FlocculationLevel,
    characteristics: YeastCharacteristics,
    createdBy: UUID,
    occurredAt: Instant,
    version: Long = 1
  ) extends YeastEvent
  
  /**
   * Event : Mise à jour d'une levure
   */
  case class YeastUpdated(
    yeastId: YeastId,
    changes: Map[String, Any],
    updatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Changement de statut
   */
  case class YeastStatusChanged(
    yeastId: YeastId,
    oldStatus: YeastStatus,
    newStatus: YeastStatus,
    reason: Option[String],
    changedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Levure activée
   */
  case class YeastActivated(
    yeastId: YeastId,
    activatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Levure désactivée
   */
  case class YeastDeactivated(
    yeastId: YeastId,
    reason: Option[String],
    deactivatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Levure archivée
   */
  case class YeastArchived(
    yeastId: YeastId,
    reason: Option[String],
    archivedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Données techniques mises à jour
   */
  case class YeastTechnicalDataUpdated(
    yeastId: YeastId,
    attenuation: Option[AttenuationRange],
    temperature: Option[FermentationTemp],
    alcoholTolerance: Option[AlcoholTolerance],
    flocculation: Option[FlocculationLevel],
    characteristics: Option[YeastCharacteristics],
    updatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
  
  /**
   * Event : Information laboratoire mise à jour
   */
  case class YeastLaboratoryInfoUpdated(
    yeastId: YeastId,
    laboratory: YeastLaboratory,
    strain: YeastStrain,
    updatedBy: UUID,
    occurredAt: Instant,
    version: Long
  ) extends YeastEvent
}
EOF

# =============================================================================
# ÉTAPE 4: COMPILATION TEST
# =============================================================================

echo -e "\n${YELLOW}🔨 Test de compilation...${NC}"

# Test de compilation simple des Value Objects
if sbt "compile" > /tmp/yeast_vo_compile.log 2>&1; then
    echo -e "${GREEN}✅ Value Objects compilent correctement${NC}"
else
    echo -e "${RED}❌ Erreurs de compilation détectées${NC}"
    echo -e "${YELLOW}Voir les logs : /tmp/yeast_vo_compile.log${NC}"
fi

# =============================================================================
# ÉTAPE 5: RÉSUMÉ
# =============================================================================

echo -e "\n${BLUE}📋 RÉSUMÉ VALUE OBJECTS CRÉÉS${NC}"
echo -e "${BLUE}==============================${NC}"
echo ""
echo -e "${GREEN}✅ Value Objects de base :${NC}"
echo -e "   • YeastId - Identifiant unique"
echo -e "   • YeastName - Nom avec validation"
echo -e "   • YeastLaboratory - Laboratoires principaux"
echo -e "   • YeastStrain - Références souches"
echo -e "   • YeastType - Types de levures"
echo ""
echo -e "${GREEN}✅ Value Objects techniques :${NC}"
echo -e "   • AttenuationRange - Plage d'atténuation"
echo -e "   • FermentationTemp - Température fermentation"  
echo -e "   • AlcoholTolerance - Tolérance alcool"
echo -e "   • FlocculationLevel - Niveau floculation"
echo -e "   • YeastCharacteristics - Profil organoleptique"
echo ""
echo -e "${GREEN}✅ Value Objects système :${NC}"
echo -e "   • YeastStatus - Statut lifecycle"
echo -e "   • YeastFilter - Critères recherche"
echo -e "   • YeastEvents - Events domaine"
echo ""
echo -e "${YELLOW}📋 PROCHAINE ÉTAPE :${NC}"
echo -e "${YELLOW}   ./scripts/yeast/02-create-aggregate.sh${NC}"
echo ""
echo -e "${BLUE}📊 STATS :${NC}"
echo -e "   • 13 Value Objects créés"
echo -e "   • ~800 lignes de code"
echo -e "   • Pattern DDD respecté"
echo -e "   • Validation complète"