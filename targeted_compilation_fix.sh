#!/bin/bash

# =============================================================================
# SCRIPT DE CORRECTION CIBLÉ - ERREURS DE COMPILATION DOMAINE MALTS
# =============================================================================
# Basé sur l'analyse méthodique des erreurs existantes
# Crée UNIQUEMENT les composants manquants identifiés dans les erreurs
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🔧 =============================================================================="
echo "   CORRECTION CIBLÉE DES ERREURS DE COMPILATION - DOMAINE MALTS"
echo "   Crée uniquement les composants manquants identifiés dans l'analyse"
echo "==============================================================================${NC}"

echo -e "${CYAN}📋 PROBLÈMES IDENTIFIÉS ET À CORRIGER :${NC}"
echo "  ❌ MaltReadModel manquant"
echo "  ❌ PagedResult manquant"
echo "  ❌ BaseController et AdminSecuredAction manquants"
echo "  ❌ Types Slick (MaltRow, Origin) mal définis"
echo "  ❌ Imports manquants"
echo ""

# =============================================================================
# 1. CRÉATION DES READMODELS MANQUANTS
# =============================================================================

echo -e "${GREEN}📦 1. Création des ReadModels manquants${NC}"

# Créer les dossiers si nécessaire
mkdir -p app/application/queries/public/malts/readmodels
mkdir -p app/application/queries/admin/malts/readmodels

# MaltReadModel - COMPOSANT PRINCIPAL MANQUANT
cat > app/application/queries/public/malts/readmodels/MaltReadModel.scala << 'EOF'
package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel pour les malts - API publique
 * Utilisé par MaltSearchQueryHandler et autres
 */
case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  // Propriétés calculées attendues par MaltsController
  colorName: String,
  extractionCategory: String,
  enzymaticCategory: String,
  maxRecommendedPercent: Option[Double],
  isBaseMalt: Boolean,
  canSelfConvert: Boolean,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
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
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      // Propriétés calculées depuis l'aggregate
      colorName = malt.ebcColor.colorName,
      extractionCategory = malt.extractionRate.extractionCategory,
      enzymaticCategory = malt.diastaticPower.enzymaticCategory,
      maxRecommendedPercent = malt.maxRecommendedPercent,
      isBaseMalt = malt.isBaseMalt,
      canSelfConvert = malt.canSelfConvert,
      isActive = malt.isActive,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
  
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
EOF

# AdminMaltReadModel - Pour l'interface admin
cat > app/application/queries/admin/malts/readmodels/AdminMaltReadModel.scala << 'EOF'
package application.queries.admin.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel pour les malts - Interface admin
 * Inclut les informations de credibilité et gestion
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
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt,
      version = malt.version
    )
  }
  
  implicit val format: Format[AdminMaltReadModel] = Json.format[AdminMaltReadModel]
}
EOF

# AdminMaltDetailResult - Attendu par AdminMaltsController
cat > app/application/queries/admin/malts/readmodels/AdminMaltDetailResult.scala << 'EOF'
package application.queries.admin.malts.readmodels

import play.api.libs.json._

/**
 * Résultat détaillé pour l'admin avec substituts et compatibilités
 */
case class AdminMaltDetailResult(
  malt: AdminMaltReadModel,
  substitutes: List[AdminSubstituteReadModel] = List.empty,
  compatibilities: List[BeerStyleCompatibility] = List.empty,
  statistics: Option[MaltUsageStatistics] = None,
  qualityAnalysis: Option[QualityAnalysis] = None
)

case class AdminSubstituteReadModel(
  id: String,
  name: String,
  compatibilityScore: Double,
  notes: String
)

case class BeerStyleCompatibility(
  styleId: String,
  styleName: String,
  compatibilityScore: Double,
  usageNotes: String
)

case class MaltUsageStatistics(
  totalUsage: Long,
  popularityScore: Double,
  averageUsagePercentage: Double,
  topBeerStyles: List[String]
)

case class QualityAnalysis(
  overallScore: Double,
  consistencyRating: String,
  dataCompleteness: Double,
  recommendedActions: List[String]
)

object AdminMaltDetailResult {
  implicit val substituteFormat: Format[AdminSubstituteReadModel] = Json.format[AdminSubstituteReadModel]
  implicit val compatibilityFormat: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
  implicit val statisticsFormat: Format[MaltUsageStatistics] = Json.format[MaltUsageStatistics]
  implicit val qualityFormat: Format[QualityAnalysis] = Json.format[QualityAnalysis]
  implicit val format: Format[AdminMaltDetailResult] = Json.format[AdminMaltDetailResult]
}
EOF

