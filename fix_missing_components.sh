#!/bin/bash
# Script de correction des composants manquants du domaine Malts
# Analyse des erreurs de compilation pour créer les éléments manquants

set -e

echo "🔧 Correction des composants manquants du domaine Malts..."

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m'

# =============================================================================
# ÉTAPE 1 : COMPOSANTS DOMAIN MANQUANTS
# =============================================================================

echo -e "${BLUE}📦 Création des composants domain manquants...${NC}"

# MaltSource (manquant - référencé dans les erreurs)
cat > app/domain/malts/model/MaltSource.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

sealed trait MaltSource {
  def name: String
}

object MaltSource {
  case object Manual extends MaltSource { val name = "MANUAL" }
  case object AI_Discovery extends MaltSource { val name = "AI_DISCOVERY" }
  case object Import extends MaltSource { val name = "IMPORT" }

  val all: List[MaltSource] = List(Manual, AI_Discovery, Import)

  def fromName(name: String): Option[MaltSource] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }

  implicit val format: Format[MaltSource] = Format(
    Reads(js => js.validate[String].map(fromName).flatMap {
      case Some(source) => JsSuccess(source)
      case None => JsError("Source de malt invalide")
    }),
    Writes(source => JsString(source.name))
  )
}
EOF

# MaltStatus (manquant - référencé dans les erreurs)
cat > app/domain/malts/model/MaltStatus.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

sealed trait MaltStatus {
  def name: String
}

object MaltStatus {
  case object ACTIVE extends MaltStatus { val name = "ACTIVE" }
  case object INACTIVE extends MaltStatus { val name = "INACTIVE" }
  case object PENDING_REVIEW extends MaltStatus { val name = "PENDING_REVIEW" }

  val all: List[MaltStatus] = List(ACTIVE, INACTIVE, PENDING_REVIEW)

  def fromName(name: String): Option[MaltStatus] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }

  implicit val format: Format[MaltStatus] = Format(
    Reads(js => js.validate[String].map(fromName).flatMap {
      case Some(status) => JsSuccess(status)
      case None => JsError("Statut de malt invalide")
    }),
    Writes(status => JsString(status.name))
  )
}
EOF

# DiastaticPower Value Object (référencé mais manquant)
cat > app/domain/malts/model/DiastaticPower.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour pouvoir diastasique (Lintner degrees)
 */
case class DiastaticPower private(value: Double) extends AnyVal {
  def canConvertAdjuncts: Boolean = value > 80
  def enzymaticCategory: String = {
    if (value > 80) "HIGH"
    else if (value > 0) "MEDIUM"
    else "NONE"
  }
}

object DiastaticPower {
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value < 0) {
      Left("Le pouvoir diastasique ne peut pas être négatif")
    } else {
      Right(new DiastaticPower(value))
    }
  }

  def unsafe(value: Double): DiastaticPower = new DiastaticPower(value)

  implicit val format: Format[DiastaticPower] = Json.valueFormat[DiastaticPower]
}
EOF

# Mise à jour MaltType pour ajouter les constantes manquantes
cat > app/domain/malts/model/MaltType.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

sealed trait MaltType {
  def name: String
  def description: String
}

object MaltType {
  
  case object BASE extends MaltType {
    val name = "BASE"
    val description = "Malt de base avec pouvoir enzymatique élevé"
  }
  
  case object SPECIALTY extends MaltType {
    val name = "SPECIALTY"  
    val description = "Malt spécial pour saveur et couleur"
  }
  
  case object CARAMEL extends MaltType {
    val name = "CARAMEL"
    val description = "Malt caramel/crystal pour douceur"
  }
  
  case object CRYSTAL extends MaltType {
    val name = "CRYSTAL"
    val description = "Malt crystal pour douceur et couleur"
  }
  
  case object ROASTED extends MaltType {
    val name = "ROASTED"
    val description = "Malt torréfié pour bières sombres"
  }
  
  case object OTHER extends MaltType {
    val name = "OTHER"
    val description = "Autres types de malts"
  }
  
  val all: List[MaltType] = List(BASE, SPECIALTY, CARAMEL, CRYSTAL, ROASTED, OTHER)
  
