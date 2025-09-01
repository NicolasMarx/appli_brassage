-- Évolution base de données - Tables malts
# --- !Ups

-- =============================================================================
-- ÉVOLUTION 10 - DOMAINE MALTS
-- Tables pour gestion complète des malts avec métadonnées IA
-- Suit le pattern des tables hops pour cohérence architecturale
-- =============================================================================

-- Table principale des malts
CREATE TABLE malts (
                       id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       name                VARCHAR(200) NOT NULL,
                       malt_type           VARCHAR(50) NOT NULL CHECK (malt_type IN ('BASE', 'CRYSTAL', 'ROASTED', 'SPECIALTY', 'ADJUNCT')),
                       ebc_color           DECIMAL(6,2) NOT NULL CHECK (ebc_color >= 0 AND ebc_color <= 1500),
                       extraction_rate     DECIMAL(5,2) NOT NULL CHECK (extraction_rate >= 50.0 AND extraction_rate <= 90.0),
                       diastatic_power     DECIMAL(6,2) NOT NULL CHECK (diastatic_power >= 0 AND diastatic_power <= 200),
                       origin_code         VARCHAR(10) NOT NULL REFERENCES origins(id),
                       description         TEXT,
                       status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED', 'SEASONAL', 'OUT_OF_STOCK')),
                       source              VARCHAR(50) NOT NULL CHECK (source IN ('MANUAL', 'AI_DISCOVERED', 'IMPORT', 'MANUFACTURER', 'COMMUNITY')),
                       credibility_score   INTEGER NOT NULL DEFAULT 70 CHECK (credibility_score >= 0 AND credibility_score <= 100),
                       flavor_profiles     JSONB DEFAULT '[]'::jsonb,
                       created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       version             INTEGER NOT NULL DEFAULT 1
);

-- Index pour performances
CREATE INDEX idx_malts_name ON malts(name);
CREATE INDEX idx_malts_malt_type ON malts(malt_type);
CREATE INDEX idx_malts_ebc_color ON malts(ebc_color);
CREATE INDEX idx_malts_extraction_rate ON malts(extraction_rate);
CREATE INDEX idx_malts_diastatic_power ON malts(diastatic_power);
CREATE INDEX idx_malts_origin_code ON malts(origin_code);
CREATE INDEX idx_malts_status ON malts(status);
CREATE INDEX idx_malts_source ON malts(source);
CREATE INDEX idx_malts_credibility_score ON malts(credibility_score);
CREATE INDEX idx_malts_created_at ON malts(created_at);
CREATE INDEX idx_malts_updated_at ON malts(updated_at);

-- Index composite pour recherches courantes
CREATE INDEX idx_malts_type_color ON malts(malt_type, ebc_color);
CREATE INDEX idx_malts_active_type ON malts(status, malt_type) WHERE status IN ('ACTIVE', 'SEASONAL');
CREATE INDEX idx_malts_base_extraction ON malts(extraction_rate, diastatic_power) WHERE malt_type = 'BASE';

-- Index GIN pour recherche dans flavor_profiles JSONB
CREATE INDEX idx_malts_flavor_profiles ON malts USING GIN (flavor_profiles);

-- =============================================================================
-- TABLE RELATIONS MALTS-STYLES DE BIÈRE
-- =============================================================================

-- Table de compatibilité malts-styles de bière (many-to-many)
CREATE TABLE malt_beer_styles (
                                  malt_id             UUID NOT NULL REFERENCES malts(id) ON DELETE CASCADE,
                                  beer_style_id       VARCHAR(50) NOT NULL REFERENCES beer_styles(id) ON DELETE CASCADE,
                                  compatibility_score INTEGER CHECK (compatibility_score >= 0 AND compatibility_score <= 100),
                                  typical_percentage  DECIMAL(5,2) CHECK (typical_percentage >= 0.0 AND typical_percentage <= 100.0),
                                  created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                  PRIMARY KEY (malt_id, beer_style_id)
);

