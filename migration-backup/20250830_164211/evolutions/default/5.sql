# --- !Ups

-- Table canonique des styles
CREATE TABLE IF NOT EXISTS beer_styles (
                                           id   TEXT PRIMARY KEY,
                                           name TEXT NOT NULL
);

-- Migration de hop_beer_styles (texte -> style_id) avec bloc PL/pgSQL
DO $mig$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'hop_beer_styles' AND column_name = 'style_id'
  ) THEN

    -- En cas d'exécution partielle précédente, ne pas échouer si la table temporaire existe déjà
CREATE TABLE IF NOT EXISTS hop_beer_styles_new (
                                                   hop_id   TEXT NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
    style_id TEXT NOT NULL REFERENCES beer_styles(id) ON DELETE CASCADE,
    PRIMARY KEY (hop_id, style_id)
    );

-- Peupler beer_styles depuis les valeurs existantes (style en texte)
INSERT INTO beer_styles (id, name)
SELECT DISTINCT
    lower(regexp_replace(trim(style), '[^a-z0-9]+', '-', 'g')) AS id,
    trim(style) AS name
FROM hop_beer_styles
WHERE style IS NOT NULL AND trim(style) <> ''
    ON CONFLICT (id) DO NOTHING;

-- Copier les liaisons avec style_id
INSERT INTO hop_beer_styles_new (hop_id, style_id)
SELECT
    hop_id,
    lower(regexp_replace(trim(style), '[^a-z0-9]+', '-', 'g')) AS style_id
FROM hop_beer_styles
WHERE style IS NOT NULL AND trim(style) <> '';

-- Remplacer l'ancienne table
DROP TABLE hop_beer_styles;
ALTER TABLE hop_beer_styles_new RENAME TO hop_beer_styles;

END IF;
END
$mig$ LANGUAGE plpgsql;

# --- !Downs

DO $mig$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'hop_beer_styles' AND column_name = 'style_id'
  ) THEN
CREATE TABLE IF NOT EXISTS hop_beer_styles_old (
                                                   hop_id TEXT NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
    style  TEXT NOT NULL
    );

INSERT INTO hop_beer_styles_old (hop_id, style)
SELECT hbs.hop_id, bs.name
FROM hop_beer_styles hbs
         JOIN beer_styles bs ON bs.id = hbs.style_id;

DROP TABLE hop_beer_styles;
ALTER TABLE hop_beer_styles_old RENAME TO hop_beer_styles;
END IF;

DROP TABLE IF EXISTS beer_styles;
END
$mig$ LANGUAGE plpgsql;

      # --- !Ups
-- Migration réalisée manuellement (voir scripts d'intervention).
-- No-op pour éviter un nouvel échec côté Play.
SELECT 1;

# --- !Downs
SELECT 1;
