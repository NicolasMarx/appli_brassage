-- conf/evolutions/default/2.sql
# --- !Ups

-- =============================================================================
-- SCHEMA HOPS - ARCHITECTURE DDD/CQRS
-- Tables houblons avec métadonnées IA et scoring crédibilité
-- =============================================================================

-- Table principale des houblons
CREATE TABLE hops (
                      id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                      name              VARCHAR(100) NOT NULL,
                      alpha_acid        DECIMAL(4,2) NOT NULL CHECK (alpha_acid >= 0.0 AND alpha_acid <= 25.0),
                      beta_acid         DECIMAL(4,2) CHECK (beta_acid >= 0.0 AND beta_acid <= 25.0),
                      origin_code       VARCHAR(10) NOT NULL REFERENCES origins(id),
                      usage             VARCHAR(20) NOT NULL CHECK (usage IN ('BITTERING', 'AROMA', 'DUAL_PURPOSE', 'NOBLE', 'FLAVORING')),
    description       TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'PENDING_REVIEW')),
    source            VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'AI_DISCOVERY', 'IMPORT')),
    credibility_score INTEGER NOT NULL DEFAULT 95 CHECK (credibility_score >= 0 AND credibility_score <= 100),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version           INTEGER NOT NULL DEFAULT 1
);

-- Index pour performances
CREATE INDEX idx_hops_name ON hops(name);
CREATE INDEX idx_hops_origin ON hops(origin_code);
CREATE INDEX idx_hops_usage ON hops(usage);
CREATE INDEX idx_hops_status ON hops(status);
CREATE INDEX idx_hops_alpha_acid ON hops(alpha_acid);
CREATE INDEX idx_hops_source ON hops(source);
CREATE INDEX idx_hops_credibility ON hops(credibility_score);

-- Table de liaison houblons <-> profils aromatiques
CREATE TABLE hop_aroma_profiles (
                                    hop_id         UUID NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
                                    aroma_id       VARCHAR(50) NOT NULL REFERENCES aroma_profiles(id) ON DELETE CASCADE,
                                    intensity      INTEGER DEFAULT 5 CHECK (intensity >= 1 AND intensity <= 10),
                                    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                    PRIMARY KEY (hop_id, aroma_id)
);

-- Index pour performances liaison arômes
CREATE INDEX idx_hop_aroma_hop_id ON hop_aroma_profiles(hop_id);
CREATE INDEX idx_hop_aroma_aroma_id ON hop_aroma_profiles(aroma_id);

-- Table de liaison houblons <-> styles de bière recommandés
CREATE TABLE hop_beer_style_recommendations (
                                                hop_id         UUID NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
                                                style_id       VARCHAR(50) NOT NULL REFERENCES beer_styles(id) ON DELETE CASCADE,
                                                suitability    INTEGER DEFAULT 8 CHECK (suitability >= 1 AND suitability <= 10),
                                                notes          TEXT,
                                                created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                                PRIMARY KEY (hop_id, style_id)
);

-- Index pour performances liaison styles
CREATE INDEX idx_hop_style_hop_id ON hop_beer_style_recommendations(hop_id);
CREATE INDEX idx_hop_style_style_id ON hop_beer_style_recommendations(style_id);

-- Table des substituts de houblons
CREATE TABLE hop_substitutes (
                                 primary_hop_id    UUID NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
                                 substitute_hop_id UUID NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
                                 similarity_score  INTEGER DEFAULT 8 CHECK (similarity_score >= 1 AND similarity_score <= 10),
                                 notes            TEXT,
                                 created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                 PRIMARY KEY (primary_hop_id, substitute_hop_id),
                                 CHECK (primary_hop_id != substitute_hop_id)
    );

-- Index pour performances substituts
CREATE INDEX idx_hop_substitutes_primary ON hop_substitutes(primary_hop_id);
CREATE INDEX idx_hop_substitutes_substitute ON hop_substitutes(substitute_hop_id);

-- =============================================================================
-- FONCTIONS UTILITAIRES HOUBLONS
-- =============================================================================

-- Trigger pour mise à jour automatique updated_at
CREATE TRIGGER update_hops_updated_at
    BEFORE UPDATE ON hops
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Fonction pour calculer l'usage suggéré basé sur alpha acid
CREATE OR REPLACE FUNCTION suggest_hop_usage(alpha_acid_value DECIMAL)
RETURNS TEXT AS $$
BEGIN
    IF alpha_acid_value > 12.0 THEN
        RETURN 'BITTERING';
    ELSIF alpha_acid_value >= 6.0 THEN
        RETURN 'DUAL_PURPOSE';
