# Domaine RECIPES - Architecture et Infrastructure

## 🏛️ Architecture DDD/CQRS Complexe

Le domaine Recipes constitue l'agrégat le plus sophistiqué de la plateforme, orchestrant tous les autres domaines pour créer des recettes de brassage complètes avec calculs automatiques, scaling intelligent et intégration IA.

**Statut** : 🔜 **À IMPLÉMENTER - Phase 4 Planifiée**

---

## 📁 Structure des Couches Avancée

### Domain Layer (Couche Domaine)

```
app/domain/recipes/
├── model/
│   ├── RecipeAggregate.scala              🔜 Agrégat principal complexe
│   ├── RecipeId.scala                     🔜 Identity unique (UUID)
│   ├── RecipeName.scala                   🔜 Value Object nom
│   ├── RecipeDescription.scala            🔜 Description détaillée
│   ├── RecipeStatus.scala                 🔜 Enum (DRAFT, PUBLISHED, ARCHIVED, PRIVATE)
│   ├── RecipeVersion.scala                🔜 Versioning recettes
│   ├── BatchSize.scala                    🔜 Volume brassage (5L, 10L, 20L...)
│   ├── RecipeComplexity.scala             🔜 Niveau difficulté (BEGINNER, INTERMEDIATE, ADVANCED)
│   │
│   ├── ingredients/
│   │   ├── RecipeIngredient.scala         🔜 Ingrédient + quantité + timing
│   │   ├── HopIngredient.scala            🔜 Houblon + timing + usage spécifique
│   │   ├── MaltIngredient.scala           🔜 Malt + quantité + % dans bill
│   │   ├── YeastIngredient.scala          🔜 Levure + quantité + starter
│   │   ├── OtherIngredient.scala          🔜 Autres (épices, fruits, clarifiants)
│   │   ├── IngredientQuantity.scala       🔜 Quantité avec unités multiples
│   │   ├── HopTiming.scala                🔜 Timing ajout houblon précis
│   │   ├── MaltBillPercentage.scala       🔜 Pourcentage dans facture maltée
│   │   └── IngredientPurpose.scala        🔜 Usage spécifique ingrédient
│   │
│   ├── procedures/
│   │   ├── BrewingProcedure.scala         🔜 Procédure complète
│   │   ├── ProcedureStep.scala            🔜 Étape individuelle
│   │   ├── StepType.scala                 🔜 Enum (MASH, BOIL, FERMENT, PACKAGE)
│   │   ├── StepDuration.scala             🔜 Durée étape
│   │   ├── StepTemperature.scala          🔜 Température cible
│   │   ├── MashProfile.scala              🔜 Profil empâtage multi-paliers
│   │   ├── BoilSchedule.scala             🔜 Planning ébullition avec additions
│   │   ├── FermentationProfile.scala      🔜 Profil fermentation (température/temps)
│   │   ├── EquipmentProfile.scala         🔜 Profil équipement utilisé
│   │   └── ProcedureNotes.scala           🔜 Notes et astuces par étape
│   │
│   ├── calculations/
│   │   ├── RecipeCharacteristics.scala    🔜 ABV, IBU, SRM, OG, FG
│   │   ├── GravityCalculation.scala       🔜 Calculs densités avec efficiency
│   │   ├── BitternessCalculation.scala    🔜 Calcul IBU (Tinseth, Rager)
│   │   ├── ColorCalculation.scala         🔜 Calcul couleur SRM/EBC (Morey)
│   │   ├── AlcoholCalculation.scala       🔜 Calcul ABV précis
│   │   ├── AttenuationCalculation.scala   🔜 Calcul atténuation prédite
│   │   ├── EfficiencyFactor.scala         🔜 Facteur efficacité équipement
│   │   ├── VolumeScaling.scala            🔜 Scaling volumes avec ajustements
│   │   ├── HopUtilizationFactor.scala     🔜 Facteur utilisation houblon
│   │   └── CalculationValidation.scala    🔜 Validation cohérence calculs
│   │
│   ├── equipment/
│   │   ├── BrewingEquipment.scala         🔜 Équipement général
│   │   ├── MiniBrew.scala                 🔜 Spécialisation MiniBrew
│   │   ├── TraditionalEquipment.scala     🔜 Setup traditionnel
│   │   ├── ProfessionalEquipment.scala    🔜 Équipement professionnel
│   │   ├── EquipmentEfficiency.scala      🔜 Efficacité par équipement
│   │   ├── ScalingProfile.scala           🔜 Profil scaling équipement
│   │   ├── EquipmentLimitations.scala     🔜 Limites équipement
│   │   └── EquipmentCalibration.scala     🔜 Calibration utilisateur
│   │
│   ├── validation/
│   │   ├── RecipeValidator.scala          🔜 Validation recette complète
│   │   ├── StyleCompliance.scala          🔜 Conformité style BJCP
│   │   ├── IngredientBalance.scala        🔜 Équilibre ingrédients
│   │   ├── ProcedureValidation.scala      🔜 Validation procédures
│   │   └── SafetyValidation.scala         🔜 Validation sécurité brassage
│   │
│   └── RecipeEvents.scala                 🔜 Domain events complexes
│
├── services/
│   ├── RecipeDomainService.scala          🔜 Logique métier transverse
│   ├── RecipeValidationService.scala      🔜 Validation business rules
│   ├── RecipeCalculationService.scala     🔜 Calculs automatiques
│   ├── RecipeScalingService.scala         🔜 Service scaling intelligent
│   ├── StyleComplianceService.scala       🔜 Vérification conformité BJCP
│   ├── IngredientBalanceService.scala     🔜 Analyse équilibre recette
│   ├── ProcedureGenerationService.scala   🔜 Génération procédures automatique
│   ├── CostEstimationService.scala        🔜 Estimation coûts ingrédients
│   └── RecipeOptimizationService.scala    🔜 Optimisation IA
│
└── repositories/
    ├── RecipeRepository.scala             🔜 Interface repository principale
    ├── RecipeReadRepository.scala         🔜 Interface lecture (Query)
    ├── RecipeWriteRepository.scala        🔜 Interface écriture (Command)
    ├── RecipeCalculationRepository.scala  🔜 Cache calculs complexes
    └── RecipeVersionRepository.scala      🔜 Gestion versions
```

