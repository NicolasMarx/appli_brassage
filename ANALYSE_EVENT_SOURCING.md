# 🔄 ANALYSE EVENT SOURCING - PG-EVENT-STORE vs ARCHITECTURE ACTUELLE

**Date d'analyse** : 2 septembre 2025  
**Projet** : My-brew-app-V2  
**Contexte** : Incohérence architecturale Event Sourcing vs CRUD

---

## 📊 **ÉTAT ACTUEL - ARCHITECTURE HYBRIDE**

### 🔍 **DOMAINES ANALYSÉS**

| **Domaine** | **Pattern** | **Tables** | **Status** | **Problèmes** |
|-------------|-------------|------------|------------|---------------|
| **🧬 Yeasts** | Event Sourcing | `yeasts` + `yeast_events` | ⚠️ Partiel | Events sauvés, pas de projection |
| **📝 Recipes** | Event Sourcing | `recipes` + `recipe_events` | ❌ Incomplet | Table events manquante |
| **🌿 Hops** | CRUD Direct | `hops` seulement | ✅ Fonctionnel | Pas d'Event Sourcing |
| **🌾 Malts** | CRUD Direct | `malts` seulement | ❌ Admin TODO | Pas d'Event Sourcing |

### 🧬 **YEASTS - EVENT SOURCING ACTUEL**

**Code existant** :
```scala
// SlickYeastWriteRepository.scala:82
override def saveEvents(yeastId: YeastId, events: List[YeastEvent], expectedVersion: Long): Future[Unit] = {
  val eventRows = events.map(YeastEventRow.fromEvent)
  val action = yeastEvents ++= eventRows
  db.run(action).map(_ => ())
}
```

**Table créée** :
```sql
CREATE TABLE yeast_events (
  id VARCHAR PRIMARY KEY,
  yeast_id VARCHAR NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  event_data TEXT NOT NULL,
  version INTEGER NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  created_by VARCHAR
);
```

**⚠️ PROBLÈME CRITIQUE** : **Pas de projection automatique events → yeasts table**

---

## 🌟 **PATTERNS EVENT SOURCING POSTGRESQL - ANALYSE**

### 📋 **PATTERNS IDENTIFIÉS DANS L'ÉCOSYSTÈME**

#### **1. Structure Table Events Standard**
```sql
-- Pattern commun PostgreSQL Event Store
CREATE TABLE events (
    sequence_num BIGSERIAL NOT NULL,
    stream_id UUID NOT NULL,
    data JSONB NOT NULL,
    event_type TEXT NOT NULL,
    meta JSONB,
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (sequence_num)
);
CREATE INDEX idx_events_stream_id ON events (stream_id);
```

#### **2. Optimisations PostgreSQL Spécifiques**
- **JSONB** pour performance queries JSON
- **BIGSERIAL** pour ordre global des événements
- **TIMESTAMPTZ** pour gestion timezone
- **GIN indexes** pour requêtes JSON complexes

#### **3. Projections Automatiques**
```scala
// Pattern typique Event Sourcing mature
trait ProjectionHandler[E] {
  def handle(event: E, metadata: EventMetadata): Future[Unit]
  def rebuild(): Future[Unit]
}
```

### 🔄 **COMPARAISON AVEC VOTRE IMPLÉMENTATION**

| **Aspect** | **Votre Code** | **Pattern Standard** | **Recommandation** |
|------------|----------------|---------------------|-------------------|
| **Event Table** | `yeast_events` VARCHAR | `events` avec JSONB | ✅ Acceptable |
| **Projection** | ❌ Manquante | ✅ Automatique | 🚨 **CRITIQUE** |
| **Versioning** | ✅ Par aggregate | ✅ Global sequence | ✅ Bon |
| **JSON Storage** | TEXT | JSONB | ⚠️ Amélioration possible |
| **Metadata** | Minimal | Riche (causation, correlation) | ⚠️ À étendre |

---

## 🎯 **RECOMMANDATIONS TECHNIQUES**

### 🔴 **PRIORITÉ 1 - CORRECTION IMMÉDIATE**

