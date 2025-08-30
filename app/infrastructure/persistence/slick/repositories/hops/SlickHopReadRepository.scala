// app/infrastructure/persistence/slick/repositories/hops/SlickHopReadRepository.scala
package infrastructure.persistence.slick.repositories.hops

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

import domain.hops.model._
import domain.hops.repositories.HopReadRepository
import domain.shared.NonEmptyString
import infrastructure.persistence.slick.tables.HopTables

@Singleton
class SlickHopReadRepository @Inject()(
                                        protected val dbConfigProvider: DatabaseConfigProvider
                                      )(implicit ec: ExecutionContext)
  extends HopReadRepository
    with HasDatabaseConfigProvider[JdbcProfile]
    with HopTables {

  import profile.api._

  def findById(id: HopId): Future[Option[HopAggregate]] = {
    val query = hops.filter(_.id === id.value)

    db.run(query.result.headOption).map { rowOpt =>
      rowOpt.map(row => mapRowToAggregate(row))
    }
  }

  def findActiveHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)] = {
    val offset = page * pageSize

    val countQuery = hops.filter(_.status === "ACTIVE").length
    val dataQuery = hops
      .filter(_.status === "ACTIVE")
      .sortBy(_.name)
      .drop(offset)
      .take(pageSize)

    for {
      totalCount <- db.run(countQuery.result)
      hopsData <- db.run(dataQuery.result)
    } yield {
      val hopsList = hopsData.map(mapRowToAggregate).toList
      (hopsList, totalCount)
    }
  }

  def findAllHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)] = {
    val offset = page * pageSize

    val countQuery = hops.length
    val dataQuery = hops
      .sortBy(_.name)
      .drop(offset)
      .take(pageSize)

    for {
      totalCount <- db.run(countQuery.result)
      hopsData <- db.run(dataQuery.result)
    } yield {
      val hopsList = hopsData.map(mapRowToAggregate).toList
      (hopsList, totalCount)
    }
  }

  def searchHops(
                  name: Option[String] = None,
                  originCode: Option[String] = None,
                  usage: Option[String] = None,
                  minAlphaAcid: Option[Double] = None,
                  maxAlphaAcid: Option[Double] = None,
                  activeOnly: Boolean = true
                ): Future[List[HopAggregate]] = {

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

    db.run(query.sortBy(_.name).result).map { rows =>
      rows.map(mapRowToAggregate).toList
    }
  }

  def findByOrigin(originCode: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    var query = hops.filter(_.originCode === originCode)

    if (activeOnly) {
      query = query.filter(_.status === "ACTIVE")
    }

    db.run(query.sortBy(_.name).result).map { rows =>
      rows.map(mapRowToAggregate).toList
    }
  }

  def findByUsage(usage: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    var query = hops.filter(_.usage === usage)

    if (activeOnly) {
      query = query.filter(_.status === "ACTIVE")
    }

    db.run(query.sortBy(_.name).result).map { rows =>
      rows.map(mapRowToAggregate).toList
    }
  }

  def findByAromaProfile(aromaId: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    if (activeOnly) {
      // Version avec filtre actif
      val query = for {
        (hop, aromaLink) <- hops join hopAromaProfiles on (_.id === _.hopId)
        if aromaLink.aromaId === aromaId
        if hop.status === "ACTIVE"
      } yield hop

      db.run(query.sortBy(_.name).result).map { rows =>
        rows.map(mapRowToAggregate).toList
      }
    } else {
      // Version sans filtre actif
      val query = for {
        (hop, aromaLink) <- hops join hopAromaProfiles on (_.id === _.hopId)
        if aromaLink.aromaId === aromaId
      } yield hop

      db.run(query.sortBy(_.name).result).map { rows =>
        rows.map(mapRowToAggregate).toList
      }
    }
  }

  def existsByName(name: String): Future[Boolean] = {
    val query = hops.filter(_.name.toLowerCase === name.toLowerCase).exists
    db.run(query.result)
  }

  private def mapRowToAggregate(row: HopRow): HopAggregate = {
    // Cette méthode sera implémentée pour convertir une ligne DB en HopAggregate
    // Pour l'instant, version simplifiée - à étendre selon vos besoins
    val hopName = NonEmptyString.fromString(row.name)
    val alphaAcid = AlphaAcidPercentage.fromString(row.alphaAcid.toString).getOrElse(AlphaAcidPercentage.zero)
    val betaAcid = row.betaAcid.map(ba => AlphaAcidPercentage.fromString(ba.toString).getOrElse(AlphaAcidPercentage.zero))
    val origin = HopOrigin.fromCode(row.originCode).getOrElse(HopOrigin.US)
    val usage = HopUsage.fromName(row.usage).getOrElse(HopUsage.Aroma)
    val description = row.description.map(NonEmptyString.fromString)
    val status = HopStatus.fromName(row.status).getOrElse(HopStatus.Draft)
    val source = HopSource.fromName(row.source).getOrElse(HopSource.Manual)

    HopAggregate(
      id = HopId.fromString(row.id),
      name = hopName,
      alphaAcid = alphaAcid,
      betaAcid = betaAcid,
      origin = origin,
      usage = usage,
      description = description,
      aromaProfile = List.empty, // TODO: Charger les arômes liés
      status = status,
      source = source,
      credibilityScore = row.credibilityScore,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = row.version
    )
  }
}