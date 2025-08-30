package infrastructure.db

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

import domain.hops._
import repositories.read.{HopExtrasReadRepository, HopExtras}

@Singleton
class HopExtrasReadRepositoryDb @Inject()(
                                           protected val dbConfigProvider: DatabaseConfigProvider
                                         )(implicit ec: ExecutionContext) extends HopExtrasReadRepository {

  protected val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig.profile.api._

  // --- Tables lecture minimales (read-side only) ---
  final class HopDetailsTable(tag: Tag) extends Table[(String, Option[String], Option[String])](tag, "hop_details") {
    def hopId       = column[String]("hop_id", O.PrimaryKey)
    def description = column[Option[String]]("description")
    def hopType     = column[Option[String]]("hop_type")
    def * = (hopId, description, hopType)
  }

  final class BeerStylesTable(tag: Tag) extends Table[(String, String)](tag, "beer_styles") {
    def id   = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def * = (id, name)
  }

  final class HopBeerStylesTable(tag: Tag) extends Table[(String, String)](tag, "hop_beer_styles") {
    def hopId   = column[String]("hop_id")
    def styleId = column[String]("style_id")
    def * = (hopId, styleId)
  }

  final class HopSubstitutesTable(tag: Tag) extends Table[(String, String)](tag, "hop_substitutes") {
    def hopId = column[String]("hop_id")
    def name  = column[String]("name")
    def * = (hopId, name)
  }

  private val HopDetails     = TableQuery[HopDetailsTable]
  private val HopBeerStyles  = TableQuery[HopBeerStylesTable]
  private val BeerStyles     = TableQuery[BeerStylesTable]
  private val HopSubstitutes = TableQuery[HopSubstitutesTable]

  override def findByHopIds(ids: Seq[HopId]): Future[Map[HopId, HopExtras]] = {
    val idStrings = ids.map(_.value)

    val detailsQ =
      HopDetails
        .filter(_.hopId.inSet(idStrings))
        .result

    // Jointure explicite: hop_beer_styles.style_id -> beer_styles.id
    val stylesQ =
      HopBeerStyles
        .filter(_.hopId.inSet(idStrings))
        .join(BeerStyles).on { case (hbs, bs) => hbs.styleId === bs.id }
        .map { case (hbs, bs) => (hbs.hopId, bs.name) }
        .result

    val subsQ =
      HopSubstitutes
        .filter(_.hopId.inSet(idStrings))
        .result

    val action: DBIO[Map[HopId, HopExtras]] =
      for {
        d <- detailsQ
        s <- stylesQ
        u <- subsQ
      } yield {
        val dMap: Map[String, (Option[String], Option[String])] =
          d.map { case (id, desc, typ) => id -> (desc, typ) }.toMap

        val sMap: Map[String, Seq[String]] =
          s.groupBy(_._1).view.mapValues(_.map(_._2)).toMap

        val uMap: Map[String, Seq[String]] =
          u.groupBy(_._1).view.mapValues(_.map(_._2)).toMap

        idStrings.map { id =>
          val (desc, typ) = dMap.getOrElse(id, (None, None))
          HopId(id) -> HopExtras(
            description = desc,
            hopType     = typ,
            beerStyles  = sMap.getOrElse(id, Nil).toList,
            substitutes = uMap.getOrElse(id, Nil).toList
          )
        }.toMap
      }

    dbConfig.db.run(action)
  }
}
