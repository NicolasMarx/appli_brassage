# 🚀 PROMPT PARFAIT - INTÉGRATION PG-EVENT-STORE

**Objectif** : Migration complète vers pg-event-store avec 100% de fonctionnalité API  
**Contrainte** : Sauvegarde + migration Git + nouvelle branche  
**Critère de succès** : Compilation garantie + APIs fonctionnelles + TODOs résolus

---

## 📋 **PROMPT D'EXÉCUTION PARFAIT**

### **PHASE 1 : SAUVEGARDE ET PRÉPARATION GIT** ⏱️ 5min

```bash
# 1. Commit et sauvegarde état actuel
git add -A
git commit -m "💾 SAVE: État actuel avant migration pg-event-store

- APIs partiellement fonctionnelles (Hops ✅, Yeasts events only, Malts TODO)
- 82 fichiers avec TODOs architecture DDD/CQRS  
- Event Sourcing hybride (Yeasts/Recipes events, Hops/Malts CRUD)
- Base de données: 3 hops, 1 yeast event, tables complètes

🎯 Préparation migration vers pg-event-store unifié

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"

# 2. Merge vers main et création nouvelle branche
git checkout main
git merge feature/yeast-domain --no-ff -m "✅ MERGE: Yeasts domain avec Event Sourcing partiel"
git checkout -b feature/pg-event-store-integration
git push -u origin feature/pg-event-store-integration
```

### **PHASE 1.5 : MIGRATION JAVA 21** ⏱️ 30min-1h

```bash
# 1. Installation Java 21 LTS (si pas déjà installé)
# Via SDKMAN (recommandé)
sdk install java 21.0.4-tem
sdk use java 21.0.4-tem

# Ou via Homebrew
# brew install openjdk@21
# export JAVA_HOME="/opt/homebrew/opt/openjdk@21"

# 2. Vérification version
java -version  # Doit afficher Java 21

# 3. Configuration projet pour Java 21
```

```scala
// build.sbt - Ajouter après ThisBuild / scalaVersion
ThisBuild / javacOptions ++= Seq(
  "--enable-native-access=ALL-UNNAMED"  // Fix JNA warnings Java 21
)

// Optimisations Java 21 (optionnel)
ThisBuild / javaOptions ++= Seq(
  "-XX:+UseG1GC",
  "-XX:+UseStringDeduplication"
)
```

```bash
# 4. Test compilation avec Java 21
sbt clean compile
# Si succès : commit immédiatement

# 5. Commit migration Java 21
git add -A
git commit -m "⬆️ UPGRADE: Migration vers Java 21 LTS

- Java 17 → Java 21 LTS (support jusqu'en 2028)
- Configuration JVM optimisée pour Event Sourcing
- Correction warnings JNA/Unsafe avec --enable-native-access
- Base technique optimale pour pg-event-store

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"
```

### **PHASE 2 : VALIDATION ET CORRECTION PRÉ-MIGRATION** ⏱️ 15-30min

```bash
# 1. Test de compilation état actuel
sbt clean compile
# Si échec : corriger erreurs de compilation immédiatement

# 2. Test APIs actuelles fonctionnelles
sbt run &
sleep 10
curl -f http://localhost:9000/api/v1/hops || echo "❌ Hops API failed"
curl -f http://localhost:9000/api/v1/malts || echo "⚠️ Malts API empty (expected)"
curl -f http://localhost:9000/api/v1/yeasts || echo "⚠️ Yeasts API empty (projection issue)"

# 3. Commit correction si nécessaire
git add -A
git commit -m "🔧 PRE-MIGRATION: Corrections compilation et stabilisation

- Résolution erreurs compilation avant pg-event-store
- Validation APIs fonctionnelles en état baseline
- Base stable pour migration Event Sourcing

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"
```

### **PHASE 3 : AJOUT DÉPENDANCE PG-EVENT-STORE** ⏱️ 10min

