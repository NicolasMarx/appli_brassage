package infrastructure.db

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import repositories.write.HopWriteRepository
import domain.hops._
import infrastructure.db.Tables._

/**
 * Companion interne : modèles de lignes pour les tables locales non générées
 * (hop_details, hop_substitutes). Visibilité limitée au package infrastructure.db.
 */
private[infrastructure] object HopWriteRepositoryDb {
  private[db] final case class HopDetailRow(
                                             hopId: String,
                                             description: Option[String],
                                             hopType: Option[String]
                                           )
  private[db] final case class HopSubstituteRow(
                                                 hopId: String,
                                                 name: String
                                               )
}

@Singleton
final class HopWriteRepositoryDb @Inject()(
                                            protected val dbConfigProvider: DatabaseConfigProvider
                                          )(implicit ec: ExecutionContext)
  extends HopWriteRepository
    with HasDatabaseConfigProvider[PostgresProfile] {

  import HopWriteRepositoryDb._
  import profile.api._

  // --- Tables exposées par infrastructure.db.Tables (générées)
  private val H  = Tables.hops
  private val HA = Tables.hopAromas
  private val HB = Tables.hopBeerStyles

  // --- Tables locales (déclarées ici car non exposées par Tables.scala)
  private class HopDetails(tag: Tag) extends Table[HopDetailRow](tag, "hop_details") {
    def hopId       = column[String]("hop_id", O.PrimaryKey)
    def description = column[Option[String]]("description")
    def hopType     = column[Option[String]]("hop_type")
    def *           = (hopId, description, hopType) <> (HopDetailRow.tupled, HopDetailRow.unapply)
  }
  private val hopDetails = TableQuery[HopDetails]

  private class HopSubstitutes(tag: Tag) extends Table[HopSubstituteRow](tag, "hop_substitutes") {
    def hopId = column[String]("hop_id")
    def name  = column[String]("name")
    def pk    = primaryKey("pk_hop_substitutes", (hopId, name))
    def *     = (hopId, name) <> (HopSubstituteRow.tupled, HopSubstituteRow.unapply)
  }
  private val hopSubstitutes = TableQuery[HopSubstitutes]

  // --- Helpers

  /** Exécute une action d'écriture dans une transaction. */
  private def runWrite[T](action: DBIO[T]): Future[T] =
    db.run(action.transactionally)

  // --- Conversions domaine -> rows (Tables.*Row)

  private def toHopRow(h: Hop): HopRow =
    HopRow(
      id           = h.id.value,
      name         = h.name.value,
      country      = h.country.value,
      alphaMin     = h.alphaAcid.min,
      alphaMax     = h.alphaAcid.max,
      betaMin      = h.betaAcid.min,
      betaMax      = h.betaAcid.max,
      cohumMin     = h.cohumulone.min,
      cohumMax     = h.cohumulone.max,
      oilsMin      = h.totalOilsMlPer100g.min,
      oilsMax      = h.totalOilsMlPer100g.max,
      myrceneMin   = h.myrcene.min,
      myrceneMax   = h.myrcene.max,
      humuleneMin  = h.humulene.min,
      humuleneMax  = h.humulene.max,
      caryoMin     = h.caryophyllene.min,
      caryoMax     = h.caryophyllene.max,
      farneseneMin = h.farnesene.min,
      farneseneMax = h.farnesene.max
    )

  private def toHopDetailRow(d: HopDetail): HopDetailRow =
    HopDetailRow(
      hopId       = d.hopId.value,
      description = d.description,
      hopType     = d.hopType
    )

  private def toHopAromaRow(p: (HopId, String)): HopAromaRow =
    HopAromaRow(hopId = p._1.value, aromaId = p._2)

  private def toHopBeerStyleRow(p: (HopId, String)): HopBeerStyleRow =
    HopBeerStyleRow(hopId = p._1.value, styleId = p._2)

  private def toHopSubRow(p: (HopId, String)): HopSubstituteRow =
    HopSubstituteRow(hopId = p._1.value, name = p._2)

  // -------------------- Commandes CQRS --------------------

  /** Remplace l’ensemble des houblons (truncate + bulk insert). */
  override def replaceAll(hopsDomain: Seq[Hop]): Future[Unit] = {
    val rows = hopsDomain.map(toHopRow)
    val action: DBIO[Unit] = for {
      _ <- H.delete
      _ <- H ++= rows
    } yield ()
    runWrite(action).map(_ => ())
  }

  /** Remplace hop_details (truncate + bulk insert). */
  override def replaceDetails(details: Seq[HopDetail]): Future[Unit] = {
    val rows = details.distinct.map(toHopDetailRow)
    val action: DBIO[Unit] = for {
      _ <- hopDetails.delete
      _ <- hopDetails ++= rows
    } yield ()
    runWrite(action).map(_ => ())
  }

  /** Remplace liaisons Hop ↔ Aroma (IDs d’arômes). */
  override def replaceAromas(pairs: Seq[(HopId, String)]): Future[Unit] = {
    val rows = pairs.distinct.map(toHopAromaRow)
    val action: DBIO[Unit] = for {
      _ <- HA.delete
      _ <- HA ++= rows
    } yield ()
    runWrite(action).map(_ => ())
  }

  /** Remplace liaisons Hop ↔ BeerStyle (IDs de styles). */
  override def replaceBeerStyles(pairs: Seq[(HopId, String)]): Future[Unit] = {
    val rows = pairs.distinct.map(toHopBeerStyleRow)
    val action: DBIO[Unit] = for {
      _ <- HB.delete
      _ <- HB ++= rows
    } yield ()
    runWrite(action).map(_ => ())
  }

  /** Remplace hop_substitutes (noms simples côté write). */
  override def replaceSubstitutes(pairs: Seq[(HopId, String)]): Future[Unit] = {
    val rows = pairs.distinct.map(toHopSubRow)
    val action: DBIO[Unit] = for {
      _ <- hopSubstitutes.delete
      _ <- hopSubstitutes ++= rows
    } yield ()
    runWrite(action).map(_ => ())
  }
}
