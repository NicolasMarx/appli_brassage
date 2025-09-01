# Domaine YEASTS - Architecture et Infrastructure

## 🏛️ Architecture DDD/CQRS

Le domaine Yeasts implémente une architecture sophistiquée pour gérer la complexité des levures : souches multiples, caractéristiques de fermentation, compatibilités de styles et prédictions de performance.

**Statut** : 🔜 **À IMPLÉMENTER - Phase 2B Planifiée**

---

## 📁 Structure des Couches Prévue

### Domain Layer (Couche Domaine)

```
app/domain/yeasts/
├── model/
│   ├── YeastAggregate.scala               🔜 Agrégat principal
│   ├── YeastId.scala                      🔜 Identity unique (UUID)
│   ├── YeastName.scala                    🔜 Value Object nom
│   ├── YeastStrain.scala                  🔜 Souche (WLP001, S-04, etc.)
│   ├── YeastType.scala                    🔜 Enum (ALE, LAGER, WILD, SPECIALTY)
│   ├── YeastLaboratory.scala              🔜 Laboratoire (White Labs, Wyeast, etc.)
│   ├── AttenuationRange.scala             🔜 Plage atténuation (65-90%)
│   ├── FermentationTemperature.scala      🔜 Température optimale
│   ├── AlcoholTolerance.scala             🔜 Tolérance alcoolique maximum
│   ├── FlocculationLevel.scala            🔜 Enum (LOW, MEDIUM, HIGH)
│   ├── YeastCharacteristics.scala         🔜 Profil gustatif (esters, phénols)
│   ├── YeastViability.scala               🔜 Viabilité selon date/stockage
│   ├── YeastForm.scala                    🔜 Enum (LIQUID, DRY, SLANT)
│   ├── YeastStatus.scala                  🔜 Enum (ACTIVE, SEASONAL, DISCONTINUED)
│   ├── YeastFilter.scala                  🔜 Filtres recherche spécialisés
│   └── YeastEvents.scala                  🔜 Domain Events
│
├── services/
│   ├── YeastDomainService.scala           🔜 Logique métier transverse
│   ├── YeastValidationService.scala       🔜 Validations business rules
│   ├── YeastRecommendationService.scala   🔜 Recommandations par style
│   ├── YeastSubstitutionService.scala     🔜 Substitutions levures
│   ├── FermentationPredictionService.scala🔜 Prédictions fermentation
│   └── YeastFilterService.scala           🔜 Service filtrage avancé
│
└── repositories/
    ├── YeastRepository.scala              🔜 Interface repository principale
    ├── YeastReadRepository.scala          🔜 Interface lecture (Query)
    └── YeastWriteRepository.scala         🔜 Interface écriture (Command)
```

### Application Layer (Couche Application)

```
app/application/yeasts/
├── commands/
│   ├── CreateYeastCommand.scala           🔜 Command création
│   ├── CreateYeastCommandHandler.scala    🔜 Handler création
│   ├── UpdateYeastCommand.scala           🔜 Command modification
│   ├── UpdateYeastCommandHandler.scala    🔜 Handler modification
│   ├── DeleteYeastCommand.scala           🔜 Command suppression
│   ├── DeleteYeastCommandHandler.scala    🔜 Handler suppression
│   ├── UpdateViabilityCommand.scala       🔜 Command maj viabilité
│   └── UpdateViabilityCommandHandler.scala🔜 Handler viabilité
│
├── queries/
│   ├── YeastListQuery.scala               🔜 Query liste paginée
│   ├── YeastListQueryHandler.scala        🔜 Handler liste
│   ├── YeastDetailQuery.scala             🔜 Query détail
│   ├── YeastDetailQueryHandler.scala      🔜 Handler détail
│   ├── YeastSearchQuery.scala             🔜 Query recherche avancée
│   ├── YeastSearchQueryHandler.scala      🔜 Handler recherche
│   ├── YeastByStyleQuery.scala            🔜 Query par style bière
│   ├── YeastByStyleQueryHandler.scala     🔜 Handler par style
│   ├── YeastByLaboratoryQuery.scala       🔜 Query par laboratoire
│   ├── YeastByLaboratoryQueryHandler.scala🔜 Handler par laboratoire
│   ├── YeastSubstitutesQuery.scala        🔜 Query substitutions
│   └── YeastSubstitutesQueryHandler.scala 🔜 Handler substitutions
│
├── predictions/
│   ├── FermentationOutcome.scala          🔜 Prédiction résultats fermentation
│   ├── AttentuationPredictor.scala        🔜 Prédiction atténuation
│   ├── FlavorProfilePredictor.scala       🔜 Prédiction profil gustatif
│   └── FermentationTimePredictor.scala    🔜 Prédiction durée fermentation
│
└── dto/
    ├── YeastDto.scala                     🔜 DTO transfert données
    ├── YeastListDto.scala                 🔜 DTO liste avec pagination
    ├── YeastSearchDto.scala               🔜 DTO résultats recherche
    ├── FermentationPredictionDto.scala    🔜 DTO prédictions fermentation
    └── YeastCompatibilityDto.scala        🔜 DTO compatibilité styles
```

