#!/bin/bash
# =============================================================================
# SCRIPT : Création Repositories domaine Yeast
# OBJECTIF : Créer les interfaces et implémentations repository
# USAGE : ./scripts/yeast/04-create-repositories.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🗄️ CRÉATION REPOSITORIES YEAST${NC}"
echo -e "${BLUE}==============================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: INTERFACES REPOSITORY
# =============================================================================

echo -e "${YELLOW}📋 Création interfaces repository...${NC}"

cat > app/domain/yeasts/repositories/YeastRepository.scala << 'EOF'
package domain.yeasts.repositories

import domain.yeasts.model._
import scala.concurrent.Future

/**
 * Repository principal pour les opérations CRUD des levures
 * Interface générique combinant lecture et écriture
 */
trait YeastRepository extends YeastReadRepository with YeastWriteRepository {
  
  /**
   * Opération transactionnelle : créer ou mettre à jour
   */
  def save(yeast: YeastAggregate): Future[YeastAggregate]
  
  /**
   * Opération transactionnelle : supprimer définitivement
   */  
  def delete(yeastId: YeastId): Future[Unit]
  
  /**
   * Vérification existence
   */
  def exists(yeastId: YeastId): Future[Boolean]
}
EOF

cat > app/domain/yeasts/repositories/YeastReadRepository.scala << 'EOF'
package domain.yeasts.repositories

import domain.yeasts.model._
import scala.concurrent.Future

/**
 * Repository de lecture pour les levures (CQRS - Query side)
 * Interface optimisée pour les requêtes et recherches
 */
trait YeastReadRepository {
  
  /**
   * Recherche par identifiant
   */
  def findById(yeastId: YeastId): Future[Option[YeastAggregate]]
  
  /**
   * Recherche par nom exact
   */
  def findByName(name: YeastName): Future[Option[YeastAggregate]]
  
  /**
   * Recherche par laboratoire et souche
   */
  def findByLaboratoryAndStrain(
    laboratory: YeastLaboratory, 
    strain: YeastStrain
  ): Future[Option[YeastAggregate]]
  
  /**
   * Recherche par filtre avec pagination
   */
  def findByFilter(filter: YeastFilter): Future[PaginatedResult[YeastAggregate]]
  
  /**
   * Recherche par type de levure
   */
  def findByType(yeastType: YeastType): Future[List[YeastAggregate]]
  
  /**
   * Recherche par laboratoire
   */
  def findByLaboratory(laboratory: YeastLaboratory): Future[List[YeastAggregate]]
  
  /**
   * Recherche par statut
   */
  def findByStatus(status: YeastStatus): Future[List[YeastAggregate]]
  
  /**
   * Recherche par plage d'atténuation
   */
  def findByAttenuationRange(
    minAttenuation: Int, 
    maxAttenuation: Int
  ): Future[List[YeastAggregate]]
  
  /**
   * Recherche par plage de température
   */
  def findByTemperatureRange(
    minTemp: Int, 
    maxTemp: Int
  ): Future[List[YeastAggregate]]
  
  /**
   * Recherche par caractéristiques aromatiques
   */
  def findByCharacteristics(characteristics: List[String]): Future[List[YeastAggregate]]
  
  /**
   * Recherche full-text dans nom et caractéristiques
   */
  def searchText(query: String): Future[List[YeastAggregate]]
  
  /**
   * Compte total des levures par statut
   */
  def countByStatus: Future[Map[YeastStatus, Long]]
  
  /**
   * Statistiques par laboratoire
   */
  def getStatsByLaboratory: Future[Map[YeastLaboratory, Long]]
  
  /**
   * Statistiques par type
   */
  def getStatsByType: Future[Map[YeastType, Long]]
  
  /**
   * Levures les plus populaires (à implémenter selon métriques usage)
   */
  def findMostPopular(limit: Int = 10): Future[List[YeastAggregate]]
  
  /**
   * Levures ajoutées récemment
   */
  def findRecentlyAdded(limit: Int = 5): Future[List[YeastAggregate]]
}

/**
 * Résultat paginé générique
 */
