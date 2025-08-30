-- !Ups

-- Ajoute les colonnes manquantes en toute sécurité
ALTER TABLE hops ADD COLUMN IF NOT EXISTS cohumulone_min NUMERIC;
ALTER TABLE hops ADD COLUMN IF NOT EXISTS cohumulone_max NUMERIC;

-- En "sécurité", on re-tente aussi les autres colonnes au cas où
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

-- !Downs

ALTER TABLE hops DROP COLUMN IF EXISTS cohumulone_min;
ALTER TABLE hops DROP COLUMN IF EXISTS cohumulone_max;
-- On ne drop pas les autres “sécurités” pour éviter de casser un schéma valide
