package infrastructure.db

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import repositories.read.BeerStyleReadRepository
import repositories.write.BeerStyleWriteRepository
import domain.beerstyles._
import infrastructure.db.Tables._

@Singleton
final class BeerStyleReadRepositoryDb @Inject()(
                                                 protected val dbConfigProvider: DatabaseConfigProvider
                                               )(implicit ec: ExecutionContext)
  extends BeerStyleReadRepository
    with HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  private val BS = Tables.beerStyles

  override def all(): Future[Seq[BeerStyle]] =
    db.run(BS.result).map { rows =>
      rows
        .map(r => BeerStyle(BeerStyleId(r.id), BeerStyleName(r.name)))
        .sortBy(_.name.value.toLowerCase)
    }
}

@Singleton
final class BeerStyleWriteRepositoryDb @Inject()(
                                                  protected val dbConfigProvider: DatabaseConfigProvider
                                                )(implicit ec: ExecutionContext)
  extends BeerStyleWriteRepository
    with HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  private val BS = Tables.beerStyles

  /** Exécute une action d’écriture dans une transaction */
  private def runWrite[T](action: DBIO[T]): Future[T] =
    db.run(action.transactionally)

  override def replaceAll(styles: Seq[BeerStyle]): Future[Unit] = {
    val rows = styles.map(s => Tables.BeerStyleRow(s.id.value, s.name.value))

    val action: DBIO[Unit] = for {
      _ <- BS.delete
      _ <- BS ++= rows
    } yield ()

    runWrite(action).map(_ => ())
  }
}
