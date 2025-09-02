# Intelligence des Ingrédients - Démarche Unifiée

## 🎯 Vision et Objectif

**Principe fondamental** : Chaque domaine d'ingrédient (Hops, Malts, Yeasts) doit offrir le même niveau d'intelligence et de guidance utilisateur pour optimiser les choix brassicoles et les substitutions.

---

## 🔍 Constat Initial

### ❌ **Déséquilibre Fonctionnel Actuel**
- **YEASTS** : 6 endpoints intelligents, recommandations ML, prédictions fermentation
- **HOPS** : 3 endpoints basiques, substitutions simples
- **MALTS** : 4 endpoints basiques, substitutions simples

### 🎯 **Impact Gustatif Réel**
- **HOPS** : Maîtres de l'arôme (agrumes, floraux, fruités, épicés, résineux)
- **MALTS** : Fondation gustative (biscuit, caramel, chocolat, café, fumé)  
- **YEASTS** : Agents de transformation (esters, phénols, atténuation)

**Conclusion** : Les 3 domaines contribuent autant au profil gustatif final et méritent la même sophistication.

---

## 🚀 Objectif d'Uniformisation

### **Niveau d'Intelligence Cible** 
Chaque domaine doit proposer :
- ✅ **Recommandations par style BJCP**
- ✅ **Profils gustatifs avancés** 
- ✅ **Substitutions intelligentes**
- ✅ **Combinaisons optimales**
- ✅ **Prédictions d'impact**
- ✅ **Guidance utilisateur** (débutant → expert)

### **APIs Intelligentes Standardisées**
```bash
# Pattern unifié pour les 3 domaines
/api/v1/{ingredient}/recommendations/style/:style     # Par style BJCP
/api/v1/{ingredient}/recommendations/beginner         # Guidance débutant  
/api/v1/{ingredient}/recommendations/experimental     # Options avancées
/api/v1/{ingredient}/:id/alternatives                 # Substitutions smart
/api/v1/{ingredient}/analyze-flavor                   # Analyse profil gustatif
/api/v1/{ingredient}/combinations/optimize            # Combinaisons recommandées
```

---

## 🏗️ Démarche d'Implémentation

### **Phase 2C - Hops Intelligence** 🍺
**Objectif** : Élever les houblons au niveau yeasts

#### **Fonctionnalités à Implémenter**
1. **Recommandations Style** 
   - Houblons optimaux par style BJCP (IPA, Pilsner, Stout...)
   - Timing d'ajout recommandé (début, fin, dry hop)
   - Proportions suggérées selon objectif (amertume vs arôme)

2. **Profils Aromatiques Avancés**
   - Classification par famille (agrumes, tropical, floral, épicé, résineux)
   - Intensité aromatique prédite selon timing
   - Compatibilité inter-houblons pour combinaisons

3. **Substitutions Intelligentes**
   - Conservation profil aromatique (pas seulement alpha acids)
   - Alternatives selon disponibilité géographique/saisonnière
   - Impact sur profil final (préservation vs variation)

4. **Guidance Utilisateur**
   - Houblons "beginner-friendly" (Cascade, Centennial...)
   - Variétés expérimentales pour brasseurs avancés
   - Combinaisons éprouvées vs créatives

#### **Business Logic**
```scala
class HopIntelligenceService {
  def getStyleRecommendations(style: BJCPStyle): List[HopRecommendation]
  def predictAromaImpact(hop: Hop, timing: AdditionTiming): AromaProfile
  def findFlavorCompatibleHops(primaryHop: Hop): List[Hop]
  def optimizeHopSchedule(targetProfile: FlavorProfile): HopSchedule
  def generateBeginnerGuidance(hop: Hop): BrewingGuidance
}
```

---

### **Phase 2D - Malts Intelligence** 🌾
**Objectif** : Sophistication égale pour la fondation gustative

#### **Fonctionnalités à Implémenter**
1. **Construction de Recettes**
   - Sélection automatique malt de base selon style
   - Ajout specialty malts pour atteindre profil cible
   - Équilibrage couleur/saveur/corps automatique

2. **Profils Gustatifs Prédictifs**
   - Impact saveur par pourcentage (Crystal 10% = caramel léger)
   - Prédiction corps final selon extraction/diastatic power
   - Équilibre sucrosité/amertume selon composition

