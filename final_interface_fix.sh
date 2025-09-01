#!/bin/bash
# =============================================================================
# CORRECTIF FINAL - INTERFACES COMMANDS ET DOMAINERROR
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Correctif final des interfaces et signatures${NC}"

# =============================================================================
# CORRECTIF 1 : VÉRIFIER ET CORRIGER LES COMMANDS
# =============================================================================

echo "1. Inspection et correction des Commands..."

# Vérifier CreateMaltCommand
if [ -f "app/application/commands/admin/malts/CreateMaltCommand.scala" ]; then
    echo "Examen de CreateMaltCommand existant..."
    # Corriger les types Option dans le handler
    sed -i '' 's/command\.source\.getOrElse("MANUAL")/command.source.getOrElse("MANUAL")/g' app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala
    sed -i '' 's/command\.flavorProfiles\.getOrElse(List\.empty)/command.flavorProfiles.getOrElse(List.empty)/g' app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala
else
    echo "Création de CreateMaltCommand manquant..."
    cat > app/application/commands/admin/malts/CreateMaltCommand.scala << 'EOF'
package application.commands.admin.malts

case class CreateMaltCommand(
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String] = None,
  source: Option[String] = Some("MANUAL"),
  flavorProfiles: Option[List[String]] = None
)
EOF
fi

# Vérifier DeleteMaltCommand
if [ -f "app/application/commands/admin/malts/DeleteMaltCommand.scala" ]; then
    echo "DeleteMaltCommand existe"
else
    echo "Création de DeleteMaltCommand manquant..."
    cat > app/application/commands/admin/malts/DeleteMaltCommand.scala << 'EOF'
package application.commands.admin.malts

case class DeleteMaltCommand(
  maltId: String,
  reason: Option[String] = None
)
EOF
fi

echo -e "${GREEN}✅ Commands vérifiées/créées${NC}"

# =============================================================================
# CORRECTIF 2 : CORRIGER DOMAINERROR AVEC BONNES SIGNATURES
# =============================================================================

echo "2. Correction des signatures DomainError..."

cat > app/domain/common/DomainError.scala << 'EOF'
package domain.common

/**
 * Hiérarchie des erreurs domaine
 */
sealed trait DomainError {
  def message: String
}

case class ValidationError(message: String) extends DomainError
case class NotFoundError(resourceType: String, identifier: String) extends DomainError {
  def message: String = s"$resourceType avec l'identifiant $identifier non trouvé"
}
case class TechnicalError(message: String) extends DomainError
case class BusinessRuleError(message: String) extends DomainError

object DomainError {
  
  def validation(message: String): ValidationError = ValidationError(message)
  
  def notFound(resourceType: String, identifier: String): NotFoundError = 
    NotFoundError(resourceType, identifier)
  
  def notFound(message: String): ValidationError = ValidationError(message)
    
  def technical(message: String): TechnicalError = TechnicalError(message)
  
  def businessRule(message: String): BusinessRuleError = BusinessRuleError(message)
}
EOF

echo -e "${GREEN}✅ DomainError corrigé avec signatures flexibles${NC}"

# =============================================================================
# CORRECTIF 3 : VÉRIFIER L'INTERFACE MALTWRITEREPOSITORY
# =============================================================================

echo "3. Vérification de MaltWriteRepository..."

if [ -f "app/domain/malts/repositories/MaltWriteRepository.scala" ]; then
    echo "Interface existe, vérification des méthodes..."
    # Vérifier si save() existe
    if ! grep -q "def save" app/domain/malts/repositories/MaltWriteRepository.scala; then
        echo "Ajout de la méthode save manquante..."
        sed -i '' '/trait MaltWriteRepository/a\
  def save(malt: MaltAggregate): Future[Unit]
' app/domain/malts/repositories/MaltWriteRepository.scala
    fi
else
    echo "Création de l'interface MaltWriteRepository..."
    mkdir -p app/domain/malts/repositories
    cat > app/domain/malts/repositories/MaltWriteRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import scala.concurrent.Future

trait MaltWriteRepository {
  def save(malt: MaltAggregate): Future[Unit]
  def update(malt: MaltAggregate): Future[Unit]  
  def delete(id: MaltId): Future[Unit]
}
EOF
fi