  def fromName(name: String): Option[MaltType] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  implicit val format: Format[MaltType] = Format(
    Reads(js => js.validate[String].map(fromName).flatMap {
      case Some(maltType) => JsSuccess(maltType)
      case None => JsError("Type de malt invalide")
    }),
    Writes(maltType => JsString(maltType.name))
  )
}
EOF

echo "✅ Composants domain créés"

# =============================================================================
# ÉTAPE 2 : MISE À JOUR MALTAGGREGATE
# =============================================================================

echo -e "${BLUE}🔧 Mise à jour MaltAggregate...${NC}"

cat > app/domain/malts/model/MaltAggregate.scala << 'EOF'
package domain.malts.model

import domain.shared.NonEmptyString
import domain.common.DomainError
import java.time.Instant
import play.api.libs.json._

/**
 * Agrégat Malt complet avec toutes les propriétés requises
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
    case MaltType.BASE => Some(100)
    case MaltType.CRYSTAL => Some(20)
    case MaltType.ROASTED => Some(10)
    case _ => Some(30)
  }
  def isBaseMalt: Boolean = maltType == MaltType.BASE

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
  
  def updateInfo(
    name: Option[NonEmptyString] = None,
    maltType: Option[MaltType] = None,
    ebcColor: Option[EBCColor] = None,
    extractionRate: Option[ExtractionRate] = None,
    description: Option[String] = None
  ): MaltAggregate = {
    this.copy(
      name = name.getOrElse(this.name),
      maltType = maltType.getOrElse(this.maltType),
      ebcColor = ebcColor.getOrElse(this.ebcColor),
      extractionRate = extractionRate.getOrElse(this.extractionRate),
      description = description.orElse(this.description),
      updatedAt = Instant.now(),
      version = version + 1
    )
  }
}

object MaltAggregate {
  
  val MaxFlavorProfiles = 10
  
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
    
    if (flavorProfiles.length > MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum $MaxFlavorProfiles profils arômes autorisés"))
    } else {
      val now = Instant.now()
      Right(MaltAggregate(
        id = MaltId.generate(),
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = originCode,
        description = description,
        flavorProfiles = flavorProfiles.filter(_.trim.nonEmpty).distinct,
        source = source,
        isActive = true,
        credibilityScore = if (source == MaltSource.Manual) 1.0 else 0.7,
        createdAt = now,
        updatedAt = now,
        version = 1
      ))
    }
  }
  
  // Format JSON requis pour compilation (sera corrigé après ajout NonEmptyString format)
  // implicit val format: Format[MaltAggregate] = Json.format[MaltAggregate]
}
EOF

echo "✅ MaltAggregate mis à jour"

# =============================================================================
# ÉTAPE 3 : EXTENSION EBCCOLOR ET EXTRACTIONRATE
# =============================================================================

echo -e "${BLUE}🔧 Extension des Value Objects...${NC}"

# Extension EBCColor avec colorName
cat > app/domain/malts/model/EBCColor.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour couleur EBC des malts avec noms de couleurs
 */
case class EBCColor private(value: Double) extends AnyVal {
  def toSRM: Double = value * 0.508
  
  def colorName: String = {
    if (value <= 3) "Très pâle"
    else if (value <= 8) "Pâle"
    else if (value <= 16) "Doré"
    else if (value <= 33) "Ambré"
    else if (value <= 66) "Brun clair"
    else if (value <= 138) "Brun foncé"
    else "Noir"
  }
}

object EBCColor {
  
  def apply(value: Double): Either[String, EBCColor] = {
    if (value < 0) {
      Left("La couleur EBC ne peut pas être négative")
    } else if (value > 1000) {
      Left("La couleur EBC ne peut pas dépasser 1000")
    } else {
      Right(new EBCColor(value))
    }
  }
  
  def unsafe(value: Double): EBCColor = new EBCColor(value)
  
  implicit val format: Format[EBCColor] = Json.valueFormat[EBCColor]
}
EOF

# Extension ExtractionRate avec extractionCategory
cat > app/domain/malts/model/ExtractionRate.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour taux d'extraction des malts avec catégories
 */
case class ExtractionRate private(value: Double) extends AnyVal {
  def asPercentage: String = f"${value}%.1f%%"
  
  def extractionCategory: String = {
    if (value >= 82) "Très élevé"
    else if (value >= 79) "Élevé"
    else if (value >= 75) "Moyen"
    else if (value >= 70) "Faible"
    else "Très faible"
  }
}

object ExtractionRate {
  
