package infrastructure.persistence.slick.repositories.hops

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

import domain.hops.model._
import domain.hops.repositories.HopWriteRepository
import domain.shared.NonEmptyString

@Singleton
class SlickHopWriteRepository @Inject()(
                                         protected val dbConfigProvider: DatabaseConfigProvider
                                       )(implicit ec: ExecutionContext)
  extends HopWriteRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  // ===============================
  // DÉFINITION DES TABLES ET ROWS
  // ===============================

  case class HopRow(
                     id: String,
                     name: String,
                     alphaAcid: Double,
                     betaAcid: Option[Double],
                     originCode: String,
                     usage: String,
                     description: Option[String],
                     status: String,
                     source: String,
                     credibilityScore: Int,
                     createdAt: Instant,
                     updatedAt: Instant,
                     version: Long
                   )

  class HopsTable(tag: Tag) extends Table[HopRow](tag, "hops") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def alphaAcid = column[Double]("alpha_acid")
    def betaAcid = column[Option[Double]]("beta_acid")
    def originCode = column[String]("origin_code")
    def usage = column[String]("usage")
    def description = column[Option[String]]("description")
    def status = column[String]("status")
    def source = column[String]("source")
    def credibilityScore = column[Int]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, alphaAcid, betaAcid, originCode, usage, description, status, source, credibilityScore, createdAt, updatedAt, version).mapTo[HopRow]
  }

  val hops = TableQuery[HopsTable]

  // ===============================
  // MÉTHODES DE CONVERSION
  // ===============================

  /**
   * Convertit un HopAggregate du domaine vers une HopRow pour la base de données
   */
  private def mapAggregateToRow(hop: HopAggregate): HopRow = {
    HopRow(
      id = hop.id.value,
      name = hop.name.value,
      alphaAcid = hop.alphaAcid.value,
      betaAcid = hop.betaAcid.map(_.value),
      originCode = hop.origin.code,
      usage = hop.usage match {
        case HopUsage.Bittering => "BITTERING"
        case HopUsage.Aroma => "AROMA"
        case HopUsage.DualPurpose => "DUAL_PURPOSE"
        case HopUsage.NobleHop => "NOBLE_HOP"
      },
      description = hop.description.map(_.value),
      status = hop.status match {
        case HopStatus.Active => "ACTIVE"
        case HopStatus.Discontinued => "DISCONTINUED"
        case HopStatus.Limited => "LIMITED"
      },
      source = hop.source match {
        case HopSource.Manual => "MANUAL"
        case HopSource.AI_Discovery => "AI_DISCOVERED"
        case HopSource.Import => "IMPORT"
      },
      credibilityScore = hop.credibilityScore,
      createdAt = hop.createdAt,
      updatedAt = hop.updatedAt,
      version = hop.version.toLong
    )
  }

  /**
   * Convertit une HopRow de la base de données vers un HopAggregate du domaine
   */
  private def mapRowToAggregate(row: HopRow): HopAggregate = {
    try {
      val hopId = HopId(row.id)
      val hopName = NonEmptyString(row.name)
      val alphaAcid = AlphaAcidPercentage(row.alphaAcid)

      val betaAcid = row.betaAcid.map(ba => AlphaAcidPercentage(ba))

      val origin = HopOrigin.fromCode(row.originCode).getOrElse(
        HopOrigin(row.originCode, row.originCode, "Unknown")
      )

      val usage = HopUsage.fromString(row.usage)
      val status = HopStatus.fromString(row.status)
      val source = HopSource.fromString(row.source)

      val description = row.description.map(d => NonEmptyString(d))

      HopAggregate(
        id = hopId,
        name = hopName,
        alphaAcid = alphaAcid,
        betaAcid = betaAcid,
        origin = origin,
        usage = usage,
        description = description,
        aromaProfile = List.empty,
        status = status,
        source = source,
        credibilityScore = row.credibilityScore,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version.toInt
      )
    } catch {
      case ex: Exception =>
        println(s"ERREUR MAPPING houblon ${row.id}: ${ex.getMessage}")
        ex.printStackTrace()
        throw ex
    }
  }

  // ===============================
  // IMPLÉMENTATION DES MÉTHODES WRITE
  // ===============================

  override def save(hop: HopAggregate): Future[HopAggregate] = {
    val hopRow = mapAggregateToRow(hop)
    val action = hops.insertOrUpdate(hopRow)

    db.run(action).map(_ => hop).recover {
      case ex =>
        println(s"Erreur lors de la sauvegarde du houblon ${hop.id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        throw ex
    }
  }

  override def saveAll(hopsList: List[HopAggregate]): Future[List[HopAggregate]] = {
    val hopRows = hopsList.map(mapAggregateToRow)
    val action = DBIO.sequence(hopRows.map(hops.insertOrUpdate))

    db.run(action.transactionally).map(_ => hopsList).recover {
      case ex =>
        println(s"Erreur lors de la sauvegarde en lot: ${ex.getMessage}")
        ex.printStackTrace()
        throw ex
    }
  }

  override def delete(id: HopId): Future[Boolean] = {
    val action = hops.filter(_.id === id.value).delete

    db.run(action).map(_ > 0).recover {
      case ex =>
        println(s"Erreur lors de la suppression du houblon ${id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        false
    }
  }

  override def update(hop: HopAggregate): Future[Option[HopAggregate]] = {
    val hopRow = mapAggregateToRow(hop)
    val action = hops.filter(_.id === hop.id.value).update(hopRow)

    db.run(action).map { rowsAffected =>
      if (rowsAffected > 0) Some(hop) else None
    }.recover {
      case ex =>
        println(s"Erreur lors de la mise à jour du houblon ${hop.id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  override def exists(id: HopId): Future[Boolean] = {
    val query = hops.filter(_.id === id.value).exists

    db.run(query.result).recover {
      case ex =>
        println(s"Erreur lors de la vérification d'existence du houblon ${id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        false
    }
  }

  // ===============================
  // MÉTHODES UTILITAIRES
  // ===============================

  def findById(id: HopId): Future[Option[HopAggregate]] = {
    val query = hops.filter(_.id === id.value)

    db.run(query.result.headOption).map { rowOpt =>
      rowOpt.map(mapRowToAggregate)
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche du houblon ${id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  def deleteAll(): Future[Int] = {
    val action = hops.delete

    db.run(action).recover {
      case ex =>
        println(s"Erreur lors de la suppression de tous les houblons: ${ex.getMessage}")
        ex.printStackTrace()
        0
    }
  }

  def count(): Future[Int] = {
    val action = hops.length.result

    db.run(action).recover {
      case ex =>
        println(s"Erreur lors du comptage des houblons: ${ex.getMessage}")
        ex.printStackTrace()
        0
    }
  }
}