### Application Layer (Couche Application)

```
app/application/recipes/
├── commands/
│   ├── CreateRecipeCommand.scala          🔜 Command création
│   ├── CreateRecipeCommandHandler.scala   🔜 Handler création avec calculs
│   ├── UpdateRecipeCommand.scala          🔜 Command modification
│   ├── UpdateRecipeCommandHandler.scala   🔜 Handler modification + recalcul
│   ├── ScaleRecipeCommand.scala           🔜 Command scaling volume
│   ├── ScaleRecipeCommandHandler.scala    🔜 Handler scaling intelligent
│   ├── OptimizeRecipeCommand.scala        🔜 Command optimisation IA
│   ├── OptimizeRecipeCommandHandler.scala 🔜 Handler optimisation
│   ├── PublishRecipeCommand.scala         🔜 Command publication
│   ├── PublishRecipeCommandHandler.scala  🔜 Handler publication + validation
│   ├── ForkRecipeCommand.scala            🔜 Command fork/clone recette
│   └── ForkRecipeCommandHandler.scala     🔜 Handler fork avec versioning
│
├── queries/
│   ├── RecipeListQuery.scala              🔜 Query liste avec filtres avancés
│   ├── RecipeListQueryHandler.scala       🔜 Handler liste optimisé
│   ├── RecipeDetailQuery.scala            🔜 Query détail complet
│   ├── RecipeDetailQueryHandler.scala     🔜 Handler détail avec calculs
│   ├── RecipeSearchQuery.scala            🔜 Query recherche complexe
│   ├── RecipeSearchQueryHandler.scala     🔜 Handler recherche multi-critères
│   ├── RecipesByStyleQuery.scala          🔜 Query par style bière
│   ├── RecipesByStyleQueryHandler.scala   🔜 Handler par style
│   ├── RecipesByAuthorQuery.scala         🔜 Query par créateur
│   ├── RecipesByAuthorQueryHandler.scala  🔜 Handler par créateur
│   ├── PopularRecipesQuery.scala          🔜 Query recettes populaires
│   ├── PopularRecipesQueryHandler.scala   🔜 Handler popularité + metrics
│   ├── SimilarRecipesQuery.scala          🔜 Query recettes similaires
│   ├── SimilarRecipesQueryHandler.scala   🔜 Handler similarité IA
│   ├── RecipeCalculationsQuery.scala      🔜 Query calculs seuls
│   └── RecipeCalculationsQueryHandler.scala🔜 Handler calculs optimisé
│
├── calculations/
│   ├── GravityCalculator.scala            🔜 Calculateur densités
│   ├── IBUCalculator.scala                🔜 Calculateur amertume
│   ├── SRMCalculator.scala                🔜 Calculateur couleur
│   ├── ABVCalculator.scala                🔜 Calculateur alcool
│   ├── BrewingMathService.scala           🔜 Service mathématiques brassage
│   ├── ScalingCalculator.scala            🔜 Calculateur scaling volumes
│   ├── EfficiencyCalculator.scala         🔜 Calculateur efficacité
│   └── ValidationCalculator.scala         🔜 Validation résultats calculs
│
├── ai/
│   ├── RecipeAnalyzer.scala               🔜 Analyseur recettes IA
│   ├── StyleComplianceAnalyzer.scala      🔜 Analyseur conformité BJCP
│   ├── BalanceAnalyzer.scala              🔜 Analyseur équilibre
│   ├── RecipeOptimizer.scala              🔜 Optimiseur recettes IA
│   ├── IngredientSuggester.scala          🔜 Suggestions ingrédients
│   ├── ProcedureOptimizer.scala           🔜 Optimiseur procédures
│   └── FlavorProfilePredictor.scala       🔜 Prédicteur profil gustatif
│
└── dto/
    ├── RecipeDto.scala                    🔜 DTO transfert recette complète
    ├── RecipeListDto.scala                🔜 DTO liste avec pagination
    ├── RecipeSearchDto.scala              🔜 DTO résultats recherche
    ├── RecipeCalculationsDto.scala        🔜 DTO calculs seuls
    ├── ScaledRecipeDto.scala              🔜 DTO recette scalée
    ├── RecipeAnalysisDto.scala            🔜 DTO analyse IA
    └── RecipeOptimizationDto.scala        🔜 DTO suggestions optimisation
```