### Infrastructure Layer (Couche Infrastructure)

```
app/infrastructure/
├── persistence/yeasts/
│   ├── YeastTables.scala                  🔜 Définitions tables Slick
│   ├── YeastCharacteristicsTables.scala   🔜 Tables profils gustatifs
│   ├── YeastStyleCompatibilityTables.scala🔜 Tables compatibilité styles
│   ├── SlickYeastReadRepository.scala     🔜 Implémentation lecture
│   ├── SlickYeastWriteRepository.scala    🔜 Implémentation écriture
│   └── YeastRowMapper.scala               🔜 Mapping Row ↔ Domain complexe
│
├── external/
│   ├── YeastLabApiClient.scala            🔜 Client API laboratoires
│   ├── WhiteLabsClient.scala              🔜 Client spécialisé White Labs
│   ├── WyeastClient.scala                 🔜 Client spécialisé Wyeast
│   └── YeastDataScraper.scala             🔜 Scraping données automatique
│
├── serialization/
│   ├── YeastJsonFormats.scala             🔜 Formats JSON Play
│   ├── YeastEventSerialization.scala     🔜 Sérialisation events
│   └── FermentationDataFormats.scala     🔜 Formats données fermentation
│
└── configuration/
    ├── YeastModule.scala                  🔜 Injection dépendances Guice
    ├── YeastDatabaseConfig.scala          🔜 Configuration base données
    └── YeastLabApiConfig.scala            🔜 Configuration APIs externes
```

### Interface Layer (Couche Interface)

```
app/interfaces/http/
├── controllers/
│   ├── YeastsController.scala             🔜 API publique (/api/v1/yeasts)
│   ├── AdminYeastsController.scala        🔜 API admin (/api/admin/yeasts)
│   └── YeastPredictionsController.scala   🔜 API prédictions fermentation
│
├── dto/
│   ├── requests/
│   │   ├── CreateYeastRequest.scala       🔜 DTO requête création
│   │   ├── UpdateYeastRequest.scala       🔜 DTO requête modification
│   │   ├── YeastSearchRequest.scala       🔜 DTO requête recherche
│   │   ├── FermentationPredictionRequest.scala🔜 DTO prédiction
│   │   └── YeastRecommendationRequest.scala🔜 DTO recommandation
│   │
│   └── responses/
│       ├── YeastResponse.scala            🔜 DTO réponse détail
│       ├── YeastListResponse.scala        🔜 DTO réponse liste
│       ├── YeastSearchResponse.scala      🔜 DTO réponse recherche
│       ├── FermentationPredictionResponse.scala🔜 DTO prédiction
│       ├── YeastCompatibilityResponse.scala🔜 DTO compatibilité
│       └── YeastRecommendationResponse.scala🔜 DTO recommandation
│
└── validation/
    ├── YeastRequestValidator.scala        🔜 Validation requêtes HTTP
    ├── FermentationConstraints.scala     🔜 Contraintes fermentation
    └── YeastCharacteristicsValidator.scala🔜 Validation caractéristiques
```

