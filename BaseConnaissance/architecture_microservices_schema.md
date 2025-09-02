# Architecture Microservices - Brewing Platform

## 🏗️ Vue d'ensemble Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND LAYER                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   PUBLIC WEB    │  │   ADMIN WEB     │  │  MOBILE PWA     │              │
│  │   (Elm SPA)     │  │   (Elm SPA)     │  │   (Elm PWA)     │              │
│  │                 │  │                 │  │                 │              │
│  │ • Catalog       │  │ • CRUD Mgmt     │  │ • Recipe View   │              │
│  │ • Recipe Browse │  │ • Analytics     │  │ • Social Feed   │              │
│  │ • Community     │  │ • Users Mgmt    │  │ • Quick Post    │              │
│  │ • Social Feed   │  │ • Moderation    │  │ • Offline Mode  │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│                                   │                                         │
└───────────────────────────────────┼─────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼─────────────────────────────────────────┐
│                              API GATEWAY                                    │
├───────────────────────────────────┼─────────────────────────────────────────┤
│                                   │                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        NGINX + API GATEWAY                             │  │
│  │                                                                         │  │
│  │ • Authentication & Authorization (JWT)                                 │  │
│  │ • Rate Limiting & DDoS Protection                                      │  │
│  │ • Load Balancing & Circuit Breaker                                     │  │
│  │ • API Versioning (/v1, /v2)                                           │  │
│  │ • CORS & Security Headers                                             │  │
│  │ • Request/Response Logging                                            │  │
│  │ • SSL Termination                                                     │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼─────────────────────────────────────────┐
│                           MICROSERVICES LAYER                               │
├───────────────────────────────────┼─────────────────────────────────────────┤
│                                   │                                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │   CORE      │ │   RECIPE    │ │ COMMUNITY   │ │   SHARED    │           │
│  │  SERVICE    │ │  SERVICE    │ │  SERVICE    │ │ SERVICES    │           │
│  │             │ │             │ │             │ │             │           │
│  │Port: 9001   │ │Port: 9002   │ │Port: 9003   │ │             │           │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 🎯 Services Détaillés

### 🔧 **CORE SERVICE** (Port 9001)
*Service principal - Ingrédients et fondations*

```
┌─────────────────────────────────────────┐
│            CORE SERVICE                 │
│         (Scala + Play 2.8)             │
├─────────────────────────────────────────┤
│                                         │
│ DOMAINS:                                │
│ ├── 🌿 Hops Domain (✅ PROD)           │
│ │   ├── HopAggregate                   │
│ │   ├── CRUD APIs (/api/v1/hops)       │
│ │   └── Admin APIs (/api/admin/hops)   │
│ │                                       │
│ ├── 🌾 Malts Domain (✅ PROD)          │
│ │   ├── MaltAggregate                  │
│ │   ├── CRUD APIs (/api/v1/malts)      │
│ │   └── Admin APIs (/api/admin/malts)  │
│ │                                       │
│ ├── 🦠 Yeasts Domain (🔜 DEV)          │
│ │   ├── YeastAggregate                 │
│ │   ├── Fermentation Predictions       │
│ │   └── Lab Integration                │
│ │                                       │
│ ├── 🎯 Beer Styles Domain              │
│ │   ├── BJCP Guidelines                │
│ │   ├── Style Matching                 │
│ │   └── Compliance Validation          │
│ │                                       │
│ └── 👤 User Management                 │
│     ├── Authentication                 │
│     ├── Authorization                  │
│     └── Profile Management             │
│                                         │
│ EXTERNAL APIS:                          │
│ ├── /api/v1/* (Public Read-Only)       │
│ ├── /api/admin/* (Admin CRUD)          │
│ ├── /api/internal/* (Service-to-Service)│
│ └── /health, /metrics                  │
└─────────────────────────────────────────┘
           │
           ▼
   ┌─────────────────┐
   │ CORE DATABASE   │
   │ (PostgreSQL)    │
   │                 │
   │ • hops          │
   │ • malts         │
   │ • yeasts        │
   │ • beer_styles   │
   │ • origins       │
   │ • users         │
   │ • audit_logs    │
   │ • event_store   │
   └─────────────────┘
```

### 📝 **RECIPE SERVICE** (Port 9002)
*Service recettes - Création, calculs, scaling*

