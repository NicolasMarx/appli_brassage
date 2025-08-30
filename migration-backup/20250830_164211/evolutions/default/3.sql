# --- !Ups

CREATE TABLE IF NOT EXISTS hop_details (
                                           hop_id TEXT PRIMARY KEY REFERENCES hops(id) ON DELETE CASCADE,
    description TEXT NULL,
    hop_type TEXT NULL
    );

CREATE TABLE IF NOT EXISTS hop_beer_styles (
                                               hop_id TEXT NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
    style  TEXT NOT NULL
    );

CREATE TABLE IF NOT EXISTS hop_substitutes (
                                               hop_id TEXT NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
    name   TEXT NOT NULL
    );

# --- !Downs

DROP TABLE IF EXISTS hop_substitutes;
DROP TABLE IF EXISTS hop_beer_styles;
DROP TABLE IF EXISTS hop_details;
