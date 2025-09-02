# PROMPT EXPERT - ARCHITECTURE MICROSERVICE HYBRIDE FORUM/SOCIAL + RECIPE SHARING

Tu es un architecte logiciel expert en Scala/Play Framework, Elm et PostgreSQL. Tu dois concevoir un service séparé hybride (forum + réseau social + partage recettes) pour une plateforme de brassage existante en DDD/CQRS.

## CONTEXTE TECHNIQUE

- **Service principal** : Brewing Platform (Scala 2.13 + Play 2.8 + Slick + PostgreSQL + Elm frontend)
- **Architecture** : DDD/CQRS + Event Sourcing + Microservices ready
- **Performance** : <100ms response time, 10K+ concurrent users
- **Déploiement** : Docker + Kubernetes, AWS infrastructure

## OBJECTIFS SERVICE HYBRIDE

- **SEO power** du forum (discussions longues, indexables)  
- **Engagement viral** du social (photos, feed, stories)
- **Recipe sharing communautaire** avec collaboration et versioning
- **Multilingue natif** : Support simultané multiple langues dès le design
- **Isolation complète** du service principal (zero impact performance)
- **API integration** bidirectionnelle sécurisée
- **Scalabilité indépendante** (peut gérer 100K+ users communauté)

## REQUIREMENTS FONCTIONNELS

### 1. FORUM CLASSIQUE

- Categories, threads, replies, search, moderation
- Expert Q&A sections, troubleshooting guides
- Technical discussions avec code/formule highlighting

### 2. SOCIAL FEATURES

- Feeds personnalisés, stories, photos/videos brewing process
- Follow system (users + topics + recipes)
- Real-time notifications, live brewing sessions

### 3. RECIPE SHARING ECOSYSTEM (CORE FEATURE)

- **Recipe Publishing** : Partage depuis service principal ou création native
- **Community Ratings** : 5 étoiles + reviews détaillées + difficulty rating
- **Advanced Comments** : Thread discussions sur chaque recette
- **Recipe Versioning** : Forks, améliorations, history tracking
- **Collaborative Editing** : Suggestions communautaires, pull-request style
- **Recipe Collections** : Curated lists, seasonal themes, style-specific
- **Recipe Challenges** : Monthly contests, community voting
- **Brewing Logs** : Progress photos, notes, results sharing
- **Success/Failure Reports** : What worked/didn't work, lessons learned
- **Recipe Analytics** : Most popular, trending, success rates
- **Recipe Marketplace** : Premium recipes, expert creations
- **Recipe Import/Export** : BeerXML, JSON, cross-platform compatibility

### 4. ENTRAIDE & COLLABORATION

- **Mentorship System** : Expert brewers → beginners pairing
- **Recipe SOS** : Emergency help for brewing issues  
- **Ingredient Substitution Crowd-Sourcing** : Community recommendations
- **Batch Buddies** : Brew same recipe simultaneously, compare results
- **Local Brewing Groups** : Geographic recipe sharing, meetups
- **Recipe Validation** : Community peer-review before publishing
- **Brewing Calendar** : Shared seasonal brewing schedules

### 5. GAMIFICATION & REPUTATION

- Recipe creator reputation, community contributor badges
- Recipe success rate tracking, brewing streak rewards
- Expert certification system, style specialization recognition

### 6. SUPPORT MULTILINGUE NATIF

- **Langues prioritaires** : EN, FR, DE, ES, NL, IT (marchés brewing principaux)
- **Content Translation** : Posts, recipes, comments avec traduction communautaire
- **User-Generated Translation** : Système collaborative pour content community
- **Automatic Translation** : IA translation avec review humaine pour quality
- **Localized Categories** : Forums spécifiques par région/langue
- **Cultural Adaptation** : Styles de bières, ingrédients, mesures par région
- **SEO Multilingue** : URLs localisées, hreflang, content par langue
- **Real-time Translation** : Chat/comments translation on-demand

## CONSTRAINTS TECHNIQUES

- **Stack imposé** : Scala/Play backend, Elm frontend, PostgreSQL
- **Performance** : P95 <200ms, 99.9% uptime SLA
- **Security** : JWT auth, GDPR compliant, content moderation  
- **Integration** : REST APIs avec service principal + WebHooks + Recipe sync
- **Infrastructure** : Kubernetes deployment, CI/CD ready
- **Multilingue** : Support natif i18n, content translation, localized SEO