case class PaginatedResult[T](
  items: List[T],
  totalCount: Long,
  page: Int,
  size: Int
) {
  def hasNextPage: Boolean = (page + 1) * size < totalCount
  def hasPreviousPage: Boolean = page > 0
  def totalPages: Long = math.ceil(totalCount.toDouble / size).toLong
}
EOF

cat > app/domain/yeasts/repositories/YeastWriteRepository.scala << 'EOF'
package domain.yeasts.repositories

import domain.yeasts.model._
import domain.yeasts.model.YeastEvent._
import scala.concurrent.Future

/**
 * Repository d'écriture pour les levures (CQRS - Command side)
 * Interface optimisée pour les modifications et Event Sourcing
 */
trait YeastWriteRepository {
  
  /**
   * Crée une nouvelle levure
   */
  def create(yeast: YeastAggregate): Future[YeastAggregate]
  
  /**
   * Met à jour une levure existante
   */
  def update(yeast: YeastAggregate): Future[YeastAggregate]
  
  /**
   * Archive une levure (soft delete)
   */
  def archive(yeastId: YeastId, reason: Option[String]): Future[Unit]
  
  /**
   * Active une levure
   */
  def activate(yeastId: YeastId): Future[Unit]
  
  /**
   * Désactive une levure
   */
  def deactivate(yeastId: YeastId, reason: Option[String]): Future[Unit]
  
  /**
   * Change le statut d'une levure
   */
  def changeStatus(
    yeastId: YeastId, 
    newStatus: YeastStatus, 
    reason: Option[String] = None
  ): Future[Unit]
  
  /**
   * Sauvegarde les événements (Event Sourcing)
   */
  def saveEvents(yeastId: YeastId, events: List[YeastEvent], expectedVersion: Long): Future[Unit]
  
  /**
   * Récupère tous les événements d'une levure
   */
  def getEvents(yeastId: YeastId): Future[List[YeastEvent]]
  
  /**
   * Récupère les événements depuis une version
   */
  def getEventsSinceVersion(yeastId: YeastId, sinceVersion: Long): Future[List[YeastEvent]]
  
  /**
   * Opération batch : créer plusieurs levures
   */
  def createBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]]
  
  /**
   * Opération batch : mettre à jour plusieurs levures
   */
  def updateBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]]
}
EOF

echo -e "${GREEN}✅ Interfaces repository créées${NC}"

# =============================================================================
# ÉTAPE 2: IMPLÉMENTATION SLICK - TABLE DEFINITIONS
# =============================================================================

echo -e "\n${YELLOW}🗃️ Création définitions tables Slick...${NC}"

mkdir -p app/infrastructure/persistence/slick/tables

cat > app/infrastructure/persistence/slick/tables/YeastsTable.scala << 'EOF'
package infrastructure.persistence.slick.tables

import domain.yeasts.model._
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.UUID

/**
 * Définition table yeasts pour Slick
 * Mapping objet-relationnel pour PostgreSQL
 */
class YeastsTable(tag: Tag) extends Table[YeastRow](tag, "yeasts") {
  
  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def laboratory = column[String]("laboratory")
  def strain = column[String]("strain")
  def yeastType = column[String]("yeast_type")
  def attenuationMin = column[Int]("attenuation_min")
  def attenuationMax = column[Int]("attenuation_max")
  def temperatureMin = column[Int]("temperature_min")
  def temperatureMax = column[Int]("temperature_max")
  def alcoholTolerance = column[Double]("alcohol_tolerance")
  def flocculation = column[String]("flocculation")
  def characteristics = column[String]("characteristics") // JSON
  def status = column[String]("status")
  def version = column[Long]("version")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  
  // Index composé pour performance
  def idxLaboratoryStrain = index("idx_yeasts_laboratory_strain", (laboratory, strain), unique = true)
  def idxStatus = index("idx_yeasts_status", status)
  def idxType = index("idx_yeasts_type", yeastType)
  def idxLaboratory = index("idx_yeasts_laboratory", laboratory)
  def idxAttenuation = index("idx_yeasts_attenuation", (attenuationMin, attenuationMax))
  def idxTemperature = index("idx_yeasts_temperature", (temperatureMin, temperatureMax))
  
