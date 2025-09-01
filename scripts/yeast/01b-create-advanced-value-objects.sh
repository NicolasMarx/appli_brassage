EOF

echo -e "${GREEN}✅ Value Objects avancés créés${NC}"

# =============================================================================
# COMPILATION ET VÉRIFICATION
# =============================================================================

echo -e "\n${YELLOW}🔨 Test de compilation Value Objects avancés...${NC}"

if sbt "compile" > /tmp/yeast_advanced_vo_compile.log 2>&1; then
    echo -e "${GREEN}✅ Value Objects avancés compilent correctement${NC}"
else
    echo -e "${RED}❌ Erreurs de compilation détectées${NC}"
    echo -e "${YELLOW}Voir les logs : /tmp/yeast_advanced_vo_compile.log${NC}"
    tail -10 /tmp/yeast_advanced_vo_compile.log
fi

echo -e "\n${BLUE}📋 RÉSUMÉ VALUE OBJECTS AVANCÉS${NC}"
echo -e "${GREEN}✅ AttenuationRange - Plage d'atténuation${NC}"
echo -e "${GREEN}✅ FermentationTemp - Température fermentation${NC}"
echo -e "${GREEN}✅ AlcoholTolerance - Tolérance alcool${NC}"
echo -e "${GREEN}✅ FlocculationLevel - Niveau floculation${NC}"
echo -e "${GREEN}✅ YeastCharacteristics - Profil organoleptique${NC}"
echo ""
echo -e "${YELLOW}📋 PROCHAINE ÉTAPE :${NC}"
echo -e "${YELLOW}   ./scripts/yeast/01c-create-status-filter.sh${NC}"
echo ""
echo -e "${GREEN}🎉 VALUE OBJECTS AVANCÉS TERMINÉS AVEC SUCCÈS !${NC}"#!/bin/bash
# =============================================================================
# SCRIPT : Création Value Objects avancés domaine Yeast
# OBJECTIF : Créer les Value Objects complexes (températures, atténuation, etc.)
# USAGE : ./scripts/yeast/01b-create-advanced-value-objects.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🌡️ CRÉATION VALUE OBJECTS AVANCÉS YEAST${NC}"
echo -e "${BLUE}=====================================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: ATTENUATION RANGE
# =============================================================================

echo -e "${YELLOW}📊 Création AttenuationRange...${NC}"

cat > app/domain/yeasts/model/AttenuationRange.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour la plage d'atténuation des levures
 * Représente le pourcentage d'atténuation (ex: 75-82%)
 */
case class AttenuationRange(min: Int, max: Int) extends ValueObject {
  require(min >= 30 && min <= 100, s"Atténuation minimale invalide: $min% (doit être entre 30% et 100%)")
  require(max >= 30 && max <= 100, s"Atténuation maximale invalide: $max% (doit être entre 30% et 100%)")
  require(min <= max, s"Atténuation min ($min%) doit être <= max ($max%)")
  require((max - min) <= 30, s"Écart d'atténuation trop large: ${max - min}% (max 30%)")
  
  def average: Double = (min + max) / 2.0
  def range: Int = max - min
  def isHigh: Boolean = min >= 80
  def isMedium: Boolean = min >= 70 && max <= 85
  def isLow: Boolean = max <= 75
  
  def contains(attenuation: Int): Boolean = attenuation >= min && attenuation <= max
  
  override def toString: String = s"$min-$max%"
}

object AttenuationRange {
  /**
   * Constructeur avec validation
   */
  def apply(min: Int, max: Int): AttenuationRange = new AttenuationRange(min, max)
  
  /**
   * Constructeur à partir d'une valeur unique (±3%)
   */
  def fromSingle(attenuation: Int): AttenuationRange = {
    AttenuationRange(math.max(30, attenuation - 3), math.min(100, attenuation + 3))
  }
  
