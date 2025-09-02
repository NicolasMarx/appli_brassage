# Domaine RECIPES (Recettes) - Descriptif Fonctionnel

## 🎯 Vue d'ensemble du domaine

Le domaine Recipes constitue le cœur métier de la plateforme, orchestrant tous les autres domaines pour créer des recettes de brassage complètes. Il gère la composition, les procédures, les calculs automatiques et le scaling des recettes selon l'équipement.

**Statut** : 🔜 **À IMPLÉMENTER - Planifié Phase 4**

---

## 🚀 Fonctionnalités Prévues

### Création et Gestion
- **Recipe Builder** interactif avec drag & drop
- **Gestion versions** et historique modifications
- **Templates** par style de bière (BJCP)
- **Import/Export** formats standards (BeerXML, JSON)
- **Collaboration** et partage entre utilisateurs

### Calculs Automatiques
- **Caractéristiques** : ABV, IBU, SRM, OG, FG automatiques
- **Efficacité équipement** : Ajustement selon profil matériel
- **Prédictions** : Couleur finale, profil gustatif
- **Validation style** : Conformité BJCP en temps réel

### Scaling et Adaptation
- **Volumes flexibles** : 5L, 10L, 20L, 40L+
- **Profils équipement** : MiniBrew, traditionnel, professionnel
- **Adaptation ingrédients** : Substitutions automatiques si indisponible
- **Procédures adaptées** : Timing selon équipement

### Assistance IA
- **Recommandations** ingrédients selon style cible
- **Optimisation équilibre** malt/houblon/levure
- **Suggestions améliorations** basées sur analyse
- **Détection anomalies** dans la composition

---

## 📝 Types et Éléments Métier

### Structure de Recette

**RecipeAggregate** (Agrégat principal)
- **Identity** : RecipeId unique
- **Metadata** : Nom, description, créateur, version
- **Status** : Draft, Published, Archived, Private
- **Style Target** : Style BJCP ciblé avec tolérances
- **Batch Size** : Volume de base pour scaling

### Ingrédients et Quantités

**Ingrédients Maltés**
- **Malt de Base** : 80-100% facture maltée
- **Malts Spéciaux** : Caractère, couleur, corps (5-30%)
- **Adjuvants** : Texture spécifique (5-20% max)
- **Unités** : kg, g, livres, onces (conversion automatique)

**Houblons et Timing**
- **Ajouts** : 60min, 30min, 15min, flameout, dry hop
- **Usage** : Bittering, Flavor, Aroma, Whirlpool
- **Formes** : Pellets, cônes, extraits (facteur conversion)
- **Calcul IBU** : Formules Tinseth, Rager, adaptées équipement

**Levures et Fermentation**
- **Souche primaire** : Levure principale fermentation
- **Souches secondaires** : Brett, lactiques pour styles mixtes
- **Quantité** : Auto-calculée selon OG et volume
- **Starter** : Recommandations selon viabilité

**Autres Ingrédients**
- **Épices** : Timing ajout, quantités
- **Fruits** : Frais/congelés/purée, impact sur gravité
- **Clarifiants** : Irish moss, whirlfloc, gélatine
- **Sels** : Ajustement profil eau

### Procédures de Brassage

**Empâtage (Mashing)**
- **Single Infusion** : 1 palier température
- **Step Mash** : Multi-paliers (protein, β-amylase, α-amylase)
- **Decoction** : Méthode traditionnelle allemande
- **Température/Durée** : Optimisées selon composition malt

**Ébullition (Boiling)**
- **Durée** : 60-90 minutes selon style
- **Planning houblons** : Timing précis ajouts
- **Évaporation** : Calcul volume final selon équipement
- **Hot Break** : Coagulation protéines

**Fermentation**
- **Température** : Profil selon levure sélectionnée
- **Durée phases** : Primaire, secondaire si nécessaire
- **Dry Hopping** : Timing et durée optimaux
- **Transferts** : Planification selon équipement

**Conditionnement**
- **Carbonatation** : Calcul CO₂ selon style (volumes/g/L)
- **Priming** : Sucre fermentescible pour refermentation
- **Force Carb** : CO₂ direct si équipement disponible
- **Maturation** : Durée recommandée avant dégustation

### Calculs de Brassage

**Densités (Gravity)**
- **OG** : Densité initiale = Σ(Extraction malt × PPG) / Volume
- **FG** : Densité finale = OG × (1 - Atténuation levure)
- **Efficiency** : Facteur équipement (65-85% typique)

