-- Évolution base de données - Tables recipes
# --- !Ups

-- =============================================================================
-- ÉVOLUTION 12 - DOMAINE RECIPES
-- Tables pour gestion complète des recettes avec Event Sourcing
-- Suit exactement le pattern Yeasts pour cohérence architecturale
-- =============================================================================

-- Table principale des recettes
CREATE TABLE recipes (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    
    -- Style de bière
    style_id            VARCHAR(10) NOT NULL,
    style_name          VARCHAR(100) NOT NULL,
    style_category      VARCHAR(100) NOT NULL,
    
    -- Taille du lot
    batch_size_value    DECIMAL(8,2) NOT NULL CHECK (batch_size_value > 0),
    batch_size_unit     VARCHAR(20) NOT NULL DEFAULT 'LITERS' CHECK (batch_size_unit IN ('LITERS', 'GALLONS')),
    batch_size_liters   DECIMAL(8,2) NOT NULL CHECK (batch_size_liters > 0), -- Valeur normalisée
    
    -- Ingrédients (JSON pour flexibilité)
    hops                JSONB NOT NULL DEFAULT '[]'::jsonb,
    malts               JSONB NOT NULL DEFAULT '[]'::jsonb,
    yeast               JSONB,
    other_ingredients   JSONB NOT NULL DEFAULT '[]'::jsonb,
    
    -- Calculs
    original_gravity    DECIMAL(5,3) CHECK (original_gravity >= 1.000 AND original_gravity <= 1.200),
    final_gravity       DECIMAL(5,3) CHECK (final_gravity >= 0.990 AND final_gravity <= 1.100),
    abv                 DECIMAL(4,2) CHECK (abv >= 0 AND abv <= 20),
    ibu                 DECIMAL(5,1) CHECK (ibu >= 0 AND ibu <= 200),
    srm                 DECIMAL(5,1) CHECK (srm >= 0 AND srm <= 80),
    efficiency          DECIMAL(4,1) CHECK (efficiency >= 50.0 AND efficiency <= 100.0),
    calculations_at     TIMESTAMP WITH TIME ZONE,
    
    -- Procédures
    has_mash_profile        BOOLEAN NOT NULL DEFAULT true,
    has_boil_procedure      BOOLEAN NOT NULL DEFAULT true,
    has_fermentation_profile BOOLEAN NOT NULL DEFAULT true,
    has_packaging_procedure BOOLEAN NOT NULL DEFAULT false,
    
    -- Métadonnées
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_by          UUID NOT NULL,
    version             INTEGER NOT NULL DEFAULT 1,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index pour performances (suivant pattern malts/yeasts)
CREATE INDEX idx_recipes_name ON recipes(name);
CREATE INDEX idx_recipes_style_id ON recipes(style_id);
CREATE INDEX idx_recipes_style_category ON recipes(style_category);
CREATE INDEX idx_recipes_batch_size_value ON recipes(batch_size_value);
CREATE INDEX idx_recipes_batch_size_unit ON recipes(batch_size_unit);
CREATE INDEX idx_recipes_status ON recipes(status);
CREATE INDEX idx_recipes_created_by ON recipes(created_by);
CREATE INDEX idx_recipes_created_at ON recipes(created_at);
CREATE INDEX idx_recipes_updated_at ON recipes(updated_at);

-- Index composite pour recherches courantes
CREATE INDEX idx_recipes_active_style ON recipes(status, style_category) WHERE status IN ('DRAFT', 'PUBLISHED');
CREATE INDEX idx_recipes_published ON recipes(status, created_at) WHERE status = 'PUBLISHED';

-- Index GIN pour recherche dans ingredients JSONB
CREATE INDEX idx_recipes_hops ON recipes USING GIN (hops);
CREATE INDEX idx_recipes_malts ON recipes USING GIN (malts);
CREATE INDEX idx_recipes_yeast ON recipes USING GIN (yeast);
CREATE INDEX idx_recipes_other_ingredients ON recipes USING GIN (other_ingredients);

-- Index pour recherche par plages de valeurs
CREATE INDEX idx_recipes_abv ON recipes(abv) WHERE abv IS NOT NULL;
CREATE INDEX idx_recipes_ibu ON recipes(ibu) WHERE ibu IS NOT NULL;
CREATE INDEX idx_recipes_srm ON recipes(srm) WHERE srm IS NOT NULL;

-- =============================================================================
-- TABLE ÉVÉNEMENTS RECIPES POUR EVENT SOURCING
-- =============================================================================

-- Table des événements recipe pour Event Sourcing (suit pattern yeast_events)
CREATE TABLE recipe_events (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id        UUID NOT NULL,
    aggregate_type      VARCHAR(50) NOT NULL DEFAULT 'RecipeAggregate',
    event_type          VARCHAR(100) NOT NULL,
    event_version       INTEGER NOT NULL DEFAULT 1,
    event_data          JSONB NOT NULL,
    metadata            JSONB DEFAULT '{}'::jsonb,
    sequence_number     BIGSERIAL,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index pour performances Event Sourcing (identique à yeast_events)
CREATE INDEX idx_recipe_events_aggregate_id ON recipe_events(aggregate_id);
CREATE INDEX idx_recipe_events_aggregate_type ON recipe_events(aggregate_type);
CREATE INDEX idx_recipe_events_event_type ON recipe_events(event_type);
CREATE INDEX idx_recipe_events_occurred_at ON recipe_events(occurred_at);
CREATE INDEX idx_recipe_events_sequence_number ON recipe_events(sequence_number);

-- Index composite pour reconstruction d'agrégat
CREATE INDEX idx_recipe_events_aggregate_sequence ON recipe_events(aggregate_id, sequence_number);
CREATE INDEX idx_recipe_events_aggregate_occurred ON recipe_events(aggregate_id, occurred_at);

-- Index GIN pour recherche dans event_data et metadata JSONB
CREATE INDEX idx_recipe_events_event_data ON recipe_events USING GIN (event_data);
CREATE INDEX idx_recipe_events_metadata ON recipe_events USING GIN (metadata);

-- =============================================================================
-- TABLE SNAPSHOTS RECIPES (Pour optimisation Event Sourcing)
-- =============================================================================

-- Table des snapshots d'agrégats pour optimiser la reconstruction
CREATE TABLE recipe_snapshots (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id        UUID NOT NULL,
    aggregate_type      VARCHAR(50) NOT NULL DEFAULT 'RecipeAggregate',
    aggregate_version   INTEGER NOT NULL,
    snapshot_data       JSONB NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Un seul snapshot par agrégat (le plus récent)
    UNIQUE(aggregate_id)
);

-- Index pour performances snapshots
CREATE INDEX idx_recipe_snapshots_aggregate_id ON recipe_snapshots(aggregate_id);
CREATE INDEX idx_recipe_snapshots_aggregate_version ON recipe_snapshots(aggregate_version);
CREATE INDEX idx_recipe_snapshots_created_at ON recipe_snapshots(created_at);

-- Index GIN pour recherche dans snapshot_data JSONB
CREATE INDEX idx_recipe_snapshots_data ON recipe_snapshots USING GIN (snapshot_data);

-- =============================================================================
-- CONTRAINTES ET TRIGGERS
-- =============================================================================

-- Contrainte pour s'assurer que sequence_number est incrémental par agrégat
CREATE UNIQUE INDEX idx_recipe_events_unique_sequence 
ON recipe_events(aggregate_id, sequence_number);

-- Trigger pour validation des événements
CREATE OR REPLACE FUNCTION validate_recipe_event()
RETURNS TRIGGER AS $$
BEGIN
    -- Validation du type d'événement
    IF NEW.event_type NOT IN (
        'RecipeCreated',
        'RecipeUpdated', 
        'RecipeStatusChanged',
        'RecipeArchived',
        'RecipeRestored',
        'RecipeDeleted'
    ) THEN
        RAISE EXCEPTION 'Type d''événement non valide: %', NEW.event_type;
    END IF;
    
    -- Validation de la structure event_data selon le type
    IF NEW.event_type = 'RecipeCreated' AND NOT (NEW.event_data ? 'name' AND NEW.event_data ? 'styleId') THEN
        RAISE EXCEPTION 'Données manquantes pour événement RecipeCreated';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_recipe_event
    BEFORE INSERT ON recipe_events
    FOR EACH ROW
    EXECUTE FUNCTION validate_recipe_event();

-- Trigger pour mise à jour automatique updated_at
CREATE OR REPLACE FUNCTION update_recipe_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_recipes_updated_at
    BEFORE UPDATE ON recipes
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_updated_at_column();

-- =============================================================================
-- VUES POUR REQUÊTES COURANTES
-- =============================================================================

-- Vue des derniers événements par agrégat
CREATE VIEW latest_recipe_events AS
SELECT DISTINCT ON (aggregate_id)
    aggregate_id,
    event_type,
    event_data,
    occurred_at,
    sequence_number
FROM recipe_events
ORDER BY aggregate_id, sequence_number DESC;

-- Vue statistiques événements
CREATE VIEW recipe_event_stats AS
SELECT
    event_type,
    COUNT(*) as event_count,
    COUNT(DISTINCT aggregate_id) as unique_aggregates,
    MIN(occurred_at) as first_occurrence,
    MAX(occurred_at) as last_occurrence
FROM recipe_events
GROUP BY event_type
ORDER BY event_count DESC;

-- Vue recettes actives avec statistiques
CREATE VIEW active_recipes AS
SELECT
    r.*,
    (SELECT COUNT(*) FROM recipe_events re WHERE re.aggregate_id = r.id) as total_events_count
FROM recipes r
WHERE r.status IN ('DRAFT', 'PUBLISHED');

-- Vue statistiques recettes par style
CREATE VIEW recipe_stats_by_style AS
SELECT
    style_category,
    style_id,
    style_name,
    COUNT(*) as total_count,
    COUNT(*) FILTER (WHERE status = 'PUBLISHED') as published_count,
    AVG(batch_size_liters) as avg_batch_size_liters,
    AVG(abv) FILTER (WHERE abv IS NOT NULL) as avg_abv,
    AVG(ibu) FILTER (WHERE ibu IS NOT NULL) as avg_ibu,
    AVG(srm) FILTER (WHERE srm IS NOT NULL) as avg_srm
FROM recipes
GROUP BY style_category, style_id, style_name;

-- =============================================================================
-- DONNÉES DE RÉFÉRENCE INITIALES
-- =============================================================================

-- Insertion de recettes de test pour démo (utilisant les malts/hops/yeasts existants)
INSERT INTO recipes (
    id, name, description, style_id, style_name, style_category,
    batch_size_value, batch_size_unit, batch_size_liters,
    hops, malts, yeast, 
    original_gravity, final_gravity, abv, ibu, srm, efficiency,
    calculations_at, status, created_by
) VALUES
(
    uuid_generate_v4(),
    'American IPA Classique',
    'Une IPA houblonnée avec des notes d''agrumes et de pin, parfaite pour découvrir le style.',
    '21A',
    'American IPA',
    'IPA',
    20.0,
    'LITERS',
    20.0,
    '[
        {"hopId": "cascade", "amount": 50.0, "time": 60, "usage": "BOIL"},
        {"hopId": "centennial", "amount": 30.0, "time": 10, "usage": "AROMA"}
    ]'::jsonb,
    '[
        {"maltId": "pale-ale", "amount": 4.5, "percentage": 85.0},
        {"maltId": "crystal-40", "amount": 0.5, "percentage": 15.0}
    ]'::jsonb,
    '{"yeastId": "us-05", "amount": 11.5, "temperature": null}'::jsonb,
    1.060,
    1.010,
    6.5,
    45.0,
    8.0,
    75.0,
    NOW(),
    'PUBLISHED',
    uuid_generate_v4()
),
(
    uuid_generate_v4(),
    'Wheat Beer Allemande',
    'Une bière de blé traditionnelle, trouble et rafraîchissante.',
    '1D',
    'American Wheat Beer',
    'Standard American Beer',
    20.0,
    'LITERS',
    20.0,
    '[
        {"hopId": "hallertau", "amount": 25.0, "time": 60, "usage": "BOIL"}
    ]'::jsonb,
    '[
        {"maltId": "wheat-malt", "amount": 3.0, "percentage": 60.0},
        {"maltId": "pilsner-malt", "amount": 2.0, "percentage": 40.0}
    ]'::jsonb,
    '{"yeastId": "wheat-yeast", "amount": 11.5, "temperature": null}'::jsonb,
    1.048,
    1.012,
    4.7,
    15.0,
    3.5,
    78.0,
    NOW(),
    'PUBLISHED',
    uuid_generate_v4()
),
(
    uuid_generate_v4(),
    'Imperial Stout Expérimental',
    'Stout impérial complexe, en cours de développement.',
    '20B',
    'American Stout',
    'American Porter and Stout',
    20.0,
    'LITERS',
    20.0,
    '[
        {"hopId": "columbus", "amount": 40.0, "time": 60, "usage": "BOIL"},
        {"hopId": "cascade", "amount": 20.0, "time": 0, "usage": "WHIRLPOOL"}
    ]'::jsonb,
    '[
        {"maltId": "pale-ale", "amount": 6.0, "percentage": 70.0},
        {"maltId": "chocolate-malt", "amount": 1.0, "percentage": 15.0},
        {"maltId": "crystal-120", "amount": 0.8, "percentage": 10.0},
        {"maltId": "roasted-barley", "amount": 0.4, "percentage": 5.0}
    ]'::jsonb,
    '{"yeastId": "american-ale", "amount": 23.0, "temperature": null}'::jsonb,
    1.085,
    1.018,
    8.8,
    65.0,
    40.0,
    72.0,
    NOW(),
    'DRAFT',
    uuid_generate_v4()
);

