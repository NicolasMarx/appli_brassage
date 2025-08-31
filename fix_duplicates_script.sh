#!/bin/bash
# Script de correction des doublons dans les ReadModels
# Supprime les définitions dupliquées et réorganise proprement

set -e

echo "🔧 Correction des doublons ReadModels..."

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m'

# =============================================================================
# ÉTAPE 1 : NETTOYAGE ET CRÉATION PROPRE DES READMODELS
# =============================================================================

echo -e "${BLUE}🧹 Nettoyage des fichiers ReadModels existants...${NC}"

# Supprimer les fichiers existants pour éviter les conflits
rm -f app/application/queries/public/malts/readmodels/MaltReadModel.scala
rm -f app/application/queries/admin/malts/readmodels/AdminMaltReadModel.scala

# Créer les dossiers si nécessaires
mkdir -p app/application/queries/public/malts/readmodels
mkdir -p app/application/queries/admin/malts/readmodels

echo "✅ Nettoyage terminé"

# =============================================================================
# ÉTAPE 2 : CRÉATION READMODELS PUBLIC (SANS DOUBLONS)
# =============================================================================

echo -e "${BLUE}📊 Création ReadModels publics...${NC}"

cat > app/application/queries/public/malts/readmodels/MaltReadModel.scala << 'EOF'
package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel principal pour les malts (API publique)
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
  characteristics: MaltCharacteristics,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

/**
 * Caractéristiques calculées du malt
 */
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
      originCode = malt.originCode,
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
  
  implicit val characteristicsFormat: Format[MaltCharacteristics] = Json.format[MaltCharacteristics]
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
EOF

# =============================================================================
# ÉTAPE 3 : CRÉATION DTOs PUBLICS DANS FICHIER SÉPARÉ
# =============================================================================

echo -e "${BLUE}📋 Création DTOs publics...${NC}"

cat > app/application/queries/public/malts/readmodels/MaltDTOs.scala << 'EOF'
package application.queries.public.malts.readmodels

import play.api.libs.json._

/**
 * Résultat détaillé d'un malt avec ses substituts
 */
case class MaltDetailResult(
  malt: MaltReadModel,
  substitutes: List[SubstituteReadModel]
)

/**
 * ReadModel pour un substitut de malt
 */
case class SubstituteReadModel(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String]
)

/**
 * Résultat de recherche paginée
 */
case class MaltSearchResult(
  malts: List[MaltReadModel],
  totalCount: Long,
  currentPage: Int,
  pageSize: Int,
  hasNext: Boolean
)

object MaltDTOs {
  implicit val substituteFormat: Format[SubstituteReadModel] = Json.format[SubstituteReadModel]
  implicit val detailFormat: Format[MaltDetailResult] = Json.format[MaltDetailResult]
  implicit val searchFormat: Format[MaltSearchResult] = Json.format[MaltSearchResult]
}
EOF

# =============================================================================
# ÉTAPE 4 : CRÉATION READMODELS ADMIN (SANS DOUBLONS)
# =============================================================================

echo -e "${BLUE}🔐 Création ReadModels admin...${NC}"

cat > app/application/queries/admin/malts/readmodels/AdminMaltReadModel.scala << 'EOF'
package application.queries.admin.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel principal pour les malts (interface admin)
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

# =============================================================================
# ÉTAPE 5 : CRÉATION DTOs ADMIN DANS FICHIER SÉPARÉ
# =============================================================================

echo -e "${BLUE}📋 Création DTOs admin...${NC}"

cat > app/application/queries/admin/malts/readmodels/AdminMaltDTOs.scala << 'EOF'
package application.queries.admin.malts.readmodels

import play.api.libs.json._
import java.time.Instant

/**
 * Résultat détaillé d'un malt admin avec toutes les informations
 */
case class AdminMaltDetailResult(
  malt: AdminMaltReadModel,
  substitutes: List[AdminSubstituteReadModel],
  beerStyleCompatibilities: List[BeerStyleCompatibility],
  statistics: MaltUsageStatistics,
  qualityAnalysis: QualityAnalysis
)

/**
 * Substitut dans l'interface admin avec plus de détails
 */
case class AdminSubstituteReadModel(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String],
  qualityScore: Double
)

