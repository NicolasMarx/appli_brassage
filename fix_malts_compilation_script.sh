#!/bin/bash

# =============================================================================
# SCRIPT DE CORRECTION - COMPILATION DOMAINE MALTS
# =============================================================================
# Corrige tous les problèmes de compilation identifiés dans le domaine Malts
# Crée/remplace tous les fichiers nécessaires avec les bonnes implémentations
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🔧 =============================================================================="
echo "   SCRIPT DE CORRECTION - COMPILATION DOMAINE MALTS"
echo "==============================================================================${NC}"

# Fonction de création de fichiers avec backup
create_or_update_file() {
    local file_path="$1"
    local content="$2"
    local description="$3"
    
    # Créer le dossier si nécessaire
    mkdir -p "$(dirname "$file_path")"
    
    # Backup si le fichier existe
    if [ -f "$file_path" ]; then
        cp "$file_path" "$file_path.backup.$(date +%Y%m%d_%H%M%S)"
        echo -e "   ${YELLOW}📁 Backup: $file_path.backup${NC}"
    fi
    
    echo "$content" > "$file_path"
    echo -e "   ${GREEN}✅ $description${NC}"
}

echo ""
echo -e "${PURPLE}🌾 ÉTAPE 1 : CRÉATION/CORRECTION VALUE OBJECTS${NC}"

# =============================================================================
# MALTID.SCALA
# =============================================================================

create_or_update_file "app/domain/malts/model/MaltId.scala" 'package domain.malts.model

import play.api.libs.json._
import java.util.UUID

/**
 * Value Object MaltId - Identifiant unique pour les malts
 * Utilise UUID pour garantir l'\''unicité globale
 */
final case class MaltId private (value: String) extends AnyVal {
  override def toString: String = value
}

object MaltId {
  
  def apply(value: String): Either[String, MaltId] = {
    val trimmed = value.trim
    
    if (trimmed.isEmpty) {
      Left("MaltId ne peut pas être vide")
    } else {
      try {
        UUID.fromString(trimmed) // Validation UUID
        Right(new MaltId(trimmed))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"MaltId doit être un UUID valide: '\''$trimmed'\''")
      }
    }
  }
  
  def generate(): MaltId = new MaltId(UUID.randomUUID().toString)
  
  def fromString(value: String): MaltId = {
    apply(value) match {
      case Right(maltId) => maltId
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }
  
  implicit val maltIdFormat: Format[MaltId] = new Format[MaltId] {
    def reads(json: JsValue): JsResult[MaltId] = {
      json.validate[String].flatMap { str =>
        MaltId(str) match {
          case Right(maltId) => JsSuccess(maltId)
          case Left(error) => JsError(error)
        }
      }
    }
    
    def writes(maltId: MaltId): JsValue = JsString(maltId.value)
  }
}' "MaltId.scala avec validation UUID"

# =============================================================================
# EBCCOLOR.SCALA
# =============================================================================

create_or_update_file "app/domain/malts/model/EBCColor.scala" 'package domain.malts.model

import play.api.libs.json._

/**
 * Value Object EBCColor - Couleur EBC avec validation et calcul automatique du nom
 */
final case class EBCColor private (value: Double) extends AnyVal {
  def colorName: String = EBCColor.getColorName(value)
  def isMaltCanBeUsed: Boolean = value >= 0 && value <= 1000
  def isBaseMaltColor: Boolean = value >= 2 && value <= 8
  def isCrystalMaltColor: Boolean = value >= 20 && value <= 300
  def isRoastedMaltColor: Boolean = value >= 300
}

object EBCColor {
  
  def apply(value: Double): Either[String, EBCColor] = {
    if (value < 0) {
      Left(s"La couleur EBC doit être positive: $value")
    } else if (value > 1000) {
      Left(s"La couleur EBC ne peut excéder 1000: $value")
    } else {
      Right(new EBCColor(value))
    }
  }
  
