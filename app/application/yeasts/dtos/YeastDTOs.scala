package application.yeasts.dtos
import application.yeasts.dtos.YeastResponseDTOs._
import application.yeasts.dtos.YeastRequestDTOs._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import application.yeasts.handlers.{YeastCommandHandlers, YeastQueryHandlers}
import application.commands.admin.yeasts._
import application.queries.public.yeasts._

// Importer les DTOs

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
