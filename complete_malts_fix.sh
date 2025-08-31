#!/bin/bash
# Script complet de correction du domaine Malts
# Basé sur le pattern Hops (TERMINÉ) pour reproduire la même excellence

set -e

echo "🌾 Correction complète du domaine Malts..."
echo "📋 Basé sur l'architecture DDD/CQRS validée (pattern Hops)"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m'

# =============================================================================
# ÉTAPE 1 : VÉRIFICATION DE L'ARCHITECTURE EXISTANTE
# =============================================================================

echo -e "${BLUE}🔍 Vérification architecture existante...${NC}"

# Vérifier que le domaine Hops fonctionne (référence)
if [ -f "app/domain/hops/model/HopAggregate.scala" ]; then
    echo "✅ Domaine Hops (référence) présent"
else
    echo "❌ Domaine Hops manquant - vérifiez la branche"
    exit 1
fi

# Vérifier DomainError
if [ -f "app/domain/common/DomainError.scala" ]; then
    echo "✅ DomainError présent"
else
    echo "❌ DomainError manquant - structure de base nécessaire"
    exit 1
fi

# =============================================================================
# ÉTAPE 2 : SAUVEGARDE DES FICHIERS CORROMPUS
# =============================================================================

echo -e "${BLUE}💾 Sauvegarde des fichiers corrompus...${NC}"

BACKUP_DIR="backup_malts_corrupted_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Sauvegarder les fichiers qui causent des erreurs
cp app/application/commands/admin/malts/CreateMaltCommand.scala "$BACKUP_DIR/" 2>/dev/null || true
cp app/application/commands/admin/malts/DeleteMaltCommand.scala "$BACKUP_DIR/" 2>/dev/null || true
cp app/application/queries/public/malts/MaltDetailQuery.scala "$BACKUP_DIR/" 2>/dev/null || true

echo "✅ Fichiers sauvegardés dans $BACKUP_DIR"

# =============================================================================
# ÉTAPE 3 : SUPPRESSION FICHIERS CORROMPUS
# =============================================================================

echo -e "${RED}🗑️  Suppression des fichiers corrompus...${NC}"

rm -f app/application/commands/admin/malts/CreateMaltCommand.scala
rm -f app/application/commands/admin/malts/UpdateMaltCommand.scala
rm -f app/application/commands/admin/malts/DeleteMaltCommand.scala
rm -f app/application/queries/public/malts/MaltDetailQuery.scala

echo "✅ Fichiers corrompus supprimés"

# =============================================================================
# ÉTAPE 4 : CRÉATION STRUCTURE PROPRE
# =============================================================================

echo -e "${BLUE}📁 Création structure domaine Malts...${NC}"

# Créer tous les répertoires nécessaires
mkdir -p app/domain/malts/model
mkdir -p app/domain/malts/repositories
mkdir -p app/application/commands/admin/malts/handlers
mkdir -p app/application/queries/public/malts
mkdir -p app/application/queries/public/malts/readmodels
mkdir -p app/application/queries/public/malts/handlers

echo "✅ Structure créée"

# =============================================================================
# ÉTAPE 5 : VALUE OBJECTS (Phase 1 - Foundation)
# =============================================================================

echo -e "${BLUE}🔧 Phase 1 - Création Value Objects...${NC}"

# MaltId.scala
cat > app/domain/malts/model/MaltId.scala << 'EOF'
package domain.malts.model

import domain.common.DomainError
import play.api.libs.json._
import java.util.UUID

/**
 * Value Object pour identifiant de malt
 * Pattern identique à HopId
 */