  def getColorName(ebc: Double): String = ebc match {
    case v if v <= 4   => "Très pâle"
    case v if v <= 8   => "Pâle"
    case v if v <= 12  => "Doré"
    case v if v <= 25  => "Ambre"
    case v if v <= 50  => "Cuivre"
    case v if v <= 100 => "Brun"
    case v if v <= 300 => "Brun foncé"
    case _             => "Noir"
  }
  
  // Constantes communes
  def PILSNER: EBCColor = EBCColor(3.5).getOrElse(throw new Exception("Invalid EBC"))
  def WHEAT: EBCColor = EBCColor(4.0).getOrElse(throw new Exception("Invalid EBC"))
  def MUNICH: EBCColor = EBCColor(15.0).getOrElse(throw new Exception("Invalid EBC"))
  def CRYSTAL_40: EBCColor = EBCColor(80.0).getOrElse(throw new Exception("Invalid EBC"))
  def CHOCOLATE: EBCColor = EBCColor(900.0).getOrElse(throw new Exception("Invalid EBC"))
  def ROASTED_BARLEY: EBCColor = EBCColor(1000.0).getOrElse(throw new Exception("Invalid EBC"))
  
  implicit val format: Format[EBCColor] = new Format[EBCColor] {
    def reads(json: JsValue): JsResult[EBCColor] = {
      json.validate[Double].flatMap { value =>
        EBCColor(value) match {
          case Right(ebc) => JsSuccess(ebc)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(ebc: EBCColor): JsValue = JsNumber(ebc.value)
  }
}' "EBCColor.scala avec validation et noms de couleurs"

# =============================================================================
# EXTRACTIONRATE.SCALA
# =============================================================================

create_or_update_file "app/domain/malts/model/ExtractionRate.scala" 'package domain.malts.model

import play.api.libs.json._

/**
 * Value Object ExtractionRate - Taux d'\''extraction avec validation et catégorisation
 */
final case class ExtractionRate private (value: Double) extends AnyVal {
  def percentage: Double = value * 100
  def extractionCategory: String = ExtractionRate.getCategory(value)
  def isHighExtraction: Boolean = value >= 0.82
  def isLowExtraction: Boolean = value <= 0.75
}

object ExtractionRate {
  
  def apply(value: Double): Either[String, ExtractionRate] = {
    if (value < 0.5) {
      Left(s"Le taux d'\''extraction doit être au moins 50%: ${value * 100}%")
    } else if (value > 1.0) {
      Left(s"Le taux d'\''extraction ne peut excéder 100%: ${value * 100}%")
    } else {
      Right(new ExtractionRate(value))
    }
  }
  
  def fromPercentage(percentage: Double): Either[String, ExtractionRate] = {
    apply(percentage / 100.0)
  }
  
  def getCategory(rate: Double): String = rate match {
    case v if v >= 0.85 => "Très élevé"
    case v if v >= 0.82 => "Élevé"
    case v if v >= 0.78 => "Moyen"
    case v if v >= 0.75 => "Faible"
    case _              => "Très faible"
  }
  
  // Constantes typiques
  def HIGH_QUALITY_BASE: ExtractionRate = ExtractionRate(0.85).getOrElse(throw new Exception("Invalid extraction"))
  def STANDARD_BASE: ExtractionRate = ExtractionRate(0.82).getOrElse(throw new Exception("Invalid extraction"))
  def CRYSTAL_AVERAGE: ExtractionRate = ExtractionRate(0.75).getOrElse(throw new Exception("Invalid extraction"))
  def ROASTED_AVERAGE: ExtractionRate = ExtractionRate(0.70).getOrElse(throw new Exception("Invalid extraction"))
  
  implicit val format: Format[ExtractionRate] = new Format[ExtractionRate] {
    def reads(json: JsValue): JsResult[ExtractionRate] = {
      json.validate[Double].flatMap { value =>
        ExtractionRate(value) match {
          case Right(rate) => JsSuccess(rate)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(rate: ExtractionRate): JsValue = JsNumber(rate.value)
  }
}' "ExtractionRate.scala avec validation et catégories"

# =============================================================================
# DIASTATICPOWER.SCALA
# =============================================================================

create_or_update_file "app/domain/malts/model/DiastaticPower.scala" 'package domain.malts.model

import play.api.libs.json._

/**
 * Value Object DiastaticPower - Pouvoir diastasique avec validation métier
 */
final case class DiastaticPower private (value: Double) extends AnyVal {
  def canConvertAdjuncts: Boolean = value >= 30
  def isHighEnzyme: Boolean = value >= 100
  def isLowEnzyme: Boolean = value <= 20
  def enzymePowerCategory: String = DiastaticPower.getCategory(value)
}

object DiastaticPower {
  
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value < 0) {
      Left(s"Le pouvoir diastasique ne peut être négatif: $value")
    } else if (value > 200) {
      Left(s"Le pouvoir diastasique semble irréaliste: $value (max recommandé: 200)")
    } else {
      Right(new DiastaticPower(value))
    }
  }
  
  def getCategory(power: Double): String = power match {
    case v if v >= 100 => "Très élevé"
    case v if v >= 50  => "Élevé"
    case v if v >= 30  => "Moyen"
    case v if v >= 10  => "Faible"
    case _             => "Négligeable"
  }
  
  // Constantes typiques
  def BASE_MALT_HIGH: DiastaticPower = DiastaticPower(140.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def BASE_MALT_STANDARD: DiastaticPower = DiastaticPower(100.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def SELF_CONVERTING: DiastaticPower = DiastaticPower(30.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def CRYSTAL_TYPICAL: DiastaticPower = DiastaticPower(0.0).getOrElse(throw new Exception("Invalid diastatic power"))
  def ROASTED_TYPICAL: DiastaticPower = DiastaticPower(0.0).getOrElse(throw new Exception("Invalid diastatic power"))
  
  implicit val format: Format[DiastaticPower] = new Format[DiastaticPower] {
    def reads(json: JsValue): JsResult[DiastaticPower] = {
      json.validate[Double].flatMap { value =>
        DiastaticPower(value) match {
          case Right(power) => JsSuccess(power)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(power: DiastaticPower): JsValue = JsNumber(power.value)
  }
}' "DiastaticPower.scala avec validation et logique enzymatique"

echo ""
echo -e "${PURPLE}🌾 ÉTAPE 2 : CORRECTION MALTAGGREGATE${NC}"

# =============================================================================
# MALTAGGREGATE.SCALA CORRIGÉ
# =============================================================================

create_or_update_file "app/domain/malts/model/MaltAggregate.scala" 'package domain.malts.model

import domain.shared.NonEmptyString
import domain.common.DomainError
import java.time.Instant
import play.api.libs.json._

/**
 * Agrégat Malt - Représente un malt dans le système de brassage
 * Suit le pattern DDD avec Event Sourcing
 */
case class MaltAggregate private(
  id: MaltId,
  name: NonEmptyString,
  maltType: MaltType,
  ebcColor: EBCColor,
  extractionRate: ExtractionRate,
  diastaticPower: DiastaticPower,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: MaltSource,
  isActive: Boolean,
  credibilityScore: Double,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
) {

  // Propriétés calculées pour compatibility avec les handlers existants
  def needsReview: Boolean = credibilityScore < 0.8
  def canSelfConvert: Boolean = diastaticPower.canConvertAdjuncts
  def qualityScore: Double = credibilityScore * 100
  def maxRecommendedPercent: Option[Double] = maltType match {
    case MaltType.BASE => Some(100.0)
    case MaltType.CRYSTAL => Some(20.0)
    case MaltType.ROASTED => Some(10.0)
    case _ => Some(30.0)
  }
  def isBaseMalt: Boolean = maltType == MaltType.BASE
  
  // Propriétés métier calculées
  def colorCategory: String = ebcColor.colorName
  def extractionCategory: String = extractionRate.extractionCategory
  def enzymePowerCategory: String = diastaticPower.enzymePowerCategory
  
  // Validation métier
  def isValidForBrewing: Boolean = {
    isActive && credibilityScore >= 0.5 && 
    ebcColor.isMaltCanBeUsed && 
    extractionRate.value >= 0.6
  }

  /**
   * Désactivation du malt avec validation métier
   */
  def deactivate(reason: String): Either[DomainError, MaltAggregate] = {
    if (!isActive) {
      Left(DomainError.businessRule("Le malt est déjà désactivé", "ALREADY_DEACTIVATED"))
    } else {
      Right(this.copy(
        isActive = false,
        updatedAt = Instant.now(),
        version = version + 1
      ))
    }
  }
  
  /**
   * Mise à jour des informations du malt avec versioning
   */
  def updateInfo(
    name: Option[NonEmptyString] = None,
    maltType: Option[MaltType] = None,
    ebcColor: Option[EBCColor] = None,
    extractionRate: Option[ExtractionRate] = None,
    diastaticPower: Option[DiastaticPower] = None,
    originCode: Option[String] = None,
    description: Option[Option[String]] = None,
    flavorProfiles: Option[List[String]] = None
  ): Either[DomainError, MaltAggregate] = {
    
    val updatedFlavorProfiles = flavorProfiles.getOrElse(this.flavorProfiles)
    
    if (updatedFlavorProfiles.length > MaltAggregate.MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum ${MaltAggregate.MaxFlavorProfiles} profils arômes autorisés"))
    } else {
      Right(this.copy(
        name = name.getOrElse(this.name),
        maltType = maltType.getOrElse(this.maltType),
        ebcColor = ebcColor.getOrElse(this.ebcColor),
        extractionRate = extractionRate.getOrElse(this.extractionRate),
        diastaticPower = diastaticPower.getOrElse(this.diastaticPower),
        originCode = originCode.getOrElse(this.originCode),
        description = description.getOrElse(this.description),
        flavorProfiles = updatedFlavorProfiles.filter(_.trim.nonEmpty).distinct,
        updatedAt = Instant.now(),
        version = version + 1
      ))
    }
  }
}

object MaltAggregate {
  
  val MaxFlavorProfiles = 10
  
  /**
   * Création d'\''un nouveau malt avec validation complète
   */
  def create(
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    originCode: String,
    source: MaltSource,
    description: Option[String] = None,
    flavorProfiles: List[String] = List.empty
  ): Either[DomainError, MaltAggregate] = {
    
    // Validation des profils aromatiques
    val cleanedProfiles = flavorProfiles.filter(_.trim.nonEmpty).distinct
    if (cleanedProfiles.length > MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum $MaxFlavorProfiles profils arômes autorisés"))
    } else if (originCode.trim.isEmpty) {
      Left(DomainError.validation("Le code origine ne peut pas être vide"))
    } else if (originCode.length > 10) {
      Left(DomainError.validation("Le code origine ne peut excéder 10 caractères"))
    } else {
      val now = Instant.now()
      Right(MaltAggregate(
        id = MaltId.generate(),
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = originCode.trim.toUpperCase,
        description = description.map(_.trim).filter(_.nonEmpty),
        flavorProfiles = cleanedProfiles,
        source = source,
        isActive = true,
        credibilityScore = calculateInitialCredibility(source),
        createdAt = now,
        updatedAt = now,
        version = 1L
      ))
    }
  }
  
  /**
   * ❗ Constructeur direct pour la persistence (ne pas utiliser dans la logique métier)
   * Utilisé par les repositories pour reconstituer l'\''agrégat depuis la DB
   */
  def apply(
    id: MaltId,
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    originCode: String,
    description: Option[String],
    flavorProfiles: List[String],
    source: MaltSource,
    isActive: Boolean,
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  ): MaltAggregate = {
    new MaltAggregate(
      id, name, maltType, ebcColor, extractionRate, diastaticPower,
      originCode, description, flavorProfiles, source, isActive,
      credibilityScore, createdAt, updatedAt, version
    )
  }
  
  /**
   * Calcule le score de crédibilité initial selon la source
   */
  private def calculateInitialCredibility(source: MaltSource): Double = source match {
    case MaltSource.Manual => 1.0
    case MaltSource.AI_Discovery => 0.7
    case MaltSource.Import => 0.8
  }
}' "MaltAggregate.scala avec logique métier complète"

echo ""
echo -e "${PURPLE}🌾 ÉTAPE 3 : CORRECTION MALTTABLES${NC}"

# =============================================================================
# MALTTABLES.SCALA CORRIGÉ
# =============================================================================

create_or_update_file "infrastructure/persistence/slick/tables/MaltTables.scala" 'package infrastructure.persistence.slick.tables

import slick.jdbc.JdbcProfile
import java.time.Instant

trait MaltTables {
  val profile: JdbcProfile
  import profile.api._

  // Case class pour les lignes de la table malts (corrigé)
  case class MaltRow(
    id: String, // UUID au format String
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String],
    source: String,
    isActive: Boolean,
    credibilityScore: Double, // ❗ Corrigé: Double au lieu d'\''Int
    createdAt: Instant,
    updatedAt: Instant,
    version: Long // ❗ Corrigé: Long au lieu d'\''Int
  )

  // Table Slick pour malts (corrigée)
  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def maltType = column[String]("malt_type")
    def ebcColor = column[Double]("ebc_color")
    def extractionRate = column[Double]("extraction_rate")
    def diastaticPower = column[Double]("diastatic_power")
    def originCode = column[String]("origin_code")
    def description = column[Option[String]]("description")
    def flavorProfiles = column[Option[String]]("flavor_profiles")
    def source = column[String]("source")
    def isActive = column[Boolean]("is_active")
    def credibilityScore = column[Double]("credibility_score") // ❗ Corrigé: Double
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version") // ❗ Corrigé: Long

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
    
    // Index pour optimiser les requêtes courantes
    def idx = index("idx_malts_active_type", (isActive, maltType))
    def nameIdx = index("idx_malts_name", name, unique = true)
    def colorIdx = index("idx_malts_color", ebcColor)
  }

  // Tables de support (simplifiées)
  case class MaltBeerStyleRow(
    id: String,
    maltId: String,
    beerStyleId: String,
    compatibilityScore: Double,
    usageNotes: Option[String]
  )

  class MaltBeerStyleTable(tag: Tag) extends Table[MaltBeerStyleRow](tag, "malt_beer_styles") {
    def id = column[String]("id", O.PrimaryKey)
    def maltId = column[String]("malt_id")
    def beerStyleId = column[String]("beer_style_id")
    def compatibilityScore = column[Double]("compatibility_score")
    def usageNotes = column[Option[String]]("usage_notes")

    def * = (id, maltId, beerStyleId, compatibilityScore, usageNotes).mapTo[MaltBeerStyleRow]
    
    // Foreign keys pour intégrité référentielle
    def maltFk = foreignKey("fk_malt_beer_style_malt", maltId, malts)(_.id)
  }

  case class MaltSubstitutionRow(
    id: String,
    maltId: String,
    substituteId: String,
    substitutionRatio: Double,
    notes: Option[String],
    qualityScore: Double
  )

  class MaltSubstitutionTable(tag: Tag) extends Table[MaltSubstitutionRow](tag, "malt_substitutions") {
    def id = column[String]("id", O.PrimaryKey)
    def maltId = column[String]("malt_id")
    def substituteId = column[String]("substitute_id")
    def substitutionRatio = column[Double]("substitution_ratio")
    def notes = column[Option[String]]("notes")
    def qualityScore = column[Double]("quality_score")

    def * = (id, maltId, substituteId, substitutionRatio, notes, qualityScore).mapTo[MaltSubstitutionRow]
    
    // Foreign keys pour intégrité référentielle
    def maltFk = foreignKey("fk_malt_substitution_malt", maltId, malts)(_.id)
    def substituteFk = foreignKey("fk_malt_substitution_substitute", substituteId, malts)(_.id)
  }

  // Conversion row -> aggregate (corrigée)
  def rowToAggregate(row: MaltRow): Either[String, domain.malts.model.MaltAggregate] = {
    import domain.malts.model._
    import domain.shared.NonEmptyString
    
    for {
      maltId <- MaltId(row.id) // MaltId.apply(String): Either[String, MaltId]
      name <- NonEmptyString.create(row.name) // ✅ Utilise create qui retourne Either[String, NonEmptyString]
      maltType <- MaltType.fromName(row.maltType).toRight(s"Type malt invalide: ${row.maltType}")
      ebcColor <- EBCColor(row.ebcColor) // EBCColor.apply(Double): Either[String, EBCColor]
      extractionRate <- ExtractionRate(row.extractionRate) // ExtractionRate.apply(Double): Either[String, ExtractionRate]
      diastaticPower <- DiastaticPower(row.diastaticPower) // DiastaticPower.apply(Double): Either[String, DiastaticPower]
      source <- MaltSource.fromName(row.source).toRight(s"Source invalide: ${row.source}")
    } yield {
      // ❗ Utilise la méthode de création directe de MaltAggregate (pour persistence)
      MaltAggregate(
        id = maltId,
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = row.originCode,
        description = row.description,
        flavorProfiles = row.flavorProfiles.map(_.split(",").toList.filter(_.trim.nonEmpty)).getOrElse(List.empty),
        source = source,
        isActive = row.isActive,
        credibilityScore = row.credibilityScore, // ✅ Déjà Double
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version // ✅ Déjà Long
      )
    }
  }

  // Conversion aggregate -> row (corrigée)
  def aggregateToRow(aggregate: domain.malts.model.MaltAggregate): MaltRow = {
    MaltRow(
      id = aggregate.id.value, // MaltId.value est String (UUID au format String)
      name = aggregate.name.value, // NonEmptyString.value
      maltType = aggregate.maltType.name, // MaltType.name
      ebcColor = aggregate.ebcColor.value, // EBCColor.value: Double
      extractionRate = aggregate.extractionRate.value, // ExtractionRate.value: Double
      diastaticPower = aggregate.diastaticPower.value, // DiastaticPower.value: Double
      originCode = aggregate.originCode,
      description = aggregate.description,
      flavorProfiles = if (aggregate.flavorProfiles.nonEmpty) Some(aggregate.flavorProfiles.mkString(",")) else None,
      source = aggregate.source.name, // MaltSource.name
      isActive = aggregate.isActive,
      credibilityScore = aggregate.credibilityScore, // ✅ Déjà Double
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version // ✅ Déjà Long
    )
  }

  // ❗ Helper pour conversion sécurisée avec gestion d'\''erreurs
  def safeRowToAggregate(row: MaltRow): Option[domain.malts.model.MaltAggregate] = {
    rowToAggregate(row) match {
      case Right(aggregate) => Some(aggregate)
      case Left(error) => 
        // Log l'\''erreur dans un vrai système
        println(s"Erreur conversion row -> aggregate: $error")
        None
    }
  }

  // TableQuery instances
  val malts = TableQuery[MaltTable]
  val maltBeerStyles = TableQuery[MaltBeerStyleTable]
  val maltSubstitutions = TableQuery[MaltSubstitutionTable]
}' "MaltTables.scala avec types corrigés et index optimisés"

echo ""
echo -e "${PURPLE}🌾 ÉTAPE 4 : AJOUT FORMAT JSON POUR NONEMPTYSTRING${NC}"

# =============================================================================
# CORRECTION NONEMPTYSTRING AVEC FORMAT JSON
# =============================================================================

create_or_update_file "app/domain/shared/NonEmptyString.scala" 'package domain.shared

import play.api.libs.json._

case class NonEmptyString(value: String) extends AnyVal

object NonEmptyString {
  def apply(value: String): NonEmptyString = {
    val trimmed = value.trim
    require(trimmed.nonEmpty, "String cannot be empty")
    new NonEmptyString(trimmed)
  }

  def create(value: String): Either[String, NonEmptyString] = {
    val trimmed = value.trim
    if (trimmed.nonEmpty) Right(NonEmptyString(trimmed))
    else Left("String cannot be empty")
  }
  
  // ❗ Format JSON manquant ajouté
  implicit val format: Format[NonEmptyString] = new Format[NonEmptyString] {
    def reads(json: JsValue): JsResult[NonEmptyString] = {
      json.validate[String].flatMap { str =>
        create(str) match {
          case Right(nonEmptyString) => JsSuccess(nonEmptyString)
          case Left(error) => JsError(error)
        }
      }
    }
    def writes(nonEmptyString: NonEmptyString): JsValue = JsString(nonEmptyString.value)
  }
}' "NonEmptyString.scala avec format JSON"

echo ""
echo -e "${PURPLE}🌾 ÉTAPE 5 : TEST DE COMPILATION${NC}"

echo -e "${BLUE}🔍 Test de compilation du projet...${NC}"

# Test de compilation
if sbt compile > /tmp/malts_compilation_fix.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    COMPILATION_SUCCESS=true
else
    echo -e "${RED}❌ Erreurs de compilation persistantes${NC}"
    echo -e "${YELLOW}Consultez /tmp/malts_compilation_fix.log pour les détails${NC}"
    echo ""
    echo -e "${YELLOW}Dernières erreurs :${NC}"
    tail -20 /tmp/malts_compilation_fix.log
    COMPILATION_SUCCESS=false
fi

echo ""
echo -e "${BLUE}📊 RAPPORT FINAL${NC}"
echo ""

if [ "$COMPILATION_SUCCESS" = true ]; then
    echo -e "${GREEN}🎉 TOUTES LES CORRECTIONS APPLIQUÉES AVEC SUCCÈS !${NC}"
    echo ""
    echo -e "${GREEN}✅ Composants créés/corrigés :${NC}"
    echo -e "   🔧 MaltId : UUID avec validation Either[String, MaltId]"
    echo -e "   🔧 EBCColor : Validation couleur avec noms automatiques"
    echo -e "   🔧 ExtractionRate : Validation taux avec catégories"
    echo -e "   🔧 DiastaticPower : Validation enzymatique avec logique métier"
    echo -e "   🏗️  MaltAggregate : Logique métier complète avec Event Sourcing"
    echo -e "   💾 MaltTables : Types corrigés (Double, Long) avec index optimisés"
    echo -e "   📝 NonEmptyString : Format JSON ajouté"
    echo ""
    echo -e "${BLUE}🎯 Prochaines étapes recommandées :${NC}"
    echo -e "   1. Implémenter les repositories Slick complets"
    echo -e "   2. Créer les command handlers avec logique métier"
    echo -e "   3. Implémenter les query handlers avec pagination"
    echo -e "   4. Créer les API controllers sécurisés"
    echo -e "   5. Ajouter les tests unitaires"
    echo ""
    echo -e "${GREEN}🚀 Le domaine Malts est maintenant prêt pour l'\''implémentation !${NC}"
    
else
    echo -e "${RED}❌ PROBLÈMES PERSISTANTS${NC}"
    echo ""
    echo -e "${YELLOW}Actions recommandées :${NC}"
    echo -e "   1. Vérifiez que tous les imports sont corrects"
    echo -e "   2. Consultez le log complet : /tmp/malts_compilation_fix.log"
    echo -e "   3. Vérifiez que MaltType, MaltSource existent"
    echo -e "   4. Vérifiez que DomainError existe dans domain.common"
    echo ""
    echo -e "${YELLOW}Si problèmes persistent, partagez le contenu du log.${NC}"
fi

echo ""
echo -e "${BLUE}📁 Fichiers de backup créés avec timestamp pour rollback si nécessaire${NC}"
echo -e "${GREEN}🌾 Script de correction terminé !${NC}"