  def * = (
    id, name, laboratory, strain, yeastType,
    attenuationMin, attenuationMax, temperatureMin, temperatureMax,
    alcoholTolerance, flocculation, characteristics, status,
    version, createdAt, updatedAt
  ) <> (YeastRow.tupled, YeastRow.unapply)
}

/**
 * Row class pour mapping Slick
 */
case class YeastRow(
  id: UUID,
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  characteristics: String, // JSON serialized
  status: String,
  version: Long,
  createdAt: Instant,
  updatedAt: Instant
)

object YeastRow {
  
  /**
   * Conversion YeastAggregate -> YeastRow
   */
  def fromAggregate(yeast: YeastAggregate): YeastRow = {
    YeastRow(
      id = yeast.id.value,
      name = yeast.name.value,
      laboratory = yeast.laboratory.name,
      strain = yeast.strain.value,
      yeastType = yeast.yeastType.name,
      attenuationMin = yeast.attenuation.min,
      attenuationMax = yeast.attenuation.max,
      temperatureMin = yeast.temperature.min,
      temperatureMax = yeast.temperature.max,
      alcoholTolerance = yeast.alcoholTolerance.percentage,
      flocculation = yeast.flocculation.name,
      characteristics = Json.toJson(yeast.characteristics).toString(),
      status = yeast.status.name,
      version = yeast.version,
      createdAt = yeast.createdAt,
      updatedAt = yeast.updatedAt
    )
  }
  
  /**
   * Conversion YeastRow -> YeastAggregate
   */
  def toAggregate(row: YeastRow): Either[String, YeastAggregate] = {
    for {
      yeastId <- Right(YeastId(row.id))
      name <- YeastName.fromString(row.name)
      laboratory <- YeastLaboratory.fromName(row.laboratory)
        .toRight(s"Laboratoire inconnu: ${row.laboratory}")
      strain <- YeastStrain.fromString(row.strain)
      yeastType <- YeastType.fromName(row.yeastType)
        .toRight(s"Type levure inconnu: ${row.yeastType}")
      attenuation <- Right(AttenuationRange(row.attenuationMin, row.attenuationMax))
      temperature <- Right(FermentationTemp(row.temperatureMin, row.temperatureMax))
      alcoholTolerance <- Right(AlcoholTolerance(row.alcoholTolerance))
      flocculation <- FlocculationLevel.fromName(row.flocculation)
        .toRight(s"Floculation inconnue: ${row.flocculation}")
      characteristics <- parseCharacteristics(row.characteristics)
      status <- YeastStatus.fromName(row.status)
        .toRight(s"Statut inconnu: ${row.status}")
    } yield {
      YeastAggregate(
        id = yeastId,
        name = name,
        laboratory = laboratory,
        strain = strain,
        yeastType = yeastType,
        attenuation = attenuation,
        temperature = temperature,
        alcoholTolerance = alcoholTolerance,
        flocculation = flocculation,
        characteristics = characteristics,
        status = status,
        version = row.version,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt
      )
    }
  }
  
  private def parseCharacteristics(json: String): Either[String, YeastCharacteristics] = {
    try {
      val parsed = Json.parse(json)
      val aromaProfile = (parsed \ "aromaProfile").asOpt[List[String]].getOrElse(List.empty)
      val flavorProfile = (parsed \ "flavorProfile").asOpt[List[String]].getOrElse(List.empty)
      val esters = (parsed \ "esters").asOpt[List[String]].getOrElse(List.empty)
      val phenols = (parsed \ "phenols").asOpt[List[String]].getOrElse(List.empty)
      val otherCompounds = (parsed \ "otherCompounds").asOpt[List[String]].getOrElse(List.empty)
      val notes = (parsed \ "notes").asOpt[String]
      
      Right(YeastCharacteristics(aromaProfile, flavorProfile, esters, phenols, otherCompounds, notes))
    } catch {
      case _: Exception => Left(s"Impossible de parser les caractéristiques: $json")
    }
  }
  
