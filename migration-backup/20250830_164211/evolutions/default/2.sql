# --- !Ups

CREATE TABLE IF NOT EXISTS hops (
                                    id                       TEXT PRIMARY KEY,
                                    name                     TEXT NOT NULL,
                                    country                  TEXT NOT NULL,
                                    alpha_min                NUMERIC,
                                    alpha_max                NUMERIC,
                                    beta_min                 NUMERIC,
                                    beta_max                 NUMERIC,
                                    cohumulone_min           NUMERIC,
                                    cohumulone_max           NUMERIC,
                                    oils_total_ml_100g_min   NUMERIC,
                                    oils_total_ml_100g_max   NUMERIC,
                                    myrcene_min              NUMERIC,
                                    myrcene_max              NUMERIC,
                                    humulene_min             NUMERIC,
                                    humulene_max             NUMERIC,
                                    caryophyllene_min        NUMERIC,
                                    caryophyllene_max        NUMERIC,
                                    farnesene_min            NUMERIC,
                                    farnesene_max            NUMERIC
);

CREATE TABLE IF NOT EXISTS aromas (
                                      id   TEXT PRIMARY KEY,
                                      name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS hop_aromas (
                                          hop_id   TEXT NOT NULL REFERENCES hops(id)   ON DELETE CASCADE,
    aroma_id TEXT NOT NULL REFERENCES aromas(id) ON DELETE CASCADE,
    PRIMARY KEY (hop_id, aroma_id)
    );

-- En cas de schéma partiel déjà existant, on ajoute les colonnes manquantes :
ALTER TABLE hops ADD COLUMN IF NOT EXISTS oils_total_ml_100g_min NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS oils_total_ml_100g_max NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS myrcene_min NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS myrcene_max NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS humulene_min NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS humulene_max NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS caryophyllene_min NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS caryophyllene_max NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS farnesene_min NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS farnesene_max NUMERIC;

# --- !Downs

DROP TABLE IF EXISTS hop_aromas;
DROP TABLE IF EXISTS aromas;
DROP TABLE IF EXISTS hops;