```
┌─────────────────────────────────────────┐
│           RECIPE SERVICE                │
│         (Scala + Play 2.8)             │
├─────────────────────────────────────────┤
│                                         │
│ DOMAINS:                                │
│ ├── 📝 Recipe Domain                   │
│ │   ├── RecipeAggregate                │
│ │   ├── Recipe Builder                 │
│ │   ├── Ingredient Management          │
│ │   └── Procedure Steps                │
│ │                                       │
│ ├── 🧮 Calculations Domain             │
│ │   ├── ABV Calculator                 │
│ │   ├── IBU Calculator (Tinseth)       │
│ │   ├── SRM Color Calculator           │
│ │   ├── Gravity Calculator             │
│ │   └── Efficiency Calculator          │
│ │                                       │
│ ├── 📏 Scaling Domain                  │
│ │   ├── Volume Scaling                 │
│ │   ├── Equipment Profiles             │
│ │   ├── Non-linear Adjustments         │
│ │   └── MiniBrew Integration           │
│ │                                       │
│ ├── 🤖 AI Optimization                │
│ │   ├── Recipe Recommendations         │
│ │   ├── Style Compliance Check         │
│ │   ├── Balance Analysis               │
│ │   └── Problem Detection              │
│ │                                       │
│ └── 📤 Import/Export                   │
│     ├── BeerXML Support                │
│     ├── JSON Format                    │
│     ├── PDF Generation                 │
│     └── Shopping Lists                 │
│                                         │
│ EXTERNAL APIS:                          │
│ ├── /api/v1/recipes/* (Public)         │
│ ├── /api/v1/calculations/*             │
│ ├── /api/v1/scaling/*                  │
│ ├── /api/internal/recipes/*            │
│ └── /api/ai/recommendations/*          │
└─────────────────────────────────────────┘
           │
           ▼
   ┌─────────────────┐      ┌─────────────────┐
   │ RECIPE DATABASE │      │    EXTERNAL     │
   │ (PostgreSQL)    │      │   INTEGRATIONS  │
   │                 │      │                 │
   │ • recipes       │◄────►│ • Core Service  │
   │ • ingredients   │      │   (Ingredients) │
   │ • procedures    │      │ • OpenAI API    │
   │ • calculations  │      │   (AI Features) │
   │ • scaling_hist  │      │ • Community     │
   │ • ai_models     │      │   (Recipe Share)│
   └─────────────────┘      └─────────────────┘
```

### 🌐 **COMMUNITY SERVICE** (Port 9003)
*Service communauté - Forum, social, partage*

```
┌─────────────────────────────────────────┐
│         COMMUNITY SERVICE               │
│         (Scala + Play 2.8)             │
├─────────────────────────────────────────┤
│                                         │
│ DOMAINS:                                │
│ ├── 🏛️ Forum Domain                    │
│ │   ├── Categories & Threads           │
│ │   ├── Posts & Replies               │
│ │   ├── Search & Indexing             │
│ │   ├── Expert Q&A                    │
│ │   └── Moderation System             │
│ │                                       │
│ ├── 📱 Social Domain                   │
│ │   ├── User Feeds & Timeline         │
│ │   ├── Follow System                 │
│ │   ├── Stories & Media               │
│ │   ├── Real-time Chat                │
│ │   └── Notifications                 │
│ │                                       │
│ ├── 🍺 Recipe Sharing Domain          │
│ │   ├── Community Recipes             │
│ │   ├── Recipe Rating & Reviews       │
│ │   ├── Recipe Versioning & Forks     │
│ │   ├── Collaborative Editing         │
│ │   ├── Recipe Collections            │
│ │   └── Brewing Challenges            │
│ │                                       │
│ ├── 🎮 Gamification Domain            │
│ │   ├── User Reputation               │
│ │   ├── Badges & Achievements         │
│ │   ├── Leaderboards                  │
│ │   ├── Challenges & Contests         │
│ │   └── Expert Certification          │
│ │                                       │
│ ├── 🌍 I18n Domain                    │
│ │   ├── Multi-language Content        │
│ │   ├── Community Translation         │
│ │   ├── Cultural Adaptation           │
│ │   ├── Localized Categories          │
│ │   └── Regional Communities          │
│ │                                       │
│ └── 🛡️ Moderation Domain              │
│     ├── Content Filtering             │
│     ├── Spam Detection                │
│     ├── Community Guidelines          │
│     ├── Report System                 │
│     └── AI Moderation                 │
│                                         │
│ EXTERNAL APIS:                          │
│ ├── /api/v1/forum/*                    │
│ ├── /api/v1/social/*                   │
│ ├── /api/v1/recipes/community/*        │
│ ├── /api/v1/i18n/*                     │
│ ├── /api/realtime/* (WebSocket)        │
│ └── /api/internal/community/*          │
└─────────────────────────────────────────┘
           │
           ▼
   ┌─────────────────┐      ┌─────────────────┐
   │COMMUNITY DATABASE│      │    EXTERNAL     │
   │ (PostgreSQL)    │      │   INTEGRATIONS  │
   │                 │      │                 │
   │ • forum_posts   │◄────►│ • Core Service  │
   │ • social_feeds  │      │   (User Auth)   │
   │ • community_rec │      │ • Recipe Service│
   │ • translations  │      │   (Recipe Sync) │
   │ • user_social   │      │ • Translation   │
   │ • gamification │      │   APIs (DeepL)  │
   │ • moderation    │      │ • Media Storage │
   │ • media_files   │      │   (AWS S3/CDN)  │
   └─────────────────┘      └─────────────────┘
```