```scala
// build.sbt - Ajouter dépendance
libraryDependencies ++= Seq(
  "com.performanceimmo" %% "pg-event-store-postgres" % "0.0.19",
  "com.performanceimmo" %% "pg-event-store-play-json" % "0.0.19"
)
```

### **PHASE 4 : ARCHITECTURE UNIFIÉE** ⏱️ 2-3h

#### **4.1 Configuration Event Store Global**
```scala
// app/infrastructure/eventstore/EventStoreModule.scala
@Module
class EventStoreModule extends AbstractModule {
  @Provides @Singleton
  def provideEventRepository(dbConfig: DatabaseConfigProvider): EventRepository[Future] = {
    new PostgresEventRepository(dbConfig.get[JdbcProfile].db)
  }
}
```

#### **4.2 Migration par Domaine (Ordre optimisé)**

**A. HOPS (CRUD → Event Sourcing)** 
```scala
// Créer : domain/hops/events/HopEvents.scala
// Créer : infrastructure/eventstore/hops/HopEventStore.scala  
// Migrer : HopService → Event Sourcing + projections
// Garder : APIs existantes (compatibilité)
```

**B. MALTS (TODO → Event Sourcing complet)**
```scala
// Implémenter : 82 fichiers TODO architecture
// Créer : domain/malts/events/MaltEvents.scala
// Implémenter : AdminMaltsController CRUD complet
// Résultat : APIs admin malts 100% fonctionnelles
```

**C. YEASTS (Event Sourcing custom → pg-event-store)**
```scala
// Migrer : SlickYeastWriteRepository → pg-event-store
// Implémenter : Projections automatiques  
// Résultat : APIs yeasts publiques fonctionnelles
```

**D. RECIPES (Event Sourcing custom → pg-event-store)**
```scala
// Créer : table recipe_events manquante
// Migrer : vers pg-event-store unifié
// Corriger : endpoints retournant erreurs
```

### **PHASE 5 : RÉSOLUTION EXHAUSTIVE TODOs** ⏱️ 4-5h

#### **5.1 Modules d'Architecture (82 fichiers)**
```scala
// Pattern de résolution systématique :
// 1. CoreModule.scala → Injection dépendances DDD
// 2. SecurityModule.scala → Auth/Authorization production  
// 3. ApplicationModule.scala → Command/Query handlers
// 4. Tous les ValueObjects shared → Implémentations complètes
```

#### **5.2 Fonctionnalités Manquantes Critiques**
```scala
// YeastAdminController.scala:156 - Batch create
// YeastAdminController.scala:168 - Export functionality  
// MaltPublicController.scala:240 - Search suggestions
// RecipePublicController.scala:51 - Recipe facets
// AdminMaltsController.scala:29-41 - CRUD complet
```

### **PHASE 6 : TESTS ET VALIDATION** ⏱️ 1-2h

```bash
# Compilation guaranteed
sbt clean compile

# Tests exhaustifs
sbt test

# Vérification architecture
sbt verifyArchitecture

# Tests APIs end-to-end
curl -X GET http://localhost:9000/api/v1/hops    # ✅ Doit retourner données
curl -X GET http://localhost:9000/api/v1/malts   # ✅ Doit retourner données  
curl -X GET http://localhost:9000/api/v1/yeasts  # ✅ Doit retourner données
curl -X GET http://localhost:9000/api/v1/recipes # ✅ Doit retourner données
```

---

## 🎯 **PROMPT PARFAIT D'EXÉCUTION**

**À copier-coller pour exécution complète :**

---

**MISSION** : Intégrer pg-event-store dans le projet brewing pour une architecture Event Sourcing unifiée et des APIs 100% fonctionnelles.

**CONTRAINTES ABSOLUES** :
1. **Sauvegarde complète** : Commit état actuel → merge main → nouvelle branche
2. **Compilation garantie** : 100% de probabilité de succès du premier coup
3. **Non-destructif** : Préserver données existantes et fonctionnalités
4. **TODOs épuisés** : Résoudre les 82 fichiers d'architecture TODO

