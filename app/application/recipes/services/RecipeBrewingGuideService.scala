package application.recipes.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.recipes.model._
import domain.recipes.repositories.{RecipeReadRepository, RecipeWriteRepository}
import domain.recipes.services.{RecipeEventStoreService, RecipeSnapshotService}
import domain.hops.repositories.HopReadRepository
import domain.malts.repositories.MaltReadRepository
import domain.yeasts.repositories.YeastReadRepository
import java.time.LocalTime
import domain.common.DomainError
import play.api.libs.json._
import java.time.Instant
import domain.recipes.model.{ComplexityLevel, DifficultyLevel}

/**
 * Service pour la génération de guides de brassage avec Event Sourcing avancé
 * Architecture: GenerateBrewingGuideCommand → BrewingGuideGenerated event → Projection
 * 
 * Fonctionnalité: Timeline détaillée de brassage avec optimisation des procédures
 * Event Sourcing: Utilise table recipe_snapshots (DÉJÀ CRÉÉE) + Event Store
 */
@Singleton
class RecipeBrewingGuideService @Inject()(
  recipeReadRepository: RecipeReadRepository,
  recipeWriteRepository: RecipeWriteRepository,
  eventStoreService: RecipeEventStoreService,
  snapshotService: RecipeSnapshotService,
  hopReadRepository: HopReadRepository,
  maltReadRepository: MaltReadRepository,
  yeastReadRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Génération d'un guide de brassage complet avec Event Sourcing
   */
  def generateBrewingGuide(
    recipeId: RecipeId,
    guideType: GuideType,
    complexity: ComplexityLevel = ComplexityLevel(DifficultyLevel.Intermediate, 5, 5, 5, 5),
    generatedBy: String = "system"
  ): Future[Either[DomainError, BrewingGuideResult]] = {
    
    for {
      // Étape 1: Récupérer la recette existante
      recipeOpt <- recipeReadRepository.findById(recipeId)
      
      result <- recipeOpt match {
        case Some(recipe) =>
          for {
            // Étape 2: Générer timeline et étapes détaillées avec lookup des noms
            timeline <- generateBrewingTimelineAsync(recipe, guideType, complexity)
            detailedSteps <- generateDetailedStepsAsync(recipe, guideType)  
            brewingTips <- Future.successful(generateBrewingTips(recipe, complexity))
            troubleshooting <- Future.successful(generateTroubleshooting(recipe))
            
            // Étape 3: Créer le guide de brassage complet
            brewingGuide = BrewingGuide(
              recipe = recipe,
              steps = detailedSteps,
              timeline = timeline,
              equipment = List("Cuve de brassage", "Thermomètre"),
              tips = brewingTips,
              troubleshooting = troubleshooting
            )
            
            // Étape 4: Créer l'événement Event Sourcing
            guideEvent: BrewingGuideGenerated = BrewingGuideGenerated(
              recipeId = recipeId.toString,
              guideType = guideType.value,
              brewingSteps = s"${detailedSteps.length} étapes générées",
              estimatedDuration = (timeline.preparationTasks ++ timeline.brewingDay ++ timeline.postBrewing).map(_.duration).sum,
              complexityLevel = complexity.overall.name,
              generatedBy = generatedBy,
              version = recipe.aggregateVersion + 1
            )
            
            // Étape 5: Persister l'événement dans l'Event Store
            _ <- eventStoreService.saveEvents(recipeId.toString, List(guideEvent), recipe.aggregateVersion)
            
            // Étape 6: Créer snapshot de la recette enrichie (optionnel)
            _ <- snapshotService.saveSnapshot(recipe)
            
            // Étape 7: Créer le résultat avec métadonnées complètes
            result = BrewingGuideResult(
              brewingGuide = brewingGuide,
              metadata = BrewingGuideMetadata(
                recipeId = recipeId,
                guideType = guideType,
                complexity = complexity,
                totalSteps = detailedSteps.length,
                estimatedDuration = (timeline.preparationTasks ++ timeline.brewingDay ++ timeline.postBrewing).map(_.duration).sum,
                eventId = guideEvent.eventId,
                timestamp = guideEvent.occurredAt,
                version = guideEvent.version
              )
            )
            
          } yield Right(result)
          
        case None =>
          Future.successful(Left(DomainError.notFound(s"Recette non trouvée: ${recipeId.value}", "RECIPE_NOT_FOUND")))
      }
    } yield result
  }

  /**
   * Récupérer l'historique des guides de brassage générés
   */
  def getBrewingGuideHistory(recipeId: RecipeId): Future[List[BrewingGuideEvent]] = {
    eventStoreService.getEvents(recipeId.toString).map { events =>
      events.collect {
        case generated: BrewingGuideGenerated =>
          BrewingGuideEvent(
            eventId = generated.eventId,
            timestamp = generated.occurredAt,
            guideType = generated.guideType,
            complexityLevel = generated.complexityLevel,
            estimatedDuration = generated.estimatedDuration,
            generatedBy = generated.generatedBy,
            version = generated.version
          )
      }
    }
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - GÉNÉRATION DU GUIDE DE BRASSAGE
  // ==========================================================================

  private def generateBrewingTimelineAsync(
    recipe: RecipeAggregate,
    guideType: GuideType,
    complexity: ComplexityLevel
  ): Future[BrewingTimeline] = {
    
    Future.successful {
      import java.time.LocalTime
      
      // Timeline tasks pour préparation
      val preparationTasks = List(
        TimelineTask(
          time = LocalTime.of(8, 0),
          task = "Peser tous les ingrédients",
          duration = 15,
          phase = BrewingPhase.Preparation,
          priority = TaskPriority.High
        ),
        TimelineTask(
          time = LocalTime.of(8, 15),
          task = "Concasser les malts",
          duration = 20,
          phase = BrewingPhase.Preparation,
          priority = TaskPriority.Normal
        ),
        TimelineTask(
          time = LocalTime.of(8, 35),
          task = "Préparer et nettoyer le matériel",
          duration = 25,
          phase = BrewingPhase.Preparation,
          priority = TaskPriority.High
        )
      )
      
      // Timeline tasks pour le jour de brassage
      val brewingDayTasks = List(
        TimelineTask(
          time = LocalTime.of(9, 0),
          task = "Chauffer l'eau d'empâtage à 72°C",
          duration = 30,
          phase = BrewingPhase.Mashing,
          priority = TaskPriority.Critical
        ),
        TimelineTask(
          time = LocalTime.of(9, 30),
          task = "Empâtage - ajouter les malts",
          duration = 90,
          phase = BrewingPhase.Mashing,
          priority = TaskPriority.Critical
        ),
        TimelineTask(
          time = LocalTime.of(11, 0),
          task = "Filtration du moût",
          duration = 45,
          phase = BrewingPhase.Lautering,
          priority = TaskPriority.High
        ),
        TimelineTask(
          time = LocalTime.of(11, 45),
          task = "Ébullition et houblonnage",
          duration = 60,
          phase = BrewingPhase.Boiling,
          priority = TaskPriority.Critical
        ),
        TimelineTask(
          time = LocalTime.of(12, 45),
          task = "Refroidissement rapide du moût",
          duration = 30,
          phase = BrewingPhase.Cooling,
          priority = TaskPriority.Critical
        ),
        TimelineTask(
          time = LocalTime.of(13, 15),
          task = "Ensemencement de la levure",
          duration = 15,
          phase = BrewingPhase.Fermentation,
          priority = TaskPriority.High
        )
      )
      
      // Timeline tasks après brassage
      val postBrewingTasks = List(
        TimelineTask(
          time = LocalTime.of(13, 30),
          task = "Nettoyage complet du matériel",
          duration = 45,
          phase = BrewingPhase.Packaging,
          priority = TaskPriority.Normal
        ),
        TimelineTask(
          time = LocalTime.of(14, 15),
          task = "Ranger et nettoyer l'espace de brassage",
          duration = 30,
          phase = BrewingPhase.Packaging,
          priority = TaskPriority.Normal
        )
      )
      
      BrewingTimeline(
        preparationTasks = preparationTasks,
        brewingDay = brewingDayTasks,
        postBrewing = postBrewingTasks
      )
    }
  }

  private def generateDetailedStepsAsync(recipe: RecipeAggregate, guideType: GuideType): Future[List[BrewingStep]] = {
    for {
      // Récupérer les noms réels des malts
      maltStepsData <- generateMaltStepsDataAsync(recipe)
      // Récupérer les noms réels des houblons pour les étapes de brassage
      hopStepsData <- generateHopStepsAsync(recipe)
      // Récupérer les noms des levures si présentes
      yeastStepsData <- generateYeastStepsAsync(recipe)
    } yield {
      val baseSteps = List(
        BrewingStep(
          order = 1,
          phase = BrewingPhase.Preparation,
          title = "Préparation de l'eau",
          description = s"Préparer ${recipe.batchSize.toLiters.formatted("%.1f")}L d'eau de brassage",
          duration = Some(10),
          instructions = List("Mesurer le volume d'eau nécessaire", "Vérifier la qualité de l'eau"),
          criticalPoints = List("Volume exact crucial pour les calculs"),
          tips = List("Prévoir 10% d'eau supplémentaire pour les pertes")
        ),
        BrewingStep(
          order = 2,
          phase = BrewingPhase.Mashing,
          title = "Empâtage",
          description = "Conversion des amidons en sucres",
          duration = Some(90),
          temperature = Some(67.0),
          instructions = List("Maintenir température constante", "Brasser doucement toutes les 15min"),
          criticalPoints = List("Température critique pour les enzymes"),
          tips = List("Vérifier le pH entre 5.2 et 5.6")
        ),
        BrewingStep(
          order = 3,
          phase = BrewingPhase.Lautering,
          title = "Rinçage (Sparging)",
          description = "Extraction des sucres restants",
          duration = Some(30),
          temperature = Some(78.0),
          instructions = List("Rincer avec eau à 78°C max", "Maintenir vitesse constante"),
          criticalPoints = List("Ne pas dépasser 78°C pour éviter l'extraction de tanins"),
          tips = List("Surveiller la clarté du moût en sortie")
        ),
        BrewingStep(
          order = 4,
          phase = BrewingPhase.Boiling,
          title = "Début ébullition",
          description = "Stérilisation et coagulation des protéines",
          duration = Some(5),
          instructions = List("Porter à ébullition vigoureuse", "Écumer les protéines"),
          criticalPoints = List("Ébullition vigoureuse essentielle"),
          tips = List("Ne pas couvrir complètement pour éviter les débordements")
        ),
        BrewingStep(
          order = 5,
          phase = BrewingPhase.Cooling,
          title = "Refroidissement",
          description = "Refroidissement rapide pour éviter contamination",
          duration = Some(25),
          temperature = Some(20.0),
          instructions = List("Refroidir le plus rapidement possible", "Éviter l'oxydation"),
          criticalPoints = List("Hygiène stricte post-ébullition"),
          tips = List("Utiliser un refroidisseur à plaques si disponible")
        ),
        BrewingStep(
          order = 6,
          phase = BrewingPhase.Fermentation,
          title = "Nettoyage du matériel",
          description = "Désinfection immédiate",
          duration = Some(30),
          instructions = List("Nettoyer immédiatement", "Désinfecter tout le matériel"),
          criticalPoints = List("Nettoyage immédiat pour éviter les dépôts"),
          tips = List("Utiliser des produits adaptés au contact alimentaire")
        )
      )
      
      // Combiner toutes les étapes avec ordre correct
      val allStepsData = maltStepsData ++ hopStepsData ++ yeastStepsData
      val additionalSteps = allStepsData.zipWithIndex.map { case (stepData, idx) =>
        stepData.copy(order = baseSteps.length + idx + 1)
      }
      
      baseSteps ++ additionalSteps
    }
  }

  private def generateMaltStepsDataAsync(recipe: RecipeAggregate): Future[List[BrewingStep]] = {
    val maltStepsFutures = recipe.malts.zipWithIndex.map { case (malt, idx) =>
      maltReadRepository.findById(malt.maltId).map { maltOpt =>
        maltOpt match {
          case Some(maltData) =>
            BrewingStep(
              order = 100 + idx, // Ordre temporaire, sera ajusté plus tard
              phase = BrewingPhase.Preparation,
              title = s"Concassage ${maltData.name.value}",
              description = s"Concasser ${malt.quantityKg}kg de ${maltData.name.value}",
              duration = Some(5),
              instructions = List(s"Peser exactement ${malt.quantityKg}kg", "Concasser avec mouture appropriée", "Vérifier l'homogénéité"),
              criticalPoints = List("Mouture uniforme essentielle pour extraction"),
              tips = List(s"Malt ${maltData.maltType.name} - ajuster la mouture selon le type")
            )
          case None =>
            BrewingStep(
              order = 100 + idx,
              phase = BrewingPhase.Preparation,
              title = s"Concassage malt ID:${malt.maltId.value}",
              description = s"Concasser ${malt.quantityKg}kg de malt",
              duration = Some(5),
              instructions = List(s"Peser exactement ${malt.quantityKg}kg", "Concasser avec mouture standard"),
              criticalPoints = List("Mouture uniforme essentielle"),
              tips = List("ID malt non trouvé - utiliser mouture standard")
            )
        }
      }
    }
    Future.sequence(maltStepsFutures.toList)
  }

  private def generateHopStepsAsync(recipe: RecipeAggregate): Future[List[BrewingStep]] = {
    val hopStepsFutures = recipe.hops.map { hop =>
      hopReadRepository.findById(hop.hopId).map { hopOpt =>
        hopOpt match {
          case Some(hopData) =>
            List(BrewingStep(
              order = 200 + hop.additionTime,
              phase = BrewingPhase.Boiling,
              title = s"Ajout ${hopData.name.value}",
              description = s"Ajouter ${hop.quantityGrams}g de ${hopData.name.value}",
              duration = Some(2),
              temperature = Some(100.0),
              instructions = List(s"Ajouter ${hop.quantityGrams}g à T-${hop.additionTime}min", "Remuer délicatement"),
              criticalPoints = List("Timing précis essentiel"),
              tips = List(s"Usage: ${hopData.usage.value} - arômes: ${hopData.aromaProfile.take(2).mkString(", ")}")
            ))
          case None =>
            List(BrewingStep(
              order = 200 + hop.additionTime,
              phase = BrewingPhase.Boiling,
              title = s"Ajout houblon ${hop.hopId.toString}",
              description = s"Ajouter ${hop.quantityGrams}g de houblon",
              duration = Some(2),
              temperature = Some(100.0),
              instructions = List(s"Ajouter ${hop.quantityGrams}g à T-${hop.additionTime}min", "Remuer délicatement"),
              criticalPoints = List("Timing précis essentiel"),
              tips = List(s"Usage: ${hop.usage.name} - ID non trouvé dans la base")
            ))
        }
      }
    }
    Future.sequence(hopStepsFutures.toSeq).map(_.flatten.toList)
  }

  private def generateYeastStepsAsync(recipe: RecipeAggregate): Future[List[BrewingStep]] = {
    recipe.yeast match {
      case Some(yeast) =>
        yeastReadRepository.findById(yeast.yeastId).map { yeastOpt =>
          yeastOpt match {
            case Some(yeastData) =>
              List(BrewingStep(
                order = 500,
                phase = BrewingPhase.Fermentation,
                title = s"Ensemencement ${yeastData.name.value}",
                description = s"Ajouter ${yeast.quantityGrams}g de levure ${yeastData.name.value}",
                duration = Some(10),
                temperature = Some(yeastData.fermentationTemp.min.toDouble),
                instructions = List(
                  s"Vérifier température entre ${yeastData.fermentationTemp.min}°C et ${yeastData.fermentationTemp.max}°C",
                  s"Ajouter ${yeast.quantityGrams}g de levure",
                  "Mélanger délicatement"
                ),
                criticalPoints = List("Température de fermentation critique", "Hygiène absolue"),
                tips = List(s"Type: ${yeastData.yeastType.name}", s"Atténuation: ${yeastData.attenuationRange.min}-${yeastData.attenuationRange.max}%")
              ))
            case None =>
              List(BrewingStep(
                order = 500,
                phase = BrewingPhase.Fermentation,
                title = s"Ensemencement levure ${yeast.yeastId.toString}",
                description = s"Ajouter ${yeast.quantityGrams}g de levure",
                duration = Some(10),
                temperature = Some(25.0),
                instructions = List(
                  "Vérifier température < 25°C",
                  s"Ajouter ${yeast.quantityGrams}g de levure",
                  "Mélanger délicatement"
                ),
                criticalPoints = List("Température critique", "Hygiène absolue"),
                tips = List("ID levure non trouvé - utiliser dosage standard")
              ))
          }
        }
      case None =>
        Future.successful(List(BrewingStep(
          order = 500,
          phase = BrewingPhase.Fermentation,
          title = "Ensemencement levure",
          description = "Ajouter la levure dans le moût refroidi",
          duration = Some(5),
          temperature = Some(25.0),
          instructions = List("Vérifier température < 25°C", "Ajouter la levure"),
          criticalPoints = List("Température critique"),
          tips = List("Levure inconnue - utiliser dosage standard")
        )))
    }
  }


  private def generateBrewingTips(recipe: RecipeAggregate, complexity: ComplexityLevel): List[BrewingTip] = {
    List(
      BrewingTip(
        category = TipCategory.Quality,
        title = "Température d'empâtage",
        description = "Maintenir 65-68°C pendant l'empâtage pour une conversion optimale des amidons",
        importance = TipImportance.Critical,
        phase = Some(BrewingPhase.Mashing)
      ),
      BrewingTip(
        category = TipCategory.Quality,
        title = "pH du moût",
        description = "Vérifier que le pH du moût se situe entre 5.2 et 5.6 pour une extraction optimale",
        importance = TipImportance.Important,
        phase = Some(BrewingPhase.Mashing)
      ),
      BrewingTip(
        category = TipCategory.Technique,
        title = "Densité initiale",
        description = s"Viser une OG de ${recipe.calculations.originalGravity.getOrElse(1.050)} pour ce style",
        importance = TipImportance.Important,
        phase = Some(BrewingPhase.Boiling)
      ),
      BrewingTip(
        category = TipCategory.Safety,
        title = "Hygiène post-ébullition",
        description = "Désinfecter rigoureusement tout équipement en contact avec le moût refroidi",
        importance = TipImportance.Critical,
        phase = Some(BrewingPhase.Cooling)
      )
    )
  }

  private def generateTroubleshooting(recipe: RecipeAggregate): List[TroubleshootingItem] = {
    List(
      TroubleshootingItem(
        problem = "Température d'empâtage trop élevée",
        symptoms = List("Température dépasse 70°C", "Risque de dénaturation enzymatique"),
        causes = List("Eau d'empâtage trop chaude", "Ajout de grain trop rapide"),
        solutions = List("Ajouter de l'eau froide graduellement", "Attendre le refroidissement naturel", "Brasser énergiquement"),
        prevention = List("Préchauffer la cuve d'empâtage", "Mesurer la température avant ajout du grain", "Calculer la température d'infusion")
      ),
      TroubleshootingItem(
        problem = "Ébullition insuffisamment vigoureuse",
        symptoms = List("Pas de mouvement visible du moût", "Vapeur faible ou inexistante", "Coagulation protéique insuffisante"),
        causes = List("Puissance de chauffe insuffisante", "Volume de moût trop important", "Casserole inadaptée"),
        solutions = List("Augmenter la puissance de chauffe", "Couvrir partiellement le récipient", "Réduire légèrement le volume"),
        prevention = List("Vérifier la puissance de chauffe avant le jour J", "Adapter la taille du récipient au volume", "Prévoir une marge de puissance")
      ),
      TroubleshootingItem(
        problem = "Refroidissement trop lent",
        symptoms = List("Température > 25°C après 45 minutes", "Risque de contamination accru", "Fermentation retardée"),
        causes = List("Refroidisseur sous-dimensionné", "Eau de refroidissement trop chaude", "Circulation insuffisante"),
        solutions = List("Utiliser un bain de glace", "Améliorer la circulation d'eau", "Pré-refroidir l'équipement"),
        prevention = List("Tester le refroidisseur avant utilisation", "Préparer suffisamment de glace", "Vérifier la température de l'eau du réseau")
      )
    )
  }
}

// ==========================================================================
// TYPES DE SUPPORT POUR GUIDES DE BRASSAGE
// ==========================================================================

sealed trait GuideType {
  def value: String
}

object GuideType {
  case object Basic extends GuideType { val value = "BASIC" }
  case object Detailed extends GuideType { val value = "DETAILED" }
  case object Professional extends GuideType { val value = "PROFESSIONAL" }
  
  def fromString(value: String): Option[GuideType] = value.toUpperCase match {
    case "BASIC" => Some(Basic)
    case "DETAILED" => Some(Detailed)
    case "PROFESSIONAL" => Some(Professional)
    case _ => None
  }
}

case class BrewingGuideResult(
  brewingGuide: BrewingGuide,
  metadata: BrewingGuideMetadata
)

case class BrewingGuideMetadata(
  recipeId: RecipeId,
  guideType: GuideType,
  complexity: ComplexityLevel,
  totalSteps: Int,
  estimatedDuration: Int,
  eventId: String,
  timestamp: Instant,
  version: Int
)

case class BrewingGuideEvent(
  eventId: String,
  timestamp: Instant,
  guideType: String,
  complexityLevel: String,
  estimatedDuration: Int,
  generatedBy: String,
  version: Int
)