# MaltDetailResult - Pour l'API publique
mkdir -p app/application/queries/public/malts/readmodels
cat > app/application/queries/public/malts/readmodels/MaltDetailResult.scala << 'EOF'
package application.queries.public.malts.readmodels

import play.api.libs.json._

/**
 * Résultat détaillé pour l'API publique
 */
case class MaltDetailResult(
  malt: MaltReadModel,
  substitutes: List[SubstituteReadModel] = List.empty,
  compatibleBeerStyles: List[String] = List.empty
)

case class SubstituteReadModel(
  id: String,
  name: String,
  compatibilityScore: Double
)

object MaltDetailResult {
  implicit val substituteFormat: Format[SubstituteReadModel] = Json.format[SubstituteReadModel]
  implicit val format: Format[MaltDetailResult] = Json.format[MaltDetailResult]
}
EOF

echo "✅ ReadModels créés"

# =============================================================================
# 2. CRÉATION DU TYPE PAGEDRESULT MANQUANT
# =============================================================================

echo -e "${GREEN}📦 2. Création du type PagedResult${NC}"

# PagedResult - Manquant dans domain.malts.repositories
mkdir -p app/domain/malts/repositories
cat > app/domain/malts/repositories/PagedResult.scala << 'EOF'
package domain.malts.repositories

/**
 * Type PagedResult utilisé par MaltSearchQueryHandler
 * Résultat paginé générique
 */
case class PagedResult[T](
  items: List[T],
  currentPage: Int,
  pageSize: Int,
  totalCount: Long,
  hasNext: Boolean
)
EOF

echo "✅ PagedResult créé"

# =============================================================================
# 3. CRÉATION DES COMPOSANTS HTTP MANQUANTS
# =============================================================================

echo -e "${GREEN}📦 3. Création des composants HTTP manquants${NC}"

# BaseController - Manquant dans interfaces.http.common
mkdir -p app/interfaces/http/common
cat > app/interfaces/http/common/BaseController.scala << 'EOF'
package interfaces.http.common

import play.api.mvc._
import play.api.libs.json._
import domain.common.DomainError

/**
 * Contrôleur de base avec méthodes communes
 * Utilisé par MaltsController et AdminMaltsController
 */
abstract class BaseController(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Gestion standardisée des erreurs de domaine
   */
  protected def handleDomainError(error: DomainError): Result = error match {
    case DomainError.NotFound(message, _) => 
      NotFound(Json.obj("error" -> "not_found", "message" -> message))
    case DomainError.BusinessRule(message, code) => 
      BadRequest(Json.obj("error" -> "business_rule", "code" -> code, "message" -> message))
    case DomainError.Validation(message, field) => 
      BadRequest(Json.obj("error" -> "validation", "field" -> field, "message" -> message))
    case _ => 
      InternalServerError(Json.obj("error" -> "internal_error", "message" -> "Une erreur interne s'est produite"))
  }

  /**
   * Réponse de succès standardisée
   */
  protected def successResponse[T](data: T)(implicit writes: Writes[T]): Result = {
    Ok(Json.obj("success" -> true, "data" -> data))
  }

  /**
   * Réponse d'erreur standardisée
   */
  protected def errorResponse(message: String, errorType: String = "error"): Result = {
    BadRequest(Json.obj("success" -> false, "error" -> errorType, "message" -> message))
  }
}
EOF

# AdminSecuredAction - Manquant dans interfaces.actions
mkdir -p app/interfaces/actions
cat > app/interfaces/actions/AdminSecuredAction.scala << 'EOF'
package interfaces.actions

import domain.admin.model.AdminAggregate
import domain.admin.repositories.AdminReadRepository
import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}

/**
 * Requête sécurisée avec administrateur authentifié
 */
case class AdminRequest[A](admin: AdminAggregate, request: Request[A]) extends WrappedRequest[A](request)

/**
 * Action sécurisée pour les administrateurs
 * Utilisée par AdminMaltsController
 */