**OBJECTIFS BASECONNAISSANCE RESPECTÉS** :
- ✅ Intelligence équivalente Hops/Malts/Yeasts (recommandations, substitutions)
- ✅ Architecture DDD/CQRS pure avec Event Sourcing unifié
- ✅ APIs production-ready avec authentification, validation, monitoring
- ✅ Performance optimisée avec cache, index, pagination

**ÉTAPES D'EXÉCUTION** :

1. **GIT WORKFLOW** :
   - Commit + tag état actuel "pre-pg-event-store"  
   - Merge feature/yeast-domain → main
   - Créer branche feature/pg-event-store-integration

2. **ARCHITECTURE TRANSFORMATION** :
   - Ajouter dépendance pg-event-store dans build.sbt
   - Créer EventStoreModule unifié pour tous les domaines
   - Migrer Hops CRUD → Event Sourcing (préserver APIs)
   - Finaliser Malts Event Sourcing (implémenter tous TODOs)
   - Migrer Yeasts custom → pg-event-store (projections auto)
   - Compléter Recipes Event Sourcing (créer table events)

3. **IMPLÉMENTATION EXHAUSTIVE** :
   - Résoudre 82 fichiers TODO architecture DDD/CQRS
   - Implémenter ValueObjects shared manquants
   - Finaliser Controllers admin (Malts CRUD, Yeasts batch/export)
   - Implémenter fonctionnalités avancées (search suggestions, facets)

4. **VALIDATION FINALE** :
   - Compilation sans erreur : `sbt clean compile`
   - Tests passent : `sbt test` 
   - APIs 100% fonctionnelles : Test end-to-end tous endpoints
   - Architecture cohérente : `sbt verifyArchitecture`

**CRITÈRES DE SUCCÈS VALIDATION** :
- [ ] Toutes APIs retournent données réelles (pas d'empty arrays)
- [ ] Admin CRUD complet sur tous domaines (create/read/update/delete)
- [ ] Event Sourcing unifié avec pg-event-store sur 4 domaines  
- [ ] Zéro TODO dans le codebase app/
- [ ] Intelligence équivalente Hops=Malts=Yeasts (recommandations)
- [ ] Compilation du premier coup garantie

**LIVRABLE ATTENDU** : Projet brewing avec Event Sourcing professionnel via pg-event-store, APIs complètement fonctionnelles, architecture DDD/CQRS finalisée, 0 TODO technique.

---

**⚡ CONSEIL D'EXÉCUTION** : Procéder domaine par domaine (Hops→Malts→Yeasts→Recipes) pour validation incrémentale. Tester compilation + APIs après chaque domaine migré.

---

## 📈 **ESTIMATION EFFORT TOTAL**

- **Phase 1** : 5 minutes (Git workflow)
- **Phase 1.5** : 30min-1h (Migration Java 21)
- **Phase 2** : 15-30min (Validation pré-migration)
- **Phase 3** : 10 minutes (Dépendance pg-event-store)  
- **Phase 4** : 2-3 heures (Architecture unifiée)
- **Phase 5** : 4-5 heures (Résolution TODOs)
- **Phase 6** : 1-2 heures (Tests et validation)

**TOTAL** : 7-10 heures pour une transformation complète et professionnelle.

---

## 🎯 **RÉSULTAT GARANTI**

À la fin de cette migration :
- ✅ **Event Sourcing unifié** avec pg-event-store professionnel
- ✅ **APIs 100% fonctionnelles** avec données réelles
- ✅ **Architecture DDD/CQRS complète** sans TODOs
- ✅ **Intelligence équivalente** sur tous domaines ingrédients
- ✅ **Production-ready** avec monitoring, auth, validation

**Prêt pour mise en production immédiate.**