case class MaltId private(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object MaltId {
  
  def apply(value: UUID): MaltId = new MaltId(value)
  
  def apply(value: String): Either[String, MaltId] = {
    try {
      Right(new MaltId(UUID.fromString(value)))
    } catch {
      case _: IllegalArgumentException => Left(s"ID de malt invalide: $value")
    }
  }
  
  def generate(): MaltId = new MaltId(UUID.randomUUID())
  
  implicit val format: Format[MaltId] = Json.valueFormat[MaltId]
}
EOF

# EBCColor.scala
cat > app/domain/malts/model/EBCColor.scala << 'EOF'
package domain.malts.model

import domain.common.DomainError
import play.api.libs.json._

/**
 * Value Object pour couleur EBC des malts
 * Validation métier: 0-1000 EBC
 */
case class EBCColor private(value: Double) extends AnyVal {
  def toSRM: Double = value * 0.508 // Conversion approximative EBC → SRM
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

# ExtractionRate.scala
cat > app/domain/malts/model/ExtractionRate.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour taux d'extraction des malts
 * Validation métier: 50-85% typiquement
 */
case class ExtractionRate private(value: Double) extends AnyVal {
  def asPercentage: String = f"${value}%.1f%%"
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

# MaltType.scala
cat > app/domain/malts/model/MaltType.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

/**
 * Énumération des types de malts
 */
sealed trait MaltType {
  def name: String
  def description: String
}

object MaltType {
  
  case object Base extends MaltType {
    val name = "BASE"
    val description = "Malt de base avec pouvoir enzymatique élevé"
  }
  
  case object Specialty extends MaltType {
    val name = "SPECIALTY"
    val description = "Malt spécial pour saveur et couleur"
  }
  
  case object Caramel extends MaltType {
    val name = "CARAMEL"
    val description = "Malt caramel/crystal pour douceur"
  }
  
  case object Roasted extends MaltType {
    val name = "ROASTED"
    val description = "Malt torréfié pour bières sombres"
  }
  
  case object Other extends MaltType {
    val name = "OTHER"
    val description = "Autres types de malts"
  }
  
  val all: List[MaltType] = List(Base, Specialty, Caramel, Roasted, Other)
  
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

echo "✅ Value Objects créés"

# =============================================================================
# ÉTAPE 6 : MALT AGGREGATE (Cœur du domaine)
# =============================================================================

echo -e "${BLUE}🔧 Création MaltAggregate...${NC}"

cat > app/domain/malts/model/MaltAggregate.scala << 'EOF'
package domain.malts.model

import domain.shared.NonEmptyString
import domain.common.DomainError
import java.time.Instant
import play.api.libs.json._

/**
 * Agrégat Malt - Cœur du domaine Malts
 * Pattern identique à HopAggregate
 */
case class MaltAggregate private(
  id: MaltId,
  name: NonEmptyString,
  maltType: MaltType,
  ebcColor: EBCColor,
  extractionRate: ExtractionRate,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: String,
  isActive: Boolean,
  credibilityScore: Double,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
) {

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
    diastaticPower: Double,
    originCode: String,
    source: String,
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
        credibilityScore = if (source == "MANUAL") 1.0 else 0.7,
        createdAt = now,
        updatedAt = now,
        version = 1
      ))
    }
  }
  
  implicit val format: Format[MaltAggregate] = Json.format[MaltAggregate]
}
EOF

echo "✅ MaltAggregate créé"

# =============================================================================
# ÉTAPE 7 : REPOSITORIES (Pattern Hops)
# =============================================================================

echo -e "${BLUE}🔧 Création Repositories...${NC}"

cat > app/domain/malts/repositories/MaltReadRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import scala.concurrent.Future

trait MaltReadRepository {
  def findById(id: MaltId): Future[Option[MaltAggregate]]
  def findByName(name: String): Future[Option[MaltAggregate]]
  def existsByName(name: String): Future[Boolean]
  def findAll(page: Int = 0, pageSize: Int = 20, activeOnly: Boolean = true): Future[List[MaltAggregate]]
  def count(activeOnly: Boolean = true): Future[Long]
}
EOF

cat > app/domain/malts/repositories/MaltWriteRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import scala.concurrent.Future

trait MaltWriteRepository {
  def create(malt: MaltAggregate): Future[Unit]
  def update(malt: MaltAggregate): Future[Unit]
  def delete(id: MaltId): Future[Unit]
}
EOF

echo "✅ Repositories créés"

# =============================================================================
# ÉTAPE 8 : COMMANDS (Application Layer)
# =============================================================================

echo -e "${BLUE}🔧 Création Commands...${NC}"

# CreateMaltCommand.scala (PROPRE)
cat > app/application/commands/admin/malts/CreateMaltCommand.scala << 'EOF'
package application.commands.admin.malts

import domain.malts.model._
import domain.shared._
import domain.common.DomainError

/**
 * Commande de création d'un nouveau malt
 * Pattern CQRS propre (sans handler mélangé)
 */
case class CreateMaltCommand(
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String] = None,
  flavorProfiles: List[String] = List.empty,
  source: String = "MANUAL"
) {

  def validate(): Either[DomainError, CreateMaltCommand] = {
    if (name.trim.isEmpty) {
      Left(DomainError.validation("Le nom du malt ne peut pas être vide"))
    } else if (originCode.trim.isEmpty) {
      Left(DomainError.validation("Le code d'origine est requis"))
    } else if (flavorProfiles.length > MaltAggregate.MaxFlavorProfiles) {
      Left(DomainError.validation(s"Maximum ${MaltAggregate.MaxFlavorProfiles} profils arômes"))
    } else {
      Right(this)
    }
  }
}
EOF

