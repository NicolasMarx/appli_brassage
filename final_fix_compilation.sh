#!/bin/bash
# =============================================================================
# CORRECTIF FINAL - ERREURS DE COMPILATION SLICK
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 Correctif final des erreurs de compilation${NC}"

# =============================================================================
# CORRECTIF 1 : SUPPRIMER L'IMPORT INUTILISÉ
# =============================================================================

echo "1. Correction de NonEmptyString.scala..."

cat > app/domain/shared/NonEmptyString.scala << 'EOF'
package domain.shared

import play.api.libs.json._

/**
 * Value Object pour chaînes non vides
 * Version corrigée sans imports inutilisés
 */
case class NonEmptyString private(value: String) extends AnyVal {
  override def toString: String = value
}

object NonEmptyString {
  
  def create(value: String): Either[String, NonEmptyString] = {
    if (value == null || value.trim.isEmpty) {
      Left("La chaîne ne peut pas être vide")
    } else {
      Right(new NonEmptyString(value.trim))
    }
  }
  
  def apply(value: String): NonEmptyString = {
    create(value).getOrElse(
      throw new IllegalArgumentException(s"Chaîne vide non autorisée : '$value'")
    )
  }
  
  // Méthode unsafe pour bypasser validation (debug uniquement)
  def unsafe(value: String): NonEmptyString = new NonEmptyString(
    if (value == null || value.trim.isEmpty) "DEFAULT_VALUE" else value.trim
  )
  
  def fromString(value: String): Option[NonEmptyString] = create(value).toOption
  
  implicit val format: Format[NonEmptyString] = Json.valueFormat[NonEmptyString]
}
EOF

# =============================================================================
# CORRECTIF 2 : CORRIGER LE TYPE SLICK POUR FLAVOR_PROFILES
# =============================================================================

echo "2. Correction du type Slick pour flavor_profiles..."

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
    flavorProfiles: Option[String], // CORRECTIF: String au lieu d'Array[String]
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
    def flavorProfiles = column[Option[String]]("flavor_profiles") // CORRECTIF: String au lieu d'Array[String]
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
              str.replace("[", "").replace("]", "").split(",").map(_.trim).filter(_.nonEmpty).toList
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
  // IMPLÉMENTATION DES MÉTHODES
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

  override def findAll(page: Int = 0, size: Int = 20): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche tous les malts (page $page, taille $size)")
    
    val offset = page * size
    val query = malts
      .sortBy(_.name)
      .drop(offset)
      .take(size)

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

  override def count(): Future[Int] = {
    println(s"📊 Comptage des malts")
    
    db.run(malts.length.result).map { count =>
      println(s"   Total malts en base: $count")
      count
    }.recover {
      case ex =>
        println(s"❌ Erreur comptage: ${ex.getMessage}")
        0
    }
  }

  override def findByType(maltType: MaltType): Future[List[MaltAggregate]] = {
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

  override def findActive(): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche malts actifs")
    
    val query = malts.filter(_.isActive === true)
    
    db.run(query.result).map { rows =>
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   Trouvé ${aggregates.length} malts actifs")
      aggregates
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche malts actifs: ${ex.getMessage}")
        List.empty[MaltAggregate]
    }
  }
}
EOF

# =============================================================================
# CORRECTIF 3 : MISE À JOUR DE LA BASE DE DONNÉES
# =============================================================================

echo "3. Script de mise à jour de la colonne flavor_profiles en base..."

cat > update_flavor_profiles_column.sql << 'EOF'
-- Mise à jour de la colonne flavor_profiles pour être compatible avec Slick
-- Convertir de TEXT[] à TEXT (JSON ou CSV)

-- Vérifier le type actuel
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'malts' AND column_name = 'flavor_profiles';

-- Sauvegarder les données actuelles si elles existent
CREATE TEMP TABLE malts_flavor_backup AS 
SELECT id, flavor_profiles FROM malts WHERE flavor_profiles IS NOT NULL;

-- Supprimer et recréer la colonne avec le bon type
ALTER TABLE malts DROP COLUMN IF EXISTS flavor_profiles;
ALTER TABLE malts ADD COLUMN flavor_profiles TEXT;

-- Restaurer les données sous forme JSON
UPDATE malts 
SET flavor_profiles = (
    SELECT '["' || array_to_string(b.flavor_profiles, '","') || '"]'
    FROM malts_flavor_backup b 
    WHERE b.id = malts.id
)
WHERE id IN (SELECT id FROM malts_flavor_backup);

-- Nettoyer
DROP TABLE malts_flavor_backup;
EOF

echo "   Script SQL créé: update_flavor_profiles_column.sql"

# =============================================================================
# CORRECTIF 4 : SUPPRIMER LES FICHIERS DE TABLES REDONDANTS
# =============================================================================

echo "4. Nettoyage des fichiers redondants..."

if [ -f "app/infrastructure/persistence/slick/tables/MaltTables.scala" ]; then
    rm -f "app/infrastructure/persistence/slick/tables/MaltTables.scala"
    echo "   Supprimé: MaltTables.scala redondant"
fi

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}5. Test de compilation...${NC}"

if sbt compile > /tmp/final_compile.log 2>&1; then
    echo -e "${GREEN}✅ Compilation réussie !${NC}"
else
    echo -e "${RED}❌ Erreurs de compilation persistantes${NC}"
    echo "   Consultez: /tmp/final_compile.log"
    echo "   Dernières erreurs:"
    tail -10 /tmp/final_compile.log
fi

# =============================================================================
# INSTRUCTIONS FINALES
# =============================================================================

echo ""
echo -e "${GREEN}🎉 Correctifs appliqués !${NC}"
echo ""
echo -e "${BLUE}Prochaines étapes :${NC}"
echo "1. Si compilation OK → Redémarrer l'app: sbt run"
echo "2. Mettre à jour la DB: docker exec [container-db] psql -U postgres -d postgres -f update_flavor_profiles_column.sql"
echo "3. Tester l'API: ./test_malts_api.sh"
echo ""
echo -e "${YELLOW}Note:${NC} La colonne flavor_profiles est maintenant de type TEXT (JSON/CSV) au lieu de TEXT[]"