  /**
   * Parsing depuis string "75-82" ou "78"
   */
  def parse(input: String): Either[String, AttenuationRange] = {
    try {
      val cleaned = input.trim.replaceAll("[%\\s]", "")
      if (cleaned.contains("-")) {
        val parts = cleaned.split("-")
        if (parts.length == 2) {
          val min = parts(0).toInt
          val max = parts(1).toInt
          Right(AttenuationRange(min, max))
        } else {
          Left(s"Format invalide: $input (attendu: '75-82' ou '75')")
        }
      } else {
        val value = cleaned.toInt
        Right(fromSingle(value))
      }
    } catch {
      case _: NumberFormatException => Left(s"Format numérique invalide: $input")
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Plages d'atténuation typiques
   */
  val Low = AttenuationRange(65, 75)      // Faible atténuation
  val Medium = AttenuationRange(75, 82)   // Atténuation moyenne
  val High = AttenuationRange(82, 88)     // Haute atténuation
  val VeryHigh = AttenuationRange(88, 95) // Très haute atténuation
}
EOF

# =============================================================================
# ÉTAPE 2: FERMENTATION TEMPERATURE
# =============================================================================

echo -e "${YELLOW}🌡️ Création FermentationTemp...${NC}"

cat > app/domain/yeasts/model/FermentationTemp.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour la température de fermentation
 * Plage optimale en Celsius
 */
case class FermentationTemp(min: Int, max: Int) extends ValueObject {
  require(min >= 0 && min <= 50, s"Température minimale invalide: ${min}°C (doit être entre 0°C et 50°C)")
  require(max >= 0 && max <= 50, s"Température maximale invalide: ${max}°C (doit être entre 0°C et 50°C)")
  require(min <= max, s"Température min (${min}°C) doit être <= max (${max}°C)")
  require((max - min) <= 25, s"Écart de température trop large: ${max - min}°C (max 25°C)")
  
  def average: Double = (min + max) / 2.0
  def range: Int = max - min
  def isLowTemp: Boolean = max <= 15    // Lager range
  def isMediumTemp: Boolean = min >= 15 && max <= 25  // Ale range
  def isHighTemp: Boolean = min >= 25   // Kveik/Saison range
  
  def contains(temp: Int): Boolean = temp >= min && temp <= max
  def toFahrenheit: (Int, Int) = ((min * 9/5) + 32, (max * 9/5) + 32)
  
  override def toString: String = s"${min}-${max}°C"
}

object FermentationTemp {
  /**
   * Constructeur avec validation
   */
  def apply(min: Int, max: Int): FermentationTemp = new FermentationTemp(min, max)
  
  /**
   * Constructeur à partir d'une température unique (±2°C)
   */
  def fromSingle(temp: Int): FermentationTemp = {
    FermentationTemp(math.max(0, temp - 2), math.min(50, temp + 2))
  }
  
  /**
   * Parsing depuis string "18-22" ou "20"
   */
  def parse(input: String): Either[String, FermentationTemp] = {
    try {
      val cleaned = input.trim.replaceAll("[°CcF\\s]", "").toLowerCase
      if (cleaned.contains("-")) {
        val parts = cleaned.split("-")
        if (parts.length == 2) {
          val min = parts(0).toInt
          val max = parts(1).toInt
          Right(FermentationTemp(min, max))
        } else {
          Left(s"Format invalide: $input (attendu: '18-22' ou '20')")
        }
      } else {
        val value = cleaned.toInt
        Right(fromSingle(value))
      }
    } catch {
      case _: NumberFormatException => Left(s"Format numérique invalide: $input")
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Plages de température typiques
   */
  val LagerRange = FermentationTemp(7, 15)      // Fermentation basse
  val AleRange = FermentationTemp(18, 22)       // Fermentation haute standard
  val WarmAleRange = FermentationTemp(20, 25)   // Fermentation haute chaude
  val SaisonRange = FermentationTemp(25, 35)    // Levures saison
  val KveikRange = FermentationTemp(30, 40)     // Levures kveik
  val SourRange = FermentationTemp(25, 40)      // Bactéries lactiques
}
EOF

# =============================================================================
# ÉTAPE 3: ALCOHOL TOLERANCE
# =============================================================================

echo -e "${YELLOW}🍷 Création AlcoholTolerance...${NC}"

cat > app/domain/yeasts/model/AlcoholTolerance.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour la tolérance à l'alcool des levures
 * Pourcentage maximum d'alcool supporté
 */
case class AlcoholTolerance(percentage: Double) extends ValueObject {
  require(percentage >= 0.0 && percentage <= 20.0, 
    s"Tolérance alcool invalide: $percentage% (doit être entre 0% et 20%)")
  
  // Arrondi à une décimale
  val rounded: Double = math.round(percentage * 10) / 10.0
  
  def isLow: Boolean = percentage < 8.0
  def isMedium: Boolean = percentage >= 8.0 && percentage < 12.0
  def isHigh: Boolean = percentage >= 12.0 && percentage < 16.0
  def isVeryHigh: Boolean = percentage >= 16.0
  
  def canFerment(targetAbv: Double): Boolean = percentage >= targetAbv
  
  override def toString: String = s"$rounded%"
}

object AlcoholTolerance {
  /**
   * Constructeur avec validation
   */
  def apply(percentage: Double): AlcoholTolerance = new AlcoholTolerance(percentage)
  
  /**
   * Constructeur depuis entier
   */
  def fromInt(percentage: Int): AlcoholTolerance = AlcoholTolerance(percentage.toDouble)
  
  /**
   * Parsing depuis string
   */
  def parse(input: String): Either[String, AlcoholTolerance] = {
    try {
      val cleaned = input.trim.replaceAll("[%\\s]", "")
      val value = cleaned.toDouble
      Right(AlcoholTolerance(value))
    } catch {
      case _: NumberFormatException => Left(s"Format numérique invalide: $input")
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Tolérances typiques
   */
  val Low = AlcoholTolerance(6.0)        // Levures délicates
  val Standard = AlcoholTolerance(10.0)  // Levures bière standard
  val High = AlcoholTolerance(14.0)      // Levures haute tolérance
  val Champagne = AlcoholTolerance(18.0) // Levures champagne/vin
}
EOF

# =============================================================================
# ÉTAPE 4: FLOCCULATION LEVEL
# =============================================================================

echo -e "${YELLOW}🎈 Création FlocculationLevel...${NC}"

cat > app/domain/yeasts/model/FlocculationLevel.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour le niveau de floculation des levures
 * Capacité d'agglomération et de sédimentation
 */
sealed trait FlocculationLevel extends ValueObject {
  def name: String
  def description: String
  def clarificationTime: String
  def rackingRecommendation: String
}

object FlocculationLevel {
  
  case object Low extends FlocculationLevel {
    val name = "Low"
    val description = "Faible floculation - levures restent en suspension"
    val clarificationTime = "Longue (2-4 semaines)"
    val rackingRecommendation = "Soutirage tardif, filtration recommandée"
  }
  
  case object Medium extends FlocculationLevel {
    val name = "Medium" 
    val description = "Floculation moyenne - équilibre suspension/sédimentation"
    val clarificationTime = "Moyenne (1-2 semaines)"
    val rackingRecommendation = "Soutirage standard après fermentation"
  }
  
  case object MediumHigh extends FlocculationLevel {
    val name = "Medium-High"
    val description = "Floculation élevée - bonne sédimentation"
    val clarificationTime = "Rapide (5-10 jours)"
    val rackingRecommendation = "Soutirage précoce possible"
  }
  
  case object High extends FlocculationLevel {
    val name = "High"
    val description = "Haute floculation - sédimentation rapide et compacte"
    val clarificationTime = "Très rapide (3-7 jours)"
    val rackingRecommendation = "Soutirage précoce, attention atténuation"
  }
  
  case object VeryHigh extends FlocculationLevel {
    val name = "Very High"
    val description = "Floculation très élevée - risque d'atténuation incomplète"
    val clarificationTime = "Immédiate (2-5 jours)"
    val rackingRecommendation = "Remise en suspension si nécessaire"
  }
  
  val values: List[FlocculationLevel] = List(Low, Medium, MediumHigh, High, VeryHigh)
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[FlocculationLevel] = {
    values.find(_.name.equalsIgnoreCase(name.trim.replaceAll("-", "")))
      .orElse(values.find(_.name.toLowerCase.contains(name.toLowerCase.trim)))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, FlocculationLevel] = {
    val cleaned = input.trim.toLowerCase
    cleaned match {
      case s if s.matches("(very )?low|faible") => Right(Low)
      case s if s.matches("medium|moyenne?|moy") => Right(Medium)
      case s if s.matches("medium.?high|élevée?") => Right(MediumHigh)
      case s if s.matches("(very )?high|haute") => Right(High)
      case s if s.matches("very.?high|très.?haute") => Right(VeryHigh)
      case _ => fromName(input).toRight(s"Niveau de floculation inconnu: $input")
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 5: YEAST CHARACTERISTICS
# =============================================================================

echo -e "${YELLOW}🎭 Création YeastCharacteristics...${NC}"

cat > app/domain/yeasts/model/YeastCharacteristics.scala << 'EOF'
package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les caractéristiques organoleptiques des levures
 * Profil aromatique et gustatif produit
 */
case class YeastCharacteristics(
  aromaProfile: List[String],
  flavorProfile: List[String], 
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String] = None
) extends ValueObject {
  
  require(aromaProfile.nonEmpty || flavorProfile.nonEmpty, 
    "Au moins un profil aromatique ou gustatif requis")
  require(aromaProfile.forall(_.trim.nonEmpty), "Profils aromatiques ne peuvent pas être vides")
  require(flavorProfile.forall(_.trim.nonEmpty), "Profils gustatifs ne peuvent pas être vides")
  
  def allCharacteristics: List[String] = 
    aromaProfile ++ flavorProfile ++ esters ++ phenols ++ otherCompounds
  
  def hasEsters: Boolean = esters.nonEmpty
  def hasPhenols: Boolean = phenols.nonEmpty
  def isFruity: Boolean = allCharacteristics.exists(_.toLowerCase.contains("fruit"))
  def isSpicy: Boolean = allCharacteristics.exists(c => 
    c.toLowerCase.matches(".*(spic|pepper|clove|phenol).*"))
  def isClean: Boolean = allCharacteristics.exists(_.toLowerCase.contains("clean"))
  
  override def toString: String = {
    val characteristics = List(
      if (aromaProfile.nonEmpty) Some(s"Arômes: ${aromaProfile.mkString(", ")}") else None,
      if (flavorProfile.nonEmpty) Some(s"Saveurs: ${flavorProfile.mkString(", ")}") else None,
      if (esters.nonEmpty) Some(s"Esters: ${esters.mkString(", ")}") else None,
      if (phenols.nonEmpty) Some(s"Phénols: ${phenols.mkString(", ")}") else None
    ).flatten
    
    characteristics.mkString(" | ") + notes.map(n => s" | Notes: $n").getOrElse("")
  }
}

object YeastCharacteristics {
  
  /**
   * Constructeur simple pour profils basiques
   */
  def simple(characteristics: String*): YeastCharacteristics = {
    YeastCharacteristics(
      aromaProfile = characteristics.toList,
      flavorProfile = List.empty,
      esters = List.empty,
      phenols = List.empty,
      otherCompounds = List.empty
    )
  }
  
  /**
   * Caractéristiques prédéfinies communes
   */
  val Clean = YeastCharacteristics(
    aromaProfile = List("Clean", "Neutral"),
    flavorProfile = List("Clean", "Subtle"),
    esters = List.empty,
    phenols = List.empty,
    otherCompounds = List.empty,
    notes = Some("Profil neutre, met en valeur les malts et houblons")
  )
  
  val Fruity = YeastCharacteristics(
    aromaProfile = List("Fruity", "Stone fruit", "Citrus"),
    flavorProfile = List("Fruity", "Sweet"),
    esters = List("Ethyl acetate", "Isoamyl acetate"),
    phenols = List.empty,
    otherCompounds = List.empty,
    notes = Some("Esters fruités prononcés")
  )
  
  val Spicy = YeastCharacteristics(
    aromaProfile = List("Spicy", "Phenolic", "Clove"),
    flavorProfile = List("Spicy", "Peppery"),
    esters = List.empty,
    phenols = List("4-vinyl guaiacol"),
    otherCompounds = List.empty,
    notes = Some("Phénols épicés caractéristiques")
  )
  
  val Tropical = YeastCharacteristics(
    aromaProfile = List("Tropical", "Pineapple", "Mango", "Passion fruit"),
    flavorProfile = List("Fruity", "Tropical"),
    esters = List("Ethyl butyrate", "Ethyl hexanoate"),
    phenols = List.empty,
    otherCompounds = List("Thiols"),
    notes = Some("Arômes tropicaux intenses, populaire pour NEIPAs")
  )
}
EOF

echo -e "${GREEN}✅ Value Objects avancés créés${NC}"
echo -e "${YELLOW}📋 Prochaine étape : ./scripts/yeast/02-create-aggregate.sh${NC}"