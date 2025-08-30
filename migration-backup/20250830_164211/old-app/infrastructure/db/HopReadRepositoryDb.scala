package infrastructure.db

private[db] final case class HopDetailRow(
                                           hopId: String,
                                           description: Option[String],
                                           hopType: Option[String]
                                         )

private[db] final case class HopSubstituteRow(
                                               hopId: String,
                                               name: String
                                             )

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import domain.common.Range
import domain.hops._
import repositories.read.HopReadRepository

/** Read side (CQRS) : agrège Hop avec ses arômes (noms) via hop_aromas → aromas. */
@Singleton
final class HopReadRepositoryDb @Inject()(
                                           protected val dbConfigProvider: DatabaseConfigProvider
                                         )(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[PostgresProfile]
    with HopReadRepository {

  import Tables._

  private val H  = Tables.hops
  private val HA = Tables.hopAromas
  private val A  = Tables.aromas

  private def toRange(min: Option[BigDecimal], max: Option[BigDecimal]): Range =
    Range(min, max)

  private def rowToHop(r: HopRow, aromas: List[String]): Hop =
    Hop(
      id          = HopId(r.id),
      name        = HopName(r.name),
      country     = Country(r.country),
      alphaAcid   = toRange(r.alphaMin, r.alphaMax),
      betaAcid    = toRange(r.betaMin,  r.betaMax),
      cohumulone  = toRange(r.cohumMin, r.cohumMax),
      totalOilsMlPer100g = toRange(r.oilsMin, r.oilsMax),
      myrcene     = toRange(r.myrceneMin, r.myrceneMax),
      humulene    = toRange(r.humuleneMin, r.humuleneMax),
      caryophyllene = toRange(r.caryoMin, r.caryoMax),
      farnesene   = toRange(r.farneseneMin, r.farneseneMax),
      aromas      = aromas
    )

  override def all(): Future[Seq[Hop]] = {
    // 1) H LEFT JOIN HA (Option[HA])
    // 2) (H, Option[HA]) LEFT JOIN A, en reliant haOpt.map(_.aromaId) à a.id
    val q =
      H
        .joinLeft(HA).on(_.id === _.hopId)
        .joinLeft(A).on { case ((_, haOpt), a) => haOpt.map(_.aromaId) === a.id }
        .map { case ((h, _), aOpt) => (h, aOpt.map(_.name)) }

    val action = q.result.map { rows =>
      rows
        .groupBy(_._1)
        .toSeq
        .map { case (hrow, group) =>
          val aromaNames = group.flatMap(_._2).distinct.toList
          rowToHop(hrow, aromaNames)
        }
        .sortBy(_.name.value.toLowerCase)
    }

    db.run(action)
  }
}