### Infrastructure Layer (Couche Infrastructure)

```
app/infrastructure/
├── persistence/recipes/
│   ├── RecipeTables.scala                 🔜 Tables principales
│   ├── RecipeIngredientTables.scala       🔜 Tables ingrédients
│   ├── RecipeProcedureTables.scala        🔜 Tables procédures
│   ├── RecipeCalculationTables.scala      🔜 Tables cache calculs
│   ├── RecipeVersionTables.scala          🔜 Tables versioning
│   ├── SlickRecipeReadRepository.scala    🔜 Repository lecture complexe
│   ├── SlickRecipeWriteRepository.scala   🔜 Repository écriture
│   ├── SlickRecipeCalculationRepository.scala🔜 Repository calculs
│   └── RecipeRowMapper.scala              🔜 Mapping complexe Row ↔ Domain
│
├── calculations/
│   ├── BrewingFormulas.scala              🔜 Formules brassage standards
│   ├── TinsethIBUCalculator.scala         🔜 Implémentation Tinseth
│   ├── RagerIBUCalculator.scala           🔜 Implémentation Rager
│   ├── MoreySRMCalculator.scala           🔜 Implémentation Morey
│   ├── PreciseABVCalculator.scala         🔜 Calculs ABV précis
│   └── ScalingAlgorithms.scala            🔜 Algorithmes scaling
│
├── ai/
│   ├── MLRecipeAnalyzer.scala             🔜 Analyseur ML
│   ├── OpenAIRecipeAssistant.scala        🔜 Assistant OpenAI
│   ├── StyleComplianceML.scala            🔜 ML conformité styles
│   ├── FlavorPredictionML.scala           🔜 ML prédiction saveurs
│   └── RecipeOptimizationML.scala         🔜 ML optimisation
│
├── external/
│   ├── BJCPApiClient.scala                🔜 Client API BJCP
│   ├── IngredientPricingService.scala     🔜 Service prix ingrédients
│   ├── BrewingCommunityApi.scala          🔜 APIs communautés brassage
│   └── RecipeDataImporter.scala           🔜 Import recettes externes
│
├── serialization/
│   ├── RecipeJsonFormats.scala            🔜 Formats JSON complexes
│   ├── BeerXMLConverter.scala             🔜 Conversion BeerXML
│   ├── RecipeEventSerialization.scala     🔜 Sérialisation events
│   └── CalculationFormats.scala           🔜 Formats calculs
│
└── configuration/
    ├── RecipeModule.scala                 🔜 Injection dépendances
    ├── CalculationConfig.scala            🔜 Configuration calculs
    ├── AIModelConfig.scala                🔜 Configuration modèles IA
    └── RecipeDatabaseConfig.scala         🔜 Configuration BDD
```

---

## 🗃️ Infrastructure Base de Données Avancée

### Schéma PostgreSQL Principal

