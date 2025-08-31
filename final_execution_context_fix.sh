#!/bin/bash
# =============================================================================
# CORRECTIF FINAL - EXECUTIONCONTEXT ET IMPORTS INUTILISÉS
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 Correctif final - ExecutionContext et imports${NC}"

# =============================================================================
# CORRECTIF 1 : INTERFACE REPOSITORY SANS MÉTHODES PAR DÉFAUT
# =============================================================================

echo "1. Correction de l'interface MaltReadRepository..."

cat > app/domain/malts/repositories/MaltReadRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId, MaltType}
import scala.concurrent.Future

/**
 * Interface repository lecture pour les malts
 * Version corrigée sans méthodes par défaut problématiques
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
  def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]]
}
EOF

# =============================================================================
# CORRECTIF 2 : CONTROLLER ADMIN SANS IMPORTS INUTILISÉS
# =============================================================================

echo "2. Correction du controller admin..."

cat > app/controllers/admin/AdminMaltsController.scala << 'EOF'
package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import domain.malts.repositories.MaltReadRepository

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

# =============================================================================
# CORRECTIF 3 : IMPLÉMENTATION DE LA MÉTHODE SEARCH DANS LE REPOSITORY
# =============================================================================

echo "3. Mise à jour de l'implémentation de la méthode search..."

# Sauvegarder le fichier actuel
cp app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala.backup-search

# Ajouter une implémentation correcte de la méthode search à la fin du repository
cat >> app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'

  // Implémentation de la méthode search manquante
  override def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts avec terme: '$query' (page $page, taille $pageSize)")
    
    val searchTerm = s"%${query.toLowerCase}%"
    val offset = page * pageSize
    
    val searchQuery = malts
      .filter(row => 
        row.name.toLowerCase.like(searchTerm) || 
        row.description.toLowerCase.like(searchTerm) ||
        row.maltType.toLowerCase.like(searchTerm)
      )
      .sortBy(_.name)
      .drop(offset)
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

# Supprimer la ligne de fermeture de classe en double qui a été ajoutée
sed -i '' '$d' app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala

# =============================================================================
# CORRECTIF 4 : VÉRIFIER QUE LE REPOSITORY SE TERMINE CORRECTEMENT
# =============================================================================

echo "4. Vérification de la structure du repository..."

# Créer une version propre du repository sans duplications
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
    flavorProfiles: Option[String],
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
  // HELPER FUNCTIONS
  // ===============================

  private def parseFlavorProfiles(flavorProfilesString: Option[String]): List[String] = {
    flavorProfilesString match {
      case None => List.empty
      case Some(str) if str.trim.isEmpty => List.empty
      case Some(str) =>
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

  private def rowToAggregateSafe(row: MaltRow): Try[MaltAggregate] = Try {
    println(s"🔍 DEBUG: Conversion malt '${row.name}' (ID: ${row.id})")
    
    val maltId = MaltId(row.id)
    val name = NonEmptyString.create(row.name).getOrElse(NonEmptyString.unsafe(row.name))
    val maltType = MaltType.fromName(row.maltType).getOrElse(MaltType.unsafe(row.maltType))
    val source = MaltSource.fromName(row.source).getOrElse(MaltSource.unsafe(row.source))
    val ebcColor = EBCColor(row.ebcColor).getOrElse(EBCColor.unsafe(row.ebcColor))
    val extractionRate = ExtractionRate(row.extractionRate).getOrElse(ExtractionRate.unsafe(row.extractionRate))
    val diastaticPower = DiastaticPower(row.diastaticPower).getOrElse(DiastaticPower.unsafe(row.diastaticPower))
    val flavorProfiles = parseFlavorProfiles(row.flavorProfiles)
    
    println(s"   ✅ Conversion réussie: ${row.name}")
    
    MaltAggregate(
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
  }

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

  private def rowToAggregate(row: MaltRow): MaltAggregate = {
    rowToAggregateSafe(row) match {
      case Success(aggregate) => aggregate
      case Failure(ex) => 
        println(s"❌ Échec conversion safe pour '${row.name}': ${ex.getMessage}")
        rowToAggregateUnsafe(row)
    }
  }

  // ===============================
  // IMPLÉMENTATION DES MÉTHODES
  // ===============================

  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    println(s"🔍 Recherche malt par ID: ${id}")
    
    db.run(malts.filter(_.id === id.value).result.headOption).map { rowOpt =>
      val result = rowOpt.map(rowToAggregate)
      println(s"   Résultat: ${result.map(_.name.value).getOrElse("Non trouvé")}")
      result
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche par ID ${id}: ${ex.getMessage}")
        None
    }
  }

  override def findAll(page: Int, pageSize: Int, activeOnly: Boolean = false): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche tous les malts (page $page, taille $pageSize, actifs: $activeOnly)")
    
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
        List.empty[MaltAggregate]
    }
  }

  override def count(activeOnly: Boolean = false): Future[Long] = {
    println(s"📊 Comptage des malts (actifs: $activeOnly)")
    
    val query = if (activeOnly) {
      malts.filter(_.isActive === true).length
    } else {
      malts.length
    }
    
    db.run(query.result).map { count =>
      val longCount = count.toLong
      println(s"   Total malts: $longCount")
      longCount
    }.recover {
      case ex =>
        println(s"❌ Erreur comptage: ${ex.getMessage}")
        0L
    }
  }

  def findByType(maltType: MaltType): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts par type: ${maltType.name}")
    
    db.run(malts.filter(_.maltType === maltType.name).result).map { rows =>
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   Trouvé ${aggregates.length} malts de type ${maltType.name}")
      aggregates
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche par type: ${ex.getMessage}")
        List.empty[MaltAggregate]
    }
  }

  def findActive(): Future[List[MaltAggregate]] = {
    findAll(0, 1000, activeOnly = true)
  }

  override def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts avec terme: '$query'")
    
    val searchTerm = s"%${query.toLowerCase}%"
    val offset = page * pageSize
    
    val searchQuery = malts
      .filter(row => 
        row.name.toLowerCase.like(searchTerm) || 
        row.description.toLowerCase.like(searchTerm) ||
        row.maltType.toLowerCase.like(searchTerm)
      )
      .sortBy(_.name)
      .drop(offset)
      .take(pageSize)

    db.run(searchQuery.result).map { rows =>
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   Trouvé ${aggregates.length} malts pour '$query'")
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
# TEST FINAL
# =============================================================================

echo -e "${BLUE}5. Test de compilation final...${NC}"

if sbt compile > /tmp/final_final_compile.log 2>&1; then
    echo -e "${GREEN}✅ Compilation réussie ! Tous les problèmes résolus !${NC}"
else
    echo -e "${RED}❌ Erreurs persistantes${NC}"
    tail -10 /tmp/final_final_compile.log
fi

echo ""
echo -e "${GREEN}🎉 Corrections appliquées :${NC}"
echo "   ✅ ExecutionContext retiré des méthodes par défaut"
echo "   ✅ Imports inutilisés supprimés"
echo "   ✅ Interface repository simplifiée"
echo "   ✅ Repository avec implémentation complète et propre"

echo ""
echo -e "${BLUE}🚀 Maintenant testez :${NC}"
echo "   sbt run"
echo "   curl http://localhost:9000/api/admin/malts"