  // Formatters JSON implicites pour YeastCharacteristics
  implicit val yeastCharacteristicsWrites: Writes[YeastCharacteristics] = Json.writes[YeastCharacteristics]
  implicit val yeastCharacteristicsReads: Reads[YeastCharacteristics] = Json.reads[YeastCharacteristics]
}

/**
 * Companion object pour la table
 */
object YeastsTable {
  val yeasts = TableQuery[YeastsTable]
}
EOF

# =============================================================================
# ÉTAPE 3: TABLE ÉVÉNEMENTS
# =============================================================================

echo -e "\n${YELLOW}📅 Création table événements...${NC}"

cat > app/infrastructure/persistence/slick/tables/YeastEventsTable.scala << 'EOF'
package infrastructure.persistence.slick.tables

import domain.yeasts.model.YeastEvent
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.UUID

/**
 * Table pour Event Sourcing des levures
 */
class YeastEventsTable(tag: Tag) extends Table[YeastEventRow](tag, "yeast_events") {
  
  def id = column[UUID]("id", O.PrimaryKey, O.AutoInc)
  def yeastId = column[UUID]("yeast_id")
  def eventType = column[String]("event_type")
  def eventData = column[String]("event_data") // JSON
  def version = column[Long]("version")
  def occurredAt = column[Instant]("occurred_at")
  def createdBy = column[Option[UUID]]("created_by")
  
  def idxYeastId = index("idx_yeast_events_yeast_id", yeastId)
  def idxYeastIdVersion = index("idx_yeast_events_yeast_id_version", (yeastId, version))
  def idxOccurredAt = index("idx_yeast_events_occurred_at", occurredAt)
  
  def * = (id, yeastId, eventType, eventData, version, occurredAt, createdBy) <> 
    (YeastEventRow.tupled, YeastEventRow.unapply)
}

case class YeastEventRow(
  id: UUID,
  yeastId: UUID,
  eventType: String,
  eventData: String,
  version: Long,
  occurredAt: Instant,
  createdBy: Option[UUID]
)

object YeastEventRow {
  
  def fromEvent(event: YeastEvent): YeastEventRow = {
    val eventType = event.getClass.getSimpleName
    val eventData = Json.toJson(event)(eventWrites).toString()
    val createdBy = extractCreatedBy(event)
    
    YeastEventRow(
      id = UUID.randomUUID(),
      yeastId = event.yeastId.value,
      eventType = eventType,
      eventData = eventData,
      version = event.version,
      occurredAt = event.occurredAt,
      createdBy = createdBy
    )
  }
  
  def toEvent(row: YeastEventRow): Either[String, YeastEvent] = {
    try {
      val json = Json.parse(row.eventData)
      row.eventType match {
        case "YeastCreated" => json.as[YeastEvent.YeastCreated](yeastCreatedReads).asRight
        case "YeastUpdated" => json.as[YeastEvent.YeastUpdated](yeastUpdatedReads).asRight
        case "YeastStatusChanged" => json.as[YeastEvent.YeastStatusChanged](yeastStatusChangedReads).asRight
        case "YeastActivated" => json.as[YeastEvent.YeastActivated](yeastActivatedReads).asRight
        case "YeastDeactivated" => json.as[YeastEvent.YeastDeactivated](yeastDeactivatedReads).asRight
        case "YeastArchived" => json.as[YeastEvent.YeastArchived](yeastArchivedReads).asRight
        case "YeastTechnicalDataUpdated" => json.as[YeastEvent.YeastTechnicalDataUpdated](yeastTechnicalDataUpdatedReads).asRight
        case "YeastLaboratoryInfoUpdated" => json.as[YeastEvent.YeastLaboratoryInfoUpdated](yeastLaboratoryInfoUpdatedReads).asRight
        case unknown => Left(s"Type d'événement inconnu: $unknown")
      }
    } catch {
      case e: Exception => Left(s"Erreur parsing événement: ${e.getMessage}")
    }
  }
  
