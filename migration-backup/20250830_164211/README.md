# Appli brassage — Slick + Postgres

Intègre Play 2.9.x (Scala 2.13) avec CQRS + DDD et persistance Postgres via Slick.
- Endpoints: `/api/hops`, `/api/aromas`, `/api/reseed`, `/`
- Données seed: `conf/reseed/*.csv` (remplace par ton CSV "dernier" si besoin)

## Lancer Postgres (Docker)
```bash
docker compose up -d db
```

## Config Play (Slick)
- Par défaut, `slick.dbs.default.db` utilise `DATABASE_URL` si présent.
- Sinon, décommente dans `conf/application.conf` les lignes `url/user/password`.

## Démarrer l'app
```bash
sbt run
# Play appliquera les evolutions (conf/evolutions/default/1.sql) au premier démarrage
```

## Reseed (charger CSVs en DB)
```bash
curl -X POST http://localhost:9000/api/reseed
```

## Compiler Elm en bundle Play
```bash
cd elm
elm make src/Main.elm --optimize --output ../public/js/app.js
```
