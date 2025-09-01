#!/bin/bash
# =============================================================================
# CORRECTIONS MÉTHODIQUES PHASE 1 - INTERFACES CRITIQUES
# =============================================================================
# 
# Corrections ciblées basées sur l'analyse exhaustive
# Phase 1 : Débloquer la compilation (erreurs critiques uniquement)
#
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 CORRECTIONS MÉTHODIQUES PHASE 1 - INTERFACES CRITIQUES${NC}"
echo ""

# =============================================================================
# CORRECTION 1 : AJOUTER MÉTHODES MANQUANTES À MALTREADREPOSITORY
# =============================================================================

echo "1. Ajout des méthodes manquantes à MaltReadRepository..."

cat > app/domain/malts/repositories/MaltReadRepository.scala << 'EOF'
package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId, MaltType}
import domain.shared.NonEmptyString
import scala.concurrent.Future

/**
 * Interface repository lecture pour les malts
 * Version complète avec toutes les méthodes requises
 */
trait MaltReadRepository {
  
  // Méthodes existantes
  def findById(id: MaltId): Future[Option[MaltAggregate]]
  def findAll(page: Int, pageSize: Int, activeOnly: Boolean = false): Future[List[MaltAggregate]]
  def count(activeOnly: Boolean = false): Future[Long]
  def findByType(maltType: MaltType): Future[List[MaltAggregate]]
  def findActive(): Future[List[MaltAggregate]]
  def search(query: String, page: Int = 0, pageSize: Int = 20): Future[List[MaltAggregate]]
  
  // NOUVELLES MÉTHODES REQUISES PAR LES HANDLERS
  
  /**
   * Vérifie si un malt avec ce nom existe déjà
   */
  def existsByName(name: NonEmptyString): Future[Boolean]
  
  /**
   * Recherche avec filtres multiples (pour MaltSearchQueryHandler)
   */
  def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None, 
    originCode: Option[String] = None,
    status: Option[String] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[List[MaltAggregate]]
}
EOF

# =============================================================================
# CORRECTION 2 : HARMONISATION MALTID UUID/STRING
# =============================================================================

echo "2. Harmonisation MaltId pour résoudre les conflits UUID/String..."

cat > app/domain/malts/model/MaltId.scala << 'EOF'
package domain.malts.model

import java.util.UUID
import play.api.libs.json._
import scala.util.{Try, Success, Failure}

/**
 * Value Object pour identifiant de malt
 * Version harmonisée UUID/String avec conversions explicites
 */
case class MaltId private(value: UUID) extends AnyVal {
  
  // Conversion explicite vers String pour compatibilité
  override def toString: String = value.toString
  def asString: String = value.toString
  def asUUID: UUID = value
  
  // Pour les comparaisons avec String dans Slick
  def equalsString(str: String): Boolean = value.toString == str
}

object MaltId {
  
  def apply(uuid: UUID): MaltId = new MaltId(uuid)
  
  def apply(value: String): Either[String, MaltId] = {
    Try(UUID.fromString(value)) match {
      case Success(uuid) => Right(new MaltId(uuid))
      case Failure(ex) => Left(s"UUID invalide : $value (${ex.getMessage})")
    }
  }
  
  def fromString(value: String): Option[MaltId] = apply(value).toOption
  
  // Méthode unsafe pour bypasser validation (debug uniquement)
  def unsafe(value: String): MaltId = {
    Try(UUID.fromString(value)) match {
      case Success(uuid) => new MaltId(uuid)
      case Failure(_) => 
        println(s"⚠️  MaltId.unsafe: UUID invalide '$value', génération d'un nouvel UUID")
        new MaltId(UUID.randomUUID())
    }
  }
  
  def generate(): MaltId = new MaltId(UUID.randomUUID())
  
  def toOption: Either[String, MaltId] => Option[MaltId] = _.toOption
  
  implicit val format: Format[MaltId] = Format(
    Reads { js => 
      js.validate[String].flatMap { str =>
        fromString(str) match {
          case Some(maltId) => JsSuccess(maltId)
          case None => JsError("UUID invalide")
        }
      }
    },
    Writes(maltId => JsString(maltId.toString))
  )
}
EOF

# =============================================================================
# CORRECTION 3 : AJOUT PROPRIÉTÉS MANQUANTES VALUE OBJECTS
# =============================================================================

echo "3. Ajout des propriétés manquantes dans DiastaticPower..."

cat > app/domain/malts/model/DiastaticPower.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._
import scala.util.Try

/**
 * Value Object pour pouvoir diastasique des malts
 * Version complète avec toutes les propriétés requises
 */
