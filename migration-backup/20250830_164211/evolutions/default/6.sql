# --- !Ups

-- Table des administrateurs (auth par session côté Play, mots de passe hachés Argon2)
CREATE TABLE IF NOT EXISTS admins (
                                      id            varchar(64)  PRIMARY KEY,
    email         varchar(255) UNIQUE NOT NULL,
    first_name    varchar(100) NOT NULL,
    last_name     varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_admins_email ON admins (email);

-- (Optionnel) seed DEV : décommente et remplace <HASH_ARGON2>
-- INSERT INTO admins (id, email, first_name, last_name, password_hash)
-- VALUES ('00000000-0000-0000-0000-000000000001', 'admin@local', 'Admin', 'Local', '<HASH_ARGON2>');

# --- !Downs

DROP INDEX IF EXISTS ux_admins_email;
DROP TABLE IF EXISTS admins;
