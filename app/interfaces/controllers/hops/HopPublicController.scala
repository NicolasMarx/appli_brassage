package interfaces.controllers.hops

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

import domain.hops.model._
import domain.hops.services.HopRecommendationService
import domain.hops.repositories.HopReadRepository
import domain.recipes.model.Season
import application.hops.dtos._
import application.hops.dtos.HopResponseDTOs._

/**
 * Contrôleur public pour les API intelligentes des houblons
 * Réplique exactement YeastPublicController pour les houblons
 */
@Singleton
class HopPublicController @Inject()(
  cc: ControllerComponents,
  hopRecommendationService: HopRecommendationService,
  hopRepository: HopReadRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ============================================================================
  // ENDPOINTS DE RECOMMANDATIONS INTELLIGENTES
  // ============================================================================
  
  /**
   * GET /hops/recommendations/beginner
   * Recommandations pour brasseurs débutants
   */
  def getBeginnerRecommendations(maxResults: Int = 5): Action[AnyContent] = Action.async {
    hopRecommendationService.getBeginnerFriendlyHops(maxResults).map { recommendations =>
      val response = HopRecommendationListResponseDTO(
        recommendations = recommendations.map(convertRecommendedHopToDTO),
        totalCount = recommendations.size,
        context = "beginner",
        description = "Houblons recommandés pour les brasseurs débutants"
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /hops/recommendations/seasonal/{season}
   * Recommandations saisonnières
   */
  def getSeasonalRecommendations(season: String, maxResults: Int = 8): Action[AnyContent] = Action.async {
    Season.fromString(season) match {
      case Right(seasonEnum) =>
        hopRecommendationService.getSeasonalRecommendations(seasonEnum, maxResults).map { recommendations =>
          val response = HopRecommendationListResponseDTO(
            recommendations = recommendations.map(convertRecommendedHopToDTO),
            totalCount = recommendations.size,
            context = s"seasonal_${season.toLowerCase}",
            description = s"Houblons recommandés pour la saison ${season.toLowerCase}"
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
        }
      case Left(_) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Saison invalide",
          "validSeasons" -> List("spring", "summer", "autumn", "winter")
        )))
    }
  }
  
  /**
   * GET /hops/recommendations/experimental
   * Recommandations pour houblons expérimentaux
   */
  def getExperimentalRecommendations(maxResults: Int = 6): Action[AnyContent] = Action.async {
    hopRecommendationService.getExperimentalHops(maxResults).map { recommendations =>
      val response = HopRecommendationListResponseDTO(
        recommendations = recommendations.map(convertRecommendedHopToDTO),
        totalCount = recommendations.size,
        context = "experimental",
        description = "Houblons expérimentaux pour brasseurs avancés"
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /hops/recommendations/aroma
   * Recommandations par profil aromatique
   */
  def getAromaRecommendations(
    aromas: String, 
    maxResults: Int = 10
  ): Action[AnyContent] = Action.async {
    val aromaList = aromas.split(",").map(_.trim).filter(_.nonEmpty).toList
    
    if (aromaList.isEmpty) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Au moins un profil aromatique requis",
        "example" -> "citrus,floral,pine"
      )))
    } else {
      hopRecommendationService.getByAromaProfile(aromaList, maxResults).map { recommendations =>
        val response = HopRecommendationListResponseDTO(
          recommendations = recommendations.map(convertRecommendedHopToDTO),
          totalCount = recommendations.size,
          context = s"aroma_${aromaList.mkString("_")}",
          description = s"Houblons avec profils aromatiques: ${aromaList.mkString(", ")}"
        )
        Ok(Json.toJson(response))
      }.recover {
        case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
      }
    }
  }
  
  /**
   * GET /hops/{id}/alternatives
   * Alternatives intelligentes à un houblon
   */
  def getAlternatives(
    hopId: String,
    reason: String = "unavailable",
    maxResults: Int = 5
  ): Action[AnyContent] = Action.async {
    (HopId.create(hopId), AlternativeReason.fromString(reason)) match {
      case (Right(id), Right(reasonEnum)) =>
        hopRecommendationService.findAlternatives(id, reasonEnum, maxResults).map { alternatives =>
          val response = HopRecommendationListResponseDTO(
            recommendations = alternatives.map(convertRecommendedHopToDTO),
            totalCount = alternatives.size,
            context = s"alternatives_${reason}",
            description = s"Alternatives pour ${reasonEnum.value.toLowerCase}"
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche d'alternatives: ${ex.getMessage}"))
        }
      case (Left(_), _) =>
        Future.successful(BadRequest(Json.obj("error" -> "ID de houblon invalide")))
      case (_, Left(_)) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Raison invalide",
          "validReasons" -> List("unavailable", "too_expensive", "experiment")
        )))
    }
  }
  
  // ============================================================================
  // RECHERCHE INTELLIGENTE
  // ============================================================================
  
  /**
   * GET /hops/search
   * Recherche avancée avec filtres intelligents
   */
  def searchHops(): Action[AnyContent] = Action.async { request =>
    val queryParams = request.queryString
    
    // Construction du filtre à partir des paramètres de requête
    val filter = HopFilter(
      name = queryParams.get("name").flatMap(_.headOption),
      origin = queryParams.get("origin").flatMap(_.headOption),
      country = queryParams.get("country").flatMap(_.headOption),
      usage = queryParams.get("usage").flatMap(_.headOption).flatMap(HopUsage.fromString(_).toOption),
      minAlphaAcids = queryParams.get("minAlphaAcids").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxAlphaAcids = queryParams.get("maxAlphaAcids").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minBetaAcids = queryParams.get("minBetaAcids").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxBetaAcids = queryParams.get("maxBetaAcids").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minCohumulone = queryParams.get("minCohumulone").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxCohumulone = queryParams.get("maxCohumulone").flatMap(_.headOption).flatMap(_.toDoubleOption),
      aromaProfiles = queryParams.get("aromaProfiles").map(_.flatMap(_.split(",").map(_.trim)).toList).getOrElse(List.empty),
      harvestYear = queryParams.get("harvestYear").flatMap(_.headOption).flatMap(_.toIntOption),
      available = queryParams.get("available").flatMap(_.headOption).map(_.toBoolean),
      page = queryParams.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(0),
      size = queryParams.get("size").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(20)
    )
    
    filter.validate() match {
      case Right(validFilter) =>
        hopRepository.findByFilter(validFilter).map { result =>
          val response = HopSearchResponseDTO(
            hops = result.items.map(convertHopToDTO),
            totalCount = result.totalCount.toInt,
            page = result.page,
            size = result.size,
            hasNext = result.hasNext,
            appliedFilters = validFilter,
            suggestions = generateSearchSuggestions(validFilter, result)
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche: ${ex.getMessage}"))
        }
      case Left(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> errors)))
    }
  }
  
  /**
   * GET /hops/recommendations/style/{style}
   * Recommandations par style de bière
   */
  def getStyleRecommendations(
    style: String, 
    maxResults: Int = 8
  ): Action[AnyContent] = Action.async {
    // Logique de recommandation basée sur le style de bière
    val styleSpecificAromas = getAromasForBeerStyle(style)
    
    if (styleSpecificAromas.nonEmpty) {
      hopRecommendationService.getByAromaProfile(styleSpecificAromas, maxResults).map { recommendations =>
        val response = HopRecommendationListResponseDTO(
          recommendations = recommendations.map(convertRecommendedHopToDTO),
          totalCount = recommendations.size,
          context = s"style_${style.toLowerCase}",
          description = s"Houblons recommandés pour le style ${style}"
        )
        Ok(Json.toJson(response))
      }.recover {
        case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
      }
    } else {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Style de bière non reconnu",
        "supportedStyles" -> getSupportedBeerStyles()
      )))
    }
  }
  
  // ============================================================================
  // MÉTHODES UTILITAIRES
  // ============================================================================
  
  private def convertRecommendedHopToDTO(recommendedHop: RecommendedHop): HopRecommendationDTO = {
    HopRecommendationDTO(
      hop = convertHopToDTO(recommendedHop.hop),
      score = recommendedHop.score,
      reason = recommendedHop.reason,
      tips = recommendedHop.tips,
      confidence = calculateConfidenceLevel(recommendedHop.score)
    )
  }
  
  private def convertHopToDTO(hop: HopAggregate): HopResponseDTO = {
    HopResponseDTO(
      id = hop.id.value.toString,
      name = hop.name.value,
      description = hop.description.map(_.value),
      origin = HopOriginDTO(
        country = hop.origin.name,
        region = None, // No region field in current HopAggregate
        farm = None    // No farm field in current HopAggregate
      ),
      characteristics = HopCharacteristicsDTO(
        alphaAcids = Some(hop.alphaAcid.value),
        betaAcids = hop.betaAcid.map(_.value),
        cohumulone = None, // No cohumulone field in current HopAggregate
        totalOils = None,  // No totalOils field in current HopAggregate
        usage = List(hop.usage.value),
        aromaProfiles = hop.aromaProfile.map(aroma => 
          AromaProfileDTO(aroma, None, None) // Simple string to DTO conversion
        ),
        notes = None // No notes field in current HopAggregate
      ),
      availability = HopAvailabilityDTO(
        isAvailable = hop.isActive,
        harvestYear = None, // No harvestYear field in current HopAggregate
        freshness = None,   // No freshness field in current HopAggregate
        suppliers = List.empty // No suppliers field in current HopAggregate
      ),
      createdAt = hop.createdAt,
      updatedAt = hop.updatedAt,
      version = hop.version
    )
  }
  
  private def calculateConfidenceLevel(score: Double): String = {
    if (score >= 0.8) "HIGH"
    else if (score >= 0.5) "MEDIUM"
    else "LOW"
  }
  
  private def generateSearchSuggestions(
    filter: HopFilter, 
    result: domain.common.PaginatedResult[HopAggregate]
  ): List[String] = {
    val suggestions = scala.collection.mutable.ListBuffer[String]()
    
    if (result.totalCount == 0) {
      suggestions += "Essayez d'élargir vos critères de recherche"
      if (filter.minAlphaAcids.isDefined || filter.maxAlphaAcids.isDefined) {
        suggestions += "Ajustez les plages d'alpha acids"
      }
      if (filter.aromaProfiles.nonEmpty) {
        suggestions += "Utilisez des profils aromatiques plus généraux"
      }
    } else if (result.totalCount < 5) {
      suggestions += "Peu de résultats trouvés - considérez des critères plus larges"
    }
    
    if (filter.usage.isDefined) {
      suggestions += "Explorez d'autres usages (bittering, aroma, dual-purpose)"
    }
    
    suggestions.toList
  }
  
  private def getAromasForBeerStyle(style: String): List[String] = {
    style.toLowerCase match {
      case s if s.contains("ipa") => List("citrus", "pine", "resinous", "tropical")
      case s if s.contains("pale ale") => List("citrus", "floral", "fruity")
      case s if s.contains("lager") || s.contains("pilsner") => List("floral", "spicy", "herbal")
      case s if s.contains("wheat") => List("citrus", "coriander", "spicy")
      case s if s.contains("stout") || s.contains("porter") => List("earthy", "woody", "chocolate")
      case s if s.contains("saison") => List("spicy", "peppery", "earthy")
      case s if s.contains("belgique") || s.contains("belgian") => List("spicy", "fruity", "floral")
      case _ => List.empty
    }
  }
  
  private def getSupportedBeerStyles(): List[String] = {
    List(
      "IPA", "Pale Ale", "Lager", "Pilsner", "Wheat Beer", "Stout", 
      "Porter", "Saison", "Belgian Ale", "Amber Ale", "Brown Ale"
    )
  }
  
  // ============================================================================
  // ENDPOINTS D'INFORMATION ET SANTÉ
  // ============================================================================
  
  /**
   * GET /hops/health
   * Vérification de santé du service
   */
  def health(): Action[AnyContent] = Action {
    Ok(Json.obj(
      "status" -> "healthy",
      "service" -> "HopIntelligence",
      "version" -> "1.0.0",
      "features" -> Json.arr(
        "beginner-recommendations",
        "seasonal-recommendations", 
        "experimental-recommendations",
        "aroma-matching",
        "intelligent-alternatives",
        "advanced-search",
        "style-recommendations"
      ),
      "timestamp" -> System.currentTimeMillis()
    ))
  }
}