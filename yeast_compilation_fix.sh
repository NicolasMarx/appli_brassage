#!/bin/bash

# =============================================================================
# CORRECTION COMPLÈTE ERREURS COMPILATION DOMAINE YEASTS
# =============================================================================
# Basé sur l'analyse des 100 erreurs de compilation
# Stratégie: Créer les types manquants référencés par YeastDTOs.scala
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTION COMPLÈTE ERREURS COMPILATION YEASTS${NC}"
echo -e "${BLUE}=================================================${NC}"

# =============================================================================
# ÉTAPE 1 : CRÉER COMMANDS MANQUANTES
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 1 : Création Commands manquantes${NC}"

# Créer les dossiers s'ils n'existent pas
mkdir -p app/application/commands/admin/yeasts/handlers

# CreateYeastCommand
cat > app/application/commands/admin/yeasts/CreateYeastCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class CreateYeastCommand(
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String],
  flavorProfile: List[String],
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String],
  createdBy: UUID
)
EOF

# UpdateYeastCommand
cat > app/application/commands/admin/yeasts/UpdateYeastCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class UpdateYeastCommand(
  yeastId: UUID,
  name: String,
  laboratory: String,
  strain: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String],
  flavorProfile: List[String],
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String],
  updatedBy: UUID
)
EOF

# DeleteYeastCommand
cat > app/application/commands/admin/yeasts/DeleteYeastCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class DeleteYeastCommand(
  yeastId: UUID,
  reason: Option[String],
  deletedBy: UUID
)
EOF

# ActivateYeastCommand
cat > app/application/commands/admin/yeasts/ActivateYeastCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class ActivateYeastCommand(
  yeastId: UUID,
  activatedBy: UUID
)
EOF

# DeactivateYeastCommand
cat > app/application/commands/admin/yeasts/DeactivateYeastCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class DeactivateYeastCommand(
  yeastId: UUID,
  reason: Option[String],
  deactivatedBy: UUID
)
EOF

# ArchiveYeastCommand
cat > app/application/commands/admin/yeasts/ArchiveYeastCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class ArchiveYeastCommand(
  yeastId: UUID,
  reason: Option[String],
  archivedBy: UUID
)
EOF

# ChangeYeastStatusCommand
cat > app/application/commands/admin/yeasts/ChangeYeastStatusCommand.scala << 'EOF'
package application.commands.admin.yeasts

import java.util.UUID

case class ChangeYeastStatusCommand(
  yeastId: UUID,
  newStatus: String,
  reason: Option[String],
  changedBy: UUID
)
EOF

# CreateYeastsBatchCommand
cat > app/application/commands/admin/yeasts/CreateYeastsBatchCommand.scala << 'EOF'
package application.commands.admin.yeasts

case class CreateYeastsBatchCommand(
  commands: List[CreateYeastCommand]
)
EOF

echo -e "${GREEN}✅ Commands créées${NC}"

# =============================================================================
# ÉTAPE 2 : CRÉER QUERIES MANQUANTES
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 2 : Création Queries manquantes${NC}"

mkdir -p app/application/queries/public/yeasts/handlers

# GetYeastByIdQuery
cat > app/application/queries/public/yeasts/GetYeastByIdQuery.scala << 'EOF'
package application.queries.public.yeasts

import java.util.UUID

case class GetYeastByIdQuery(
  yeastId: UUID
)
EOF

# FindYeastsQuery
cat > app/application/queries/public/yeasts/FindYeastsQuery.scala << 'EOF'
package application.queries.public.yeasts

case class FindYeastsQuery(
  name: Option[String],
  laboratory: Option[String],
  yeastType: Option[String],
  minAttenuation: Option[Int],
  maxAttenuation: Option[Int],
  minTemperature: Option[Int],
  maxTemperature: Option[Int],
  minAlcoholTolerance: Option[Double],
  maxAlcoholTolerance: Option[Double],
  flocculation: Option[String],
  characteristics: Option[List[String]],
  status: Option[String],
  page: Int,
  size: Int
)
EOF

