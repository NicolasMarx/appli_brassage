package infrastructure.persistence.slick.repositories.hops

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

import domain.hops.model._
import domain.hops.repositories.HopReadRepository
import domain.shared.NonEmptyString

@Singleton
class SlickHopReadRepository @Inject()(
                                        protected val dbConfigProvider: DatabaseConfigProvider
                                      )(implicit ec: ExecutionContext)
  extends HopReadRepository
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

  case class OriginRow(
                        id: String,
                        name: String,
                        region: Option[String]
                      )

  case class AromaProfileRow(
                              id: String,
                              name: String,
                              category: Option[String]
                            )

  case class HopAromaProfileRow(
                                 hopId: String,
                                 aromaId: String,
                                 intensity: Option[Int]
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

  class OriginsTable(tag: Tag) extends Table[OriginRow](tag, "origins") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def region = column[Option[String]]("region")
    def * = (id, name, region).mapTo[OriginRow]
  }

  class AromaProfilesTable(tag: Tag) extends Table[AromaProfileRow](tag, "aroma_profiles") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def category = column[Option[String]]("category")
    def * = (id, name, category).mapTo[AromaProfileRow]
  }

  class HopAromaProfilesTable(tag: Tag) extends Table[HopAromaProfileRow](tag, "hop_aroma_profiles") {
    def hopId = column[String]("hop_id")
    def aromaId = column[String]("aroma_id")
    def intensity = column[Option[Int]]("intensity")
    def * = (hopId, aromaId, intensity).mapTo[HopAromaProfileRow]
  }

  val hops = TableQuery[HopsTable]
  val origins = TableQuery[OriginsTable]
  val aromaProfiles = TableQuery[AromaProfilesTable]
  val hopAromaProfiles = TableQuery[HopAromaProfilesTable]

  // ===============================
  // RÉCUPÉRATION DES ARÔMES (NOUVELLE MÉTHODE)
  // ===============================

  /**
   * Récupère la liste des noms d'arômes pour un houblon donné
   */
  private def getAromaProfilesForHop(hopId: String): Future[List[String]] = {
    val query = (for {
      hap <- hopAromaProfiles if hap.hopId === hopId
      ap <- aromaProfiles if ap.id === hap.aromaId
    } yield ap.name).result

    db.run(query).map(_.toList)
  }

  // ===============================
  // MAPPING VERS LE DOMAINE (CORRIGÉ)
  // ===============================

  /**
   * Convertit une HopRow vers un HopAggregate AVEC ses arômes
   */
  private def mapRowToAggregateWithAromas(row: HopRow): Future[HopAggregate] = {
    getAromaProfilesForHop(row.id).map { aromaNames =>
      try {
        val hopId = HopId(row.id)
        val hopName = NonEmptyString(row.name)
        val alphaAcid = AlphaAcidPercentage(row.alphaAcid)

        // Traitement optionnel de betaAcid
        val betaAcid = row.betaAcid.map(ba => AlphaAcidPercentage(ba))

        // Création de HopOrigin
        val origin = HopOrigin.fromCode(row.originCode).getOrElse(
          HopOrigin(row.originCode, row.originCode, "Unknown")
        )

        // Mapping des énumérations
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
          aromaProfile = aromaNames, // ✅ ARÔMES RÉCUPÉRÉS !
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
  }

  // ===============================
  // IMPLÉMENTATION DES MÉTHODES (MISES À JOUR)
  // ===============================

  override def byId(id: HopId): Future[Option[HopAggregate]] = {
    val query = hops.filter(_.id === id.value)

    db.run(query.result.headOption).flatMap {
      case Some(row) => mapRowToAggregateWithAromas(row).map(Some(_))
      case None => Future.successful(None)
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par ID ${id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  override def byName(name: NonEmptyString): Future[Option[HopAggregate]] = {
    val query = hops.filter(_.name === name.value)

    db.run(query.result.headOption).flatMap {
      case Some(row) => mapRowToAggregateWithAromas(row).map(Some(_))
      case None => Future.successful(None)
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par nom ${name.value}: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  override def all(page: Int = 0, size: Int = 20): Future[(List[HopAggregate], Int)] = {
    val offset = page * size

    val countQuery = hops.length
    val dataQuery = hops
      .sortBy(_.name)
      .drop(offset)
      .take(size)

    (for {
      totalCount <- db.run(countQuery.result)
      hopsData <- db.run(dataQuery.result)
      hopsWithAromas <- Future.traverse(hopsData) { row =>
        mapRowToAggregateWithAromas(row)
      }
    } yield {
      (hopsWithAromas.toList, totalCount)
    }).recover {
      case ex =>
        println(s"Erreur lors de la récupération de tous les houblons (page=$page, size=$size): ${ex.getMessage}")
        ex.printStackTrace()
        (List.empty[HopAggregate], 0)
    }
  }

  override def searchByCharacteristics(
                                        name: Option[String] = None,
                                        originCode: Option[String] = None,
                                        usage: Option[String] = None,
                                        minAlphaAcid: Option[Double] = None,
                                        maxAlphaAcid: Option[Double] = None,
                                        activeOnly: Boolean = true
                                      ): Future[Seq[HopAggregate]] = {
    var query = hops.asInstanceOf[Query[HopsTable, HopRow, Seq]]

    if (activeOnly) {
      query = query.filter(_.status === "ACTIVE")
    }

    name.foreach { n =>
      query = query.filter(_.name.toLowerCase like s"%${n.toLowerCase}%")
    }

    originCode.foreach { origin =>
      query = query.filter(_.originCode === origin)
    }

    usage.foreach { u =>
      query = query.filter(_.usage === u)
    }

    minAlphaAcid.foreach { min =>
      query = query.filter(_.alphaAcid >= min)
    }

    maxAlphaAcid.foreach { max =>
      query = query.filter(_.alphaAcid <= max)
    }

    db.run(query.sortBy(_.name).result).flatMap { rows =>
      Future.traverse(rows) { row =>
        mapRowToAggregateWithAromas(row)
      }
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par caractéristiques: ${ex.getMessage}")
        ex.printStackTrace()
        Seq.empty[HopAggregate]
    }
  }

  override def byStatus(status: String): Future[Seq[HopAggregate]] = {
    val query = hops.filter(_.status === status)

    db.run(query.sortBy(_.name).result).flatMap { rows =>
      Future.traverse(rows) { row =>
        mapRowToAggregateWithAromas(row)
      }
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par statut $status: ${ex.getMessage}")
        ex.printStackTrace()
        Seq.empty[HopAggregate]
    }
  }

  override def aiDiscovered(limit: Int = 50): Future[Seq[HopAggregate]] = {
    val query = hops
      .filter(_.source === "AI_DISCOVERED")
      .sortBy(_.createdAt.desc)
      .take(limit)

    db.run(query.result).flatMap { rows =>
      Future.traverse(rows) { row =>
        mapRowToAggregateWithAromas(row)
      }
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche des houblons découverts par IA: ${ex.getMessage}")
        ex.printStackTrace()
        Seq.empty[HopAggregate]
    }
  }

  // ===============================
  // MÉTHODES D'ALIAS POUR COMPATIBILITÉ (MISES À JOUR)
  // ===============================

  override def findById(id: HopId): Future[Option[HopAggregate]] = byId(id)

  override def findActiveHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)] = {
    val offset = page * pageSize

    val countQuery = hops.filter(_.status === "ACTIVE").length
    val dataQuery = hops
      .filter(_.status === "ACTIVE")
      .sortBy(_.name)
      .drop(offset)
      .take(pageSize)

    (for {
      totalCount <- db.run(countQuery.result)
      hopsData <- db.run(dataQuery.result)
      hopsWithAromas <- Future.traverse(hopsData) { row =>
        mapRowToAggregateWithAromas(row)
      }
    } yield {
      (hopsWithAromas.toList, totalCount)
    }).recover {
      case ex =>
        println(s"Erreur lors de la récupération des houblons actifs (page=$page, size=$pageSize): ${ex.getMessage}")
        ex.printStackTrace()
        (List.empty[HopAggregate], 0)
    }
  }

  override def searchHops(
                           name: Option[String] = None,
                           originCode: Option[String] = None,
                           usage: Option[String] = None,
                           minAlphaAcid: Option[Double] = None,
                           maxAlphaAcid: Option[Double] = None,
                           activeOnly: Boolean = true
                         ): Future[List[HopAggregate]] = {
    searchByCharacteristics(name, originCode, usage, minAlphaAcid, maxAlphaAcid, activeOnly).map(_.toList)
  }

  // ===============================
  // MÉTHODES UTILITAIRES (MISES À JOUR)
  // ===============================

  override def existsByName(name: String): Future[Boolean] = {
    val query = hops.filter(_.name.toLowerCase === name.toLowerCase).exists
    db.run(query.result).recover {
      case ex =>
        println(s"Erreur lors de la vérification d'existence du nom '$name': ${ex.getMessage}")
        ex.printStackTrace()
        false
    }
  }

  override def findByOrigin(originCode: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    var query = hops.filter(_.originCode === originCode)

    if (activeOnly) {
      query = query.filter(_.status === "ACTIVE")
    }

    db.run(query.sortBy(_.name).result).flatMap { rows =>
      Future.traverse(rows) { row =>
        mapRowToAggregateWithAromas(row)
      }.map(_.toList)
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par origine $originCode: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[HopAggregate]
    }
  }

  override def findByUsage(usage: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    var query = hops.filter(_.usage === usage)

    if (activeOnly) {
      query = query.filter(_.status === "ACTIVE")
    }

    db.run(query.sortBy(_.name).result).flatMap { rows =>
      Future.traverse(rows) { row =>
        mapRowToAggregateWithAromas(row)
      }.map(_.toList)
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par usage $usage: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[HopAggregate]
    }
  }

  override def findByAromaProfile(aromaId: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    val query = if (activeOnly) {
      for {
        (hop, aromaLink) <- hops join hopAromaProfiles on (_.id === _.hopId)
        if aromaLink.aromaId === aromaId
        if hop.status === "ACTIVE"
      } yield hop
    } else {
      for {
        (hop, aromaLink) <- hops join hopAromaProfiles on (_.id === _.hopId)
        if aromaLink.aromaId === aromaId
      } yield hop
    }

    db.run(query.sortBy(_.name).result).flatMap { rows =>
      Future.traverse(rows) { row =>
        mapRowToAggregateWithAromas(row)
      }.map(_.toList)
    }.recover {
      case ex =>
        println(s"Erreur lors de la recherche par profil aromatique $aromaId: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[HopAggregate]
    }
  }
}