---

## 🗃️ Infrastructure Base de Données Complexe

### Schéma PostgreSQL Principal

```sql
-- Table principale yeasts
CREATE TABLE yeasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    strain VARCHAR(50) NOT NULL, -- WLP001, S-04, 1056, etc.
    yeast_type VARCHAR(20) NOT NULL CHECK (yeast_type IN ('ALE', 'LAGER', 'WILD', 'SPECIALTY')),
    laboratory VARCHAR(50) NOT NULL, -- White Labs, Wyeast, Lallemand, etc.
    laboratory_code VARCHAR(20), -- Code laboratoire (WLP001, 1056, etc.)
    
    -- Caractéristiques de fermentation
    attenuation_min DECIMAL(4,2) NOT NULL CHECK (attenuation_min >= 50 AND attenuation_min <= 100),
    attenuation_max DECIMAL(4,2) NOT NULL CHECK (attenuation_max >= attenuation_min),
    temperature_min DECIMAL(4,1) NOT NULL CHECK (temperature_min >= 0),
    temperature_max DECIMAL(4,1) NOT NULL CHECK (temperature_max >= temperature_min),
    temperature_optimal_min DECIMAL(4,1) NOT NULL,
    temperature_optimal_max DECIMAL(4,1) NOT NULL,
    alcohol_tolerance DECIMAL(4,2) NOT NULL CHECK (alcohol_tolerance >= 5 AND alcohol_tolerance <= 25),
    
    -- Caractéristiques physiques
    flocculation_level VARCHAR(10) NOT NULL CHECK (flocculation_level IN ('LOW', 'MEDIUM', 'HIGH')),
    yeast_form VARCHAR(10) NOT NULL CHECK (yeast_form IN ('LIQUID', 'DRY', 'SLANT')),
    
    -- Profil gustatif et métadonnées
    flavor_profile TEXT[], -- Descripteurs: fruity, clean, spicy, phenolic, etc.
    ester_production VARCHAR(10) CHECK (ester_production IN ('LOW', 'MEDIUM', 'HIGH')),
    phenol_production BOOLEAN NOT NULL DEFAULT false, -- POF+ ou POF-
    
    -- Disponibilité et viabilité
    seasonal_availability BOOLEAN NOT NULL DEFAULT false,
    availability_months INTEGER[], -- [1,2,3,10,11,12] pour hiver par exemple
    viability_months INTEGER NOT NULL DEFAULT 6, -- Durée viabilité en mois
    
    -- Métadonnées standard
    origin_id UUID REFERENCES origins(id),
    description TEXT,
    usage_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table des équivalences entre laboratoires
CREATE TABLE yeast_equivalences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_yeast_id UUID NOT NULL REFERENCES yeasts(id),
    equivalent_yeast_id UUID NOT NULL REFERENCES yeasts(id),
    confidence_level DECIMAL(3,2) NOT NULL CHECK (confidence_level >= 0 AND confidence_level <= 1),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table de compatibilité avec styles de bière
CREATE TABLE yeast_beer_style_compatibility (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    yeast_id UUID NOT NULL REFERENCES yeasts(id),
    beer_style_id UUID NOT NULL REFERENCES beer_styles(id),
    compatibility_score DECIMAL(3,2) NOT NULL CHECK (compatibility_score >= 0 AND compatibility_score <= 1),
    is_traditional BOOLEAN NOT NULL DEFAULT false,
    is_recommended BOOLEAN NOT NULL DEFAULT false,
    notes TEXT
);

-- Table historique performance fermentation
CREATE TABLE fermentation_performances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    yeast_id UUID NOT NULL REFERENCES yeasts(id),
    recipe_id UUID, -- Référence recette si disponible
    
    -- Conditions fermentation
    temperature DECIMAL(4,1) NOT NULL,
    original_gravity DECIMAL(5,3) NOT NULL,
    final_gravity DECIMAL(5,3) NOT NULL,
    actual_attenuation DECIMAL(4,2) NOT NULL,
    
    -- Résultats
    fermentation_duration_days INTEGER NOT NULL,
    flavor_descriptors TEXT[],
    clarity VARCHAR(20), -- Clear, Hazy, Cloudy
    user_rating DECIMAL(3,2) CHECK (user_rating >= 1 AND user_rating <= 5),
    
    -- Métadonnées
    equipment_type VARCHAR(50),
    user_id UUID, -- Référence utilisateur
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Index de Performance Spécialisés

```sql
-- Index principaux pour recherche
CREATE INDEX idx_yeasts_type ON yeasts(yeast_type);
CREATE INDEX idx_yeasts_laboratory ON yeasts(laboratory);
CREATE INDEX idx_yeasts_strain ON yeasts(strain);
CREATE INDEX idx_yeasts_status ON yeasts(status);

