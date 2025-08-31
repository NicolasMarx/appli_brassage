#!/bin/bash
# Nettoyage complet et définitif des doublons ReadModels

set -e

echo "🧹 Nettoyage complet des doublons ReadModels..."

# Supprimer TOUS les fichiers ReadModels existants pour recommencer proprement
echo "Suppression de tous les fichiers ReadModels existants..."

rm -rf app/application/queries/public/malts/readmodels/
rm -rf app/application/queries/admin/malts/readmodels/

# Recréer les dossiers
mkdir -p app/application/queries/public/malts/readmodels
mkdir -p app/application/queries/admin/malts/readmodels

echo "✅ Nettoyage terminé"

# =============================================================================
# CRÉATION READMODELS PUBLICS (COMPLET)
# =============================================================================

echo "📊 Création ReadModels publics..."

cat > app/application/queries/public/malts/readmodels/MaltReadModel.scala << 'EOF'
package application.queries.public.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

case class MaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  characteristics: MaltCharacteristics,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

case class MaltCharacteristics(
  colorName: String,
  extractionCategory: String,
  enzymaticCategory: String,
  maxRecommendedPercent: Option[Double],
  isBaseMalt: Boolean,
  canSelfConvert: Boolean
)

case class MaltDetailResult(
  malt: MaltReadModel,
  substitutes: List[SubstituteReadModel]
)

case class SubstituteReadModel(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String]
)

case class MaltSearchResult(
  malts: List[MaltReadModel],
  totalCount: Long,
  currentPage: Int,
  pageSize: Int,
  hasNext: Boolean
)

object MaltReadModel {
  def fromAggregate(malt: MaltAggregate): MaltReadModel = {
    MaltReadModel(
      id = malt.id.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      characteristics = MaltCharacteristics(
        colorName = malt.ebcColor.colorName,
        extractionCategory = malt.extractionRate.extractionCategory,
        enzymaticCategory = malt.diastaticPower.enzymaticCategory,
        maxRecommendedPercent = malt.maxRecommendedPercent,
        isBaseMalt = malt.isBaseMalt,
        canSelfConvert = malt.canSelfConvert
      ),
      isActive = malt.isActive,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt
    )
  }
  
  implicit val characteristicsFormat: Format[MaltCharacteristics] = Json.format[MaltCharacteristics]
  implicit val substituteFormat: Format[SubstituteReadModel] = Json.format[SubstituteReadModel]
  implicit val detailFormat: Format[MaltDetailResult] = Json.format[MaltDetailResult]
  implicit val searchFormat: Format[MaltSearchResult] = Json.format[MaltSearchResult]
  implicit val format: Format[MaltReadModel] = Json.format[MaltReadModel]
}
EOF

# =============================================================================
# CRÉATION READMODELS ADMIN (COMPLET)
# =============================================================================

echo "🔐 Création ReadModels admin..."

cat > app/application/queries/admin/malts/readmodels/AdminMaltReadModel.scala << 'EOF'
package application.queries.admin.malts.readmodels

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

case class AdminMaltReadModel(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: String,
  isActive: Boolean,
  credibilityScore: Double,
  qualityScore: Double,
  needsReview: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
)

case class AdminMaltDetailResult(
  malt: AdminMaltReadModel,
  substitutes: List[AdminSubstituteReadModel],
  beerStyleCompatibilities: List[BeerStyleCompatibility],
  statistics: MaltUsageStatistics,
  qualityAnalysis: QualityAnalysis
)

case class AdminSubstituteReadModel(
  id: String,
  name: String,
  substitutionRatio: Double,
  notes: Option[String],
  qualityScore: Double
)

case class BeerStyleCompatibility(
  styleId: String,
  styleName: String,
  compatibilityScore: Double,
  usageNotes: String
)

case class MaltUsageStatistics(
  recipeCount: Int,
  avgUsagePercent: Double,
  popularityScore: Double
)

case class QualityAnalysis(
  dataCompleteness: Double,
  sourceReliability: String,
  reviewStatus: String,
  lastValidated: Option[Instant]
)

case class AdminMaltSearchResult(
  malts: List[AdminMaltReadModel],
  totalCount: Long,
  currentPage: Int,
  pageSize: Int,
  hasNext: Boolean,
  filters: AdminSearchFilters
)

case class AdminSearchFilters(
  maltType: Option[String],
  status: Option[String],
  source: Option[String],
  minCredibility: Option[Double],
  needsReview: Boolean,
  searchTerm: Option[String]
)

object AdminMaltReadModel {
  def fromAggregate(malt: MaltAggregate): AdminMaltReadModel = {
    AdminMaltReadModel(
      id = malt.id.toString,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      source = malt.source.name,
      isActive = malt.isActive,
      credibilityScore = malt.credibilityScore,
      qualityScore = malt.qualityScore,
      needsReview = malt.needsReview,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt,
      version = malt.version
    )
  }
  
  implicit val format: Format[AdminMaltReadModel] = Json.format[AdminMaltReadModel]
  implicit val substituteFormat: Format[AdminSubstituteReadModel] = Json.format[AdminSubstituteReadModel]
  implicit val compatibilityFormat: Format[BeerStyleCompatibility] = Json.format[BeerStyleCompatibility]
  implicit val statisticsFormat: Format[MaltUsageStatistics] = Json.format[MaltUsageStatistics]
  implicit val qualityFormat: Format[QualityAnalysis] = Json.format[QualityAnalysis]
  implicit val detailFormat: Format[AdminMaltDetailResult] = Json.format[AdminMaltDetailResult]
  implicit val searchFiltersFormat: Format[AdminSearchFilters] = Json.format[AdminSearchFilters]
  implicit val searchResultFormat: Format[AdminMaltSearchResult] = Json.format[AdminMaltSearchResult]
}
EOF

echo "✅ ReadModels créés"

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo "🔍 Test de compilation..."

if sbt compile > /tmp/final_cleanup.log 2>&1; then
    echo "✅ COMPILATION RÉUSSIE !"
    echo ""
    echo "📊 Structure finale:"
    echo "  📁 app/application/queries/public/malts/readmodels/"
    echo "     📄 MaltReadModel.scala (tout en un)"
    echo "  📁 app/application/queries/admin/malts/readmodels/"  
    echo "     📄 AdminMaltReadModel.scala (tout en un)"
    echo ""
    echo "🎉 Tous les doublons ont été éliminés !"
else
    echo "❌ Erreurs de compilation persistantes:"
    head -15 /tmp/final_cleanup.log
    echo ""
    echo "Log complet disponible dans: /tmp/final_cleanup.log"
fi