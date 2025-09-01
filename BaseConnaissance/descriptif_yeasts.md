# Domaine YEASTS (Levures) - Descriptif Fonctionnel

## 🎯 Vue d'ensemble du domaine

Le domaine Yeasts gère l'écosystème complexe des levures de brassage, micro-organismes responsables de la fermentation alcoolique. Il centralise les souches commerciales avec leurs caractéristiques de fermentation, profils gustatifs et compatibilités avec les styles de bière.

**Statut** : 🔜 **À IMPLÉMENTER - Planifié Phase 2B**

---

## 🚀 Fonctionnalités Prévues

### Gestion des Souches
- **CRUD complet** suivant le pattern des domaines existants
- **Gestion des laboratoires** (White Labs, Wyeast, Lallemand, Fermentis)
- **Traçabilité des lots** et disponibilité saisonnière
- **Compatibilité équipement** (fermenteur, température contrôlée)

### Caractérisation Technique
- **Profils de fermentation** (atténuation, température, durée)
- **Tolérance alcoolique** et conditions optimales
- **Floculation** et clarification naturelle
- **Viabilité** et conditions de stockage

### Recommandations Intelligentes
- **Compatibility style** automatique selon BJCP
- **Substitutions de levures** par caractéristiques similaires
- **Optimisation conditions** selon équipement utilisateur
- **Prédiction résultats** fermentation

---

## 🦠 Types et Éléments Métier

### Types de Levures

**`ALE`** - Saccharomyces cerevisiae
- **Fermentation haute** : 18-24°C
- **Durée** : 3-7 jours fermentation primaire
- **Profil** : Esters fruités, phénols épicés possibles
- **Styles** : Pale Ale, IPA, Stout, Wheat Beer
- **Atténuation** : 68-80% typique

**`LAGER`** - Saccharomyces pastorianus
- **Fermentation basse** : 8-14°C
- **Durée** : 7-14 jours + lagering 4-8 semaines
- **Profil** : Propre, neutre, mise en valeur du malt/houblon
- **Styles** : Pilsner, Märzen, Bock, Schwarzbier
- **Atténuation** : 70-85% typique

**`WILD`** - Levures sauvages
- **Brettanomyces** : Fermentation longue, arômes complexes
- **Levures indigènes** : Terroir spécifique
- **Fermentation mixte** : Association avec bactéries lactiques
- **Styles** : Lambic, Saison farmhouse, Sour ales

**`SPECIALTY`** - Levures spécialisées
- **Kveik** : Levures norvégiennes haute température (25-40°C)
- **Champagne** : Haute tolérance alcoolique (12-18%)
- **Weizen** : Spécialisées blé, production d'esters spécifiques

### Caractéristiques de Fermentation

**Atténuation** (65% - 90%)
- Pourcentage de sucres fermentés
- **Faible** (65-75%) : Corps résiduel, douceur
- **Moyenne** (75-82%) : Équilibre standard
- **Élevée** (82-90%) : Bière sèche, mise en valeur houblon

**Température de Fermentation**
- **Ale basse** : 16-20°C (profil propre)
- **Ale standard** : 18-22°C (équilibre esters/propre)
- **Ale haute** : 22-26°C (esters prononcés)
- **Lager** : 8-14°C (fermentation lente et propre)
- **Kveik** : 25-40°C (fermentation ultra-rapide)

**Tolérance Alcoolique**
- **Standard** : 8-10% ABV
- **Haute** : 12-15% ABV (Imperial styles)
- **Extrême** : 15-20% ABV (Barleywine, Belgian Strong)

**Niveau de Floculation**
- **Low** : Reste en suspension, filtration nécessaire
- **Medium** : Équilibre, clarification progressive
- **High** : Sédimentation rapide, bière claire naturellement

### Laboratoires et Souches