-- Index pour recherche par caractéristiques fermentation
CREATE INDEX idx_yeasts_attenuation ON yeasts(attenuation_min, attenuation_max) WHERE status = 'ACTIVE';
CREATE INDEX idx_yeasts_temperature ON yeasts(temperature_min, temperature_max) WHERE status = 'ACTIVE';
CREATE INDEX idx_yeasts_alcohol_tolerance ON yeasts(alcohol_tolerance) WHERE status = 'ACTIVE';

-- Index composite pour recommandations
CREATE INDEX idx_yeasts_recommendation ON yeasts(yeast_type, flocculation_level, attenuation_min, attenuation_max) 
    WHERE status = 'ACTIVE';

-- Index pour disponibilité saisonnière
CREATE INDEX idx_yeasts_seasonal ON yeasts(seasonal_availability, availability_months) WHERE status = 'ACTIVE';

-- Index pour équivalences
CREATE INDEX idx_yeast_equivalences_primary ON yeast_equivalences(primary_yeast_id);
CREATE INDEX idx_yeast_equivalences_equivalent ON yeast_equivalences(equivalent_yeast_id);
CREATE INDEX idx_yeast_equivalences_confidence ON yeast_equivalences(confidence_level DESC);

-- Index pour compatibilité styles
CREATE INDEX idx_yeast_style_compatibility ON yeast_beer_style_compatibility(yeast_id, beer_style_id);
CREATE INDEX idx_yeast_style_traditional ON yeast_beer_style_compatibility(beer_style_id, is_traditional) 
    WHERE is_traditional = true;
```

---

## ⚙️ Event Sourcing et Prédictions

### Événements du Domaine Levures

```scala
sealed trait YeastEvent extends DomainEvent {
  def aggregateId: YeastId
  def version: Long
  def timestamp: Instant
}

case class YeastCreated(
  aggregateId: YeastId,
  yeastData: YeastCreationData,
  version: Long = 1,
  timestamp: Instant = Instant.now()
) extends YeastEvent

case class YeastCharacteristicsUpdated(
  aggregateId: YeastId,
  oldCharacteristics: YeastCharacteristics,
  newCharacteristics: YeastCharacteristics,
  source: UpdateSource, // LAB_DATA, USER_FEEDBACK, PERFORMANCE_DATA
  version: Long,
  timestamp: Instant = Instant.now()
) extends YeastEvent

case class FermentationPerformanceRecorded(
  aggregateId: YeastId,
  performanceData: FermentationPerformanceData,
  recipeContext: Option[RecipeContext],
  version: Long,
  timestamp: Instant = Instant.now()
) extends YeastEvent

case class YeastEquivalenceEstablished(
  aggregateId: YeastId,
  equivalentYeastId: YeastId,
  confidenceLevel: BigDecimal,
  evidenceSource: EvidenceSource, // LAB_SPEC, USER_EXPERIENCE, GENETIC_ANALYSIS
  version: Long,
  timestamp: Instant = Instant.now()
) extends YeastEvent
```

### Algorithmes de Prédiction

```scala
trait FermentationPredictionService {
  def predictAttenuation(
    yeast: YeastAggregate,
    originalGravity: BigDecimal,
    temperature: BigDecimal,
    oxygenLevel: OxygenLevel
  ): Either[PredictionError, AttenuationPrediction]
  