@Singleton
class AdminSecuredAction @Inject()(
  parser: BodyParsers.Default,
  adminRepo: AdminReadRepository
)(implicit ec: ExecutionContext) extends ActionBuilder[AdminRequest, AnyContent] {

  override def parser: BodyParser[AnyContent] = parser.default
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: AdminRequest[A] => Future[Result]): Future[Result] = {
    extractAdminFromSession(request).flatMap {
      case Some(admin) if admin.isActive =>
        block(AdminRequest(admin, request))
      case Some(_) =>
        Future.successful(Results.Forbidden(Json.obj("error" -> "account_disabled")))
      case None =>
        Future.successful(Results.Unauthorized(Json.obj("error" -> "authentication_required")))
    }
  }

  private def extractAdminFromSession[A](request: Request[A]): Future[Option[AdminAggregate]] = {
    request.session.get("admin_id") match {
      case Some(adminId) => 
        // Simplification pour compilation - à améliorer avec le vrai repository
        Future.successful(None)
      case None => 
        Future.successful(None)
    }
  }
}
EOF

echo "✅ Composants HTTP créés"

# =============================================================================
# 4. CORRECTION DES IMPORTS DANS SLICK TABLES
# =============================================================================

echo -e "${GREEN}📦 4. Correction des types Slick${NC}"

# Ajout du type Origin manquant dans MaltTables
# On le définit directement dans le fichier pour éviter les dépendances complexes
cat > app/infrastructure/persistence/slick/tables/MaltTablesAdditions.scala << 'EOF'
package infrastructure.persistence.slick.tables

/**
 * Types additionnels pour corriger les erreurs de compilation MaltTables
 */

// Type Origin manquant dans MaltTables.scala
case class Origin(
  code: String,
  name: String,
  region: Option[String] = None
)

// MaltRow pour SlickMaltReadRepository
case class MaltRow(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: Option[List[String]],
  source: String,
  isActive: Boolean,
  credibilityScore: Double,
  createdAt: java.time.Instant,
  updatedAt: java.time.Instant,
  version: Long
)
EOF

echo "✅ Types Slick ajoutés"

# =============================================================================
# 5. CORRECTION DES HANDLERS EXISTANTS
# =============================================================================

echo -e "${GREEN}📦 5. Mise à jour des imports dans les handlers${NC}"

# Créer un fichier de package object pour faciliter les imports
cat > app/application/queries/public/malts/package.scala << 'EOF'
package application.queries.public

/**
 * Package object pour faciliter les imports des malts
 */
package object malts {
  // Import des ReadModels
  import readmodels._
  
  // Types d'exports pour éviter les imports complexes
  type MaltReadModel = readmodels.MaltReadModel
  type MaltDetailResult = readmodels.MaltDetailResult
}
EOF

# =============================================================================
# 6. AJOUT DES IMPORTS MANQUANTS EN DOMAIN
# =============================================================================

echo -e "${GREEN}📦 6. Ajout des imports de base du domaine${NC}"

# Ajout de UpdateMaltCommandHandler manquant
mkdir -p app/application/commands/admin/malts/handlers
cat > app/application/commands/admin/malts/handlers/UpdateMaltCommandHandler.scala << 'EOF'
package application.commands.admin.malts.handlers

import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour UpdateMaltCommand - Implémentation basique pour compilation
 */
@Singleton
class UpdateMaltCommandHandler @Inject()(
  // repositories seront injectés plus tard
)(implicit ec: ExecutionContext) {

  // Implémentation temporaire pour permettre la compilation
  def handle(command: Any): Future[Either[DomainError, String]] = {
    Future.successful(Right("updated-malt-id"))
  }
}
EOF

echo "✅ Handlers de base ajoutés"

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 =============================================================================="
echo "   RAPPORT DE CORRECTION TERMINÉ"
echo "==============================================================================${NC}"

echo -e "${GREEN}✅ COMPOSANTS CRÉÉS :${NC}"
echo "   📦 MaltReadModel et AdminMaltReadModel"
echo "   📦 MaltDetailResult et AdminMaltDetailResult"
echo "   📦 PagedResult dans domain.malts.repositories"
echo "   📦 BaseController dans interfaces.http.common"
echo "   📦 AdminSecuredAction dans interfaces.actions"
echo "   📦 Types Slick (MaltRow, Origin)"
echo "   📦 Package objects pour faciliter les imports"
echo "   📦 UpdateMaltCommandHandler basique"

echo ""
echo -e "${CYAN}🎯 PROCHAINES ÉTAPES :${NC}"
echo "   1. Tester la compilation : ${YELLOW}sbt compile${NC}"
echo "   2. Corriger les imports restants si nécessaire"
echo "   3. Implémenter la logique métier dans les handlers"
echo "   4. Ajouter les tests unitaires"

echo ""
echo -e "${GREEN}🚀 La plupart des erreurs de compilation devraient être résolues !${NC}"
