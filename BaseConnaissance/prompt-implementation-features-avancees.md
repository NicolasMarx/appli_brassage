# Prompt d'Implémentation - Fonctionnalités Avancées Recipes
## Contexte et Objectif

Implémenter 5 fonctionnalités avancées pour finaliser le domaine Recipes dans le respect STRICT des principes établis dans BaseConnaissance et des patterns existants. **OBJECTIF : 100% de compilation sans erreur**.

## Fonctionnalités à Implémenter

### 1. Repository Recipes avec Persistance PostgreSQL Réelle
### 2. Uniformisation Intelligence (Hops/Malts au niveau Yeasts) 
### 3. Event Sourcing Recipes avec Historique
### 4. Calculs Brassage Avancés (IBU/SRM/ABV précis)
### 5. API Publique Recipes pour Consultation Communautaire

---

## RÈGLES CRITIQUES D'IMPLÉMENTATION

### ⚠️ PATTERNS OBLIGATOIRES À RESPECTER

1. **Copier EXACTEMENT les patterns existants** - Ne jamais inventer
2. **Value Objects** : Suivre le pattern exact de `YeastName.scala`
3. **Repositories** : Suivre le pattern exact de `YeastRepository.scala` 
4. **Controllers** : Suivre le pattern exact de `YeastPublicController.scala`
5. **Database** : Utiliser EXACTEMENT le même pattern que `YeastSlickRepository.scala`
6. **JSON** : Utiliser EXACTEMENT les mêmes formats que les Yeasts
7. **Events** : Suivre le pattern exact de `YeastEvent.scala`

### 📋 CHECKLIST OBLIGATOIRE AVANT IMPLÉMENTATION

- [ ] Lire et analyser tous les fichiers Yeasts existants
- [ ] Identifier les patterns exacts dans Hops et Malts 
- [ ] Vérifier les imports et dépendances existantes
- [ ] Confirmer les formats JSON utilisés
- [ ] Valider la structure des tables PostgreSQL

---

## FONCTIONNALITÉ 1 : Repository Recipes avec Persistance Réelle

### Objectif
Remplacer les mocks par une vraie persistance PostgreSQL en suivant le pattern `YeastSlickRepository.scala`.

### Pattern à Suivre EXACTEMENT
```scala
// Copier EXACTEMENT de app/infrastructure/persistence/slick/yeasts/YeastSlickRepository.scala
class RecipeSlickRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
  extends RecipeRepository {
  
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._
  
  // Table mapping EXACT comme YeastsTable
  class RecipesTable(tag: Tag) extends Table[RecipeRow](tag, "recipes") {
    // Colonnes EXACTES comme dans yeasts_table
  }
}
```

### Actions Requises
1. **Créer `RecipeSlickRepository.scala`** - Copier pattern exact YeastSlickRepository
2. **Créer migration SQL** - Suivre le pattern exact `yeasts_table.sql`
3. **Créer `RecipeRow.scala`** - Copier pattern exact YeastRow
4. **Mettre à jour Module DI** - Ajouter binding comme pour Yeasts
5. **Intégrer dans `RecipeAdminController`** - Remplacer mocks

### Validation Obligatoire
- Table PostgreSQL créée avec mêmes colonnes metadata
- Repository injecté correctement dans controller
- CRUD complet fonctionnel 
- Tests de persistance réussis

---

## FONCTIONNALITÉ 2 : Uniformisation Intelligence (Hops/Malts niveau Yeasts)

### Objectif
Mettre Hops et Malts au même niveau d'intelligence que Yeasts avec recommandations, alternatives, recherche avancée.

