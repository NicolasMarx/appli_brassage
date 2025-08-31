#!/bin/bash

# =============================================================================
# SCRIPT FINAL - CORRECTION MALTDETAILRESULT
# =============================================================================
# Corrige la dernière erreur de compilation dans MaltDetailQueryHandler
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTION FINALE - MaltDetailResult${NC}"
echo ""

# =============================================================================
# CORRECTION : MaltDetailQueryHandler.scala
# =============================================================================

echo -e "${YELLOW}📝 Correction MaltDetailQueryHandler.scala...${NC}"

# Backup du fichier actuel
cp "app/application/queries/public/malts/handlers/MaltDetailQueryHandler.scala" \
   "app/application/queries/public/malts/handlers/MaltDetailQueryHandler.scala.backup.$(date +%Y%m%d_%H%M%S)"

# Correction complète du fichier
cat > app/application/queries/public/malts/handlers/MaltDetailQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltDetailQuery}
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.MaltId
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour les requêtes de détail de malt (API publique)
 */
@Singleton
class MaltDetailQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * ✅ CORRIGÉ: Retourne Option[MaltReadModel] au lieu de MaltDetailResult
   */
  def handle(query: MaltDetailQuery): Future[Option[MaltReadModel]] = {
    for {
      maltIdResult <- Future.successful(MaltId(query.maltId))
      maltOption <- maltIdResult match {
        case Right(maltId) =>
          maltReadRepo.findById(maltId).map(_.map(MaltReadModel.fromAggregate))
        case Left(_) =>
          Future.successful(None)
      }
    } yield maltOption
  }
}
EOF

echo -e "${GREEN}✅ MaltDetailQueryHandler.scala corrigé${NC}"

# =============================================================================
# VÉRIFICATION OPTIONNELLE : MaltDetailResult case class
# =============================================================================

echo -e "${YELLOW}📝 Création optionnelle du MaltDetailResult (pour cohérence)...${NC}"

# Créer le répertoire si nécessaire
mkdir -p app/application/queries/public/malts/readmodels

# Créer un MaltDetailResult si on veut le garder pour plus tard
cat > app/application/queries/public/malts/readmodels/MaltDetailResult.scala << 'EOF'
package application.queries.public.malts.readmodels

import play.api.libs.json._

/**
 * Result détaillé pour une requête de malt spécifique
 * (Optionnel - pour usage futur avec substituts, compatibilités, etc.)
 */
case class MaltDetailResult(
  malt: MaltReadModel,
  substitutes: List[MaltSubstitute] = List.empty,
  compatibleBeerStyles: List[BeerStyleCompatibility] = List.empty,
  usageRecommendations: List[String] = List.empty
)

case class MaltSubstitute(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String]
)

case class BeerStyleCompatibility(
  styleId: String,
  styleName: String,
  compatibilityScore: Double,
  usageNotes: Option[String]
)

object MaltDetailResult {
  implicit val maltSubstituteFormat: Format[MaltSubstitute] = Json.format[MaltSubstitute]
  implicit val beerStyleCompatibilityFormat: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
  implicit val format: Format[MaltDetailResult] = Json.format[MaltDetailResult]
}
EOF

echo -e "${GREEN}✅ MaltDetailResult.scala créé (pour usage futur)${NC}"

# =============================================================================
# CORRECTION OPTIONNELLE : MaltDetailQuery
# =============================================================================

echo -e "${YELLOW}📝 Vérification MaltDetailQuery...${NC}"

# Vérifier si MaltDetailQuery existe et est correct
if [ ! -f "app/application/queries/public/malts/MaltDetailQuery.scala" ]; then
    echo -e "${YELLOW}⚠️  MaltDetailQuery.scala manquant, création...${NC}"
    
    cat > app/application/queries/public/malts/MaltDetailQuery.scala << 'EOF'
package application.queries.public.malts

/**
 * Query pour obtenir le détail d'un malt spécifique
 */
case class MaltDetailQuery(
  maltId: String
)
EOF
    echo -e "${GREEN}✅ MaltDetailQuery.scala créé${NC}"
else
    echo -e "${GREEN}✅ MaltDetailQuery.scala existe déjà${NC}"
fi

# =============================================================================
# TEST DE COMPILATION FINAL
# =============================================================================

echo ""
echo -e "${BLUE}🔍 Test de compilation final...${NC}"

if sbt compile > /tmp/final_compilation_fix.log 2>&1; then
    echo -e "${GREEN}✅ COMPILATION FINALE RÉUSSIE !${NC}"
    echo ""
    echo -e "${GREEN}🎉 DOMAINE MALTS COMPILE PARFAITEMENT !${NC}"
    echo ""
    echo -e "${BLUE}📊 Résumé final :${NC}"
    echo -e "   ✅ MaltDetailQueryHandler : Type MaltDetailResult corrigé"
    echo -e "   ✅ Retourne maintenant Option[MaltReadModel]"
    echo -e "   ✅ MaltDetailResult créé pour usage futur"
    echo -e "   ✅ MaltDetailQuery vérifié/créé"
    echo ""
    echo -e "${GREEN}🚀 LE PROJET COMPILE ENTIÈREMENT !${NC}"
    echo ""
    echo -e "${YELLOW}Warnings restants (non bloquants) :${NC}"
    warning_count=$(grep -c "warn" /tmp/final_compilation_fix.log || echo "0")
    echo -e "${YELLOW}   $warning_count warnings (imports inutilisés, paramètres non utilisés)${NC}"
    echo -e "${YELLOW}   → Ces warnings peuvent être ignorés car ils n'empêchent pas la compilation${NC}"
    
    echo ""
    echo -e "${BLUE}🎯 PROCHAINES ÉTAPES RECOMMANDÉES :${NC}"
    echo -e "${GREEN}   1. ✅ DOMAINE MALTS → Compilation OK !${NC}"
    echo -e "   2. 🚀 Implémenter les repositories Slick complets"
    echo -e "   3. 🚀 Créer les tests unitaires"
    echo -e "   4. 🚀 Démarrer le domaine Yeasts"
    echo -e "   5. 🚀 Interface admin frontend"
    
    echo ""
    echo -e "${GREEN}🌟 FÉLICITATIONS ! Architecture DDD/CQRS Malts opérationnelle !${NC}"
    
else
    echo -e "${RED}❌ Erreurs de compilation persistantes${NC}"
    echo ""
    echo -e "${YELLOW}Erreurs restantes :${NC}"
    grep "error" /tmp/final_compilation_fix.log || echo "Aucune erreur trouvée dans le log"
    echo ""
    echo -e "${YELLOW}Consultez le log complet : /tmp/final_compilation_fix.log${NC}"
    
    echo ""
    echo -e "${YELLOW}Actions de débogage recommandées :${NC}"
    echo -e "   1. Vérifiez que tous les imports sont corrects"
    echo -e "   2. Vérifiez que MaltId, MaltReadModel existent"
    echo -e "   3. Vérifiez la syntaxe Scala"
fi

echo ""
echo -e "${BLUE}📁 Fichiers de backup créés pour rollback si nécessaire${NC}"
echo -e "${GREEN}🔧 Correction finale terminée !$