ELSE
        RETURN 'AROMA';
END IF;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- DONNÉES INITIALES HOUBLONS
-- =============================================================================

-- Quelques houblons de référence pour démarrer
INSERT INTO hops (name, alpha_acid, beta_acid, origin_code, usage, description, status, source, credibility_score) VALUES
                                                                                                                       ('Cascade', 5.5, 4.8, 'US', 'AROMA', 'Houblon américain iconique aux arômes d''agrumes et floraux', 'ACTIVE', 'MANUAL', 98),
                                                                                                                       ('Centennial', 9.5, 3.8, 'US', 'DUAL_PURPOSE', 'Houblon polyvalent avec des notes d''agrumes et de pin', 'ACTIVE', 'MANUAL', 98),
                                                                                                                       ('Chinook', 13.0, 3.5, 'US', 'BITTERING', 'Houblon amérisant avec des arômes épicés et d''agrumes', 'ACTIVE', 'MANUAL', 98),
                                                                                                                       ('Saaz', 3.2, 4.2, 'CZ', 'NOBLE', 'Houblon noble tchèque aux arômes fins et épicés', 'ACTIVE', 'MANUAL', 99),
                                                                                                                       ('Hallertau Mittelfrüh', 3.8, 4.5, 'DE', 'NOBLE', 'Houblon noble allemand traditionnel', 'ACTIVE', 'MANUAL', 99),
                                                                                                                       ('Galaxy', 14.2, 5.8, 'AU', 'DUAL_PURPOSE', 'Houblon australien aux arômes tropicaux intenses', 'ACTIVE', 'MANUAL', 97);

-- Associer quelques profils aromatiques
INSERT INTO hop_aroma_profiles (hop_id, aroma_id, intensity)
SELECT h.id, 'citrus', 9 FROM hops h WHERE h.name = 'Cascade'
UNION ALL
SELECT h.id, 'floral', 7 FROM hops h WHERE h.name = 'Cascade'
UNION ALL
SELECT h.id, 'citrus', 8 FROM hops h WHERE h.name = 'Centennial'
UNION ALL
SELECT h.id, 'piney', 6 FROM hops h WHERE h.name = 'Centennial'
UNION ALL
SELECT h.id, 'spicy', 8 FROM hops h WHERE h.name = 'Saaz'
UNION ALL
SELECT h.id, 'herbal', 6 FROM hops h WHERE h.name = 'Saaz'
UNION ALL
SELECT h.id, 'tropical', 10 FROM hops h WHERE h.name = 'Galaxy'
UNION ALL
SELECT h.id, 'citrus', 8 FROM hops h WHERE h.name = 'Galaxy';

-- Recommandations styles de bière
INSERT INTO hop_beer_style_recommendations (hop_id, style_id, suitability, notes)
SELECT h.id, 'pale-ale', 10, 'Parfait pour les Pale Ales américaines' FROM hops h WHERE h.name = 'Cascade'
UNION ALL
SELECT h.id, 'ipa', 9, 'Classique pour les IPAs' FROM hops h WHERE h.name = 'Cascade'
UNION ALL
SELECT h.id, 'ipa', 10, 'Reference pour les IPAs américaines' FROM hops h WHERE h.name = 'Centennial'
UNION ALL
SELECT h.id, 'pils', 10, 'Houblon signature des Pilsners tchèques' FROM hops h WHERE h.name = 'Saaz'
UNION ALL
SELECT h.id, 'lager', 9, 'Excellent pour les lagers allemandes' FROM hops h WHERE h.name = 'Hallertau Mittelfrüh'
UNION ALL
SELECT h.id, 'ipa', 8, 'Apporte des arômes tropicaux uniques aux IPAs' FROM hops h WHERE h.name = 'Galaxy';

# --- !Downs

DROP TABLE IF EXISTS hop_substitutes;
DROP TABLE IF EXISTS hop_beer_style_recommendations;
DROP TABLE IF EXISTS hop_aroma_profiles;
DROP TABLE IF EXISTS hops;

DROP FUNCTION IF EXISTS suggest_hop_usage(DECIMAL);