**Amertume (IBU)**
- **Formule Tinseth** : IBU = (α% × Poids × Utilisation × 0.749) / Volume
- **Facteur Utilisation** : Temps ébullition, densité moût
- **Cohumulone** : Ajustement qualité amertume

**Couleur (SRM/EBC)**
- **Formule Morey** : SRM = 1.49 × (MCU^0.69)
- **MCU** : Malt Color Units = Σ(°L malt × Poids) / Volume
- **Conversion** : EBC = SRM × 1.97

**Alcool (ABV)**
- **Formule standard** : ABV = (OG - FG) × 131.25
- **Formule précise** : ABV = (76.08 × (OG-FG) / (1.775-OG)) × (FG/0.794)

### Profils d'Équipement

**MiniBrew** (Système automatisé)
- **Volumes** : 5L standard
- **Efficacité** : 72% typique
- **Évaporation** : 12% par heure
- **Timing** : Automatisé, moins de flexibilité
- **Température** : Contrôle précis ±0.5°C

**Équipement Traditionnel**
- **Volumes** : 10L, 20L, 40L standards
- **Efficacité** : Variable (65-85%)
- **Évaporation** : 10-15% selon configuration
- **Flexibilité** : Contrôle total timing/température
- **Skills** : Expertise brasseur importante

**Professionnel/Commercial**
- **Volumes** : 100L, 500L, 1000L+
- **Efficacité** : Optimisée (80-90%)
- **Contrôle** : Automatisation complète
- **Scaling** : Problématiques spécifiques (extraction, IBU)

---

## 📊 Données Prévues

### Recettes Templates

**Styles Populaires**
- **American IPA** : Cascade/Centennial, American Ale yeast
- **German Pilsner** : Pilsner malt, Saaz, Lager yeast
- **Irish Stout** : Roasted barley, East Kent Goldings
- **Belgian Witbier** : Wheat, Coriander, Orange peel
- **English Bitter** : Maris Otter, English hops, London yeast

**Recettes Débutants**
- **Kit Basis** : Ingrédients minimal, procédures simplifiées
- **Extract** : Extrait malt liquide, steeping speciality
- **Mini Mash** : Introduction empâtage partiel
- **All Grain** : Progression vers tout grain

### Métadonnées Calculées

**Caractéristiques Principales**
- **ABV** : 3.5-12%+ selon style
- **IBU** : 10-100+ selon amertume cible
- **SRM** : 2-40+ selon couleur style
- **OG/FG** : Densités avec tolérance style

**Profils Sensoriels**
- **Balance** : Malt/Houblon selon IBU/OG ratio
- **Body** : Light/Medium/Full selon FG et composition
- **Dryness** : Atténuation et malts résiduels
- **Complexity** : Nombre ingrédients, procédures spéciales

---

## 🎯 Business Rules Prévues

### Validation Recettes

**Composition Coherence**
- **Malts de base** : ≥60% facture (sauf styles spéciaux)
- **Houblons** : Minimum 1 addition, timing cohérent
- **Levure** : Compatible avec style et température fermentation
- **Adjuvants** : Limites selon style BJCP

**Style Compliance**
- **Ranges BJCP** : ABV, IBU, SRM dans tolérances
- **Ingrédients traditionnels** : Flagging si atypiques
- **Procédures** : Cohérentes avec style historique
- **Balance** : Ratios malt/houblon appropriés

### Calculs et Optimisations

**Ajustements Automatiques**
- **Efficiency** : Selon historique équipement utilisateur
- **Évaporation** : Calcul volume final précis
- **Hop Utilization** : Facteurs densité, temps, forme
- **Atténuation** : Prédiction selon levure et composition

**Recommandations IA**
- **Substitutions** : Si ingrédients indisponibles
- **Amélioration équilibre** : Suggestions optimisation
- **Timing** : Optimisation selon équipement
- **Troubleshooting** : Diagnostic problèmes potentiels

### Scaling et Adaptation

**Conservation Ratios**
- **Proportions ingrédients** : Scaling linéaire avec ajustements
- **IBU scaling** : Compensation utilisation selon volume
- **Couleur scaling** : Ajustements extraction différentielle
- **Timing** : Adaptation durées selon volume

**Optimisation Équipement**
- **Efficacité** : Ajustement selon profil matériel
- **Contraintes** : Volumes min/max, températures
- **Automatisation** : Adaptation niveau contrôle disponible
- **Coûts** : Estimation selon ingrédients et volumes