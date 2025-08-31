-- Mise à jour de la colonne flavor_profiles pour être compatible avec Slick
-- Convertir de TEXT[] à TEXT (JSON ou CSV)

-- Vérifier le type actuel
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'malts' AND column_name = 'flavor_profiles';

-- Sauvegarder les données actuelles si elles existent
CREATE TEMP TABLE malts_flavor_backup AS 
SELECT id, flavor_profiles FROM malts WHERE flavor_profiles IS NOT NULL;

-- Supprimer et recréer la colonne avec le bon type
ALTER TABLE malts DROP COLUMN IF EXISTS flavor_profiles;
ALTER TABLE malts ADD COLUMN flavor_profiles TEXT;

-- Restaurer les données sous forme JSON
UPDATE malts 
SET flavor_profiles = (
    SELECT '["' || array_to_string(b.flavor_profiles, '","') || '"]'
    FROM malts_flavor_backup b 
    WHERE b.id = malts.id
)
WHERE id IN (SELECT id FROM malts_flavor_backup);

-- Nettoyer
DROP TABLE malts_flavor_backup;