```sql
-- Table principale recettes
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    author_id UUID, -- Référence utilisateur créateur
    beer_style_id UUID REFERENCES beer_styles(id),
    
    -- Métadonnées recette
    recipe_type VARCHAR(20) NOT NULL DEFAULT 'ALL_GRAIN' 
        CHECK (recipe_type IN ('ALL_GRAIN', 'EXTRACT', 'PARTIAL_MASH')),
    complexity_level VARCHAR(20) NOT NULL DEFAULT 'INTERMEDIATE'
        CHECK (complexity_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    batch_size_liters DECIMAL(6,2) NOT NULL CHECK (batch_size_liters > 0),
    
    -- Caractéristiques calculées
    original_gravity DECIMAL(5,3) NOT NULL CHECK (original_gravity >= 1.000 AND original_gravity <= 1.200),
    final_gravity DECIMAL(5,3) NOT NULL CHECK (final_gravity >= 0.990 AND final_gravity <= 1.050),
    abv DECIMAL(4,2) NOT NULL CHECK (abv >= 0 AND abv <= 25),
    ibu DECIMAL(5,1) NOT NULL CHECK (ibu >= 0 AND ibu <= 150),
    srm DECIMAL(4,1) NOT NULL CHECK (srm >= 0 AND srm <= 50),
    
    -- Efficacité et profil équipement
    mash_efficiency DECIMAL(4,2) NOT NULL DEFAULT 75.0 CHECK (mash_efficiency >= 50 AND mash_efficiency <= 95),
    equipment_profile_id UUID REFERENCES equipment_profiles(id),
    boil_time_minutes INTEGER NOT NULL DEFAULT 60 CHECK (boil_time_minutes >= 15 AND boil_time_minutes <= 180),
    
    -- Métadonnées
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'PRIVATE', 'ARCHIVED')),
    version INTEGER NOT NULL DEFAULT 1,
    parent_recipe_id UUID REFERENCES recipes(id), -- Pour forks/variations
    
    -- Métriques
    view_count INTEGER NOT NULL DEFAULT 0,
    brew_count INTEGER NOT NULL DEFAULT 0, -- Nombre fois brassée
    rating_average DECIMAL(3,2) CHECK (rating_average >= 1 AND rating_average <= 5),
    rating_count INTEGER NOT NULL DEFAULT 0,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table ingrédients houblons dans recettes
CREATE TABLE recipe_hops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    hop_id UUID NOT NULL REFERENCES hops(id),
    
    -- Quantité et unités
    amount DECIMAL(8,3) NOT NULL CHECK (amount > 0),
    unit VARCHAR(10) NOT NULL DEFAULT 'g' CHECK (unit IN ('g', 'kg', 'oz', 'lb')),
    
    -- Timing et usage
    time_minutes INTEGER NOT NULL CHECK (time_minutes >= 0 AND time_minutes <= 180),
    usage_type VARCHAR(20) NOT NULL CHECK (usage_type IN ('BOIL', 'AROMA', 'DRY_HOP', 'FIRST_WORT', 'WHIRLPOOL')),
    
    -- Forme houblon
    form VARCHAR(10) NOT NULL DEFAULT 'PELLET' CHECK (form IN ('PELLET', 'WHOLE', 'PLUG', 'EXTRACT')),
    
    -- Calculs
    alpha_acid_utilized DECIMAL(4,2), -- AA utilisé pour calcul IBU
    contributed_ibu DECIMAL(5,1), -- IBU contributés par cet ajout
    
    -- Ordre dans la recette
    display_order INTEGER NOT NULL DEFAULT 1,
    notes TEXT
);

-- Table ingrédients malts dans recettes  
CREATE TABLE recipe_malts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    malt_id UUID NOT NULL REFERENCES malts(id),
    
    -- Quantité et pourcentages
    amount DECIMAL(8,3) NOT NULL CHECK (amount > 0),
    unit VARCHAR(10) NOT NULL DEFAULT 'kg' CHECK (unit IN ('g', 'kg', 'oz', 'lb')),
    percentage DECIMAL(5,2) NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    
    -- Contribution aux calculs
    contributed_gravity_points DECIMAL(6,2), -- Points gravité contributés
    contributed_color DECIMAL(6,2), -- Contribution couleur
    
    display_order INTEGER NOT NULL DEFAULT 1,
    notes TEXT
);

-- Table ingrédients levures dans recettes
CREATE TABLE recipe_yeasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    yeast_id UUID NOT NULL REFERENCES yeasts(id),
    
    -- Quantité selon forme
    amount DECIMAL(8,3) NOT NULL CHECK (amount > 0),
    unit VARCHAR(10) NOT NULL DEFAULT 'g' CHECK (unit IN ('g', 'ml', 'packet', 'vial')),
    
    -- Gestion starter
    starter_required BOOLEAN NOT NULL DEFAULT false,
    starter_volume_ml INTEGER,
    starter_gravity DECIMAL(5,3),
    
    -- Fermentation
    fermentation_stage VARCHAR(10) NOT NULL DEFAULT 'PRIMARY' 
        CHECK (fermentation_stage IN ('PRIMARY', 'SECONDARY', 'TERTIARY')),
    temperature_celsius DECIMAL(4,1),
    
    display_order INTEGER NOT NULL DEFAULT 1,
    notes TEXT
);

-- Table autres ingrédients (épices, fruits, etc.)
CREATE TABLE recipe_other_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    
    name VARCHAR(100) NOT NULL,
    ingredient_type VARCHAR(20) NOT NULL 
        CHECK (ingredient_type IN ('SPICE', 'FRUIT', 'CLARIFIER', 'SALT', 'ACID', 'OTHER')),
    
    amount DECIMAL(8,3) NOT NULL CHECK (amount > 0),
    unit VARCHAR(10) NOT NULL,
    
    -- Timing ajout
    usage_stage VARCHAR(20) NOT NULL
        CHECK (usage_stage IN ('MASH', 'BOIL', 'PRIMARY', 'SECONDARY', 'BOTTLING')),
    time_minutes INTEGER, -- Temps dans l'étape
    
    display_order INTEGER NOT NULL DEFAULT 1,
    notes TEXT
);

-- Table procédures brassage
CREATE TABLE recipe_procedures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    
    step_type VARCHAR(20) NOT NULL
        CHECK (step_type IN ('MASH_IN', 'MASH_STEP', 'MASH_OUT', 'SPARGE', 'BOIL_START', 'HOP_ADDITION', 'BOIL_END', 'COOL', 'TRANSFER', 'FERMENT', 'DRY_HOP', 'COLD_CRASH', 'PACKAGE')),
    
    step_order INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    
    -- Paramètres température
    temperature_celsius DECIMAL(4,1),
    temperature_tolerance DECIMAL(3,1) DEFAULT 1.0,
    
    -- Paramètres temps
    duration_minutes INTEGER,
    is_duration_critical BOOLEAN NOT NULL DEFAULT false,
    
    -- Paramètres spécialisés
    target_volume_liters DECIMAL(6,2),
    target_gravity DECIMAL(5,3),
    
    -- Instructions équipement
    equipment_notes TEXT,
    safety_warnings TEXT
);
```

