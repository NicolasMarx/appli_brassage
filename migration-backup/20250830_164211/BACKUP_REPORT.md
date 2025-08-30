# Rapport de sauvegarde - Migration DDD/CQRS

**Date**: Sat Aug 30 16:43:47 CEST 2025
**Branche originale**: master
**Branche de sauvegarde**: backup-before-ddd-migration-20250830_164211

## Fichiers sauvegardés

- `build.sbt` - Configuration SBT originale
- `project/` - Configuration projet originale
- `conf/` - Configuration Play originale
- `old-app/` - Code Scala original
- `old-elm/` - Applications Elm originales
- `old-public/` - Ressources publiques originales
- `old-test/` - Tests originaux

## Restauration

En cas de problème, vous pouvez restaurer avec:

```bash
git checkout backup-before-ddd-migration-20250830_164211
# ou
cp -r migration-backup/20250830_164211/old-app/ app/
# etc.
```

## Structure originale

./.idea/modules/My-brew-app-V2
./app
./app/actions
./app/application
./app/application/Admin
./app/application/commands
./app/application/queries
./app/controllers
./app/controllers/admin
./app/domain
./app/domain/admin
./app/domain/aromas
./app/domain/beerstyles
./app/domain/common
./app/domain/hops
./app/infrastructure
./app/infrastructure/db
./app/modules
./app/repositories
./app/repositories/read
./app/repositories/write
./app/services
./app/services/crypto
./app/views
./migration-backup/20250830_164211/old-app
./target/streams/test
./ui/admin/elm
./ui/admin/elm-stuff
./ui/site/elm-stuff