### ⚙️ **SHARED SERVICES**
*Services transversaux - Infrastructure commune*

```
┌─────────────────────────────────────────┐
│          SHARED SERVICES                │
├─────────────────────────────────────────┤
│                                         │
│ 🔐 AUTH SERVICE (Port 9010)            │
│ ├── JWT Token Management               │
│ ├── OAuth2 Integration                 │
│ ├── Session Management                 │
│ ├── Permission Engine                  │
│ └── SSO Integration                    │
│                                         │
│ 📧 NOTIFICATION SERVICE (Port 9011)    │
│ ├── Email Templates                    │
│ ├── Push Notifications                 │
│ ├── In-App Notifications              │
│ ├── SMS Integration                    │
│ └── Notification Preferences          │
│                                         │
│ 📁 MEDIA SERVICE (Port 9012)          │
│ ├── Image Upload & Processing          │
│ ├── Video Processing                   │
│ ├── CDN Integration                    │
│ ├── Image Optimization                │
│ └── Media Security                     │
│                                         │
│ 📊 ANALYTICS SERVICE (Port 9013)       │
│ ├── Event Tracking                     │
│ ├── User Behavior Analytics           │
│ ├── Business Metrics                  │
│ ├── Performance Monitoring            │
│ └── Report Generation                  │
│                                         │
│ 🌍 I18N SERVICE (Port 9014)           │
│ ├── Translation Management            │
│ ├── Content Localization              │
│ ├── Cultural Adaptation               │
│ ├── Translation Quality Control       │
│ └── Community Translation Platform    │
└─────────────────────────────────────────┘
```

## 🔗 Inter-Service Communication

### 📡 **Communication Patterns**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SERVICE COMMUNICATION                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    HTTP/REST     ┌─────────────┐    HTTP/REST     ┌──────┐  │
│  │    CORE     │◄────────────────►│   RECIPE    │◄────────────────►│COMM. │  │
│  │   SERVICE   │                  │   SERVICE   │                  │SERV. │  │
│  └─────────────┘                  └─────────────┘                  └──────┘  │
│         │                                │                            │      │
│         │ Events                         │ Events                     │      │
│         ▼                                ▼                            ▼      │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                    EVENT BUS (Redis Pub/Sub)                           │  │
│  │                                                                         │  │
│  │ • IngredientUpdated → Recipe Service (recalculate)                     │  │
│  │ • RecipePublished → Community Service (share in feed)                  │  │
│  │ • UserRegistered → All Services (create profiles)                      │  │
│  │ • RecipeRated → Analytics Service (update metrics)                     │  │
│  │ • TranslationCompleted → I18n Service (update content)                 │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  SYNCHRONOUS COMMUNICATION:                                                 │
│  ├── API Gateway → Services: HTTP/REST                                     │
│  ├── Frontend → API Gateway: HTTP/REST + WebSocket                         │
│  └── Service-to-Service: HTTP/REST (for immediate data needs)              │
│                                                                             │
│  ASYNCHRONOUS COMMUNICATION:                                                │
│  ├── Events: Redis Pub/Sub                                                 │
│  ├── Background Jobs: Redis Queue                                          │
│  └── File Processing: AWS SQS                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 🔄 **Event-Driven Architecture**