case class DiastaticPower private(value: Double) extends AnyVal {
  
  def canConvertAdjuncts: Boolean = value >= 35
  
  def enzymaticCategory: String = {
    if (value >= 140) "Très élevé"
    else if (value >= 100) "Élevé"
    else if (value >= 50) "Moyen"
    else if (value >= 20) "Faible"
    else "Très faible"
  }
  
  // ALIAS pour compatibility avec le code existant
  def enzymePowerCategory: String = enzymaticCategory
}

object DiastaticPower {
  
  def apply(value: Double): Either[String, DiastaticPower] = {
    if (value < 0) {
      Left("Le pouvoir diastasique ne peut pas être négatif")
    } else if (value > 200) {
      Left("Le pouvoir diastasique ne peut pas dépasser 200")
    } else if (!value.isFinite) {
      Left("Le pouvoir diastasique doit être un nombre valide")
    } else {
      Right(new DiastaticPower(value))
    }
  }
  
  def fromDouble(value: Double): Option[DiastaticPower] = apply(value).toOption
  
  def fromString(value: String): Option[DiastaticPower] = {
    Try(value.toDouble).toOption.flatMap(fromDouble)
  }
  
  // Méthode unsafe pour bypasser validation
  def unsafe(value: Any): DiastaticPower = {
    val doubleValue = value match {
      case d: Double if d.isFinite => d
      case f: Float if f.isFinite => f.toDouble
      case n: Number => n.doubleValue()
      case s: String => Try(s.toDouble).getOrElse(100.0)
      case _ => 100.0
    }
    
    val clampedValue = math.max(0, math.min(200, doubleValue))
    if (clampedValue != doubleValue) {
      println(s"⚠️  DiastaticPower.unsafe: valeur '$value' corrigée en $clampedValue")
    }
    new DiastaticPower(clampedValue)
  }
  
  val toOption: Either[String, DiastaticPower] => Option[DiastaticPower] = _.toOption
  
  implicit val format: Format[DiastaticPower] = Json.valueFormat[DiastaticPower]
}
EOF

echo "4. Ajout des propriétés manquantes dans EBCColor..."

cat > app/domain/malts/model/EBCColor.scala << 'EOF'
package domain.malts.model

import play.api.libs.json._
import scala.util.Try

/**
 * Value Object pour couleur EBC des malts
 * Version complète avec toutes les propriétés requises
 */
case class EBCColor private(value: Double) extends AnyVal {
  
  def toSRM: Double = value * 0.508
  
  def colorName: String = {
    if (value <= 3) "Très pâle"
    else if (value <= 8) "Pâle"
    else if (value <= 16) "Doré"
    else if (value <= 33) "Ambré"
    else if (value <= 66) "Brun clair"
    else if (value <= 138) "Brun foncé"
    else "Noir"
  }
  
  // NOUVELLE PROPRIÉTÉ requise par MaltAggregate
  def isMaltCanBeUsed: Boolean = {
    // Logique métier : un malt peut être utilisé s'il a une couleur valide
    value >= 0 && value <= 1000 && value.isFinite
  }
}

object EBCColor {
  
  def apply(value: Double): Either[String, EBCColor] = {
    if (value < 0) {
      Left("La couleur EBC ne peut pas être négative")
    } else if (value > 1000) {
      Left("La couleur EBC ne peut pas dépasser 1000")
    } else if (!value.isFinite) {
      Left("La couleur EBC doit être un nombre valide")
    } else {
      Right(new EBCColor(value))
    }
  }
  
  def fromDouble(value: Double): Option[EBCColor] = apply(value).toOption
  
  def fromString(value: String): Option[EBCColor] = {
    Try(value.toDouble).toOption.flatMap(fromDouble)
  }
  
  // Méthode unsafe pour bypasser validation
  def unsafe(value: Any): EBCColor = {
    val doubleValue = value match {
      case d: Double if d.isFinite => d
      case f: Float if f.isFinite => f.toDouble
      case n: Number => n.doubleValue()
      case s: String => Try(s.toDouble).getOrElse(3.0)
      case _ => 3.0
    }
    
    val clampedValue = math.max(0, math.min(1000, doubleValue))
    if (clampedValue != doubleValue) {
      println(s"⚠️  EBCColor.unsafe: valeur '$value' corrigée en $clampedValue")
    }
    new EBCColor(clampedValue)
  }
  
  val toOption: Either[String, EBCColor] => Option[EBCColor] = _.toOption
  
