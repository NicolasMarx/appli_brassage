package application.recipes.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.recipes.model._
import domain.recipes.repositories.{RecipeReadRepository, RecipeWriteRepository}
import domain.yeasts.repositories.YeastReadRepository
import domain.hops.repositories.HopReadRepository
import domain.malts.repositories.MaltReadRepository
import domain.common.DomainError
import play.api.libs.json._
import java.time.Instant

/**
 * Service pour les recommandations de progression de brasseur ML-ready
 * Endpoint: GET /api/v1/recipes/recommendations/progression
 * 
 * Fonctionnalité: Recommandations personnalisées basées sur l'historique et niveau
 * Features: ML-ready feature extraction, progression intelligente, recommandations adaptatives
 */
@Singleton
class ProgressionRecommendationService @Inject()(
  recipeReadRepository: RecipeReadRepository,
  recipeWriteRepository: RecipeWriteRepository,
  yeastReadRepository: YeastReadRepository,
  hopReadRepository: HopReadRepository,
  maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Générer des recommandations de progression personnalisées pour un brasseur
   */
  def generateProgressionRecommendations(
    userId: UserId,
    brewerLevel: BrewerLevel,
    preferences: BrewerPreferences = BrewerPreferences.default,
    historicalData: Option[BrewerHistoricalData] = None
  ): Future[Either[DomainError, ProgressionRecommendationResult]] = {
    
    for {
      // Étape 1: Analyser l'historique du brasseur (si disponible)
      brewerProfile <- analyzeBrewerProfile(userId, brewerLevel, historicalData)
      
      // Étape 2: Identifier les lacunes et opportunités de progression
      progressionGaps <- identifyProgressionGaps(brewerProfile, preferences)
      
      // Étape 3: Générer les recommandations adaptées
      recommendations <- generateAdaptiveRecommendations(brewerProfile, progressionGaps, preferences)
      
      // Étape 4: Extraire les features ML pour apprentissage futur
      mlFeatures <- extractMLFeatures(brewerProfile, recommendations)
      
      // Étape 5: Créer le plan de progression personnalisé
      progressionPlan <- createProgressionPlan(recommendations, brewerLevel, preferences)
      
      result = ProgressionRecommendationResult(
        brewerProfile = brewerProfile,
        currentLevel = brewerLevel,
        progressionGaps = progressionGaps,
        recommendations = recommendations,
        progressionPlan = progressionPlan,
        mlFeatures = mlFeatures,
        metadata = ProgressionMetadata(
          userId = userId,
          generatedAt = Instant.now(),
          recommendationCount = recommendations.length,
          targetLevel = getNextTargetLevel(brewerLevel),
          confidence = calculateOverallConfidence(recommendations)
        )
      )
      
    } yield Right(result)
  }

  /**
   * Recommander des recettes pour débuter selon le niveau
   */
  def recommendBeginnerRecipes(
    preferences: BrewerPreferences = BrewerPreferences.default,
    maxComplexity: ComplexityLevel = ComplexityLevel.BEGINNER,
    count: Int = 5
  ): Future[Either[DomainError, BeginnerRecommendationResult]] = {
    
    for {
      // Étape 1: Récupérer toutes les recettes disponibles
      allRecipes <- getAllRecipes()
      
      // Étape 2: Filtrer et scorer les recettes adaptées aux débutants
      beginnerRecipes = allRecipes
        .filter(isBeginnerFriendly)
        .map(recipe => ScoredRecipeRecommendation(
          recipe = recipe,
          beginnerScore = calculateBeginnerScore(recipe),
          difficultyLevel = assessDifficultyLevel(recipe),
          learningObjectives = identifyLearningObjectives(recipe),
          estimatedDuration = estimateBrewingDuration(recipe),
          requiredEquipment = getRequiredEquipment(recipe),
          successTips = generateSuccessTips(recipe)
        ))
        .sortBy(-_.beginnerScore)
        .take(count)
      
      // Étape 3: Créer le parcours d'apprentissage structuré
      learningPath <- Future.successful(createBeginnerLearningPath(beginnerRecipes))
      
      result <- Future.successful(BeginnerRecommendationResult(
        recommendedRecipes = beginnerRecipes,
        learningPath = learningPath,
        generalTips = generateGeneralBeginnerTips(),
        equipment = getEssentialBeginnerEquipment(),
        timeline = estimateBeginnerTimeline(beginnerRecipes)
      ))
      
    } yield Right(result)
  }

  /**
   * Recommander une progression naturelle pour un brasseur expérimenté
   */
  def recommendAdvancedProgression(
    userId: UserId,
    currentSkills: List[BrewingSkill],
    targetStyles: List[String] = List.empty,
    challengeLevel: String = "MODERATE"
  ): Future[Either[DomainError, AdvancedProgressionResult]] = {
    
    for {
      // Étape 1: Analyser les compétences actuelles
      skillAssessment <- Future.successful(assessCurrentSkills(currentSkills))
      
      // Étape 2: Identifier les prochaines techniques à maîtriser
      nextTechniques <- Future.successful(identifyNextTechniques(skillAssessment, targetStyles, challengeLevel))
      
      // Étape 3: Récupérer toutes les recettes pour rechercher les techniques
      allRecipes <- getAllRecipes()
      
      // Étape 4: Proposer des recettes qui développent ces techniques
      techniqueRecipes = nextTechniques.flatMap { technique =>
        findRecipesForTechnique(allRecipes, technique).map { recipe =>
          TechniqueRecipeRecommendation(
            recipe = recipe,
            technique = technique,
            difficultyIncrease = assessDifficultyIncrease(recipe, currentSkills),
            newSkillsIntroduced = identifyNewSkills(recipe, currentSkills),
            successProbability = calculateSuccessProbability(recipe, skillAssessment),
            learningValue = calculateLearningValue(recipe, currentSkills)
          )
        }
      }
      
      // Étape 5: Créer un plan de progression avancée
      advancedPlan <- Future.successful(createAdvancedProgressionPlan(techniqueRecipes, skillAssessment, challengeLevel))
      
      result <- Future.successful(AdvancedProgressionResult(
        currentSkillAssessment = skillAssessment,
        nextTechniques = nextTechniques,
        recommendedRecipes = techniqueRecipes,
        progressionPlan = advancedPlan,
        milestones = createProgressionMilestones(nextTechniques, challengeLevel),
        resources = getResourcesForTechniques(nextTechniques)
      ))
      
    } yield Right(result)
  }

  /**
   * Générer des recommandations de styles à explorer
   */
  def recommendStyleExploration(
    userId: UserId,
    brewerHistory: Option[BrewerHistoricalData] = None,
    exploration: ExplorationPreference = ExplorationPreference.BALANCED
  ): Future[Either[DomainError, StyleExplorationResult]] = {
    
    for {
      // Analyser les styles déjà brassés
      brewedStyles <- analyzeBrowedStyles(brewerHistory)
      
      // Identifier les styles à explorer selon les préférences
      candidateStyles <- identifyStyleCandidates(brewedStyles, exploration)
      
      // Récupérer toutes les recettes pour proposer des recettes d'introduction
      allRecipes <- getAllRecipes()
      
      // Pour chaque style candidat, proposer une recette d'introduction
      styleRecommendations = candidateStyles.map { style =>
        val introRecipe = findBestIntroductionRecipe(allRecipes, style)
        StyleRecommendation(
          style = style,
          introductionRecipe = introRecipe,
          styleDescription = getStyleDescription(style),
          difficultyRating = assessStyleDifficulty(style),
          flavorProfile = getStyleFlavorProfile(style),
          keyTechniques = getStyleKeyTechniques(style),
          seasonalSuitability = getStyleSeasonalSuitability(style),
          progressionValue = calculateStyleProgressionValue(style, brewedStyles)
        )
      }.sortBy(-_.progressionValue).take(6)
      
      // Créer un plan d'exploration
      explorationPlan <- Future.successful(createStyleExplorationPlan(styleRecommendations, exploration))
      
      result <- Future.successful(StyleExplorationResult(
        brewedStyles = brewedStyles,
        recommendedStyles = styleRecommendations,
        explorationPlan = explorationPlan,
        seasonalRecommendations = getSeasonalStyleRecommendations(styleRecommendations),
        flavor = createFlavorJourneyMap(styleRecommendations)
      ))
      
    } yield Right(result)
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - ANALYSE ET GÉNÉRATION  
  // ==========================================================================

  /**
   * Récupérer toutes les recettes disponibles (implémentation complète)
   */
  private def getAllRecipes(): Future[List[RecipeAggregate]] = {
    // Utiliser findByFilter avec un filtre large pour récupérer toutes les recettes
    val openFilter = RecipeFilter(
      status = Some(RecipeStatus.Published), // Seulement les recettes publiées
      size = 1000 // Limite raisonnable
    )
    recipeReadRepository.findByFilter(openFilter).map(_.items)
  }

  private def analyzeBrewerProfile(
    userId: UserId, 
    brewerLevel: BrewerLevel, 
    historicalData: Option[BrewerHistoricalData]
  ): Future[BrewerProfile] = {
    
    val profile = BrewerProfile(
      userId = userId,
      level = brewerLevel,
      experience = historicalData.map(_.totalBatches).getOrElse(0),
      favoriteStyles = historicalData.map(_.favoriteStyles).getOrElse(List.empty),
      masteredTechniques = extractMasteredTechniques(historicalData),
      preferredComplexity = inferPreferredComplexity(brewerLevel, historicalData),
      successRate = historicalData.map(_.successRate).getOrElse(0.8),
      averageBatchSize = historicalData.map(_.averageBatchSize).getOrElse(20.0),
      equipment = historicalData.map(_.availableEquipment).getOrElse(List("basic")),
      lastBrewDate = historicalData.flatMap(_.lastBrewDate)
    )
    
    Future.successful(profile)
  }

  private def identifyProgressionGaps(
    brewerProfile: BrewerProfile, 
    preferences: BrewerPreferences
  ): Future[ProgressionGaps] = {
    
    val expectedSkillsForLevel = getExpectedSkillsForLevel(brewerProfile.level)
    val missingSkills = expectedSkillsForLevel -- brewerProfile.masteredTechniques.toSet
    
    val gaps = ProgressionGaps(
      missingSkills = missingSkills.toList,
      underexploredStyles = identifyUnderexploredStyles(brewerProfile, preferences),
      techniqueOpportunities = identifyTechniqueOpportunities(brewerProfile),
      equipmentUpgrades = suggestEquipmentUpgrades(brewerProfile),
      knowledgeGaps = identifyKnowledgeGaps(brewerProfile)
    )
    
    Future.successful(gaps)
  }

  private def generateAdaptiveRecommendations(
    brewerProfile: BrewerProfile,
    gaps: ProgressionGaps,
    preferences: BrewerPreferences
  ): Future[List[ProgressionRecommendation]] = {
    
    val recommendations = scala.collection.mutable.ListBuffer[ProgressionRecommendation]()
    
    // Recommandations pour combler les lacunes techniques
    gaps.missingSkills.foreach { skill =>
      recommendations += ProgressionRecommendation(
        type_ = RecommendationType.SKILL_DEVELOPMENT,
        priority = determinePriority(skill, brewerProfile.level),
        title = s"Maîtriser ${skill}",
        description = s"Développer la compétence: ${skill}",
        actionItems = generateSkillActionItems(skill),
        estimatedTimeWeeks = estimateSkillDevelopmentTime(skill),
        difficulty = assessSkillDifficulty(skill),
        prerequisites = getSkillPrerequisites(skill),
        confidence = 0.85
      )
    }
    
    // Recommandations d'exploration de styles  
    gaps.underexploredStyles.take(3).foreach { style =>
      recommendations += ProgressionRecommendation(
        type_ = RecommendationType.STYLE_EXPLORATION,
        priority = if (preferences.explorationPreference == ExplorationPreference.ADVENTUROUS) "HIGH" else "MEDIUM",
        title = s"Explorer le style ${style}",
        description = s"Découvrir et brasser un ${style}",
        actionItems = List(s"Rechercher les caractéristiques du ${style}", s"Brasser une recette classique de ${style}"),
        estimatedTimeWeeks = 2,
        difficulty = assessStyleDifficulty(style),
        prerequisites = getStylePrerequisites(style),
        confidence = 0.75
      )
    }
    
    // Recommandations d'amélioration d'équipement
    gaps.equipmentUpgrades.take(2).foreach { equipment =>
      recommendations += ProgressionRecommendation(
        type_ = RecommendationType.EQUIPMENT_UPGRADE,
        priority = "MEDIUM",
        title = s"Acquérir ${equipment}",
        description = s"Investissement dans ${equipment} pour améliorer la qualité",
        actionItems = List(s"Rechercher les options pour ${equipment}", s"Budgetiser l'achat", s"Apprendre à utiliser ${equipment}"),
        estimatedTimeWeeks = 4,
        difficulty = "EASY",
        prerequisites = List.empty,
        confidence = 0.9
      )
    }
    
    Future.successful(recommendations.toList)
  }

  private def extractMLFeatures(
    brewerProfile: BrewerProfile, 
    recommendations: List[ProgressionRecommendation]
  ): Future[ProgressionMLFeatures] = {
    
    val features = ProgressionMLFeatures(
      userId = brewerProfile.userId.toString,
      featureVector = Map(
        "experience_batches" -> brewerProfile.experience.toDouble,
        "level_numeric" -> brewerLevelToNumeric(brewerProfile.level),
        "success_rate" -> brewerProfile.successRate,
        "style_diversity" -> brewerProfile.favoriteStyles.length.toDouble,
        "technique_count" -> brewerProfile.masteredTechniques.length.toDouble,
        "equipment_sophistication" -> calculateEquipmentScore(brewerProfile.equipment),
        "batch_size_avg" -> brewerProfile.averageBatchSize,
        "recommendation_count" -> recommendations.length.toDouble,
        "high_priority_recs" -> recommendations.count(_.priority == "HIGH").toDouble,
        "skill_focused_recs" -> recommendations.count(_.type_ == RecommendationType.SKILL_DEVELOPMENT).toDouble
      ),
      categoricalFeatures = Map(
        "level" -> brewerProfile.level.toString,
        "dominant_style" -> brewerProfile.favoriteStyles.headOption.getOrElse("UNKNOWN"),
        "equipment_class" -> classifyEquipmentLevel(brewerProfile.equipment)
      ),
      progressionVector = recommendations.map { rec =>
        Map(
          "type" -> rec.type_.toString,
          "priority_numeric" -> priorityToNumeric(rec.priority),
          "difficulty_numeric" -> difficultyToNumeric(rec.difficulty),
          "confidence" -> rec.confidence,
          "estimated_weeks" -> rec.estimatedTimeWeeks.toDouble
        )
      },
      timestamp = Instant.now()
    )
    
    Future.successful(features)
  }

  private def createProgressionPlan(
    recommendations: List[ProgressionRecommendation],
    currentLevel: BrewerLevel,
    preferences: BrewerPreferences
  ): Future[ProgressionPlan] = {
    
    // Organiser les recommandations par phases
    val phases = organizeRecommendationsIntoPhases(recommendations, preferences)
    
    val plan = ProgressionPlan(
      currentLevel = currentLevel,
      targetLevel = getNextTargetLevel(currentLevel),
      phases = phases,
      totalEstimatedWeeks = phases.map(_.estimatedWeeks).sum,
      keyMilestones = extractKeyMilestones(phases),
      successMetrics = defineSuccessMetrics(phases),
      adaptationStrategy = createAdaptationStrategy(recommendations, preferences)
    )
    
    Future.successful(plan)
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def isBeginnerFriendly(recipe: RecipeAggregate): Boolean = {
    val hopCount = recipe.hops.length
    val maltCount = recipe.malts.length
    val hasComplexIngredients = recipe.otherIngredients.nonEmpty
    
    hopCount <= 3 && maltCount <= 4 && !hasComplexIngredients
  }

  private def calculateBeginnerScore(recipe: RecipeAggregate): Double = {
    var score = 1.0
    
    // Pénaliser la complexité
    if (recipe.hops.length > 2) score -= 0.2
    if (recipe.malts.length > 3) score -= 0.2
    if (recipe.otherIngredients.nonEmpty) score -= 0.3
    
    // Favoriser les styles classiques
    if (List("Pale Ale", "IPA", "Wheat", "Stout").exists(recipe.style.name.contains)) score += 0.2
    
    // Favoriser les ABV modérés
    recipe.calculations.abv.foreach { abv =>
      if (abv >= 4.0 && abv <= 6.0) score += 0.1
      else if (abv > 8.0) score -= 0.2
    }
    
    Math.max(0.0, Math.min(1.0, score))
  }

  private def assessDifficultyLevel(recipe: RecipeAggregate): String = {
    val complexity = recipe.hops.length + recipe.malts.length * 0.5 + recipe.otherIngredients.length * 2
    
    complexity match {
      case c if c <= 4 => "BEGINNER"
      case c if c <= 8 => "INTERMEDIATE" 
      case c if c <= 12 => "ADVANCED"
      case _ => "EXPERT"
    }
  }

  private def identifyLearningObjectives(recipe: RecipeAggregate): List[String] = {
    val objectives = scala.collection.mutable.ListBuffer[String]()
    
    if (recipe.hops.length > 1) objectives += "Gestion du houblonnage multiple"
    if (recipe.malts.length > 2) objectives += "Assemblage de malts"
    if (recipe.calculations.abv.exists(_ > 6.0)) objectives += "Bières plus fortes"
    if (recipe.style.name.contains("Lager")) objectives += "Fermentation basse"
    
    objectives.toList
  }

  private def estimateBrewingDuration(recipe: RecipeAggregate): Int = {
    val baseMinutes = 240 // 4 heures de base
    val complexity = recipe.hops.length * 5 + recipe.malts.length * 3
    baseMinutes + complexity
  }

  private def getRequiredEquipment(recipe: RecipeAggregate): List[String] = {
    List("Cuve de brassage", "Fermenteur", "Thermomètre", "Densimètre") ++
    (if (recipe.hops.length > 2) List("Sac à houblon") else List.empty) ++
    (if (recipe.style.name.contains("Lager")) List("Contrôle température") else List.empty)
  }

  private def generateSuccessTips(recipe: RecipeAggregate): List[String] = {
    val tips = scala.collection.mutable.ListBuffer[String]()
    
    tips += "Maintenir une hygiène stricte"
    tips += "Contrôler la température de fermentation"
    
    if (recipe.hops.length > 1) tips += "Noter les temps d'ajout des houblons"
    if (recipe.malts.length > 2) tips += "Bien mélanger les malts lors du concassage"
    
    tips.toList
  }

  private def extractMasteredTechniques(historicalData: Option[BrewerHistoricalData]): List[String] = {
    historicalData.map(_.masteredTechniques).getOrElse(List("basic_ale", "simple_mashing"))
  }

  private def inferPreferredComplexity(level: BrewerLevel, historicalData: Option[BrewerHistoricalData]): String = {
    level match {
      case BrewerLevel.NOVICE => "SIMPLE"
      case BrewerLevel.INTERMEDIATE => "MODERATE"
      case BrewerLevel.ADVANCED => "COMPLEX"
      case BrewerLevel.EXPERT => "VERY_COMPLEX"
    }
  }

  private def getExpectedSkillsForLevel(level: BrewerLevel): Set[String] = level match {
    case BrewerLevel.NOVICE => Set("basic_ale", "simple_mashing", "basic_hopping")
    case BrewerLevel.INTERMEDIATE => Set("all_grain", "hop_scheduling", "yeast_management", "water_chemistry_basics")
    case BrewerLevel.ADVANCED => Set("lager_brewing", "barrel_aging", "wild_fermentation", "recipe_formulation")
    case BrewerLevel.EXPERT => Set("sour_brewing", "blending", "advanced_water_chemistry", "experimental_techniques")
  }

  private def identifyUnderexploredStyles(profile: BrewerProfile, preferences: BrewerPreferences): List[String] = {
    val allStyles = List("IPA", "Stout", "Lager", "Wheat", "Belgian", "Sour", "Porter", "Saison")
    val brewedStyles = profile.favoriteStyles.toSet
    (allStyles.toSet -- brewedStyles).toList
  }

  private def identifyTechniqueOpportunities(profile: BrewerProfile): List[String] = {
    val allTechniques = getExpectedSkillsForLevel(getNextTargetLevel(profile.level))
    val masterед = profile.masteredTechniques.toSet
    (allTechniques -- masterед).toList
  }

  private def suggestEquipmentUpgrades(profile: BrewerProfile): List[String] = {
    val suggestions = scala.collection.mutable.ListBuffer[String]()
    
    if (!profile.equipment.contains("refroidisseur")) suggestions += "Refroidisseur à plaques"
    if (!profile.equipment.contains("fermenteur_conique")) suggestions += "Fermenteur conique"
    if (!profile.equipment.contains("controle_temperature")) suggestions += "Contrôleur de température"
    
    suggestions.toList
  }

  private def identifyKnowledgeGaps(profile: BrewerProfile): List[String] = {
    List("Chimie de l'eau", "Microbiologie", "Analyse sensorielle", "Formulation de recettes")
  }

  private def getNextTargetLevel(currentLevel: BrewerLevel): BrewerLevel = currentLevel match {
    case BrewerLevel.NOVICE => BrewerLevel.INTERMEDIATE
    case BrewerLevel.INTERMEDIATE => BrewerLevel.ADVANCED  
    case BrewerLevel.ADVANCED => BrewerLevel.EXPERT
    case BrewerLevel.EXPERT => BrewerLevel.EXPERT // Already at max
  }

  private def calculateOverallConfidence(recommendations: List[ProgressionRecommendation]): Double = {
    if (recommendations.isEmpty) 0.0
    else recommendations.map(_.confidence).sum / recommendations.length
  }

  private def organizeRecommendationsIntoPhases(
    recommendations: List[ProgressionRecommendation], 
    preferences: BrewerPreferences
  ): List[ProgressionPhase] = {
    val highPriority = recommendations.filter(_.priority == "HIGH")
    val mediumPriority = recommendations.filter(_.priority == "MEDIUM")
    val lowPriority = recommendations.filter(_.priority == "LOW")
    
    List(
      ProgressionPhase("Phase 1: Fondamentaux", highPriority, highPriority.map(_.estimatedTimeWeeks).sum),
      ProgressionPhase("Phase 2: Approfondissement", mediumPriority, mediumPriority.map(_.estimatedTimeWeeks).sum),
      ProgressionPhase("Phase 3: Perfectionnement", lowPriority, lowPriority.map(_.estimatedTimeWeeks).sum)
    ).filter(_.recommendations.nonEmpty)
  }

  // Helper methods pour conversions ML
  private def brewerLevelToNumeric(level: BrewerLevel): Double = level match {
    case BrewerLevel.NOVICE => 1.0
    case BrewerLevel.INTERMEDIATE => 2.0
    case BrewerLevel.ADVANCED => 3.0
    case BrewerLevel.EXPERT => 4.0
  }

  private def calculateEquipmentScore(equipment: List[String]): Double = equipment.length * 0.1

  private def classifyEquipmentLevel(equipment: List[String]): String = {
    equipment.length match {
      case n if n <= 3 => "BASIC"
      case n if n <= 6 => "INTERMEDIATE"
      case n if n <= 10 => "ADVANCED"
      case _ => "PROFESSIONAL"
    }
  }

  private def priorityToNumeric(priority: String): Double = priority match {
    case "HIGH" => 3.0
    case "MEDIUM" => 2.0
    case "LOW" => 1.0
    case _ => 0.0
  }

  private def difficultyToNumeric(difficulty: String): Double = difficulty match {
    case "EASY" => 1.0
    case "MEDIUM" => 2.0
    case "HARD" => 3.0
    case _ => 2.0
  }

  // Méthodes de stub pour les autres fonctionnalités
  private def generateSkillActionItems(skill: String): List[String] = List(s"Étudier ${skill}", s"Pratiquer ${skill}")
  private def estimateSkillDevelopmentTime(skill: String): Int = 3
  private def assessSkillDifficulty(skill: String): String = "MEDIUM"
  private def getSkillPrerequisites(skill: String): List[String] = List.empty
  private def determinePriority(skill: String, level: BrewerLevel): String = "MEDIUM"
  private def assessStyleDifficulty(style: String): String = "MEDIUM"
  private def getStylePrerequisites(style: String): List[String] = List.empty
  private def createBeginnerLearningPath(recipes: List[ScoredRecipeRecommendation]): LearningPath = LearningPath(List.empty, 0)
  private def generateGeneralBeginnerTips(): List[String] = List("Commencer simple", "Maintenir la propreté")
  private def getEssentialBeginnerEquipment(): List[String] = List("Cuve", "Fermenteur", "Thermomètre")
  private def estimateBeginnerTimeline(recipes: List[ScoredRecipeRecommendation]): Timeline = Timeline(12, "semaines")
  private def assessCurrentSkills(skills: List[BrewingSkill]): SkillAssessment = SkillAssessment(Map.empty, 0.5)
  private def identifyNextTechniques(assessment: SkillAssessment, styles: List[String], level: String): List[BrewingTechnique] = List.empty
  private def findRecipesForTechnique(recipes: List[RecipeAggregate], technique: BrewingTechnique): List[RecipeAggregate] = List.empty
  private def extractKeyMilestones(phases: List[ProgressionPhase]): List[String] = List.empty
  private def defineSuccessMetrics(phases: List[ProgressionPhase]): List[String] = List.empty
  private def createAdaptationStrategy(recommendations: List[ProgressionRecommendation], preferences: BrewerPreferences): String = "Adaptatif"
  
  // Méthodes supplémentaires pour le style exploration
  private def analyzeBrowedStyles(history: Option[BrewerHistoricalData]): Future[List[String]] = Future.successful(List.empty)
  private def identifyStyleCandidates(brewedStyles: List[String], exploration: ExplorationPreference): Future[List[String]] = Future.successful(List("IPA", "Stout"))
  private def findBestIntroductionRecipe(recipes: List[RecipeAggregate], style: String): Option[RecipeAggregate] = recipes.headOption
  private def getStyleDescription(style: String): String = s"Description du style ${style}"
  private def getStyleFlavorProfile(style: String): FlavorProfile = FlavorProfile(List.empty)
  private def getStyleKeyTechniques(style: String): List[String] = List.empty
  private def getStyleSeasonalSuitability(style: String): String = "Toute saison"
  private def calculateStyleProgressionValue(style: String, brewedStyles: List[String]): Double = 0.8
  private def createStyleExplorationPlan(recommendations: List[StyleRecommendation], exploration: ExplorationPreference): ExplorationPlan = ExplorationPlan(List.empty, 0)
  private def getSeasonalStyleRecommendations(recommendations: List[StyleRecommendation]): List[SeasonalRecommendation] = List.empty
  private def createFlavorJourneyMap(recommendations: List[StyleRecommendation]): FlavorJourneyMap = FlavorJourneyMap(List.empty)
  private def assessDifficultyIncrease(recipe: RecipeAggregate, skills: List[BrewingSkill]): String = "MODERATE"
  private def identifyNewSkills(recipe: RecipeAggregate, currentSkills: List[BrewingSkill]): List[String] = List.empty
  private def calculateSuccessProbability(recipe: RecipeAggregate, assessment: SkillAssessment): Double = 0.75
  private def calculateLearningValue(recipe: RecipeAggregate, skills: List[BrewingSkill]): Double = 0.8
  private def createAdvancedProgressionPlan(recipes: List[TechniqueRecipeRecommendation], assessment: SkillAssessment, level: String): AdvancedProgressionPlan = AdvancedProgressionPlan(List.empty, 0)
  private def createProgressionMilestones(techniques: List[BrewingTechnique], level: String): List[ProgressionMilestone] = List.empty
  private def getResourcesForTechniques(techniques: List[BrewingTechnique]): List[LearningResource] = List.empty
}

// ==========================================================================
// TYPES DE SUPPORT POUR PROGRESSION
// ==========================================================================

sealed trait BrewerLevel {
  def value: String
}

object BrewerLevel {
  case object NOVICE extends BrewerLevel { val value = "NOVICE" }
  case object INTERMEDIATE extends BrewerLevel { val value = "INTERMEDIATE" }
  case object ADVANCED extends BrewerLevel { val value = "ADVANCED" }
  case object EXPERT extends BrewerLevel { val value = "EXPERT" }
}

sealed trait ComplexityLevel {
  def value: String
}

object ComplexityLevel {
  case object BEGINNER extends ComplexityLevel { val value = "BEGINNER" }
  case object INTERMEDIATE extends ComplexityLevel { val value = "INTERMEDIATE" }
  case object ADVANCED extends ComplexityLevel { val value = "ADVANCED" }
  case object EXPERT extends ComplexityLevel { val value = "EXPERT" }
}

sealed trait ExplorationPreference {
  def value: String
}

object ExplorationPreference {
  case object CONSERVATIVE extends ExplorationPreference { val value = "CONSERVATIVE" }
  case object BALANCED extends ExplorationPreference { val value = "BALANCED" }
  case object ADVENTUROUS extends ExplorationPreference { val value = "ADVENTUROUS" }
}

sealed trait RecommendationType

object RecommendationType {
  case object SKILL_DEVELOPMENT extends RecommendationType
  case object STYLE_EXPLORATION extends RecommendationType
  case object EQUIPMENT_UPGRADE extends RecommendationType
  case object TECHNIQUE_MASTERY extends RecommendationType
}

case class BrewerPreferences(
  explorationPreference: ExplorationPreference = ExplorationPreference.BALANCED,
  timeCommitment: Int = 10, // heures par semaine
  budget: Option[Double] = None,
  focusAreas: List[String] = List.empty,
  avoidedStyles: List[String] = List.empty
)

object BrewerPreferences {
  val default = BrewerPreferences()
}

case class BrewerHistoricalData(
  totalBatches: Int,
  favoriteStyles: List[String],
  masteredTechniques: List[String],
  successRate: Double,
  averageBatchSize: Double,
  availableEquipment: List[String],
  lastBrewDate: Option[Instant]
)

case class BrewerProfile(
  userId: UserId,
  level: BrewerLevel,
  experience: Int,
  favoriteStyles: List[String],
  masteredTechniques: List[String],
  preferredComplexity: String,
  successRate: Double,
  averageBatchSize: Double,
  equipment: List[String],
  lastBrewDate: Option[Instant]
)

case class ProgressionGaps(
  missingSkills: List[String],
  underexploredStyles: List[String],
  techniqueOpportunities: List[String],
  equipmentUpgrades: List[String],
  knowledgeGaps: List[String]
)

case class ProgressionRecommendation(
  type_ : RecommendationType,
  priority: String, // HIGH, MEDIUM, LOW
  title: String,
  description: String,
  actionItems: List[String],
  estimatedTimeWeeks: Int,
  difficulty: String,
  prerequisites: List[String],
  confidence: Double
)

case class ProgressionMLFeatures(
  userId: String,
  featureVector: Map[String, Double],
  categoricalFeatures: Map[String, String],
  progressionVector: List[Map[String, Any]],
  timestamp: Instant
)

case class ProgressionPlan(
  currentLevel: BrewerLevel,
  targetLevel: BrewerLevel,
  phases: List[ProgressionPhase],
  totalEstimatedWeeks: Int,
  keyMilestones: List[String],
  successMetrics: List[String],
  adaptationStrategy: String
)

case class ProgressionPhase(
  name: String,
  recommendations: List[ProgressionRecommendation],
  estimatedWeeks: Int
)

case class ProgressionMetadata(
  userId: UserId,
  generatedAt: Instant,
  recommendationCount: Int,
  targetLevel: BrewerLevel,
  confidence: Double
)

case class ProgressionRecommendationResult(
  brewerProfile: BrewerProfile,
  currentLevel: BrewerLevel,
  progressionGaps: ProgressionGaps,
  recommendations: List[ProgressionRecommendation],
  progressionPlan: ProgressionPlan,
  mlFeatures: ProgressionMLFeatures,
  metadata: ProgressionMetadata
)

// Types pour recommandations débutants
case class ScoredRecipeRecommendation(
  recipe: RecipeAggregate,
  beginnerScore: Double,
  difficultyLevel: String,
  learningObjectives: List[String],
  estimatedDuration: Int, // en minutes
  requiredEquipment: List[String],
  successTips: List[String]
)

case class BeginnerRecommendationResult(
  recommendedRecipes: List[ScoredRecipeRecommendation],
  learningPath: LearningPath,
  generalTips: List[String],
  equipment: List[String],
  timeline: Timeline
)

case class LearningPath(steps: List[LearningStep], totalWeeks: Int)
case class LearningStep(name: String, description: String, recipes: List[RecipeAggregate])
case class Timeline(weeks: Int, unit: String)

// Types pour progression avancée
case class BrewingSkill(name: String, level: Double, experience: Int)
case class BrewingTechnique(name: String, description: String, difficulty: String, prerequisites: List[String])
case class SkillAssessment(skills: Map[String, Double], overallLevel: Double)

case class TechniqueRecipeRecommendation(
  recipe: RecipeAggregate,
  technique: BrewingTechnique,
  difficultyIncrease: String,
  newSkillsIntroduced: List[String],
  successProbability: Double,
  learningValue: Double
)

case class AdvancedProgressionResult(
  currentSkillAssessment: SkillAssessment,
  nextTechniques: List[BrewingTechnique],
  recommendedRecipes: List[TechniqueRecipeRecommendation],
  progressionPlan: AdvancedProgressionPlan,
  milestones: List[ProgressionMilestone],
  resources: List[LearningResource]
)

case class AdvancedProgressionPlan(phases: List[AdvancedPhase], totalMonths: Int)
case class AdvancedPhase(name: String, techniques: List[BrewingTechnique], estimatedMonths: Int)
case class ProgressionMilestone(name: String, criteria: List[String], reward: String)
case class LearningResource(type_ : String, title: String, url: Option[String], description: String)

// Types pour exploration de styles
case class StyleRecommendation(
  style: String,
  introductionRecipe: Option[RecipeAggregate],
  styleDescription: String,
  difficultyRating: String,
  flavorProfile: FlavorProfile,
  keyTechniques: List[String],
  seasonalSuitability: String,
  progressionValue: Double
)

case class StyleExplorationResult(
  brewedStyles: List[String],
  recommendedStyles: List[StyleRecommendation],
  explorationPlan: ExplorationPlan,
  seasonalRecommendations: List[SeasonalRecommendation],
  flavor: FlavorJourneyMap
)

case class FlavorProfile(characteristics: List[String])
case class ExplorationPlan(phases: List[ExplorationPhase], totalMonths: Int)
case class ExplorationPhase(name: String, styles: List[String], focus: String)
case class SeasonalRecommendation(season: String, styles: List[String], rationale: String)
case class FlavorJourneyMap(journey: List[FlavorMilestone])
case class FlavorMilestone(style: String, flavorNotes: List[String], progression: String)