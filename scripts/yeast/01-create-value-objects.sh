#!/bin/bash
# =============================================================================
# SCRIPT : Création Value Objects domaine Yeast
# OBJECTIF : Créer tous les Value Objects suivant le pattern DDD
# USAGE : ./scripts/yeast/01-create-value-objects.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🧬 CRÉATION VALUE OBJECTS YEAST${NC}"
echo -e "${BLUE}==============================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: YEAST ID
# =============================================================================

echo -e "${YELLOW}🆔 Création YeastId...${NC}"

cat > app/domain/yeasts/model/YeastId.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject
import java.util.UUID

/**
 * Value Object pour l'identifiant unique des levures
 * Garantit l'unicité et la validité des identifiants yeast
 */
case class YeastId(value: UUID) extends ValueObject {
  require(value != null, "YeastId ne peut pas être null")
  
  def asString: String = value.toString
  
  override def toString: String = value.toString
}

object YeastId {
  /**
   * Génère un nouvel identifiant unique
   */
  def generate(): YeastId = YeastId(UUID.randomUUID())
  
  /**
   * Crée un YeastId à partir d'une chaîne UUID
   */
  def fromString(id: String): Either[String, YeastId] = {
    try {
      Right(YeastId(UUID.fromString(id)))
    } catch {
      case _: IllegalArgumentException => Left(s"Format UUID invalide: $id")
    }
  }
  
  /**
   * Crée un YeastId à partir d'un UUID existant
   */
  def apply(uuid: UUID): YeastId = new YeastId(uuid)
}
EOF

# =============================================================================
# ÉTAPE 2: YEAST NAME
# =============================================================================

echo -e "${YELLOW}📝 Création YeastName...${NC}"

cat > app/domain/yeasts/model/YeastName.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour le nom des levures
 * Validation et normalisation des noms de levures
 */
case class YeastName(value: String) extends ValueObject {
  require(value != null && value.trim.nonEmpty, "Le nom de la levure ne peut pas être vide")
  require(value.trim.length >= 2, "Le nom de la levure doit contenir au moins 2 caractères")
  require(value.trim.length <= 100, "Le nom de la levure ne peut pas dépasser 100 caractères")
  
  // Normalisation : trim et suppression des espaces multiples
  val normalized: String = value.trim.replaceAll("\\s+", " ")
  
  override def toString: String = normalized
}

