# Domaine MALTS - Descriptif Fonctionnel

## 🎯 Vue d'ensemble du domaine

Le domaine Malts centralise les connaissances sur les malts et céréales utilisés en brassage. Il forme la base glucidique des recettes de bière, gérant les caractéristiques de couleur, d'extraction et de pouvoir enzymatique essentielles aux calculs de brassage.

**Statut** : ✅ **TERMINÉ - Production Ready**

---

## 🚀 Fonctionnalités

### Gestion Administrative
- **CRUD complet** aligné sur le pattern du domaine Hops
- **API Admin** (`/api/admin/malts`) avec sécurisation
- **Event Sourcing** et versioning automatique
- **Validation métier** spécialisée (EBC, extraction, pouvoir diastasique)
- **Audit trail** des modifications

### Interface Publique
- **API Publique** (`/api/v1/malts`) optimisée
- **Recherche par type** (`/api/v1/malts/type/{type}`)
- **Filtrage avancé** par caractéristiques techniques
- **Format JSON uniforme** avec les autres domaines
- **Performance** ~31ms en moyenne

### Gestion des Types
- **Classification hiérarchique** Base → Specialty → Adjunct
- **Validation croisée** type/couleur/extraction
- **Recommandations d'usage** par style de bière
- **Substitutions** entre malts compatibles

---

## 🌾 Types et Éléments Métier

### Types Principaux

**`BASE`** - Malts de Base
- **Usage** : 80% à 100% de la facture maltée
- **Caractéristiques** : Pouvoir diastasique élevé (100+ Lintner)
- **Extraction** : 80-85% (rendement sucres fermentescibles)
- **Couleur** : 2-8 EBC (très pâle à ambrée)
- **Exemples** : Pilsner Malt, Pale Ale Malt

**`SPECIALTY`** - Malts Spéciaux
- **Usage** : 5% à 30% de la facture maltée
- **Rôle** : Couleur, saveur, corps, mousse
- **Extraction** : 70-82% (variable selon transformation)
- **Couleur** : 15-1000+ EBC (ambrée à noire)
- **Exemples** : Munich, Crystal, Chocolate

**`ADJUNCT`** - Adjuvants
- **Usage** : 5% à 20% maximum
- **Rôle** : Texture, légèreté, caractères spécifiques
- **Nécessité** : Doit être converti par des enzymes de malts de base
- **Exemples** : Avoine, Blé, Riz, Maïs

### Caractéristiques Techniques

**Couleur EBC** (European Brewery Convention)
- **2-4 EBC** : Pilsner (très pâle)
- **5-8 EBC** : Pale Ale (pâle)
- **15-25 EBC** : Munich (ambrée)
- **50-80 EBC** : Crystal (caramel)
- **200-500 EBC** : Chocolate (brune)
- **1000+ EBC** : Roasted Barley (noire)

**Potentiel d'Extraction** (75% - 85%)
- Pourcentage de matière sèche extractible
- Base du calcul de densité initiale (OG)
- Impacte le rendement brasserie
- Varie selon maltage et transformation

**Pouvoir Diastasique** (0 - 150+ Lintner)
- Capacité enzymatique de conversion amidon → sucres
- **>100 Lintner** : Peut convertir des adjuvants
- **50-100 Lintner** : Auto-conversion seulement
- **<50 Lintner** : Pas d'activité enzymatique

### Grades et Variétés

**Malts de Base**
- **Pilsner Malt** : Base universelle, très pâle (2-4 EBC)
- **Pale Ale Malt** : Plus de corps que Pilsner (5-8 EBC)
- **Munich Malt** : Saveurs maltées prononcées (15-25 EBC)
- **Vienna Malt** : Caractère toasté léger (8-12 EBC)

**Malts Crystal/Caramel**
- **Crystal 40** : Caramel léger, sucré (80 EBC)
- **Crystal 60** : Caramel médium (118 EBC)
- **Crystal 120** : Caramel foncé, raisins secs (236 EBC)

