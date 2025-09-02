# Domaine HOPS (Houblons) - Descriptif Fonctionnel

## 🎯 Vue d'ensemble du domaine

Le domaine Hops gère l'ensemble des connaissances liées aux houblons utilisés dans le brassage. Il constitue l'un des piliers fondamentaux de la plateforme, offrant une base de données exhaustive des variétés de houblons avec leurs caractéristiques chimiques, aromatiques et d'usage.

**Statut** : ✅ **TERMINÉ - Production Ready**

---

## 🚀 Fonctionnalités

### Gestion Administrative
- **CRUD complet** avec Event Sourcing et versioning automatique
- **API Admin sécurisée** (`/api/admin/hops`) avec authentification
- **Validation métier** robuste (cohérence des données chimiques)
- **Audit trail** complet des modifications
- **Permissions granulaires** par rôle administrateur

### Interface Publique
- **API Publique optimisée** (`/api/v1/hops`) en lecture seule
- **Recherche avancée** multi-critères avec pagination
- **Performance garantie** < 100ms
- **Format JSON normalisé** pour intégrations tierces

### Recherche et Filtrage
- **Recherche textuelle** sur nom, description, arômes
- **Filtres par caractéristiques** : type, origine, taux d'acides
- **Recherche par style** de bière compatible
- **Système de substitution** : houblons alternatifs recommandés

---

## 🧬 Types et Éléments Métier

### Types de Houblons

**`DUAL_PURPOSE`** - Double Usage
- Usage : Amérisant ET aromatique
- Exemples : Amarillo, Azacca, Ariana
- Caractéristique : Équilibre acides alpha/profil aromatique

**`BITTERING`** - Amérisant
- Usage : Principalement pour l'amertume
- Acides alpha élevés (>10%)
- Ajout en début d'ébullition

**`AROMA`** - Aromatique
- Usage : Parfum et saveur
- Ajout en fin d'ébullition ou à froid (dry hopping)
- Exemples : Aramis, Bobek

### Caractéristiques Chimiques

**Acides Alpha** (3.5% - 14.5%)
- Responsables de l'amertume
- Transformation en iso-alpha-acides lors de l'ébullition
- Valeur clé pour le calcul des IBU

**Acides Beta** (3% - 7%)
- Contribuent à la conservation naturelle
- Oxydation → composés amers différents
- Important pour la stabilité de la bière

**Cohumulone** (21% - 42%)
- Fraction des acides alpha
- Impact sur la qualité de l'amertume
- Faible taux = amertume plus douce

**Huiles Essentielles** (0.7 - 3.0 ml/100g)
- **Myrcène** : 35-70% - Arôme herbacé, résineux
- **Humulène** : 9-25% - Arôme noble, épicé
- **Caryophyllène** : 2-14% - Notes poivrées, épicées
- **Farnésène** : 0.1-7% - Arômes floraux, fruités

### Profils Aromatiques

**Agrumes**
- Notes de citron, pamplemousse, orange
- Houblons : Amarillo, Ariana, Bobek

**Floraux**
- Parfums de fleurs, roses, jasmin
- Caractéristiques des houblons nobles européens

**Fruités**
- Fruits tropicaux, fruits à noyau
- Expressions modernes des houblons américains/océaniens

**Épicés**
- Poivre, coriandre, herbes
- Profil traditionnel des houblons continentaux

**Résineux/Pin**
- Notes de résine, pin, eucalyptus
- Signature des houblons de la côte ouest américaine

### Origines Géographiques

**Houblons Nobles** (Tradition européenne)
- **Allemagne** : Hallertau, Spalt, Tettnang
- **République Tchèque** : Saaz (Žatec)
- Caractéristiques : Faible alpha, arômes fins et délicats

**Nouveau Monde** (Innovation moderne)
- **USA** : Cascade, Amarillo, Azacca
- **Nouvelle-Zélande** : Nelson Sauvin, Motueka
- **Australie** : Galaxy, Ella
- Caractéristiques : Alpha élevé, arômes intenses et originaux

**Européens Modernes**
- **France** : Aramis, Barbe Rouge
- **Angleterre** : Challenger, Goldings
- **Slovénie** : Bobek, Celeia
- Caractéristiques : Fusion tradition/innovation

---

## 📊 Données Actuelles

### Houblons en Base (8 variétés actives)

1. **Amarillo** (USA) - Dual Purpose
   - Alpha : 6-11% | Beta : 6-7%
   - Arômes : Floral, Agrume, Fruits tropicaux

2. **Aramis** (France) - Arôme
   - Alpha : 6.5-8.5% | Beta : 3.5-5.5%
   - Arômes : Agrume, Épicé

3. **Ariana** (Allemagne) - Dual Purpose
   - Alpha : 8-14.5% | Beta : 3.7-6.6%
   - Arômes : Fruits tropicaux, Pêche

4. **Azacca** (USA) - Dual Purpose
   - Alpha : 10-14% | Beta : 4-5.5%
   - Arômes : Agrume, Pin, Fruits tropicaux

5. **Barbe Rouge** (France) - Arôme
   - Alpha : 7.5-9.5% | Beta : 3-3.8%
   - Arômes : Fruité, Baies rouges, Cerise

6. **Bobek** (Slovénie) - Arôme
   - Alpha : 3.5-7.8% | Beta : 4-6.1%
   - Arômes : Floral, Agrume, Pamplemousse

### Métadonnées Associées

**Styles de Bière Compatibles**
- Pale Ale Américaine, IPA
- Pils, Saison, Bière blanche
- Porter, Altbier
- Triple, Blonde

**Système de Substitution**
- Amarillo → Cascade, Centennial, Chinook, Simcoe
- Aramis → Willamette, Challenger, Strisselspalt
- Ariana → Mandarina Bavaria, Huell Melon

**Historique et Audit**
- Versioning automatique à chaque modification
- Traçabilité complète des changements
- Timestamps création/modification
- Identification des administrateurs ayant effectué les modifications

---

## 🎯 Business Rules

### Validation des Données
- **Acides Alpha** : Valeur min < max, plages cohérentes par type
- **Cohumulone** : Pourcentage des acides alpha, cohérence chimique
- **Huiles essentielles** : Somme des composants = 100%
- **Origine** : Validation contre le référentiel géographique

### Règles Métier
- **Houblon Noble** : Alpha < 6%, origine européenne traditionnelle
- **Dual Purpose** : Alpha 6-12%, profil aromatique équilibré
- **Bittering** : Alpha > 10%, usage prioritaire amertume
- **Substitution** : Compatibilité de profils aromatiques et usage

### Performance
- **Cache** : Données fréquemment accédées en mémoire
- **Index** : Sur nom, origine, type, statut actif
- **Pagination** : Limite 50 items par requête par défaut
- **Response Time** : < 100ms garanti pour l'API publique