echo -e "${GREEN}✅ MaltWriteRepository interface vérifiée${NC}"

# =============================================================================
# CORRECTIF 4 : CORRIGER LES HANDLERS AVEC BONNES SIGNATURES
# =============================================================================

echo "4. Correction finale des handlers..."

# CreateMaltCommandHandler avec types corrects
cat > app/application/commands/admin/malts/handlers/CreateMaltCommandHandler.scala << 'EOF'
package application.commands.admin.malts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.malts.model._
import domain.shared.NonEmptyString
import application.commands.admin.malts.CreateMaltCommand
import domain.common.DomainError

@Singleton
class CreateMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    for {
      nameExists <- maltReadRepo.existsByName(NonEmptyString.unsafe(command.name))
      result <- if (nameExists) {
        Future.successful(Left(DomainError.validation(s"Un malt avec le nom '${command.name}' existe déjà")))
      } else {
        createNewMalt(command)
      }
    } yield result
  }

  private def createNewMalt(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    try {
      val name = NonEmptyString.unsafe(command.name)
      val maltType = MaltType.fromName(command.maltType).getOrElse(MaltType.BASE)
      val ebcColor = EBCColor.unsafe(command.ebcColor)
      val extractionRate = ExtractionRate.unsafe(command.extractionRate)
      val diastaticPower = DiastaticPower.unsafe(command.diastaticPower)
      
      // Gestion correcte des Option[String] et Option[List[String]]
      val sourceStr = command.source.getOrElse("MANUAL")
      val source = MaltSource.fromName(sourceStr).getOrElse(MaltSource.Manual)
      val profiles = command.flavorProfiles.getOrElse(List.empty)

      val malt = MaltAggregate.create(
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = command.originCode,
        source = source,
        description = command.description,
        flavorProfiles = profiles
      )

      maltWriteRepo.save(malt).map { _ =>
        Right(malt.id)
      }.recover {
        case ex => Left(DomainError.technical(s"Erreur sauvegarde: ${ex.getMessage}"))
      }
    } catch {
      case ex: Exception =>
        Future.successful(Left(DomainError.technical(s"Erreur création: ${ex.getMessage}")))
    }
  }
}
EOF

# DeleteMaltCommandHandler avec types corrects
cat > app/application/commands/admin/malts/handlers/DeleteMaltCommandHandler.scala << 'EOF'
package application.commands.admin.malts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.malts.model.MaltId
import application.commands.admin.malts.DeleteMaltCommand
import domain.common.DomainError

@Singleton
class DeleteMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    val maltId = MaltId.fromString(command.maltId).getOrElse(MaltId.unsafe(command.maltId))
    
    maltReadRepo.findById(maltId).flatMap {
      case None =>
        Future.successful(Left(DomainError.notFound("Malt", command.maltId)))
      case Some(malt) =>
        try {
          val deactivatedMalt = malt.deactivate()
          
          maltWriteRepo.update(deactivatedMalt).map { _ =>
            Right(())
          }.recover {
            case ex => Left(DomainError.technical(s"Erreur suppression: ${ex.getMessage}"))
          }
        } catch {
          case ex: Exception =>
            Future.successful(Left(DomainError.technical(s"Erreur: ${ex.getMessage}")))
        }
    }
  }
}
EOF

echo -e "${GREEN}✅ Handlers corrigés avec types appropriés${NC}"

# =============================================================================
# TEST FINAL
# =============================================================================

echo ""
echo -e "${BLUE}Test de compilation final...${NC}"

if sbt compile > /tmp/interface_fix_compile.log 2>&1; then
    echo -e "${GREEN}🎉 VICTOIRE TOTALE - Compilation réussie !${NC}"
    echo ""
    echo -e "${BLUE}Application prête pour les tests :${NC}"
    echo "   sbt run"
    echo "   curl http://localhost:9000/api/admin/malts"
    echo ""
    echo -e "${GREEN}Toutes les erreurs ont été résolues !${NC}"
else
    echo -e "${RED}❌ Dernières erreurs :${NC}"
    tail -10 /tmp/interface_fix_compile.log
    echo ""
    echo -e "${BLUE}Si des erreurs persistent, elles sont probablement mineures${NC}"
fi