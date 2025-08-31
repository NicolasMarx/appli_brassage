#!/bin/bash

# =============================================================================
# SCRIPT DE CORRECTION RAPIDE - ERREUR COMPILATION
# =============================================================================
# Corrige l'erreur "value enzymaticCategory is not a member of DiastaticPower"
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTION RAPIDE - ERREUR enzymaticCategory${NC}"
echo ""

# =============================================================================
# CORRECTION 1 : MaltReadModel.scala
# =============================================================================

echo -e "${YELLOW}📝 Correction MaltReadModel.scala...${NC}"

# Backup du fichier actuel
cp "app/application/queries/public/malts/readmodels/MaltReadModel.scala" \
   "app/application/queries/public/malts/readmodels/MaltReadModel.scala.backup.$(date +%Y%m%d_%H%M%S)"

# Correction du fichier
cat > app/application/queries/public/malts/readmodels/MaltReadModel.scala << 'EOF'
package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

/**
 * ReadModel pour les malts (interface publique)
 * Projection optimisée pour les APIs publiques
 */
case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  characteristics: MaltCharacteristics,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  isActive: Boolean,
  qualityScore: Double,
  createdAt: Instant,
  updatedAt: Instant
)

case class MaltCharacteristics(
  ebcColor: Double,
  colorName: String,
  extractionRate: Double,
  extractionCategory: String,
  diastaticPower: Double,
  enzymaticCategory: String,  // ✅ Corrigé: utilisera enzymePowerCategory
  canSelfConvert: Boolean,
  isBaseMalt: Boolean,
  maxRecommendedPercent: Option[Double]
)

object MaltReadModel {
  
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.value,
      name = malt.name.value,
      maltType = malt.maltType.name,
      characteristics = MaltCharacteristics(
        ebcColor = malt.ebcColor.value,
        colorName = malt.ebcColor.colorName,
        extractionRate = malt.extractionRate.value,
        extractionCategory = malt.extractionRate.extractionCategory,
        diastaticPower = malt.diastaticPower.value,
        enzymaticCategory = malt.diastaticPower.enzymePowerCategory, // ✅ CORRIGÉ ICI
        canSelfConvert = malt.canSelfConvert,
        isBaseMalt = malt.isBaseMalt,
        maxRecommendedPercent = malt.maxRecommendedPercent
      ),
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      isActive = malt.isActive,
      qualityScore = malt.qualityScore,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
  
  implicit val characteristicsFormat: Format[MaltCharacteristics] = Json.format[MaltCharacteristics]
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
EOF

echo -e "${GREEN}✅ MaltReadModel.scala corrigé${NC}"

# =============================================================================
# CORRECTION 2 : Suppression des imports inutilisés
# =============================================================================

echo -e "${YELLOW}🧹 Nettoyage des imports inutilisés...${NC}"

# Correction CreateMaltCommand.scala
sed -i.bak 's/import domain.shared._/\/\/ import domain.shared._ \/\/ Unused import removed/' \
    app/application/commands/admin/malts/CreateMaltCommand.scala

# Correction UpdateMaltCommand.scala  
sed -i.bak 's/import domain.shared.NonEmptyString/\/\/ import domain.shared.NonEmptyString \/\/ Unused import removed/' \
    app/application/commands/admin/malts/UpdateMaltCommand.scala

# Correction AdminMaltListQueryHandler.scala
sed -i.bak 's/import domain.malts.model.{MaltType, MaltStatus, MaltSource}/import domain.malts.model._  \/\/ Simplified import/' \
    app/application/queries/admin/malts/handlers/AdminMaltListQueryHandler.scala

# Correction MaltListQueryHandler.scala
sed -i.bak 's/import domain.malts.model.{MaltType, MaltStatus}/import domain.malts.model._  \/\/ Simplified import/' \
    app/application/queries/public/malts/handlers/MaltListQueryHandler.scala

echo -e "${GREEN}✅ Imports inutilisés nettoyés${NC}"

# =============================================================================
# CORRECTION 3 : Corrections mineures de types
# =============================================================================

echo -e "${YELLOW}🔧 Corrections mineures...${NC}"

# Correction du widening implicite dans AdminMaltListQueryHandler
sed -i.bak 's/totalCount = adminReadModels.length,/totalCount = adminReadModels.length.toLong,/' \
    app/application/queries/admin/malts/handlers/AdminMaltListQueryHandler.scala

# Correction du widening implicite dans MaltListQueryHandler
sed -i.bak 's/totalCount = readModels.length,/totalCount = readModels.length.toLong,/' \
    app/application/queries/public/malts/handlers/MaltListQueryHandler.scala

echo -e "${GREEN}✅ Types corrigés${NC}"

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}🔍 Test de compilation...${NC}"

if sbt compile > /tmp/quick_fix_compilation.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION RÉUSSIE !${NC}"
    echo ""
    echo -e "${GREEN}🎉 Erreur corrigée avec succès !${NC}"
    echo ""
    echo -e "${BLUE}📊 Résumé des corrections :${NC}"
    echo -e "   ✅ MaltReadModel.scala : enzymaticCategory → enzymePowerCategory"
    echo -e "   ✅ Imports inutilisés supprimés"
    echo -e "   ✅ Types implicites corrigés (length.toLong)"
    echo ""
    echo -e "${GREEN}🚀 Le projet compile maintenant sans erreurs !${NC}"
    
    # Afficher seulement les warnings restants
    echo ""
    echo -e "${YELLOW}⚠️  Warnings restants (non bloquants) :${NC}"
    grep "warn" /tmp/quick_fix_compilation.log | head -5
    echo -e "${YELLOW}... (warnings peuvent être ignorés pour l'instant)${NC}"
    
else
    echo -e "${RED}❌ Erreurs persistantes${NC}"
    echo ""
    echo -e "${YELLOW}Nouvelles erreurs :${NC}"
    grep "error" /tmp/quick_fix_compilation.log
    echo ""
    echo -e "${YELLOW}Consultez le log complet : /tmp/quick_fix_compilation.log${NC}"
fi

echo ""
echo -e "${BLUE}📁 Fichiers de backup créés pour rollback si nécessaire${NC}"
echo -e "${GREEN}🔧 Correction rapide terminée !${NC}"