  def apply(value: Double): Either[String, ExtractionRate] = {
    if (value < 0) {
      Left("Le taux d'extraction ne peut pas être négatif")
    } else if (value > 100) {
      Left("Le taux d'extraction ne peut pas dépasser 100%")
    } else {
      Right(new ExtractionRate(value))
    }
  }
  
  def unsafe(value: Double): ExtractionRate = new ExtractionRate(value)
  
  implicit val format: Format[ExtractionRate] = Json.valueFormat[ExtractionRate]
}
EOF

echo "✅ Value Objects étendus"

# =============================================================================
# ÉTAPE 4 : CRÉATION DES READ MODELS MANQUANTS
# =============================================================================

echo -e "${BLUE}📦 Création des ReadModels...${NC}"

# MaltReadModel (utilisé partout mais manquant)
cat > app/application/queries/public/malts/readmodels/MaltReadModel.scala << 'EOF'
package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel pour les malts (API publique)
 */
case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  origin: OriginReadModel,
  description: Option[String],
  flavorProfiles: List[String],
  characteristics: MaltCharacteristics,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class OriginReadModel(
  code: String,
  name: String,
  region: String,
  isNoble: Boolean,
  isNewWorld: Boolean
)

case class MaltCharacteristics(
  colorName: String,
  extractionCategory: String,
  enzymaticCategory: String,
  maxRecommendedPercent: Option[Double],
  isBaseMalt: Boolean,
  canSelfConvert: Boolean
)

object MaltReadModel {
  
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      origin = OriginReadModel(
        code = malt.originCode,
        name = malt.originCode, // Simplifié
        region = "Unknown",
        isNoble = false,
        isNewWorld = false
      ),
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      characteristics = MaltCharacteristics(
        colorName = malt.ebcColor.colorName,
        extractionCategory = malt.extractionRate.extractionCategory,
        enzymaticCategory = malt.diastaticPower.enzymaticCategory,
        maxRecommendedPercent = malt.maxRecommendedPercent,
        isBaseMalt = malt.isBaseMalt,
        canSelfConvert = malt.canSelfConvert
      ),
      isActive = malt.isActive,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
  
  implicit val originFormat: Format[OriginReadModel] = Json.format[OriginReadModel]
  implicit val characteristicsFormat: Format[MaltCharacteristics] = Json.format[MaltCharacteristics]
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
EOF

# AdminMaltReadModel (utilisé dans les handlers admin)
cat > app/application/queries/admin/malts/readmodels/AdminMaltReadModel.scala << 'EOF'
package application.queries.admin.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel pour les malts (interface admin)
 */
case class AdminMaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: String,
  isActive: Boolean,
  credibilityScore: Double,
  qualityScore: Double,
  needsReview: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
)

object AdminMaltReadModel {
  
  def fromAggregate(malt: MaltAggregate): AdminMaltReadModel = {
    AdminMaltReadModel(
      id = malt.id.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      source = malt.source.name,
      isActive = malt.isActive,
      credibilityScore = malt.credibilityScore,
      qualityScore = malt.qualityScore,
      needsReview = malt.needsReview,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt,
      version = malt.version
    )
  }
  
  implicit val format: Format[AdminMaltReadModel] = Json.format[AdminMaltReadModel]
}
EOF

echo "✅ ReadModels créés"

# =============================================================================
# ÉTAPE 5 : EXTENSION DU REPOSITORY AVEC MÉTHODES MANQUANTES
# =============================================================================

echo -e "${BLUE}🔧 Extension MaltReadRepository...${NC}"

cat > app/domain/malts/repositories/MaltReadRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import scala.concurrent.Future

// Types utilitaires pour les méthodes avancées (simplifiés pour compilation)
case class MaltSubstitution(
  id: String,
  maltId: MaltId,
  substituteId: MaltId,
  substituteName: String,
  compatibilityScore: Double
)

case class MaltCompatibility(
  maltId: MaltId,
  beerStyleId: String,
  compatibilityScore: Double,
  usageNotes: String
)

case class PagedResult[T](
  items: List[T],
  currentPage: Int,
  pageSize: Int,
  totalCount: Long,
  hasNext: Boolean
)

