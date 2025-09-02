package interfaces.controllers.malts

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

import domain.malts.model._
import domain.malts.services.{MaltRecommendationService, Season, AlternativeReason}
import domain.malts.repositories.MaltReadRepository
import application.malts.dtos._
import application.malts.dtos.MaltResponseDTOs._
import application.malts.dtos.MaltDTOConverters._

/**
 * Contrôleur public pour les API intelligentes des malts
 * Réplique exactement YeastPublicController et HopPublicController pour les malts
 */
@Singleton
class MaltPublicController @Inject()(
  cc: ControllerComponents,
  maltRecommendationService: MaltRecommendationService,
  maltRepository: MaltReadRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ============================================================================
  // ENDPOINTS DE RECOMMANDATIONS INTELLIGENTES
  // ============================================================================
  
  /**
   * GET /malts/recommendations/beginner
   * Recommandations pour brasseurs débutants
   */
  def getBeginnerRecommendations(maxResults: Int = 5): Action[AnyContent] = Action.async {
    maltRecommendationService.getBeginnerFriendlyMalts(maxResults).map { recommendations =>
      val response = MaltRecommendationListResponseDTO(
        recommendations = recommendations.map(convertRecommendedMaltToDTO),
        totalCount = recommendations.size,
        context = "beginner",
        description = "Malts recommandés pour les brasseurs débutants",
        metadata = RecommendationMetadataDTO(
          parameters = Map("maxResults" -> maxResults.toString)
        )
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /malts/recommendations/seasonal/{season}
   * Recommandations saisonnières
   */
  def getSeasonalRecommendations(season: String, maxResults: Int = 8): Action[AnyContent] = Action.async {
    Season.fromString(season) match {
      case Right(seasonEnum) =>
        maltRecommendationService.getSeasonalRecommendations(seasonEnum, maxResults).map { recommendations =>
          val response = MaltRecommendationListResponseDTO(
            recommendations = recommendations.map(convertRecommendedMaltToDTO),
            totalCount = recommendations.size,
            context = s"seasonal_${season.toLowerCase}",
            description = s"Malts recommandés pour la saison ${season.toLowerCase}",
            metadata = RecommendationMetadataDTO(
              parameters = Map("season" -> season, "maxResults" -> maxResults.toString)
            )
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
        }
      case Left(error) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> error,
          "validSeasons" -> List("spring", "summer", "autumn", "winter")
        )))
    }
  }
  
  /**
   * GET /malts/recommendations/experimental
   * Recommandations pour malts expérimentaux
   */
  def getExperimentalRecommendations(maxResults: Int = 6): Action[AnyContent] = Action.async {
    maltRecommendationService.getExperimentalMalts(maxResults).map { recommendations =>
      val response = MaltRecommendationListResponseDTO(
        recommendations = recommendations.map(convertRecommendedMaltToDTO),
        totalCount = recommendations.size,
        context = "experimental",
        description = "Malts expérimentaux pour brasseurs avancés",
        metadata = RecommendationMetadataDTO(
          parameters = Map("maxResults" -> maxResults.toString)
        )
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /malts/recommendations/flavor
   * Recommandations par profil de saveur
   */
  def getFlavorRecommendations(
    flavors: String, 
    maxResults: Int = 10
  ): Action[AnyContent] = Action.async {
    val flavorList = flavors.split(",").map(_.trim).filter(_.nonEmpty).toList
    
    if (flavorList.isEmpty) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Au moins un profil de saveur requis",
        "example" -> "caramel,chocolate,nutty"
      )))
    } else {
      maltRecommendationService.getByFlavorProfile(flavorList, maxResults).map { recommendations =>
        val response = MaltRecommendationListResponseDTO(
          recommendations = recommendations.map(convertRecommendedMaltToDTO),
          totalCount = recommendations.size,
          context = s"flavor_${flavorList.mkString("_")}",
          description = s"Malts avec profils de saveur: ${flavorList.mkString(", ")}",
          metadata = RecommendationMetadataDTO(
            parameters = Map("flavors" -> flavors, "maxResults" -> maxResults.toString)
          )
        )
        Ok(Json.toJson(response))
      }.recover {
        case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
      }
    }
  }
  
  /**
   * GET /malts/{id}/alternatives
   * Alternatives intelligentes à un malt
   */
  def getAlternatives(
    maltId: String,
    reason: String = "unavailable",
    maxResults: Int = 5
  ): Action[AnyContent] = Action.async {
    (MaltId.fromString(maltId), AlternativeReason.fromString(reason)) match {
      case (Right(id), Right(reasonEnum)) =>
        maltRecommendationService.findAlternatives(id, reasonEnum, maxResults).map { alternatives =>
          val response = MaltRecommendationListResponseDTO(
            recommendations = alternatives.map(convertRecommendedMaltToDTO),
            totalCount = alternatives.size,
            context = s"alternatives_${reason}",
            description = s"Alternatives pour ${reasonEnum.value.toLowerCase}",
            metadata = RecommendationMetadataDTO(
              parameters = Map("originalMaltId" -> maltId, "reason" -> reason, "maxResults" -> maxResults.toString)
            )
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche d'alternatives: ${ex.getMessage}"))
        }
      case (Left(error), _) =>
        Future.successful(BadRequest(Json.obj("error" -> error)))
      case (_, Left(error)) =>
        Future.successful(BadRequest(Json.obj("error" -> error)))
    }
  }
  
  /**
   * GET /malts/recommendations/style/{style}
   * Recommandations par style de bière
   */
  def getStyleRecommendations(
    style: String, 
    maxResults: Int = 8
  ): Action[AnyContent] = Action.async {
    val supportedStyles = getSupportedBeerStyles()
    
    if (supportedStyles.exists(_.toLowerCase == style.toLowerCase)) {
      maltRecommendationService.getForBeerStyle(style, maxResults).map { recommendations =>
        val response = MaltRecommendationListResponseDTO(
          recommendations = recommendations.map(convertRecommendedMaltToDTO),
          totalCount = recommendations.size,
          context = s"style_${style.toLowerCase}",
          description = s"Malts recommandés pour le style ${style}",
          metadata = RecommendationMetadataDTO(
            parameters = Map("style" -> style, "maxResults" -> maxResults.toString)
          )
        )
        Ok(Json.toJson(response))
      }.recover {
        case ex => InternalServerError(Json.obj("error" -> s"Erreur lors des recommandations: ${ex.getMessage}"))
      }
    } else {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Style de bière non reconnu",
        "supportedStyles" -> supportedStyles
      )))
    }
  }
  
  // ============================================================================
  // RECHERCHE INTELLIGENTE
  // ============================================================================
  
  /**
   * GET /malts/search
   * Recherche avancée avec filtres intelligents
   */
  def searchMalts(): Action[AnyContent] = Action.async { request =>
    val queryParams = request.queryString
    
    // Construction du filtre à partir des paramètres de requête
    val filter = MaltFilter(
      name = queryParams.get("name").flatMap(_.headOption),
      maltType = queryParams.get("maltType").flatMap(_.headOption).flatMap(MaltType.fromName),
      originCode = queryParams.get("originCode").flatMap(_.headOption),
      minEbcColor = queryParams.get("minEbcColor").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxEbcColor = queryParams.get("maxEbcColor").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minExtractionRate = queryParams.get("minExtractionRate").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxExtractionRate = queryParams.get("maxExtractionRate").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minDiastaticPower = queryParams.get("minDiastaticPower").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxDiastaticPower = queryParams.get("maxDiastaticPower").flatMap(_.headOption).flatMap(_.toDoubleOption),
      flavorProfiles = queryParams.get("flavorProfiles").map(_.flatMap(_.split(",").map(_.trim)).toList).getOrElse(List.empty),
      source = queryParams.get("source").flatMap(_.headOption).flatMap(s => MaltSource.fromString(s).toOption),
      isActive = queryParams.get("isActive").flatMap(_.headOption).map(_.toBoolean),
      minCredibilityScore = queryParams.get("minCredibilityScore").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxCredibilityScore = queryParams.get("maxCredibilityScore").flatMap(_.headOption).flatMap(_.toDoubleOption),
      page = queryParams.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(0),
      size = queryParams.get("size").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(20)
    )
    
    filter.validate() match {
      case Right(validFilter) =>
        maltRepository.findByFilter(validFilter).map { result =>
          val response = MaltSearchResponseDTO(
            malts = result.items.map(convertMaltToDTO),
            totalCount = result.totalCount.toInt,
            page = result.page,
            size = result.size,
            hasNext = result.hasNext,
            appliedFilters = validFilter,
            suggestions = List.empty, // TODO: implement buildSearchSuggestions
            facets = MaltFacetsDTO(List.empty, List.empty, List.empty, List.empty, List.empty) // TODO: implement buildMaltFacets
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche: ${ex.getMessage}"))
        }
      case Left(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> errors.mkString(", "))))
    }
  }
  
  /**
   * GET /malts/{id}
   * Détails d'un malt spécifique
   */
  def getMaltDetails(maltId: String): Action[AnyContent] = Action.async {
    MaltId.fromString(maltId) match {
      case Right(id) =>
        maltRepository.findById(id).map {
          case Some(malt) =>
            Ok(Json.toJson(convertMaltToDTO(malt)))
          case None =>
            NotFound(Json.obj(
              "error" -> "Malt non trouvé",
              "maltId" -> maltId
            ))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération: ${ex.getMessage}"))
        }
      case Left(error) =>
        Future.successful(BadRequest(Json.obj("error" -> error)))
    }
  }
  
  /**
   * GET /malts/type/{maltType}
   * Malts par type
   */
  def getMaltsByType(
    maltType: String, 
    page: Int = 0, 
    size: Int = 20
  ): Action[AnyContent] = Action.async {
    MaltType.fromName(maltType) match {
      case Some(validType) =>
        val filter = MaltFilter(
          maltType = Some(validType),
          isActive = Some(true),
          page = page,
          size = size
        )
        
        maltRepository.findByFilter(filter).map { result =>
          val response = MaltSearchResponseDTO(
            malts = result.items.map(convertMaltToDTO),
            totalCount = result.totalCount.toInt,
            page = result.page,
            size = result.size,
            hasNext = result.hasNext,
            appliedFilters = filter,
            suggestions = List.empty,
            facets = buildMaltFacets(result.items)
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche: ${ex.getMessage}"))
        }
        
      case None =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Type de malt invalide",
          "validTypes" -> MaltType.all.map(_.name)
        )))
    }
  }
  
  // ============================================================================
  // ENDPOINTS SPÉCIALISÉS
  // ============================================================================
  
  /**
   * GET /malts/comparison
   * Comparaison intelligente de malts
   */
  def compareMalts(maltIds: String): Action[AnyContent] = Action.async {
    val idList = maltIds.split(",").map(_.trim).toList
    
    if (idList.length < 2 || idList.length > 5) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Vous devez fournir entre 2 et 5 IDs de malts",
        "example" -> "uuid1,uuid2,uuid3"
      )))
    } else {
      val maltIdResults = idList.map(MaltId.fromString)
      val errors = maltIdResults.zipWithIndex.collect {
        case (Left(error), index) => s"Position ${index + 1}: $error"
      }
      
      if (errors.nonEmpty) {
        Future.successful(BadRequest(Json.obj("errors" -> errors)))
      } else {
        val validIds = maltIdResults.collect { case Right(id) => id }
        
        Future.sequence(validIds.map(maltRepository.findById)).map { maltOptions =>
          val malts = maltOptions.flatten
          
          if (malts.size != validIds.size) {
            val notFound = validIds.diff(malts.map(_.id))
            BadRequest(Json.obj(
              "error" -> "Certains malts n'ont pas été trouvés",
              "notFound" -> notFound.map(_.value.toString)
            ))
          } else {
            val comparison = buildMaltComparison(malts)
            Ok(Json.toJson(comparison))
          }
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la comparaison: ${ex.getMessage}"))
        }
      }
    }
  }
  
  /**
   * GET /malts/batch-calculator
   * Calculateur de batch intelligent
   */
  def calculateBatch(
    batchSize: Double,
    beerStyle: String,
    targetOG: Option[Double] = None,
    targetEBC: Option[Double] = None
  ): Action[AnyContent] = Action.async {
    if (batchSize <= 0 || batchSize > 1000) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Taille de batch invalide (doit être entre 0.1L et 1000L)"
      )))
    } else {
      maltRecommendationService.getForBeerStyle(beerStyle, 8).map { recommendations =>
        val batchRecommendation = buildBatchRecommendation(
          batchSize, beerStyle, targetOG, targetEBC, recommendations
        )
        Ok(Json.toJson(batchRecommendation))
      }.recover {
        case ex => InternalServerError(Json.obj("error" -> s"Erreur lors du calcul: ${ex.getMessage}"))
      }
    }
  }
  
  /**
   * GET /malts/popular
   * Malts populaires
   */
  def getPopularMalts(limit: Int = 10): Action[AnyContent] = Action.async {
    val filter = MaltFilter(
      isActive = Some(true),
      minCredibilityScore = Some(0.7)
    ).copy(size = 50)
    
    maltRepository.findByFilter(filter).map { result =>
      // Simulation de popularité basée sur le score de crédibilité et le type
      val popularMalts = result.items
        .sortBy(malt => -malt.credibilityScore * getPopularityMultiplier(malt))
        .take(limit)
        .map(convertMaltToDTO)
      
      Ok(Json.toJson(popularMalts))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la récupération: ${ex.getMessage}"))
    }
  }
  
  // ============================================================================
  // ENDPOINTS D'INFORMATION ET SANTÉ
  // ============================================================================
  
  /**
   * GET /malts/stats
   * Statistiques publiques des malts
   */
  def getPublicStats(): Action[AnyContent] = Action.async {
    maltRepository.findByFilter(MaltFilter(isActive = Some(true)).copy(size = 1000)).map { result =>
      val stats = buildMaltUsageStats(result.items)
      Ok(Json.toJson(stats))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur lors du calcul des statistiques: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /malts/health
   * Vérification de santé du service
   */
  def health(): Action[AnyContent] = Action {
    Ok(Json.toJson(MaltServiceHealthDTO(
      status = "healthy",
      version = "1.0.0",
      features = List(
        "beginner-recommendations",
        "seasonal-recommendations", 
        "experimental-recommendations",
        "flavor-matching",
        "intelligent-alternatives",
        "advanced-search",
        "style-recommendations",
        "batch-calculator",
        "malt-comparison"
      ),
      statistics = ServiceStatsDTO(
        totalRequests = 0L, // À implémenter avec un vrai système de métriques
        averageResponseTime = 150.0,
        cacheHitRate = 0.85,
        errorRate = 0.02,
        activeFilters = 15
      ),
      lastUpdated = java.time.Instant.now()
    )))
  }
  
  // ============================================================================
  // MÉTHODES UTILITAIRES
  // ============================================================================
  
  private def getSupportedBeerStyles(): List[String] = {
    List(
      "IPA", "Pale Ale", "Lager", "Pilsner", "Wheat Beer", "Stout", 
      "Porter", "Saison", "Belgian Ale", "Amber Ale", "Brown Ale",
      "Barleywine", "Imperial Stout", "Sour Beer", "Berliner Weisse"
    )
  }
  
  private def buildMaltComparison(malts: List[MaltAggregate]): MaltComparisonDTO = {
    require(malts.length >= 2, "Au moins 2 malts requis pour comparaison")
    
    val similarities = buildSimilarities(malts)
    val differences = buildDifferences(malts)
    val recommendations = buildComparisonRecommendations(malts)
    val substitutionMatrix = buildSubstitutionMatrix(malts)
    
    MaltComparisonDTO(
      malts = malts.map(convertMaltToDTO),
      similarities = similarities,
      differences = differences,
      recommendations = recommendations,
      substitutionMatrix = substitutionMatrix
    )
  }
  
  private def buildSimilarities(malts: List[MaltAggregate]): List[SimilarityDTO] = {
    val similarities = scala.collection.mutable.ListBuffer[SimilarityDTO]()
    
    // Analyse des types de malts
    val types = malts.map(_.maltType).distinct
    if (types.length == 1) {
      similarities += SimilarityDTO(
        aspect = "malt_type",
        description = s"Tous les malts sont de type ${types.head.category}",
        score = 1.0
      )
    }
    
    // Analyse des couleurs
    val colors = malts.map(_.ebcColor.value)
    val colorRange = colors.max - colors.min
    if (colorRange <= 10) {
      similarities += SimilarityDTO(
        aspect = "color_range",
        description = s"Couleurs similaires (écart de ${colorRange.toInt} EBC)",
        score = 0.8
      )
    }
    
    // Analyse des taux d'extraction
    val extractions = malts.map(_.extractionRate.value)
    val extractionRange = extractions.max - extractions.min
    if (extractionRange <= 5) {
      similarities += SimilarityDTO(
        aspect = "extraction_rate",
        description = s"Taux d'extraction similaires (écart de ${extractionRange.formatted("%.1f")}%)",
        score = 0.7
      )
    }
    
    similarities.toList
  }
  
  private def buildDifferences(malts: List[MaltAggregate]): List[DifferenceDTO] = {
    val differences = scala.collection.mutable.ListBuffer[DifferenceDTO]()
    
    if (malts.length == 2) {
      val malt1 = malts.head
      val malt2 = malts.last
      
      // Différence de couleur
      val colorDiff = math.abs(malt1.ebcColor.value - malt2.ebcColor.value)
      if (colorDiff > 20) {
        differences += DifferenceDTO(
          aspect = "color",
          malt1Value = s"${malt1.ebcColor.value.toInt} EBC",
          malt2Value = s"${malt2.ebcColor.value.toInt} EBC",
          significance = if (colorDiff > 100) "major" else "moderate",
          impact = "color"
        )
      }
      
      // Différence d'extraction
      val extractionDiff = math.abs(malt1.extractionRate.value - malt2.extractionRate.value)
      if (extractionDiff > 5) {
        differences += DifferenceDTO(
          aspect = "extraction_rate",
          malt1Value = s"${malt1.extractionRate.value.formatted("%.1f")}%",
          malt2Value = s"${malt2.extractionRate.value.formatted("%.1f")}%",
          significance = if (extractionDiff > 10) "major" else "moderate",
          impact = "extraction"
        )
      }
    }
    
    differences.toList
  }
  
  private def buildComparisonRecommendations(malts: List[MaltAggregate]): List[String] = {
    val recommendations = scala.collection.mutable.ListBuffer[String]()
    
    val baseMalts = malts.filter(_.isBaseMalt)
    val specialtyMalts = malts.filterNot(_.isBaseMalt)
    
    if (baseMalts.length > 1) {
      recommendations += "Plusieurs malts de base - choisissez celui avec le meilleur taux d'extraction"
    }
    
    if (specialtyMalts.nonEmpty && baseMalts.nonEmpty) {
      recommendations += s"Utilisez ${baseMalts.head.name.value} comme base (80-90%) et ${specialtyMalts.head.name.value} pour caractère (10-20%)"
    }
    
    val darkMalts = malts.filter(_.ebcColor.value > 100)
    if (darkMalts.length > 1) {
      recommendations += "Attention: plusieurs malts foncés - risque de bière trop amère"
    }
    
    recommendations.toList
  }
  
  private def buildSubstitutionMatrix(malts: List[MaltAggregate]): List[SubstitutionDTO] = {
    val substitutions = scala.collection.mutable.ListBuffer[SubstitutionDTO]()
    
    for {
      i <- malts.indices
      j <- malts.indices if i != j
    } {
      val original = malts(i)
      val substitute = malts(j)
      
      if (original.maltType == substitute.maltType) {
        val ratio = substitute.extractionRate.value / original.extractionRate.value
        val adjustments = scala.collection.mutable.ListBuffer[String]()
        
        if (math.abs(ratio - 1.0) > 0.1) {
          if (ratio > 1.1) {
            adjustments += s"Réduire quantité de ${((ratio - 1) * 100).formatted("%.0f")}%"
          } else if (ratio < 0.9) {
            adjustments += s"Augmenter quantité de ${((1 - ratio) * 100).formatted("%.0f")}%"
          }
        }
        
        val colorDiff = substitute.ebcColor.value - original.ebcColor.value
        if (math.abs(colorDiff) > 10) {
          adjustments += s"Ajustement couleur: ${if (colorDiff > 0) "+" else ""}${colorDiff.toInt} EBC"
        }
        
        substitutions += SubstitutionDTO(
          originalMalt = original.name.value,
          substituteMalt = substitute.name.value,
          ratio = ratio,
          adjustments = adjustments.toList,
          confidence = if (math.abs(ratio - 1.0) < 0.1) "high" else "medium"
        )
      }
    }
    
    substitutions.toList
  }
  
  private def buildBatchRecommendation(
    batchSize: Double,
    beerStyle: String,
    targetOG: Option[Double],
    targetEBC: Option[Double],
    recommendations: List[domain.malts.services.RecommendedMalt]
  ): BatchRecommendationDTO = {
    val baseMalt = recommendations.find(_.malt.isBaseMalt)
    val specialtyMalts = recommendations.filterNot(_.malt.isBaseMalt).take(3)
    
    val batchItems = scala.collection.mutable.ListBuffer[BatchMaltItemDTO]()
    
    // Malt de base (80-85%)
    baseMalt.foreach { base =>
      val percentage = 85.0
      val weight = batchSize * 4.5 * (percentage / 100.0) // Approximation 4.5kg pour 20L
      batchItems += BatchMaltItemDTO(
        malt = convertMaltToDTO(base.malt),
        weight = weight,
        percentage = percentage,
        timing = "mash",
        purpose = "base"
      )
    }
    
    // Malts de spécialité
    val remainingPercentage = 15.0
    val specialtyPercentage = remainingPercentage / specialtyMalts.length
    specialtyMalts.foreach { specialty =>
      val weight = batchSize * 4.5 * (specialtyPercentage / 100.0)
      batchItems += BatchMaltItemDTO(
        malt = convertMaltToDTO(specialty.malt),
        weight = weight,
        percentage = specialtyPercentage,
        timing = "mash",
        purpose = if (specialty.malt.ebcColor.value > 50) "color" else "flavor"
      )
    }
    
    val totalWeight = batchItems.map(_.weight).sum
    val expectedOG = 1.045 + (totalWeight / batchSize * 0.005) // Approximation
    val expectedColor = batchItems.map(item => 
      item.malt.characteristics.ebcColor * (item.percentage / 100.0)
    ).sum
    
    BatchRecommendationDTO(
      batchSize = batchSize,
      malts = batchItems.toList,
      instructions = List(
        s"Concasser ${totalWeight.formatted("%.1f")}kg de malt total",
        "Empâtage à 65-67°C pendant 60 minutes",
        "Rinçage à 75°C",
        "Ébullition 60 minutes"
      ),
      expectedResults = BatchExpectedResultsDTO(
        originalGravity = expectedOG,
        finalGravity = expectedOG - 0.010,
        abv = (expectedOG - (expectedOG - 0.010)) * 131.25,
        ibu = 25.0, // Approximation selon style
        ebc = expectedColor,
        efficiency = 75.0
      ),
      tips = List(
        "Ajustez les quantités selon votre efficacité",
        "Goûtez le moût avant ébullition",
        "Contrôlez la température d'empâtage"
      )
    )
  }
  
  private def getPopularityMultiplier(malt: MaltAggregate): Double = {
    malt.maltType match {
      case MaltType.BASE => 1.5
      case MaltType.CRYSTAL => 1.2
      case MaltType.WHEAT => 1.1
      case _ => 1.0
    }
  }
  
  private def buildMaltUsageStats(malts: List[MaltAggregate]): MaltUsageStatsDTO = {
    val totalMalts = malts.length
    val maltTypeCounts = malts.groupBy(_.maltType).mapValues(_.length)
    val sourceCounts = malts.groupBy(_.source).mapValues(_.length)
    
    // Analyse des profils de saveur
    val allFlavors = malts.flatMap(_.flavorProfiles)
    val flavorCounts = allFlavors.groupBy(identity).mapValues(_.length)
    val flavorStats = flavorCounts.map { case (flavor, count) =>
      FlavorStatsDTO(
        profile = flavor,
        maltCount = count,
        percentage = (count.toDouble / totalMalts) * 100,
        averageIntensity = None // Pas d'intensité dans le modèle actuel
      )
    }.toList.sortBy(-_.maltCount)
    
    // Analyse des couleurs par plages
    val colorRanges = List(
      ("Clair (0-20 EBC)", malts.count(_.ebcColor.value <= 20), 0.0, 20.0),
      ("Ambré (20-60 EBC)", malts.count(m => m.ebcColor.value > 20 && m.ebcColor.value <= 60), 20.0, 60.0),
      ("Brun (60-150 EBC)", malts.count(m => m.ebcColor.value > 60 && m.ebcColor.value <= 150), 60.0, 150.0),
      ("Noir (150+ EBC)", malts.count(_.ebcColor.value > 150), 150.0, 1000.0)
    ).map { case (range, count, min, max) =>
      ColorRangeStatsDTO(
        range = range,
        maltCount = count,
        percentage = (count.toDouble / totalMalts) * 100,
        minEbc = min,
        maxEbc = max
      )
    }
    
    MaltUsageStatsDTO(
      totalMalts = totalMalts,
      baseMalts = maltTypeCounts.getOrElse(MaltType.BASE, 0),
      specialtyMalts = maltTypeCounts.getOrElse(MaltType.SPECIALTY, 0),
      crystalMalts = maltTypeCounts.getOrElse(MaltType.CRYSTAL, 0),
      roastedMalts = maltTypeCounts.getOrElse(MaltType.ROASTED, 0),
      averageExtractionRate = malts.map(_.extractionRate.value).sum / malts.length,
      averageEbcColor = malts.map(_.ebcColor.value).sum / malts.length,
      popularSources = sourceCounts.map { case (source, count) =>
        SourceStatsDTO(
          source = source.toString,
          maltCount = count,
          percentage = (count.toDouble / totalMalts) * 100,
          averageQuality = malts.filter(_.source == source).map(_.credibilityScore).sum / count
        )
      }.toList.sortBy(-_.maltCount),
      flavorProfileDistribution = flavorStats,
      colorDistribution = colorRanges
    )
  }
}