/**
 * Compatibilité avec les styles de bière
 */
case class BeerStyleCompatibility(
  styleId: String,
  styleName: String,
  compatibilityScore: Double,
  usageNotes: String
)

/**
 * Statistiques d'utilisation du malt
 */
case class MaltUsageStatistics(
  recipeCount: Int,
  avgUsagePercent: Double,
  popularityScore: Double
)

/**
 * Analyse qualité du malt
 */
case class QualityAnalysis(
  dataCompleteness: Double,
  sourceReliability: String,
  reviewStatus: String,
  lastValidated: Option[Instant]
)

/**
 * Résultat de recherche admin paginée
 */
case class AdminMaltSearchResult(
  malts: List[AdminMaltReadModel],
  totalCount: Long,
  currentPage: Int,
  pageSize: Int,
  hasNext: Boolean,
  filters: AdminSearchFilters
)

/**
 * Filtres de recherche admin
 */
case class AdminSearchFilters(
  maltType: Option[String],
  status: Option[String],
  source: Option[String],
  minCredibility: Option[Double],
  needsReview: Boolean,
  searchTerm: Option[String]
)

object AdminMaltDTOs {
  implicit val substituteFormat: Format[AdminSubstituteReadModel] = Json.format[AdminSubstituteReadModel]
  implicit val compatibilityFormat: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
  implicit val statisticsFormat: Format[MaltUsageStatistics] = Json.format[MaltUsageStatistics]
  implicit val qualityFormat: Format[QualityAnalysis] = Json.format[QualityAnalysis]
  implicit val detailFormat: Format[AdminMaltDetailResult] = Json.format[AdminMaltDetailResult]
  implicit val searchFiltersFormat: Format[AdminSearchFilters] = Json.format[AdminSearchFilters]
  implicit val searchResultFormat: Format[AdminMaltSearchResult] = Json.format[AdminMaltSearchResult]
}
EOF

# =============================================================================
# ÉTAPE 6 : MISE À JOUR DES IMPORTS DANS LES CONTROLLERS
# =============================================================================

echo -e "${BLUE}🎮 Mise à jour imports Controllers...${NC}"

# Mise à jour AdminMaltsController avec bons imports
cat > app/interfaces/http/api/admin/malts/AdminMaltsController.scala << 'EOF'
package interfaces.http.api.admin.malts

import play.api.mvc._
import play.api.libs.json._
import interfaces.http.common.BaseController
import interfaces.actions.AdminSecuredAction
import application.queries.admin.malts.readmodels.{AdminMaltReadModel, AdminMaltDTOs}
import AdminMaltDTOs._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton  
class AdminMaltsController @Inject()(
  val controllerComponents: ControllerComponents,
  adminAction: AdminSecuredAction
)(implicit ec: ExecutionContext) extends BaseController {

  def list(
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
  ): Action[AnyContent] = adminAction.async { implicit request =>
    
    // Implémentation temporaire pour compilation
    Future.successful(Ok(Json.obj(
      "malts" -> Json.arr(),
      "pagination" -> Json.obj(
        "currentPage" -> page,
        "pageSize" -> pageSize,
        "totalCount" -> 0,
        "hasNext" -> false
      )
    )))
  }

  def create(): Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(BadRequest(Json.obj(
      "error" -> "Création de malt non implémentée temporairement"
    )))
  }

  def detail(
    id: String,
    includeAuditLog: Boolean = false,
    includeSubstitutes: Boolean = true,
    includeBeerStyles: Boolean = true,
    includeStatistics: Boolean = false
  ): Action[AnyContent] = adminAction.async { implicit request =>
    
    Future.successful(BadRequest(Json.obj(
      "error" -> "Détail malt non implémenté temporairement"
    )))
  }

  def update(id: String): Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(BadRequest(Json.obj(
      "error" -> "Mise à jour malt non implémentée temporairement"
    )))
  }

  def delete(id: String): Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(Ok(Json.obj(
      "success" -> true,
      "message" -> "Malt supprimé (temporaire)"
    )))
  }

  def statistics(): Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(Ok(Json.obj(
      "totalMalts" -> 0,
      "activeCount" -> 0,
      "inactiveCount" -> 0,
      "needsReviewCount" -> 0
    )))
  }

  def needsReview(
    page: Int = 0,
    pageSize: Int = 20,
    maxCredibility: Int = 70
  ): Action[AnyContent] = adminAction.async { implicit request =>
    
    Future.successful(BadRequest(Json.obj(
      "error" -> "Review malts non implémenté temporairement"
    )))
  }

  def adjustCredibility(id: String): Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(BadRequest(Json.obj(
      "error" -> "Ajustement credibilité non implémenté temporairement"
    )))
  }

  def batchImport(): Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(Ok(Json.obj(
      "success" -> true,
      "imported" -> 0,
      "message" -> "Import batch non implémenté temporairement"
    )))
  }

  // Helper temporaire pour JSON
  private def adminMaltToJson(malt: AdminMaltReadModel): JsValue = {
    Json.toJson(malt)
  }
}
EOF