# --- !Downs

-- Suppression dans l'ordre inverse des dépendances
DROP VIEW IF EXISTS recipe_stats_by_style;
DROP VIEW IF EXISTS active_recipes;
DROP VIEW IF EXISTS recipe_event_stats;
DROP VIEW IF EXISTS latest_recipe_events;

DROP TRIGGER IF EXISTS trigger_recipes_updated_at ON recipes;
DROP TRIGGER IF EXISTS trigger_validate_recipe_event ON recipe_events;
DROP FUNCTION IF EXISTS update_recipe_updated_at_column();
DROP FUNCTION IF EXISTS validate_recipe_event();

DROP INDEX IF EXISTS idx_recipe_snapshots_data;
DROP INDEX IF EXISTS idx_recipe_snapshots_created_at;
DROP INDEX IF EXISTS idx_recipe_snapshots_aggregate_version;
DROP INDEX IF EXISTS idx_recipe_snapshots_aggregate_id;

DROP INDEX IF EXISTS idx_recipe_events_metadata;
DROP INDEX IF EXISTS idx_recipe_events_event_data;
DROP INDEX IF EXISTS idx_recipe_events_aggregate_occurred;
DROP INDEX IF EXISTS idx_recipe_events_aggregate_sequence;
DROP INDEX IF EXISTS idx_recipe_events_unique_sequence;
DROP INDEX IF EXISTS idx_recipe_events_sequence_number;
DROP INDEX IF EXISTS idx_recipe_events_occurred_at;
DROP INDEX IF EXISTS idx_recipe_events_event_type;
DROP INDEX IF EXISTS idx_recipe_events_aggregate_type;
DROP INDEX IF EXISTS idx_recipe_events_aggregate_id;