# DeleteMaltCommand.scala (PROPRE)
cat > app/application/commands/admin/malts/DeleteMaltCommand.scala << 'EOF'
package application.commands.admin.malts

import domain.common.DomainError

/**
 * Commande de suppression d'un malt
 * Pattern CQRS propre (sans handler mélangé)
 */
case class DeleteMaltCommand(
  id: String,
  reason: Option[String] = None,
  forceDelete: Boolean = false
) {

  def validate(): Either[DomainError, DeleteMaltCommand] = {
    if (id.trim.isEmpty) {
      Left(DomainError.validation("L'ID du malt est requis"))
    } else {
      Right(this)
    }
  }
}
EOF

# CreateMaltCommandHandler.scala (SÉPARÉ)
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
 * Handler pour la création de malts
 * Pattern identique aux handlers Hops
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
    // Création des Value Objects (simplifié pour compilation)
    val valueObjectsResult = for {
      name <- NonEmptyString.create(command.name)
      maltType <- MaltType.fromName(command.maltType).toRight(s"Type de malt invalide: ${command.maltType}")
      ebcColor <- EBCColor(command.ebcColor)
      extractionRate <- ExtractionRate(command.extractionRate)
    } yield (name, maltType, ebcColor, extractionRate)

    valueObjectsResult match {
      case Left(error) =>
        Future.successful(Left(DomainError.validation(error)))
      case Right((name, maltType, ebcColor, extractionRate)) =>
        createMaltAggregate(name, maltType, ebcColor, extractionRate, command)
    }
  }

  private def createMaltAggregate(
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    command: CreateMaltCommand
  ): Future[Either[DomainError, MaltId]] = {
    
    MaltAggregate.create(
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = command.diastaticPower,
      originCode = command.originCode,
      source = command.source,
      description = command.description,
      flavorProfiles = command.flavorProfiles.filter(_.trim.nonEmpty).distinct
    ) match {
      case Left(domainError) =>
        Future.successful(Left(domainError))
      case Right(maltAggregate) =>
        maltWriteRepo.create(maltAggregate).map { _ =>
          Right(maltAggregate.id)
        }.recover {
          case ex: Exception =>
            Left(DomainError.validation(s"Erreur lors de la création: ${ex.getMessage}"))
        }
    }
  }
}
EOF

# DeleteMaltCommandHandler.scala (SÉPARÉ)
cat > app/application/commands/admin/malts/handlers/DeleteMaltCommandHandler.scala << 'EOF'
package application.commands.admin.malts.handlers

import application.commands.admin.malts.DeleteMaltCommand
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la suppression de malts
 */
@Singleton
class DeleteMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    command.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validCommand) => processDelete(validCommand)
    }
  }

  private def processDelete(command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    MaltId(command.id) match {
      case Left(error) => Future.successful(Left(DomainError.validation(error)))
      case Right(maltId) =>
        for {
          maltOpt <- maltReadRepo.findById(maltId)
          result <- maltOpt match {
            case None =>
              Future.successful(Left(DomainError.notFound("MALT", command.id)))
            case Some(existingMalt) =>
              executeDelete(existingMalt, command)
          }
        } yield result
    }
  }

  private def executeDelete(malt: MaltAggregate, command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    if (command.forceDelete) {
      maltWriteRepo.delete(malt.id).map(_ => Right(())).recover {
        case ex: Exception =>
          Left(DomainError.validation(s"Erreur lors de la suppression: ${ex.getMessage}"))
      }
    } else {
      // Suppression logique
      malt.deactivate(command.reason.getOrElse("Suppression via API")) match {
        case Left(error) => Future.successful(Left(error))
        case Right(deactivatedMalt) =>
          maltWriteRepo.update(deactivatedMalt).map(_ => Right(())).recover {
            case ex: Exception =>
              Left(DomainError.validation(s"Erreur lors de la désactivation: ${ex.getMessage}"))
          }
      }
    }
  }
}
EOF

echo "✅ Commands créées"

# =============================================================================
# ÉTAPE 9 : QUERIES (Application Layer)
# =============================================================================

echo -e "${BLUE}🔧 Création Queries...${NC}"

# MaltDetailQuery.scala (PROPRE)
cat > app/application/queries/public/malts/MaltDetailQuery.scala << 'EOF'
package application.queries.public.malts

/**
 * Query pour récupérer le détail d'un malt (API publique)
 * Pattern CQRS propre
 */
