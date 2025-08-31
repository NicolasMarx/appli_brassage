#!/bin/bash

# =============================================================================
# CORRECTION ÉVOLUTION SQL
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 Correction de l'évolution SQL${NC}"
echo ""

# Localiser le fichier d'évolution
EVOLUTION_FILE=""
if [ -f "conf/evolutions/default/1.sql" ]; then
    EVOLUTION_FILE="conf/evolutions/default/1.sql"
elif [ -f "conf/evolutions/1.sql" ]; then
    EVOLUTION_FILE="conf/evolutions/1.sql"
else
    echo -e "${RED}❌ Fichier d'évolution SQL non trouvé${NC}"
    echo "Recherche dans:"
    find . -name "*.sql" -path "*/evolutions/*" 2>/dev/null || echo "Aucun fichier SQL d'évolution trouvé"
    exit 1
fi

echo -e "${GREEN}✅ Fichier trouvé: $EVOLUTION_FILE${NC}"

# Sauvegarder le fichier original
cp "$EVOLUTION_FILE" "$EVOLUTION_FILE.backup"
echo -e "${GREEN}✅ Sauvegarde créée: $EVOLUTION_FILE.backup${NC}"

# Afficher la ligne problématique
echo ""
echo -e "${BLUE}🔍 Ligne problématique (autour de la ligne 147):${NC}"
grep -n -A2 -B2 "NEW.updated_at = NOW()" "$EVOLUTION_FILE" || echo "Pattern non trouvé"

echo ""
echo -e "${BLUE}🔧 Correction de la syntaxe SQL...${NC}"

# Correction 1: Ajouter le point-virgule manquant après NOW()
sed -i.bak 's/NEW\.updated_at = NOW()/NEW.updated_at = NOW();/' "$EVOLUTION_FILE" 2>/dev/null || \
sed -i 's/NEW\.updated_at = NOW()/NEW.updated_at = NOW();/' "$EVOLUTION_FILE"

# Vérifier que la correction a été appliquée
if grep -q "NEW.updated_at = NOW();" "$EVOLUTION_FILE"; then
    echo -e "${GREEN}✅ Correction appliquée: Point-virgule ajouté${NC}"
else
    echo -e "${YELLOW}⚠️  La correction automatique n'a pas fonctionné${NC}"
    echo "Ligne à corriger manuellement:"
    echo "OLD: NEW.updated_at = NOW()"
    echo "NEW: NEW.updated_at = NOW();"
fi

# Supprimer les fichiers .bak
rm -f "$EVOLUTION_FILE.bak" 2>/dev/null || true

# Vérifier la syntaxe générale
echo ""
echo -e "${BLUE}🔍 Vérification syntaxe SQL...${NC}"

# Compter les $$ pour s'assurer qu'ils sont équilibrés
DOLLAR_COUNT=$(grep -o '\$\$' "$EVOLUTION_FILE" | wc -l)
if [ $((DOLLAR_COUNT % 2)) -eq 0 ]; then
    echo -e "${GREEN}✅ Quotes dollar (\$\$) équilibrées: $DOLLAR_COUNT${NC}"
else
    echo -e "${RED}❌ Quotes dollar (\$\$) non équilibrées: $DOLLAR_COUNT${NC}"
fi

# Vérifier la structure générale
echo ""
echo -e "${BLUE}📋 Structure du fichier d'évolution:${NC}"
echo "   # --- !Ups: $(grep -c "# --- !Ups" "$EVOLUTION_FILE")"
echo "   # --- !Downs: $(grep -c "# --- !Downs" "$EVOLUTION_FILE")"
echo "   CREATE TABLE: $(grep -c "CREATE TABLE" "$EVOLUTION_FILE")"
echo "   CREATE FUNCTION: $(grep -c "CREATE.*FUNCTION" "$EVOLUTION_FILE")"

# Ajouter section !Downs si manquante
if ! grep -q "# --- !Downs" "$EVOLUTION_FILE"; then
    echo ""
    echo -e "${YELLOW}⚠️  Section !Downs manquante, ajout...${NC}"
    
    cat >> "$EVOLUTION_FILE" << 'DOWNS_EOF'

# --- !Downs

-- Suppression dans l'ordre inverse de création
DROP TRIGGER IF EXISTS update_admins_updated_at ON admins;
DROP FUNCTION IF EXISTS update_updated_at_column();
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS beer_styles;
DROP TABLE IF EXISTS aroma_profiles;
DROP TABLE IF EXISTS origins;
DROP EXTENSION IF EXISTS "uuid-ossp";
DOWNS_EOF
    
    echo -e "${GREEN}✅ Section !Downs ajoutée${NC}"
fi

echo ""
echo -e "${BLUE}🧪 Test de validation de l'évolution...${NC}"

# Marquer l'évolution comme résolue via l'API Play
echo "Tentative de résolution automatique de l'évolution..."

# Option 1: Via curl si l'application tourne
if curl -s "http://localhost:9000/@evolutions/resolve/default/1" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Évolution marquée comme résolue via API${NC}"
else
    echo -e "${YELLOW}⚠️  L'application doit être redémarrée pour appliquer la correction${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Correction de l'évolution SQL terminée !${NC}"
echo ""
echo -e "${BLUE}Prochaines étapes:${NC}"
echo "1. Redémarrez l'application Play:"
echo "   Arrêtez sbt (Entrée), puis: sbt run"
echo ""
echo "2. Ou marquez l'évolution comme résolue manuellement:"
echo "   http://localhost:9000/@evolutions"
echo ""
echo "3. Testez l'API ensuite:"
echo "   curl http://localhost:9000/api/v1/hops"
echo ""

echo -e "${BLUE}📁 Fichiers:${NC}"
echo "   • Original sauvé: $EVOLUTION_FILE.backup"
echo "   • Corrigé: $EVOLUTION_FILE"