#### **Option A : Implémenter Projections Manquantes**
```scala
// À créer : YeastProjectionService.scala
@Singleton
class YeastProjectionService @Inject()(
  eventRepository: YeastEventRepository,
  yeastRepository: YeastWriteRepository
) {
  
  def rebuildFromEvents(yeastId: YeastId): Future[Unit] = {
    for {
      events <- eventRepository.getEvents(yeastId)
      aggregate <- YeastAggregate.fromEvents(events)
      _ <- yeastRepository.saveSnapshot(aggregate)
    } yield ()
  }
  
  def handleEvent(event: YeastEvent): Future[Unit] = {
    // Projection temps réel
    rebuildFromEvents(event.yeastId)
  }
}
```

#### **Option B : Migration vers Pattern Standard**
```sql
-- Nouvelle table events unifiée
CREATE TABLE events (
  sequence_num BIGSERIAL PRIMARY KEY,
  stream_id TEXT NOT NULL,
  event_type TEXT NOT NULL,
  event_data JSONB NOT NULL,
  meta JSONB,
  occurred_at TIMESTAMPTZ DEFAULT now()
);
```

### 🟡 **PRIORITÉ 2 - UNIFORMISATION ARCHITECTURE**

#### **Choix Architectural Recommandé** :

**1. 🔄 Event Sourcing Complet (Recommandé)**
- Migrer Hops et Malts vers Event Sourcing
- Uniformité architecturale
- Auditabilité complète
- Projections pour performance

**2. 🗂️ CRUD Complet (Alternative)**
- Migrer Yeasts et Recipes vers CRUD
- Simplicité d'implémentation
- Performance directe
- Perte de l'historique

### 🟢 **PRIORITÉ 3 - OPTIMISATIONS POSTGRESQL**

```sql
-- Optimisations Event Store PostgreSQL
CREATE INDEX idx_events_stream_type ON events (stream_id, event_type);
CREATE INDEX idx_events_occurred_at ON events (occurred_at);
CREATE INDEX CONCURRENTLY idx_events_data_gin ON events USING gin(event_data);
```

---

## 📈 **PLAN D'ACTION DÉTAILLÉ**

### **Phase 1 : Correction Immédiate (2-3h)**
1. **Implémenter YeastProjectionService**
   - Rebuilder yeasts table depuis yeast_events
   - Projections automatiques sur nouveaux events

2. **Créer table recipe_events manquante**
   - Copier structure yeast_events
   - Tester création recettes

### **Phase 2 : Uniformisation (1-2 jours)**
3. **Migrer Hops vers Event Sourcing**
   - Créer hop_events table
   - Adapter HopService pour Event Sourcing

4. **Migrer Malts vers Event Sourcing**
   - Créer malt_events table  
   - Implémenter MaltEventRepository

### **Phase 3 : Optimisations (1 jour)**
5. **Migration vers JSONB**
   - Performance queries complexes
   - Index GIN pour recherche JSON

6. **Projections temps réel**
   - Event handlers automatiques
   - Monitoring consistance

---

## 🔧 **SOLUTION IMMÉDIATE RECOMMANDÉE**

Pour corriger le problème actuel des **yeasts invisibles** :

```scala
// Service de projection minimal à créer
@Singleton  
class YeastReadModelUpdater @Inject()(/* deps */) {
  
  def syncFromEvents(): Future[Unit] = {
    // 1. Lire tous les events yeast_events
    // 2. Reconstruire les aggregates
    // 3. Sauver dans table yeasts
    // 4. APIs publiques fonctionneront instantanément
  }
}
```

**Estimation** : **2-3 heures** pour corriger le problème principal et avoir des APIs 100% fonctionnelles.

---

## 💡 **CONCLUSION**

**Votre architecture actuelle est hybride et incomplète** :
- Event Sourcing bien implémenté côté write
- **Projections manquantes** côté read  
- **Incohérence entre domaines**

**Recommandation** : Commencer par **corriger les projections** puis **uniformiser** l'architecture selon vos préférences (Event Sourcing ou CRUD).