# FindYeastsByTypeQuery
cat > app/application/queries/public/yeasts/FindYeastsByTypeQuery.scala << 'EOF'
package application.queries.public.yeasts

case class FindYeastsByTypeQuery(
  yeastType: String,
  limit: Option[Int]
)
EOF

# FindYeastsByLaboratoryQuery
cat > app/application/queries/public/yeasts/FindYeastsByLaboratoryQuery.scala << 'EOF'
package application.queries.public.yeasts

case class FindYeastsByLaboratoryQuery(
  laboratory: String,
  limit: Option[Int]
)
EOF

# SearchYeastsQuery
cat > app/application/queries/public/yeasts/SearchYeastsQuery.scala << 'EOF'
package application.queries.public.yeasts

case class SearchYeastsQuery(
  searchTerm: String,
  limit: Option[Int]
)
EOF

# GetYeastRecommendationsQuery
cat > app/application/queries/public/yeasts/GetYeastRecommendationsQuery.scala << 'EOF'
package application.queries.public.yeasts

case class GetYeastRecommendationsQuery(
  beerStyle: Option[String],
  targetAbv: Option[Double],
  fermentationTemp: Option[Int],
  desiredCharacteristics: Option[List[String]],
  limit: Int
)
EOF

# GetYeastAlternativesQuery
cat > app/application/queries/public/yeasts/GetYeastAlternativesQuery.scala << 'EOF'
package application.queries.public.yeasts

import java.util.UUID

case class GetYeastAlternativesQuery(
  originalYeastId: UUID,
  reason: String,
  limit: Int
)
EOF

echo -e "${GREEN}✅ Queries créées${NC}"

# =============================================================================
# ÉTAPE 3 : CRÉER HANDLERS MANQUANTS
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 3 : Création Handlers manquants${NC}"

# YeastCommandHandlers (service qui groupe tous les handlers)
cat > app/application/yeasts/handlers/YeastCommandHandlers.scala << 'EOF'
package application.yeasts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import application.commands.admin.yeasts._
import application.yeasts.dtos._