DROP INDEX IF EXISTS idx_recipes_srm;
DROP INDEX IF EXISTS idx_recipes_ibu;
DROP INDEX IF EXISTS idx_recipes_abv;
DROP INDEX IF EXISTS idx_recipes_other_ingredients;
DROP INDEX IF EXISTS idx_recipes_yeast;
DROP INDEX IF EXISTS idx_recipes_malts;
DROP INDEX IF EXISTS idx_recipes_hops;
DROP INDEX IF EXISTS idx_recipes_published;
DROP INDEX IF EXISTS idx_recipes_active_style;
DROP INDEX IF EXISTS idx_recipes_updated_at;
DROP INDEX IF EXISTS idx_recipes_created_at;
DROP INDEX IF EXISTS idx_recipes_created_by;
DROP INDEX IF EXISTS idx_recipes_status;
DROP INDEX IF EXISTS idx_recipes_batch_size_unit;
DROP INDEX IF EXISTS idx_recipes_batch_size_value;
DROP INDEX IF EXISTS idx_recipes_style_category;
DROP INDEX IF EXISTS idx_recipes_style_id;
DROP INDEX IF EXISTS idx_recipes_name;

DROP TABLE IF EXISTS recipe_snapshots;
DROP TABLE IF EXISTS recipe_events;
DROP TABLE IF EXISTS recipes;