### Pattern Yeasts à Reproduire EXACTEMENT
```scala
// Endpoints à reproduire pour Hops et Malts
GET /api/v1/hops/recommendations/beginner
GET /api/v1/hops/recommendations/style/:style  
GET /api/v1/hops/:hopId/alternatives
GET /api/v1/hops/popular
POST /api/v1/hops/search (recherche complexe)

GET /api/v1/malts/recommendations/beginner
GET /api/v1/malts/recommendations/style/:style
GET /api/v1/malts/:maltId/alternatives  
GET /api/v1/malts/popular
POST /api/v1/malts/search (recherche complexe)
```

### Actions Requises
1. **Étendre `HopsController`** - Ajouter tous les endpoints de YeastPublicController
2. **Étendre `MaltsController`** - Ajouter tous les endpoints de YeastPublicController  
3. **Créer services de recommandation** - Copier logique YeastRecommendationService
4. **Ajouter metadata intelligente** - Profils aromatiques, compatibilités styles
5. **Mettre à jour routes** - Respecter l'ordre exact des routes Yeasts

### Validation Obligatoire
- Mêmes endpoints disponibles pour Hops/Malts/Yeasts
- Recommandations pertinentes générées
- Alternatives calculées correctement
- Recherche avancée fonctionnelle

---

## FONCTIONNALITÉ 3 : Event Sourcing Recipes avec Historique

### Objectif
Implémenter Event Sourcing complet pour tracer toutes les modifications des recettes.

### Pattern à Suivre EXACTEMENT
```scala
// Copier EXACTEMENT de domain/yeasts/model/YeastEvent.scala
sealed trait RecipeEvent {
  def aggregateId: RecipeId
  def timestamp: Instant
  def eventType: String
  def toJson: JsValue
}

case class RecipeCreatedEvent(
  aggregateId: RecipeId,
  name: RecipeName,
  timestamp: Instant = Instant.now()
) extends RecipeEvent {
  override def eventType: String = "RecipeCreated"
  override def toJson: JsValue = Json.toJson(this)
}
```

### Actions Requises
1. **Créer `RecipeEvent.scala`** - Tous les events de cycle de vie
2. **Créer `RecipeEventStore.scala`** - Persistance events en JSONB
3. **Créer migration events** - Table recipe_events comme yeast_events
4. **Intégrer dans Repository** - Publier events à chaque modification
5. **Créer endpoint historique** - GET /api/admin/recipes/:id/history

### Events Obligatoires
- `RecipeCreatedEvent`
- `RecipeUpdatedEvent` 
- `RecipeStatusChangedEvent`
- `RecipeArchivedEvent`
- `RecipeDeletedEvent`

### Validation Obligatoire
- Events stockés en JSONB PostgreSQL
- Historique complet récupérable
- Timeline reconstituable
- Performance optimisée

---

## FONCTIONNALITÉ 4 : Calculs Brassage Avancés

### Objectif
Implémenter des calculs précis pour IBU, SRM, ABV avec formules scientifiques reconnues.

### Formules à Implémenter
```scala
// Service de calculs précis
class BrewingCalculationsService {
  
  // IBU - Formule Tinseth (précision maximale)
  def calculateIBU(hops: List[RecipeHop], batchSize: BatchSize, originalGravity: Double): Double
  
  // SRM - Formule Morey (couleur précise) 
  def calculateSRM(malts: List[RecipeMalt], batchSize: BatchSize): Double
  
  // ABV - Formule standard avec correction température
  def calculateABV(originalGravity: Double, finalGravity: Double): Double
  
  // Efficacité brasserie (rendement réel vs théorique)
  def calculateEfficiency(theoretical: Double, actual: Double): Double
}
```

### Actions Requises
1. **Créer `BrewingCalculationsService.scala`** - Toutes les formules précises
2. **Créer `BrewingFormulas.scala`** - Constantes et formules scientifiques  
3. **Intégrer dans RecipeAggregate** - Calculs automatiques à la création/modification
4. **Créer endpoint calculs** - POST /api/admin/recipes/calculate
5. **Valider avec données réelles** - Comparer avec logiciels pro (BeerSmith)