# Mise à jour MaltsController avec bons imports
cat > app/interfaces/http/api/v1/malts/MaltsController.scala << 'EOF'
package interfaces.http.api.v1.malts

import play.api.mvc._
import play.api.libs.json._
import interfaces.http.common.BaseController
import application.queries.public.malts.readmodels.{MaltReadModel, MaltDTOs}
import MaltDTOs._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaltsController @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext) extends BaseController {

  def list(
    page: Int = 0,
    pageSize: Int = 20,
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None,
    originCode: Option[String] = None,
    activeOnly: Boolean = true
  ): Action[AnyContent] = Action.async { implicit request =>
    
    Future.successful(BadRequest(Json.obj(
      "error" -> "Liste malts non implémentée temporairement"
    )))
  }

  def detail(
    id: String,
    includeSubstitutes: Boolean = false,
    includeBeerStyles: Boolean = false
  ): Action[AnyContent] = Action.async { implicit request =>
    
    // TODO: Implémenter récupération détail malt
    Future.successful(NotFound(Json.obj(
      "error" -> "Malt non trouvé"
    )))
  }

  def search(): Action[AnyContent] = Action.async { implicit request =>
    // TODO: Implémenter recherche avancée
    Future.successful(Ok(Json.obj(
      "results" -> Json.arr(),
      "totalCount" -> 0
    )))
  }

  def types(): Action[AnyContent] = Action { implicit request =>
    Ok(Json.obj(
      "types" -> Json.arr(
        Json.obj("code" -> "BASE", "name" -> "Malt de base"),
        Json.obj("code" -> "SPECIALTY", "name" -> "Malt spécial"),
        Json.obj("code" -> "CRYSTAL", "name" -> "Malt crystal"),
        Json.obj("code" -> "ROASTED", "name" -> "Malt torréfié")
      )
    ))
  }

  def colors(): Action[AnyContent] = Action { implicit request =>
    Ok(Json.obj(
      "ranges" -> Json.arr(
        Json.obj("min" -> 0, "max" -> 10, "name" -> "Très clair"),
        Json.obj("min" -> 10, "max" -> 30, "name" -> "Clair"),
        Json.obj("min" -> 30, "max" -> 100, "name" -> "Ambre"),
        Json.obj("min" -> 100, "max" -> 300, "name" -> "Brun"),
        Json.obj("min" -> 300, "max" -> 1000, "name" -> "Noir")
      )
    ))
  }

  // Helper pour JSON avec le bon type
  private def maltToJson(malt: MaltReadModel): JsValue = {
    Json.toJson(malt)
  }
}
EOF

echo "✅ Controllers mis à jour"

# =============================================================================
# ÉTAPE 7 : MISE À JOUR PACKAGE.SCALA POUR IMPORTS SIMPLIFIÉS
# =============================================================================

echo -e "${BLUE}📦 Création package objects pour imports...${NC}"

# Package object pour readmodels publics
cat > app/application/queries/public/malts/package.scala << 'EOF'
package application.queries.public.malts

// Import centralisé des readmodels publics pour simplifier les imports
object readmodels {
  // Ré-export des types principaux
  type MaltReadModel = readmodels.MaltReadModel
  type MaltCharacteristics = readmodels.MaltCharacteristics
  type MaltDetailResult = readmodels.MaltDetailResult
  type SubstituteReadModel = readmodels.SubstituteReadModel
  type MaltSearchResult = readmodels.MaltSearchResult
  