CREATE INDEX idx_malt_beer_styles_compatibility ON malt_beer_styles(compatibility_score);
CREATE INDEX idx_malt_beer_styles_percentage ON malt_beer_styles(typical_percentage);

-- =============================================================================
-- TABLE SUBSTITUTIONS MALTS
-- =============================================================================

-- Table des substitutions possibles entre malts
CREATE TABLE malt_substitutions (
                                    original_malt_id    UUID NOT NULL REFERENCES malts(id) ON DELETE CASCADE,
                                    substitute_malt_id  UUID NOT NULL REFERENCES malts(id) ON DELETE CASCADE,
                                    substitution_ratio  DECIMAL(4,2) NOT NULL DEFAULT 1.00 CHECK (substitution_ratio > 0),
                                    compatibility_notes TEXT,
                                    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                    PRIMARY KEY (original_malt_id, substitute_malt_id),
                                    CHECK (original_malt_id != substitute_malt_id)
    );

CREATE INDEX idx_malt_substitutions_ratio ON malt_substitutions(substitution_ratio);

-- =============================================================================
-- TABLE SPÉCIFICATIONS TECHNIQUES DÉTAILLÉES
-- =============================================================================

-- Table pour spécifications techniques avancées (optionnel)
CREATE TABLE malt_technical_specs (
                                      malt_id             UUID PRIMARY KEY REFERENCES malts(id) ON DELETE CASCADE,
    -- Composition chimique
                                      moisture_percent    DECIMAL(4,2) CHECK (moisture_percent >= 0 AND moisture_percent <= 20),
                                      protein_percent     DECIMAL(4,2) CHECK (protein_percent >= 0 AND protein_percent <= 30),
    -- Propriétés physiques
                                      coarse_fine_diff    DECIMAL(4,2) CHECK (coarse_fine_diff >= 0 AND coarse_fine_diff <= 10),
                                      friability_percent  DECIMAL(4,2) CHECK (friability_percent >= 0 AND friability_percent <= 100),
    -- Enzymes
                                      alpha_amylase       INTEGER CHECK (alpha_amylase >= 0),
                                      kolbach_index       INTEGER CHECK (kolbach_index >= 0 AND kolbach_index <= 100),
    -- Métadonnées
                                      lab_analysis_date   DATE,
                                      analysis_method     VARCHAR(100),
                                      certified_organic   BOOLEAN DEFAULT FALSE,
                                      created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =============================================================================
-- TABLE AUDIT MALTS (Pattern audit_logs)
-- =============================================================================

-- Extension de la table audit_logs pour malts (utilise table existante)
-- Les changements malts seront loggés dans audit_logs avec resource_type = 'MALT'

-- =============================================================================
-- CONTRAINTES ET TRIGGERS
-- =============================================================================

-- Trigger pour mise à jour automatique updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_malts_updated_at
    BEFORE UPDATE ON malts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_malt_technical_specs_updated_at
    BEFORE UPDATE ON malt_technical_specs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- DONNÉES DE RÉFÉRENCE INITIALES
-- =============================================================================

-- Insertion de malts de référence pour tests et démo
INSERT INTO malts (
    id, name, malt_type, ebc_color, extraction_rate, diastatic_power,
    origin_code, description, source, credibility_score, flavor_profiles
) VALUES
      (
          uuid_generate_v4(),
          'Pilsner Malt',
          'BASE',
          3.5,
          82.0,
          105.0,
          'DE',
          'Malt de base traditionnel allemand, base idéale pour lagers claires',
          'MANUAL',
          95,
          '["Céréale", "Biscuit léger", "Miel"]'::jsonb
      ),
      (
          uuid_generate_v4(),
          'Munich Malt',
          'BASE',
          15.0,
          80.0,
          45.0,
          'DE',
          'Malt de base allemand, apporte couleur dorée et saveurs maltées',
          'MANUAL',
          95,
          '["Malté", "Pain grillé", "Noisette"]'::jsonb
      ),
      (
          uuid_generate_v4(),
          'Crystal 60L',
          'CRYSTAL',
          118.0,
          72.0,
          0.0,
          'US',
          'Malt caramel medium, ajoute couleur ambrée et douceur',
          'MANUAL',
          90,
          '["Caramel", "Toffee", "Biscuit"]'::jsonb
      ),
      (
          uuid_generate_v4(),
          'Chocolate Malt',
          'ROASTED',
          900.0,
          68.0,
          0.0,
          'UK',
          'Malt torréfié, apporte couleur foncée et notes chocolat',
          'MANUAL',
          90,
          '["Chocolat", "Café", "Noisette grillée"]'::jsonb
      ),
      (
          uuid_generate_v4(),
          'Wheat Malt',
          'SPECIALTY',
          4.0,
          83.0,
          120.0,
          'DE',
          'Malt de blé allemand, améliore mousse et texture',
          'MANUAL',
          92,
          '["Blé", "Levure", "Biscuit tendre"]'::jsonb
      );

-- =============================================================================
-- VUES POUR REQUÊTES COURANTES
-- =============================================================================

-- Vue malts actifs avec informations complètes
CREATE VIEW active_malts AS
SELECT
    m.*,
    o.name as origin_name,
    o.region as origin_region,
    (SELECT COUNT(*) FROM malt_beer_styles mbs WHERE mbs.malt_id = m.id) as compatible_styles_count
FROM malts m
         JOIN origins o ON m.origin_code = o.id
WHERE m.status IN ('ACTIVE', 'SEASONAL');

-- Vue statistiques malts par type
CREATE VIEW malt_stats_by_type AS
SELECT
    malt_type,
    COUNT(*) as total_count,
    COUNT(*) FILTER (WHERE status IN ('ACTIVE', 'SEASONAL')) as active_count,
    AVG(ebc_color) as avg_ebc_color,
    AVG(extraction_rate) as avg_extraction_rate,
    AVG(diastatic_power) as avg_diastatic_power,
    AVG(credibility_score) as avg_credibility_score
FROM malts
GROUP BY malt_type;

# --- !Downs

-- Suppression dans l'ordre inverse des dépendances
DROP VIEW IF EXISTS malt_stats_by_type;
DROP VIEW IF EXISTS active_malts;

DROP TRIGGER IF EXISTS trigger_malt_technical_specs_updated_at ON malt_technical_specs;
DROP TRIGGER IF EXISTS trigger_malts_updated_at ON malts;
DROP FUNCTION IF EXISTS update_updated_at_column();

DROP INDEX IF EXISTS idx_malt_substitutions_ratio;
DROP INDEX IF EXISTS idx_malt_beer_styles_percentage;
DROP INDEX IF EXISTS idx_malt_beer_styles_compatibility;
DROP INDEX IF EXISTS idx_malts_flavor_profiles;
DROP INDEX IF EXISTS idx_malts_base_extraction;
DROP INDEX IF EXISTS idx_malts_active_type;
DROP INDEX IF EXISTS idx_malts_type_color;
DROP INDEX IF EXISTS idx_malts_updated_at;
DROP INDEX IF EXISTS idx_malts_created_at;
DROP INDEX IF EXISTS idx_malts_credibility_score;
DROP INDEX IF EXISTS idx_malts_source;
DROP INDEX IF EXISTS idx_malts_status;
DROP INDEX IF EXISTS idx_malts_origin_code;
DROP INDEX IF EXISTS idx_malts_diastatic_power;
DROP INDEX IF EXISTS idx_malts_extraction_rate;
DROP INDEX IF EXISTS idx_malts_ebc_color;
DROP INDEX IF EXISTS idx_malts_malt_type;
DROP INDEX IF EXISTS idx_malts_name;

DROP TABLE IF EXISTS malt_technical_specs;
DROP TABLE IF EXISTS malt_substitutions;
DROP TABLE IF EXISTS malt_beer_styles;
DROP TABLE IF EXISTS malts;