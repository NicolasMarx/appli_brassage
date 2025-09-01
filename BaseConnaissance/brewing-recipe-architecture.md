│   │   │               ├── ErrorResponse.scala
│   │   │               ├── PageResponse.scala
│   │   │               ├── SuccessResponse.scala
│   │   │               └── ValidationResponse.scala
│   │   │
│   │   └── actions/                      # Actions Play Framework
│   │       ├── AdminSecuredAction.scala
│   │       ├── UserSecuredAction.scala               # NOUVEAU: Auth utilisateur
│   │       ├── CsrfAction.scala
│   │       ├── LoggingAction.scala
│   │       ├── RateLimitingAction.scala
│   │       └── RecipeOwnershipAction.scala           # NOUVEAU: Vérif propriété recette
│   │
│   ├── modules/                          # 🔌 INJECTION DE DÉPENDANCES
│   │   ├── CoreModule.scala              # Bindings principaux
│   │   ├── PersistenceModule.scala       # Bindings persistance
│   │   ├── ApplicationModule.scala       # Bindings couche application
│   │   ├── DomainModule.scala            # Bindings services domaine
│   │   ├── RecipeModule.scala            # NOUVEAU: Bindings recettes
│   │   ├── AiModule.scala                # NOUVEAU: Bindings IA
│   │   ├── BrewingModule.scala           # NOUVEAU: Bindings services brassage
│   │   └── TestModule.scala              # Bindings pour tests
│   │
│   └── utils/                            # 🛠️ UTILITAIRES
│       ├── functional/
│       │   ├── Syntax.scala              # Extensions fonctionnelles
│       │   ├── Validation.scala          # Combinateurs de validation
│       │   ├── FutureOps.scala           # Opérations sur Future
│       │   └── EitherOps.scala           # Opérations sur Either
│       │
│       ├── csv/
│       │   ├── CsvParser.scala           # Parser générique
│       │   ├── CsvValidator.scala        # Validation CSV
│       │   ├── HopCsvMapper.scala        # Mapping spécifique houblons
│       │   ├── MaltCsvMapper.scala       # Mapping spécifique malts
│       │   ├── YeastCsvMapper.scala      # Mapping spécifique levures
│       │   └── RecipeCsvMapper.scala     # NOUVEAU: Mapping recettes
│       │
│       ├── brewing/                      # 🍺 NOUVEAU: Calculs brassage
│       │   ├── calculations/             # Moteurs de calcul
│       │   │   ├── GravityCalculator.scala         # Calculs densité OG/FG
│       │   │   ├── ColorCalculator.scala           # Calculs couleur SRM/EBC
│       │   │   ├── BitternessCalculator.scala      # Calculs IBU (Tinseth, Rager...)
│       │   │   ├── AlcoholCalculator.scala         # Calculs ABV
│       │   │   ├── AttenuationCalculator.scala     # Calculs atténuation
│       │   │   ├── EfficiencyCalculator.scala      # Efficacité brasserie
│       │   │   ├── BoilOffCalculator.scala         # Évaporation ébullition
│       │   │   ├── YeastPitchingCalculator.scala   # Calculs ensemencement
│       │   │   └── CostCalculator.scala            # Calculs de coût
│       │   │
│       │   ├── scaling/                  # Scaling recettes
│       │   │   ├── VolumeScaler.scala              # Scaling par volume
│       │   │   ├── IngredientScaler.scala          # Scaling ingrédients
│       │   │   ├── ProcedureScaler.scala           # Scaling procédures
│       │   │   ├── TimingScaler.scala              # Scaling timings
│       │   │   └── EquipmentScaler.scala           # Scaling équipement
│       │   │
│       │   ├── conversions/              # Conversions d'unités
│       │   │   ├── VolumeConverter.scala           # L/gal/qt/pt...
│       │   │   ├── WeightConverter.scala           # kg/g/lb/oz...
│       │   │   ├── TemperatureConverter.scala      # C°/F°/K
│       │   │   ├── ColorConverter.scala            # SRM/EBC/Lovibond
│       │   │   ├── GravityConverter.scala          # SG/Plato/Brix
│       │   │   └── UnitConverter.scala             # Conversions génériques
│       │   │
│       │   ├── formulas/                 # Formules de brassage
│       │   │   ├── TinsethFormula.scala            # IBU Tinseth
│       │   │   ├── RagerFormula.scala              # IBU Rager
│       │   │   ├── MorysFormula.scala              # Couleur Morey
│       │   │   ├── DanielFormula.scala             # Couleur Daniels
│       │   │   ├── MostlyFormula.scala             # SRM → EBC
│       │   │   └── BrewingFormulas.scala           # Formules génériques
│       │   │
│       │   └── profiles/                 # Profils de brassage
│       │       ├── MashProfile.scala               # Profils d'empâtage
│       │       ├── BoilProfile.scala               # Profils d'ébullition
│       │       ├── FermentationProfile.scala      # Profils fermentation
│       │       ├── WaterProfile.scala              # Profils d'eau
│       │       └── HopProfile.scala                # Profils houblonnage
│       │
│       ├── ai/                           # 🤖 NOUVEAU: Utilitaires IA
│       │   ├── prompts/                  # Templates de prompts
│       │   │   ├── RecipeAnalysisPrompts.scala     # Prompts analyse recettes
│       │   │   ├── IngredientSuggestionPrompts.scala # Prompts suggestions
│       │   │   ├── StyleMatchingPrompts.scala      # Prompts matching styles
│       │   │   ├── BalanceOptimizationPrompts.scala # Prompts optimisation
│       │   │   └── GeneralBrewingPrompts.scala     # Prompts généraux
│       │   │
│       │   ├── processors/               # Processeurs réponses IA
│       │   │   ├── SuggestionProcessor.scala       # Traitement suggestions
│       │   │   ├── AnalysisProcessor.scala         # Traitement analyses
│       │   │   ├── RecommendationProcessor.scala   # Traitement recommandations
│       │   │   └── ResponseValidator.scala         # Validation réponses IA
│       │   │
│       │   └── models/                   # Modèles IA locaux
│       │       ├── RecipeClassifier.scala          # Classification recettes
│       │       ├── IngredientPredictor.scala       # Prédiction ingrédients
│       │       └── BalanceScorer.scala             # Score d'équilibre
│       │
│       ├── equipment/                    # ⚙️ NOUVEAU: Utilitaires équipement
│       │   ├── profiles/                 # Profils d'équipement
│       │   │   ├── MiniBrewProfile.scala           # Profil MiniBrew
│       │   │   ├── TraditionalProfile.scala        # Profil traditionnel
│       │   │   ├── AllInOneProfile.scala           # Profil all-in-one
│       │   │   └── ElectricProfile.scala           # Profil électrique
│       │   │
│       │   ├── adapters/                 # Adaptateurs équipement
│       │   │   ├── MiniBrewAdapter.scala           # Adaptation MiniBrew
│       │   │   ├── BiabAdapter.scala               # Adaptation BIAB
│       │   │   ├── ThreeVesselAdapter.scala        # Adaptation 3 cuves
│       │   │   └── GeneralAdapter.scala            # Adaptation générique
│       │   │
│       │   └── calibration/              # Calibration équipement
│       │       ├── EfficiencyCalibrator.scala      # Calibration efficacité
│       │       ├── TemperatureCalibrator.scala     # Calibration température
│       │       ├── VolumeCalibrator.scala          # Calibration volumes
│       │       └── TimingCalibrator.scala          # Calibration timings
│       │
│       ├── json/                         # 🔧 NOUVEAU: Utilitaires JSON
│       │   ├── RecipeJsonFormats.scala             # Formats JSON recettes
│       │   ├── IngredientJsonFormats.scala         # Formats JSON ingrédients
│       │   ├── AiJsonFormats.scala                 # Formats JSON IA
│       │   ├── DashboardJsonFormats.scala          # Formats JSON dashboard
│       │   └── BrewingJsonFormats.scala            # Formats JSON brassage
│       │
│       └── security/
│           ├── Crypto.scala
│           ├── TokenGenerator.scala
│           └── RecipePermissions.scala             # NOUVEAU: Permissions recettes
│
├── test/                                 # 🧪 TESTS
│   ├── domain/                           # Tests du domaine (unit tests)
│   │   ├── hops/
│   │   │   ├── HopAggregateSpec.scala
│   │   │   ├── HopFilterServiceSpec.scala          # NOUVEAU: Test filtrage
│   │   │   └── HopToRecipeServiceSpec.scala        # NOUVEAU: Test ajout recette
│   │   │
│   │   ├── malts/
│   │   │   ├── MaltAggregateSpec.scala
│   │   │   ├── MaltFilterServiceSpec.scala         # NOUVEAU: Test filtrage
│   │   │   └── MaltToRecipeServiceSpec.scala       # NOUVEAU: Test ajout recette
│   │   │
│   │   ├── yeasts/
│   │   │   ├── YeastAggregateSpec.scala
│   │   │   ├── YeastFilterServiceSpec.scala        # NOUVEAU: Test filtrage
│   │   │   └── YeastToRecipeServiceSpec.scala      # NOUVEAU: Test ajout recette
│   │   │
│   │   ├── recipes/                      # 📝 NOUVEAU: Tests recettes
│   │   │   ├── RecipeAggregateSpec.scala           # Tests agrégat recette
│   │   │   ├── RecipeIngredientSpec.scala          # Tests ingrédients
│   │   │   ├── RecipeProcedureSpec.scala           # Tests procédures
│   │   │   ├── RecipeCalculationServiceSpec.scala  # Tests calculs
│   │   │   ├── RecipeScalingServiceSpec.scala      # Tests scaling
│   │   │   ├── RecipeValidationServiceSpec.scala   # Tests validation
│   │   │   └── RecipeVersioningServiceSpec.scala   # Tests versioning
│   │   │
│   │   ├── ai/                           # 🤖 NOUVEAU: Tests IA
│   │   │   ├── RecipeAnalysisServiceSpec.scala     # Tests analyse IA
│   │   │   ├── IngredientSuggestionServiceSpec.scala # Tests suggestions
│   │   │   ├── StyleMatchingServiceSpec.scala      # Tests matching
│   │   │   ├── BalanceOptimizationServiceSpec.scala # Tests optimisation
│   │   │   └── AiPromptSpec.scala                  # Tests prompts
│   │   │
│   │   └── shared/
│   │       ├── VolumeSpec.scala                    # NOUVEAU: Tests volumes
│   │       ├── WeightSpec.scala                    # NOUVEAU: Tests poids
│   │       ├── ColorSpec.scala                     # NOUVEAU: Tests couleur
│   │       ├── BitternessSpec.scala                # NOUVEAU: Tests amertume
│   │       └── AlcoholContentSpec.scala            # NOUVEAU: Tests alcool
│   │
│   ├── application/                      # Tests des use cases
│   │   ├── commands/
│   │   │   ├── recipes/                  # NOUVEAU: Tests commandes recettes
│   │   │   │   ├── CreateRecipeCommandHandlerSpec.scala
│   │   │   │   ├── UpdateRecipeCommandHandlerSpec.scala
│   │   │   │   ├── ScaleRecipeCommandHandlerSpec.scala
│   │   │   │   ├── AddIngredientCommandHandlerSpec.scala
│   │   │   │   └── GenerateProcedureCommandHandlerSpec.scala
│   │   │   │
│   │   │   └── ai/                       # NOUVEAU: Tests commandes IA
│   │   │       ├── RequestAiSuggestionsCommandHandlerSpec.scala
│   │   │       ├── ApplyAiSuggestionCommandHandlerSpec.scala
│   │   │       └── RefineRecipeCommandHandlerSpec.scala
│   │   │
│   │   ├── queries/
│   │   │   ├── recipes/                  # NOUVEAU: Tests queries recettes
│   │   │   │   ├── RecipeListQueryHandlerSpec.scala
│   │   │   │   ├── RecipeDetailQueryHandlerSpec.scala
│   │   │   │   ├── RecipeCalculationsQueryHandlerSpec.scala
│   │   │   │   └── RecipeCharacteristicsQueryHandlerSpec.scala
│   │   │   │
│   │   │   ├── ai/                       # NOUVEAU: Tests queries IA
│   │   │   │   ├── AiSuggestionsQueryHandlerSpec.scala
│   │   │   │   ├── RecipeAnalysisQueryHandlerSpec.scala
│   │   │   │   └── StyleComplianceQueryHandlerSpec.scala
│   │   │   │
│   │   │   └── dashboard/                # NOUVEAU: Tests queries dashboard
│   │   │       ├── RecipeDashboardQueryHandlerSpec.scala
│   │   │       └── CharacteristicsQueryHandlerSpec.scala
│   │   │
│   │   └── services/
│   │       ├── brewing/                  # NOUVEAU: Tests services brassage
│   │       │   ├── RecipeCalculationServiceSpec.scala
│   │       │   ├── ScalingServiceSpec.scala
│   │       │   ├── ProcedureGenerationServiceSpec.scala
│   │       │   └── EquipmentAdaptationServiceSpec.scala
│   │       │
│   │       ├── filtering/                # NOUVEAU: Tests services filtrage
│   │       │   ├── AdvancedFilterServiceSpec.scala
│   │       │   ├── HopFilterServiceSpec.scala
│   │       │   ├── MaltFilterServiceSpec.scala
│   │       │   └── YeastFilterServiceSpec.scala
│   │       │
│   │       └── dashboard/                # NOUVEAU: Tests services dashboard
│   │           ├── DashboardServiceSpec.scala
│   │           ├── CharacteristicsServiceSpec.scala
│   │           └── StatisticsServiceSpec.scala
│   │
│   ├── infrastructure/                   # Tests d'intégration
│   │   ├── persistence/
│   │   │   └── slick/
│   │   │       ├── SlickRecipeRepositorySpec.scala # NOUVEAU
│   │   │       └── SlickAiSuggestionRepositorySpec.scala # NOUVEAU
│   │   │
│   │   ├── external/
│   │   │   ├── ai/                       # NOUVEAU: Tests clients IA
│   │   │   │   ├── OpenAiClientSpec.scala
│   │   │   │   ├── AnthropicClientSpec.scala
│   │   │   │   └── BrewingAiClientSpec.scala
│   │   │   │
│   │   │   └── equipment/                # NOUVEAU: Tests clients équipement
│   │   │       ├── MiniBrewClientSpec.scala
│   │   │       └── GenericEquipmentClientSpec.scala
│   │   │
│   │   └── utils/
│   │       ├── brewing/                  # NOUVEAU: Tests utilitaires brassage
│   │       │   ├── GravityCalculatorSpec.scala
│   │       │   ├── ColorCalculatorSpec.scala
│   │       │   ├── BitternessCalculatorSpec.scala
│   │       │   ├── AlcoholCalculatorSpec.scala
│   │       │   └── VolumeScalerSpec.scala
│   │       │
│   │       └── conversions/              # NOUVEAU: Tests conversions
│   │           ├── VolumeConverterSpec.scala
│   │           ├── WeightConverterSpec.scala
│   │           └── TemperatureConverterSpec.scala
│   │
│   ├── interfaces/                       # Tests des contrôleurs
│   │   └── http/
│   │       └── api/
│   │           └── v1/
│   │               ├── ingredients/      # Tests contrôleurs ingrédients
│   │               │   ├── HopsControllerSpec.scala
│   │               │   ├── MaltsControllerSpec.scala
│   │               │   ├── YeastsControllerSpec.scala
│   │               │   └── FilterControllerSpec.scala
│   │               │
│   │               ├── recipes/          # NOUVEAU: Tests contrôleurs recettes
│   │               │   ├── RecipesControllerSpec.scala
│   │               │   ├── RecipeIngredientsControllerSpec.scala
│   │               │   ├── RecipeProceduresControllerSpec.scala
│   │               │   ├── RecipeScalingControllerSpec.scala
│   │               │   └── RecipeCalculationsControllerSpec.scala
│   │               │
│   │               ├── ai/               # NOUVEAU: Tests contrôleurs IA
│   │               │   ├── AiSuggestionsControllerSpec.scala
│   │               │   ├── RecipeAnalysisControllerSpec.scala
│   │               │   └── StyleMatchingControllerSpec.scala
│   │               │
│   │               ├── dashboard/        # NOUVEAU: Tests contrôleurs dashboard
│   │               │   ├── RecipeDashboardControllerSpec.scala
│   │               │   ├── CharacteristicsControllerSpec.scala
│   │               │   └── ComparisonControllerSpec.scala
│   │               │
│   │               └── equipment/        # NOUVEAU: Tests contrôleurs équipement
│   │                   ├── EquipmentControllerSpec.scala
│   │                   ├── MiniBrewControllerSpec.scala
│   │                   └── ProcedureAdaptationControllerSpec.scala
│   │
│   ├── fixtures/                         # Données de test
│   │   ├── HopFixtures.scala
│   │   ├── MaltFixtures.scala
│   │   ├── YeastFixtures.scala
│   │   ├── RecipeFixtures.scala                  # NOUVEAU: Fixtures recettes
│   │   ├── AiSuggestionFixtures.scala            # NOUVEAU: Fixtures IA
│   │   ├── BeerStyleFixtures.scala
│   │   ├── OriginFixtures.scala
│   │   ├── AromaFixtures.scala
│   │   └── AdminFixtures.scala
│   │
│   ├── integration/                      # Tests d'intégration end-to-end
│   │   ├── HopWorkflowSpec.scala
│   │   ├── MaltWorkflowSpec.scala
│   │   ├── YeastWorkflowSpec.scala
│   │   ├── RecipeCreationWorkflowSpec.scala      # NOUVEAU: Workflow recette complète
│   │   ├── RecipeScalingWorkflowSpec.scala       # NOUVEAU: Workflow scaling
│   │   ├── AiSuggestionWorkflowSpec.scala        # NOUVEAU: Workflow IA
│   │   ├── DashboardWorkflowSpec.scala           # NOUVEAU: Workflow dashboard
│   │   ├── EquipmentAdaptationWorkflowSpec.scala # NOUVEAU: Workflow équipement
│   │   └── ImportWorkflowSpec.scala
│   │
│   └── utils/                            # Utilitaires de test
│       ├── TestDatabase.scala
│       ├── TestData.scala
│       ├── IntegrationSpec.scala
│       ├── DomainSpec.scala
│       ├── ApplicationSpec.scala
│       ├── ControllerSpec.scala
│       ├── RecipeTestUtils.scala                 # NOUVEAU: Utils tests recettes
│       ├── AiTestUtils.scala                     # NOUVEAU: Utils tests IA
│       └── BrewingTestUtils.scala                # NOUVEAU: Utils tests brassage
│
├── conf/                                 # ⚙️ CONFIGURATION
│   ├── application.conf                  # Configuration principale
│   ├── routes                            # Routes étendues
│   ├── logback.xml                       # Configuration logs
│   ├── ai.conf                           # NOUVEAU: Configuration IA
│   └── brewing.conf                      # NOUVEAU: Configuration brassage
│   │
│   ├── evolutions/                       # Évolutions base de données
│   │   └── default/
│   │       ├── 1.sql                     # Schema initial
│   │       ├── 2.sql                     # Indexes et contraintes
│   │       ├── 3.sql                     # Tables référentiels
│   │       ├── 4.sql                     # Tables recettes (NOUVEAU)
│   │       ├── 5.sql                     # Tables IA (NOUVEAU)
│   │       ├── 6.sql                     # Tables dashboard (NOUVEAU)
│   │       ├── 7.sql                     # Relations many-to-many
│   │       └── 8.sql                     # Données de référence
│   │
│   └── reseed/                           # Données CSV pour imports
│       ├── ingredients/                  # Données ingrédients
│       │   ├── hops.csv
│       │   ├── malts.csv
│       │   └── yeasts.csv
│       │
│       ├── referentials/                 # Données référentiels
│       │   ├── beer_styles.csv
│       │   ├── origins.csv
│       │   └── aromas.csv
│       │
│       ├── relations/                    # Relations many-to-many
│       │   ├── hop_beer_styles.csv
│       │   ├── hop_aromas.csv
│       │   ├── malt_beer_styles.csv
│       │   ├── yeast_beer_styles.csv
│       │   └── hop_substitutes.csv
│       │
│       ├── recipes/                      # NOUVEAU: Recettes exemples
│       │   ├── sample_recipes.csv
│       │   ├── popular_recipes.csv
│       │   └── beginner_recipes.csv
│       │
│       ├── equipment/                    # NOUVEAU: Profils équipement
│       │   ├── minibrew_profile.csv
│       │   ├── traditional_profile.csv
│       │   └── equipment_efficiency.csv
│       │
│       └── samples/                      # Données d'exemple
│           ├── sample_hops.csv
│           ├── sample_malts.csv
│           ├── sample_yeasts.csv
│           └── sample_recipes.csv        # NOUVEAU
│
├── public/                               # 🌐 RESSOURCES STATIQUES
│   ├── js/
│   │   ├── app.js                        # Application Elm publique
│   │   ├── recipe-builder.js             # NOUVEAU: Builder recettes
│   │   ├── dashboard.js                  # NOUVEAU: Dashboard interactif
│   │   └── charts.js                     # NOUVEAU: Graphiques
│   │
│   ├── admin/                            # Interface admin
│   │   ├── index.html
│   │   ├── admin.js                      # Application Elm admin
│   │   ├── admin.css                     # Styles admin
│   │   └── boot.js
│   │
│   ├── css/
│   │   ├── main.css                      # Styles principaux
│   │   ├── brewing.css                   # Styles brassage
│   │   ├── recipe-builder.css            # NOUVEAU: Styles builder
│   │   ├── dashboard.css                 # NOUVEAU: Styles dashboard
│   │   ├── filters.css                   # NOUVEAU: Styles filtres
│   │   └── components.css                # Styles composants
│   │
│   ├── images/
│   │   ├── logo.png
│   │   ├── icons/                        # Icônes ingrédients et actions
│   │   │   ├── hop.svg
│   │   │   ├── malt.svg
│   │   │   ├── yeast.svg
│   │   │   ├── recipe.svg                # NOUVEAU
│   │   │   ├── ai-suggestion.svg         # NOUVEAU
│   │   │   ├── dashboard.svg             # NOUVEAU
│   │   │   ├── minibrew.svg              # NOUVEAU
│   │   │   └── scaling.svg               # NOUVEAU
│   │   │
│   │   ├── equipment/                    # NOUVEAU: Images équipement
│   │   │   ├── minibrew.png
│   │   │   ├── traditional-setup.png
│   │   │   └── brewing-process.png
│   │   │
│   │   └── backgrounds/
│   │       ├── brewing-bg.jpg
│   │       ├── recipe-builder-bg.jpg     # NOUVEAU
│   │       └── dashboard-bg.jpg          # NOUVEAU
│   │
│   └── fonts/
│       ├── brewing-icons.woff            # Police d'icônes brassage
│       └── recipe-icons.woff             # NOUVEAU: Police icônes recettes
│
├── elm/                                  # 🌳 APPLICATIONS ELM
│   ├── public/                           # Application publique
│   │   ├── src/
│   │   │   ├── Main.elm
│   │   │   ├── Api/                      # Modules API
│   │   │   │   ├── Hops.elm
│   │   │   │   ├── Malts.elm
│   │   │   │   ├── Yeasts.elm
│   │   │   │   ├── Recipes.elm           # NOUVEAU: API recettes
│   │   │   │   ├── Ai.elm                # NOUVEAU: API IA
│   │   │   │   ├── Dashboard.elm         # NOUVEAU: API dashboard
│   │   │   │   ├── Equipment.elm         # NOUVEAU: API équipement
│   │   │   │   └── Filtering.elm         # NOUVEAU: API filtrage
│   │   │   │
│   │   │   ├── Models/                   # Modèles de données
│   │   │   │   ├── Hop.elm
│   │   │   │   ├── Malt.elm
│   │   │   │   ├── Yeast.elm
│   │   │   │   ├── Recipe.elm            # NOUVEAU: Modèle recette
│   │   │   │   ├── Ingredient.elm        # NOUVEAU: Modèle ingrédient
│   │   │   │   ├── Procedure.elm         # NOUVEAU: Modèle procédure
│   │   │   │   ├── Equipment.elm         # NOUVEAU: Modèle équipement
│   │   │   │   ├── AiSuggestion.elm      # NOUVEAU: Modèle suggestion IA
│   │   │   │   ├── Dashboard.elm         # NOUVEAU: Modèle dashboard
│   │   │   │   ├── BeerStyle.elm
│   │   │   │   ├── Origin.elm
│   │   │   │   └── Aroma.elm
│   │   │   │
│   │   │   ├── Pages/                    # Pages de l'application
│   │   │   │   ├── Home.elm              # Page d'accueil
│   │   │   │   ├── ingredients/          # NOUVEAU: Pages ingrédients
│   │   │   │   │   ├── HopSearch.elm     # Recherche houblons avec filtres
│   │   │   │   │   ├── MaltSearch.elm    # Recherche malts avec filtres
│   │   │   │   │   ├── YeastSearch.elm   # Recherche levures avec filtres
│   │   │   │   │   ├── HopDetail.elm     # Détail houblon + ajout recette
│   │   │   │   │   ├── MaltDetail.elm    # Détail malt + ajout recette
│   │   │   │   │   └── YeastDetail.elm   # Détail levure + ajout recette
│   │   │   │   │
│   │   │   │   ├── recipes/              # NOUVEAU: Pages recettes
│   │   │   │   │   ├── RecipeBuilder.elm # Builder de recette interactif
│   │   │   │   │   ├── RecipeList.elm    # Liste des recettes utilisateur
│   │   │   │   │   ├── RecipeDetail.elm  # Détail recette + modifications
│   │   │   │   │   ├── RecipeScaling.elm # Scaling de recette par volume
│   │   │   │   │   ├── RecipeDashboard.elm # Dashboard recette temps réel
│   │   │   │   │   └── RecipeProcedure.elm # Procédures par équipement
│   │   │   │   │
│   │   │   │   ├── ai/                   # NOUVEAU: Pages IA
│   │   │   │   │   ├── AiSuggestions.elm # Suggestions IA pour recette
│   │   │   │   │   ├── RecipeAnalysis.elm # Analyse recette par IA
│   │   │   │   │   ├── StyleCompliance.elm # Conformité aux styles BJCP
│   │   │   │   │   └── BalanceOptimization.elm # Optimisation équilibre
│   │   │   │   │
│   │   │   │   └── equipment/            # NOUVEAU: Pages équipement
│   │   │   │       ├── EquipmentSetup.elm # Configuration équipement
│   │   │   │       ├── MiniBrewSetup.elm  # Configuration spécifique MiniBrew
│   │   │   │       └── ProcedureGeneration.elm # Génération procédures
│   │   │   │
│   │   │   ├── Components/               # Composants réutilisables
│   │   │   │   ├── common/               # Composants génériques
│   │   │   │   │   ├── SearchBar.elm     # Barre de recherche
│   │   │   │   │   ├── Pagination.elm    # Pagination
│   │   │   │   │   ├── LoadingSpinner.elm # Indicateur de chargement
│   │   │   │   │   ├── ErrorMessage.elm  # Messages d'erreur
│   │   │   │   │   └── SuccessMessage.elm # Messages de succès
│   │   │   │   │
│   │   │   │   ├── filters/              # NOUVEAU: Composants filtrage
│   │   │   │   │   ├── AdvancedFilterPanel.elm # Panneau filtres avancés
│   │   │   │   │   ├── HopFilterPanel.elm      # Filtres spécifiques houblons
│   │   │   │   │   ├── MaltFilterPanel.elm     # Filtres spécifiques malts
│   │   │   │   │   ├── YeastFilterPanel.elm    # Filtres spécifiques levures
│   │   │   │   │   ├── CharacteristicSlider.elm # Slider caractéristiques
│   │   │   │   │   └── FilterChips.elm         # Puces filtres actifs
│   │   │   │   │
│   │   │   │   ├── ingredients/          # NOUVEAU: Composants ingrédients
│   │   │   │   │   ├── IngredientCard.elm      # Carte ingrédient
│   │   │   │   │   ├── IngredientModal.elm     # Modal détail ingrédient
│   │   │   │   │   ├── AddToRecipeButton.elm   # Bouton ajout à recette
│   │   │   │   │   ├── CharacteristicBadge.elm # Badge caractéristique
│   │   │   │   │   └── SubstitutionList.elm    # Liste substitutions
│   │   │   │   │
│   │   │   │   ├── recipes/              # NOUVEAU: Composants recettes
│   │   │   │   │   ├── RecipeCard.elm          # Carte résumé recette
│   │   │   │   │   ├── IngredientList.elm      # Liste ingrédients recette
│   │   │   │   │   ├── ProcedureSteps.elm      # Étapes de procédure
│   │   │   │   │   ├── RecipeCharacteristics.elm # Affichage caractéristiques
│   │   │   │   │   ├── ScalingControls.elm     # Contrôles de scaling
│   │   │   │   │   ├── VolumeSelector.elm      # Sélecteur volume
│   │   │   │   │   ├── EquipmentSelector.elm   # Sélecteur équipement
│   │   │   │   │   └── RecipeTimer.elm         # Minuteur de brassage
│   │   │   │   │
│   │   │   │   ├── dashboard/            # NOUVEAU: Composants dashboard
│   │   │   │   │   ├── CharacteristicsGauge.elm # Jauges caractéristiques
│   │   │   │   │   ├── StyleMatchMeter.elm      # Indicateur conformité style
│   │   │   │   │   ├── BalanceRadar.elm         # Radar d'équilibre
│   │   │   │   │   ├── ColorPreview.elm         # Aperçu couleur bière
│   │   │   │   │   ├── FlavorProfile.elm        # Profil gustatif
│   │   │   │   │   ├── IngredientBreakdown.elm  # Répartition ingrédients
│   │   │   │   │   └── CostEstimate.elm         # Estimation coût
│   │   │   │   │
│   │   │   │   ├── ai/                   # NOUVEAU: Composants IA
│   │   │   │   │   ├── SuggestionCard.elm       # Carte suggestion IA
│   │   │   │   │   ├── AnalysisReport.elm       # Rapport d'analyse
│   │   │   │   │   ├── ImprovementTips.elm      # Conseils d'amélioration
│   │   │   │   │   ├── StyleRecommendations.elm # Recommandations style
│   │   │   │   │   ├── ConfidenceIndicator.elm  # Indicateur confiance IA
│   │   │   │   │   └── AiThinking.elm           # Animation IA en cours
│   │   │   │   │
│   │   │   │   └── charts/               # NOUVEAU: Composants graphiques
│   │   │   │       ├── BarChart.elm            # Graphique barres
│   │   │   │       ├── LineChart.elm           # Graphique courbes
│   │   │   │       ├── PieChart.elm            # Graphique secteurs
│   │   │   │       ├── RadarChart.elm          # Graphique radar
│   │   │   │       └── ProgressBar.elm         # Barre de progression
│   │   │   │
│   │   │   └── Utils/                    # Utilitaires
│   │   │       ├── Route.elm             # Routage
│   │   │       ├── Http.elm              # Utilitaires HTTP
│   │   │       ├── Format.elm            # Formatage données
│   │   │       ├── Validation.elm        # Validation formulaires
│   │   │       ├── LocalStorage.elm      # Stockage local
│   │   │       ├── BrewingCalculations.elm # NOUVEAU: Calculs côté client
│   │   │       ├── UnitConversions.elm   # NOUVEAU: Conversions unités
│   │   │       └── RecipeExport.elm      # NOUVEAU: Export recettes
│   │   │
│   │   └── elm.json                      # Dépendances Elm publique
│   │
│   └── admin/                            # Application admin
│       ├── src/
│       │   ├── Main.elm
│       │   ├── Api/                      # APIs admin
│       │   │   ├── Auth.elm              # Authentification
│       │   │   ├── Hops.elm              # CRUD houblons
│       │   │   ├── Malts.elm             # CRUD malts
│       │   │   ├── Yeasts.elm            # CRUD levures
│       │   │   ├── Recipes.elm           # NOUVEAU: CRUD recettes admin
│       │   │   ├── Users.elm             # NOUVEAU: Gestion utilisateurs
│       │   │   ├── Analytics.elm         # NOUVEAU: Analytics admin
│       │   │   ├── BeerStyles.elm        # CRUD styles
│       │   │   ├── Origins.elm           # CRUD origines
│       │   │   ├── Aromas.elm            # CRUD arômes
│       │   │   └── Import.elm            # Imports CSV
│       │   │
│       │   ├── Pages/                    # Pages admin
│       │   │   ├── Login.elm             # Connexion
│       │   │   ├── Dashboard.elm         # Tableau de bord admin
│       │   │   ├── ingredients/          # Gestion ingrédients
│       │   │   │   ├── HopManagement.elm
│       │   │   │   ├── MaltManagement.elm
│       │   │   │   └── YeastManagement.elm
│       │   │   │
│       │   │   ├── recipes/              # NOUVEAU: Gestion recettes
│       │   │   │   ├── RecipeManagement.elm     # Gestion globale recettes
│       │   │   │   ├── PopularRecipes.elm       # Recettes populaires
│       │   │   │   └── RecipeReporting.elm      # Rapports recettes
│       │   │   │
│       │   │   ├── users/                # NOUVEAU: Gestion utilisateurs
│       │   │   │   ├── UserManagement.elm       # Gestion utilisateurs
│       │   │   │   ├── UserActivity.elm         # Activité utilisateurs
│       │   │   │   └── UserStatistics.elm       # Stats utilisateurs
│       │   │   │
│       │   │   ├── analytics/            # NOUVEAU: Analytics
│       │   │   │   ├── UsageAnalytics.elm       # Analyse usage
│       │   │   │   ├── IngredientTrends.elm     # Tendances ingrédients
│       │   │   │   ├── RecipeTrends.elm         # Tendances recettes
│       │   │   │   └── PerformanceMetrics.elm   # Métriques performance
│       │   │   │
│       │   │   ├── referentials/         # Gestion référentiels
│       │   │   │   ├── ReferentialManagement.elm
│       │   │   │   ├── BeerStyleManagement.elm
│       │   │   │   ├── OriginManagement.elm
│       │   │   │   └── AromaManagement.elm
│       │   │   │
│       │   │   └── imports/              # Gestion imports
│       │   │       ├── ImportData.elm
│       │   │       ├── ImportHistory.elm         # NOUVEAU: Historique imports
│       │   │       └── BatchOperations.elm       # NOUVEAU: Opérations en lot
│       │   │
│       │   ├── Components/               # Composants admin
│       │   │   ├── Navigation.elm        # Navigation admin
│       │   │   ├── DataTable.elm         # Table de données
│       │   │   ├── EditForm.elm          # Formulaire d'édition
│       │   │   ├── ImportDialog.elm      # Dialogue d'import
│       │   │   ├── ConfirmDialog.elm     # Dialogue de confirmation
│       │   │   ├── StatusBadge.elm       # Badge de statut
│       │   │   ├── UserCard.elm          # NOUVEAU: Carte utilisateur
│       │   │   ├── AnalyticsChart.elm    # NOUVEAU: Graphique analytics
│       │   │   ├── ProgressIndicator.elm # NOUVEAU: Indicateur progression
│       │   │   └── ActionButton.elm      # NOUVEAU: Bouton d'action
│       │   │
│       │   └── Utils/                    # Utilitaires admin
│       │       ├── Route.elm
│       │       ├── Http.elm
│       │       ├── Auth.elm
│       │       ├── Validation.elm
│       │       ├── Export.elm            # NOUVEAU: Export données
│       │       ├── Permissions.elm       # NOUVEAU: Gestion permissions
│       │       └── Notifications.elm     # NOUVEAU: Notifications admin
│       │
│       └── elm.json                      # Dépendances Elm admin
│
├── project/                              # 🔧 CONFIGURATION SBT
│   ├── build.properties
│   ├── plugins.sbt
│   ├── Dependencies.scala                # Dépendances centralisées
│   ├── BuildSettings.scala               # Configuration build
│   └── AiDependencies.scala              # NOUVEAU: Dépendances IA
│
├── scripts/                              # 🚀 SCRIPTS UTILITAIRES
│   ├── dev-setup.sh                      # Setup environnement développement
│   ├── compile-elm.sh                    # Compilation applications Elm
│   ├── run-tests.sh                      # Exécution tests complets
│   ├── data-migration.sh                 # Migration données
│   ├── ai-setup.sh                       # NOUVEAU: Setup services IA
│   ├── recipe-migration.sh               # NOUVEAU: Migration recettes
│   ├── backup-recipes.sh                 # NOUVEAU: Sauvegarde recettes
│   └── deploy.sh                         # Déploiement
│
├── docs/                                 # 📚 DOCUMENTATION
│   ├── architecture/
│   │   ├── ddd-design.md                 # Design DDD
│   │   ├── cqrs-implementation.md        # Implémentation CQRS
│   │   ├── bounded-contexts.md           # Contextes délimités
│   │   ├── recipe-domain.md              # NOUVEAU: Domaine recettes
│   │   ├── ai-integration.md             # NOUVEAU: Intégration IA
│   │   └── equipment-adaptation.md       # NOUVEAU: Adaptation équipement
│   │
│   ├── api/
│   │   ├── openapi.yml                   # Spécification OpenAPI complète
│   │   ├── endpoints.md                  # Documentation endpoints
│   │   ├── filtering.md                  # NOUVEAU: Documentation filtrage
│   │   ├── recipes-api.md                # NOUVEAU: API recettes
│   │   ├── ai-api.md                     # NOUVEAU: API IA
│   │   ├── dashboard-api.md              # NOUVEAU: API dashboard
│   │   └── examples.md                   # Exemples d'utilisation
│   │
│   ├── domain/
│   │   ├── business-rules.md             # Règles métier
│   │   ├── hops-domain.md                # Domaine houblons
│   │   ├── malts-domain.md               # Domaine malts
│   │   ├── yeasts-domain.md              # Domaine levures
│   │   ├── recipes-domain.md             # NOUVEAU: Domaine recettes
│   │   ├── ai-domain.md                  # NOUVEAU: Domaine IA
│   │   ├── referentials.md               # Référentiels partagés
│   │   └── calculations.md               # NOUVEAU: Calculs de brassage
│   │
│   ├── user-guide/                       # NOUVEAU: Guide utilisateur
│   │   ├── getting-started.md            # Démarrage
│   │   ├── ingredient-search.md          # Recherche ingrédients
│   │   ├── recipe-creation.md            # Création recettes
│   │   ├── ai-suggestions.md             # Utilisation IA
│   │   ├── equipment-setup.md            # Configuration équipement
│   │   ├── scaling-recipes.md            # Scaling recettes
│   │   └── dashboard-guide.md            # Utilisation dashboard
│   │
│   ├── admin-guide/                      # NOUVEAU: Guide admin
│   │   ├── user-management.md            # Gestion utilisateurs
│   │   ├── content-moderation.md         # Modération contenu
│   │   ├── analytics.md                  # Analytics et reporting
│   │   ├── data-import.md                # Import de données
│   │   └── system-monitoring.md          # Monitoring système
│   │
│   ├── brewing/                          # NOUVEAU: Documentation brassage
│   │   ├── brewing-calculations.md       # Calculs de brassage
│   │   ├── equipment-profiles.md         # Profils d'équipement
│   │   ├── procedure-generation.md       # Génération procédures
│   │   ├── minibrew-integration.md       # Intégration MiniBrew
│   │   ├── scaling-formulas.md           # Formules de scaling
│   │   └── style-guidelines.md           # Guidelines styles BJCP
│   │
│   ├── ai/                               # NOUVEAU: Documentation IA
│   │   ├── ai-models.md                  # Modèles IA utilisés
│   │   ├── prompt-engineering.md         # Engineering des prompts
│   │   ├── suggestion-algorithms.md      # Algorithmes de suggestion
│   │   ├── analysis-methods.md           # Méthodes d'analyse
│   │   └── training-data.md              # Données d'entraînement
│   │
│   └── deployment/
│       ├── docker.md                     # Déploiement Docker
│       ├── database.md                   # Configuration base de données
│       ├── ai-services.md                # NOUVEAU: Déploiement services IA
│       ├── scaling.md                    # NOUVEAU: Montée en charge
│       └── monitoring.md                 # Monitoring et observabilité
│
├── build.sbt                             # Configuration principale SBT
├── docker-compose.yml                    # Services (Postgres, Redis, AI services...)
├── docker-compose.dev.yml                # NOUVEAU: Services développement
├── docker-compose.ai.yml                 # NOUVEAU: Services IA
├── Dockerfile                            # Image de l'application
├── Dockerfile.ai                         # NOUVEAU: Image services IA
├── README.md                             # Documentation principale
├── CHANGELOG.md                          # NOUVEAU: Journal des modifications
├── CONTRIBUTING.md                       # NOUVEAU: Guide de contribution
├── LICENSE                               # Licence du projet
└── .gitignore                            # Fichiers ignorés par Git
```

## 🎯 **Nouvelles fonctionnalités clés implémentées :**

### **🔍 Filtrage avancé des ingrédients :**
- **Filtres par caractéristiques** : Alpha acid, couleur, atténuation, température...
- **Filtres multi-critères** : Combinaison de plusieurs filtres
- **Filtres prédéfinis** : Par style de bière, par usage, par origine
- **Sauvegarde de filtres** : Filtres favoris pour recherches récurrentes

### **📝 Système de recettes complet :**
- **Builder interactif** : Interface drag & drop pour créer recettes
- **Ajout direct** : Bouton "Ajouter à recette" depuis chaque ingrédient
- **Scaling intelligent** : Adaptation automatique 5L → 10L → 20L → 40L
- **Procédures adaptées** : Génération selon l'équipement (MiniBrew, traditionnel)
- **Versioning** : Historique modifications et clonage recettes

### **📊 Dashboard temps réel :**
- **Caractéristiques calculées** : ABV, IBU, SRM, OG/FG automatiques
- **Jauges visuelles** : Indicateurs conformité style BJCP
- **Radar d'équilibre** : Balance malt/houblon/levure
- **Aperçu couleur** : Simulation couleur bière finale
- **Estimation coût** : Calcul prix par batch

### **🤖 IA spécialisée brassage :**
- **Analyse de recette** : Conformité style, équilibre, améliorations
- **Suggestions contextuelles** : Basées sur style choisi et ingrédients
- **Optimisation automatique** : Propositions d'ajustements
- **Substitutions intelligentes** : Alternatives selon disponibilité
- **Apprentissage** : Amélioration basée sur feedback utilisateur

### **⚙️ Intégration équipement :**
- **Profils spécialisés** : MiniBrew, BIAB, 3-vessels, électrique
- **Adaptation procédures** : Timings et températures par équipement
- **Efficacité calibrée** : Calculs selon rendement équipement
- **Export formats** : BeerXML, JSON, PDF procédures

### **🎛️ Interface utilisateur avancée :**
- **Recherche unifiée** : Recherche globale ingrédients + recettes
- **Filtres visuels** : Sliders, puces, sélecteurs graphiques
- **Vue adaptative** : Mobile-first, responsive design
- **Notifications** : Alertes brassage, rappels, suggestions
- **Mode sombre** : Interface adaptée conditions de brassage

Cette architecture permet de créer un **écosystème complet de brassage** avec IA, calculs automatiques, adaptation équipement et expérience utilisateur exceptionnelle !
│   │   │   │   │   │   │   │   │       └── PopularIngredientsQueryHandler.scala
│   │   │   │
│   │   │   └── referentials/             # Queries référentiels
│   │   │       ├── beerstyles/
│   │   │       │   ├── BeerStyleListQuery.scala
│   │   │       │   ├── BeerStyleDetailQuery.scala
│   │   │       │   └── handlers/
│   │   │       │       ├── BeerStyleListQueryHandler.scala
│   │   │       │       └── BeerStyleDetailQueryHandler.scala
│   │   │       │
│   │   │       ├── origins/
│   │   │       │   ├── OriginListQuery.scala
│   │   │       │   └── handlers/
│   │   │       │       └── OriginListQueryHandler.scala
│   │   │       │
│   │   │       └── aromas/
│   │   │           ├── AromaListQuery.scala
│   │   │           └── handlers/
│   │   │               └── AromaListQueryHandler.scala
│   │   │
│   │   ├── services/                     # Services applicatifs
│   │   │   ├── auth/
│   │   │   │   ├── AuthenticationService.scala
│   │   │   │   ├── AuthorizationService.scala
│   │   │   │   └── PasswordService.scala
│   │   │   │
│   │   │   ├── brewing/                  # 🍺 NOUVEAU: Services de brassage
│   │   │   │   ├── RecipeCalculationService.scala    # Calculs automatiques
│   │   │   │   ├── GravityCalculationService.scala   # Calculs densité
│   │   │   │   ├── BitternessCalculationService.scala # Calculs IBU
│   │   │   │   ├── ColorCalculationService.scala     # Calculs couleur
│   │   │   │   ├── AlcoholCalculationService.scala   # Calculs ABV
│   │   │   │   ├── AttenuationService.scala          # Calculs atténuation
│   │   │   │   ├── EfficiencyService.scala           # Efficacité équipement
│   │   │   │   ├── ScalingService.scala              # Scaling recettes
│   │   │   │   ├── ProcedureGenerationService.scala  # Génération procédures
│   │   │   │   └── EquipmentAdaptationService.scala  # Adaptation équipements
│   │   │   │
│   │   │   ├── ai/                       # 🤖 NOUVEAU: Services IA
│   │   │   │   ├── RecipeAnalysisService.scala       # Analyse recettes par IA
│   │   │   │   ├── IngredientSuggestionService.scala # Suggestions ingrédients
│   │   │   │   ├── StyleMatchingService.scala        # Matching styles BJCP
│   │   │   │   ├── BalanceOptimizationService.scala  # Optimisation équilibre
│   │   │   │   ├── SubstitutionService.scala         # Service substitutions
│   │   │   │   ├── RecipeRefinementService.scala     # Raffinement recettes
│   │   │   │   ├── TrendAnalysisService.scala        # Analyse tendances
│   │   │   │   └── PredictiveModelingService.scala   # Modélisation prédictive
│   │   │   │
│   │   │   ├── filtering/                # 🔍 NOUVEAU: Services de filtrage
│   │   │   │   ├── AdvancedFilterService.scala       # Filtrage multi-critères
│   │   │   │   ├── HopFilterService.scala            # Filtrage houblons
│   │   │   │   ├── MaltFilterService.scala           # Filtrage malts
│   │   │   │   ├── YeastFilterService.scala          # Filtrage levures
│   │   │   │   ├── RecipeFilterService.scala         # Filtrage recettes
│   │   │   │   ├── CharacteristicFilterService.scala # Filtrage par caractéristiques
│   │   │   │   └── CombinedFilterService.scala       # Filtres combinés
│   │   │   │
│   │   │   ├── dashboard/                # 📊 NOUVEAU: Services dashboard
│   │   │   │   ├── DashboardService.scala            # Service dashboard principal
│   │   │   │   ├── CharacteristicsService.scala     # Calcul caractéristiques
│   │   │   │   ├── StatisticsService.scala          # Statistiques utilisateur
│   │   │   │   ├── ComparisonService.scala           # Comparaisons recettes
│   │   │   │   ├── TrendService.scala                # Tendances brassage
│   │   │   │   └── ReportingService.scala            # Génération rapports
│   │   │   │
│   │   │   ├── import/
│   │   │   │   ├── CsvImportService.scala
│   │   │   │   ├── HopCsvImportService.scala
│   │   │   │   ├── MaltCsvImportService.scala
│   │   │   │   ├── YeastCsvImportService.scala
│   │   │   │   └── ImportValidationService.scala
│   │   │   │
│   │   │   └── notification/
│   │   │       ├── NotificationService.scala
│   │   │       └── BrewingReminderService.scala      # Rappels de brassage
│   │   │
│   │   └── orchestrators/                # Orchestration use cases complexes
│   │       ├── ReseedOrchestrator.scala
│   │       ├── ImportOrchestrator.scala
│   │       ├── RecipeCreationOrchestrator.scala      # NOUVEAU: Création recette
│   │       ├── RecipeAnalysisOrchestrator.scala      # NOUVEAU: Analyse complète
│   │       ├── IngredientSelectionOrchestrator.scala # NOUVEAU: Sélection ingrédients
│   │       └── BrewingPlanOrchestrator.scala         # NOUVEAU: Planification brassage
│   │
│   ├── infrastructure/                   # 🔧 COUCHE INFRASTRUCTURE
│   │   ├── persistence/
│   │   │   ├── slick/
│   │   │   │   ├── SlickProfile.scala
│   │   │   │   ├── tables/
│   │   │   │   │   ├── HopTables.scala
│   │   │   │   │   ├── MaltTables.scala
│   │   │   │   │   ├── YeastTables.scala
│   │   │   │   │   ├── BeerStyleTables.scala
│   │   │   │   │   ├── OriginTables.scala
│   │   │   │   │   ├── AromaTables.scala
│   │   │   │   │   ├── AdminTables.scala
│   │   │   │   │   ├── RecipeTables.scala            # NOUVEAU: Tables recettes
│   │   │   │   │   ├── IngredientTables.scala        # NOUVEAU: Tables ingrédients
│   │   │   │   │   ├── ProcedureTables.scala         # NOUVEAU: Tables procédures
│   │   │   │   │   ├── AiSuggestionTables.scala      # NOUVEAU: Tables IA
│   │   │   │   │   └── RelationTables.scala          # Tables de liaison
│   │   │   │   │
│   │   │   │   └── repositories/         # Implémentations Slick
│   │   │   │       ├── hops/
│   │   │   │       │   ├── SlickHopReadRepository.scala
│   │   │   │       │   └── SlickHopWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── malts/
│   │   │   │       │   ├── SlickMaltReadRepository.scala
│   │   │   │       │   └── SlickMaltWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── yeasts/
│   │   │   │       │   ├── SlickYeastReadRepository.scala
│   │   │   │       │   └── SlickYeastWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── recipes/           # NOUVEAU: Repositories recettes
│   │   │   │       │   ├── SlickRecipeReadRepository.scala
│   │   │   │       │   └── SlickRecipeWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── ai/                # NOUVEAU: Repositories IA
│   │   │   │       │   ├── SlickAiSuggestionReadRepository.scala
│   │   │   │       │   └── SlickAiSuggestionWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── beerstyles/
│   │   │   │       │   ├── SlickBeerStyleReadRepository.scala
│   │   │   │       │   └── SlickBeerStyleWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── origins/
│   │   │   │       │   ├── SlickOriginReadRepository.scala
│   │   │   │       │   └── SlickOriginWriteRepository.scala
│   │   │   │       │
│   │   │   │       ├── aromas/
│   │   │   │       │   ├── SlickAromaReadRepository.scala
│   │   │   │       │   └── SlickAromaWriteRepository.scala
│   │   │   │       │
│   │   │   │       └── admin/
│   │   │   │           ├── SlickAdminReadRepository.scala
│   │   │   │           └── SlickAdminWriteRepository.scala
│   │   │   │
│   │   │   └── memory/                   # Implémentations mémoire pour tests
│   │   │       ├── InMemoryHopRepository.scala
│   │   │       ├── InMemoryMaltRepository.scala
│   │   │       ├── InMemoryYeastRepository.scala
│   │   │       ├── InMemoryRecipeRepository.scala    # NOUVEAU
│   │   │       ├── InMemoryAiSuggestionRepository.scala # NOUVEAU
│   │   │       ├── InMemoryBeerStyleRepository.scala
│   │   │       ├── InMemoryOriginRepository.scala
│   │   │       ├── InMemoryAromaRepository.scala
│   │   │       └── InMemoryAdminRepository.scala
│   │   │
│   │   ├── external/                     # Services externes
│   │   │   ├── email/
│   │   │   │   └── EmailService.scala
│   │   │   │
│   │   │   ├── files/
│   │   │   │   └── FileStorageService.scala
│   │   │   │
│   │   │   ├── ai/                       # 🤖 NOUVEAU: Clients IA externes
│   │   │   │   ├── OpenAiClient.scala               # Client OpenAI
│   │   │   │   ├── AnthropicClient.scala            # Client Anthropic
│   │   │   │   ├── BrewingAiClient.scala            # IA spécialisée brassage
│   │   │   │   ├── RecipeAnalysisClient.scala       # Service analyse recettes
│   │   │   │   └── IngredientDataClient.scala       # Données ingrédients
│   │   │   │
│   │   │   ├── brewing/                  # 🍺 NOUVEAU: APIs brassage externes
│   │   │   │   ├── BreweryDbClient.scala            # Base données brasseries
│   │   │   │   ├── BjcpApiClient.scala              # API BJCP
│   │   │   │   ├── HopUnionClient.scala             # Données houblons
│   │   │   │   ├── MaltsterClient.scala             # Données malteries
│   │   │   │   └── YeastLabClient.scala             # Données laboratoires
│   │   │   │
│   │   │   └── equipment/                # ⚙️ NOUVEAU: Intégrations équipement
│   │   │       ├── MiniBrewClient.scala             # API MiniBrew
│   │   │       ├── BrewfatherClient.scala           # Brewfather integration
│   │   │       ├── BeerSmithClient.scala            # BeerSmith integration
│   │   │       └── GenericEquipmentClient.scala     # Équipements génériques
│   │   │
│   │   └── config/
│   │       ├── DatabaseConfig.scala
│   │       ├── AppConfig.scala
│   │       ├── AiConfig.scala                       # NOUVEAU: Config IA
│   │       └── BrewingConfig.scala                  # NOUVEAU: Config brassage
│   │
│   ├── interfaces/                       # 🌐 COUCHE INTERFACE (Contrôleurs)
│   │   ├── http/
│   │   │   ├── common/
│   │   │   │   ├── BaseController.scala
│   │   │   │   ├── ErrorHandler.scala
│   │   │   │   ├── JsonFormats.scala
│   │   │   │   ├── Validation.scala
│   │   │   │   └── PaginationController.scala       # NOUVEAU: Base pagination
│   │   │   │
│   │   │   ├── api/                      # API REST
│   │   │   │   ├── v1/
│   │   │   │   │   ├── ingredients/      # 🧪 NOUVEAU: Contrôleurs ingrédients
│   │   │   │   │   │   ├── HopsController.scala     # CRUD + filtrage avancé
│   │   │   │   │   │   ├── MaltsController.scala    # CRUD + filtrage avancé  
│   │   │   │   │   │   ├── YeastsController.scala   # CRUD + filtrage avancé
│   │   │   │   │   │   └── FilterController.scala   # Filtrage multi-ingrédients
│   │   │   │   │   │
│   │   │   │   │   ├── recipes/          # 📝 NOUVEAU: Contrôleurs recettes
│   │   │   │   │   │   ├── RecipesController.scala      # CRUD recettes
│   │   │   │   │   │   ├── RecipeIngredientsController.scala # Gestion ingrédients
│   │   │   │   │   │   ├── RecipeProceduresController.scala  # Gestion procédures
│   │   │   │   │   │   ├── RecipeScalingController.scala     # Scaling volumes
│   │   │   │   │   │   ├── RecipeCalculationsController.scala # Calculs temps réel
│   │   │   │   │   │   ├── RecipeExportController.scala      # Export recettes
│   │   │   │   │   │   └── RecipeCloneController.scala       # Clonage recettes
│   │   │   │   │   │
│   │   │   │   │   ├── ai/               # 🤖 NOUVEAU: Contrôleurs IA
│   │   │   │   │   │   ├── AiSuggestionsController.scala     # Suggestions IA
│   │   │   │   │   │   ├── RecipeAnalysisController.scala    # Analyse recettes
│   │   │   │   │   │   ├── StyleMatchingController.scala     # Matching styles
│   │   │   │   │   │   ├── BalanceOptimizationController.scala # Optimisation
│   │   │   │   │   │   └── IngredientAlternativesController.scala # Alternatives
│   │   │   │   │   │
│   │   │   │   │   ├── dashboard/        # 📊 NOUVEAU: Contrôleurs dashboard
│   │   │   │   │   │   ├── RecipeDashboardController.scala   # Dashboard recette
│   │   │   │   │   │   ├── CharacteristicsController.scala   # Caractéristiques bière
│   │   │   │   │   │   ├── ComparisonController.scala        # Comparaisons
│   │   │   │   │   │   └── StatisticsController.scala        # Statistiques
│   │   │   │   │   │
│   │   │   │   │   ├── equipment/        # ⚙️ NOUVEAU: Contrôleurs équipement
│   │   │   │   │   │   ├── EquipmentController.scala         # Gestion équipements
│   │   │   │   │   │   ├── MiniBrewController.scala          # Spécialisation MiniBrew
│   │   │   │   │   │   ├── ProcedureAdaptationController.scala # Adaptation procédures
│   │   │   │   │   │   └── EfficiencyController.scala        # Calculs efficacité
│   │   │   │   │   │
│   │   │   │   │   ├── referentials/     # Référentiels
│   │   │   │   │   │   ├── BeerStylesController.scala
│   │   │   │   │   │   ├── OriginsController.scala
│   │   │   │   │   │   └── AromasController.scala
│   │   │   │   │   │
│   │   │   │   │   └── ReseedController.scala
│   │   │   │   │
│   │   │   │   └── admin/
│   │   │   │       ├── AdminAuthController.scala
│   │   │   │       ├── AdminUsersController.scala
│   │   │   │       ├── AdminIngredientsController.scala  # NOUVEAU: Admin ingrédients
│   │   │   │       ├── AdminRecipesController.scala      # NOUVEAU: Admin recettes
│   │   │   │       ├── AdminAiController.scala           # NOUVEAU: Admin IA
│   │   │   │       └── AdminImportController.scala
│   │   │   │
│   │   │   ├── web/                      # Interface web
│   │   │   │   ├── HomeController.scala
│   │   │   │   ├── RecipeBuilderController.scala        # NOUVEAU: Builder recettes
│   │   │   │   └── DashboardController.scala            # NOUVEAU: Dashboard web
│   │   │   │
│   │   │   └── dto/                      # Data Transfer Objects
│   │   │       ├── requests/
│   │   │       │   ├── ingredients/      # Requests ingrédients
│   │   │       │   │   ├── HopFilterRequest.scala
│   │   │       │   │   ├── MaltFilterRequest.scala
│   │   │       │   │   ├── YeastFilterRequest.scala
│   │   │       │   │   ├── AddToRecipeRequest.scala     # NOUVEAU: Ajouter à recette
│   │   │       │   │   └── AdvancedFilterRequest.scala  # NOUVEAU: Filtres avancés
│   │   │       │   │
│   │   │       │   ├── recipes/          # NOUVEAU: Requests recettes
│   │   │       │   │   ├── CreateRecipeRequest.scala
│   │   │       │   │   ├── UpdateRecipeRequest.scala
│   │   │       │   │   ├── AddIngredientRequest.scala
│   │   │       │   │   ├── UpdateIngredientRequest.scala
│   │   │       │   │   ├── ScaleRecipeRequest.scala
│   │   │       │   │   ├── GenerateProcedureRequest.scala
│   │   │       │   │   └── CloneRecipeRequest.scala
│   │   │       │   │
│   │   │       │   ├── ai/               # NOUVEAU: Requests IA
│   │   │       │   │   ├── AiSuggestionRequest.scala
│   │   │       │   │   ├── RecipeAnalysisRequest.scala
│   │   │       │   │   ├── StyleMatchingRequest.scala
│   │   │       │   │   └── OptimizationRequest.scala
│   │   │       │   │
│   │   │       │   └── dashboard/        # NOUVEAU: Requests dashboard
│   │   │       │       ├── DashboardRequest.scala
│   │   │       │       └── ComparisonRequest.scala
│   │   │       │
│   │   │       └── responses/
│   │   │           ├── ingredients/      # Responses ingrédients
│   │   │           │   ├── HopResponse.scala
│   │   │           │   ├── MaltResponse.scala
│   │   │           │   ├── YeastResponse.scala
│   │   │           │   ├── FilteredIngredientsResponse.scala # NOUVEAU
│   │   │           │   └── IngredientForRecipeResponse.scala # NOUVEAU
│   │   │           │
│   │   │           ├── recipes/          # NOUVEAU: Responses recettes
│   │   │           │   ├── RecipeResponse.scala
│   │   │           │   ├── RecipeDetailResponse.scala
│   │   │           │   ├── RecipeListResponse.scala
│   │   │           │   ├── RecipeCalculationsResponse.scala
│   │   │           │   ├── RecipeProcedureResponse.scala
│   │   │           │   ├── ScaledRecipeResponse.scala
│   │   │           │   └── MiniBrowProcedureResponse.scala
│   │   │           │
│   │   │           ├── ai/               # NOUVEAU: Responses IA
│   │   │           │   ├── AiSuggestionResponse.scala
│   │   │           │   ├── RecipeAnalysisResponse.scala
│   │   │           │   ├── StyleComplianceResponse.scala
│   │   │           │   ├── BalanceAnalysisResponse.scala
│   │   │           │   └── ImprovementSuggestionResponse.scala
│   │   │           │
│   │   │           ├── dashboard/        # NOUVEAU: Responses dashboard
│   │   │           │   ├── RecipeDashboardResponse.scala
│   │   │           │   ├── BeerCharacteristicsResponse.scala
│   │   │           │   ├── IngredientStatsResponse.scala
│   │   │           │   ├── StyleMatchResponse.scala
│   │   │           │   └── ComparisonResponse.scala
│   │   │           │
│   │   │           ├── referentials/
│   │   │           │   ├── BeerStyleResponse.scala
│   │   │           │   ├── OriginResponse.scala
│   │   │           │   └── AromaResponse.scala
│   │   │           │
│   │   │           └── common/
│   │   │               ├── ErrorResponse.scala
│   │   │               ├# Architecture complète - Système de recettes de brassage
## Domaines + Recettes + IA + Calculs + Dashboard

## 📂 Structure complète du projet étendue

```
project/
├── app/
│   ├── domain/                           # 🏛️ COUCHE DOMAINE (Pure business logic)
│   │   ├── common/
│   │   │   ├── ValueObject.scala         # Trait de base pour Value Objects
│   │   │   ├── DomainError.scala         # Types d'erreurs métier
│   │   │   ├── DomainEvent.scala         # Events de base
│   │   │   ├── AggregateRoot.scala       # Trait pour les agrégats
│   │   │   └── Repository.scala          # Interfaces repository de base
│   │   │
│   │   ├── shared/                       # Value Objects partagés
│   │   │   ├── Email.scala
│   │   │   ├── NonEmptyString.scala
│   │   │   ├── PositiveDouble.scala
│   │   │   ├── Range.scala
│   │   │   ├── Percentage.scala
│   │   │   ├── Temperature.scala         # Température (°C/°F)
│   │   │   ├── Duration.scala            # Durée (minutes/heures)
│   │   │   ├── Gravity.scala             # Densité spécifique (1.040-1.120)
│   │   │   ├── Volume.scala              # Volume (L/gal) avec conversions
│   │   │   ├── Weight.scala              # Poids (g/kg/oz/lb) avec conversions
│   │   │   ├── Color.scala               # Couleur bière (SRM/EBC/Lovibond)
│   │   │   ├── Bitterness.scala          # Amertume (IBU)
│   │   │   └── AlcoholContent.scala      # Taux alcool (ABV)
│   │   │
│   │   ├── referentials/                 # 📚 RÉFÉRENTIELS PARTAGÉS
│   │   │   ├── beerstyles/               # Styles de bière BJCP
│   │   │   │   ├── model/
│   │   │   │   │   ├── BeerStyleAggregate.scala
│   │   │   │   │   ├── BeerStyleId.scala
│   │   │   │   │   ├── BeerStyleName.scala
│   │   │   │   │   ├── BeerStyleCategory.scala  # IPA, Stout, Lager...
│   │   │   │   │   ├── BjcpCode.scala           # Code BJCP (21A, 12C...)
│   │   │   │   │   ├── StyleCharacteristics.scala # Ranges ABV, IBU, SRM
│   │   │   │   │   ├── StyleProfile.scala       # Apparence, arôme, saveur
│   │   │   │   │   └── BeerStyleEvents.scala
│   │   │   │   │
│   │   │   │   ├── services/
│   │   │   │   │   ├── BeerStyleDomainService.scala
│   │   │   │   │   └── StyleMatchingService.scala  # Matching recette ↔ style
│   │   │   │   │
│   │   │   │   └── repositories/
│   │   │   │       ├── BeerStyleRepository.scala
│   │   │   │       ├── BeerStyleReadRepository.scala
│   │   │   │       └── BeerStyleWriteRepository.scala
│   │   │   │
│   │   │   ├── origins/                  # Origines géographiques
│   │   │   │   ├── model/
│   │   │   │   │   ├── OriginAggregate.scala
│   │   │   │   │   ├── OriginId.scala
│   │   │   │   │   ├── OriginName.scala
│   │   │   │   │   ├── Country.scala         # Pays normalisé
│   │   │   │   │   ├── Region.scala          # Région (Hallertau, Kent...)
│   │   │   │   │   └── OriginEvents.scala
│   │   │   │   │
│   │   │   │   └── repositories/
│   │   │   │       ├── OriginRepository.scala
│   │   │   │       ├── OriginReadRepository.scala
│   │   │   │       └── OriginWriteRepository.scala
│   │   │   │
│   │   │   └── aromas/                   # Profils aromatiques partagés
│   │   │       ├── model/
│   │   │       │   ├── AromaAggregate.scala
│   │   │       │   ├── AromaId.scala
│   │   │       │   ├── AromaName.scala
│   │   │       │   ├── AromaCategory.scala   # Citrus, Floral, Earthy...
│   │   │       │   ├── AromaIntensity.scala  # Low, Medium, High
│   │   │       │   └── AromaEvents.scala
│   │   │       │
│   │   │       └── repositories/
│   │   │           ├── AromaRepository.scala
│   │   │           ├── AromaReadRepository.scala
│   │   │           └── AromaWriteRepository.scala
│   │   │
│   │   ├── hops/                         # 🍺 DOMAINE: Houblons
│   │   │   ├── model/
│   │   │   │   ├── HopAggregate.scala    # Agrégat principal houblon
│   │   │   │   ├── HopId.scala
│   │   │   │   ├── HopName.scala
│   │   │   │   ├── AlphaAcidPercentage.scala
│   │   │   │   ├── BetaAcidPercentage.scala
│   │   │   │   ├── CohumulonePercentage.scala
│   │   │   │   ├── HopUsage.scala        # Bittering, Aroma, Dual-purpose
│   │   │   │   ├── HopForm.scala         # Pellets, Whole, Extract
│   │   │   │   ├── HopStatus.scala       # Active, Discontinued, Limited
│   │   │   │   ├── HopProfile.scala      # Profil complet (usage + arômes)
│   │   │   │   ├── HopFilter.scala       # Filtres de recherche spécialisés
│   │   │   │   └── HopEvents.scala
│   │   │   │
│   │   │   ├── services/
│   │   │   │   ├── HopDomainService.scala
│   │   │   │   ├── HopValidationService.scala
│   │   │   │   ├── HopSubstitutionService.scala
│   │   │   │   └── HopFilterService.scala    # Service de filtrage avancé
│   │   │   │
│   │   │   └── repositories/
│   │   │       ├── HopRepository.scala
│   │   │       ├── HopReadRepository.scala
│   │   │       └── HopWriteRepository.scala
│   │   │
│   │   ├── malts/                        # 🌾 DOMAINE: Malts
│   │   │   ├── model/
│   │   │   │   ├── MaltAggregate.scala
│   │   │   │   ├── MaltId.scala
│   │   │   │   ├── MaltName.scala
│   │   │   │   ├── MaltType.scala        # Base, Specialty, Adjunct
│   │   │   │   ├── MaltGrade.scala       # Pilsner, Munich, Crystal...
│   │   │   │   ├── LovibondColor.scala   # Couleur SRM/EBC
│   │   │   │   ├── ExtractPotential.scala # Potentiel d'extraction (%)
│   │   │   │   ├── DiastaticPower.scala  # Pouvoir diastasique (Lintner)
│   │   │   │   ├── MaltUsage.scala       # Base malt, Specialty, Adjunct
│   │   │   │   ├── MaltCharacteristics.scala # Saveur, corps, etc.
│   │   │   │   ├── MaltStatus.scala
│   │   │   │   ├── MaltFilter.scala      # Filtres de recherche spécialisés
│   │   │   │   └── MaltEvents.scala
│   │   │   │
│   │   │   ├── services/
│   │   │   │   ├── MaltDomainService.scala
│   │   │   │   ├── MaltValidationService.scala
│   │   │   │   ├── MaltSubstitutionService.scala
│   │   │   │   └── MaltFilterService.scala   # Service de filtrage avancé
│   │   │   │
│   │   │   └── repositories/
│   │   │       ├── MaltRepository.scala
│   │   │       ├── MaltReadRepository.scala
│   │   │       └── MaltWriteRepository.scala
│   │   │
│   │   ├── yeasts/                       # 🦠 DOMAINE: Levures
│   │   │   ├── model/
│   │   │   │   ├── YeastAggregate.scala
│   │   │   │   ├── YeastId.scala
│   │   │   │   ├── YeastName.scala
│   │   │   │   ├── YeastStrain.scala     # Souche (WLP001, S-04...)
│   │   │   │   ├── YeastType.scala       # Ale, Lager, Wild, Brett
│   │   │   │   ├── YeastLaboratory.scala # Wyeast, White Labs, Lallemand...
│   │   │   │   ├── AttenuationRange.scala # Atténuation 75-82%
│   │   │   │   ├── FermentationTemp.scala # Température optimale
│   │   │   │   ├── AlcoholTolerance.scala # Tolérance alcool max
│   │   │   │   ├── FlocculationLevel.scala # Low, Medium, High
│   │   │   │   ├── YeastCharacteristics.scala # Profil gustatif
│   │   │   │   ├── YeastStatus.scala
│   │   │   │   ├── YeastFilter.scala     # Filtres de recherche spécialisés
│   │   │   │   └── YeastEvents.scala
│   │   │   │
│   │   │   ├── services/
│   │   │   │   ├── YeastDomainService.scala
│   │   │   │   ├── YeastValidationService.scala
│   │   │   │   ├── YeastRecommendationService.scala
│   │   │   │   └── YeastFilterService.scala  # Service de filtrage avancé
│   │   │   │
│   │   │   └── repositories/
│   │   │       ├── YeastRepository.scala
│   │   │       ├── YeastReadRepository.scala
│   │   │       └── YeastWriteRepository.scala
│   │   │
│   │   ├── recipes/                      # 📝 NOUVEAU DOMAINE: Recettes
│   │   │   ├── model/
│   │   │   │   ├── RecipeAggregate.scala # Agrégat principal recette
│   │   │   │   ├── RecipeId.scala        # Identity unique
│   │   │   │   ├── RecipeName.scala      # Nom de la recette
│   │   │   │   ├── RecipeDescription.scala # Description détaillée
│   │   │   │   ├── RecipeStatus.scala    # Draft, Published, Archived
│   │   │   │   ├── RecipeVersion.scala   # Versioning des recettes
│   │   │   │   ├── BatchSize.scala       # Volume de brassage (5L, 10L...)
│   │   │   │   │
│   │   │   │   ├── ingredients/          # Ingrédients de la recette
│   │   │   │   │   ├── RecipeIngredient.scala    # Ingrédient + quantité
│   │   │   │   │   ├── HopIngredient.scala       # Houblon + timing + usage
│   │   │   │   │   ├── MaltIngredient.scala      # Malt + quantité + %
│   │   │   │   │   ├── YeastIngredient.scala     # Levure + quantité
│   │   │   │   │   ├── OtherIngredient.scala     # Autres (épices, fruits...)
│   │   │   │   │   ├── IngredientQuantity.scala  # Quantité avec unités
│   │   │   │   │   ├── HopTiming.scala           # Timing ajout houblon
│   │   │   │   │   └── IngredientPurpose.scala   # Usage de l'ingrédient
│   │   │   │   │
│   │   │   │   ├── procedures/           # Procédures de brassage
│   │   │   │   │   ├── BrewingProcedure.scala    # Procédure complète
│   │   │   │   │   ├── ProcedureStep.scala       # Étape individuelle
│   │   │   │   │   ├── StepType.scala            # Mash, Boil, Ferment...
│   │   │   │   │   ├── StepDuration.scala        # Durée de l'étape
│   │   │   │   │   ├── StepTemperature.scala     # Température cible
│   │   │   │   │   ├── MashProfile.scala         # Profil d'empâtage
│   │   │   │   │   ├── BoilSchedule.scala        # Planning d'ébullition
│   │   │   │   │   ├── FermentationProfile.scala # Profil fermentation
│   │   │   │   │   └── EquipmentProfile.scala    # Profil équipement
│   │   │   │   │
│   │   │   │   ├── calculations/         # Calculs et caractéristiques
│   │   │   │   │   ├── RecipeCharacteristics.scala # ABV, IBU, SRM, etc.
│   │   │   │   │   ├── GravityCalculation.scala    # Densités OG/FG
│   │   │   │   │   ├── BitternessCalculation.scala # Calcul IBU
│   │   │   │   │   ├── ColorCalculation.scala      # Calcul couleur SRM
│   │   │   │   │   ├── AlcoholCalculation.scala    # Calcul ABV
│   │   │   │   │   ├── AttenuationCalculation.scala # Calcul atténuation
│   │   │   │   │   ├── EfficiencyFactor.scala      # Facteur d'efficacité
│   │   │   │   │   └── VolumeScaling.scala         # Scaling par volume
│   │   │   │   │
│   │   │   │   ├── equipment/            # Équipement et scaling
│   │   │   │   │   ├── BrewingEquipment.scala      # Équipement général
│   │   │   │   │   ├── MiniBrew.scala              # Spécialisation MiniBrew
│   │   │   │   │   ├── TraditionalEquipment.scala  # Équipement traditionnel
│   │   │   │   │   ├── EquipmentEfficiency.scala   # Efficacité par équipement
│   │   │   │   │   └── ScalingProfile.scala        # Profil de scaling
│   │   │   │   │
│   │   │   │   └── RecipeEvents.scala    # Domain events
│   │   │   │
│   │   │   ├── services/                 # Services domaine recettes
│   │   │   │   ├── RecipeDomainService.scala
│   │   │   │   ├── RecipeValidationService.scala
│   │   │   │   ├── RecipeCalculationService.scala  # Calculs automatiques
│   │   │   │   ├── RecipeScalingService.scala      # Scaling par volume
│   │   │   │   ├── ProcedureGenerationService.scala # Génération procédures
│   │   │   │   ├── EquipmentAdaptationService.scala # Adaptation équipement
│   │   │   │   └── RecipeVersioningService.scala   # Gestion versions
│   │   │   │
│   │   │   └── repositories/
│   │   │       ├── RecipeRepository.scala
│   │   │       ├── RecipeReadRepository.scala
│   │   │       └── RecipeWriteRepository.scala
│   │   │
│   │   ├── ai/                           # 🤖 NOUVEAU DOMAINE: Intelligence Artificielle
│   │   │   ├── model/
│   │   │   │   ├── AiSuggestion.scala           # Suggestion IA
│   │   │   │   ├── SuggestionId.scala           # Identity suggestion
│   │   │   │   ├── SuggestionType.scala         # Type de suggestion
│   │   │   │   ├── SuggestionCategory.scala     # Catégorie (ingredient, procedure...)
│   │   │   │   ├── SuggestionConfidence.scala   # Niveau de confiance
│   │   │   │   ├── SuggestionReason.scala       # Raison de la suggestion
│   │   │   │   ├── RecipeAnalysis.scala         # Analyse complète recette
│   │   │   │   ├── StyleCompliance.scala        # Conformité au style
│   │   │   │   ├── BalanceAnalysis.scala        # Analyse d'équilibre
│   │   │   │   └── ImprovementSuggestion.scala  # Suggestions d'amélioration
│   │   │   │
│   │   │   ├── services/                        # Services IA
│   │   │   │   ├── RecipeAnalysisService.scala      # Analyse recettes
│   │   │   │   ├── StyleMatchingService.scala       # Matching style BJCP
│   │   │   │   ├── IngredientSuggestionService.scala # Suggestions ingrédients
│   │   │   │   ├── ProcedureSuggestionService.scala  # Suggestions procédures
│   │   │   │   ├── BalanceOptimizationService.scala # Optimisation équilibre
│   │   │   │   ├── SubstitutionService.scala        # Suggestions substitutions
│   │   │   │   └── RecipeRefinementService.scala    # Raffinement recettes
│   │   │   │
│   │   │   └── repositories/
│   │   │       ├── AiSuggestionRepository.scala
│   │   │       ├── AiSuggestionReadRepository.scala
│   │   │       └── AiSuggestionWriteRepository.scala
│   │   │
│   │   └── admin/                        # 👤 Administration
│   │       ├── model/
│   │       │   ├── AdminAggregate.scala
│   │       │   ├── AdminId.scala
│   │       │   ├── AdminName.scala
│   │       │   ├── AdminRole.scala
│   │       │   └── AdminEvents.scala
│   │       │
│   │       └── repositories/
│   │           ├── AdminRepository.scala
│   │           ├── AdminReadRepository.scala
│   │           └── AdminWriteRepository.scala
│   │
│   ├── application/                      # 🚀 COUCHE APPLICATION (Use cases)
│   │   ├── common/
│   │   │   ├── Command.scala
│   │   │   ├── Query.scala
│   │   │   ├── Handler.scala
│   │   │   ├── Page.scala
│   │   │   └── Result.scala
│   │   │
│   │   ├── commands/                     # 📝 CQRS - Write side
│   │   │   ├── hops/
│   │   │   │   ├── CreateHopCommand.scala
│   │   │   │   ├── UpdateHopCommand.scala
│   │   │   │   ├── DeleteHopCommand.scala
│   │   │   │   ├── AddHopToRecipeCommand.scala    # NOUVEAU: Ajouter à recette
│   │   │   │   └── handlers/
│   │   │   │       ├── CreateHopCommandHandler.scala
│   │   │   │       ├── UpdateHopCommandHandler.scala
│   │   │   │       ├── DeleteHopCommandHandler.scala
│   │   │   │       └── AddHopToRecipeCommandHandler.scala
│   │   │   │
│   │   │   ├── malts/
│   │   │   │   ├── CreateMaltCommand.scala
│   │   │   │   ├── UpdateMaltCommand.scala
│   │   │   │   ├── DeleteMaltCommand.scala
│   │   │   │   ├── AddMaltToRecipeCommand.scala   # NOUVEAU: Ajouter à recette
│   │   │   │   └── handlers/
│   │   │   │       ├── CreateMaltCommandHandler.scala
│   │   │   │       ├── UpdateMaltCommandHandler.scala
│   │   │   │       ├── DeleteMaltCommandHandler.scala
│   │   │   │       └── AddMaltToRecipeCommandHandler.scala
│   │   │   │
│   │   │   ├── yeasts/
│   │   │   │   ├── CreateYeastCommand.scala
│   │   │   │   ├── UpdateYeastCommand.scala
│   │   │   │   ├── DeleteYeastCommand.scala
│   │   │   │   ├── AddYeastToRecipeCommand.scala  # NOUVEAU: Ajouter à recette
│   │   │   │   └── handlers/
│   │   │   │       ├── CreateYeastCommandHandler.scala
│   │   │   │       ├── UpdateYeastCommandHandler.scala
│   │   │   │       ├── DeleteYeastCommandHandler.scala
│   │   │   │       └── AddYeastToRecipeCommandHandler.scala
│   │   │   │
│   │   │   ├── recipes/                  # 📝 NOUVEAU: Commandes recettes
│   │   │   │   ├── CreateRecipeCommand.scala       # Créer nouvelle recette
│   │   │   │   ├── UpdateRecipeCommand.scala       # Modifier recette
│   │   │   │   ├── DeleteRecipeCommand.scala       # Supprimer recette
│   │   │   │   ├── ScaleRecipeCommand.scala        # Scaler recette (volume)
│   │   │   │   ├── CloneRecipeCommand.scala        # Cloner recette
│   │   │   │   ├── AddIngredientCommand.scala      # Ajouter ingrédient
│   │   │   │   ├── RemoveIngredientCommand.scala   # Retirer ingrédient
│   │   │   │   ├── UpdateIngredientCommand.scala   # Modifier ingrédient
│   │   │   │   ├── GenerateProcedureCommand.scala  # Générer procédure
│   │   │   │   ├── AdaptToEquipmentCommand.scala   # Adapter équipement
│   │   │   │   └── handlers/
│   │   │   │       ├── CreateRecipeCommandHandler.scala
│   │   │   │       ├── UpdateRecipeCommandHandler.scala
│   │   │   │       ├── DeleteRecipeCommandHandler.scala
│   │   │   │       ├── ScaleRecipeCommandHandler.scala
│   │   │   │       ├── CloneRecipeCommandHandler.scala
│   │   │   │       ├── AddIngredientCommandHandler.scala
│   │   │   │       ├── RemoveIngredientCommandHandler.scala
│   │   │   │       ├── UpdateIngredientCommandHandler.scala
│   │   │   │       ├── GenerateProcedureCommandHandler.scala
│   │   │   │       └── AdaptToEquipmentCommandHandler.scala
│   │   │   │
│   │   │   ├── ai/                       # 🤖 NOUVEAU: Commandes IA
│   │   │   │   ├── RequestAiSuggestionsCommand.scala    # Demander suggestions IA
│   │   │   │   ├── ApplyAiSuggestionCommand.scala       # Appliquer suggestion
│   │   │   │   ├── RefineRecipeCommand.scala            # Raffiner avec IA
│   │   │   │   ├── OptimizeBalanceCommand.scala         # Optimiser équilibre
│   │   │   │   └── handlers/
│   │   │   │       ├── RequestAiSuggestionsCommandHandler.scala
│   │   │   │       ├── ApplyAiSuggestionCommandHandler.scala
│   │   │   │       ├── RefineRecipeCommandHandler.scala
│   │   │   │       └── OptimizeBalanceCommandHandler.scala
│   │   │   │
│   │   │   └── referentials/             # Gestion référentiels
│   │   │       ├── beerstyles/
│   │   │       │   ├── CreateBeerStyleCommand.scala
│   │   │       │   └── handlers/
│   │   │       │       └── CreateBeerStyleCommandHandler.scala
│   │   │       │
│   │   │       ├── origins/
│   │   │       │   ├── CreateOriginCommand.scala
│   │   │       │   └── handlers/
│   │   │       │       └── CreateOriginCommandHandler.scala
│   │   │       │
│   │   │       └── aromas/
│   │   │           ├── CreateAromaCommand.scala
│   │   │           └── handlers/
│   │   │               └── CreateAromaCommandHandler.scala
│   │   │
│   │   ├── queries/                      # 📖 CQRS - Read side
│   │   │   ├── hops/
│   │   │   │   ├── HopListQuery.scala
│   │   │   │   ├── HopDetailQuery.scala
│   │   │   │   ├── HopAdvancedFilterQuery.scala    # NOUVEAU: Filtrage avancé
│   │   │   │   ├── HopsByCharacteristicsQuery.scala # Filtrer par caractéristiques
│   │   │   │   ├── readmodels/
│   │   │   │   │   ├── HopReadModel.scala
│   │   │   │   │   ├── HopListItemReadModel.scala
│   │   │   │   │   ├── HopDetailReadModel.scala
│   │   │   │   │   └── HopForRecipeReadModel.scala  # NOUVEAU: Format recette
│   │   │   │   └── handlers/
│   │   │   │       ├── HopListQueryHandler.scala
│   │   │   │       ├── HopDetailQueryHandler.scala
│   │   │   │       ├── HopAdvancedFilterQueryHandler.scala
│   │   │   │       └── HopsByCharacteristicsQueryHandler.scala
│   │   │   │
│   │   │   ├── malts/
│   │   │   │   ├── MaltListQuery.scala
│   │   │   │   ├── MaltDetailQuery.scala
│   │   │   │   ├── MaltAdvancedFilterQuery.scala   # NOUVEAU: Filtrage avancé
│   │   │   │   ├── MaltsByCharacteristicsQuery.scala # Filtrer par caractéristiques
│   │   │   │   ├── readmodels/
│   │   │   │   │   ├── MaltReadModel.scala
│   │   │   │   │   ├── MaltListItemReadModel.scala
│   │   │   │   │   ├── MaltDetailReadModel.scala
│   │   │   │   │   └── MaltForRecipeReadModel.scala # NOUVEAU: Format recette
│   │   │   │   └── handlers/
│   │   │   │       ├── MaltListQueryHandler.scala
│   │   │   │       ├── MaltDetailQueryHandler.scala
│   │   │   │       ├── MaltAdvancedFilterQueryHandler.scala
│   │   │   │       └── MaltsByCharacteristicsQueryHandler.scala
│   │   │   │
│   │   │   ├── yeasts/
│   │   │   │   ├── YeastListQuery.scala
│   │   │   │   ├── YeastDetailQuery.scala
│   │   │   │   ├── YeastAdvancedFilterQuery.scala  # NOUVEAU: Filtrage avancé
│   │   │   │   ├── YeastsByCharacteristicsQuery.scala # Filtrer par caractéristiques
│   │   │   │   ├── readmodels/
│   │   │   │   │   ├── YeastReadModel.scala
│   │   │   │   │   ├── YeastListItemReadModel.scala
│   │   │   │   │   ├── YeastDetailReadModel.scala
│   │   │   │   │   └── YeastForRecipeReadModel.scala # NOUVEAU: Format recette
│   │   │   │   └── handlers/
│   │   │   │       ├── YeastListQueryHandler.scala
│   │   │   │       ├── YeastDetailQueryHandler.scala
│   │   │   │       ├── YeastAdvancedFilterQueryHandler.scala
│   │   │   │       └