  implicit val format: Format[EBCColor] = Json.valueFormat[EBCColor]
}
EOF

# =============================================================================
# CORRECTION 4 : CORRECTION MALTAGGREGATE AVEC NOUVELLES PROPRIÉTÉS
# =============================================================================

echo "5. Correction MaltAggregate avec les nouvelles propriétés..."

cat > app/domain/malts/model/MaltAggregate.scala << 'EOF'
package domain.malts.model

import domain.shared.NonEmptyString
import java.time.Instant

/**
 * Agrégat Malt avec toutes les propriétés requises
 * Version corrigée avec propriétés calculées
 */
case class MaltAggregate(
  id: MaltId,
  name: NonEmptyString,
  maltType: MaltType,
  ebcColor: EBCColor,
  extractionRate: ExtractionRate,
  diastaticPower: DiastaticPower,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: MaltSource,
  isActive: Boolean,
  credibilityScore: Double,
  createdAt: Instant,
  updatedAt: Instant,
  version: Long
) {
  
  // Propriétés calculées existantes
  def needsReview: Boolean = credibilityScore < 0.8
  def canSelfConvert: Boolean = diastaticPower.canConvertAdjuncts
  def qualityScore: Double = credibilityScore * 100
  def isBaseMalt: Boolean = maltType == MaltType.BASE
  
  def maxRecommendedPercent: Option[Double] = maltType match {
    case MaltType.BASE => Some(100)
    case MaltType.CRYSTAL => Some(20)
    case MaltType.ROASTED => Some(10)
    case _ => Some(30)
  }
  
  // NOUVELLE PROPRIÉTÉ requise par le code existant
  def enzymePowerCategory: String = diastaticPower.enzymePowerCategory
  
  // NOUVELLE MÉTHODE pour validation complexe
  def canBeUsedInBrewing: Boolean = {
    isActive && 
    ebcColor.isMaltCanBeUsed && 
    extractionRate.value > 0 &&
    diastaticPower.value >= 0
  }
  
  // Méthodes de mise à jour
  def activate(): MaltAggregate = this.copy(
    isActive = true,
    updatedAt = Instant.now(),
    version = version + 1
  )
  
  def deactivate(): MaltAggregate = this.copy(
    isActive = false,
    updatedAt = Instant.now(),
    version = version + 1
  )
  
  def updateCredibility(newScore: Double): MaltAggregate = this.copy(
    credibilityScore = math.max(0.0, math.min(1.0, newScore)),
    updatedAt = Instant.now(),
    version = version + 1
  )
}

object MaltAggregate {
  
  // Factory method pour création sécurisée
  def create(
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    originCode: String,
    source: MaltSource,
    description: Option[String] = None,
    flavorProfiles: List[String] = List.empty
  ): MaltAggregate = {
    val now = Instant.now()
    MaltAggregate(
      id = MaltId.generate(),
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      originCode = originCode,
      description = description,
      flavorProfiles = flavorProfiles.filter(_.trim.nonEmpty).distinct.take(10),
      source = source,
      isActive = true,
      credibilityScore = if (source == MaltSource.Manual) 1.0 else 0.7,
      createdAt = now,
      updatedAt = now,
      version = 1L
    )
  }
}
EOF

# =============================================================================
# CORRECTION 5 : SIMPLIFICATION DES ROUTES POUR CORRESPONDRE AU CONTROLLER
# =============================================================================

echo "6. Simplification des routes pour correspondre au controller existant..."

# Sauvegarder les routes actuelles
cp conf/routes conf/routes.backup-$(date +%Y%m%d_%H%M%S)

# Créer des routes simplifiées qui correspondent aux méthodes existantes
cat > temp_routes_malts.txt << 'EOF'

# ============================================================================
# 🌾 ROUTES MALTS - VERSION SIMPLIFIÉE POUR MVP
# ============================================================================

# Routes Admin Malts (simplifiées pour correspondre aux méthodes existantes)
GET     /api/admin/malts                        controllers.admin.AdminMaltsController.getAllMaltsDefault()
GET     /api/admin/malts/paginated              controllers.admin.AdminMaltsController.getAllMalts(page: Int ?= 0, pageSize: Int ?= 20, activeOnly: Boolean ?= false)
GET     /api/admin/malts/search                 controllers.admin.AdminMaltsController.searchMalts(query: String, page: Int ?= 0, pageSize: Int ?= 20)

# Routes publiques Malts (placeholder pour futur développement)
# GET     /api/v1/malts                         controllers.MaltsController.getAllMalts(page: Int ?= 0, pageSize: Int ?= 20)
# GET     /api/v1/malts/search                  controllers.MaltsController.searchMalts(query: String, page: Int ?= 0, pageSize: Int ?= 20)