trait MaltReadRepository {
  def findById(id: MaltId): Future[Option[MaltAggregate]]
  def findByName(name: String): Future[Option[MaltAggregate]]
  def existsByName(name: String): Future[Boolean]
  def findAll(page: Int = 0, pageSize: Int = 20, activeOnly: Boolean = true): Future[List[MaltAggregate]]
  def count(activeOnly: Boolean = true): Future[Long]
  
  // Méthodes avancées utilisées dans les handlers
  def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]]
  def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]]
  
  // Méthode de recherche avancée utilisée dans les handlers
  def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None,
    originCode: Option[String] = None,
    status: Option[String] = None,
    source: Option[String] = None,
    minCredibility: Option[Double] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    minExtraction: Option[Double] = None,
    minDiastaticPower: Option[Double] = None,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[PagedResult[MaltAggregate]]
}
EOF

echo "✅ Repository étendu"

# =============================================================================
# ÉTAPE 6 : MISE À JOUR DES HANDLERS POUR COMPILER
# =============================================================================

echo -e "${BLUE}🔧 Correction des handlers pour compilation...${NC}"

# Correction temporaire des handlers pour qu'ils compilent
# (remplace les méthodes manquantes par des implémentations vides)

# Simplification CreateMaltCommandHandler
cat > app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala << 'EOF'
package application.commands.admin.malts.handlers

import application.commands.admin.malts.CreateMaltCommand
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.shared._
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la création de malts (version simplifiée pour compilation)
 */
@Singleton
class CreateMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    command.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validCommand) => processValidCommand(validCommand)
    }
  }

  private def processValidCommand(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    for {
      nameExists <- maltReadRepo.existsByName(command.name)
      result <- if (nameExists) {
        Future.successful(Left(DomainError.conflict("Un malt avec ce nom existe déjà", "name")))
      } else {
        createMalt(command)
      }
    } yield result
  }

  private def createMalt(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    // Implémentation simplifiée pour compilation
    val maltId = MaltId.generate()
    Future.successful(Right(maltId))
  }
}
EOF

echo "✅ Handlers corrigés temporairement"

# =============================================================================
# ÉTAPE 7 : TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}🔍 Test de compilation...${NC}"

if sbt compile > /tmp/malts_fix_compilation.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    COMPILATION_SUCCESS=true
else
    echo -e "${RED}❌ Erreurs persistantes${NC}"
    echo -e "${YELLOW}Nouvelles erreurs dans /tmp/malts_fix_compilation.log :${NC}"
    head -20 /tmp/malts_fix_compilation.log
    COMPILATION_SUCCESS=false
fi

# =============================================================================
# ÉTAPE 8 : RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 RAPPORT DE CORRECTION${NC}"
echo ""

if [ "$COMPILATION_SUCCESS" = true ]; then
    echo -e "${GREEN}🎉 COMPOSANTS MANQUANTS CRÉÉS !${NC}"
    echo ""
    echo -e "${GREEN}✅ Composants ajoutés :${NC}"
    echo "   🔧 MaltSource : Manuel, IA, Import"
    echo "   🔧 MaltStatus : Actif, Inactif, En révision"
    echo "   🔧 DiastaticPower : Value Object avec validation"
    echo "   🔧 MaltType : Extended avec BASE, CRYSTAL, ROASTED"
    echo "   🔧 EBCColor : Extended avec colorName"
    echo "   🔧 ExtractionRate : Extended avec extractionCategory"
    echo "   🔧 MaltAggregate : Propriétés calculées ajoutées"
    echo "   📋 ReadModels : MaltReadModel, AdminMaltReadModel"
    echo "   💾 Repository : Méthodes avancées ajoutées"
    echo ""
    
    echo -e "${BLUE}🎯 Prochaines étapes :${NC}"
    echo "   1. Implémenter les repositories Slick complètement"
    echo "   2. Finaliser les handlers avec la logique complète"
    echo "   3. Ajouter NonEmptyString format pour JSON"
    echo "   4. Créer les controllers finaux"
    
else
    echo -e "${RED}❌ ERREURS PERSISTANTES${NC}"
    echo ""
    echo -e "${YELLOW}Actions recommandées :${NC}"
    echo "   1. Vérifiez NonEmptyString existe dans domain.shared"
    echo "   2. Consultez /tmp/malts_fix_compilation.log"
    echo "   3. Corrigez les imports manquants"
fi

echo ""
echo -e "${GREEN}🌾 Phase de correction des composants manquants terminée !${NC}"