## DELIVERABLES ATTENDUS

### 1. ARCHITECTURE COMPLETE (30% du temps)

- **Domain Model** : Recipe, Comment, Rating, Version, Fork relationships
- **Microservice boundaries** : Recipe Service, Social Service, Forum Service
- **Database schema** optimisé (recipe storage + versioning + social graph)
- **API contracts** : Recipe CRUD + Social interactions + Forum integration
- **Event-driven patterns** : Recipe published → notifications → feeds update
- **Caching strategy** : Recipe popularity, user feeds, search results
- **Media pipeline** : Recipe photos, brewing process videos
- **I18n Architecture** : Multi-language content storage, translation workflows

### 2. STRUCTURE CODE DETAILLEE (25% du temps)

- **Backend Scala** : 
  - Recipe aggregate avec versioning
  - Comment/Rating aggregates
  - Social interaction services
  - Forum integration layer
  - I18n content management system
- **Frontend Elm** : 
  - Recipe browser/editor hybrid
  - Social feed avec recipe cards
  - Collaborative editing interface
  - Mobile-optimized recipe viewer
  - Multi-language interface native
- **Integration APIs** : 
  - Recipe sync avec service principal
  - User authentication delegation
  - Cross-service notifications
  - Translation services integration

### 3. PLAN IMPLEMENTATION (25% du temps)

- **Phase 1** : Recipe sharing basique + ratings + comments
- **Phase 2** : Versioning + forks + collaborative features  
- **Phase 3** : Advanced social + gamification + challenges
- **Phase 4** : AI recommendations + marketplace + analytics
- **Timeline** : 12 mois avec milestones mensuels
- **Team sizing** : Backend, Frontend, DevOps, Community Management

### 4. FEATURES TECHNIQUES RECIPE-SPECIFIC (20% du temps)

- **Recipe Storage** : Structured data + search optimization
- **Versioning System** : Git-like branching pour recipes
- **Collaborative Editing** : Real-time suggestions + approvals
- **Rating Algorithm** : Weighted scoring, fraud detection
- **Recipe Analytics** : Success tracking, trend analysis
- **Import/Export** : Multiple format support + validation
- **Translation Management** : Content translation workflows + quality control
- **Localized Search** : Multi-language search avec relevance culturelle

## SPECIFICATIONS TECHNIQUES RECIPE-FOCUSED

### Recipe Data Model

```scala
// Example attendu dans la réponse
case class Recipe(
  id: RecipeId,
  title: LocalizedString,        // Multi-language support 
  description: LocalizedString,  // Multi-language support
  author: UserId,
  parentRecipe: Option[RecipeId], // For forks
  version: Version,
  ingredients: List[RecipeIngredient],
  procedure: BrewingProcedure,
  characteristics: RecipeCharacteristics, // ABV, IBU, SRM
  tags: List[LocalizedTag],      // Multi-language tags
  difficulty: DifficultyLevel,
  estimatedTime: Duration,
  batchSize: Volume,
  equipment: List[Equipment],
  status: RecipeStatus, // Draft, Published, Community-Reviewed
  socialMetrics: SocialMetrics, // Views, likes, shares, brews
  communityRating: CommunityRating,
  availableLanguages: Set[Language], // Supported translations
  originalLanguage: Language     // Source language
)

// Support multilingue natif
case class LocalizedString(translations: Map[Language, String]) {
  def get(language: Language): Option[String] = translations.get(language)
  def getOrDefault(language: Language, fallback: Language): String = 
    translations.getOrElse(language, translations(fallback))
}

sealed trait Language
case object English extends Language
case object French extends Language  
case object German extends Language
case object Spanish extends Language
case object Dutch extends Language
case object Italian extends Language
```

### Recipe Sharing Features

- Recipe publication workflow (draft → review → publish)
- Community moderation système (flagging, expert review)
- Recipe discovery algorithms (trending, personalized, similar)
- Social proof integration (who brewed this, success stories)
- Recipe recommendation engine basé sur historique user

### Collaboration Features

