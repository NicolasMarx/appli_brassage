package application.recipes.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.recipes.model._
import domain.recipes.repositories.{RecipeReadRepository, RecipeWriteRepository}
import domain.recipes.services.{RecipeEventStoreService, RecipeSnapshotService}
import domain.common.DomainError
import play.api.libs.json._
import java.time.Instant

/**
 * Service pour l'analyse et validation de recettes avec Event Sourcing
 * Endpoint: POST /api/v1/recipes/analyze
 * Architecture: AnalyzeRecipeCommand → RecipeAnalyzed event → Projections
 * 
 * Fonctionnalité: Validation flexible, analyse de balance, conformité aux styles
 * Event Sourcing: Utilise table recipe_snapshots + Event Store
 */
@Singleton
class RecipeAnalyzeService @Inject()(
  recipeReadRepository: RecipeReadRepository,
  recipeWriteRepository: RecipeWriteRepository,
  eventStoreService: RecipeEventStoreService,
  snapshotService: RecipeSnapshotService
)(implicit ec: ExecutionContext) {

  /**
   * Analyser une recette selon différents critères avec Event Sourcing
   */
  def analyzeRecipe(
    recipeId: RecipeId,
    analysisType: AnalysisType,
    validationLevel: ValidationLevel = ValidationLevel.Standard,
    analyzedBy: String = "system"
  ): Future[Either[DomainError, RecipeAnalysisResult]] = {
    
    for {
      // Étape 1: Récupérer la recette existante
      recipeOpt <- recipeReadRepository.findById(recipeId)
      
      result <- recipeOpt match {
        case Some(recipe) =>
          
          // Étape 2: Effectuer l'analyse selon le type demandé
          val analysisResults = analysisType match {
            case AnalysisType.Balance =>
              performBalanceAnalysis(recipe, validationLevel)
            case AnalysisType.StyleCompliance =>
              performStyleComplianceAnalysis(recipe, validationLevel)
            case AnalysisType.FullAnalysis =>
              performFullAnalysis(recipe, validationLevel)
            case AnalysisType.QualityAssessment =>
              performQualityAssessment(recipe, validationLevel)
          }
          
          // Étape 3: Déterminer le status global de validation
          val validationStatus = determineValidationStatus(analysisResults)
          val recommendations = generateRecommendations(recipe, analysisResults, validationLevel)
          
          // Étape 4: Créer l'événement Event Sourcing
          val analysisEvent = RecipeAnalyzed(
            recipeId = recipeId.toString,
            analysisType = analysisType.value,
            analysisResults = "{}",  // JSON serialization simplifié
            validationStatus = validationStatus.value,
            recommendations = "[]", // JSON serialization simplifié
            analyzedBy = analyzedBy,
            version = recipe.aggregateVersion + 1
          )
          
          // Étape 5: Persister l'événement dans l'Event Store
          for {
            _ <- eventStoreService.saveEvents(recipeId.toString, List(analysisEvent), recipe.aggregateVersion)
            
            // Étape 6: Optionnel - sauver snapshot si analyse modifie la recette
            _ <- if (validationStatus == ValidationStatus.Valid) {
              snapshotService.saveSnapshot(recipe)
            } else {
              Future.successful(())
            }
            
            // Étape 7: Créer le résultat d'analyse
            result = RecipeAnalysisResult(
              recipe = recipe,
              analysisType = analysisType,
              validationLevel = validationLevel,
              analysisResults = analysisResults,
              validationStatus = validationStatus,
              recommendations = recommendations,
              qualityScore = calculateQualityScore(analysisResults),
              metadata = AnalysisMetadata(
                analyzedAt = Instant.now(),
                eventId = analysisEvent.eventId,
                version = analysisEvent.version,
                totalChecks = analysisResults.checks.length,
                passedChecks = analysisResults.checks.count(_.status == CheckStatus.Pass),
                warningChecks = analysisResults.checks.count(_.status == CheckStatus.Warning),
                errorChecks = analysisResults.checks.count(_.status == CheckStatus.Error)
              )
            )
            
          } yield Right(result)
          
        case None =>
          Future.successful(Left(DomainError.notFound(s"Recette non trouvée: ${recipeId.value}", "RECIPE_NOT_FOUND")))
      }
    } yield result
  }

  /**
   * Récupérer l'historique des analyses effectuées sur une recette
   */
  def getAnalysisHistory(recipeId: RecipeId): Future[List[RecipeAnalysisEvent]] = {
    // Simulation - dans l'implémentation réelle, utiliser eventStoreService
    Future.successful(List.empty)
  }

  /**
   * Valider une recette avec des critères flexibles
   */
  def validateRecipe(
    recipeId: RecipeId,
    customCriteria: List[ValidationCriterion] = List.empty,
    strictMode: Boolean = false
  ): Future[Either[DomainError, RecipeValidationResult]] = {
    
    for {
      recipeOpt <- recipeReadRepository.findById(recipeId)
      
      result <- recipeOpt match {
        case Some(recipe) =>
          val validationLevel = if (strictMode) ValidationLevel.Strict else ValidationLevel.Flexible
          val checks = performValidationChecks(recipe, customCriteria, validationLevel)
          val overallStatus = if (checks.exists(_.status == CheckStatus.Error)) {
            ValidationStatus.Invalid
          } else if (checks.exists(_.status == CheckStatus.Warning)) {
            ValidationStatus.Warnings
          } else {
            ValidationStatus.Valid
          }
          
          Future.successful(Right(RecipeValidationResult(
            recipe = recipe,
            validationChecks = checks,
            overallStatus = overallStatus,
            customCriteria = customCriteria,
            strictMode = strictMode,
            validatedAt = Instant.now()
          )))
          
        case None =>
          Future.successful(Left(DomainError.notFound(s"Recette non trouvée: ${recipeId.value}", "RECIPE_NOT_FOUND")))
      }
    } yield result
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - ANALYSES SPÉCIALISÉES
  // ==========================================================================

  /**
   * Analyse de l'équilibre de la recette
   */
  private def performBalanceAnalysis(recipe: RecipeAggregate, validationLevel: ValidationLevel): AnalysisResults = {
    val checks = scala.collection.mutable.ListBuffer[ValidationCheck]()
    
    // Vérification équilibre malt/houblon
    val maltWeight = recipe.malts.map(_.quantity).sum
    val hopWeight = recipe.hops.map(_.quantity).sum
    val maltHopRatio = if (hopWeight > 0) maltWeight / hopWeight else 0.0
    
    if (maltHopRatio < 10.0 || maltHopRatio > 200.0) {
      checks += ValidationCheck(
        category = "BALANCE",
        checkType = "MALT_HOP_RATIO",
        description = "Rapport malt/houblon déséquilibré",
        status = CheckStatus.Warning,
        message = s"Ratio malt/houblon: ${maltHopRatio.toInt}:1 (recommandé: 20-100:1)",
        suggestion = Some("Ajuster les quantités de malt ou de houblon")
      )
    } else {
      checks += ValidationCheck(
        category = "BALANCE",
        checkType = "MALT_HOP_RATIO",
        description = "Rapport malt/houblon équilibré",
        status = CheckStatus.Pass,
        message = s"Ratio malt/houblon optimal: ${maltHopRatio.toInt}:1",
        suggestion = None
      )
    }
    
    // Vérification équilibre IBU/OG
    val ibu = recipe.calculations.ibu.getOrElse(0.0)
    val og = recipe.calculations.originalGravity.getOrElse(1.040)
    val ibuOgRatio = ibu / ((og - 1.0) * 1000)
    
    if (ibuOgRatio < 0.3 || ibuOgRatio > 1.5) {
      checks += ValidationCheck(
        category = "BALANCE",
        checkType = "IBU_OG_RATIO",
        description = "Équilibre amertume/sucrosité",
        status = CheckStatus.Warning,
        message = s"Ratio IBU/OG: ${ibuOgRatio.formatted("%.2f")} (recommandé: 0.5-1.0)",
        suggestion = Some("Ajuster le houblonnage ou la densité")
      )
    } else {
      checks += ValidationCheck(
        category = "BALANCE", 
        checkType = "IBU_OG_RATIO",
        description = "Équilibre amertume/sucrosité optimal",
        status = CheckStatus.Pass,
        message = s"Ratio IBU/OG équilibré: ${ibuOgRatio.formatted("%.2f")}",
        suggestion = None
      )
    }
    
    // Vérification diversité des houblons
    val hopVarieties = recipe.hops.map(_.hopId.value).distinct.length
    if (hopVarieties == 0) {
      checks += ValidationCheck(
        category = "BALANCE",
        checkType = "HOP_DIVERSITY",
        description = "Absence de houblon",
        status = CheckStatus.Error,
        message = "Aucun houblon dans la recette",
        suggestion = Some("Ajouter au moins un houblon pour l'amertume")
      )
    } else if (hopVarieties == 1 && recipe.hops.length > 1) {
      checks += ValidationCheck(
        category = "BALANCE",
        checkType = "HOP_DIVERSITY", 
        description = "Diversité limitée des houblons",
        status = CheckStatus.Warning,
        message = "Un seul type de houblon utilisé",
        suggestion = Some("Considérer l'ajout d'houblons aromatiques différents")
      )
    } else {
      checks += ValidationCheck(
        category = "BALANCE",
        checkType = "HOP_DIVERSITY",
        description = "Bonne diversité des houblons",
        status = CheckStatus.Pass,
        message = s"${hopVarieties} variétés de houblon utilisées",
        suggestion = None
      )
    }
    
    AnalysisResults(
      analysisType = "BALANCE",
      checks = checks.toList,
      summary = BalanceAnalysisSummary(
        maltHopRatio = maltHopRatio,
        ibuOgRatio = ibuOgRatio,
        hopDiversity = hopVarieties,
        overallBalance = calculateOverallBalance(checks.toList)
      )
    )
  }

  /**
   * Analyse de conformité au style
   */
  private def performStyleComplianceAnalysis(recipe: RecipeAggregate, validationLevel: ValidationLevel): AnalysisResults = {
    val checks = scala.collection.mutable.ListBuffer[ValidationCheck]()
    val styleName = recipe.style.name
    
    // Vérification ABV selon le style
    recipe.calculations.abv.foreach { abv =>
      val (minAbv, maxAbv) = getStyleAbvRange(styleName)
      if (abv < minAbv || abv > maxAbv) {
        checks += ValidationCheck(
          category = "STYLE_COMPLIANCE",
          checkType = "ABV_RANGE",
          description = s"ABV hors normes pour ${styleName}",
          status = if (validationLevel == ValidationLevel.Strict) CheckStatus.Error else CheckStatus.Warning,
          message = s"ABV ${abv}% (attendu: ${minAbv}-${maxAbv}%)",
          suggestion = Some("Ajuster les malts ou la densité finale")
        )
      } else {
        checks += ValidationCheck(
          category = "STYLE_COMPLIANCE",
          checkType = "ABV_RANGE",
          description = s"ABV conforme pour ${styleName}",
          status = CheckStatus.Pass,
          message = s"ABV ${abv}% dans la norme",
          suggestion = None
        )
      }
    }
    
    // Vérification IBU selon le style  
    recipe.calculations.ibu.foreach { ibu =>
      val (minIbu, maxIbu) = getStyleIbuRange(styleName)
      if (ibu < minIbu || ibu > maxIbu) {
        checks += ValidationCheck(
          category = "STYLE_COMPLIANCE",
          checkType = "IBU_RANGE", 
          description = s"IBU hors normes pour ${styleName}",
          status = if (validationLevel == ValidationLevel.Strict) CheckStatus.Error else CheckStatus.Warning,
          message = s"IBU ${ibu.toInt} (attendu: ${minIbu.toInt}-${maxIbu.toInt})",
          suggestion = Some("Ajuster le houblonnage")
        )
      } else {
        checks += ValidationCheck(
          category = "STYLE_COMPLIANCE",
          checkType = "IBU_RANGE",
          description = s"IBU conforme pour ${styleName}",
          status = CheckStatus.Pass,
          message = s"IBU ${ibu.toInt} dans la norme",
          suggestion = None
        )
      }
    }
    
    // Vérification ingrédients typiques du style
    val traditionalIngredients = getTraditionalIngredients(styleName)
    val recipeIngredients = (recipe.hops.map(_.hopId.value.toString) ++ recipe.malts.map(_.maltId.value.toString)).toSet
    val traditionalCount = recipeIngredients.intersect(traditionalIngredients.toSet).size
    
    if (traditionalCount < traditionalIngredients.length / 2) {
      checks += ValidationCheck(
        category = "STYLE_COMPLIANCE",
        checkType = "TRADITIONAL_INGREDIENTS",
        description = s"Ingrédients peu typiques pour ${styleName}",
        status = CheckStatus.Warning,
        message = s"Seulement ${traditionalCount}/${traditionalIngredients.length} ingrédients traditionnels",
        suggestion = Some(s"Considérer l'utilisation de: ${traditionalIngredients.take(3).mkString(", ")}")
      )
    } else {
      checks += ValidationCheck(
        category = "STYLE_COMPLIANCE", 
        checkType = "TRADITIONAL_INGREDIENTS",
        description = s"Bonne utilisation d'ingrédients traditionnels",
        status = CheckStatus.Pass,
        message = s"${traditionalCount}/${traditionalIngredients.length} ingrédients traditionnels utilisés",
        suggestion = None
      )
    }
    
    AnalysisResults(
      analysisType = "STYLE_COMPLIANCE",
      checks = checks.toList,
      summary = StyleComplianceAnalysisSummary(
        styleName = styleName,
        abvCompliance = recipe.calculations.abv.map(calculateAbvCompliance(_, styleName)).getOrElse(0.0),
        ibuCompliance = recipe.calculations.ibu.map(calculateIbuCompliance(_, styleName)).getOrElse(0.0),
        ingredientCompliance = calculateIngredientCompliance(recipeIngredients.toList, styleName),
        overallCompliance = calculateOverallCompliance(checks.toList)
      )
    )
  }

  /**
   * Analyse complète (combine toutes les analyses)
   */
  private def performFullAnalysis(recipe: RecipeAggregate, validationLevel: ValidationLevel): AnalysisResults = {
    val balanceAnalysis = performBalanceAnalysis(recipe, validationLevel)
    val styleAnalysis = performStyleComplianceAnalysis(recipe, validationLevel)
    val qualityAnalysis = performQualityAssessment(recipe, validationLevel)
    
    val allChecks = balanceAnalysis.checks ++ styleAnalysis.checks ++ qualityAnalysis.checks
    
    AnalysisResults(
      analysisType = "FULL_ANALYSIS",
      checks = allChecks,
      summary = FullAnalysisSummary(
        balanceScore = calculateBalanceScore(balanceAnalysis.checks),
        styleComplianceScore = calculateStyleComplianceScore(styleAnalysis.checks),
        qualityScore = calculateQualityScore(qualityAnalysis),
        overallScore = calculateOverallScore(allChecks),
        strengths = identifyStrengths(allChecks),
        improvements = identifyImprovements(allChecks)
      )
    )
  }

  /**
   * Évaluation de la qualité générale
   */
  private def performQualityAssessment(recipe: RecipeAggregate, validationLevel: ValidationLevel): AnalysisResults = {
    val checks = scala.collection.mutable.ListBuffer[ValidationCheck]()
    
    // Cohérence des calculs
    if (recipe.calculations.abv.isDefined && recipe.calculations.originalGravity.isDefined && recipe.calculations.finalGravity.isDefined) {
      val calculatedAbv = calculateAbvFromGravities(
        recipe.calculations.originalGravity.get,
        recipe.calculations.finalGravity.get
      )
      val declaredAbv = recipe.calculations.abv.get
      val abvDifference = Math.abs(calculatedAbv - declaredAbv)
      
      if (abvDifference > 0.5) {
        checks += ValidationCheck(
          category = "QUALITY",
          checkType = "CALCULATION_CONSISTENCY",
          description = "Incohérence dans les calculs ABV",
          status = CheckStatus.Warning,
          message = s"ABV calculé: ${calculatedAbv.formatted("%.1f")}%, déclaré: ${declaredAbv.formatted("%.1f")}%",
          suggestion = Some("Vérifier les calculs de densité et ABV")
        )
      } else {
        checks += ValidationCheck(
          category = "QUALITY",
          checkType = "CALCULATION_CONSISTENCY",
          description = "Calculs cohérents",
          status = CheckStatus.Pass,
          message = "ABV cohérent avec les densités",
          suggestion = None
        )
      }
    }
    
    // Faisabilité du brassage
    val totalGrainWeight = recipe.malts.map(_.quantity).sum
    val batchSizeKg = recipe.batchSize.toLiters
    val grainToBatchRatio = totalGrainWeight / batchSizeKg
    
    if (grainToBatchRatio < 0.8 || grainToBatchRatio > 2.0) {
      checks += ValidationCheck(
        category = "QUALITY",
        checkType = "BREWING_FEASIBILITY",
        description = "Ratio grain/volume inhabituel",
        status = CheckStatus.Warning,
        message = s"${grainToBatchRatio.formatted("%.1f")} kg/L (normal: 1.0-1.5 kg/L)",
        suggestion = Some("Vérifier les quantités de grain et volume")
      )
    } else {
      checks += ValidationCheck(
        category = "QUALITY",
        checkType = "BREWING_FEASIBILITY", 
        description = "Ratio grain/volume approprié",
        status = CheckStatus.Pass,
        message = s"Ratio grain/volume: ${grainToBatchRatio.formatted("%.1f")} kg/L",
        suggestion = None
      )
    }
    
    // Complétude de la recette
    val completenessScore = calculateRecipeCompleteness(recipe)
    if (completenessScore < 0.7) {
      checks += ValidationCheck(
        category = "QUALITY",
        checkType = "RECIPE_COMPLETENESS",
        description = "Recette incomplète",
        status = CheckStatus.Warning,
        message = s"Complétude: ${(completenessScore * 100).toInt}%",
        suggestion = Some("Ajouter les informations manquantes")
      )
    } else {
      checks += ValidationCheck(
        category = "QUALITY",
        checkType = "RECIPE_COMPLETENESS",
        description = "Recette bien documentée",
        status = CheckStatus.Pass,
        message = s"Complétude: ${(completenessScore * 100).toInt}%",
        suggestion = None
      )
    }
    
    AnalysisResults(
      analysisType = "QUALITY_ASSESSMENT",
      checks = checks.toList,
      summary = QualityAnalysisSummary(
        calculationConsistency = !checks.exists(c => c.checkType == "CALCULATION_CONSISTENCY" && c.status != CheckStatus.Pass),
        brewingFeasibility = !checks.exists(c => c.checkType == "BREWING_FEASIBILITY" && c.status != CheckStatus.Pass),
        recipeCompleteness = completenessScore,
        overallQuality = calculateOverallQuality(checks.toList, completenessScore)
      )
    )
  }

  /**
   * Effectuer des vérifications de validation personnalisées
   */
  private def performValidationChecks(
    recipe: RecipeAggregate, 
    customCriteria: List[ValidationCriterion],
    validationLevel: ValidationLevel
  ): List[ValidationCheck] = {
    val checks = scala.collection.mutable.ListBuffer[ValidationCheck]()
    
    // Vérifications de base
    if (recipe.malts.isEmpty) {
      checks += ValidationCheck(
        category = "BASIC",
        checkType = "HAS_MALTS",
        description = "Présence de malts",
        status = CheckStatus.Error,
        message = "Aucun malt dans la recette",
        suggestion = Some("Ajouter au moins un malt de base")
      )
    }
    
    if (recipe.hops.isEmpty && validationLevel != ValidationLevel.Flexible) {
      checks += ValidationCheck(
        category = "BASIC", 
        checkType = "HAS_HOPS",
        description = "Présence de houblons",
        status = CheckStatus.Warning,
        message = "Aucun houblon dans la recette",
        suggestion = Some("Ajouter au moins un houblon pour l'amertume")
      )
    }
    
    if (recipe.yeast.isEmpty && validationLevel == ValidationLevel.Strict) {
      checks += ValidationCheck(
        category = "BASIC",
        checkType = "HAS_YEAST",
        description = "Levure spécifiée",
        status = CheckStatus.Warning,
        message = "Aucune levure spécifiée",
        suggestion = Some("Spécifier le type de levure à utiliser")
      )
    }
    
    // Vérifications personnalisées
    customCriteria.foreach { criterion =>
      val check = evaluateCustomCriterion(recipe, criterion)
      checks += check
    }
    
    checks.toList
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def determineValidationStatus(analysisResults: AnalysisResults): ValidationStatus = {
    if (analysisResults.checks.exists(_.status == CheckStatus.Error)) {
      ValidationStatus.Invalid
    } else if (analysisResults.checks.exists(_.status == CheckStatus.Warning)) {
      ValidationStatus.Warnings
    } else {
      ValidationStatus.Valid
    }
  }

  private def generateRecommendations(
    recipe: RecipeAggregate, 
    analysisResults: AnalysisResults,
    validationLevel: ValidationLevel
  ): List[AnalysisRecommendation] = {
    val recommendations = scala.collection.mutable.ListBuffer[AnalysisRecommendation]()
    
    analysisResults.checks.filter(_.status != CheckStatus.Pass).foreach { check =>
      check.suggestion.foreach { suggestion =>
        recommendations += AnalysisRecommendation(
          category = check.category,
          priority = if (check.status == CheckStatus.Error) "HIGH" else "MEDIUM",
          title = check.description,
          description = suggestion,
          impact = "Améliore la qualité de la recette"
        )
      }
    }
    
    recommendations.toList
  }

  private def calculateQualityScore(analysisResults: AnalysisResults): Double = {
    val totalChecks = analysisResults.checks.length
    if (totalChecks == 0) return 1.0
    
    val passedChecks = analysisResults.checks.count(_.status == CheckStatus.Pass)
    val warningChecks = analysisResults.checks.count(_.status == CheckStatus.Warning)
    
    (passedChecks + warningChecks * 0.5) / totalChecks
  }

  private def evaluateCustomCriterion(recipe: RecipeAggregate, criterion: ValidationCriterion): ValidationCheck = {
    // Implémentation simplifiée - à étendre selon les besoins
    ValidationCheck(
      category = "CUSTOM",
      checkType = criterion.name,
      description = criterion.description,
      status = CheckStatus.Pass,
      message = "Critère personnalisé évalué",
      suggestion = None
    )
  }

  private def calculateAbvFromGravities(og: Double, fg: Double): Double = {
    (og - fg) * 131.25
  }

  private def calculateRecipeCompleteness(recipe: RecipeAggregate): Double = {
    var score = 0.0
    
    // Éléments essentiels
    if (recipe.malts.nonEmpty) score += 0.3
    if (recipe.hops.nonEmpty) score += 0.2  
    if (recipe.yeast.isDefined) score += 0.1
    if (recipe.calculations.abv.isDefined) score += 0.1
    if (recipe.calculations.ibu.isDefined) score += 0.1
    if (recipe.calculations.originalGravity.isDefined) score += 0.1
    if (recipe.description.isDefined) score += 0.05
    if (recipe.procedures.mashProfile.isDefined) score += 0.05
    
    score
  }

  // Méthodes helper pour les styles (simplifiées)
  private def getStyleAbvRange(styleName: String): (Double, Double) = styleName.toLowerCase match {
    case s if s.contains("ipa") => (5.0, 7.5)
    case s if s.contains("stout") => (4.0, 9.0)
    case s if s.contains("lager") => (4.0, 6.0)
    case s if s.contains("wheat") => (4.5, 5.5)
    case _ => (3.0, 12.0) // Plage large par défaut
  }

  private def getStyleIbuRange(styleName: String): (Double, Double) = styleName.toLowerCase match {
    case s if s.contains("ipa") => (40.0, 80.0)
    case s if s.contains("stout") => (20.0, 50.0)
    case s if s.contains("lager") => (8.0, 25.0)
    case s if s.contains("wheat") => (10.0, 30.0)
    case _ => (0.0, 100.0) // Plage large par défaut
  }

  private def getTraditionalIngredients(styleName: String): List[String] = styleName.toLowerCase match {
    case s if s.contains("ipa") => List("Cascade", "Centennial", "Pale Ale", "Munich")
    case s if s.contains("stout") => List("Roasted Barley", "Black Patent", "Fuggles", "East Kent Goldings")
    case s if s.contains("lager") => List("Pilsner", "Munich", "Hallertau", "Saaz")
    case s if s.contains("wheat") => List("Wheat", "Pilsner", "Hallertau", "Tettnang")
    case _ => List.empty
  }

  // Méthodes de calcul des scores (simplifiées)
  private def calculateOverallBalance(checks: List[ValidationCheck]): Double = 
    checks.count(_.status == CheckStatus.Pass).toDouble / checks.length

  private def calculateAbvCompliance(abv: Double, styleName: String): Double = {
    val (min, max) = getStyleAbvRange(styleName)
    if (abv >= min && abv <= max) 1.0 else 0.5
  }

  private def calculateIbuCompliance(ibu: Double, styleName: String): Double = {
    val (min, max) = getStyleIbuRange(styleName)
    if (ibu >= min && ibu <= max) 1.0 else 0.5
  }

  private def calculateIngredientCompliance(ingredients: List[String], styleName: String): Double = {
    val traditional = getTraditionalIngredients(styleName).toSet
    val recipeSet = ingredients.toSet
    if (traditional.isEmpty) 1.0 else recipeSet.intersect(traditional).size.toDouble / traditional.size
  }

  private def calculateOverallCompliance(checks: List[ValidationCheck]): Double =
    checks.count(_.status == CheckStatus.Pass).toDouble / checks.length

  private def calculateBalanceScore(checks: List[ValidationCheck]): Double =
    checks.count(_.status == CheckStatus.Pass).toDouble / checks.length

  private def calculateStyleComplianceScore(checks: List[ValidationCheck]): Double =
    checks.count(_.status == CheckStatus.Pass).toDouble / checks.length

  private def calculateOverallScore(checks: List[ValidationCheck]): Double =
    checks.count(_.status == CheckStatus.Pass).toDouble / checks.length

  private def calculateOverallQuality(checks: List[ValidationCheck], completeness: Double): Double =
    (checks.count(_.status == CheckStatus.Pass).toDouble / checks.length + completeness) / 2.0

  private def identifyStrengths(checks: List[ValidationCheck]): List[String] =
    checks.filter(_.status == CheckStatus.Pass).map(_.description)

  private def identifyImprovements(checks: List[ValidationCheck]): List[String] =
    checks.filter(_.status != CheckStatus.Pass).flatMap(_.suggestion)
}

// ==========================================================================
// TYPES DE SUPPORT POUR ANALYSE
// ==========================================================================

sealed trait AnalysisType {
  def value: String
}

object AnalysisType {
  case object Balance extends AnalysisType { val value = "BALANCE" }
  case object StyleCompliance extends AnalysisType { val value = "STYLE_COMPLIANCE" }
  case object FullAnalysis extends AnalysisType { val value = "FULL_ANALYSIS" }
  case object QualityAssessment extends AnalysisType { val value = "QUALITY_ASSESSMENT" }
}

sealed trait ValidationLevel {
  def value: String
}

object ValidationLevel {
  case object Flexible extends ValidationLevel { val value = "FLEXIBLE" }
  case object Standard extends ValidationLevel { val value = "STANDARD" }
  case object Strict extends ValidationLevel { val value = "STRICT" }
}

sealed trait ValidationStatus {
  def value: String
}

object ValidationStatus {
  case object Valid extends ValidationStatus { val value = "VALID" }
  case object Warnings extends ValidationStatus { val value = "WARNINGS" }
  case object Invalid extends ValidationStatus { val value = "INVALID" }
}

sealed trait CheckStatus
object CheckStatus {
  case object Pass extends CheckStatus
  case object Warning extends CheckStatus  
  case object Error extends CheckStatus
}

case class RecipeAnalysisResult(
  recipe: RecipeAggregate,
  analysisType: AnalysisType,
  validationLevel: ValidationLevel,
  analysisResults: AnalysisResults,
  validationStatus: ValidationStatus,
  recommendations: List[AnalysisRecommendation],
  qualityScore: Double,
  metadata: AnalysisMetadata
)

case class AnalysisResults(
  analysisType: String,
  checks: List[ValidationCheck],
  summary: Any // Peut être BalanceAnalysisSummary, StyleComplianceAnalysisSummary, etc.
)

case class ValidationCheck(
  category: String,
  checkType: String,
  description: String,
  status: CheckStatus,
  message: String,
  suggestion: Option[String]
)

case class AnalysisRecommendation(
  category: String,
  priority: String,
  title: String,
  description: String,
  impact: String
)

case class AnalysisMetadata(
  analyzedAt: Instant,
  eventId: String,
  version: Int,
  totalChecks: Int,
  passedChecks: Int,
  warningChecks: Int,
  errorChecks: Int
)

case class RecipeAnalysisEvent(
  eventId: String,
  timestamp: Instant,
  analysisType: String,
  validationStatus: String,
  qualityScore: Double,
  analyzedBy: String,
  version: Int
)

case class RecipeValidationResult(
  recipe: RecipeAggregate,
  validationChecks: List[ValidationCheck],
  overallStatus: ValidationStatus,
  customCriteria: List[ValidationCriterion],
  strictMode: Boolean,
  validatedAt: Instant
)

case class ValidationCriterion(
  name: String,
  description: String,
  checkFunction: String, // Nom de la fonction de vérification
  errorMessage: String,
  warningMessage: Option[String] = None
)

// Summaries pour les différents types d'analyse
case class BalanceAnalysisSummary(
  maltHopRatio: Double,
  ibuOgRatio: Double,
  hopDiversity: Int,
  overallBalance: Double
)

case class StyleComplianceAnalysisSummary(
  styleName: String,
  abvCompliance: Double,
  ibuCompliance: Double,
  ingredientCompliance: Double,
  overallCompliance: Double
)

case class FullAnalysisSummary(
  balanceScore: Double,
  styleComplianceScore: Double,
  qualityScore: Double,
  overallScore: Double,
  strengths: List[String],
  improvements: List[String]
)

case class QualityAnalysisSummary(
  calculationConsistency: Boolean,
  brewingFeasibility: Boolean,
  recipeCompleteness: Double,
  overallQuality: Double
)