  def predictFermentationTime(
    yeast: YeastAggregate,
    fermentationConditions: FermentationConditions
  ): Either[PredictionError, FermentationTimePrediction]
  
  def predictFlavorProfile(
    yeast: YeastAggregate,
    recipeContext: RecipeContext
  ): Either[PredictionError, FlavorProfilePrediction]
}

class AIEnhancedFermentationPredictor @Inject()(
  performanceRepository: FermentationPerformanceRepository,
  mlModelService: MLModelService
) extends FermentationPredictionService {

  def predictAttenuation(
    yeast: YeastAggregate,
    originalGravity: BigDecimal,
    temperature: BigDecimal,
    oxygenLevel: OxygenLevel
  ): Either[PredictionError, AttenuationPrediction] = {
    
    for {
      // Récupérer historique performances similaires
      historicalData <- performanceRepository.findSimilarConditions(
        yeast.id, originalGravity, temperature
      ).toEither
      
      // Ajuster selon conditions spécifiques
      baseAttenuation <- calculateBaseAttenuation(yeast, originalGravity, temperature)
      oxygenAdjustment <- calculateOxygenAdjustment(oxygenLevel)
      temperatureStress <- calculateTemperatureStress(yeast, temperature)
      
      // Utiliser ML si suffisamment de données historiques
      finalPrediction <- if (historicalData.size >= 5) {
        mlModelService.predictAttenuation(yeast, originalGravity, temperature, historicalData)
      } else {
        Right(baseAttenuation + oxygenAdjustment - temperatureStress)
      }
      
      confidenceLevel <- calculateConfidenceLevel(historicalData.size, yeast.dataQuality)
      
    } yield AttenuationPrediction(
      predictedAttenuation = finalPrediction,
      confidenceLevel = confidenceLevel,
      predictionRange = calculatePredictionRange(finalPrediction, confidenceLevel),
      basisData = historicalData.map(_.summarize),
      warnings = generateWarnings(yeast, temperature, originalGravity)
    )
  }
  
  private def calculateBaseAttenuation(
    yeast: YeastAggregate,
    og: BigDecimal,
    temp: BigDecimal
  ): Either[PredictionError, BigDecimal] = {
    
    val baseAttenuation = yeast.attenuationRange.average
    
    // Ajustements selon température
    val tempAdjustment = if (temp < yeast.temperatureRange.optimal.min) {
      -(yeast.temperatureRange.optimal.min - temp) * 0.5 // -0.5% par °C sous optimum
    } else if (temp > yeast.temperatureRange.optimal.max) {
      (temp - yeast.temperatureRange.optimal.max) * 0.3 // +0.3% par °C au-dessus (stress)
    } else {
      BigDecimal(0)
    }
    
    // Ajustements selon gravité
    val gravityAdjustment = if (og > 1.070) {
      -(og - 1.070) * 100 * 0.2 // Réduction atténuation pour hautes gravités
    } else {
      BigDecimal(0)
    }
    
    Right((baseAttenuation + tempAdjustment + gravityAdjustment).max(50).min(95))
  }
}
```

---

## 🔌 API Avancée et Recommandations

### Routes Spécialisées

```scala
# Routes publiques yeasts
GET     /api/v1/yeasts                          controllers.YeastsController.list(page: Option[Int], size: Option[Int])
GET     /api/v1/yeasts/:id                      controllers.YeastsController.detail(id: String)
GET     /api/v1/yeasts/type/:type               controllers.YeastsController.byType(type: String)
GET     /api/v1/yeasts/laboratory/:lab          controllers.YeastsController.byLaboratory(lab: String)
POST    /api/v1/yeasts/search                   controllers.YeastsController.search
GET     /api/v1/yeasts/:id/equivalents          controllers.YeastsController.equivalents(id: String)
GET     /api/v1/yeasts/:id/substitutes          controllers.YeastsController.substitutes(id: String)

