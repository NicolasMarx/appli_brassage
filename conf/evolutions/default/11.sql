-- Évolution base de données - Table yeast_events pour Event Sourcing
# --- !Ups

-- =============================================================================
-- ÉVOLUTION 11 - EVENT SOURCING YEASTS
-- Table d'événements pour Event Sourcing du domaine levures
-- Suit les principes DDD/CQRS définis dans le projet
-- =============================================================================

-- Table des événements yeast pour Event Sourcing
CREATE TABLE yeast_events (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id        UUID NOT NULL,
    aggregate_type      VARCHAR(50) NOT NULL DEFAULT 'YeastAggregate',
    event_type          VARCHAR(100) NOT NULL,
    event_version       INTEGER NOT NULL DEFAULT 1,
    event_data          JSONB NOT NULL,
    metadata            JSONB DEFAULT '{}'::jsonb,
    sequence_number     BIGSERIAL,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index pour performances Event Sourcing
CREATE INDEX idx_yeast_events_aggregate_id ON yeast_events(aggregate_id);
CREATE INDEX idx_yeast_events_aggregate_type ON yeast_events(aggregate_type);
CREATE INDEX idx_yeast_events_event_type ON yeast_events(event_type);
CREATE INDEX idx_yeast_events_occurred_at ON yeast_events(occurred_at);
CREATE INDEX idx_yeast_events_sequence_number ON yeast_events(sequence_number);

-- Index composite pour reconstruction d'agrégat
CREATE INDEX idx_yeast_events_aggregate_sequence ON yeast_events(aggregate_id, sequence_number);
CREATE INDEX idx_yeast_events_aggregate_occurred ON yeast_events(aggregate_id, occurred_at);

-- Index GIN pour recherche dans event_data et metadata JSONB
CREATE INDEX idx_yeast_events_event_data ON yeast_events USING GIN (event_data);
CREATE INDEX idx_yeast_events_metadata ON yeast_events USING GIN (metadata);

-- =============================================================================
-- TABLE SNAPSHOTS (Pour optimisation Event Sourcing)
-- =============================================================================

-- Table des snapshots d'agrégats pour optimiser la reconstruction
CREATE TABLE yeast_snapshots (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id        UUID NOT NULL,
    aggregate_type      VARCHAR(50) NOT NULL DEFAULT 'YeastAggregate',
    aggregate_version   INTEGER NOT NULL,
    snapshot_data       JSONB NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Un seul snapshot par agrégat (le plus récent)
    UNIQUE(aggregate_id)
);

-- Index pour performances snapshots
CREATE INDEX idx_yeast_snapshots_aggregate_id ON yeast_snapshots(aggregate_id);
CREATE INDEX idx_yeast_snapshots_aggregate_version ON yeast_snapshots(aggregate_version);
CREATE INDEX idx_yeast_snapshots_created_at ON yeast_snapshots(created_at);

-- Index GIN pour recherche dans snapshot_data JSONB
CREATE INDEX idx_yeast_snapshots_data ON yeast_snapshots USING GIN (snapshot_data);

-- =============================================================================
-- CONTRAINTES ET TRIGGERS
-- =============================================================================

-- Contrainte pour s'assurer que sequence_number est incrémental par agrégat
CREATE UNIQUE INDEX idx_yeast_events_unique_sequence 
ON yeast_events(aggregate_id, sequence_number);

-- Trigger pour validation des événements
CREATE OR REPLACE FUNCTION validate_yeast_event()
RETURNS TRIGGER AS $$
BEGIN
    -- Validation du type d'événement
    IF NEW.event_type NOT IN (
        'YeastCreated',
        'YeastUpdated', 
        'YeastStatusChanged',
        'YeastArchived',
        'YeastRestored',
        'YeastDeleted'
    ) THEN
        RAISE EXCEPTION 'Type d''événement non valide: %', NEW.event_type;
    END IF;
    
    -- Validation de la structure event_data selon le type
    IF NEW.event_type = 'YeastCreated' AND NOT (NEW.event_data ? 'name' AND NEW.event_data ? 'strain') THEN
        RAISE EXCEPTION 'Données manquantes pour événement YeastCreated';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_yeast_event
    BEFORE INSERT ON yeast_events
    FOR EACH ROW
    EXECUTE FUNCTION validate_yeast_event();

-- Trigger pour mise à jour automatique des snapshots
CREATE OR REPLACE FUNCTION update_yeast_snapshot()
RETURNS TRIGGER AS $$
BEGIN
    -- Logique simplifiée - en production, ceci serait géré par l'application
    -- Ce trigger sert de placeholder pour démontrer le pattern
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_yeast_snapshot
    AFTER INSERT ON yeast_events
    FOR EACH ROW
    EXECUTE FUNCTION update_yeast_snapshot();

-- =============================================================================
-- VUES POUR REQUÊTES COURANTES
-- =============================================================================

-- Vue des derniers événements par agrégat
CREATE VIEW latest_yeast_events AS
SELECT DISTINCT ON (aggregate_id)
    aggregate_id,
    event_type,
    event_data,
    occurred_at,
    sequence_number
FROM yeast_events
ORDER BY aggregate_id, sequence_number DESC;

-- Vue statistiques événements
CREATE VIEW yeast_event_stats AS
SELECT
    event_type,
    COUNT(*) as event_count,
    COUNT(DISTINCT aggregate_id) as unique_aggregates,
    MIN(occurred_at) as first_occurrence,
    MAX(occurred_at) as last_occurrence
FROM yeast_events
GROUP BY event_type
ORDER BY event_count DESC;

-- =============================================================================
-- DONNÉES INITIALES (si nécessaire pour tests)
-- =============================================================================

-- Insertion d'événements de test pour les levures existantes (si la table yeasts existe déjà)
-- Ces événements seront créés automatiquement lors de la migration des données existantes

# --- !Downs

-- Suppression dans l'ordre inverse des dépendances
DROP VIEW IF EXISTS yeast_event_stats;
DROP VIEW IF EXISTS latest_yeast_events;

DROP TRIGGER IF EXISTS trigger_update_yeast_snapshot ON yeast_events;
DROP TRIGGER IF EXISTS trigger_validate_yeast_event ON yeast_events;
DROP FUNCTION IF EXISTS update_yeast_snapshot();
DROP FUNCTION IF EXISTS validate_yeast_event();

DROP INDEX IF EXISTS idx_yeast_snapshots_data;
DROP INDEX IF EXISTS idx_yeast_snapshots_created_at;
DROP INDEX IF EXISTS idx_yeast_snapshots_aggregate_version;
DROP INDEX IF EXISTS idx_yeast_snapshots_aggregate_id;

DROP INDEX IF EXISTS idx_yeast_events_metadata;
DROP INDEX IF EXISTS idx_yeast_events_event_data;
DROP INDEX IF EXISTS idx_yeast_events_aggregate_occurred;
DROP INDEX IF EXISTS idx_yeast_events_aggregate_sequence;
DROP INDEX IF EXISTS idx_yeast_events_unique_sequence;
DROP INDEX IF EXISTS idx_yeast_events_sequence_number;
DROP INDEX IF EXISTS idx_yeast_events_occurred_at;
DROP INDEX IF EXISTS idx_yeast_events_event_type;
DROP INDEX IF EXISTS idx_yeast_events_aggregate_type;
DROP INDEX IF EXISTS idx_yeast_events_aggregate_id;

DROP TABLE IF EXISTS yeast_snapshots;
DROP TABLE IF EXISTS yeast_events;