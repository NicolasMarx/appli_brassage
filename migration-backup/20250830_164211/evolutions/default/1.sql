# --- !Ups

CREATE TABLE aromas (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE hops (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  country TEXT NOT NULL,
  alpha_min NUMERIC,
  alpha_max NUMERIC,
  beta_min NUMERIC,
  beta_max NUMERIC,
  cohum_min NUMERIC,
  cohum_max NUMERIC,
  oils_min NUMERIC,
  oils_max NUMERIC,
  myrcene_min NUMERIC,
  myrcene_max NUMERIC,
  humulene_min NUMERIC,
  humulene_max NUMERIC,
  caryo_min NUMERIC,
  caryo_max NUMERIC,
  farnesene_min NUMERIC,
  farnesene_max NUMERIC
);

CREATE TABLE hop_aromas (
  hop_id TEXT NOT NULL REFERENCES hops(id) ON DELETE CASCADE,
  aroma_id TEXT NOT NULL REFERENCES aromas(id) ON DELETE CASCADE,
  PRIMARY KEY (hop_id, aroma_id)
);

# --- !Downs

DROP TABLE IF EXISTS hop_aromas;
DROP TABLE IF EXISTS hops;
DROP TABLE IF EXISTS aromas;
