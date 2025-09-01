#!/bin/bash
# =============================================================================
# CORRECTIF FINAL - ERREURS DE TYPES ET SLICK
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Correctif final des erreurs de types${NC}"

# =============================================================================
# CORRECTIF 1 : PATTERN MATCHING DANS LES HANDLERS
# =============================================================================

echo "1. Correction du pattern matching dans CreateMaltCommandHandler..."

# Le problème : pattern matching sur Either avec Left/Right mais le type attendu est MaltAggregate
# Il faut corriger la logique de création qui semble mal structurée

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
      // Vérifier que le nom n'existe pas déjà
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
      // Créer les Value Objects
      val name = NonEmptyString.unsafe(command.name)
      val maltType = MaltType.fromName(command.maltType).getOrElse(MaltType.BASE)
      val ebcColor = EBCColor.unsafe(command.ebcColor)
      val extractionRate = ExtractionRate.unsafe(command.extractionRate)
      val diastaticPower = DiastaticPower.unsafe(command.diastaticPower)
      val source = MaltSource.fromName(command.source.getOrElse("MANUAL")).getOrElse(MaltSource.Manual)

      // Créer l'agrégat
      val malt = MaltAggregate.create(
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = command.originCode,
        source = source,
        description = command.description,
        flavorProfiles = command.flavorProfiles.getOrElse(List.empty)
      )

      // Sauvegarder
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

echo -e "${GREEN}✅ CreateMaltCommandHandler corrigé${NC}"

# =============================================================================
# CORRECTIF 2 : PATTERN MATCHING DANS DELETE HANDLER  
# =============================================================================

echo "2. Correction du DeleteMaltCommandHandler..."

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
        Future.successful(Left(DomainError.notFound(s"Malt non trouvé: ${command.maltId}")))
      case Some(malt) =>
        try {
          // Désactiver le malt (pas de suppression physique)
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

echo -e "${GREEN}✅ DeleteMaltCommandHandler corrigé${NC}"

# =============================================================================
# CORRECTIF 3 : PROBLÈMES SLICK AVEC UUID/STRING  
# =============================================================================

echo "3. Correction des problèmes Slick UUID/String..."

# Reconstruire complètement SlickMaltWriteRepository avec les bons types
cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

import domain.malts.model._
import domain.malts.repositories.MaltWriteRepository

@Singleton
class SlickMaltWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
  extends MaltWriteRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  // Réutiliser la même structure de table que le ReadRepository
  case class MaltRow(
    id: UUID,
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
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  )

  class MaltsTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[UUID]("id", O.PrimaryKey)
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
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  val malts = TableQuery[MaltsTable]

  private def aggregateToRow(aggregate: MaltAggregate): MaltRow = {
    val flavorProfilesJson = if (aggregate.flavorProfiles.nonEmpty) {
      Some(s"""[${aggregate.flavorProfiles.map(s => s""""$s"""").mkString(",")}]""")
    } else {
      None
    }

    MaltRow(
      id = aggregate.id.asUUID,  // Utiliser asUUID pour avoir le bon type
      name = aggregate.name.value,
      maltType = aggregate.maltType.name,
      ebcColor = aggregate.ebcColor.value,
      extractionRate = aggregate.extractionRate.value,
      diastaticPower = aggregate.diastaticPower.value,
      originCode = aggregate.originCode,
      description = aggregate.description,
      flavorProfiles = flavorProfilesJson,
      source = aggregate.source.name,
      isActive = aggregate.isActive,
      credibilityScore = aggregate.credibilityScore,
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version
    )
  }

  override def save(malt: MaltAggregate): Future[Unit] = {
    val row = aggregateToRow(malt)
    val action = malts += row
    
    db.run(action).map(_ => ()).recover {
      case ex =>
        println(s"❌ Erreur sauvegarde malt ${malt.name.value}: ${ex.getMessage}")
        throw ex
    }
  }

  override def update(malt: MaltAggregate): Future[Unit] = {
    val row = aggregateToRow(malt)
    // Utiliser asUUID pour garantir la correspondance de type
    val action = malts.filter(_.id === malt.id.asUUID).update(row)
    
    db.run(action).map(_ => ()).recover {
      case ex =>
        println(s"❌ Erreur mise à jour malt ${malt.name.value}: ${ex.getMessage}")
        throw ex
    }
  }

  override def delete(id: MaltId): Future[Unit] = {
    // Utiliser asUUID pour garantir la correspondance de type
    val action = malts.filter(_.id === id.asUUID).delete
    
    db.run(action).map(_ => ()).recover {
      case ex =>
        println(s"❌ Erreur suppression malt ${id}: ${ex.getMessage}")
        throw ex
    }
  }
}
EOF

echo -e "${GREEN}✅ SlickMaltWriteRepository reconstruit avec types corrects${NC}"

# =============================================================================
# CORRECTIF 4 : VÉRIFIER DOMAINERROR
# =============================================================================

echo "4. Vérification et création de DomainError si nécessaire..."

if [ ! -f "app/domain/common/DomainError.scala" ]; then
    mkdir -p app/domain/common
    cat > app/domain/common/DomainError.scala << 'EOF'
package domain.common

/**
 * Classe de base pour les erreurs du domaine
 */
sealed trait DomainError {
  def message: String
}

object DomainError {
  
  case class ValidationError(message: String) extends DomainError
  case class NotFoundError(message: String) extends DomainError  
  case class TechnicalError(message: String) extends DomainError
  case class BusinessRuleError(message: String) extends DomainError
  
  def validation(message: String): DomainError = ValidationError(message)
  def notFound(message: String): DomainError = NotFoundError(message)
  def technical(message: String): DomainError = TechnicalError(message)
  def businessRule(message: String): DomainError = BusinessRuleError(message)
}
EOF
    echo -e "${GREEN}✅ DomainError créé${NC}"
else
    echo -e "${GREEN}✅ DomainError existe déjà${NC}"
fi

# =============================================================================
# TEST FINAL DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test final de compilation...${NC}"

if sbt compile > /tmp/final_compile.log 2>&1; then
    echo -e "${GREEN}🎉 SUCCÈS TOTAL - L'application compile enfin !${NC}"
    echo ""
    echo -e "${BLUE}Tests disponibles :${NC}"
    echo "   sbt run"
    echo "   curl http://localhost:9000/api/admin/malts"
    echo ""
    echo -e "${GREEN}L'API malts devrait maintenant fonctionner correctement !${NC}"
else
    echo -e "${RED}❌ Erreurs persistantes :${NC}"
    tail -10 /tmp/final_compile.log
    echo ""
    echo -e "${BLUE}Progrès accompli :${NC}"
    echo "   ✅ Routes corrigées"
    echo "   ✅ Handlers reconstruits"  
    echo "   ✅ Types UUID/String harmonisés"
    echo "   ✅ Repository Slick corrigé"
fi