  private def extractCreatedBy(event: YeastEvent): Option[UUID] = {
    event match {
      case e: YeastEvent.YeastCreated => Some(e.createdBy)
      case e: YeastEvent.YeastUpdated => Some(e.updatedBy)
      case e: YeastEvent.YeastStatusChanged => Some(e.changedBy)
      case e: YeastEvent.YeastActivated => Some(e.activatedBy)
      case e: YeastEvent.YeastDeactivated => Some(e.deactivatedBy)
      case e: YeastEvent.YeastArchived => Some(e.archivedBy)
      case e: YeastEvent.YeastTechnicalDataUpdated => Some(e.updatedBy)
      case e: YeastEvent.YeastLaboratoryInfoUpdated => Some(e.updatedBy)
    }
  }
  
  // JSON Formatters pour les événements
  implicit val yeastIdWrites: Writes[YeastId] = (o: YeastId) => JsString(o.value.toString)
  implicit val yeastIdReads: Reads[YeastId] = (json: JsValue) => json.validate[String].map(s => YeastId(UUID.fromString(s)))
  
  implicit val yeastNameWrites: Writes[YeastName] = (o: YeastName) => JsString(o.value)
  implicit val yeastNameReads: Reads[YeastName] = (json: JsValue) => json.validate[String].flatMap(s => 
    YeastName.fromString(s).fold(e => JsError(e), JsSuccess(_)))
  
  // Continuer avec les autres formatters...
  implicit val eventWrites: Writes[YeastEvent] = new Writes[YeastEvent] {
    def writes(event: YeastEvent): JsValue = event match {
      case e: YeastEvent.YeastCreated => Json.toJson(e)
      case e: YeastEvent.YeastUpdated => Json.toJson(e)
      case e: YeastEvent.YeastStatusChanged => Json.toJson(e)
      case e: YeastEvent.YeastActivated => Json.toJson(e)
      case e: YeastEvent.YeastDeactivated => Json.toJson(e)
      case e: YeastEvent.YeastArchived => Json.toJson(e)
      case e: YeastEvent.YeastTechnicalDataUpdated => Json.toJson(e)
      case e: YeastEvent.YeastLaboratoryInfoUpdated => Json.toJson(e)
    }
  }
  
  // Readers spécifiques (simplifiés pour l'exemple)
  implicit val yeastCreatedReads: Reads[YeastEvent.YeastCreated] = Json.reads[YeastEvent.YeastCreated]
  implicit val yeastUpdatedReads: Reads[YeastEvent.YeastUpdated] = Json.reads[YeastEvent.YeastUpdated]
  implicit val yeastStatusChangedReads: Reads[YeastEvent.YeastStatusChanged] = Json.reads[YeastEvent.YeastStatusChanged]
  implicit val yeastActivatedReads: Reads[YeastEvent.YeastActivated] = Json.reads[YeastEvent.YeastActivated]
  implicit val yeastDeactivatedReads: Reads[YeastEvent.YeastDeactivated] = Json.reads[YeastEvent.YeastDeactivated]
  implicit val yeastArchivedReads: Reads[YeastEvent.YeastArchived] = Json.reads[YeastEvent.YeastArchived]
  implicit val yeastTechnicalDataUpdatedReads: Reads[YeastEvent.YeastTechnicalDataUpdated] = Json.reads[YeastEvent.YeastTechnicalDataUpdated]
  implicit val yeastLaboratoryInfoUpdatedReads: Reads[YeastEvent.YeastLaboratoryInfoUpdated] = Json.reads[YeastEvent.YeastLaboratoryInfoUpdated]
}

object YeastEventsTable {
  val yeastEvents = TableQuery[YeastEventsTable]
}
EOF

# =============================================================================
# ÉTAPE 4: IMPLÉMENTATION SLICK READ REPOSITORY
# =============================================================================

echo -e "\n${YELLOW}📖 Création SlickYeastReadRepository...${NC}"

mkdir -p app/infrastructure/persistence/slick/repositories/yeasts

cat > app/infrastructure/persistence/slick/repositories/yeasts/SlickYeastReadRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import domain.yeasts.repositories.{YeastReadRepository, PaginatedResult}
import infrastructure.persistence.slick.tables.{YeastsTable, YeastRow}
import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

/**
 * Implémentation Slick du repository de lecture des levures
 */