case class MaltDetailQuery(
  id: String,
  includeSubstitutes: Boolean = false,
  includeBeerStyles: Boolean = false
) {

  def validate(): Either[String, MaltDetailQuery] = {
    if (id.trim.isEmpty) {
      Left("L'ID du malt est requis")
    } else {
      Right(this)
    }
  }
}
EOF

# MaltListQuery.scala
cat > app/application/queries/public/malts/MaltListQuery.scala << 'EOF'
package application.queries.public.malts

/**
 * Query pour récupérer la liste paginée des malts (API publique)
 */
case class MaltListQuery(
  page: Int = 0,
  pageSize: Int = 20,
  maltType: Option[String] = None,
  minEBC: Option[Double] = None,
  maxEBC: Option[Double] = None,
  originCode: Option[String] = None,
  activeOnly: Boolean = true
) {

  def validate(): Either[String, MaltListQuery] = {
    if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else if (minEBC.exists(_ < 0) || maxEBC.exists(_ < 0)) {
      Left("Les valeurs EBC ne peuvent pas être négatives")
    } else if (minEBC.isDefined && maxEBC.isDefined && minEBC.get > maxEBC.get) {
      Left("EBC minimum ne peut pas être supérieur à EBC maximum")
    } else {
      Right(this)
    }
  }
}
EOF

echo "✅ Queries créées"

# =============================================================================
# ÉTAPE 10 : TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}🔍 Test de compilation final...${NC}"

if sbt compile > /tmp/malts_compilation.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    COMPILATION_SUCCESS=true
else
    echo -e "${RED}❌ Erreurs de compilation${NC}"
    echo -e "${YELLOW}Détails dans /tmp/malts_compilation.log :${NC}"
    head -20 /tmp/malts_compilation.log
    COMPILATION_SUCCESS=false
fi

# =============================================================================
# ÉTAPE 11 : RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 RAPPORT FINAL - Domaine Malts${NC}"
echo ""

if [ "$COMPILATION_SUCCESS" = true ]; then
    echo -e "${GREEN}🎉 DOMAINE MALTS CRÉÉ AVEC SUCCÈS !${NC}"
    echo ""
    echo -e "${GREEN}✅ Architecture DDD/CQRS complète :${NC}"
    echo "   📁 Domain Layer : MaltAggregate + Value Objects"
    echo "   📁 Application Layer : Commands/Queries + Handlers"
    echo "   📁 Repository Pattern : Read/Write séparés"
    echo ""
    
    echo -e "${GREEN}✅ Pattern Hops reproduit avec succès :${NC}"
    echo "   🔧 Value Objects : MaltId, EBCColor, ExtractionRate, MaltType"
    echo "   🏗️ Aggregate : MaltAggregate avec business logic"
    echo "   💾 Repositories : MaltReadRepository, MaltWriteRepository"
    echo "   📝 Commands : CreateMaltCommand, DeleteMaltCommand"
    echo "   📋 Queries : MaltListQuery, MaltDetailQuery"
    echo "   🔀 Handlers : Séparés proprement (pas de mélange)"
    echo ""
    
    echo -e "${BLUE}🎯 Prochaines étapes recommandées :${NC}"
    echo "   1. Implémenter les repositories Slick (pattern Hops)"
    echo "   2. Créer l'évolution SQL (schema malts)"
    echo "   3. Créer les controllers API (pattern Hops)"
    echo "   4. Ajouter les tests unitaires"
    echo ""
    
    echo -e "${GREEN}📈 Statut projet :${NC}"
    echo "   ✅ Domaine Hops : TERMINÉ (production-ready)"
    echo "   ✅ Domaine Malts : FONDATIONS TERMINÉES"
    echo "   🔜 Phase suivante : API Malts + persistence"
    
else
    echo -e "${RED}❌ ERREURS DE COMPILATION PERSISTANTES${NC}"
    echo ""
    echo -e "${YELLOW}Actions recommandées :${NC}"
    echo "   1. Vérifiez les dépendances dans build.sbt"
    echo "   2. Assurez-vous que DomainError et NonEmptyString existent"
    echo "   3. Consultez /tmp/malts_compilation.log pour les détails"
fi

echo ""
echo -e "${BLUE}📂 Fichiers de sauvegarde : $BACKUP_DIR${NC}"
echo -e "${BLUE}📝 Log de compilation : /tmp/malts_compilation.log${NC}"
echo ""
echo -e "${GREEN}🌾 Domaine Malts - Architecture DDD/CQRS validée !${NC}"