object YeastName {
  /**
   * Crée un YeastName avec validation
   */
  def fromString(name: String): Either[String, YeastName] = {
    try {
      Right(YeastName(name))
    } catch {
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Validation sans création d'objet
   */
  def isValid(name: String): Boolean = {
    name != null &&
    name.trim.nonEmpty &&
    name.trim.length >= 2 &&
    name.trim.length <= 100
  }
}
EOF

# =============================================================================
# ÉTAPE 3: YEAST LABORATORY
# =============================================================================

echo -e "${YELLOW}🔬 Création YeastLaboratory...${NC}"

cat > app/domain/yeasts/model/YeastLaboratory.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les laboratoires de levures
 * Énumération des principaux laboratoires de levures brassicoles
 */
sealed trait YeastLaboratory extends ValueObject {
  def name: String
  def code: String
  def description: String
}

object YeastLaboratory {
  
  case object WhiteLabs extends YeastLaboratory {
    val name = "White Labs"
    val code = "WLP"
    val description = "Laboratoire californien spécialisé en levures liquides"
  }
  
  case object Wyeast extends YeastLaboratory {
    val name = "Wyeast Laboratories"
    val code = "WY"
    val description = "Laboratoire de l'Oregon, pionnier des levures liquides"
  }
  
  case object Lallemand extends YeastLaboratory {
    val name = "Lallemand Brewing"
    val code = "LB"
    val description = "Groupe français, levures sèches haute qualité"
  }
  
  case object Fermentis extends YeastLaboratory {
    val name = "Fermentis"
    val code = "F"
    val description = "Division levures sèches de Lesaffre"
  }
  
  case object ImperialYeast extends YeastLaboratory {
    val name = "Imperial Yeast"
    val code = "I"
    val description = "Laboratoire américain innovant"
  }
  
  case object Mangrove extends YeastLaboratory {
    val name = "Mangrove Jack's"
    val code = "MJ"
    val description = "Laboratoire néo-zélandais"
  }
  
  case object Other extends YeastLaboratory {
    val name = "Other"
    val code = "OTH"
    val description = "Autres laboratoires"
  }
  
  val values: List[YeastLaboratory] = List(
    WhiteLabs, Wyeast, Lallemand, Fermentis, 
    ImperialYeast, Mangrove, Other
  )
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[YeastLaboratory] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Recherche par code
   */
  def fromCode(code: String): Option[YeastLaboratory] = {
    values.find(_.code.equalsIgnoreCase(code.trim))
  }
  
  /**
   * Parsing flexible
   */
  def parse(input: String): Either[String, YeastLaboratory] = {
    val trimmed = input.trim
    fromName(trimmed)
      .orElse(fromCode(trimmed))
      .toRight(s"Laboratoire inconnu: $input")
  }
}
EOF

# =============================================================================
# ÉTAPE 4: YEAST STRAIN
# =============================================================================

echo -e "${YELLOW}🧪 Création YeastStrain...${NC}"

cat > app/domain/yeasts/model/YeastStrain.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les souches de levures
 * Référence unique de la souche (WLP001, S-04, etc.)
 */
case class YeastStrain(value: String) extends ValueObject {
  require(value != null && value.trim.nonEmpty, "La souche ne peut pas être vide")
  require(value.trim.length >= 1, "La souche doit contenir au moins 1 caractère")
  require(value.trim.length <= 20, "La souche ne peut pas dépasser 20 caractères")
  require(isValidFormat(value.trim), "Format de souche invalide")
  
  val normalized: String = value.trim.toUpperCase
  
  override def toString: String = normalized
  
  private def isValidFormat(strain: String): Boolean = {
    // Formats acceptés : WLP001, S-04, 1056, BE-134, etc.
    val pattern = "^[A-Z0-9-]{1,20}$".r
    pattern.matches(strain.toUpperCase)
  }
}

object YeastStrain {
  /**
   * Crée un YeastStrain avec validation
   */
  def fromString(strain: String): Either[String, YeastStrain] = {
    try {
      Right(YeastStrain(strain))
    } catch {
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Souches populaires pour suggestions
   */
  val popularStrains: List[String] = List(
    // White Labs
    "WLP001", "WLP002", "WLP004", "WLP005", "WLP007", "WLP029", "WLP099",
    // Wyeast
    "1056", "1084", "1098", "1272", "1318", "1469", "2565", "3068", "3724",
    // Fermentis
    "S-04", "S-05", "S-23", "W-34/70", "T-58", "BE-134", "BE-256",
    // Lallemand
    "VERDANT", "NOUVEAU", "VOSS", "CBC-1", "WILD-1"
  )
  
  /**
   * Validation sans création d'objet
   */
  def isValid(strain: String): Boolean = {
    try {
      YeastStrain(strain)
      true
    } catch {
      case _: IllegalArgumentException => false
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 5: YEAST TYPE
# =============================================================================

echo -e "${YELLOW}🍺 Création YeastType...${NC}"

cat > app/domain/yeasts/model/YeastType.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les types de levures
 * Classification par famille de fermentation
 */
sealed trait YeastType extends ValueObject {
  def name: String
  def description: String
  def temperatureRange: (Int, Int) // (min, max) en Celsius
  def characteristics: String
}

object YeastType {
  
  case object Ale extends YeastType {
    val name = "Ale"
    val description = "Saccharomyces cerevisiae - Fermentation haute"
    val temperatureRange = (15, 24)
    val characteristics = "Fermentation rapide, arômes fruités et esters"
  }
  
  case object Lager extends YeastType {
    val name = "Lager"
    val description = "Saccharomyces pastorianus - Fermentation basse"
    val temperatureRange = (7, 15)
    val characteristics = "Fermentation lente, profil propre et net"
  }
  
  case object Wheat extends YeastType {
    val name = "Wheat"
    val description = "Levures spécialisées pour bières de blé"
    val temperatureRange = (18, 24)
    val characteristics = "Arômes banana et clou de girofle"
  }
  
  case object Saison extends YeastType {
    val name = "Saison"
    val description = "Levures farmhouse traditionnelles"
    val temperatureRange = (20, 35)
    val characteristics = "Tolérance chaleur, arômes poivre et épices"
  }
  
  case object Wild extends YeastType {
    val name = "Wild"
    val description = "Brettanomyces et levures sauvages"
    val temperatureRange = (15, 25)
    val characteristics = "Fermentation lente, arômes funky et complexes"
  }
  
  case object Sour extends YeastType {
    val name = "Sour"
    val description = "Lactobacillus et bactéries lactiques"
    val temperatureRange = (25, 40)
    val characteristics = "Production d'acide lactique, acidité"
  }
  
  case object Champagne extends YeastType {
    val name = "Champagne"
    val description = "Levures haute tolérance alcool"
    val temperatureRange = (12, 20)
    val characteristics = "Atténuation élevée, tolérance alcool 15%+"
  }
  
  case object Kveik extends YeastType {
    val name = "Kveik"
    val description = "Levures norvégiennes traditionnelles"
    val temperatureRange = (25, 40)
    val characteristics = "Fermentation ultra-rapide, haute température"
  }
  
  val values: List[YeastType] = List(
    Ale, Lager, Wheat, Saison, Wild, Sour, Champagne, Kveik
  )
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[YeastType] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, YeastType] = {
    fromName(input.trim).toRight(s"Type de levure inconnu: $input")
  }
  
  /**
   * Types recommandés par style de bière
   */
  def recommendedForBeerStyle(style: String): List[YeastType] = {
    style.toLowerCase match {
      case s if s.contains("ipa") || s.contains("pale ale") => List(Ale)
      case s if s.contains("lager") || s.contains("pilsner") => List(Lager)
      case s if s.contains("wheat") || s.contains("weizen") => List(Wheat)
      case s if s.contains("saison") => List(Saison)
      case s if s.contains("sour") || s.contains("gose") => List(Sour, Wild)
      case s if s.contains("imperial") => List(Ale, Champagne)
      case _ => List(Ale, Lager)
    }
  }
}
EOF

echo -e "${GREEN}✅ Value Objects de base créés${NC}"

# =============================================================================
# COMPILATION ET VÉRIFICATION
# =============================================================================

echo -e "\n${YELLOW}🔨 Test de compilation Value Objects...${NC}"

if sbt "compile" > /tmp/yeast_value_objects_compile.log 2>&1; then
    echo -e "${GREEN}✅ Value Objects compilent correctement${NC}"
else
    echo -e "${RED}❌ Erreurs de compilation détectées${NC}"
    echo -e "${YELLOW}Voir les logs : /tmp/yeast_value_objects_compile.log${NC}"
    tail -10 /tmp/yeast_value_objects_compile.log
fi

echo -e "\n${BLUE}📋 RÉSUMÉ VALUE OBJECTS CRÉÉS${NC}"
echo -e "${GREEN}✅ YeastId - Identifiant unique${NC}"
echo -e "${GREEN}✅ YeastName - Nom avec validation${NC}"
echo -e "${GREEN}✅ YeastLaboratory - Laboratoires principaux${NC}"
echo -e "${GREEN}✅ YeastStrain - Références souches${NC}"
echo -e "${GREEN}✅ YeastType - Types de levures${NC}"
echo ""
echo -e "${YELLOW}📋 PROCHAINE ÉTAPE :${NC}"
echo -e "${YELLOW}   ./scripts/yeast/01b-create-advanced-value-objects.sh${NC}"
echo ""
echo -e "${GREEN}🎉 VALUE OBJECTS DE BASE TERMINÉS AVEC SUCCÈS !${NC}"