**Malts Torréfiés**
- **Chocolate Malt** : Saveurs chocolat, café (900 EBC)
- **Roasted Barley** : Amertume, couleur noire (1000+ EBC)
- **Black Patent** : Colorant, astringent (1200+ EBC)

### Origines et Malteries

**Allemagne**
- **Weyermann** : Leader mondial, innovations
- **Best Malz** : Malts biologiques, spécialités
- **Château** : Malts belges haute qualité

**Royaume-Uni**
- **Crisp Malting** : Tradition anglaise
- **Muntons** : Malts pour ale traditionnelles
- **Simpson's** : Malts torréfiés artisanaux

**France**
- **Château Malting** : Malts premium européens
- **Soufflet** : Groupe industriel français

**Autres Origines**
- **Canada** : Rahr, Red Shed Malting
- **Australie** : Joe White Maltings
- **Nouvelle-Zélande** : Gladfield Malt

---

## 📊 Données Actuelles

### Malts en Base (3 variétés actives)

1. **Pilsner Malt** (Allemagne) - Base
   - **EBC** : 3.5 | **Extraction** : 82% | **Diastasique** : 105 Lintner
   - **Usage** : Base universelle, très pâle
   - **Styles** : Pils, Lager, Weissbier

2. **Pale Ale Malt** (Royaume-Uni) - Base  
   - **EBC** : 5-8 | **Extraction** : 81% | **Diastasique** : 95 Lintner
   - **Usage** : Ales britanniques, plus de corps
   - **Styles** : Pale Ale, IPA, Bitter

3. **Munich Malt** (Allemagne) - Specialty
   - **EBC** : 15-25 | **Extraction** : 78% | **Diastasique** : 80 Lintner
   - **Usage** : 10-60%, saveurs maltées
   - **Styles** : Märzen, Oktoberfest, Bock

### Métadonnées Techniques

**Calculs de Brassage**
- **Densité théorique** = (Extraction × Poids) / Volume
- **Couleur finale** = Σ(EBC malt × Poids) / Volume total
- **Besoin enzymatique** = Σ(Adjuvants) ÷ Pouvoir diastasique total

**Ratios Recommandés**
- **Malt de base** : 80-100% de la facture
- **Malts crystal** : 5-15% maximum
- **Malts torréfiés** : 1-5% pour couleur/saveur
- **Adjuvants** : 10-20% maximum

---

## 🎯 Business Rules

### Validation des Données

**Cohérence Type/Couleur**
- Malt `BASE` : EBC ≤ 25
- Malt `SPECIALTY` : EBC entre 15-500
- Malt `ADJUNCT` : Pas de contrainte couleur

**Cohérence Extraction/Type**
- Malts de base : Extraction ≥ 78%
- Malts spéciaux : Extraction variable (65-82%)
- Validation des plages par origine géographique

**Pouvoir Diastasique**
- Malts de base : ≥ 50 Lintner minimum
- Corrélation extraction/pouvoir enzymatique
- Validation contre les données littérature

### Règles Métier

**Classification Automatique**
- Auto-détection type selon couleur/extraction
- Flagging des incohérences pour review
- Suggestions de correction automatiques

**Compatibilité Styles**
- Malts de base → Tous styles compatibles
- Malts spéciaux → Styles spécifiques seulement
- Validation contraintes BJCP

**Substitutions**
- Malts de même type et couleur similaire
- Conservation du profil gustatif global
- Ajustement automatique des ratios

### Performance et Qualité

**Index de Qualité Automatique** (0-100)
- Cohérence des caractéristiques : 40%
- Complétude des métadonnées : 30%
- Validation croisée littérature : 20%
- Feedback utilisateur : 10%

**Optimisation Requêtes**
- Index composites (type, origine, couleur)
- Cache des malts fréquemment utilisés
- Pré-calcul des suggestions de substitution