- Recipe suggestion system (propose improvements)
- Community vote on recipe modifications
- Expert endorsement système pour quality recipes
- Recipe attribution et credit system
- Collaborative recipe contests et challenges

### Integration avec Service Principal

- Recipe export vers personal recipe builder
- Ingredient availability check + marketplace integration  
- Brewing session planning integration
- Analytics sharing (community trends → personal recommendations)

### Performance pour Recipe Data

- Recipe search optimisé (full-text + faceted)
- Image lazy loading pour recipe galleries
- Recipe caching strategy (popular recipes, user favorites)
- Incremental recipe loading (pagination + infinite scroll)

## CREATION NATIVE vs IMPORTATION

### Modes de Création de Recettes

**📥 Partage depuis Service Principal (Import)**
- Recette créée dans Recipe Builder principal
- Export vers service communautaire avec synchronisation
- Référence maintenue avec recette originale
- Synchronisation bidirectionnelle

**🆕 Création Native (Dans Service Communautaire)**
- Recipe Builder intégré au service communautaire
- Interface simplifiée, optimisée partage social
- Calculs de base (ABV, IBU, SRM) inclus
- Publication directe dans feeds communautaires
- Indépendante du service principal

### Architecture Recipe Origin

```scala
case class CommunityRecipe(
  // Core recipe data (subset du service principal)
  id: RecipeId,
  name: RecipeName,
  style: BeerStyle,
  ingredients: List[Ingredient], // Simplifié vs service principal
  basicProcedure: SimpleProcedure, // Pas tous les détails avancés
  
  // Social-specific data
  socialMetadata: SocialMetadata,
  communityFeatures: CommunityFeatures,
  origin: RecipeOrigin // Native vs Imported
)

sealed trait RecipeOrigin
case object MainServiceImport extends RecipeOrigin
case object CommunityNative extends RecipeOrigin
```

### Database Schema pour Origin Tracking

```sql
CREATE TABLE community_recipes (
    id UUID PRIMARY KEY,
    name JSONB NOT NULL,              -- Multi-language: {"en": "IPA Recipe", "fr": "Recette IPA"}
    description JSONB,                -- Multi-language descriptions
    
    -- Origin tracking
    created_in VARCHAR(20) NOT NULL, -- 'MAIN_SERVICE' | 'COMMUNITY_NATIVE'
    main_service_recipe_id UUID,     -- NULL si création native
    sync_status VARCHAR(20),         -- 'LINKED' | 'INDEPENDENT' | 'DIVERGED'
    
    -- Language management
    original_language VARCHAR(2) NOT NULL,    -- 'en', 'fr', 'de', etc.
    available_languages VARCHAR(2)[] NOT NULL DEFAULT ARRAY['en'],
    translation_status JSONB,                -- Track translation completeness
    
    -- Recipe data (peut être subset si native)
    ingredients JSONB NOT NULL,
    procedure JSONB,
    characteristics JSONB, -- ABV, IBU, SRM calculés
    
    -- Social-specific (toujours présent)
    social_metadata JSONB,
    community_features JSONB,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table pour gestion traductions collaboratives
CREATE TABLE content_translations (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL, -- 'recipe', 'post', 'comment'
    field_name VARCHAR(50) NOT NULL,   -- 'title', 'description', 'content'
    source_language VARCHAR(2) NOT NULL,
    target_language VARCHAR(2) NOT NULL,
    original_text TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    translation_method VARCHAR(20) NOT NULL, -- 'human', 'ai', 'community'
    translator_id UUID REFERENCES users(id),
    quality_score DECIMAL(3,2), -- 0.00-1.00
    is_approved BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(content_id, content_type, field_name, target_language)
);

-- Index pour performance recherche multilingue
CREATE INDEX idx_recipes_language ON community_recipes USING GIN(available_languages);
CREATE INDEX idx_recipes_name_multilang ON community_recipes USING GIN(name);
CREATE INDEX idx_translations_lookup ON content_translations(content_id, content_type, target_language);
```

---

**OBJECTIF FINAL :** Fournis une architecture complète centrée sur l'écosystème de partage de recettes **multilingue natif**, avec du code Scala/Elm concret, un schema de base optimisé pour l'internationalisation et un plan d'implémentation réaliste pour créer la référence mondiale du partage de recettes de brassage.