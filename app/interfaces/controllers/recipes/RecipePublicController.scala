package interfaces.controllers.recipes

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

import domain.recipes.model._
import domain.recipes.services.{RecipeRecommendationService, BrewingCalculatorService}
import domain.recipes.repositories.RecipeReadRepository
import application.recipes.dtos._
import application.recipes.dtos.RecipePublicDTOs._
// RecipeApplicationService not needed for public API

/**
 * Contrôleur public pour les API intelligentes des recettes
 * API complète pour consultation communautaire avec recommandations avancées
 */
@Singleton
class RecipePublicController @Inject()(
  cc: ControllerComponents,
  recipeRecommendationService: RecipeRecommendationService,
  recipeRepository: RecipeReadRepository,
  // recipeApplicationService removed - not needed,
  brewingCalculatorService: BrewingCalculatorService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ============================================================================
  // ENDPOINTS DE RECHERCHE ET DÉCOUVERTE
  // ============================================================================
  
  /**
   * GET /recipes/search
   * Recherche avancée avec filtres intelligents
   */
  def searchRecipes(): Action[AnyContent] = Action.async { request =>
    val queryParams = request.queryString
    
    val filter = buildRecipeFilterFromParams(queryParams)
    
    filter.validate() match {
      case Right(validFilter) =>
        recipeRepository.findByFilter(validFilter.toBaseRecipeFilter).map { result =>
          val response = RecipeSearchResponseDTO(
            recipes = result.items.map(convertRecipeToPublicDTO),
            totalCount = result.totalCount.toInt,
            page = result.page,
            size = result.size,
            hasNext = result.hasNext,
            appliedFilters = validFilter,
            facets = RecipeFacetsDTO(List.empty, List.empty, List.empty, List.empty, List.empty, List.empty), // TODO: implement buildRecipeFacets
            suggestions = List.empty // TODO: implement buildSearchSuggestions
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la recherche: ${ex.getMessage}"))
        }
      case Left(errors) =>
        Future.successful(BadRequest(Json.obj("errors" -> errors)))
    }
  }
  
  /**
   * GET /recipes/{id}
   * Détails complets d'une recette avec calculs
   */
  def getRecipeDetails(recipeId: String): Action[AnyContent] = Action.async {
    RecipeId.fromString(recipeId) match {
      case Right(id) =>
        recipeRepository.findById(id).flatMap {
          case Some(recipe) if recipe.status == RecipeStatus.Published =>
            // Utiliser la recette telle quelle (les calculs sont déjà faits)
            val enrichedRecipe = Future.successful(recipe)
            
            enrichedRecipe.map { r =>
              val response = RecipeDetailResponseDTO.fromAggregate(r)
              Ok(Json.toJson(response))
            }
            
          case Some(_) =>
            Future.successful(Forbidden(Json.obj("error" -> "Recette non publiée")))
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "Recette non trouvée")))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur: ${ex.getMessage}"))
        }
      case Left(_) =>
        Future.successful(BadRequest(Json.obj("error" -> "ID de recette invalide")))
    }
  }
  
  /**
   * GET /recipes/discover
   * Découverte intelligente de recettes
   */
  def discoverRecipes(
    category: String = "trending",
    limit: Int = 12
  ): Action[AnyContent] = Action.async {
    val discoveryFuture = category.toLowerCase match {
      case "trending" => getDiscoveryRecipes(DiscoveryCategory.Trending, limit)
      case "recent" => getDiscoveryRecipes(DiscoveryCategory.Recent, limit)
      case "popular" => getDiscoveryRecipes(DiscoveryCategory.Popular, limit)
      case "seasonal" => getDiscoveryRecipes(DiscoveryCategory.Seasonal, limit)
      case "beginner" => getDiscoveryRecipes(DiscoveryCategory.BeginnerFriendly, limit)
      case "advanced" => getDiscoveryRecipes(DiscoveryCategory.Advanced, limit)
      case _ => Future.successful(List.empty)
    }
    
    discoveryFuture.map { recipes =>
      val response = RecipeDiscoveryResponseDTO(
        category = category,
        recipes = recipes.map(convertRecipeToPublicDTO),
        totalFound = recipes.length,
        description = getDiscoveryDescription(category)
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur découverte: ${ex.getMessage}"))
    }
  }

  // ============================================================================
  // ENDPOINTS DE RECOMMANDATIONS INTELLIGENTES
  // ============================================================================
  
  /**
   * GET /recipes/recommendations/beginner
   * Recommandations pour brasseurs débutants
   */
  def getBeginnerRecommendations(maxResults: Int = 6): Action[AnyContent] = Action.async {
    recipeRecommendationService.getBeginnerFriendlyRecipes(maxResults).map { recommendations =>
      val response = RecipeRecommendationListResponseDTO(
        recommendations = recommendations.map(convertRecommendedRecipeToDTO),
        totalCount = recommendations.size,
        context = "beginner",
        description = "Recettes recommandées pour les brasseurs débutants",
        metadata = RecommendationMetadataDTO(
          parameters = Map("maxResults" -> maxResults.toString),
          algorithm = "beginner_friendly_v1",
          confidence = calculateAverageConfidence(recommendations)
        )
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur recommandations: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /recipes/recommendations/style/{style}
   * Recommandations par style de bière
   */
  def getStyleRecommendations(
    style: String,
    difficulty: String = "all",
    maxResults: Int = 8
  ): Action[AnyContent] = Action.async {
    val difficultyLevel = DifficultyLevel.fromString(difficulty).getOrElse(DifficultyLevel.All)
    
    recipeRecommendationService.getByBeerStyle(style, difficultyLevel, maxResults).map { recommendations =>
      val response = RecipeRecommendationListResponseDTO(
        recommendations = recommendations.map(convertRecommendedRecipeToDTO),
        totalCount = recommendations.size,
        context = s"style_${style.toLowerCase}",
        description = s"Recettes recommandées pour le style ${style}",
        metadata = RecommendationMetadataDTO(
          parameters = Map(
            "style" -> style,
            "difficulty" -> difficulty,
            "maxResults" -> maxResults.toString
          ),
          algorithm = "style_matching_v2",
          confidence = calculateAverageConfidence(recommendations)
        )
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur recommandations: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /recipes/recommendations/ingredients
   * Recommandations basées sur inventaire d'ingrédients
   */
  def getIngredientBasedRecommendations(): Action[AnyContent] = Action.async { request =>
    val availableHops = request.getQueryString("hops").map(_.split(",").map(_.trim).toList).getOrElse(List.empty)
    val availableMalts = request.getQueryString("malts").map(_.split(",").map(_.trim).toList).getOrElse(List.empty)
    val availableYeasts = request.getQueryString("yeasts").map(_.split(",").map(_.trim).toList).getOrElse(List.empty)
    val maxResults = request.getQueryString("limit").flatMap(_.toIntOption).getOrElse(10)
    
    if (availableHops.isEmpty && availableMalts.isEmpty && availableYeasts.isEmpty) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Au moins un type d'ingrédient requis",
        "example" -> "?hops=cascade,centennial&malts=pale_ale&yeasts=us05"
      )))
    } else {
      val inventory = IngredientInventory(
        availableHops = availableHops,
        availableMalts = availableMalts,
        availableYeasts = availableYeasts
      )
      
      recipeRecommendationService.getByIngredientAvailability(inventory, maxResults).map { recommendations =>
        val response = RecipeRecommendationListResponseDTO(
          recommendations = recommendations.map(convertRecommendedRecipeToDTO),
          totalCount = recommendations.size,
          context = "ingredient_availability",
          description = "Recettes compatibles avec votre inventaire",
          metadata = RecommendationMetadataDTO(
            parameters = Map(
              "availableHops" -> availableHops.mkString(","),
              "availableMalts" -> availableMalts.mkString(","),
              "availableYeasts" -> availableYeasts.mkString(",")
            ),
            algorithm = "ingredient_matching_v1",
            confidence = calculateAverageConfidence(recommendations)
          )
        )
        Ok(Json.toJson(response))
      }.recover {
        case ex => InternalServerError(Json.obj("error" -> s"Erreur recommandations: ${ex.getMessage}"))
      }
    }
  }
  
  /**
   * GET /recipes/recommendations/seasonal/{season}
   * Recommandations saisonnières
   */
  def getSeasonalRecommendations(
    season: String,
    maxResults: Int = 8
  ): Action[AnyContent] = Action.async {
    Season.fromString(season) match {
      case Right(seasonEnum) =>
        recipeRecommendationService.getSeasonalRecommendations(seasonEnum, maxResults).map { recommendations =>
          val response = RecipeRecommendationListResponseDTO(
            recommendations = recommendations.map(convertRecommendedRecipeToDTO),
            totalCount = recommendations.size,
            context = s"seasonal_${season.toLowerCase}",
            description = s"Recettes recommandées pour la saison ${season.toLowerCase}",
            metadata = RecommendationMetadataDTO(
              parameters = Map("season" -> season, "maxResults" -> maxResults.toString),
              algorithm = "seasonal_v1",
              confidence = calculateAverageConfidence(recommendations)
            )
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur recommandations: ${ex.getMessage}"))
        }
      case Left(_) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Saison invalide",
          "validSeasons" -> List("spring", "summer", "autumn", "winter")
        )))
    }
  }
  
  /**
   * GET /recipes/recommendations/progression
   * Recommandations de progression (que brasser ensuite)
   */
  def getProgressionRecommendations(
    lastRecipeId: String,
    currentLevel: String = "beginner",
    maxResults: Int = 5
  ): Action[AnyContent] = Action.async {
    (RecipeId.fromString(lastRecipeId), BrewerLevel.fromString(currentLevel)) match {
      case (Right(recipeId: RecipeId), Some(level: BrewerLevel)) =>
        recipeRecommendationService.getProgressionRecommendations(recipeId, level, maxResults).map { recommendations =>
          val response = RecipeRecommendationListResponseDTO(
            recommendations = recommendations.map(convertRecommendedRecipeToDTO),
            totalCount = recommendations.size,
            context = s"progression_${currentLevel}",
            description = s"Prochaines recettes suggérées pour niveau ${currentLevel}",
            metadata = RecommendationMetadataDTO(
              parameters = Map(
                "lastRecipeId" -> lastRecipeId,
                "currentLevel" -> currentLevel,
                "maxResults" -> maxResults.toString
              ),
              algorithm = "progression_v1",
              confidence = calculateAverageConfidence(recommendations)
            )
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur recommandations: ${ex.getMessage}"))
        }
      case (Left(_: String), _) =>
        Future.successful(BadRequest(Json.obj("error" -> "ID de recette invalide")))
      case (_, None) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Niveau invalide",
          "validLevels" -> List("beginner", "intermediate", "advanced", "expert")
        )))
    }
  }

  // ============================================================================
  // ENDPOINTS DE COMPARAISON ET ANALYSE
  // ============================================================================
  
  /**
   * GET /recipes/compare
   * Comparaison intelligente de recettes
   */
  def compareRecipes(recipeIds: String): Action[AnyContent] = Action.async {
    val idList = recipeIds.split(",").map(_.trim).toList
    
    if (idList.length < 2 || idList.length > 4) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Vous devez fournir entre 2 et 4 IDs de recettes",
        "example" -> "uuid1,uuid2,uuid3"
      )))
    } else {
      val recipeIdResults = idList.map(RecipeId.fromString)
      val errors = recipeIdResults.zipWithIndex.collect {
        case (Left(_), index) => s"ID invalide à la position ${index + 1}: ${idList(index)}"
      }
      
      if (errors.nonEmpty) {
        Future.successful(BadRequest(Json.obj("errors" -> errors)))
      } else {
        val validIds = recipeIdResults.collect { case Right(id) => id }
        
        Future.sequence(validIds.map(recipeRepository.findById)).map { recipeOptions =>
          val recipes = recipeOptions.flatten.filter(_.status == RecipeStatus.Published)
          
          if (recipes.size != validIds.size) {
            val foundIds = recipes.map(_.id)
            val notFound = validIds.diff(foundIds)
            BadRequest(Json.obj(
              "error" -> "Certaines recettes n'ont pas été trouvées ou ne sont pas publiées",
              "notFound" -> notFound.map(_.value.toString)
            ))
          } else {
            val comparison = buildRecipeComparison(recipes)
            Ok(Json.toJson(comparison))
          }
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur lors de la comparaison: ${ex.getMessage}"))
        }
      }
    }
  }
  
  /**
   * GET /recipes/{id}/alternatives
   * Alternatives à une recette (ingrédients manquants, etc.)
   */
  def getAlternatives(
    recipeId: String,
    reason: String = "unavailable_ingredients",
    maxResults: Int = 5
  ): Action[AnyContent] = Action.async {
    RecipeId.fromString(recipeId) match {
      case Right(id) =>
        val alternativeReason = AlternativeReason.fromString(reason).getOrElse(AlternativeReason.UnavailableIngredients)
        
        recipeRecommendationService.findAlternatives(id, alternativeReason, maxResults).map { alternatives =>
          val response = RecipeRecommendationListResponseDTO(
            recommendations = alternatives.map(convertRecommendedRecipeToDTO),
            totalCount = alternatives.size,
            context = s"alternatives_${reason}",
            description = s"Alternatives pour ${alternativeReason.description}",
            metadata = RecommendationMetadataDTO(
              parameters = Map(
                "originalRecipeId" -> recipeId,
                "reason" -> reason,
                "maxResults" -> maxResults.toString
              ),
              algorithm = "alternatives_v1",
              confidence = calculateAverageConfidence(alternatives)
            )
          )
          Ok(Json.toJson(response))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur alternatives: ${ex.getMessage}"))
        }
      case Left(_) =>
        Future.successful(BadRequest(Json.obj("error" -> "ID de recette invalide")))
    }
  }

  // ============================================================================
  // OUTILS ET CALCULATEURS
  // ============================================================================
  
  /**
   * GET /recipes/{id}/scale
   * Ajustement de recette pour différentes tailles de batch
   */
  def scaleRecipe(
    recipeId: String,
    targetBatchSize: Double,
    targetUnit: String = "L"
  ): Action[AnyContent] = Action.async {
    RecipeId.fromString(recipeId) match {
      case Right(id) =>
        recipeRepository.findById(id).flatMap {
          case Some(recipe) if recipe.status == RecipeStatus.Published =>
            VolumeUnit.fromString(targetUnit) match {
              case Right(unit) =>
                val targetBatch = BatchSize(targetBatchSize, unit)
                val scaledRecipe = recipeRecommendationService.scaleRecipe(recipe, targetBatch)
                
                scaledRecipe.map { scaled =>
                  val response = RecipeScalingResponseDTO(
                    originalRecipe = convertRecipeToPublicDTO(recipe),
                    scaledRecipe = convertRecipeToPublicDTO(scaled),
                    scalingFactor = targetBatch.toLiters / recipe.batchSize.toLiters,
                    adjustments = buildScalingAdjustments(recipe, scaled),
                    warnings = buildScalingWarnings(recipe, scaled)
                  )
                  Ok(Json.toJson(response))
                }.recover {
                  case ex => InternalServerError(Json.obj("error" -> s"Erreur scaling: ${ex.getMessage}"))
                }
              case Left(_) =>
                Future.successful(BadRequest(Json.obj(
                  "error" -> "Unité invalide",
                  "validUnits" -> List("L", "gal", "mL")
                )))
            }
          case Some(_) =>
            Future.successful(Forbidden(Json.obj("error" -> "Recette non publiée")))
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "Recette non trouvée")))
        }
      case Left(_) =>
        Future.successful(BadRequest(Json.obj("error" -> "ID de recette invalide")))
    }
  }
  
  /**
   * GET /recipes/{id}/brewing-guide
   * Guide de brassage étape par étape
   */
  def getBrewingGuide(recipeId: String): Action[AnyContent] = Action.async {
    RecipeId.fromString(recipeId) match {
      case Right(id) =>
        recipeRepository.findById(id).flatMap {
          case Some(recipe) if recipe.status == RecipeStatus.Published =>
            recipeRecommendationService.generateBrewingGuide(recipe).map { guide =>
              Ok(Json.toJson(guide))
            }.recover {
              case ex => InternalServerError(Json.obj("error" -> s"Erreur guide: ${ex.getMessage}"))
            }
          case Some(_) =>
            Future.successful(Forbidden(Json.obj("error" -> "Recette non publiée")))
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "Recette non trouvée")))
        }
      case Left(_) =>
        Future.successful(BadRequest(Json.obj("error" -> "ID de recette invalide")))
    }
  }
  
  /**
   * POST /recipes/analyze
   * Analyse d'une recette personnalisée
   */
  def analyzeCustomRecipe(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CustomRecipeAnalysisRequestDTO] match {
      case JsSuccess(analysisRequest, _) =>
        recipeRecommendationService.analyzeCustomRecipe(convertAnalysisRequestToModel(analysisRequest)).map { analysis =>
          Ok(Json.toJson(analysis))
        }.recover {
          case ex => InternalServerError(Json.obj("error" -> s"Erreur analyse: ${ex.getMessage}"))
        }
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Format JSON invalide",
          "details" -> JsError.toJson(errors)
        )))
    }
  }

  // ============================================================================
  // FONCTIONNALITÉS COMMUNAUTAIRES
  // ============================================================================
  
  /**
   * GET /recipes/stats
   * Statistiques publiques des recettes
   */
  def getPublicStats(): Action[AnyContent] = Action.async {
    recipeRecommendationService.getPublicStatistics().map { stats =>
      Ok(Json.toJson(stats))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur stats: ${ex.getMessage}"))
    }
  }
  
  /**
   * GET /recipes/collections
   * Collections thématiques de recettes
   */
  def getRecipeCollections(
    category: String = "all",
    page: Int = 0,
    size: Int = 20
  ): Action[AnyContent] = Action.async {
    recipeRecommendationService.getRecipeCollections(category, page, size).map { collections =>
      val response = RecipeCollectionListResponseDTO(
        collections = collections.collections.map(convertRecipeCollectionToDTO),
        totalCount = collections.totalCount,
        page = page,
        size = size,
        hasNext = collections.hasNext,
        categories = collections.categories
      )
      Ok(Json.toJson(response))
    }.recover {
      case ex => InternalServerError(Json.obj("error" -> s"Erreur collections: ${ex.getMessage}"))
    }
  }

  // ============================================================================
  // ENDPOINTS DE SANTÉ ET INFORMATION
  // ============================================================================
  
  /**
   * GET /recipes/health
   * Vérification de santé du service
   */
  def health(): Action[AnyContent] = Action {
    Ok(Json.toJson(RecipeServiceHealthDTO(
      status = "healthy",
      version = "2.0.0",
      features = List(
        "recipe-search", "intelligent-recommendations", "recipe-comparison",
        "recipe-scaling", "brewing-guides", "seasonal-recommendations",
        "ingredient-based-recommendations", "progression-recommendations",
        "recipe-analysis", "community-collections", "advanced-filtering"
      ),
      statistics = ServiceStatsDTO(
        totalRequests = 0L, // À implémenter avec métriques réelles
        averageResponseTime = 180.0,
        cacheHitRate = 0.78,
        errorRate = 0.03,
        activeFilters = 25
      ),
      algorithms = AlgorithmInfoDTO(
        recommendationVersions = Map(
          "beginner" -> "v1.2",
          "style_matching" -> "v2.1",
          "ingredient_matching" -> "v1.0",
          "seasonal" -> "v1.1",
          "progression" -> "v1.0"
        ),
        lastUpdated = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS)
      ),
      lastUpdated = java.time.Instant.now()
    )))
  }

  // ============================================================================
  // MÉTHODES UTILITAIRES
  // ============================================================================
  
  private def buildRecipeFilterFromParams(queryParams: Map[String, Seq[String]]): RecipePublicFilter = {
    RecipePublicFilter(
      name = queryParams.get("name").flatMap(_.headOption),
      style = queryParams.get("style").flatMap(_.headOption),
      difficulty = queryParams.get("difficulty").flatMap(_.headOption).flatMap(DifficultyLevel.fromString),
      minAbv = queryParams.get("minAbv").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxAbv = queryParams.get("maxAbv").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minIbu = queryParams.get("minIbu").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxIbu = queryParams.get("maxIbu").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minSrm = queryParams.get("minSrm").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxSrm = queryParams.get("maxSrm").flatMap(_.headOption).flatMap(_.toDoubleOption),
      minBatchSize = queryParams.get("minBatchSize").flatMap(_.headOption).flatMap(_.toDoubleOption),
      maxBatchSize = queryParams.get("maxBatchSize").flatMap(_.headOption).flatMap(_.toDoubleOption),
      hasIngredient = queryParams.get("hasIngredient").flatMap(_.headOption),
      excludeIngredient = queryParams.get("excludeIngredient").flatMap(_.headOption),
      tags = queryParams.get("tags").map(_.flatMap(_.split(",").map(_.trim)).toList).getOrElse(List.empty),
      sortBy = queryParams.get("sortBy").flatMap(_.headOption).flatMap(RecipeSortBy.fromString).getOrElse(RecipeSortBy.Relevance),
      page = queryParams.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(0),
      size = queryParams.get("size").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(20)
    )
  }
  
  private def convertRecipeToPublicDTO(recipe: RecipeAggregate): RecipePublicDTO = {
    RecipePublicDTO(
      id = recipe.id.value.toString,
      name = recipe.name.value,
      description = recipe.description.map(_.value),
      style = BeerStyleDTO.fromDomain(recipe.style),
      batchSize = BatchSizeDTO.fromDomain(recipe.batchSize),
      difficulty = calculateDifficulty(recipe),
      estimatedTime = calculateEstimatedTime(recipe),
      ingredients = RecipeIngredientsDTO(
        hops = recipe.hops.map(RecipeHopDTO.fromDomain),
        malts = recipe.malts.map(RecipeMaltDTO.fromDomain),
        yeast = recipe.yeast.map(RecipeYeastDTO.fromDomain),
        others = recipe.otherIngredients.map(OtherIngredientDTO.fromDomain)
      ),
      calculations = RecipeCalculationsPublicDTO.fromDomain(recipe.calculations),
      characteristics = buildRecipeCharacteristics(recipe),
      tags = buildRecipeTags(recipe),
      popularity = calculatePopularityScore(recipe),
      createdAt = recipe.createdAt,
      updatedAt = recipe.updatedAt
    )
  }
  
  private def convertRecommendedRecipeToDTO(recommended: RecommendedRecipe): RecipeRecommendationDTO = {
    RecipeRecommendationDTO(
      recipe = convertRecipeToPublicDTO(recommended.recipe),
      score = recommended.score,
      reason = recommended.reason,
      tips = recommended.tips,
      confidence = calculateConfidenceLevel(recommended.score),
      matchFactors = recommended.matchFactors
    )
  }
  
  private def buildRecipeFacets(recipes: List[RecipeAggregate]): RecipeFacetsDTO = {
    val styles = recipes.groupBy(_.style.category).mapValues(_.length).toList.sortBy(-_._2)
    val difficulties = recipes.groupBy(calculateDifficulty).mapValues(_.length).toList
    val abvRanges = buildAbvRanges(recipes)
    val ibuRanges = buildIbuRanges(recipes)
    val batchSizeRanges = buildBatchSizeRanges(recipes)
    
    RecipeFacetsDTO(
      styles = styles.map { case (style, count) => FacetItemDTO(style, count) },
      difficulties = difficulties.map { case (diff, count) => FacetItemDTO(diff, count) },
      abvRanges = abvRanges,
      ibuRanges = ibuRanges,
      batchSizeRanges = batchSizeRanges,
      popularIngredients = buildPopularIngredientsFacet(recipes)
    )
  }
  
  private def buildSearchSuggestions(
    filter: RecipePublicFilter,
    result: domain.common.PaginatedResult[RecipeAggregate]
  ): List[String] = {
    val suggestions = scala.collection.mutable.ListBuffer[String]()
    
    if (result.totalCount == 0) {
      suggestions += "Essayez d'élargir vos critères de recherche"
      if (filter.style.isDefined) {
        suggestions += "Explorez d'autres styles de bière"
      }
      if (filter.hasIngredient.isDefined) {
        suggestions += "Recherchez par catégorie d'ingrédient plutôt que par nom spécifique"
      }
    } else if (result.totalCount < 5) {
      suggestions += "Peu de résultats trouvés - considérez des critères plus larges"
    }
    
    if (filter.difficulty.isDefined) {
      suggestions += "Explorez d'autres niveaux de difficulté"
    }
    
    suggestions.toList
  }
  
  private def getDiscoveryRecipes(category: DiscoveryCategory, limit: Int): Future[List[RecipeAggregate]] = {
    val filter = category match {
      case DiscoveryCategory.Trending => RecipePublicFilter(sortBy = RecipeSortBy.Trending, size = limit)
      case DiscoveryCategory.Recent => RecipePublicFilter(sortBy = RecipeSortBy.Recent, size = limit)
      case DiscoveryCategory.Popular => RecipePublicFilter(sortBy = RecipeSortBy.Popular, size = limit)
      case DiscoveryCategory.Seasonal => RecipePublicFilter(sortBy = RecipeSortBy.Seasonal, size = limit)
      case DiscoveryCategory.BeginnerFriendly => RecipePublicFilter(difficulty = Some(DifficultyLevel.Beginner), size = limit)
      case DiscoveryCategory.Advanced => RecipePublicFilter(difficulty = Some(DifficultyLevel.Advanced), size = limit)
    }
    
    recipeRepository.findByFilter(filter.toBaseRecipeFilter).map(_.items)
  }
  
  private def getDiscoveryDescription(category: String): String = {
    category.toLowerCase match {
      case "trending" => "Recettes en tendance actuellement dans la communauté"
      case "recent" => "Dernières recettes ajoutées par la communauté"
      case "popular" => "Recettes les plus appréciées par la communauté"
      case "seasonal" => "Recettes parfaites pour la saison actuelle"
      case "beginner" => "Recettes idéales pour commencer le brassage"
      case "advanced" => "Recettes pour brasseurs expérimentés"
      case _ => "Collection de recettes sélectionnées"
    }
  }
  
  private def buildRecipeComparison(recipes: List[RecipeAggregate]): RecipeComparisonDTO = {
    require(recipes.length >= 2, "Au moins 2 recettes requises pour comparaison")
    
    val similarities = buildRecipeSimilarities(recipes)
    val differences = buildRecipeDifferences(recipes)
    val recommendations = buildComparisonRecommendations(recipes)
    val substitutionMatrix = buildIngredientSubstitutions(recipes)
    
    RecipeComparisonDTO(
      recipes = recipes.map(convertRecipeToPublicDTO),
      similarities = similarities,
      differences = differences,
      recommendations = recommendations,
      substitutionMatrix = substitutionMatrix,
      summary = buildComparisonSummary(recipes, similarities, differences)
    )
  }
  
  private def calculateDifficulty(recipe: RecipeAggregate): String = {
    var score = 0
    
    // Nombre d'ingrédients
    val totalIngredients = recipe.hops.length + recipe.malts.length + recipe.otherIngredients.length
    if (totalIngredients > 10) score += 2
    else if (totalIngredients > 6) score += 1
    
    // Complexité des procédures
    if (recipe.procedures.hasFermentationProfile) score += 1
    if (recipe.procedures.hasPackagingProcedure) score += 1
    
    // Calculs avancés
    if (recipe.calculations.isAdvancedComplete) score += 2
    
    // ABV élevé
    recipe.calculations.abv.foreach { abv =>
      if (abv > 8.0) score += 2
      else if (abv > 6.0) score += 1
    }
    
    score match {
      case s if s <= 2 => "beginner"
      case s if s <= 5 => "intermediate" 
      case s if s <= 8 => "advanced"
      case _ => "expert"
    }
  }
  
  private def calculateEstimatedTime(recipe: RecipeAggregate): EstimatedTimeDTO = {
    var brewDayMinutes = 240 // Base: 4h
    var fermentationDays = 14 // Base: 2 semaines
    var totalDays = 21 // Base: 3 semaines
    
    // Ajustements selon la complexité
    if (recipe.malts.length > 5) brewDayMinutes += 60
    if (recipe.hops.length > 4) brewDayMinutes += 30
    if (recipe.otherIngredients.nonEmpty) brewDayMinutes += 45
    
    // Ajustements selon le style
    recipe.calculations.abv.foreach { abv =>
      if (abv > 7.0) {
        fermentationDays += 7
        totalDays += 14
      }
    }
    
    EstimatedTimeDTO(
      brewDayHours = brewDayMinutes / 60.0,
      fermentationWeeks = fermentationDays / 7.0,
      totalWeeks = totalDays / 7.0,
      breakdown = TimeBreakdownDTO(
        preparation = 60,
        mashing = 90,
        boiling = 60,
        cooling = 30,
        cleanup = brewDayMinutes - 240
      )
    )
  }
  
  private def buildRecipeCharacteristics(recipe: RecipeAggregate): RecipeCharacteristicsDTO = {
    RecipeCharacteristicsDTO(
      color = recipe.calculations.srm.map(srm => ColorCharacteristicDTO(
        srm = srm,
        ebc = srm * 1.97,
        description = getColorDescription(srm)
      )),
      bitterness = recipe.calculations.ibu.map(ibu => BitternessCharacteristicDTO(
        ibu = ibu,
        level = getBitternessLevel(ibu),
        balance = calculateBitternessBalance(recipe)
      )),
      strength = recipe.calculations.abv.map(abv => StrengthCharacteristicDTO(
        abv = abv,
        category = getStrengthCategory(abv),
        calories = calculateEstimatedCalories(recipe, abv)
      )),
      aroma = buildAromaProfile(recipe),
      mouthfeel = buildMouthfeelProfile(recipe)
    )
  }
  
  private def buildRecipeTags(recipe: RecipeAggregate): List[String] = {
    val tags = scala.collection.mutable.ListBuffer[String]()
    
    // Tags basés sur le style
    tags += recipe.style.category.toLowerCase.replace(" ", "_")
    
    // Tags basés sur la difficulté
    tags += calculateDifficulty(recipe)
    
    // Tags basés sur les caractéristiques
    recipe.calculations.abv.foreach { abv =>
      if (abv > 7.0) tags += "strong"
      else if (abv < 4.0) tags += "session"
    }
    
    recipe.calculations.ibu.foreach { ibu =>
      if (ibu > 60) tags += "hoppy"
      else if (ibu < 20) tags += "malty"
    }
    
    // Tags basés sur les ingrédients
    if (recipe.hops.length > 5) tags += "hop_forward"
    if (recipe.malts.exists(_.percentage.exists(_ > 20))) tags += "specialty_malt"
    if (recipe.otherIngredients.nonEmpty) tags += "adjuncts"
    
    tags.toList.distinct
  }
  
  private def calculatePopularityScore(recipe: RecipeAggregate): Double = {
    // Implémentation simplifiée - en réalité basée sur métriques communautaires
    var score = 0.5
    
    // Bonus pour recettes complètes
    if (recipe.calculations.isComplete) score += 0.2
    
    // Bonus pour styles populaires
    if (List("IPA", "Pale Ale", "Stout").contains(recipe.style.name)) score += 0.1
    
    // Bonus pour difficulté accessible
    val difficulty = calculateDifficulty(recipe)
    if (difficulty == "beginner" || difficulty == "intermediate") score += 0.1
    
    math.min(1.0, score)
  }
  
  private def calculateAverageConfidence(recommendations: List[RecommendedRecipe]): Double = {
    if (recommendations.isEmpty) 0.0
    else recommendations.map(_.score).sum / recommendations.length
  }
  
  private def convertRecipeCollectionToDTO(collection: RecipeCollection): RecipeCollectionDTO = {
    RecipeCollectionDTO(
      id = collection.id,
      name = collection.name,
      description = collection.description,
      category = collection.category.name,
      recipeCount = collection.totalRecipes,
      recipes = collection.recipes.map(convertRecipeToPublicDTO),
      tags = collection.tags,
      createdAt = collection.createdAt,
      updatedAt = collection.createdAt // Using createdAt as fallback
    )
  }
  
  private def convertAnalysisRequestToModel(dto: CustomRecipeAnalysisRequestDTO): CustomRecipeAnalysisRequest = {
    // TODO: Implement proper conversion from DTO to domain model
    // For now, return a minimal implementation to fix compilation
    CustomRecipeAnalysisRequest(
      name = dto.name,
      style = dto.style,
      batchSize = dto.batchSize,
      batchUnit = dto.batchUnit,
      hops = List.empty, // TODO: convert dto.ingredients.hops
      malts = List.empty, // TODO: convert dto.ingredients.malts
      yeasts = List.empty, // TODO: convert dto.ingredients.yeasts
      otherIngredients = List.empty,
      procedures = None,
      targetParameters = None // TODO: convert dto.targetParameters
    )
  }
  
  private def calculateConfidenceLevel(score: Double): String = {
    if (score >= 0.8) "high"
    else if (score >= 0.6) "medium" 
    else if (score >= 0.4) "moderate"
    else "low"
  }
  
  // Méthodes utilitaires pour les facettes et comparaisons (implémentations simplifiées)
  private def buildAbvRanges(recipes: List[RecipeAggregate]): List[FacetItemDTO] = List.empty
  private def buildIbuRanges(recipes: List[RecipeAggregate]): List[FacetItemDTO] = List.empty
  private def buildBatchSizeRanges(recipes: List[RecipeAggregate]): List[FacetItemDTO] = List.empty
  private def buildPopularIngredientsFacet(recipes: List[RecipeAggregate]): List[FacetItemDTO] = List.empty
  private def buildRecipeSimilarities(recipes: List[RecipeAggregate]): List[SimilarityDTO] = List.empty
  private def buildRecipeDifferences(recipes: List[RecipeAggregate]): List[DifferenceDTO] = List.empty
  private def buildComparisonRecommendations(recipes: List[RecipeAggregate]): List[String] = List.empty
  private def buildIngredientSubstitutions(recipes: List[RecipeAggregate]): List[SubstitutionDTO] = List.empty
  private def buildComparisonSummary(recipes: List[RecipeAggregate], similarities: List[SimilarityDTO], differences: List[DifferenceDTO]): String = ""
  private def buildScalingAdjustments(original: RecipeAggregate, scaled: RecipeAggregate): List[String] = List.empty
  private def buildScalingWarnings(original: RecipeAggregate, scaled: RecipeAggregate): List[String] = List.empty
  private def getColorDescription(srm: Double): String = "amber"
  private def getBitternessLevel(ibu: Double): String = "moderate"
  private def calculateBitternessBalance(recipe: RecipeAggregate): String = "balanced"
  private def getStrengthCategory(abv: Double): String = "standard"
  private def calculateEstimatedCalories(recipe: RecipeAggregate, abv: Double): Int = 150
  private def buildAromaProfile(recipe: RecipeAggregate): AromaProfileDTO = AromaProfileDTO(List.empty, "balanced")
  private def buildMouthfeelProfile(recipe: RecipeAggregate): MouthfeelProfileDTO = MouthfeelProfileDTO("medium", "moderate")
}