@Singleton
class SlickYeastReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends YeastReadRepository with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  private val yeasts = YeastsTable.yeasts

  override def findById(yeastId: YeastId): Future[Option[YeastAggregate]] = {
    db.run(yeasts.filter(_.id === yeastId.value).result.headOption)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption))
  }

  override def findByName(name: YeastName): Future[Option[YeastAggregate]] = {
    db.run(yeasts.filter(_.name === name.value).result.headOption)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption))
  }

  override def findByLaboratoryAndStrain(
    laboratory: YeastLaboratory, 
    strain: YeastStrain
  ): Future[Option[YeastAggregate]] = {
    db.run(
      yeasts
        .filter(y => y.laboratory === laboratory.name && y.strain === strain.value)
        .result.headOption
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption))
  }

  override def findByFilter(filter: YeastFilter): Future[PaginatedResult[YeastAggregate]] = {
    val baseQuery = yeasts.filter(buildWhereClause(filter))
    
    val countQuery = baseQuery.length
    val dataQuery = baseQuery
      .drop(filter.page * filter.size)
      .take(filter.size)
      .sortBy(_.name) // Tri par défaut par nom
    
    val combinedQuery = for {
      total <- countQuery.result
      items <- dataQuery.result
    } yield (total, items)
    
    db.run(combinedQuery).map { case (total, rows) =>
      val aggregates = rows.flatMap(row => YeastRow.toAggregate(row).toOption).toList
      PaginatedResult(aggregates, total.toLong, filter.page, filter.size)
    }
  }

  override def findByType(yeastType: YeastType): Future[List[YeastAggregate]] = {
    db.run(yeasts.filter(_.yeastType === yeastType.name).result)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByLaboratory(laboratory: YeastLaboratory): Future[List[YeastAggregate]] = {
    db.run(yeasts.filter(_.laboratory === laboratory.name).result)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByStatus(status: YeastStatus): Future[List[YeastAggregate]] = {
    db.run(yeasts.filter(_.status === status.name).result)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByAttenuationRange(
    minAttenuation: Int, 
    maxAttenuation: Int
  ): Future[List[YeastAggregate]] = {
    db.run(
      yeasts.filter(y => 
        y.attenuationMin >= minAttenuation && y.attenuationMax <= maxAttenuation
      ).result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByTemperatureRange(minTemp: Int, maxTemp: Int): Future[List[YeastAggregate]] = {
    db.run(
      yeasts.filter(y => 
        y.temperatureMin >= minTemp && y.temperatureMax <= maxTemp
      ).result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByCharacteristics(characteristics: List[String]): Future[List[YeastAggregate]] = {
    if (characteristics.isEmpty) {
      Future.successful(List.empty)
    } else {
      val pattern = characteristics.mkString("|") // OR pattern pour recherche
      db.run(
        yeasts.filter(_.characteristics.toLowerCase like s"%${pattern.toLowerCase}%").result
      ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
    }
  }

  override def searchText(query: String): Future[List[YeastAggregate]] = {
    val searchPattern = s"%${query.toLowerCase}%"
    db.run(
      yeasts.filter(y => 
        y.name.toLowerCase.like(searchPattern) || 
        y.characteristics.toLowerCase.like(searchPattern)
      ).result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def countByStatus: Future[Map[YeastStatus, Long]] = {
    db.run(
      yeasts.groupBy(_.status).map { case (status, group) => 
        (status, group.length) 
      }.result
    ).map(_.toMap.flatMap { case (statusName, count) =>
      YeastStatus.fromName(statusName).map(_ -> count.toLong)
    })
  }

  override def getStatsByLaboratory: Future[Map[YeastLaboratory, Long]] = {
    db.run(
      yeasts.groupBy(_.laboratory).map { case (lab, group) => 
        (lab, group.length) 
      }.result
    ).map(_.toMap.flatMap { case (labName, count) =>
      YeastLaboratory.fromName(labName).map(_ -> count.toLong)
    })
  }

  override def getStatsByType: Future[Map[YeastType, Long]] = {
    db.run(
      yeasts.groupBy(_.yeastType).map { case (yeastType, group) => 
        (yeastType, group.length) 
      }.result
    ).map(_.toMap.flatMap { case (typeName, count) =>
      YeastType.fromName(typeName).map(_ -> count.toLong)
    })
  }

  override def findMostPopular(limit: Int = 10): Future[List[YeastAggregate]] = {
    // TODO: Implémenter selon métriques d'usage réelles
    // Pour l'instant, retourne les plus récentes
    findRecentlyAdded(limit)
  }

  override def findRecentlyAdded(limit: Int = 5): Future[List[YeastAggregate]] = {
    db.run(
      yeasts
        .sortBy(_.createdAt.desc)
        .take(limit)
        .result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - CONSTRUCTION REQUÊTES
  // ==========================================================================

  private def buildWhereClause(filter: YeastFilter): YeastsTable => Rep[Boolean] = { yeast =>
    var conditions: Rep[Boolean] = LiteralColumn(true)

    filter.name.foreach { name =>
      conditions = conditions && yeast.name.toLowerCase.like(s"%${name.toLowerCase}%")
    }

    filter.laboratory.foreach { lab =>
      conditions = conditions && yeast.laboratory === lab.name
    }

    filter.yeastType.foreach { yType =>
      conditions = conditions && yeast.yeastType === yType.name
    }

    filter.minAttenuation.foreach { minAtt =>
      conditions = conditions && yeast.attenuationMax >= minAtt
    }

    filter.maxAttenuation.foreach { maxAtt =>
      conditions = conditions && yeast.attenuationMin <= maxAtt
    }

    filter.minTemperature.foreach { minTemp =>
      conditions = conditions && yeast.temperatureMax >= minTemp
    }

    filter.maxTemperature.foreach { maxTemp =>
      conditions = conditions && yeast.temperatureMin <= maxTemp
    }

    filter.minAlcoholTolerance.foreach { minAlc =>
      conditions = conditions && yeast.alcoholTolerance >= minAlc
    }

    filter.maxAlcoholTolerance.foreach { maxAlc =>
      conditions = conditions && yeast.alcoholTolerance <= maxAlc
    }

    filter.flocculation.foreach { floc =>
      conditions = conditions && yeast.flocculation === floc.name
    }

    if (filter.characteristics.nonEmpty) {
      val charPattern = filter.characteristics.mkString("|").toLowerCase
      conditions = conditions && yeast.characteristics.toLowerCase.like(s"%$charPattern%")
    }

    if (filter.status.nonEmpty) {
      val statusNames = filter.status.map(_.name)
      conditions = conditions && yeast.status.inSet(statusNames)
    }

    conditions
  }
}
EOF

echo -e "${GREEN}✅ SlickYeastReadRepository créé${NC}"

# =============================================================================
# ÉTAPE 5: RÉSUMÉ ET PROCHAINES ÉTAPES
# =============================================================================

echo -e "\n${BLUE}🎯 RÉSUMÉ REPOSITORIES (PARTIE 1/2)${NC}"
echo -e "${BLUE}==================================${NC}"
echo ""
echo -e "${GREEN}✅ Interfaces repository créées :${NC}"
echo -e "   • YeastRepository (combiné)"
echo -e "   • YeastReadRepository (CQRS Query)"
echo -e "   • YeastWriteRepository (CQRS Command)"
echo ""
echo -e "${GREEN}✅ Tables Slick définies :${NC}"
echo -e "   • YeastsTable (données principales)"
echo -e "   • YeastEventsTable (Event Sourcing)"
echo -e "   • Index optimisés pour performance"
echo ""
echo -e "${GREEN}✅ Repository lecture implémenté :${NC}"
echo -e "   • SlickYeastReadRepository"
echo -e "   • Recherche par filtres avancés"
echo -e "   • Pagination intégrée"
echo -e "   • Statistiques par laboratoire/type"
echo ""
echo -e "${YELLOW}📋 SUITE DU SCRIPT :${NC}"
echo -e "${YELLOW}   Continuer pour créer :${NC}"
echo -e "${YELLOW}   • SlickYeastWriteRepository${NC}"
echo -e "${YELLOW}   • SlickYeastRepository (combiné)${NC}"
echo -e "${YELLOW}   • Tests d'intégration${NC}"