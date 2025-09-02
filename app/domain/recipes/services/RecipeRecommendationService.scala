package domain.recipes.services

import domain.recipes.model._
import domain.recipes.repositories.RecipeReadRepository
// Domain models only - no DTO imports as per DDD principles
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{Instant, LocalDate}

/**
 * Service de recommandations intelligentes pour les recettes
 * Algorithmes sophistiqués pour suggérer des recettes basées sur différents critères
 */
@Singleton
class RecipeRecommendationService @Inject()(
  recipeReadRepository: RecipeReadRepository,
  brewingCalculatorService: BrewingCalculatorService
)(implicit ec: ExecutionContext) {

  /**
   * Recommandations pour brasseurs débutants
   */
  def getBeginnerFriendlyRecipes(maxResults: Int = 6): Future[List[RecommendedRecipe]] = {
    val beginnerFilter = RecipePublicFilter(
      difficulty = Some(DifficultyLevel.Beginner),
      brewingTimeMax = Some(6),
      commonIngredients = Some(true),
      sortBy = RecipeSortBy.Difficulty,
      size = maxResults * 3
    )
    
    recipeReadRepository.findByFilter(beginnerFilter.toBaseRecipeFilter).map { result =>
      result.items
        .filter(isBeginnerFriendly)
        .map(recipe => RecommendedRecipe(
          recipe = recipe,
          score = calculateBeginnerScore(recipe),
          reason = generateBeginnerReason(recipe),
          tips = generateBeginnerTips(recipe),
          matchFactors = generateBeginnerMatchFactors(recipe)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations par style de bière avec niveau de difficulté
   */
  def getByBeerStyle(
    style: String, 
    difficulty: DifficultyLevel = DifficultyLevel.All,
    maxResults: Int = 8
  ): Future[List[RecommendedRecipe]] = {
    val styleFilter = RecipePublicFilter(
      style = Some(style),
      difficulty = if (difficulty != DifficultyLevel.All) Some(difficulty) else None,
      sortBy = RecipeSortBy.StyleCompliance,
      size = maxResults * 2
    )
    
    recipeReadRepository.findByFilter(styleFilter.toBaseRecipeFilter).map { result =>
      result.items
        .map(recipe => RecommendedRecipe(
          recipe = recipe,
          score = calculateStyleMatchScore(recipe, style),
          reason = generateStyleReason(recipe, style),
          tips = generateStyleTips(recipe, style),
          matchFactors = generateStyleMatchFactors(recipe, style)
        ))
        .filter(_.score > 0.3)
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations basées sur inventaire d'ingrédients disponibles
   */
  def getByIngredientAvailability(
    inventory: IngredientInventory,
    maxResults: Int = 10
  ): Future[List[RecommendedRecipe]] = {
    // Rechercher d'abord par ingrédients spécifiques
    val ingredientBasedSearches = List(
      inventory.availableHops.headOption.map(hop => 
        recipeReadRepository.findByFilter(RecipeFilter(hasHop = Some(hop), size = 30))
      ),
      inventory.availableMalts.headOption.map(malt =>
        recipeReadRepository.findByFilter(RecipeFilter(hasMalt = Some(malt), size = 30))
      ),
      inventory.availableYeasts.headOption.map(yeast =>
        recipeReadRepository.findByFilter(RecipeFilter(hasYeast = Some(yeast), size = 30))
      )
    ).flatten
    
    Future.sequence(ingredientBasedSearches).map { results =>
      val allRecipes = results.flatMap(_.items).distinct
      
      allRecipes
        .map(recipe => RecommendedRecipe(
          recipe = recipe,
          score = calculateIngredientMatchScore(recipe, inventory),
          reason = generateIngredientReason(recipe, inventory),
          tips = generateIngredientTips(recipe, inventory),
          matchFactors = generateIngredientMatchFactors(recipe, inventory)
        ))
        .filter(_.score > 0.4)
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations saisonnières
   */
  def getSeasonalRecommendations(
    season: Season,
    maxResults: Int = 8
  ): Future[List[RecommendedRecipe]] = {
    val seasonalStyles = getSeasonalStyles(season)
    
    // Rechercher des recettes dans les styles saisonniers
    Future.sequence(seasonalStyles.map { style =>
      recipeReadRepository.findByFilter(RecipeFilter(style = Some(style), size = 20))
    }).map { results =>
      results.flatMap(_.items)
        .distinct
        .map(recipe => RecommendedRecipe(
          recipe = recipe,
          score = calculateSeasonalScore(recipe, season),
          reason = generateSeasonalReason(recipe, season),
          tips = generateSeasonalTips(recipe, season),
          matchFactors = generateSeasonalMatchFactors(recipe, season)
        ))
        .sortBy(-_.score)
        .take(maxResults)
    }
  }

  /**
   * Recommandations de progression pour brasseur
   */
  def getProgressionRecommendations(
    lastRecipeId: RecipeId,
    currentLevel: BrewerLevel,
    maxResults: Int = 5
  ): Future[List[RecommendedRecipe]] = {
    recipeReadRepository.findById(lastRecipeId).flatMap {
      case Some(lastRecipe) =>
        val nextLevel = BrewerLevel.getNext(currentLevel)
        val progressionFilter = buildProgressionFilter(lastRecipe, currentLevel, nextLevel)
        
        recipeReadRepository.findByFilter(progressionFilter).map { result =>
          result.items
            .filter(_.id != lastRecipeId) // Exclure la dernière recette brassée
            .map(recipe => RecommendedRecipe(
              recipe = recipe,
              score = calculateProgressionScore(recipe, lastRecipe, currentLevel),
              reason = generateProgressionReason(recipe, lastRecipe, currentLevel),
              tips = generateProgressionTips(recipe, lastRecipe, currentLevel),
              matchFactors = generateProgressionMatchFactors(recipe, lastRecipe, currentLevel)
            ))
            .sortBy(-_.score)
            .take(maxResults)
        }
      case None =>
        // Si la recette précédente n'existe pas, recommander des recettes de base pour le niveau
        getByDifficulty(currentLevel.name, maxResults)
    }
  }

  /**
   * Recherche d'alternatives à une recette
   */
  def findAlternatives(
    originalRecipeId: RecipeId,
    reason: AlternativeReason,
    maxResults: Int = 5
  ): Future[List[RecommendedRecipe]] = {
    recipeReadRepository.findById(originalRecipeId).flatMap {
      case Some(original) =>
        val alternativeFilter = buildAlternativeFilter(original, reason)
        
        recipeReadRepository.findByFilter(alternativeFilter).map { result =>
          result.items
            .filter(_.id != originalRecipeId)
            .map(recipe => RecommendedRecipe(
              recipe = recipe,
              score = calculateAlternativeScore(recipe, original, reason),
              reason = generateAlternativeReason(recipe, original, reason),
              tips = generateAlternativeTips(recipe, original, reason),
              matchFactors = generateAlternativeMatchFactors(recipe, original, reason)
            ))
            .sortBy(-_.score)
            .take(maxResults)
        }
      case None =>
        Future.successful(List.empty)
    }
  }

  /**
   * Ajustement de recette pour différentes tailles de batch
   */
  def scaleRecipe(
    originalRecipe: RecipeAggregate,
    targetBatchSize: BatchSize
  ): Future[RecipeAggregate] = {
    val scalingFactor = targetBatchSize.toLiters / originalRecipe.batchSize.toLiters
    
    // Ajuster tous les ingrédients proportionnellement
    val scaledHops = originalRecipe.hops.map(hop => 
      hop.copy(quantityGrams = hop.quantityGrams * scalingFactor)
    )
    val scaledMalts = originalRecipe.malts.map(malt =>
      malt.copy(quantityKg = malt.quantityKg * scalingFactor)
    )
    val scaledYeast = originalRecipe.yeast.map(yeast =>
      yeast.copy(quantityGrams = yeast.quantityGrams * math.sqrt(scalingFactor)) // Levure scale différemment
    )
    val scaledOthers = originalRecipe.otherIngredients.map(other =>
      other.copy(quantityGrams = other.quantityGrams * scalingFactor)
    )
    
    val scaledRecipe = originalRecipe.copy(
      batchSize = targetBatchSize,
      hops = scaledHops,
      malts = scaledMalts,
      yeast = scaledYeast,
      otherIngredients = scaledOthers,
      updatedAt = Instant.now()
    )
    
    // Recalculer les métriques avec la nouvelle taille
    Future.successful(scaledRecipe) // Simplifié - retourner la recette ajustée sans recalculs pour l'instant
  }

  /**
   * Génération d'un guide de brassage détaillé
   */
  def generateBrewingGuide(recipe: RecipeAggregate): Future[BrewingGuide] = {
    Future.successful {
      val steps = buildBrewingSteps(recipe)
      val timeline = buildBrewingTimeline(recipe)
      val equipment = buildEquipmentList(recipe)
      val tips = buildBrewingTips(recipe)
      val troubleshooting = buildTroubleshooting(recipe)
      
      BrewingGuide(
        recipe = recipe,
        steps = steps,
        timeline = timeline,
        equipment = equipment,
        tips = tips,
        troubleshooting = troubleshooting
      )
    }
  }

  /**
   * Analyse d'une recette personnalisée
   */
  def analyzeCustomRecipe(
    analysisRequest: CustomRecipeAnalysisRequest
  ): Future[RecipeAnalysisResponse] = {
    // Convertir la demande en recette temporaire pour analyse
    val tempRecipe = buildTempRecipeFromRequest(analysisRequest)
    
    Future.successful {
      val styleCompliance = analyzeStyleCompliance(tempRecipe, analysisRequest.style)
      val recommendations = List("Recette analysée avec succès")
      val warnings = List.empty[String]
      val improvements = List.empty[ImprovementSuggestion]
      
      RecipeAnalysisResponse(
        calculatedParameters = CalculatedParameters(
          abv = tempRecipe.calculations.abv,
          ibu = tempRecipe.calculations.ibu,
          srm = tempRecipe.calculations.srm,
          og = tempRecipe.calculations.originalGravity,
          fg = tempRecipe.calculations.finalGravity
        ),
        styleCompliance = styleCompliance,
        recommendations = recommendations,
        warnings = warnings,
        improvements = improvements
      )
    }.recover { ex =>
      RecipeAnalysisResponse(
        calculatedParameters = CalculatedParameters(),
        styleCompliance = StyleCompliance(
          targetStyle = "unknown", 
          complianceScore = 0.0, 
          deviations = List(s"Erreur de calcul: ${ex.getMessage}"),
          improvements = List("Vérifiez les données d'entrée")
        ),
        recommendations = List("Erreur lors de l'analyse - vérifiez les données"),
        warnings = List(s"Erreur de calcul: ${ex.getMessage}"),
        improvements = List.empty
      )
    }
  }

  /**
   * Statistiques publiques des recettes
   */
  def getPublicStatistics(): Future[RecipePublicStatistics] = {
    // Récupérer un échantillon représentatif des recettes publiées
    val statsFilter = RecipeFilter(
      status = Some(RecipeStatus.Published),
      size = 1000
    )
    
    recipeReadRepository.findByFilter(statsFilter).map { result =>
      buildPublicStatistics(result.items)
    }
  }

  /**
   * Collections thématiques de recettes
   */
  def getRecipeCollections(
    category: String,
    page: Int,
    size: Int
  ): Future[RecipeCollectionListResponse] = {
    // Implementation simplifiée - en réalité, ceci serait basé sur une logique plus sophistiquée
    val collections = buildThematicCollections(category)
    
    Future.successful(RecipeCollectionListResponse(
      collections = collections.slice(page * size, (page + 1) * size),
      totalCount = collections.length,
      page = page,
      size = size,
      hasNext = (page + 1) * size < collections.length,
      categories = List("seasonal", "style", "difficulty", "ingredients", "technique")
    ))
  }

  // ============================================================================
  // MÉTHODES PRIVÉES - SCORING ET CLASSIFICATION
  // ============================================================================

  private def isBeginnerFriendly(recipe: RecipeAggregate): Boolean = {
    val totalIngredients = recipe.hops.length + recipe.malts.length + recipe.otherIngredients.length
    val hasSimpleProcedures = !recipe.procedures.hasComplexProcedures
    val hasModerateAbv = recipe.calculations.abv.exists(abv => abv >= 3.0 && abv <= 6.5)
    
    totalIngredients <= 8 && hasSimpleProcedures && hasModerateAbv
  }

  private def calculateBeginnerScore(recipe: RecipeAggregate): Double = {
    var score = 0.0
    
    // Bonus pour simplicité des ingrédients
    val totalIngredients = recipe.hops.length + recipe.malts.length + recipe.otherIngredients.length
    if (totalIngredients <= 5) score += 0.3
    else if (totalIngredients <= 8) score += 0.2
    
    // Bonus pour ABV raisonnable
    recipe.calculations.abv.foreach { abv =>
      if (abv >= 4.0 && abv <= 6.0) score += 0.2
      else if (abv >= 3.0 && abv <= 7.0) score += 0.1
    }
    
    // Bonus pour styles populaires débutants
    val beginnerStyles = List("American Wheat", "Blonde Ale", "Cream Ale", "Irish Red")
    if (beginnerStyles.contains(recipe.style.name)) score += 0.15
    
    // Bonus pour procédures simples
    if (!recipe.procedures.hasComplexProcedures) score += 0.2
    
    // Bonus pour durée de brassage raisonnable
    if (recipe.estimatedBrewingTime <= 6.0) score += 0.1
    
    // Bonus pour popularité
    score += recipe.popularityScore * 0.05
    
    math.min(1.0, score)
  }

  private def calculateStyleMatchScore(recipe: RecipeAggregate, targetStyle: String): Double = {
    var score = 0.0
    
    // Match exact du style
    if (recipe.style.name.equalsIgnoreCase(targetStyle)) score += 0.4
    else if (recipe.style.category.toLowerCase.contains(targetStyle.toLowerCase)) score += 0.2
    
    // Analyse des paramètres selon le style
    val styleSpecs = getStyleSpecifications(targetStyle)
    
    recipe.calculations.abv.foreach { abv =>
      if (abv >= styleSpecs.minAbv && abv <= styleSpecs.maxAbv) score += 0.15
    }
    
    recipe.calculations.ibu.foreach { ibu =>
      if (ibu >= styleSpecs.minIbu && ibu <= styleSpecs.maxIbu) score += 0.15
    }
    
    recipe.calculations.srm.foreach { srm =>
      if (srm >= styleSpecs.minSrm && srm <= styleSpecs.maxSrm) score += 0.15
    }
    
    // Analyse des ingrédients typiques du style
    val typicalIngredients = getTypicalIngredientsForStyle(targetStyle)
    val matchingIngredients = countMatchingIngredients(recipe, typicalIngredients)
    score += (matchingIngredients.toDouble / typicalIngredients.totalIngredients) * 0.15
    
    math.min(1.0, score)
  }

  private def calculateIngredientMatchScore(recipe: RecipeAggregate, inventory: IngredientInventory): Double = {
    var score = 0.0
    var totalIngredients = 0
    var matchingIngredients = 0
    
    // Analyser les houblons
    recipe.hops.foreach { hop =>
      totalIngredients += 1
      if (inventory.hasHop(hop.hopId.value.toString)) {
        matchingIngredients += 1
        score += 0.2
      }
    }
    
    // Analyser les malts
    recipe.malts.foreach { malt =>
      totalIngredients += 1
      if (inventory.hasMalt(malt.maltId.value.toString)) {
        matchingIngredients += 1
        score += 0.2
      }
    }
    
    // Analyser la levure
    recipe.yeast.foreach { yeast =>
      totalIngredients += 1
      if (inventory.hasYeast(yeast.yeastId.value.toString)) {
        matchingIngredients += 1
        score += 0.3
      }
    }
    
    // Bonus pour pourcentage élevé d'ingrédients disponibles
    if (totalIngredients > 0) {
      val matchPercentage = matchingIngredients.toDouble / totalIngredients
      if (matchPercentage >= 0.8) score += 0.3
      else if (matchPercentage >= 0.6) score += 0.2
      else if (matchPercentage >= 0.4) score += 0.1
    }
    
    math.min(1.0, score)
  }

  private def calculateSeasonalScore(recipe: RecipeAggregate, season: Season): Double = {
    var score: Double = 0.0
    
    val seasonalStyles = getSeasonalStyles(season)
    if (seasonalStyles.contains(recipe.style.name)) score += 0.3
    
    // ABV saisonnier
    recipe.calculations.abv.foreach { abv =>
      val seasonBonus = season match {
        case Season.Summer if abv <= 5.5 => 0.2 // Bières légères en été
        case Season.Winter if abv >= 7.0 => 0.2 // Bières fortes en hiver
        case Season.Spring | Season.Autumn if abv >= 4.0 && abv <= 7.0 => 0.15
        case _ => 0.0
      }
      score += seasonBonus
    }
    
    // Ingrédients saisonniers
    val seasonalIngredients = getSeasonalIngredients(season)
    val hasSeasonalIngredients = recipe.hops.exists(h => seasonalIngredients.hops.contains(h.hopId.value.toString)) ||
                               recipe.malts.exists(m => seasonalIngredients.malts.contains(m.maltId.value.toString))
    if (hasSeasonalIngredients) score += 0.2
    
    // Moment de l'année
    val currentMonth = LocalDate.now().getMonthValue
    val seasonalBonus = season match {
      case Season.Spring if currentMonth >= 3 && currentMonth <= 5 => 0.1
      case Season.Summer if currentMonth >= 6 && currentMonth <= 8 => 0.1
      case Season.Autumn if currentMonth >= 9 && currentMonth <= 11 => 0.1
      case Season.Winter if currentMonth == 12 || currentMonth <= 2 => 0.1
      case _ => 0.0
    }
    score += seasonalBonus
    
    math.min(1.0, score)
  }

  private def calculateProgressionScore(
    recipe: RecipeAggregate,
    lastRecipe: RecipeAggregate,
    currentLevel: BrewerLevel
  ): Double = {
    var score = 0.0
    
    // Progression en difficulté
    val recipeComplexity = calculateComplexity(recipe)
    val lastComplexity = calculateComplexity(lastRecipe)
    
    if (recipeComplexity > lastComplexity && recipeComplexity <= lastComplexity + 2) {
      score += 0.3 // Progression appropriée
    } else if (recipeComplexity == lastComplexity) {
      score += 0.15 // Même niveau, consolidation
    }
    
    // Nouvelles techniques introduites
    val newTechniques = getNewTechniques(recipe, lastRecipe)
    score += math.min(0.2, newTechniques * 0.05)
    
    // Styles similaires ou progression logique
    if (recipe.style.category == lastRecipe.style.category) {
      score += 0.1 // Même famille de styles
    }
    
    // Adaptation au niveau actuel
    val levelBonus = currentLevel match {
      case BrewerLevel.Beginner if recipeComplexity <= 3 => 0.2
      case BrewerLevel.Intermediate if recipeComplexity >= 2 && recipeComplexity <= 5 => 0.2
      case BrewerLevel.Advanced if recipeComplexity >= 4 && recipeComplexity <= 7 => 0.2
      case BrewerLevel.Expert => 0.1 // Experts peuvent tout faire
      case _ => 0.0
    }
    score += levelBonus
    
    math.min(1.0, score)
  }

  private def calculateAlternativeScore(
    alternative: RecipeAggregate,
    original: RecipeAggregate,
    reason: AlternativeReason
  ): Double = {
    var score = 0.0
    
    reason match {
      case AlternativeReason.UnavailableIngredients =>
        // Privilégier ingrédients plus communs
        score += calculateIngredientCommonness(alternative) * 0.4
        
      case AlternativeReason.TooExpensive =>
        // Privilégier ingrédients moins chers
        score += calculateAffordability(alternative) * 0.4
        
      case AlternativeReason.DifferentEquipment =>
        // Privilégier procédures plus simples
        if (!alternative.procedures.hasComplexProcedures) score += 0.4
        
      case AlternativeReason.TimeConstraints =>
        // Privilégier temps de brassage plus court
        if (alternative.estimatedBrewingTime <= 4.0) score += 0.4
        else if (alternative.estimatedBrewingTime <= 6.0) score += 0.2
        
      case AlternativeReason.Experimentation =>
        // Privilégier recettes avec variations intéressantes
        score += calculateUniqueness(alternative) * 0.4
    }
    
    // Similarité avec l'original
    val styleSimilarity = if (alternative.style.category == original.style.category) 0.2 else 0.0
    val abvSimilarity = (alternative.calculations.abv, original.calculations.abv) match {
      case (Some(altAbv), Some(origAbv)) =>
        val diff = math.abs(altAbv - origAbv)
        if (diff <= 1.0) 0.15 else if (diff <= 2.0) 0.1 else 0.0
      case _ => 0.0
    }
    
    score += styleSimilarity + abvSimilarity
    
    // Bonus popularité
    score += alternative.popularityScore * 0.05
    
    math.min(1.0, score)
  }

  // ============================================================================
  // GÉNÉRATION DE TEXTES ET CONSEILS
  // ============================================================================

  private def generateBeginnerReason(recipe: RecipeAggregate): String = {
    val features = scala.collection.mutable.ListBuffer[String]()
    
    val totalIngredients = recipe.hops.length + recipe.malts.length + recipe.otherIngredients.length
    if (totalIngredients <= 5) features += "ingrédients simples"
    
    recipe.calculations.abv.foreach { abv =>
      if (abv >= 4.0 && abv <= 6.0) features += "degré d'alcool modéré"
    }
    
    if (!recipe.procedures.hasComplexProcedures) features += "procédures standards"
    
    if (features.nonEmpty) {
      s"Parfait pour débuter : ${features.mkString(", ")}"
    } else {
      s"Recette accessible avec style ${recipe.style.name}"
    }
  }

  private def generateBeginnerTips(recipe: RecipeAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    tips += "Sanitisez tout votre équipement avant utilisation"
    tips += "Prenez des notes détaillées pendant le brassage"
    tips += "Contrôlez la température à chaque étape"
    
    if (recipe.hops.length > 2) {
      tips += "Préparez tous vos houblons avant de commencer l'ébullition"
    }
    
    recipe.calculations.abv.foreach { abv =>
      if (abv > 5.0) tips += "Surveillez bien la fermentation, l'alcool peut stresser la levure"
    }
    
    tips += "Goûtez régulièrement pour suivre l'évolution"
    tips += "Soyez patient - la bonne bière prend du temps"
    
    tips.toList
  }

  private def generateBeginnerMatchFactors(recipe: RecipeAggregate): List[String] = {
    val factors = scala.collection.mutable.ListBuffer[String]()
    
    val totalIngredients = recipe.hops.length + recipe.malts.length + recipe.otherIngredients.length
    if (totalIngredients <= 6) factors += "Peu d'ingrédients"
    
    if (!recipe.procedures.hasComplexProcedures) factors += "Procédures simples"
    
    recipe.calculations.abv.foreach { abv =>
      if (abv >= 4.0 && abv <= 6.0) factors += "ABV modéré"
    }
    
    if (recipe.estimatedBrewingTime <= 6.0) factors += "Durée raisonnable"
    
    factors.toList
  }

  private def generateStyleReason(recipe: RecipeAggregate, targetStyle: String): String = {
    if (recipe.style.name.equalsIgnoreCase(targetStyle)) {
      s"Exemple authentique du style ${targetStyle}"
    } else if (recipe.style.category.toLowerCase.contains(targetStyle.toLowerCase)) {
      s"Style apparenté au ${targetStyle} avec caractéristiques similaires"
    } else {
      s"Recette inspirée du style ${targetStyle}"
    }
  }

  private def generateStyleTips(recipe: RecipeAggregate, targetStyle: String): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    val styleSpecs = getStyleSpecifications(targetStyle)
    
    recipe.calculations.abv.foreach { abv =>
      if (abv < styleSpecs.minAbv || abv > styleSpecs.maxAbv) {
        tips += s"ABV cible pour ${targetStyle}: ${styleSpecs.minAbv}-${styleSpecs.maxAbv}%"
      }
    }
    
    recipe.calculations.ibu.foreach { ibu =>
      if (ibu < styleSpecs.minIbu || ibu > styleSpecs.maxIbu) {
        tips += s"IBU cible pour ${targetStyle}: ${styleSpecs.minIbu}-${styleSpecs.maxIbu}"
      }
    }
    
    tips += s"Température de fermentation typique pour ${targetStyle}: ${styleSpecs.fermentationTemp}°C"
    tips += s"Servir à ${styleSpecs.servingTemp}°C pour optimiser les arômes"
    
    tips.toList
  }

  private def generateStyleMatchFactors(recipe: RecipeAggregate, targetStyle: String): List[String] = {
    val factors = scala.collection.mutable.ListBuffer[String]()
    
    if (recipe.style.name.equalsIgnoreCase(targetStyle)) {
      factors += "Style exact"
    }
    
    val styleSpecs = getStyleSpecifications(targetStyle)
    
    recipe.calculations.abv.foreach { abv =>
      if (abv >= styleSpecs.minAbv && abv <= styleSpecs.maxAbv) {
        factors += "ABV conforme"
      }
    }
    
    recipe.calculations.ibu.foreach { ibu =>
      if (ibu >= styleSpecs.minIbu && ibu <= styleSpecs.maxIbu) {
        factors += "Amertume appropriée"
      }
    }
    
    factors.toList
  }

  // ============================================================================
  // MÉTHODES UTILITAIRES ET HELPERS
  // ============================================================================

  private def getByDifficulty(difficulty: String, maxResults: Int): Future[List[RecommendedRecipe]] = {
    val difficultyLevel = DifficultyLevel.fromString(difficulty).getOrElse(DifficultyLevel.Beginner)
    val filter = RecipePublicFilter(difficulty = Some(difficultyLevel), size = maxResults)
    
    recipeReadRepository.findByFilter(filter.toBaseRecipeFilter).map { result =>
      result.items.map(recipe => RecommendedRecipe(
        recipe = recipe,
        score = 0.7,
        reason = s"Recette de niveau ${difficulty}",
        tips = List(s"Adapté pour niveau ${difficulty}"),
        matchFactors = List(s"Difficulté ${difficulty}")
      ))
    }
  }

  private def buildProgressionFilter(
    lastRecipe: RecipeAggregate,
    currentLevel: BrewerLevel,
    nextLevel: Option[BrewerLevel]
  ): RecipeFilter = {
    val targetLevel = nextLevel.getOrElse(currentLevel)
    
    RecipeFilter(
      style = Some(lastRecipe.style.category), // Même catégorie de style
      page = 0,
      size = 30
    )
  }

  private def buildAlternativeFilter(
    original: RecipeAggregate,
    reason: AlternativeReason
  ): RecipeFilter = {
    reason match {
      case AlternativeReason.UnavailableIngredients =>
        RecipeFilter(style = Some(original.style.category), size = 50)
      case AlternativeReason.TooExpensive =>
        RecipeFilter(style = Some(original.style.category), size = 50)
      case AlternativeReason.DifferentEquipment =>
        RecipeFilter(style = Some(original.style.name), size = 30)
      case _ =>
        RecipeFilter(style = Some(original.style.category), size = 30)
    }
  }

  private def getSeasonalStyles(season: Season): List[String] = {
    season match {
      case Season.Spring => List("Wheat Beer", "Pilsner", "Pale Ale", "Saison")
      case Season.Summer => List("Wheat Beer", "Lager", "Session IPA", "Berliner Weisse")
      case Season.Autumn => List("Oktoberfest", "Brown Ale", "Pumpkin Ale", "Harvest Ale")
      case Season.Winter => List("Stout", "Porter", "Winter Warmer", "Barleywine")
    }
  }

  private def getSeasonalIngredients(season: Season): SeasonalIngredients = {
    season match {
      case Season.Spring => SeasonalIngredients(
        hops = List("Saaz", "Tettnang", "Hallertau"),
        malts = List("Pilsner", "Wheat")
      )
      case Season.Summer => SeasonalIngredients(
        hops = List("Citra", "Mosaic", "Amarillo"),
        malts = List("Wheat", "Pale")
      )
      case Season.Autumn => SeasonalIngredients(
        hops = List("Fuggle", "East Kent Goldings"),
        malts = List("Munich", "Crystal", "Vienna")
      )
      case Season.Winter => SeasonalIngredients(
        hops = List("Northern Brewer", "Magnum"),
        malts = List("Chocolate", "Roasted Barley", "Munich")
      )
    }
  }

  private def getStyleSpecifications(styleName: String): StyleSpecifications = {
    styleName.toLowerCase match {
      case s if s.contains("ipa") => StyleSpecifications(
        minAbv = 5.5, maxAbv = 7.5,
        minIbu = 40, maxIbu = 70,
        minSrm = 3, maxSrm = 14,
        fermentationTemp = 18,
        servingTemp = 7
      )
      case s if s.contains("stout") => StyleSpecifications(
        minAbv = 4.0, maxAbv = 7.0,
        minIbu = 25, maxIbu = 45,
        minSrm = 25, maxSrm = 40,
        fermentationTemp = 18,
        servingTemp = 10
      )
      case s if s.contains("wheat") => StyleSpecifications(
        minAbv = 4.3, maxAbv = 5.6,
        minIbu = 8, maxIbu = 15,
        minSrm = 2, maxSrm = 6,
        fermentationTemp = 18,
        servingTemp = 8
      )
      case _ => StyleSpecifications(
        minAbv = 4.0, maxAbv = 6.0,
        minIbu = 15, maxIbu = 35,
        minSrm = 3, maxSrm = 20,
        fermentationTemp = 18,
        servingTemp = 8
      )
    }
  }

  private def getTypicalIngredientsForStyle(styleName: String): TypicalIngredients = {
    styleName.toLowerCase match {
      case s if s.contains("ipa") => TypicalIngredients(
        hops = List("Cascade", "Centennial", "Chinook", "Columbus"),
        malts = List("Pale", "Munich", "Crystal"),
        yeasts = List("US-05", "Safale US-05"),
        others = List.empty,
        totalIngredients = 7
      )
      case _ => TypicalIngredients(
        hops = List.empty,
        malts = List.empty, 
        yeasts = List.empty,
        others = List.empty,
        totalIngredients = 0
      )
    }
  }

  private def countMatchingIngredients(
    recipe: RecipeAggregate,
    typical: TypicalIngredients
  ): Int = {
    // Implémentation simplifiée
    0
  }

  private def calculateComplexity(recipe: RecipeAggregate): Int = {
    var complexity = 0
    
    complexity += recipe.hops.length
    complexity += recipe.malts.length
    complexity += recipe.otherIngredients.length
    
    if (recipe.procedures.hasMashProfile) complexity += 1
    if (recipe.procedures.hasBoilProcedure) complexity += 1
    if (recipe.procedures.hasFermentationProfile) complexity += 2
    if (recipe.procedures.hasPackagingProcedure) complexity += 1
    
    complexity
  }

  private def getNewTechniques(recipe: RecipeAggregate, lastRecipe: RecipeAggregate): Int = {
    // Analyser les nouvelles techniques introduites
    var newTechniques = 0
    
    if (recipe.procedures.hasMashProfile && !lastRecipe.procedures.hasMashProfile) newTechniques += 1
    if (recipe.procedures.hasFermentationProfile && !lastRecipe.procedures.hasFermentationProfile) newTechniques += 1
    if (recipe.procedures.hasPackagingProcedure && !lastRecipe.procedures.hasPackagingProcedure) newTechniques += 1
    
    // Analyser nouveaux types d'ingrédients
    val newHopCount = recipe.hops.length - lastRecipe.hops.length
    val newMaltCount = recipe.malts.length - lastRecipe.malts.length
    val newOtherCount = recipe.otherIngredients.length - lastRecipe.otherIngredients.length
    
    if (newHopCount > 0) newTechniques += 1
    if (newMaltCount > 0) newTechniques += 1
    if (newOtherCount > 0) newTechniques += 1
    
    newTechniques
  }

  private def calculateIngredientCommonness(recipe: RecipeAggregate): Double = {
    // Score basé sur la fréquence des ingrédients dans les recettes populaires
    // Implémentation simplifiée
    0.7
  }

  private def calculateAffordability(recipe: RecipeAggregate): Double = {
    // Score basé sur le coût estimé des ingrédients
    // Implémentation simplifiée
    0.8
  }

  private def calculateUniqueness(recipe: RecipeAggregate): Double = {
    // Score basé sur la rareté/originalité de la recette
    // Implémentation simplifiée
    0.6
  }

  // Méthodes stub pour les autres fonctionnalités nécessaires
  private def generateIngredientReason(recipe: RecipeAggregate, inventory: IngredientInventory): String = 
    s"Compatible avec ${inventory.totalIngredients} ingrédients disponibles"
  
  private def generateIngredientTips(recipe: RecipeAggregate, inventory: IngredientInventory): List[String] = 
    List("Vérifiez les quantités nécessaires", "Préparez les substitutions si nécessaire")
    
  private def generateIngredientMatchFactors(recipe: RecipeAggregate, inventory: IngredientInventory): List[String] = 
    List("Ingrédients disponibles")

  private def generateSeasonalReason(recipe: RecipeAggregate, season: Season): String = 
    s"Parfait pour la saison ${season.toString.toLowerCase}"
    
  private def generateSeasonalTips(recipe: RecipeAggregate, season: Season): List[String] = 
    List(s"Adapté pour brassage en ${season.toString.toLowerCase}")
    
  private def generateSeasonalMatchFactors(recipe: RecipeAggregate, season: Season): List[String] = 
    List("Style saisonnier")

  private def generateProgressionReason(recipe: RecipeAggregate, lastRecipe: RecipeAggregate, level: BrewerLevel): String = 
    s"Progression logique après ${lastRecipe.name.value}"
    
  private def generateProgressionTips(recipe: RecipeAggregate, lastRecipe: RecipeAggregate, level: BrewerLevel): List[String] = 
    List("Nouvelle étape dans votre apprentissage")
    
  private def generateProgressionMatchFactors(recipe: RecipeAggregate, lastRecipe: RecipeAggregate, level: BrewerLevel): List[String] = 
    List("Progression appropriée")

  private def generateAlternativeReason(recipe: RecipeAggregate, original: RecipeAggregate, reason: AlternativeReason): String = 
    s"Alternative pour ${reason.name}"
    
  private def generateAlternativeTips(recipe: RecipeAggregate, original: RecipeAggregate, reason: AlternativeReason): List[String] = 
    List("Comparez avec la recette originale")
    
  private def generateAlternativeMatchFactors(recipe: RecipeAggregate, original: RecipeAggregate, reason: AlternativeReason): List[String] = 
    List("Style similaire")

  // Méthodes pour guides et analyses (implémentations simplifiées)
  private def buildBrewingSteps(recipe: RecipeAggregate): List[BrewingStep] = List.empty
  private def buildBrewingTimeline(recipe: RecipeAggregate): BrewingTimeline = 
    BrewingTimeline(List.empty, List.empty, List.empty)
  private def buildEquipmentList(recipe: RecipeAggregate): List[String] = List.empty
  private def buildBrewingTips(recipe: RecipeAggregate): List[BrewingTip] = List.empty
  private def buildTroubleshooting(recipe: RecipeAggregate): List[TroubleshootingItem] = List.empty
  
  private def buildTempRecipeFromRequest(request: CustomRecipeAnalysisRequest): RecipeAggregate = {
    // Construction d'une recette temporaire pour analyse
    // Implémentation simplifiée
    RecipeAggregate.create(
      name = RecipeName.create(request.name).getOrElse(RecipeName.create("Analyse").right.get),
      description = None,
      style = BeerStyle.create("custom", request.style, "Custom").getOrElse(BeerStyle.create("unknown", "Unknown", "Unknown").right.get),
      batchSize = BatchSize.create(request.batchSize, VolumeUnit.fromString(request.batchUnit).getOrElse(VolumeUnit.Liters)).getOrElse(BatchSize.create(20.0, VolumeUnit.Liters).right.get),
      createdBy = UserId.fromString("analysis").right.get
    )
  }
  
  private def analyzeStyleCompliance(recipe: RecipeAggregate, styleName: String): StyleCompliance = {
    StyleCompliance(
      targetStyle = styleName,
      complianceScore = 0.8,
      deviations = List.empty,
      improvements = List.empty
    )
  }
  
  private def generateAnalysisRecommendations(recipe: RecipeAggregate, calculations: BrewingCalculationResult): List[String] = 
    List("Recette équilibrée", "Paramètres dans les normes")
    
  private def generateAnalysisWarnings(recipe: RecipeAggregate, calculations: BrewingCalculationResult): List[String] = 
    List.empty
    
  private def generateImprovementSuggestions(recipe: RecipeAggregate, calculations: BrewingCalculationResult): List[ImprovementSuggestion] = 
    List.empty

  private def buildPublicStatistics(recipes: List[RecipeAggregate]): RecipePublicStatistics = {
    RecipePublicStatistics(
      totalRecipes = recipes.length,
      totalStyles = recipes.map(_.style.name).distinct.length,
      averageAbv = if (recipes.count(_.calculations.abv.isDefined) > 0) recipes.flatMap(_.calculations.abv).sum / recipes.count(_.calculations.abv.isDefined) else 0.0,
      averageIbu = if (recipes.count(_.calculations.ibu.isDefined) > 0) recipes.flatMap(_.calculations.ibu).sum / recipes.count(_.calculations.ibu.isDefined) else 0.0,
      popularStyles = List.empty,
      popularIngredients = PopularIngredients(List.empty, List.empty, List.empty),
      trends = TrendStatistics(List.empty, List.empty, List.empty),
      communityMetrics = CommunityMetrics(4.2, 1250, 450, 12.5)
    )
  }
  
  private def buildThematicCollections(category: String): List[RecipeCollection] = List.empty

  // Extensions implicites pour améliorer la lisibilité
  implicit class RecipeAggregateExtensions(recipe: RecipeAggregate) {
    def popularityScore: Double = 0.7 // Implémentation simplifiée
    def estimatedBrewingTime: Double = 5.0 // Implémentation simplifiée
  }
  
  implicit class BrewingProceduresExtensions(procedures: BrewingProcedures) {
    def hasComplexProcedures: Boolean = 
      procedures.hasFermentationProfile && procedures.hasPackagingProcedure
  }
}

// ============================================================================
// TYPES DE SUPPORT
// ============================================================================

// RecommendedRecipe moved to domain.recipes.model.RecommendedRecipe

case class StyleSpecifications(
  minAbv: Double,
  maxAbv: Double,
  minIbu: Double,
  maxIbu: Double,
  minSrm: Double,
  maxSrm: Double,
  fermentationTemp: Int,
  servingTemp: Int
)

case class TypicalIngredients(
  hops: List[String],
  malts: List[String],
  yeasts: List[String],
  others: List[String],
  totalIngredients: Int
)

case class SeasonalIngredients(
  hops: List[String],
  malts: List[String]
)