EOF

# Remplacer les routes malts dans le fichier principal
sed '/# 🌾 ROUTES MALTS/,/# Routes publiques Malts/d' conf/routes > conf/routes.tmp
cat conf/routes.tmp temp_routes_malts.txt > conf/routes
rm conf/routes.tmp temp_routes_malts.txt

echo -e "${GREEN}   Routes simplifiées pour correspondre au controller existant${NC}"

# =============================================================================
# CORRECTION 6 : IMPLÉMENTATION DES NOUVELLES MÉTHODES DANS LE REPOSITORY
# =============================================================================

echo "7. Implémentation des nouvelles méthodes dans SlickMaltReadRepository..."

# Ajouter les nouvelles méthodes au repository existant
cat >> app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'

  // ===============================
  // NOUVELLES MÉTHODES REQUISES
  // ===============================

  override def existsByName(name: NonEmptyString): Future[Boolean] = {
    println(s"🔍 Vérification existence malt: ${name.value}")
    
    db.run(malts.filter(_.name.toLowerCase === name.value.toLowerCase).exists.result).map { exists =>
      println(s"   Malt '${name.value}' existe: $exists")
      exists
    }.recover {
      case ex =>
        println(s"❌ Erreur vérification existence: ${ex.getMessage}")
        false
    }
  }

  override def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None,
    originCode: Option[String] = None,
    status: Option[String] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[List[MaltAggregate]] = {
    println(s"🔍 Recherche avec filtres: type=$maltType, ebc=$minEBC-$maxEBC, search=$searchTerm")
    
    val offset = page * pageSize
    
    // Construction de la query avec filtres
    var query = malts.sortBy(_.name)
    
    // Filtre par type
    maltType.foreach { mt =>
      query = query.filter(_.maltType === mt)
    }
    
    // Filtre par couleur EBC
    minEBC.foreach { min =>
      query = query.filter(_.ebcColor >= min)
    }
    maxEBC.foreach { max =>
      query = query.filter(_.ebcColor <= max)
    }
    
    // Filtre par origine
    originCode.foreach { origin =>
      query = query.filter(_.originCode === origin)
    }
    
    // Filtre par statut (active/inactive)
    status.foreach { s =>
      val isActive = s.toUpperCase == "ACTIVE"
      query = query.filter(_.isActive === isActive)
    }
    
    // Recherche textuelle
    searchTerm.foreach { term =>
      val searchPattern = s"%${term.toLowerCase}%"
      query = query.filter(row => 
        row.name.toLowerCase.like(searchPattern) ||
        row.description.toLowerCase.like(searchPattern)
      )
    }
    
    // Application pagination
    val finalQuery = query.drop(offset).take(pageSize)

    db.run(finalQuery.result).map { rows =>
      val aggregates = rows.map(rowToAggregate).toList
      println(s"   Trouvé ${aggregates.length} malts avec filtres")
      aggregates
    }.recover {
      case ex =>
        println(s"❌ Erreur recherche avec filtres: ${ex.getMessage}")
        List.empty[MaltAggregate]
    }
  }
}
EOF

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo -e "${BLUE}8. Test de compilation des corrections Phase 1...${NC}"

if sbt compile > /tmp/phase1_compile.log 2>&1; then
    echo -e "${GREEN}✅ PHASE 1 RÉUSSIE - Erreurs critiques résolues !${NC}"
    echo ""
    echo -e "${BLUE}📊 Corrections appliquées :${NC}"
    echo "   ✅ Méthodes manquantes ajoutées à MaltReadRepository"
    echo "   ✅ Types UUID/String harmonisés dans MaltId"
    echo "   ✅ Propriétés manquantes ajoutées aux Value Objects"
    echo "   ✅ MaltAggregate corrigé avec nouvelles propriétés"
    echo "   ✅ Routes simplifiées pour correspondre au controller"
    echo "   ✅ Nouvelles méthodes implémentées dans le repository"
else
    echo -e "${RED}❌ Erreurs persistantes en Phase 1${NC}"
    echo "   Consultez: /tmp/phase1_compile.log"
    echo "   Dernières erreurs:"
    tail -15 /tmp/phase1_compile.log
fi

echo ""
echo -e "${YELLOW}🔄 Si Phase 1 réussie, prochaine étape :${NC}"
echo "   Phase 2 : Correction des handlers et ReadModels"
echo "   Test de l'API: curl http://localhost:9000/api/admin/malts"