```scala
// Exemple d'événements inter-services
sealed trait DomainEvent {
  def aggregateId: UUID
  def version: Long
  def timestamp: Instant
  def serviceName: String
}

// Core Service Events
case class HopCreated(aggregateId: UUID, hopData: HopData, version: Long, timestamp: Instant) 
  extends DomainEvent { val serviceName = "core-service" }

case class HopUpdated(aggregateId: UUID, changes: HopChanges, version: Long, timestamp: Instant)
  extends DomainEvent { val serviceName = "core-service" }

// Recipe Service Events  
case class RecipeCreated(aggregateId: UUID, recipeData: RecipeData, version: Long, timestamp: Instant)
  extends DomainEvent { val serviceName = "recipe-service" }

case class RecipePublished(aggregateId: UUID, authorId: UUID, version: Long, timestamp: Instant)
  extends DomainEvent { val serviceName = "recipe-service" }

// Community Service Events
case class RecipeShared(aggregateId: UUID, communityRecipeId: UUID, authorId: UUID, version: Long, timestamp: Instant)
  extends DomainEvent { val serviceName = "community-service" }

case class RecipeRated(recipeId: UUID, userId: UUID, rating: Int, version: Long, timestamp: Instant)
  extends DomainEvent { val serviceName = "community-service" }
```

## 🗄️ **Base de Données par Service**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DATABASE ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐       ┌─────────────┐       ┌─────────────┐               │
│  │CORE DB      │       │RECIPE DB    │       │COMMUNITY DB │               │
│  │(PostgreSQL) │       │(PostgreSQL) │       │(PostgreSQL) │               │
│  │             │       │             │       │             │               │
│  │• hops       │       │• recipes    │       │• forum_*    │               │
│  │• malts      │       │• ingredients│       │• social_*   │               │
│  │• yeasts     │       │• procedures │       │• community_*│               │
│  │• beer_styles│       │• calculations│       │• translations│               │
│  │• origins    │       │• scaling_*  │       │• gamification│               │
│  │• users      │       │• ai_models  │       │• moderation │               │
│  │• audit_logs │       │• equipment  │       │• media_meta │               │
│  │• event_store│       │• versions   │       │• user_social│               │
│  └─────────────┘       └─────────────┘       └─────────────┘               │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        SHARED DATABASES                                 │  │
│  │                                                                         │  │
│  │ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │  │
│  │ │   REDIS     │  │ ELASTICSEARCH│  │    AWS S3   │  │  POSTGRES   │    │  │
│  │ │  (Cache +   │  │   (Search +  │  │   (Media    │  │  (Shared    │    │  │
│  │ │ Pub/Sub +   │  │  Analytics)  │  │   Storage)  │  │   Data)     │    │  │
│  │ │  Sessions)  │  │              │  │             │  │             │    │  │
│  │ └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 🐳 **Containerisation & Deployment**

### **Docker Architecture**

```yaml
# docker-compose.yml - Development Environment
version: '3.8'

services:
  # Frontend Services
  public-web:
    build: ./frontend/public
    ports: ["3000:3000"]
    environment:
      - API_GATEWAY_URL=http://api-gateway:8080

  admin-web:
    build: ./frontend/admin  
    ports: ["3001:3001"]
    environment:
      - API_GATEWAY_URL=http://api-gateway:8080

  # API Gateway
  api-gateway:
    image: nginx:alpine
    ports: ["8080:80"]
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
    depends_on: [core-service, recipe-service, community-service]

  # Core Microservices
  core-service:
    build: ./services/core
    ports: ["9001:9001"]
    environment:
      - DATABASE_URL=jdbc:postgresql://core-db:5432/brewing_core
      - REDIS_URL=redis://redis:6379
    depends_on: [core-db, redis]

  recipe-service:
    build: ./services/recipe
    ports: ["9002:9002"] 
    environment:
      - DATABASE_URL=jdbc:postgresql://recipe-db:5432/brewing_recipes
      - CORE_SERVICE_URL=http://core-service:9001
    depends_on: [recipe-db, core-service]

  community-service:
    build: ./services/community
    ports: ["9003:9003"]
    environment:
      - DATABASE_URL=jdbc:postgresql://community-db:5432/brewing_community
      - CORE_SERVICE_URL=http://core-service:9001
      - RECIPE_SERVICE_URL=http://recipe-service:9002
    depends_on: [community-db, core-service]

  # Databases
  core-db:
    image: postgres:15
    environment:
      - POSTGRES_DB=brewing_core
      - POSTGRES_USER=brewing
      - POSTGRES_PASSWORD=brewing123
    volumes: [core_data:/var/lib/postgresql/data]

  recipe-db:
    image: postgres:15
    environment:
      - POSTGRES_DB=brewing_recipes
    volumes: [recipe_data:/var/lib/postgresql/data]

  community-db:
    image: postgres:15
    environment:
      - POSTGRES_DB=brewing_community
    volumes: [community_data:/var/lib/postgresql/data]

  # Shared Infrastructure
  redis:
    image: redis:7-alpine
    volumes: [redis_data:/data]

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    volumes: [es_data:/usr/share/elasticsearch/data]

volumes:
  core_data:
  recipe_data: 
  community_data:
  redis_data:
  es_data:
```

