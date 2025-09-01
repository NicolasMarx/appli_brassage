package application.yeasts.dtos

import domain.yeasts.services.YeastRecommendationService
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

/**
 * Service applicatif principal pour les levures
 * Orchestration des handlers et conversion DTOs
 */
@Singleton
class YeastApplicationService @Inject()(
  commandHandlers: YeastCommandHandlers,
  queryHandlers: YeastQueryHandlers,
  yeastRecommendationService: YeastRecommendationService
)(implicit ec: ExecutionContext) {

  // ==========================================================================
  // OPÉRATIONS DE LECTURE (QUERIES)
  // ==========================================================================

  /**
   * Récupère une levure par ID
   */
  def getYeastById(yeastId: UUID): Future[Option[YeastDetailResponseDTO]] = {
    val query = GetYeastByIdQuery(yeastId)
    queryHandlers.handle(query).map(_.map(YeastDTOs.toDetailResponse))
  }

  /**
   * Recherche de levures avec filtres et pagination
   */
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
    
    queryHandlers.handle(query).map(_.map(YeastDTOs.toPageResponse))
  }

  /**
   * Recherche par type de levure
   */
  def findYeastsByType(yeastType: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    val query = FindYeastsByTypeQuery(yeastType, limit)
    queryHandlers.handle(query).map(_.map(_.map(YeastDTOs.toSummary)))
  }

  /**
   * Recherche par laboratoire
   */
  def findYeastsByLaboratory(laboratory: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    val query = FindYeastsByLaboratoryQuery(laboratory, limit)
    queryHandlers.handle(query).map(_.map(_.map(YeastDTOs.toSummary)))
  }

  /**
   * Recherche textuelle
   */
  def searchYeasts(searchTerm: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    val query = SearchYeastsQuery(searchTerm, limit)
    queryHandlers.handle(query).map(_.map(_.map(YeastDTOs.toSummary)))
  }

  /**
   * Statistiques des levures
   */
  def getYeastStatistics(): Future[YeastStatsDTO] = {
    for {
      byStatus <- queryHandlers.handle(GetYeastStatsQuery())
      byLaboratory <- queryHandlers.handle(GetYeastStatsByLaboratoryQuery())
      byType <- queryHandlers.handle(GetYeastStatsByTypeQuery())
      recentlyAdded <- queryHandlers.handle(GetRecentlyAddedYeastsQuery(5)).map(_.getOrElse(List.empty))
      mostPopular <- queryHandlers.handle(GetMostPopularYeastsQuery(5)).map(_.getOrElse(List.empty))
    } yield {
      YeastStatsDTO(
        totalCount = byStatus.values.sum,
        byStatus = byStatus.map { case (status, count) => status.name -> count },
        byLaboratory = byLaboratory.map { case (lab, count) => lab.name -> count },
        byType = byType.map { case (yeastType, count) => yeastType.name -> count },
        recentlyAdded = recentlyAdded.map(YeastDTOs.toSummary),
        mostPopular = mostPopular.map(YeastDTOs.toSummary)
      )
    }
  }

  /**
   * Recommandations de levures
   */
  def getYeastRecommendations(
    beerStyle: Option[String] = None,
    targetAbv: Option[Double] = None,
    fermentationTemp: Option[Int] = None,
    desiredCharacteristics: List[String] = List.empty,
    limit: Int = 10
  ): Future[Either[List[String], List[YeastRecommendationDTO]]] = {
    
    val query = GetYeastRecommendationsQuery(
      beerStyle = beerStyle,
      targetAbv = targetAbv,
      fermentationTemp = fermentationTemp,
      desiredCharacteristics = desiredCharacteristics,
      limit = limit
    )
    
    queryHandlers.handle(query).map { result =>
      result.map(_.map(recommended => 
        YeastRecommendationDTO(
          yeast = YeastDTOs.toSummary(recommended.yeast),
          score = recommended.score,
          reason = recommended.reason,
          tips = recommended.tips
        )
      ))
    }
  }

  /**
   * Alternatives à une levure
   */
  def getYeastAlternatives(
    originalYeastId: UUID,
    reason: String = "unavailable",
    limit: Int = 5
  ): Future[Either[List[String], List[YeastRecommendationDTO]]] = {
    
    val query = GetYeastAlternativesQuery(originalYeastId, reason, limit)
    
    queryHandlers.handle(query).map { result =>
      result.map(_.map(recommended => 
        YeastRecommendationDTO(
          yeast = YeastDTOs.toSummary(recommended.yeast),
          score = recommended.score,
          reason = recommended.reason,
          tips = recommended.tips
        )
      ))
    }
  }

  // ==========================================================================
  // OPÉRATIONS D'ÉCRITURE (COMMANDS)
  // ==========================================================================

  /**
   * Crée une nouvelle levure
   */
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
    
    commandHandlers.handle(command).map(_.map(YeastDTOs.toDetailResponse))
  }

  /**
   * Met à jour une levure existante
   */
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
    
    commandHandlers.handle(command).map(_.map(YeastDTOs.toDetailResponse))
  }

  /**
   * Change le statut d'une levure
   */
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
    
    commandHandlers.handle(command)
  }

  /**
   * Active une levure
   */
  def activateYeast(yeastId: UUID, activatedBy: UUID): Future[Either[String, YeastDetailResponseDTO]] = {
    val command = ActivateYeastCommand(yeastId, activatedBy)
    commandHandlers.handle(command).map(_.map(YeastDTOs.toDetailResponse))
  }

  /**
   * Désactive une levure
   */
  def deactivateYeast(
    yeastId: UUID, 
    reason: Option[String], 
    deactivatedBy: UUID
  ): Future[Either[String, YeastDetailResponseDTO]] = {
    val command = DeactivateYeastCommand(yeastId, reason, deactivatedBy)
    commandHandlers.handle(command).map(_.map(YeastDTOs.toDetailResponse))
  }

  /**
   * Archive une levure
   */
  def archiveYeast(
    yeastId: UUID, 
    reason: Option[String], 
    archivedBy: UUID
  ): Future[Either[String, YeastDetailResponseDTO]] = {
    val command = ArchiveYeastCommand(yeastId, reason, archivedBy)
    commandHandlers.handle(command).map(_.map(YeastDTOs.toDetailResponse))
  }

  /**
   * Supprime une levure (archive)
   */
  def deleteYeast(yeastId: UUID, reason: String, deletedBy: UUID): Future[Either[String, Unit]] = {
    val command = DeleteYeastCommand(yeastId, reason, deletedBy)
    commandHandlers.handle(command)
  }

  /**
   * Création batch de levures
   */
  def createYeastsBatch(
    requests: List[CreateYeastRequestDTO],
    createdBy: UUID
  ): Future[Either[List[String], List[YeastDetailResponseDTO]]] = {
    
    val commands = requests.map(request => 
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
    )
    
    val batchCommand = CreateYeastsBatchCommand(commands)
    commandHandlers.handle(batchCommand).map(_.map(_.map(YeastDTOs.toDetailResponse)))
  }

  // ==========================================================================
  // OPÉRATIONS SPÉCIALISÉES
  // ==========================================================================

  /**
   * Recommandations pour débutants
   */
  def getBeginnerRecommendations(limit: Int = 5): Future[List[YeastRecommendationDTO]] = {
    yeastRecommendationService.getBeginnerFriendlyYeasts(limit).map(_.map(recommended =>
      YeastRecommendationDTO(
        yeast = YeastDTOs.toSummary(recommended.yeast),
        score = recommended.score,
        reason = recommended.reason,
        tips = recommended.tips
      )
    ))
  }

  /**
   * Recommandations saisonnières
   */
  def getSeasonalRecommendations(season: String, limit: Int = 8): Future[List[YeastRecommendationDTO]] = {
    val parsedSeason = season.toLowerCase match {
      case "spring" => Season.Spring
      case "summer" => Season.Summer
      case "autumn" | "fall" => Season.Autumn
      case "winter" => Season.Winter
      case _ => Season.Spring // Default
    }
    
    yeastRecommendationService.getSeasonalRecommendations(parsedSeason, limit).map(_.map(recommended =>
      YeastRecommendationDTO(
        yeast = YeastDTOs.toSummary(recommended.yeast),
        score = recommended.score,
        reason = recommended.reason,
        tips = recommended.tips
      )
    ))
  }

  /**
   * Recommandations pour expérimentateurs
   */
  def getExperimentalRecommendations(limit: Int = 6): Future[List[YeastRecommendationDTO]] = {
    yeastRecommendationService.getExperimentalYeasts(limit).map(_.map(recommended =>
      YeastRecommendationDTO(
        yeast = YeastDTOs.toSummary(recommended.yeast),
        score = recommended.score,
        reason = recommended.reason,
        tips = recommended.tips
      )
    ))
  }

  /**
   * Validation d'une recette avec levure
   */
  def validateRecipeWithYeast(
    yeastId: UUID,
    targetTemp: Int,
    targetAbv: Double,
    targetAttenuation: Int
  ): Future[Either[String, FermentationCompatibilityReport]] = {
    
    queryHandlers.handle(GetYeastByIdQuery(yeastId)).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: $yeastId"))
      case Some(yeast) =>
        // Utiliser le service domaine pour l'analyse
        import domain.yeasts.services.YeastDomainService
        // Note: Il faudrait injecter le service domaine ici
        Future.successful(Right(FermentationCompatibilityReport(
          yeast = yeast,
          temperatureCompatible = yeast.temperature.contains(targetTemp),
          alcoholCompatible = yeast.alcoholTolerance.canFerment(targetAbv),
          attenuationCompatible = yeast.attenuation.contains(targetAttenuation),
          overallScore = 0.8, // Calculé par le service domaine
          warnings = List.empty,
          recommendations = List.empty
        )))
    }
  }
}