### Tables Support et Relations

```sql
-- Profils équipement pour calculs précis
CREATE TABLE equipment_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(20) NOT NULL
        CHECK (equipment_type IN ('MINIBREW', 'TRADITIONAL', 'HERMS', 'RIMS', 'BIAB', 'PROFESSIONAL')),
    
    -- Efficacités spécifiques
    mash_efficiency DECIMAL(4,2) NOT NULL DEFAULT 75.0,
    brewhouse_efficiency DECIMAL(4,2) NOT NULL DEFAULT 70.0,
    
    -- Paramètres physiques
    boil_off_rate_per_hour DECIMAL(4,2) NOT NULL DEFAULT 10.0, -- % évaporation/heure
    dead_space_liters DECIMAL(6,2) NOT NULL DEFAULT 0.5,
    
    -- Limites opérationnelles
    max_batch_size_liters DECIMAL(6,2) NOT NULL,
    min_batch_size_liters DECIMAL(6,2) NOT NULL DEFAULT 1.0,
    max_boil_time_minutes INTEGER NOT NULL DEFAULT 120,
    
    -- Capacités spéciales
    has_recirculation BOOLEAN NOT NULL DEFAULT false,
    has_chiller BOOLEAN NOT NULL DEFAULT false,
    has_hop_back BOOLEAN NOT NULL DEFAULT false,
    temperature_control_precision DECIMAL(3,1) NOT NULL DEFAULT 2.0,
    
    -- Métadonnées
    user_id UUID, -- NULL = profil système, sinon profil utilisateur
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cache calculs complexes pour performance
CREATE TABLE recipe_calculations_cache (
    recipe_id UUID PRIMARY KEY REFERENCES recipes(id) ON DELETE CASCADE,
    
    -- Hash des paramètres pour invalidation cache
    parameters_hash VARCHAR(64) NOT NULL,
    
    -- Calculs stockés
    gravity_calculations JSONB NOT NULL,
    ibu_calculations JSONB NOT NULL,
    color_calculations JSONB NOT NULL,
    alcohol_calculations JSONB NOT NULL,
    
    -- Métriques performance calcul
    calculation_time_ms INTEGER NOT NULL,
    calculation_complexity_score INTEGER NOT NULL,
    
    -- TTL et invalidation
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 hour')
);

-- Historique scaling pour apprentissage
CREATE TABLE recipe_scaling_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_recipe_id UUID NOT NULL REFERENCES recipes(id),
    scaled_recipe_id UUID REFERENCES recipes(id), -- Si sauvegardée
    
    -- Paramètres scaling
    original_batch_size DECIMAL(6,2) NOT NULL,
    target_batch_size DECIMAL(6,2) NOT NULL,
    scaling_method VARCHAR(20) NOT NULL DEFAULT 'LINEAR'
        CHECK (scaling_method IN ('LINEAR', 'OPTIMIZED', 'CUSTOM')),
    
    -- Ajustements appliqués
    hop_adjustments JSONB, -- Ajustements utilisation houblons
    efficiency_adjustments JSONB, -- Ajustements efficacité
    procedure_adjustments JSONB, -- Modifications procédures
    
    -- Métadonnées
    user_id UUID,
    equipment_profile_id UUID REFERENCES equipment_profiles(id),
    scaling_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Index de Performance Spécialisés

```sql
-- Index principaux recettes
CREATE INDEX idx_recipes_status ON recipes(status);
CREATE INDEX idx_recipes_style ON recipes(beer_style_id) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_author ON recipes(author_id);
CREATE INDEX idx_recipes_complexity ON recipes(complexity_level, batch_size_liters);