**White Labs (WLP)**
- **WLP001** - California Ale : Neutre, polyvalente
- **WLP002** - English Ale : Légèrement fruitée, floculation haute
- **WLP090** - San Diego Super : Atténuation élevée, IPA
- **WLP830** - German Lager : Lager propre classique

**Wyeast**
- **1056** - American Ale : Équivalent WLP001
- **1968** - London ESB : Ale anglaise traditionnelle
- **2124** - Bohemian Lager : Pilsner tchèque authentique
- **3787** - Trappist High Gravity : Ales belges fortes

**Lallemand (sèche)**
- **US-05** : Équivalent 1056 en levure sèche
- **S-04** : Ale anglaise, floculation rapide
- **W-34/70** : Lager universelle
- **Belle Saison** : Saison, haute température

**Fermentis (sèche)**
- **SafSpirit** : Distillation, haute tolérance alcool
- **SafAle** : Gamme ales diverses
- **SafLager** : Lagers, fermentation contrôlée

### Profils Gustatifs

**Esters (Composés fruités)**
- **Acétate d'éthyle** : Solvant, pomme verte (défaut si excessif)
- **Acétate d'isoamyle** : Banane (typique Weizen)
- **Butyrate d'éthyle** : Ananas, fruits tropicaux
- **Caproate d'éthyle** : Pomme, fruits à pépins

**Phénols (Composés épicés)**
- **4-vinyl-guaiacol** : Clou de girofle (Weizen)
- **4-vinyl-phenol** : Fumée, médical (défaut si excessif)
- **POF+** : Production phenol possible
- **POF-** : Pas de production phenol

**Caractères spécifiques**
- **Brett** : Funk, cuir, fruits rouges, acidité
- **Kveik** : Fruits tropicaux intenses, orange
- **Belge** : Épices, poivre, complexité

---

## 📊 Données Prévues (Structure)

### Souches Standard (25+ variétés planifiées)

**Ales Classiques**
- California Ale, English Ale, American West Coast
- Weizen, Belgian Abbey, Saison
- Irish Stout, Scottish Ale

**Lagers Standards**
- Bohemian Pilsner, German Lager, American Lager
- Vienna, Märzen, Bock specialty

**Souches Spécialisées**  
- Brett varieties, Kveik strains
- High-gravity specialists
- Sour/Wild fermentation

### Métadonnées Techniques

**Conditions Optimales**
- Température fermentation primaire/secondaire
- pH optimal et tolérance
- Nutriments requis (YAN, minéraux)
- Oxygénation recommandée

**Performance Prévue**
- Cinétique de fermentation (courbe atténuation)
- Production de sous-produits
- Besoin en brassage (vitesse, agitation)
- Compatibilité équipements (conique, cylindro-conique)

**Disponibilité**
- Formes disponibles (liquide/sèche)
- Saisonnalité production
- Prix indicatifs laboratoires
- Equivalences entre laboratoires

---

## 🎯 Business Rules Prévues

### Validation des Souches

**Cohérence Caractéristiques**
- Température fermentation vs type (Ale/Lager)
- Atténuation vs tolérance alcoolique
- Floculation vs clarification naturelle
- Profil gustatif vs styles recommandés

**Compatibilité Style**
- Validation BJCP automatique
- Flagging des associations atypiques
- Recommandations optimisation profil

### Recommandations Intelligentes

**Substitution de Levures**
- Proximité caractéristiques fermentation
- Équivalence profil gustatif
- Disponibilité et forme (liquide/sèche)
- Compatibilité équipement utilisateur

**Optimisation Conditions**
- Température selon style cible
- Durée fermentation équipement
- Gestion de la floculation
- Prédiction densité finale

### Gestion des Stocks

**Viabilité et Dates**
- Calcul viabilité selon date production
- Alertes péremption stocks
- Recommandations starter selon viabilité
- Planification commandes laboratoires

**Équivalences Laboratoires**
- Mapping souches équivalentes
- Comparaison prix/performance
- Recommandations selon disponibilité locale
- Historique performances utilisateur