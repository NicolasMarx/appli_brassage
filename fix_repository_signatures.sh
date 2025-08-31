#!/bin/bash
# =============================================================================
# CORRECTIF SIGNATURES REPOSITORY - HARMONISATION DES MÉTHODES
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 Correction des signatures de méthodes Repository${NC}"

# =============================================================================
# ÉTAPE 1 : LECTURE DE L'INTERFACE EXISTANTE
# =============================================================================

echo "1. Vérification de l'interface MaltReadRepository existante..."

if [ -f "app/domain/malts/repositories/MaltReadRepository.scala" ]; then
    echo "   Interface existante trouvée, analyse des signatures..."
    
    # Sauvegarder l'interface actuelle
    cp "app/domain/malts/repositories/MaltReadRepository.scala" "app/domain/malts/repositories/MaltReadRepository.scala.backup-$(date +%Y%m%d_%H%M%S)"
fi

# =============================================================================
# ÉTAPE 2 : CORRECTION DE L'INTERFACE REPOSITORY
# =============================================================================

echo "2. Mise à jour de l'interface MaltReadRepository avec signatures correctes..."

cat > app/domain/malts/repositories/MaltReadRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId, MaltType}
import scala.concurrent.Future

/**
 * Interface repository lecture pour les malts
 * Version corrigée avec signatures harmonisées
 */
trait MaltReadRepository {
  
  def findById(id: MaltId): Future[Option[MaltAggregate]]
  
  // Signature corrigée pour correspondre à l'implémentation existante
  def findAll(page: Int, pageSize: Int, activeOnly: Boolean = false): Future[List[MaltAggregate]]
  
  // Signature corrigée pour correspondre à l'implémentation existante  
  def count(activeOnly: Boolean = false): Future[Long]
  
  // Nouvelles méthodes sans override (pas dans l'interface originale)
  def findByType(maltType: MaltType): Future[List[MaltAggregate]]
  
  def findActive(): Future[List[MaltAggregate]]
  
  // Méthode de recherche générique
  def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]] = {
    // Implémentation par défaut
    findAll(page, pageSize).map(_.filter(_.name.value.toLowerCase.contains(query.toLowerCase)))
  }
  
  // Méthodes de commodité avec valeurs par défaut
  def findAll(): Future[List[MaltAggregate]] = findAll(0, 100, false)
  def findAll(page: Int): Future[List[MaltAggregate]] = findAll(page, 20, false)
}
EOF

# =============================================================================
# ÉTAPE 3 : MISE À JOUR DE L'IMPLÉMENTATION REPOSITORY
# =============================================================================

echo "3. Mise à jour de SlickMaltReadRepository avec signatures correctes..."

cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID
import scala.util.{Try, Success, Failure}

import domain.malts.model._
import domain.malts.repositories.MaltReadRepository
import domain.shared.NonEmptyString

