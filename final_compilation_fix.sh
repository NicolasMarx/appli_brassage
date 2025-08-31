#!/bin/bash
# Script de correction finale des erreurs de compilation Malts
# Corrige les erreurs spécifiques identifiées dans les logs

set -e

echo "🔧 Correction finale des erreurs de compilation..."

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m'

# =============================================================================
# ÉTAPE 1 : CORRECTION CREATEMALCOMMANDHANDLER
# =============================================================================

echo -e "${BLUE}🔧 Correction CreateMaltCommandHandler...${NC}"

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
 * Handler pour la création de malts (version corrigée)
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
    // Création des Value Objects avec conversion de types
    val valueObjectsResult = for {
      name <- NonEmptyString.create(command.name)
      maltType <- MaltType.fromName(command.maltType).toRight(s"Type de malt invalide: ${command.maltType}")
      ebcColor <- EBCColor(command.ebcColor)
      extractionRate <- ExtractionRate(command.extractionRate)
      diastaticPower <- DiastaticPower(command.diastaticPower) // Conversion Double -> DiastaticPower
      source <- MaltSource.fromName(command.source).toRight(s"Source invalide: ${command.source}") // Conversion String -> MaltSource
    } yield (name, maltType, ebcColor, extractionRate, diastaticPower, source)

    valueObjectsResult match {
      case Left(error) =>
        Future.successful(Left(DomainError.validation(error)))
      case Right((name, maltType, ebcColor, extractionRate, diastaticPower, source)) =>
        createMaltAggregate(name, maltType, ebcColor, extractionRate, diastaticPower, source, command)
    }
  }

  private def createMaltAggregate(
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    source: MaltSource,
    command: CreateMaltCommand
  ): Future[Either[DomainError, MaltId]] = {
    
    MaltAggregate.create(
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      originCode = command.originCode,
      source = source,
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

# =============================================================================
# ÉTAPE 2 : CORRECTION MALTCREDIBILITY
# =============================================================================

echo -e "${BLUE}🔧 Correction MaltCredibility...${NC}"

cat > app/domain/malts/model/MaltCredibility.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour la crédibilité des données de malts
 */
case class MaltCredibility private(value: Double) extends AnyVal {
  
  def isHighQuality: Boolean = value >= 0.8
  def isMediumQuality: Boolean = value >= 0.6 && value < 0.8
  def isLowQuality: Boolean = value < 0.6
  
  def category: String = {
    if (isHighQuality) "HIGH"
    else if (isMediumQuality) "MEDIUM" 
    else "LOW"
  }
}

object MaltCredibility {
  
  val MAX_CREDIBILITY = 1.0
  val MIN_CREDIBILITY = 0.0
  
  def apply(value: Double): Either[String, MaltCredibility] = {
    if (value < MIN_CREDIBILITY || value > MAX_CREDIBILITY) {
      Left(s"La crédibilité doit être entre $MIN_CREDIBILITY et $MAX_CREDIBILITY")
    } else {
      Right(new MaltCredibility(value))
    }
  }
  
  def unsafe(value: Double): MaltCredibility = new MaltCredibility(value)
  
  def fromSource(source: MaltSource): MaltCredibility = {
    val defaultValue = source match {
      case MaltSource.Manual => 1.0
      case MaltSource.AI_Discovery => 0.7
      case MaltSource.Import => 0.8
    }
    new MaltCredibility(defaultValue)
  }
  
  implicit val format: Format[MaltCredibility] = Json.valueFormat[MaltCredibility]
}
EOF

# =============================================================================
# ÉTAPE 3 : EXTENSION MALTSOURCE AVEC DEFAULTCREDIBILITY
# =============================================================================

echo -e "${BLUE}🔧 Extension MaltSource...${NC}"

cat > app/domain/malts/model/MaltSource.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._

sealed trait MaltSource {
  def name: String
  def defaultCredibility: Double
}

object MaltSource {
  case object Manual extends MaltSource { 
    val name = "MANUAL"
    val defaultCredibility = 1.0
  }
  case object AI_Discovery extends MaltSource { 
    val name = "AI_DISCOVERY"
    val defaultCredibility = 0.7
  }
  case object Import extends MaltSource { 
    val name = "IMPORT"
    val defaultCredibility = 0.8
  }

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

# =============================================================================
# ÉTAPE 4 : DÉPLACEMENT DE PAGEDRESULT DANS DOMAIN COMMON
# =============================================================================

echo -e "${BLUE}🔧 Création PagedResult dans domain.common...${NC}"

mkdir -p app/domain/common

cat > app/domain/common/PagedResult.scala << 'EOF'
package domain.common

import play.api.libs.json._

/**
 * Wrapper générique pour les résultats paginés
 */
case class PagedResult[T](
  items: List[T],
  currentPage: Int,
  pageSize: Int,
  totalCount: Long,
  hasNext: Boolean
) {
  val totalPages: Int = Math.ceil(totalCount.toDouble / pageSize).toInt
  val hasPrevious: Boolean = currentPage > 0
}

object PagedResult {
  
  def empty[T]: PagedResult[T] = PagedResult(
    items = List.empty,
    currentPage = 0,
    pageSize = 20,
    totalCount = 0,
    hasNext = false
  )
  
  def single[T](item: T): PagedResult[T] = PagedResult(
    items = List(item),
    currentPage = 0,
    pageSize = 1,
    totalCount = 1,
    hasNext = false
  )
  
  implicit def format[T: Format]: Format[PagedResult[T]] = Json.format[PagedResult[T]]
}
EOF

# =============================================================================
# ÉTAPE 5 : MISE À JOUR DU REPOSITORY AVEC IMPORT CORRECT
# =============================================================================

echo -e "${BLUE}🔧 Mise à jour MaltReadRepository...${NC}"

cat > app/domain/malts/repositories/MaltReadRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import domain.common.PagedResult
import scala.concurrent.Future

// Types utilitaires simplifiés pour les méthodes avancées
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

# =============================================================================
# ÉTAPE 6 : CORRECTION DES IMPORTS DANS LES HANDLERS
# =============================================================================

echo -e "${BLUE}🔧 Correction des imports dans les handlers...${NC}"

# Correction AdminMaltListQueryHandler
cat > app/application/queries/admin/malts/handlers/AdminMaltListQueryHandler.scala << 'EOF'
package application.queries.admin.malts.handlers

import application.queries.admin.malts.{AdminMaltListQuery}
import application.queries.admin.malts.readmodels.AdminMaltReadModel
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.{MaltType, MaltStatus, MaltSource}
import domain.common.PagedResult
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la liste admin des malts (version simplifiée)
 */
@Singleton
class AdminMaltListQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: AdminMaltListQuery): Future[Either[String, PagedResult[AdminMaltReadModel]]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeQuery(validQuery)
    }
  }

  private def executeQuery(query: AdminMaltListQuery): Future[Either[String, PagedResult[AdminMaltReadModel]]] = {
    // Pour l'instant, implémentation simplifiée qui utilise findAll
    maltReadRepo.findAll(query.page, query.pageSize, activeOnly = false).map { malts =>
      val adminReadModels = malts.map(AdminMaltReadModel.fromAggregate)
      Right(PagedResult(
        items = adminReadModels,
        currentPage = query.page,
        pageSize = query.pageSize,
        totalCount = adminReadModels.length,
        hasNext = false
      ))
    }.recover {
      case ex: Exception => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}
EOF

# Correction MaltListQueryHandler
cat > app/application/queries/public/malts/handlers/MaltListQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltListQuery}
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.{MaltType, MaltStatus}
import domain.common.PagedResult
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la liste publique des malts (version simplifiée)
 */
@Singleton
class MaltListQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltListQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeQuery(validQuery)
    }
  }

  private def executeQuery(query: MaltListQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    maltReadRepo.findAll(query.page, query.pageSize, query.activeOnly).map { malts =>
      val readModels = malts.map(MaltReadModel.fromAggregate)
      Right(PagedResult(
        items = readModels,
        currentPage = query.page,
        pageSize = query.pageSize,
        totalCount = readModels.length,
        hasNext = false
      ))
    }.recover {
      case ex: Exception => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 7 : CORRECTION DES READMODELS MANQUANTS
# =============================================================================

echo -e "${BLUE}🔧 Création des ReadModels manquants...${NC}"

# SubstituteReadModel et autres types manquants
cat > app/application/queries/public/malts/readmodels/SupportTypes.scala << 'EOF'
package application.queries.public.malts.readmodels

import play.api.libs.json._

// Types de support pour les queries détaillées
case class SubstituteReadModel(
  id: String,
  name: String,
  compatibilityScore: Double,
  notes: String
)

case class BeerStyleCompatibility(
  beerStyleId: String,
  beerStyleName: String,
  compatibilityScore: Double,
  usageNotes: String
)

case class MaltUsageStatistics(
  popularityScore: Double,
  usageCount: Long,
  averageUsagePercent: Double
)

case class QualityAnalysis(
  overallScore: Int,
  completeness: Double,
  recommendations: List[String]
)

// ReadModels pour admin
case class AdminSubstituteReadModel(
  id: String,
  name: String,
  compatibilityScore: Double,
  notes: String,
  verified: Boolean
)

object SubstituteReadModel {
  implicit val format: Format[SubstituteReadModel] = Json.format[SubstituteReadModel]
}

object BeerStyleCompatibility {
  implicit val format: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
}

object MaltUsageStatistics {
  implicit val format: Format[MaltUsageStatistics] = Json.format[MaltUsageStatistics]
}

object QualityAnalysis {
  implicit val format: Format[QualityAnalysis] = Json.format[QualityAnalysis]
}

object AdminSubstituteReadModel {
  implicit val format: Format[AdminSubstituteReadModel] = Json.format[AdminSubstituteReadModel]
}
EOF

# =============================================================================
# ÉTAPE 8 : CRÉATION DES QUERIES MANQUANTES
# =============================================================================

echo -e "${BLUE}🔧 Création des queries manquantes...${NC}"

# AdminMaltListQuery
cat > app/application/queries/admin/malts/AdminMaltListQuery.scala << 'EOF'
package application.queries.admin.malts

/**
 * Query pour la liste admin des malts avec filtres avancés
 */
case class AdminMaltListQuery(
  page: Int = 0,
  pageSize: Int = 20,
  maltType: Option[String] = None,
  status: Option[String] = None,
  source: Option[String] = None,
  minCredibility: Option[Int] = None,
  needsReview: Boolean = false,
  searchTerm: Option[String] = None,
  sortBy: String = "name",
  sortOrder: String = "asc"
) {

  def validate(): Either[String, AdminMaltListQuery] = {
    if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else {
      Right(this)
    }
  }
}
EOF

# MaltSearchQuery
cat > app/application/queries/public/malts/MaltSearchQuery.scala << 'EOF'
package application.queries.public.malts

/**
 * Query pour la recherche avancée de malts (API publique)
 */
case class MaltSearchQuery(
  searchTerm: String,
  maltType: Option[String] = None,
  minEBC: Option[Double] = None,
  maxEBC: Option[Double] = None,
  minExtraction: Option[Double] = None,
  minDiastaticPower: Option[Double] = None,
  originCode: Option[String] = None,
  flavorProfiles: List[String] = List.empty,
  activeOnly: Boolean = true,
  page: Int = 0,
  pageSize: Int = 20
) {

  def validate(): Either[String, MaltSearchQuery] = {
    if (searchTerm.trim.isEmpty) {
      Left("Le terme de recherche est requis")
    } else if (page < 0) {
      Left("Le numéro de page ne peut pas être négatif")
    } else if (pageSize < 1 || pageSize > 100) {
      Left("La taille de page doit être entre 1 et 100")
    } else {
      Right(this)
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 9 : TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}🔍 Test de compilation final...${NC}"

if sbt compile > /tmp/final_malts_compilation.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    COMPILATION_SUCCESS=true
else
    echo -e "${RED}❌ Erreurs persistantes${NC}"
    echo -e "${YELLOW}Dernières erreurs dans /tmp/final_malts_compilation.log :${NC}"
    head -15 /tmp/final_malts_compilation.log
    COMPILATION_SUCCESS=false
fi

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 RAPPORT FINAL${NC}"
echo ""

if [ "$COMPILATION_SUCCESS" = true ]; then
    echo -e "${GREEN}🎉 DOMAINE MALTS COMPILÉ AVEC SUCCÈS !${NC}"
    echo ""
    echo -e "${GREEN}✅ Corrections appliquées :${NC}"
    echo "   🔧 CreateMaltCommandHandler : Types corrigés (Double->DiastaticPower, String->MaltSource)"
    echo "   🔧 MaltCredibility : Extension avec defaultCredibility"
    echo "   🔧 MaltSource : Propriété defaultCredibility ajoutée"
    echo "   📦 PagedResult : Déplacé dans domain.common"
    echo "   📋 ReadModels : Types de support créés (SubstituteReadModel, etc.)"
    echo "   📝 Queries : AdminMaltListQuery, MaltSearchQuery créées"
    echo "   🔗 Handlers : Imports corrigés, implémentations simplifiées"
    echo ""
    
    echo -e "${BLUE}🎯 Statut du projet :${NC}"
    echo "   ✅ Domaine Hops : TERMINÉ (production-ready)"
    echo "   ✅ Domaine Malts : FONDATIONS COMPILENT"
    echo "   🔜 Étape suivante : Implémentation repositories Slick"
    
    echo ""
    echo -e "${GREEN}Le domaine Malts compile maintenant ! Prêt pour les repositories.${NC}"
    
else
    echo -e "${RED}❌ ERREURS PERSISTANTES${NC}"
    echo ""
    echo -e "${YELLOW}Problèmes probables :${NC}"
    echo "   - NonEmptyString format JSON manquant"
    echo "   - Imports infrastructure manquants"
    echo "   - Dépendances build.sbt"
    echo ""
    echo -e "${YELLOW}Consultez /tmp/final_malts_compilation.log pour plus de détails${NC}"
fi

echo ""
echo -e "${BLUE}Prochaine étape : Implémentation complète des repositories Slick${NC}