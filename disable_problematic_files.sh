#!/bin/bash

# Script pour désactiver temporairement les fichiers qui empêchent la compilation

echo "Désactivation des fichiers problématiques pour Phase 1..."

# Créer dossier temporaire
mkdir -p temp_disabled

# Déplacer les fichiers qui causent des erreurs
mv app/actions/AdminApiSecuredAction.scala temp_disabled/ 2>/dev/null || true
mv app/actions/AdminSecuredAction.scala temp_disabled/ 2>/dev/null || true
mv app/application/Admin/AdminBootstrapService.scala temp_disabled/ 2>/dev/null || true
mv app/application/commands/Reseed.scala temp_disabled/ 2>/dev/null || true
mv app/application/queries/ListAromas.scala temp_disabled/ 2>/dev/null || true
mv app/application/queries/ListBeerStyles.scala temp_disabled/ 2>/dev/null || true
mv app/application/queries/ListHops.scala temp_disabled/ 2>/dev/null || true
mv app/views/admin.scala.html temp_disabled/ 2>/dev/null || true
mv app/views/index.scala.html temp_disabled/ 2>/dev/null || true

echo "Fichiers désactivés. Ils seront remis en place progressivement."
echo "Fichiers dans temp_disabled/:"
ls temp_disabled/ 2>/dev/null || echo "Aucun fichier déplacé"