@Singleton
class SlickMaltReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
  extends MaltReadRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  // ===============================
  // DÉFINITION DES TABLES ET ROWS
  // ===============================

  case class MaltRow(
    id: UUID,
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String], // String au lieu d'Array[String]
    source: String,
    isActive: Boolean,
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  )

  class MaltsTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def maltType = column[String]("malt_type")
    def ebcColor = column[Double]("ebc_color")
    def extractionRate = column[Double]("extraction_rate")
    def diastaticPower = column[Double]("diastatic_power")
    def originCode = column[String]("origin_code")
    def description = column[Option[String]]("description")
    def flavorProfiles = column[Option[String]]("flavor_profiles")
    def source = column[String]("source")
    def isActive = column[Boolean]("is_active")
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  val malts = TableQuery[MaltsTable]

  // ===============================
  // HELPER POUR CONVERSION FLAVOR PROFILES
  // ===============================

  /**
   * Convertit une chaîne JSON ou séparée par virgules en List[String]
   */
  private def parseFlavorProfiles(flavorProfilesString: Option[String]): List[String] = {
    flavorProfilesString match {
      case None => List.empty
      case Some(str) if str.trim.isEmpty => List.empty
      case Some(str) =>
        // Essayer de parser comme JSON array d'abord, sinon split par virgules
        if (str.trim.startsWith("[") && str.trim.endsWith("]")) {
          try {
            play.api.libs.json.Json.parse(str).as[List[String]]
          } catch {
            case _: Exception => 
              str.replace("[", "").replace("]", "").replace("\"", "").split(",").map(_.trim).filter(_.nonEmpty).toList
          }
        } else {
          str.split(",").map(_.trim).filter(_.nonEmpty).toList
        }
    }
  }

  // ===============================
  // CONVERSION AVEC DEBUG DÉTAILLÉ
  // ===============================

  /**
   * Version safe avec gestion d'erreurs complète et debug
   */
  private def rowToAggregateSafe(row: MaltRow): Try[MaltAggregate] = Try {
    println(s"🔍 DEBUG: Conversion malt '${row.name}' (ID: ${row.id})")
    
    // Validation MaltId
    val maltId = MaltId(row.id)
    println(s"   ✅ MaltId: ${maltId}")
    
    // Validation NonEmptyString
    val name = NonEmptyString.create(row.name) match {
      case Right(value) => 
        println(s"   ✅ Name: ${value}")
        value
      case Left(error) => 
        println(s"   ❌ Name validation failed: $error, utilisation unsafe")
        NonEmptyString.unsafe(row.name)
    }
    
    // Validation MaltType
    val maltType = MaltType.fromName(row.maltType) match {
      case Some(value) => 
        println(s"   ✅ MaltType: ${value}")
        value
      case None =>
        println(s"   ❌ MaltType validation failed for '${row.maltType}', utilisation unsafe")
        MaltType.unsafe(row.maltType)
    }
    
    // Validation MaltSource
    val source = MaltSource.fromName(row.source) match {
      case Some(value) => 
        println(s"   ✅ MaltSource: ${value}")
        value
      case None =>
        println(s"   ❌ MaltSource validation failed for '${row.source}', utilisation unsafe")
        MaltSource.unsafe(row.source)
    }
    
    // Validation EBCColor
    val ebcColor = EBCColor(row.ebcColor) match {
      case Right(value) => 
        println(s"   ✅ EBCColor: ${value.value} (${value.colorName})")
        value
      case Left(error) =>
        println(s"   ❌ EBCColor validation failed: $error, utilisation unsafe")
        EBCColor.unsafe(row.ebcColor)
    }
    
    // Validation ExtractionRate
    val extractionRate = ExtractionRate(row.extractionRate) match {
      case Right(value) => 
        println(s"   ✅ ExtractionRate: ${value.asPercentage}")
        value
      case Left(error) =>
        println(s"   ❌ ExtractionRate validation failed: $error, utilisation unsafe")
        ExtractionRate.unsafe(row.extractionRate)
    }
    
    // Validation DiastaticPower
    val diastaticPower = DiastaticPower(row.diastaticPower) match {
      case Right(value) => 
        println(s"   ✅ DiastaticPower: ${value.value}")
        value
      case Left(error) =>
        println(s"   ❌ DiastaticPower validation failed: $error, utilisation unsafe")
        DiastaticPower.unsafe(row.diastaticPower)
    }
    
    // Traitement flavor profiles avec parser intelligent
    val flavorProfiles = parseFlavorProfiles(row.flavorProfiles)
    println(s"   ✅ FlavorProfiles: ${flavorProfiles.length} profils: ${flavorProfiles.mkString(", ")}")
    
    // Création de l'agrégat
    val aggregate = MaltAggregate(
      id = maltId,
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      originCode = row.originCode,
      description = row.description,
      flavorProfiles = flavorProfiles,
      source = source,
      isActive = row.isActive,
      credibilityScore = row.credibilityScore,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = row.version
    )
    
    println(s"   🎉 Conversion réussie: ${aggregate.name.value}")
    aggregate
  }

  /**
   * Version fallback qui ne peut pas échouer
   */
  private def rowToAggregateUnsafe(row: MaltRow): MaltAggregate = {
    println(s"🚨 FALLBACK: Conversion unsafe pour '${row.name}'")
    
    MaltAggregate(
      id = MaltId(row.id),
      name = NonEmptyString.unsafe(row.name),
      maltType = MaltType.unsafe(row.maltType),
      ebcColor = EBCColor.unsafe(row.ebcColor),
      extractionRate = ExtractionRate.unsafe(row.extractionRate),
      diastaticPower = DiastaticPower.unsafe(row.diastaticPower),
      originCode = row.originCode,
      description = row.description,
      flavorProfiles = parseFlavorProfiles(row.flavorProfiles),
      source = MaltSource.unsafe(row.source),
      isActive = row.isActive,
      credibilityScore = row.credibilityScore,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = row.version
    )
  }

  /**
   * Méthode principale de conversion avec fallback
   */
  private def rowToAggregate(row: MaltRow): MaltAggregate = {
    rowToAggregateSafe(row) match {
      case Success(aggregate) => aggregate
      case Failure(ex) => 
        println(s"❌ Échec conversion safe pour '${row.name}': ${ex.getMessage}")
        ex.printStackTrace()
        rowToAggregateUnsafe(row)
    }
  }

  // ===============================
  // IMPLÉMENTATION DES MÉTHODES AVEC SIGNATURES CORRECTES
  // ===============================

  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    println(s"🔍 Recherche malt par ID: ${id}")
    
    val query = malts.filter(_.id === id.value)
    
    db.run(query.result.headOption).map { rowOpt =>
      val result = rowOpt.map(rowToAggregate)
      println(s"   Résultat recherche ID: ${result.map(_.name.value).getOrElse("Non trouvé")}")
      result
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche par ID ${id}: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  // SIGNATURE CORRIGÉE : findAll(page: Int, pageSize: Int, activeOnly: Boolean)
  override def findAll(page: Int, pageSize: Int, activeOnly: Boolean = false): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche tous les malts (page $page, taille $pageSize, actifs seulement: $activeOnly)")
    
    val offset = page * pageSize
    val baseQuery = malts.sortBy(_.name)
    
    val query = if (activeOnly) {
      baseQuery.filter(_.isActive === true).drop(offset).take(pageSize)
    } else {
      baseQuery.drop(offset).take(pageSize)
    }

    db.run(query.result).map { rows =>
      println(s"   📋 Récupéré ${rows.length} lignes de la DB")
      
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   ✅ Converti ${aggregates.length} agrégats")
      
      aggregates
    }.recover {
      case ex =>
        println(s"❌ Erreur findAll: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[MaltAggregate]
    }
  }

  // SIGNATURE CORRIGÉE : count(activeOnly: Boolean) returns Future[Long]
  override def count(activeOnly: Boolean = false): Future[Long] = {
    println(s"📊 Comptage des malts (actifs seulement: $activeOnly)")
    
    val query = if (activeOnly) {
      malts.filter(_.isActive === true).length
    } else {
      malts.length
    }
    
    db.run(query.result).map { count =>
      val longCount = count.toLong
      println(s"   Total malts en base: $longCount")
      longCount
    }.recover {
      case ex =>
        println(s"❌ Erreur comptage: ${ex.getMessage}")
        0L
    }
  }

  // PAS D'OVERRIDE : méthode non présente dans l'interface originale
  def findByType(maltType: MaltType): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts par type: ${maltType.name}")
    
    val query = malts.filter(_.maltType === maltType.name)
    
    db.run(query.result).map { rows =>
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   Trouvé ${aggregates.length} malts de type ${maltType.name}")
      aggregates
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche par type: ${ex.getMessage}")
        List.empty[MaltAggregate]
    }
  }

  // PAS D'OVERRIDE : méthode non présente dans l'interface originale
  def findActive(): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts actifs")
    
    // Utilise la méthode findAll avec activeOnly = true
    findAll(0, 1000, activeOnly = true)
  }

  // Méthode de recherche avec terme
  override def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts avec terme: '$query' (page $page, taille $pageSize)")
    
    val searchQuery = malts
      .filter(row => row.name.toLowerCase.like(s"%${query.toLowerCase}%") || 
                     row.description.toLowerCase.like(s"%${query.toLowerCase}%"))
      .sortBy(_.name)
      .drop(page * pageSize)
      .take(pageSize)

    db.run(searchQuery.result).map { rows =>
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   Trouvé ${aggregates.length} malts correspondant à '$query'")
      aggregates
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche: ${ex.getMessage}")
        List.empty[MaltAggregate]
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 4 : TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}4. Test de compilation...${NC}"