-- Index pour recherche par caractéristiques
CREATE INDEX idx_recipes_characteristics ON recipes(abv, ibu, srm) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_gravity_range ON recipes(original_gravity, final_gravity) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_batch_size ON recipes(batch_size_liters);

-- Index pour recommandations et popularité
CREATE INDEX idx_recipes_popular ON recipes(rating_average DESC, rating_count DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_recent ON recipes(created_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_trending ON recipes(brew_count DESC, view_count DESC) WHERE status = 'PUBLISHED';

-- Index ingrédients pour recherche
CREATE INDEX idx_recipe_hops_recipe ON recipe_hops(recipe_id);
CREATE INDEX idx_recipe_hops_ingredient ON recipe_hops(hop_id);
CREATE INDEX idx_recipe_hops_usage ON recipe_hops(usage_type, time_minutes);

CREATE INDEX idx_recipe_malts_recipe ON recipe_malts(recipe_id);
CREATE INDEX idx_recipe_malts_ingredient ON recipe_malts(malt_id);
CREATE INDEX idx_recipe_malts_percentage ON recipe_malts(percentage DESC);

-- Index cache pour performance
CREATE INDEX idx_recipe_cache_hash ON recipe_calculations_cache(parameters_hash);
CREATE INDEX idx_recipe_cache_expires ON recipe_calculations_cache(expires_at);
```

---

## ⚙️ Event Sourcing et Calculs

### Événements du Domaine Recettes

```scala
sealed trait RecipeEvent extends DomainEvent {
  def aggregateId: RecipeId
  def version: Long
  def timestamp: Instant
}

case class RecipeCreated(
  aggregateId: RecipeId,
  recipeData: RecipeCreationData,
  calculatedCharacteristics: RecipeCharacteristics,
  version: Long = 1,
  timestamp: Instant = Instant.now()
) extends RecipeEvent

case class RecipeIngredientAdded(
  aggregateId: RecipeId,
  ingredient: RecipeIngredient,
  recalculatedCharacteristics: RecipeCharacteristics,
  impactAnalysis: IngredientImpactAnalysis,
  version: Long,
  timestamp: Instant = Instant.now()
) extends RecipeEvent

case class RecipeScaled(
  aggregateId: RecipeId,
  originalBatchSize: BatchSize,
  newBatchSize: BatchSize,
  scalingMethod: ScalingMethod,
  adjustments: ScalingAdjustments,
  newCharacteristics: RecipeCharacteristics,
  version: Long,
  timestamp: Instant = Instant.now()
) extends RecipeEvent

case class RecipeOptimized(
  aggregateId: RecipeId,
  optimizationType: OptimizationType, // BALANCE, STYLE_COMPLIANCE, COST, EFFICIENCY
  originalCharacteristics: RecipeCharacteristics,
  optimizedCharacteristics: RecipeCharacteristics,
  changes: Seq[RecipeChange],
  confidenceLevel: BigDecimal,
  version: Long,
  timestamp: Instant = Instant.now()
) extends RecipeEvent

case class RecipeBrewingSessionCompleted(
  aggregateId: RecipeId,
  sessionData: BrewingSessionData,
  actualResults: ActualBrewingResults,
  deviations: Seq[ProcessDeviation],
  version: Long,
  timestamp: Instant = Instant.now()
) extends RecipeEvent
```

### Service de Calculs Avancés

```scala
@Singleton
class RecipeCalculationService @Inject()(
  gravityCalculator: GravityCalculator,
  ibuCalculator: IBUCalculator,
  srmCalculator: SRMCalculator,
  abvCalculator: ABVCalculator,
  efficiencyService: EfficiencyService,
  cacheRepository: RecipeCalculationCacheRepository
) {

  def calculateCharacteristics(
    recipe: RecipeAggregate,
    equipmentProfile: EquipmentProfile
  ): Future[Either[CalculationError, RecipeCharacteristics]] = {
    
    val cacheKey = generateCacheKey(recipe, equipmentProfile)
    
    cacheRepository.get(cacheKey).flatMap {
      case Some(cached) if !cached.isExpired => 
        Future.successful(Right(cached.characteristics))
        
      case _ =>
        performCalculations(recipe, equipmentProfile).flatMap { result =>
          result.fold(
            error => Future.successful(Left(error)),
            characteristics => {
              cacheRepository.store(cacheKey, characteristics).map(_ => Right(characteristics))
            }
          )
        }
    }
  }
  
  private def performCalculations(
    recipe: RecipeAggregate,
    equipmentProfile: EquipmentProfile
  ): Future[Either[CalculationError, RecipeCharacteristics]] = {
    
    for {
      // Calcul densité initiale avec efficacité équipement
      ogResult <- gravityCalculator.calculateOG(
        malts = recipe.maltIngredients,
        batchSize = recipe.batchSize,
        efficiency = equipmentProfile.mashEfficiency
      )
      
      // Calcul amertume avec facteurs utilisation
      ibuResult <- ibuCalculator.calculateTotalIBU(
        hops = recipe.hopIngredients,
        boilGravity = ogResult.averageBoilGravity,
        boilTime = recipe.boilTime,
        batchSize = recipe.batchSize
      )
      
      // Calcul couleur
      srmResult <- srmCalculator.calculateSRM(
        malts = recipe.maltIngredients,
        batchSize = recipe.batchSize
      )
      
      // Prédiction densité finale
      fgResult <- predictFinalGravity(
        originalGravity = ogResult.og,
        yeasts = recipe.yeastIngredients,
        fermentationProfile = recipe.fermentationProfile
      )
      
      // Calcul alcool
      abvResult <- abvCalculator.calculateABV(
        originalGravity = ogResult.og,
        finalGravity = fgResult.predictedFG
      )
      
    } yield RecipeCharacteristics(
      originalGravity = ogResult.og,
      finalGravity = fgResult.predictedFG,
      abv = abvResult.abv,
      ibu = ibuResult.totalIBU,
      srm = srmResult.srm,
      calculationDetails = CalculationDetails(
        gravityBreakdown = ogResult.breakdown,
        ibuBreakdown = ibuResult.breakdown,
        colorBreakdown = srmResult.breakdown,
        efficiencyUsed = equipmentProfile.mashEfficiency,
        assumptions = generateAssumptions(recipe, equipmentProfile)
      )
    )
  }
  
  private def predictFinalGravity(
    originalGravity: BigDecimal,
    yeasts: Seq[YeastIngredient],
    fermentationProfile: FermentationProfile
  ): Future[Either[CalculationError, FGPrediction]] = {
    
    // Atténuation composite pour mélanges de levures
    val compositePlannedAttenuation = yeasts.map { yeastIngr =>
      val yeastWeight = yeastIngr.amount.toDouble / yeasts.map(_.amount.toDouble).sum
      yeastIngr.yeast.attenuationRange.average.toDouble * yeastWeight
    }.sum
    
    // Ajustements selon conditions fermentation
    val temperatureAdjustment = fermentationProfile.averageTemperature match {
      case temp if temp < yeasts.head.yeast.temperatureRange.optimal.min => -2.0 // Sous-atténuation
      case temp if temp > yeasts.head.yeast.temperatureRange.optimal.max => +1.0 // Sur-atténuation
      case _ => 0.0
    }
    
    val adjustedAttenuation = (compositePlannedAttenuation + temperatureAdjustment).max(65).min(90)
    val predictedFG = originalGravity - (originalGravity - 1.000) * (adjustedAttenuation / 100)
    
    Future.successful(Right(FGPrediction(
      predictedFG = predictedFG,
      attenuationUsed = adjustedAttenuation,
      confidence = calculateAttenuationConfidence(yeasts, fermentationProfile),
      factors = AttenuationFactors(
        yeastContribution = compositePlannedAttenuation,
        temperatureAdjustment = temperatureAdjustment,
        gravityPenalty = if (originalGravity > 1.070) -1.0 else 0.0
      )
    )))
  }
}
```

### Service de Scaling Intelligent

```scala
@Singleton
class RecipeScalingService @Inject()(
  calculationService: RecipeCalculationService,
  scalingRepository: RecipeScalingHistoryRepository
) {

  def scaleRecipe(
    originalRecipe: RecipeAggregate,
    targetBatchSize: BatchSize,
    targetEquipment: EquipmentProfile,
    scalingPreferences: ScalingPreferences = ScalingPreferences.default
  ): Future[Either[ScalingError, ScaledRecipe]] = {
    
    val scalingFactor = targetBatchSize.liters / originalRecipe.batchSize.liters
    
    for {
      // Scaling linéaire de base
      baseScaledIngredients <- scaleIngredientsLinear(originalRecipe.ingredients, scalingFactor)
      
      // Ajustements intelligents selon équipement
      adjustedIngredients <- applyEquipmentAdjustments(
        baseScaledIngredients, 
        originalRecipe.equipmentProfile, 
        targetEquipment
      )
      
      // Ajustements houblons pour utilisation différentielle
      optimizedHops <- optimizeHopUtilization(
        adjustedIngredients.hops,
        originalRecipe.batchSize,
        targetBatchSize,
        targetEquipment
      )
      
      // Ajustements procédures
      scaledProcedures <- scaleProcedures(
        originalRecipe.procedures,
        scalingFactor,
        targetEquipment
      )
      
      // Recalcul caractéristiques
      newCharacteristics <- calculationService.calculateCharacteristics(
        RecipeAggregate.withNewIngredients(originalRecipe, adjustedIngredients),
        targetEquipment
      )
      
      // Enregistrement historique scaling
      _ <- scalingRepository.record(RecipeScalingRecord(
        originalRecipeId = originalRecipe.id,
        scalingFactor = scalingFactor,
        adjustments = ScalingAdjustments.fromChanges(baseScaledIngredients, adjustedIngredients),
        targetEquipment = targetEquipment,
        results = newCharacteristics.toOption
      ))
      
    } yield ScaledRecipe(
      originalRecipeId = originalRecipe.id,
      scaledIngredients = adjustedIngredients,
      scaledProcedures = scaledProcedures,
      scaledCharacteristics = newCharacteristics.getOrElse(originalRecipe.characteristics),
      scalingNotes = generateScalingNotes(originalRecipe, scalingFactor, targetEquipment),
      confidenceLevel = calculateScalingConfidence(scalingFactor, targetEquipment)
    )
  }
  
  private def optimizeHopUtilization(
    hops: Seq[HopIngredient],
    originalBatchSize: BatchSize,
    targetBatchSize: BatchSize,
    targetEquipment: EquipmentProfile
  ): Future[Seq[HopIngredient]] = {
    
    val scalingFactor = targetBatchSize.liters / originalBatchSize.liters
    
    Future.successful(hops.map { hopIngredient =>
      val baseScaledAmount = hopIngredient.amount * scalingFactor
      
      // Ajustement utilisation selon volume (loi de puissance)
      val utilizationAdjustment = if (scalingFactor > 1.5) {
        // Volumes plus grands = utilisation légèrement réduite
        scala.math.pow(scalingFactor, 0.9)
      } else if (scalingFactor < 0.7) {
        // Volumes plus petits = utilisation légèrement augmentée  
        scala.math.pow(scalingFactor, 1.1)
      } else {
        scalingFactor // Scaling linéaire pour ratios modérés
      }
      
      hopIngredient.copy(
        amount = hopIngredient.amount * utilizationAdjustment,
        notes = hopIngredient.notes + s" [Scaled with utilization adjustment: ${utilizationAdjustment.formatted("%.3f")}]"
      )
    })
  }
}