# Routes recommandations et prédictions
POST    /api/v1/yeasts/recommend                controllers.YeastsController.recommend
POST    /api/v1/yeasts/predict                  controllers.YeastPredictionsController.predictFermentation
POST    /api/v1/yeasts/:id/predict-attenuation  controllers.YeastPredictionsController.predictAttenuation(id: String)
POST    /api/v1/yeasts/:id/predict-flavor       controllers.YeastPredictionsController.predictFlavorProfile(id: String)

# Routes admin yeasts - CRUD + gestion équivalences
GET     /api/admin/yeasts                       controllers.AdminYeastsController.list(page: Option[Int], size: Option[Int])
POST    /api/admin/yeasts                       controllers.AdminYeastsController.create
GET     /api/admin/yeasts/:id                   controllers.AdminYeastsController.detail(id: String)
PUT     /api/admin/yeasts/:id                   controllers.AdminYeastsController.update(id: String)
DELETE  /api/admin/yeasts/:id                   controllers.AdminYeastsController.delete(id: String)
POST    /api/admin/yeasts/:id/equivalences      controllers.AdminYeastsController.addEquivalence(id: String)
DELETE  /api/admin/yeasts/:id/equivalences/:eqId controllers.AdminYeastsController.removeEquivalence(id: String, eqId: String)
POST    /api/admin/yeasts/sync                  controllers.AdminYeastsController.syncWithLabs
```

### Controller avec IA

```scala
@Singleton
class YeastsController @Inject()(
  yeastRecommendationService: YeastRecommendationService,
  yeastSubstitutionService: YeastSubstitutionService,
  cc: ControllerComponents
) extends AbstractController(cc) {

  // Recommandations intelligentes selon style de bière
  def recommend(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[YeastRecommendationRequest] match {
      case JsSuccess(recommendRequest, _) =>
        yeastRecommendationService.recommendForStyle(
          beerStyleId = recommendRequest.beerStyleId,
          gravityRange = recommendRequest.gravityRange,
          temperatureConstraints = recommendRequest.temperatureConstraints,
          flavorPreferences = recommendRequest.flavorPreferences,
          equipmentType = recommendRequest.equipmentType
        ).map { recommendations =>
          Ok(Json.toJson(YeastRecommendationResponse(
            recommendations = recommendations.map(YeastRecommendationDto.fromDomain),
            totalFound = recommendations.length,
            searchCriteria = recommendRequest
          )))
        }
      case JsError(errors) =>
        Future.successful(BadRequest(Json.toJson(ValidationErrorResponse(errors))))
    }
  }

  // Équivalences entre laboratoires
  def equivalents(id: String): Action[AnyContent] = Action.async {
    YeastId.fromString(id) match {
      case Some(yeastId) =>
        for {
          yeastOpt <- yeastDetailQueryHandler.handle(YeastDetailQuery(yeastId))
          equivalents <- yeastOpt match {
            case Some(yeast) => yeastEquivalenceService.findEquivalents(yeast)
            case None => Future.successful(Seq.empty)
          }
        } yield {
          Ok(Json.toJson(YeastEquivalenceResponse(
            originalYeast = yeastOpt.map(YeastResponse.fromAggregate),
            equivalents = equivalents.map(equiv => YeastEquivalenceDto(
              yeast = YeastResponse.fromAggregate(equiv.yeast),
              confidenceLevel = equiv.confidenceLevel,
              differences = equiv.differences,
              notes = equiv.notes
            ))
          )))
        }
      case None =>
        Future.successful(BadRequest(Json.toJson(ErrorResponse("Invalid yeast ID"))))
    }
  }
}