if sbt compile > /tmp/signature_fix_compile.log 2>&1; then
    echo -e "${GREEN}✅ Compilation réussie avec signatures corrigées !${NC}"
else
    echo -e "${RED}❌ Erreurs de compilation persistantes${NC}"
    echo "   Consultez: /tmp/signature_fix_compile.log"
    echo "   Dernières erreurs:"
    tail -15 /tmp/signature_fix_compile.log
fi

# =============================================================================
# ÉTAPE 5 : MISE À JOUR DU CONTROLLER ADMIN
# =============================================================================

echo "5. Mise à jour du controller admin pour utiliser les nouvelles signatures..."

# Sauvegarder l'ancien controller
if [ -f "app/controllers/admin/AdminMaltsController.scala" ]; then
    cp "app/controllers/admin/AdminMaltsController.scala" "app/controllers/admin/AdminMaltsController.scala.backup-$(date +%Y%m%d_%H%M%S)"
fi

# Créer un controller adapté aux nouvelles signatures
cat > app/controllers/admin/AdminMaltsController.scala << 'EOF'
package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

import domain.malts.repositories.MaltReadRepository
import domain.malts.model.MaltAggregate

@Singleton
class AdminMaltsController @Inject()(
  val controllerComponents: ControllerComponents,
  maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) extends BaseController {

  /**
   * Liste tous les malts pour l'admin
   */
  def getAllMalts(page: Int, pageSize: Int, activeOnly: Boolean): Action[AnyContent] = Action.async {
    println(s"🔍 AdminMaltsController.getAllMalts: page=$page, pageSize=$pageSize, activeOnly=$activeOnly")
    
    for {
      malts <- maltReadRepository.findAll(page, pageSize, activeOnly)
      totalCount <- maltReadRepository.count(activeOnly)
    } yield {
      println(s"   Récupéré ${malts.length} malts, total: $totalCount")
      
      val maltsJson = malts.map { malt =>
        Json.obj(
          "id" -> malt.id.toString,
          "name" -> malt.name.value,
          "maltType" -> malt.maltType.name,
          "ebcColor" -> malt.ebcColor.value,
          "extractionRate" -> malt.extractionRate.value,
          "diastaticPower" -> malt.diastaticPower.value,
          "originCode" -> malt.originCode,
          "description" -> malt.description,
          "flavorProfiles" -> malt.flavorProfiles,
          "source" -> malt.source.name,
          "isActive" -> malt.isActive,
          "credibilityScore" -> malt.credibilityScore,
          "createdAt" -> malt.createdAt.toString,
          "updatedAt" -> malt.updatedAt.toString,
          "version" -> malt.version
        )
      }

      Ok(Json.obj(
        "malts" -> maltsJson,
        "totalCount" -> totalCount,
        "page" -> page,
        "pageSize" -> pageSize,
        "hasMore" -> (malts.length == pageSize)
      ))
    }
  }

  /**
   * Route par défaut compatible avec l'ancienne API
   */
  def getAllMaltsDefault: Action[AnyContent] = getAllMalts(0, 20, false)

  /**
   * Recherche de malts
   */
  def searchMalts(query: String, page: Int, pageSize: Int): Action[AnyContent] = Action.async {
    maltReadRepository.search(query, page, pageSize).map { malts =>
      val maltsJson = malts.map { malt =>
        Json.obj(
          "id" -> malt.id.toString,
          "name" -> malt.name.value,
          "maltType" -> malt.maltType.name,
          "ebcColor" -> malt.ebcColor.value,
          "extractionRate" -> malt.extractionRate.value
        )
      }
      
      Ok(Json.obj(
        "malts" -> maltsJson,
        "query" -> query,
        "totalResults" -> malts.length
      ))
    }
  }
}
EOF

echo -e "${GREEN}✅ Controller admin mis à jour${NC}"

# =============================================================================
# RAPPORT FINAL
# =============================================================================

echo ""
echo -e "${GREEN}🎉 ======================================================"
echo "   SIGNATURES REPOSITORY CORRIGÉES"
echo -e "======================================================${NC}"
echo ""

echo -e "${BLUE}📊 Corrections appliquées :${NC}"
echo "   ✅ Interface MaltReadRepository harmonisée"
echo "   ✅ SlickMaltReadRepository avec bonnes signatures"
echo "   ✅ Controller admin adapté aux nouvelles signatures"
echo "   ✅ Méthodes sans override marquées correctement"

echo ""
echo -e "${BLUE}🔧 Signatures corrigées :${NC}"
echo "   • findAll(page: Int, pageSize: Int, activeOnly: Boolean)"
echo "   • count(activeOnly: Boolean): Future[Long]"
echo "   • findByType() et findActive() sans override"

echo ""
echo -e "${BLUE}🧪 Test maintenant :${NC}"
echo "   1. sbt run"
echo "   2. curl http://localhost:9000/api/admin/malts"
echo ""
echo -e "${GREEN}🍺 Les malts devraient enfin apparaître !${NC}"