### ☸️ **Kubernetes Production**

```yaml
# k8s/core-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: core-service
  template:
    metadata:
      labels:
        app: core-service
    spec:
      containers:
      - name: core-service
        image: brewing-platform/core-service:latest
        ports: [containerPort: 9001]
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: brewing-secrets
              key: core-db-url
        resources:
          requests: {memory: "512Mi", cpu: "250m"}
          limits: {memory: "1Gi", cpu: "500m"}
        livenessProbe:
          httpGet: {path: /health, port: 9001}
          initialDelaySeconds: 30
        readinessProbe:
          httpGet: {path: /ready, port: 9001}
          initialDelaySeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: core-service
spec:
  selector:
    app: core-service
  ports:
  - port: 9001
    targetPort: 9001
  type: ClusterIP
```

## 🔍 **Monitoring & Observability**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MONITORING ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │    CORE     │  │   RECIPE    │  │ COMMUNITY   │  │   SHARED    │        │
│  │   SERVICE   │  │   SERVICE   │  │   SERVICE   │  │  SERVICES   │        │
│  │             │  │             │  │             │  │             │        │
│  │/metrics     │  │/metrics     │  │/metrics     │  │/metrics     │        │
│  │/health      │  │/health      │  │/health      │  │/health      │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│         │                 │                 │                 │           │
│         └─────────────────┼─────────────────┼─────────────────┘           │
│                           │                 │                             │
│                           ▼                 ▼                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                      PROMETHEUS                                         │  │
│  │                   (Metrics Collection)                                  │  │
│  │                                                                         │  │
│  │ • Request Rate & Response Time                                          │  │
│  │ • Error Rate & Status Codes                                            │  │
│  │ • Database Connection Pool                                              │  │
│  │ • JVM Metrics (Heap, GC)                                              │  │
│  │ • Business Metrics (Recipes created, Users active)                     │  │
│  │ • Custom Application Metrics                                           │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                           │                                                 │
│                           ▼                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                       GRAFANA                                           │  │
│  │                    (Visualization)                                      │  │
│  │                                                                         │  │
│  │ 📊 DASHBOARDS:                                                         │  │
│  │ ├── System Overview (All Services)                                     │  │
│  │ ├── Core Service Metrics                                               │  │
│  │ ├── Recipe Service Performance                                         │  │
│  │ ├── Community Engagement                                               │  │
│  │ ├── Database Performance                                               │  │
│  │ ├── Business KPIs                                                      │  │
│  │ └── SLA & Error Tracking                                               │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                    LOGGING STACK                                        │  │
│  │                                                                         │  │
│  │ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                     │  │
│  │ │   FLUENT    │  │ELASTICSEARCH│  │   KIBANA    │                     │  │
│  │ │    BIT      │  │             │  │             │                     │  │
│  │ │(Log Forward)│→ │(Log Storage)│→ │(Log Search) │                     │  │
│  │ └─────────────┘  └─────────────┘  └─────────────┘                     │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

Cette architecture microservices garantit :

✅ **Séparation claire** des responsabilités  
✅ **Scalabilité indépendante** de chaque service  
✅ **Isolation des pannes** (circuit breakers)  
✅ **Frontend découplé** du backend  
✅ **Performance optimisée** par service  
✅ **Monitoring complet** et observabilité  
✅ **Déploiement indépendant** avec CI/CD  

Une architecture robuste, moderne et prête pour une croissance mondiale ! 🚀