@Singleton
class YeastPredictionsController @Inject()(
  fermentationPredictionService: FermentationPredictionService,
  attenuationPredictor: AttenuationPredictor,
  flavorProfilePredictor: FlavorProfilePredictor,
  cc: ControllerComponents
) extends AbstractController(cc) {

  def predictAttenuation(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    (for {
      yeastId <- YeastId.fromString(id).toRight(BadRequest(Json.toJson(ErrorResponse("Invalid yeast ID"))))
      predictionRequest <- request.body.validate[AttenuationPredictionRequest].asEither
        .left.map(errors => BadRequest(Json.toJson(ValidationErrorResponse(errors))))
    } yield (yeastId, predictionRequest)) match {
      
      case Right((yeastId, predRequest)) =>
        attenuationPredictor.predict(
          yeastId = yeastId,
          originalGravity = predRequest.originalGravity,
          temperature = predRequest.temperature,
          oxygenLevel = predRequest.oxygenLevel,
          pitchingRate = predRequest.pitchingRate
        ).map {
          case Right(prediction) =>
            Ok(Json.toJson(AttenuationPredictionResponse(
              predictedAttenuation = prediction.attenuation,
              confidenceLevel = prediction.confidence,
              predictionRange = prediction.range,
              estimatedDuration = prediction.duration,
              warnings = prediction.warnings,
              recommendations = prediction.recommendations
            )))
          case Left(error) =>
            UnprocessableEntity(Json.toJson(ErrorResponse(error.message)))
        }
        
      case Left(errorResponse) =>
        Future.successful(errorResponse)
    }
  }
}
```

### Intelligence et Machine Learning

```scala
@Singleton
class YeastRecommendationService @Inject()(
  yeastRepository: YeastReadRepository,
  beerStyleRepository: BeerStyleReadRepository,
  fermentationDataRepository: FermentationPerformanceRepository,
  mlRecommendationEngine: MLRecommendationEngine
) {

  def recommendForStyle(
    beerStyleId: BeerStyleId,
    gravityRange: Option[GravityRange] = None,
    temperatureConstraints: Option[TemperatureConstraints] = None,
    flavorPreferences: Option[FlavorPreferences] = None,
    equipmentType: Option[EquipmentType] = None
  ): Future[Seq[YeastRecommendation]] = {
    
    for {
      // Récupérer style de bière cible
      beerStyle <- beerStyleRepository.findById(beerStyleId)
      
      // Chercher levures compatibles traditionnelles
      traditionalYeasts <- yeastRepository.findTraditionalForStyle(beerStyleId)
      
      // Chercher levures alternatives performantes  
      alternativeYeasts <- yeastRepository.findAlternativesForStyle(
        beerStyleId, gravityRange, temperatureConstraints
      )
      
      // Analyser performances historiques
      performanceData <- fermentationDataRepository.findByStyleAndConditions(
        beerStyleId, gravityRange, temperatureConstraints
      )
      
      // Utiliser ML pour scoring avancé
      mlRecommendations <- mlRecommendationEngine.scoreYeasts(
        candidates = traditionalYeasts ++ alternativeYeasts,
        targetStyle = beerStyle,
        constraints = RecommendationConstraints(
          gravityRange, temperatureConstraints, flavorPreferences, equipmentType
        ),
        historicalData = performanceData
      )
      
    } yield mlRecommendations
      .sortBy(_.score).reverse
      .take(10) // Top 10 recommandations
      .map(enrichWithExplanations)
  }
  
  private def enrichWithExplanations(
    recommendation: MLYeastRecommendation
  ): YeastRecommendation = {
    val explanations = Seq.newBuilder[RecommendationExplanation]
    
    if (recommendation.isTraditional) {
      explanations += RecommendationExplanation(
        reason = "Traditional choice",
        description = s"Historically used for ${recommendation.yeast.compatibleStyles.mkString(", ")}"
      )
    }
    
    if (recommendation.performanceScore > 0.8) {
      explanations += RecommendationExplanation(
        reason = "Excellent performance",
        description = s"${(recommendation.performanceScore * 100).toInt}% success rate in similar conditions"
      )
    }
    
    if (recommendation.flavorMatch > 0.7) {
      explanations += RecommendationExplanation(
        reason = "Flavor profile match",
        description = s"Produces desired ${recommendation.expectedFlavors.mkString(", ")} characteristics"
      )
    }
    
    YeastRecommendation(
      yeast = recommendation.yeast,
      score = recommendation.score,
      confidence = recommendation.confidence,
      explanations = explanations.result(),
      expectedOutcome = recommendation.expectedOutcome,
      warnings = recommendation.warnings,
      alternatives = recommendation.alternatives
    )
  }
}