3. **Optimisation Couleur**
   - Combinaisons pour atteindre EBC précis
   - Minimisation coût tout en préservant profil
   - Alternatives locales/disponibles

4. **Substitutions Complexes**
   - Remplacement composite (1 malt → 2-3 malts équivalents)
   - Préservation extraction ET profil gustatif
   - Adaptation selon équipement (BIAB, all-grain...)

#### **Business Logic**
```scala
class MaltIntelligenceService {
  def buildRecipeForStyle(style: BJCPStyle, targetOG: Double): MaltBill
  def predictFlavorProfile(malts: List[MaltRatio]): FlavorProfile
  def optimizeForColor(targetEBC: Int, constraints: List[Constraint]): List[MaltCombination]
  def findComplexSubstitution(originalMalt: Malt, context: RecipeContext): SubstitutionPlan
  def generateMaltingGuidance(maltBill: MaltBill): MashingRecommendations
}
```

---

## 📊 Architecture Technique Unifiée

### **Modèle de Données Harmonisé**
```scala
// Profil gustatif standardisé tous ingrédients
case class FlavorProfile(
  intensity: FlavorIntensity,
  primaryNotes: List[FlavorNote],
  secondaryNotes: List[FlavorNote], 
  balance: FlavorBalance,
  complexity: ComplexityLevel
)

// Recommandation standardisée
case class IngredientRecommendation(
  ingredient: Ingredient,
  confidence: ConfidenceScore,
  reasoning: List[String],
  alternatives: List[Alternative],
  brewingTips: List[BrewingTip]
)

// Analyse d'impact standardisée  
case class FlavorImpactAnalysis(
  directImpact: FlavorProfile,
  synergies: List[FlavorSynergy],
  conflicts: List[FlavorConflict],
  optimalUsage: UsageRecommendation
)
```

### **Services Intelligence Unifiés**
```scala
trait IngredientIntelligenceService[T <: Ingredient] {
  def getStyleRecommendations(style: BJCPStyle): List[IngredientRecommendation]
  def analyzeFlavorImpact(ingredient: T, context: RecipeContext): FlavorImpactAnalysis  
  def findIntelligentSubstitutions(ingredient: T): List[SubstitutionOption]
  def generateBeginnerGuidance(ingredient: T): BrewingGuidance
  def predictIngredientSynergies(ingredients: List[T]): List[FlavorSynergy]
}
```

---

## 🎯 Résultat Attendu

### **Expérience Utilisateur Cohérente**
- **Débutant** : Guidance identique sur les 3 domaines
- **Intermédiaire** : Recommandations style harmonisées  
- **Expert** : Outils prédictifs avancés uniformes

### **Écosystème Intégré**
- Recommandations croisées (hop + malt + yeast)
- Optimisation globale profil gustatif
- Intelligence collective des 3 domaines

### **Évolution Scalable**
- Pattern reproductible pour futurs ingrédients (adjuvants, épices...)
- Architecture ML réutilisable
- Base solide pour Recipe Builder (Phase 4)

---

## 📈 Métriques de Succès

### **Quantitatifs**
- **APIs** : 12+ endpoints intelligents par domaine (= Yeasts actuel)
- **Substitutions** : Précision >90% préservation profil gustatif  
- **Recommandations** : Couverture 100% styles BJCP majeurs
- **Performance** : <100ms endpoints intelligence

### **Qualitatifs** 
- Feedback utilisateur positif sur guidance
- Réduction erreurs substitution  
- Adoption croissante fonctionnalités avancées
- Cohérence expérience multi-domaines

---

## 🚀 Prochaines Étapes

1. **Validation Concept** : Implémenter 1-2 endpoints intelligence Hops
2. **Architecture Foundation** : Services et modèles unifiés  
3. **Rollout Progressif** : Phase 2C (Hops) puis 2D (Malts)
4. **Intégration Finale** : Recipe Builder avec intelligence 3 domaines

**Principe directeur** : *"Chaque ingrédient mérite la même intelligence pour guider les choix du brasseur"*

---

*Document vivant - Mise à jour selon feedback et évolution technique*