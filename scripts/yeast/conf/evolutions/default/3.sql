# --- !Ups

-- Table principale des levures
CREATE TABLE yeasts (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  laboratory VARCHAR(100) NOT NULL,
  strain VARCHAR(50) NOT NULL,
  yeast_type VARCHAR(20) NOT NULL,
  attenuation_min INTEGER NOT NULL CHECK (attenuation_min >= 30 AND attenuation_min <= 100),
  attenuation_max INTEGER NOT NULL CHECK (attenuation_max >= 30 AND attenuation_max <= 100),
  temperature_min INTEGER NOT NULL CHECK (temperature_min >= 0 AND temperature_min <= 50),
  temperature_max INTEGER NOT NULL CHECK (temperature_max >= 0 AND temperature_max <= 50),
  alcohol_tolerance DECIMAL(4,1) NOT NULL CHECK (alcohol_tolerance >= 0 AND alcohol_tolerance <= 20),
  flocculation VARCHAR(20) NOT NULL,
  characteristics TEXT NOT NULL, -- JSON
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  
  -- Contraintes
  CONSTRAINT chk_attenuation_range CHECK (attenuation_min <= attenuation_max),
  CONSTRAINT chk_temperature_range CHECK (temperature_min <= temperature_max),
  CONSTRAINT chk_yeast_type CHECK (yeast_type IN ('Ale', 'Lager', 'Wheat', 'Saison', 'Wild', 'Sour', 'Champagne', 'Kveik')),
  CONSTRAINT chk_flocculation CHECK (flocculation IN ('Low', 'Medium', 'Medium-High', 'High', 'Very High')),
  CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED', 'DRAFT', 'ARCHIVED')),
  CONSTRAINT uk_laboratory_strain UNIQUE (laboratory, strain)
);

-- Table des événements pour Event Sourcing
CREATE TABLE yeast_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  yeast_id UUID NOT NULL REFERENCES yeasts(id),
  event_type VARCHAR(100) NOT NULL,
  event_data TEXT NOT NULL, -- JSON
  version BIGINT NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  created_by UUID, -- Référence vers admin qui a créé l'event
  
  -- Contrainte d'unicité pour éviter les doublons
  CONSTRAINT uk_yeast_version UNIQUE (yeast_id, version)
);

-- Index pour optimiser les performances
CREATE INDEX idx_yeasts_status ON yeasts(status);
CREATE INDEX idx_yeasts_type ON yeasts(yeast_type);
CREATE INDEX idx_yeasts_laboratory ON yeasts(laboratory);
CREATE INDEX idx_yeasts_attenuation ON yeasts(attenuation_min, attenuation_max);
CREATE INDEX idx_yeasts_temperature ON yeasts(temperature_min, temperature_max);
CREATE INDEX idx_yeasts_alcohol_tolerance ON yeasts(alcohol_tolerance);
CREATE INDEX idx_yeasts_name ON yeasts(LOWER(name));
CREATE INDEX idx_yeasts_characteristics ON yeasts USING gin(to_tsvector('english', characteristics));

CREATE INDEX idx_yeast_events_yeast_id ON yeast_events(yeast_id);
CREATE INDEX idx_yeast_events_yeast_id_version ON yeast_events(yeast_id, version);
CREATE INDEX idx_yeast_events_occurred_at ON yeast_events(occurred_at);
CREATE INDEX idx_yeast_events_type ON yeast_events(event_type);

-- Vues pour faciliter les requêtes
CREATE VIEW active_yeasts AS
SELECT * FROM yeasts WHERE status = 'ACTIVE';

CREATE VIEW yeast_stats AS
SELECT 
  laboratory,
  yeast_type,
  COUNT(*) as count,
  AVG((attenuation_min + attenuation_max) / 2.0) as avg_attenuation,
  AVG((temperature_min + temperature_max) / 2.0) as avg_temperature,
  AVG(alcohol_tolerance) as avg_alcohol_tolerance
FROM yeasts 
WHERE status = 'ACTIVE'
GROUP BY laboratory, yeast_type;

-- Données de test (optionnel - à supprimer en production)
INSERT INTO yeasts (
  id, name, laboratory, strain, yeast_type,
  attenuation_min, attenuation_max, temperature_min, temperature_max,
  alcohol_tolerance, flocculation, characteristics, status,
  version, created_at, updated_at
) VALUES 
(
  gen_random_uuid(),
  'SafSpirit English Ale',
  'Fermentis',
  'S-04',
  'Ale',
  75, 82, 15, 24,
  9.0, 'High',
  '{"aromaProfile":["Clean","Neutral","Fruity"],"flavorProfile":["Balanced","Smooth"],"esters":["Ethyl acetate"],"phenols":[],"otherCompounds":[],"notes":"Levure anglaise classique, idéale pour débutants"}',
  'ACTIVE',
  1, NOW(), NOW()
),
(
  gen_random_uuid(),
  'SafSpirit American Ale',
  'Fermentis',
  'S-05',
  'Ale',
  78, 85, 15, 25,
  10.0, 'Medium',
  '{"aromaProfile":["Clean","Citrus","American"],"flavorProfile":["Crisp","Bright"],"esters":["Low esters"],"phenols":[],"otherCompounds":[],"notes":"Levure américaine polyvalente pour IPA et Pale Ale"}',
  'ACTIVE',
  1, NOW(), NOW()
),
(
  gen_random_uuid(),
  'Saflager West European',
  'Fermentis', 
  'S-23',
  'Lager',
  80, 88, 9, 15,
  11.0, 'High',
  '{"aromaProfile":["Clean","Neutral","Lager"],"flavorProfile":["Crisp","Clean","Smooth"],"esters":[],"phenols":[],"otherCompounds":[],"notes":"Levure lager européenne pour pilsners et lagers propres"}',
  'ACTIVE',
  1, NOW(), NOW()
);

# --- !Downs

DROP VIEW IF EXISTS yeast_stats;
DROP VIEW IF EXISTS active_yeasts;
DROP TABLE IF EXISTS yeast_events;
DROP TABLE IF EXISTS yeasts;