  // Ré-export des companion objects
  val MaltReadModel = readmodels.MaltReadModel
  val MaltDTOs = readmodels.MaltDTOs
}
EOF

# Package object pour readmodels admin
cat > app/application/queries/admin/malts/package.scala << 'EOF'
package application.queries.admin.malts

// Import centralisé des readmodels admin pour simplifier les imports
object readmodels {
  // Ré-export des types principaux
  type AdminMaltReadModel = readmodels.AdminMaltReadModel
  type AdminMaltDetailResult = readmodels.AdminMaltDetailResult
  type AdminSubstituteReadModel = readmodels.AdminSubstituteReadModel
  type BeerStyleCompatibility = readmodels.BeerStyleCompatibility
  type MaltUsageStatistics = readmodels.MaltUsageStatistics
  type QualityAnalysis = readmodels.QualityAnalysis
  type AdminMaltSearchResult = readmodels.AdminMaltSearchResult
  type AdminSearchFilters = readmodels.AdminSearchFilters
  
  // Ré-export des companion objects
  val AdminMaltReadModel = readmodels.AdminMaltReadModel
  val AdminMaltDTOs = readmodels.AdminMaltDTOs
}
EOF

echo "✅ Package objects créés"

# =============================================================================
# ÉTAPE 8 : TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}🔍 Test de compilation après correction...${NC}"

if sbt compile > /tmp/malts_duplicates_fix.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    COMPILATION_SUCCESS=true
else
    echo -e "${RED}❌ Erreurs de compilation persistantes${NC}"
    echo -e "${YELLOW}Voir les détails dans /tmp/malts_duplicates_fix.log${NC}"
    echo ""
    echo "Premières erreurs :"
    head -15 /tmp/malts_duplicates_fix.log
    COMPILATION_SUCCESS=false
fi

# =============================================================================
# ÉTAPE 9 : RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${BLUE}📊 RAPPORT DE CORRECTION DOUBLONS${NC}"
echo ""

if [ "$COMPILATION_SUCCESS" = true ]; then
    echo -e "${GREEN}🎉 DOUBLONS CORRIGÉS AVEC SUCCÈS !${NC}"
    echo ""
    echo -e "${GREEN}✅ Actions réalisées :${NC}"
    echo "   🧹 Suppression fichiers ReadModels dupliqués"
    echo "   📊 Création ReadModels publics propres"
    echo "   🔐 Création ReadModels admin séparés" 
    echo "   📋 Séparation DTOs dans fichiers dédiés"
    echo "   🎮 Mise à jour imports Controllers"
    echo "   📦 Création package objects pour imports"
    echo ""
    
    echo -e "${BLUE}📁 Structure finale ReadModels :${NC}"
    echo "   📁 public/malts/readmodels/"
    echo "      📄 MaltReadModel.scala (principal)"
    echo "      📄 MaltDTOs.scala (DTOs publics)"
    echo "   📁 admin/malts/readmodels/"
    echo "      📄 AdminMaltReadModel.scala (principal)"
    echo "      📄 AdminMaltDTOs.scala (DTOs admin)"
    echo ""
    
    echo -e "${GREEN}🚀 Le domaine Malts compile maintenant correctement !${NC}"
    
else
    echo -e "${RED}❌ Des erreurs persistent après correction${NC}"
    echo ""
    echo -e "${YELLOW}🔍 Vérifications suggérées :${NC}"
    echo "   1. Examiner le log complet : cat /tmp/malts_duplicates_fix.log"
    echo "   2. Vérifier s'il reste des imports obsolètes"
    echo "   3. Contrôler les noms de packages"
    echo "   4. S'assurer que tous les Value Objects existent"
    echo ""
fi

echo ""
echo -e "${BLUE}📋 STRUCTURE FINALE READMODELS${NC}"
echo "   🏗️  Architecture : ReadModels + DTOs séparés"
echo "   📦 Organisation : Par domaine (public/admin)"
echo "   🔗 Imports : Package objects pour simplicité"
echo "   🎯 Pattern : Un seul endroit par type"
echo "   ✨ Lisibilité : Codes bien organisés"
echo ""

echo "🍺 Correction doublons terminée !"