@Singleton
class YeastCommandHandlers @Inject()(
  // Les vrais handlers seraient injectés ici
)(implicit ec: ExecutionContext) {

  def handleCreateYeast(command: CreateYeastCommand): Future[Either[List[String], YeastDetailResponseDTO]] = {
    // TODO: Implémenter avec le vrai handler
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleUpdateYeast(command: UpdateYeastCommand): Future[Either[List[String], YeastDetailResponseDTO]] = {
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleDeleteYeast(command: DeleteYeastCommand): Future[Either[String, Unit]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleActivateYeast(command: ActivateYeastCommand): Future[Either[String, YeastDetailResponseDTO]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleDeactivateYeast(command: DeactivateYeastCommand): Future[Either[String, YeastDetailResponseDTO]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleArchiveYeast(command: ArchiveYeastCommand): Future[Either[String, YeastDetailResponseDTO]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleChangeStatus(command: ChangeYeastStatusCommand): Future[Either[String, Unit]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleBatchCreate(command: CreateYeastsBatchCommand): Future[Either[List[String], List[YeastDetailResponseDTO]]] = {
    Future.successful(Left(List("Not implemented yet")))
  }
}
EOF

# YeastQueryHandlers
cat > app/application/yeasts/handlers/YeastQueryHandlers.scala << 'EOF'
package application.yeasts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import application.queries.public.yeasts._
import application.yeasts.dtos._

@Singleton
class YeastQueryHandlers @Inject()(
  // Les vrais handlers seraient injectés ici  
)(implicit ec: ExecutionContext) {

  def handleGetById(query: GetYeastByIdQuery): Future[Option[YeastDetailResponseDTO]] = {
    // TODO: Implémenter avec le vrai handler
    Future.successful(None)
  }

  def handleFindYeasts(query: FindYeastsQuery): Future[Either[List[String], YeastPageResponseDTO]] = {
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleFindByType(query: FindYeastsByTypeQuery): Future[Either[String, List[YeastSummaryDTO]]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleFindByLaboratory(query: FindYeastsByLaboratoryQuery): Future[Either[String, List[YeastSummaryDTO]]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleSearch(query: SearchYeastsQuery): Future[Either[String, List[YeastSummaryDTO]]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleGetRecommendations(query: GetYeastRecommendationsQuery): Future[Either[List[String], List[YeastRecommendationDTO]]] = {
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleGetAlternatives(query: GetYeastAlternativesQuery): Future[Either[List[String], List[YeastRecommendationDTO]]] = {
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleGetStats(): Future[YeastStatsDTO] = {
    Future.successful(YeastStatsDTO(0, 0, 0, List.empty, List.empty, List.empty))
  }
}
EOF

echo -e "${GREEN}✅ Handlers créés${NC}"

# =============================================================================
# ÉTAPE 4 : CRÉER DTO TYPES MANQUANTS
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 4 : Création DTOs manquants${NC}"

mkdir -p app/application/yeasts/dtos

# DTOs Response
cat > app/application/yeasts/dtos/YeastResponseDTOs.scala << 'EOF'
package application.yeasts.dtos

import play.api.libs.json._
import java.util.UUID

case class YeastDetailResponseDTO(
  id: UUID,
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  characteristics: Map[String, List[String]],
  status: String,
  version: Long,
  createdAt: String,
  updatedAt: String
)

case class YeastSummaryDTO(
  id: UUID,
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  status: String
)

case class YeastPageResponseDTO(
  yeasts: List[YeastSummaryDTO],
  totalCount: Long,
  page: Int,
  pageSize: Int,
  hasNext: Boolean
)

case class YeastRecommendationDTO(
  yeast: YeastSummaryDTO,
  score: Double,
  reason: String,
  tips: List[String]
)

case class YeastStatsDTO(
  totalCount: Long,
  activeCount: Long,
  inactiveCount: Long,
  topLaboratories: List[String],
  topYeastTypes: List[String],
  recentlyAdded: List[YeastSummaryDTO]
)

object YeastDetailResponseDTO {
  implicit val format: Format[YeastDetailResponseDTO] = Json.format[YeastDetailResponseDTO]
}

object YeastSummaryDTO {
  implicit val format: Format[YeastSummaryDTO] = Json.format[YeastSummaryDTO]
}

object YeastPageResponseDTO {
  implicit val format: Format[YeastPageResponseDTO] = Json.format[YeastPageResponseDTO]
}

object YeastRecommendationDTO {
  implicit val format: Format[YeastRecommendationDTO] = Json.format[YeastRecommendationDTO]
}

object YeastStatsDTO {
  implicit val format: Format[YeastStatsDTO] = Json.format[YeastStatsDTO]
}
EOF

# DTOs Request
cat > app/application/yeasts/dtos/YeastRequestDTOs.scala << 'EOF'
package application.yeasts.dtos

import play.api.libs.json._

case class YeastSearchRequestDTO(
  name: Option[String],
  laboratory: Option[String],
  yeastType: Option[String],
  minAttenuation: Option[Int],
  maxAttenuation: Option[Int],
  minTemperature: Option[Int],
  maxTemperature: Option[Int],
  minAlcoholTolerance: Option[Double],
  maxAlcoholTolerance: Option[Double],
  flocculation: Option[String],
  characteristics: Option[List[String]],
  status: Option[String],
  page: Int = 0,
  size: Int = 20
)

case class CreateYeastRequestDTO(
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String],
  flavorProfile: List[String],
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String]
)

case class UpdateYeastRequestDTO(
  name: String,
  laboratory: String,
  strain: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String],
  flavorProfile: List[String],
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String]
)

case class ChangeStatusRequestDTO(
  status: String,
  reason: Option[String]
)

object YeastSearchRequestDTO {
  implicit val format: Format[YeastSearchRequestDTO] = Json.format[YeastSearchRequestDTO]
}

object CreateYeastRequestDTO {
  implicit val format: Format[CreateYeastRequestDTO] = Json.format[CreateYeastRequestDTO]
}

object UpdateYeastRequestDTO {
  implicit val format: Format[UpdateYeastRequestDTO] = Json.format[UpdateYeastRequestDTO]
}

object ChangeStatusRequestDTO {
  implicit val format: Format[ChangeStatusRequestDTO] = Json.format[ChangeStatusRequestDTO]
}
EOF

echo -e "${GREEN}✅ DTOs créés${NC}"

# =============================================================================
# ÉTAPE 5 : CORRIGER YeastDTOs.scala POUR UTILISER LES NOUVEAUX TYPES
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 5 : Correction YeastDTOs.scala${NC}"

# Créer sauvegarde
cp app/application/yeasts/dtos/YeastDTOs.scala app/application/yeasts/dtos/YeastDTOs.scala.backup

# Corriger les imports
cat > app/application/yeasts/dtos/YeastDTOs.scala << 'EOF'
package application.yeasts.dtos

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import application.yeasts.handlers.{YeastCommandHandlers, YeastQueryHandlers}
import application.commands.admin.yeasts._
import application.queries.public.yeasts._

// Importer les DTOs
import YeastResponseDTOs._
import YeastRequestDTOs._

@Singleton
class YeastDTOs @Inject()(
  commandHandlers: YeastCommandHandlers,
  queryHandlers: YeastQueryHandlers
)(implicit ec: ExecutionContext) {

  // =========================================================================
  // QUERIES (READ OPERATIONS)
  // =========================================================================

  def getYeastById(yeastId: UUID): Future[Option[YeastDetailResponseDTO]] = {
    val query = GetYeastByIdQuery(yeastId)
    queryHandlers.handleGetById(query)
  }

  def findYeasts(searchRequest: YeastSearchRequestDTO): Future[Either[List[String], YeastPageResponseDTO]] = {
    val query = FindYeastsQuery(
      name = searchRequest.name,
      laboratory = searchRequest.laboratory,
      yeastType = searchRequest.yeastType,
      minAttenuation = searchRequest.minAttenuation,
      maxAttenuation = searchRequest.maxAttenuation,
      minTemperature = searchRequest.minTemperature,
      maxTemperature = searchRequest.maxTemperature,
      minAlcoholTolerance = searchRequest.minAlcoholTolerance,
      maxAlcoholTolerance = searchRequest.maxAlcoholTolerance,
      flocculation = searchRequest.flocculation,
      characteristics = searchRequest.characteristics,
      status = searchRequest.status,
      page = searchRequest.page,
      size = searchRequest.size
    )
    queryHandlers.handleFindYeasts(query)
  }

  def findYeastsByType(yeastType: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    val query = FindYeastsByTypeQuery(yeastType, limit)
    queryHandlers.handleFindByType(query)
  }

  def findYeastsByLaboratory(laboratory: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    val query = FindYeastsByLaboratoryQuery(laboratory, limit)
    queryHandlers.handleFindByLaboratory(query)
  }

  def searchYeasts(searchTerm: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    val query = SearchYeastsQuery(searchTerm, limit)
    queryHandlers.handleSearch(query)
  }

  def getYeastStatistics(): Future[YeastStatsDTO] = {
    queryHandlers.handleGetStats()
  }

  def getYeastRecommendations(
    beerStyle: Option[String] = None,
    targetAbv: Option[Double] = None,
    fermentationTemp: Option[Int] = None,
    desiredCharacteristics: Option[List[String]] = None,
    limit: Int = 10
  ): Future[Either[List[String], List[YeastRecommendationDTO]]] = {
    
    val query = GetYeastRecommendationsQuery(
      beerStyle,
      targetAbv,
      fermentationTemp,
      desiredCharacteristics,
      limit
    )
    queryHandlers.handleGetRecommendations(query)
  }

  def getYeastAlternatives(
    originalYeastId: UUID,
    reason: String = "unavailable",
    limit: Int = 5
  ): Future[Either[List[String], List[YeastRecommendationDTO]]] = {

    val query = GetYeastAlternativesQuery(originalYeastId, reason, limit)
    queryHandlers.handleGetAlternatives(query)
  }

  // =========================================================================
  // COMMANDS (WRITE OPERATIONS)
  // =========================================================================

  def createYeast(
    request: CreateYeastRequestDTO, 
    createdBy: UUID
  ): Future[Either[List[String], YeastDetailResponseDTO]] = {
    
    val command = CreateYeastCommand(
      name = request.name,
      laboratory = request.laboratory,
      strain = request.strain,
      yeastType = request.yeastType,
      attenuationMin = request.attenuationMin,
      attenuationMax = request.attenuationMax,
      temperatureMin = request.temperatureMin,
      temperatureMax = request.temperatureMax,
      alcoholTolerance = request.alcoholTolerance,
      flocculation = request.flocculation,
      aromaProfile = request.aromaProfile,
      flavorProfile = request.flavorProfile,
      esters = request.esters,
      phenols = request.phenols,
      otherCompounds = request.otherCompounds,
      notes = request.notes,
      createdBy = createdBy
    )
    commandHandlers.handleCreateYeast(command)
  }

  def updateYeast(
    yeastId: UUID,
    request: UpdateYeastRequestDTO,
    updatedBy: UUID
  ): Future[Either[List[String], YeastDetailResponseDTO]] = {
    
    val command = UpdateYeastCommand(
      yeastId = yeastId,
      name = request.name,
      laboratory = request.laboratory,
      strain = request.strain,
      attenuationMin = request.attenuationMin,
      attenuationMax = request.attenuationMax,
      temperatureMin = request.temperatureMin,
      temperatureMax = request.temperatureMax,
      alcoholTolerance = request.alcoholTolerance,
      flocculation = request.flocculation,
      aromaProfile = request.aromaProfile,
      flavorProfile = request.flavorProfile,
      esters = request.esters,
      phenols = request.phenols,
      otherCompounds = request.otherCompounds,
      notes = request.notes,
      updatedBy = updatedBy
    )
    commandHandlers.handleUpdateYeast(command)
  }

  def changeYeastStatus(
    yeastId: UUID,
    request: ChangeStatusRequestDTO,
    changedBy: UUID
  ): Future[Either[String, Unit]] = {
    
    val command = ChangeYeastStatusCommand(
      yeastId = yeastId,
      newStatus = request.status,
      reason = request.reason,
      changedBy = changedBy
    )
    commandHandlers.handleChangeStatus(command)
  }

  def activateYeast(yeastId: UUID, activatedBy: UUID): Future[Either[String, YeastDetailResponseDTO]] = {
    val command = ActivateYeastCommand(yeastId, activatedBy)
    commandHandlers.handleActivateYeast(command)
  }

  def deactivateYeast(
    yeastId: UUID, 
    reason: Option[String], 
    deactivatedBy: UUID
  ): Future[Either[String, YeastDetailResponseDTO]] = {
    val command = DeactivateYeastCommand(yeastId, reason, deactivatedBy)
    commandHandlers.handleDeactivateYeast(command)
  }

  def archiveYeast(
    yeastId: UUID, 
    reason: Option[String], 
    archivedBy: UUID
  ): Future[Either[String, YeastDetailResponseDTO]] = {
    val command = ArchiveYeastCommand(yeastId, reason, archivedBy)
    commandHandlers.handleArchiveYeast(command)
  }

  def deleteYeast(yeastId: UUID, reason: Option[String], deletedBy: UUID): Future[Either[String, Unit]] = {
    val command = DeleteYeastCommand(yeastId, reason, deletedBy)
    commandHandlers.handleDeleteYeast(command)
  }

  def createYeastsBatch(
    requests: List[CreateYeastRequestDTO],
    createdBy: UUID
  ): Future[Either[List[String], List[YeastDetailResponseDTO]]] = {
    
    val commands = requests.map { request =>
      CreateYeastCommand(
        name = request.name,
        laboratory = request.laboratory,
        strain = request.strain,
        yeastType = request.yeastType,
        attenuationMin = request.attenuationMin,
        attenuationMax = request.attenuationMax,
        temperatureMin = request.temperatureMin,
        temperatureMax = request.temperatureMax,
        alcoholTolerance = request.alcoholTolerance,
        flocculation = request.flocculation,
        aromaProfile = request.aromaProfile,
        flavorProfile = request.flavorProfile,
        esters = request.esters,
        phenols = request.phenols,
        otherCompounds = request.otherCompounds,
        notes = request.notes,
        createdBy = createdBy
      )
    }
    
    val batchCommand = CreateYeastsBatchCommand(commands)
    commandHandlers.handleBatchCreate(batchCommand)
  }
}
EOF

echo -e "${GREEN}✅ YeastDTOs.scala corrigé${NC}"

# =============================================================================
# ÉTAPE 6 : CORRIGER LE SERVICE YeastRecommendationService
# =============================================================================

echo -e "\n${YELLOW}📋 ÉTAPE 6 : Correction YeastRecommendationService${NC}"

# Corriger la méthode isBeginnerFriendly
cp app/domain/yeasts/services/YeastRecommendationService.scala app/domain/yeasts/services/YeastRecommendationService.scala.backup

# Remplacer la méthode problématique
sed -i '' '/private def isBeginnerFriendly/,/^  }/c\
  private def isBeginnerFriendly(yeast: YeastAggregate): Boolean = {\
    val hasSuitableFlocculation = yeast.flocculation match {\
      case FlocculationLevel.Medium | FlocculationLevel.MediumHigh | FlocculationLevel.High => true\
      case _ => false\
    }\
    \
    hasSuitableFlocculation && \
    yeast.temperature.range <= 6 && // Plage de température pas trop large\
    yeast.characteristics.isClean // Profil neutre plus facile\
  }' app/domain/yeasts/services/YeastRecommendationService.scala

echo -e "${GREEN}✅ YeastRecommendationService corrigé${NC}"

# =============================================================================
# ÉTAPE 7 : TEST DE COMPILATION
# =============================================================================

echo -e "\n${YELLOW}🔨 ÉTAPE 7 : Test compilation${NC}"

echo "Lancement compilation..."
if sbt compile > /tmp/yeast_fix_compile.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    echo ""
    echo -e "${BLUE}📊 RÉSUMÉ DES CORRECTIONS :${NC}"
    echo "• 8 Commands créées (Create, Update, Delete, Activate, etc.)"
    echo "• 7 Queries créées (GetById, Find, Search, Recommendations, etc.)"
    echo "• 2 Handler services créés (Command + Query handlers)"
    echo "• 5 DTOs créés (Request + Response types)"
    echo "• YeastDTOs.scala complètement corrigé"
    echo "• YeastRecommendationService syntaxe corrigée"
    echo ""
    echo -e "${GREEN}🎉 Domaine Yeasts prêt pour les tests !${NC}"
else
    echo -e "${RED}❌ Des erreurs de compilation persistent${NC}"
    echo "Dernières erreurs :"
    tail -20 /tmp/yeast_fix_compile.log
    echo ""
    echo "Log complet : /tmp/yeast_fix_compile.log"
fi

# =============================================================================
# ÉTAPE 8 : INSTRUCTIONS FINALES
# =============================================================================

echo -e "\n${BLUE}📋 PROCHAINES ÉTAPES RECOMMANDÉES :${NC}"
echo ""
echo "1. ${YELLOW}Test des APIs :${NC}"
echo "   sbt run"
echo "   curl http://localhost:9000/api/v1/yeasts"
echo ""
echo "2. ${YELLOW}Implémenter les vrais handlers :${NC}"
echo "   Remplacer les stubs par la vraie logique métier"
echo ""
echo "3. ${YELLOW}Tests unitaires :${NC}"
echo "   Créer tests pour Commands, Queries et DTOs"
echo ""
echo "4. ${YELLOW}Intégration avec repositories :${NC}"
echo "   Connecter handlers aux repositories Slick existants"

echo -e "\n${GREEN}🔧 Correction terminée !${NC}"