### Validation Obligatoire
- Formules validées scientifiquement
- Précision ±2% vs logiciels référence
- Performance optimisée
- Tests unitaires exhaustifs

---

## FONCTIONNALITÉ 5 : API Publique Recipes

### Objectif
API publique pour consultation communautaire des recettes, suivant le pattern exact des Yeasts.

### Endpoints à Créer (Pattern YeastPublicController EXACT)
```scala
// Routes publiques (pas d'auth)
GET /api/v1/recipes                    # Liste recettes publiques
GET /api/v1/recipes/:recipeId          # Détail recette
GET /api/v1/recipes/search             # Recherche avancée
GET /api/v1/recipes/popular            # Recettes populaires  
GET /api/v1/recipes/style/:style       # Recettes par style
GET /api/v1/recipes/recommendations/beginner    # Pour débutants
GET /api/v1/recipes/recommendations/seasonal    # Saisonnières
GET /api/v1/recipes/recommendations/advanced    # Avancées
```

### Actions Requises
1. **Créer `RecipePublicController.scala`** - Copier pattern exact YeastPublicController
2. **Ajouter filtres de confidentialité** - Seules recettes "PUBLISHED" visibles
3. **Créer DTOs publiques** - Versions simplifiées sans données sensibles
4. **Implémenter recommandations** - Algorithmes basés sur popularité/saison
5. **Mettre à jour routes** - Respecter ordre exact (spécifiques avant :id)

### Validation Obligatoire
- API accessible sans authentification
- Données sensibles filtrées
- Performance optimisée pour grand volume
- Cache intelligent implémenté

---

## INSTRUCTIONS SPÉCIALES D'IMPLÉMENTATION

### 🔒 Sécurité et Confidentialité
```scala
// Filtre OBLIGATOIRE pour API publique
def filterPublicRecipes(recipes: Seq[RecipeAggregate]): Seq[RecipeAggregate] = {
  recipes.filter(_.status == RecipeStatus.Published)
         .map(sanitizeForPublic) // Retirer données sensibles
}
```

### 📊 Performance et Cache
```scala
// Cache intelligent comme pour Yeasts
@Cacheable("recipes.public")
def getPopularRecipes(limit: Int): Future[Seq[RecipeResponseDTO]]

@Cacheable("recipes.recommendations")  
def getRecommendations(criteria: RecommendationCriteria): Future[Seq[RecipeResponseDTO]]
```

### 🔄 Migration et Compatibilité
1. **Migration progressive** - Pas de breaking changes
2. **Backward compatibility** - Maintenir API existante
3. **Rollback plan** - Possibilité de retour en arrière
4. **Monitoring** - Logs détaillés pour debug

---

## VALIDATION FINALE OBLIGATOIRE

### ✅ Tests à Effectuer
1. **Compilation complète** - `sbt compile` sans erreur
2. **Tests unitaires** - Tous les nouveaux services  
3. **Tests d'intégration** - Endpoints complets
4. **Tests de performance** - Charge acceptable
5. **Tests sécurité** - Pas de failles introduites

### 📈 Métriques de Succès
- **0 erreur compilation** 
- **100% endpoints fonctionnels**
- **Performance < 200ms** pour requêtes simples
- **Sécurité validée** par audit
- **Documentation complète** générée

### 🚀 Déploiement
1. **Tests staging** complets
2. **Migration DB** validée  
3. **Rollback testé**
4. **Monitoring activé**
5. **Documentation mise à jour**

---

## RAPPELS CRITIQUES

⚠️ **NE JAMAIS** inventer de nouveaux patterns
✅ **TOUJOURS** copier les patterns existants  
🔍 **VÉRIFIER** chaque import et dépendance
📝 **DOCUMENTER** chaque modification
🧪 **TESTER** chaque fonctionnalité

**OBJECTIF : Fonctionnalités avancées production-ready avec 0 erreur de compilation et architecture cohérente.**