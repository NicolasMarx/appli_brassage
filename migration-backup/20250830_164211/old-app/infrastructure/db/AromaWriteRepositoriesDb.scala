package infrastructure.db

import repositories.read.AromaReadRepository
import repositories.write.AromaWriteRepository
import domain.aromas._
import infrastructure.db.Tables._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

@Singleton
final class AromaReadRepositoryDb @Inject() (
                                              protected val dbConfigProvider: DatabaseConfigProvider
                                            )(implicit ec: ExecutionContext)
  extends AromaReadRepository
    with HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  private val A = Tables.aromas

  override def all(): Future[Seq[Aroma]] =
    db.run(A.result).map { rows =>
      rows
        .map(r => Aroma(AromaId(r.id), AromaName(r.name)))
        .sortBy(_.name.value.toLowerCase)
    }
}

@Singleton
final class AromaWriteRepositoryDb @Inject() (
                                               protected val dbConfigProvider: DatabaseConfigProvider
                                             )(implicit ec: ExecutionContext)
  extends AromaWriteRepository
    with HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  private val A = Tables.aromas

  override def replaceAll(aromas: Seq[Aroma]): Future[Unit] = {
    val rows = aromas.map(a => Tables.AromaRow(a.id.value, a.name.value))

    val tx: DBIO[Unit] = (for {
      _ <- A.delete
      _ <- A ++= rows
    } yield ()).transactionally

    db.run(tx).map(_ => ())
  }
}
