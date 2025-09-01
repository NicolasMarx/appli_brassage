package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.MaltWriteRepository
import infrastructure.persistence.slick.tables.MaltTables
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltWriteRepository @Inject()(
  val profile: PostgresProfile,
  db: PostgresProfile#Backend#Database
)(implicit ec: ExecutionContext) extends MaltWriteRepository with MaltTables {

  import profile.api._

  override def save(malt: MaltAggregate): Future[MaltAggregate] = {
    val row = aggregateToRow(malt)
    val upsertQuery = malts.insertOrUpdate(row)
    
    db.run(upsertQuery).map(_ => malt)
  }

  override def delete(id: MaltId): Future[Boolean] = {
    // MaltId.value est String (UUID au format String)
    db.run(malts.filter(_.id === id.value).delete).map(_ > 0)
  }
}
