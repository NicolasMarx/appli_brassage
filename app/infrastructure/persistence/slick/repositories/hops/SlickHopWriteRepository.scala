// app/infrastructure/persistence/slick/repositories/hops/SlickHopWriteRepository.scala
package infrastructure.persistence.slick.repositories.hops

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

import domain.hops.model._
import domain.hops.repositories.HopWriteRepository
import infrastructure.persistence.slick.tables.HopTables

@Singleton
class SlickHopWriteRepository @Inject()(
                                         protected val dbConfigProvider: DatabaseConfigProvider
                                       )(implicit ec: ExecutionContext)
  extends HopWriteRepository
    with HasDatabaseConfigProvider[JdbcProfile]
    with HopTables {

  import profile.api._

  def save(hop: HopAggregate): Future[HopAggregate] = {
    val hopRow = mapAggregateToRow(hop)

    val action = hops.insertOrUpdate(hopRow).map { _ =>
      hop.markEventsAsCommitted()
      hop
    }

    db.run(action.transactionally)
  }

  def delete(id: HopId): Future[Boolean] = {
    val action = hops.filter(_.id === id.value).delete

    db.run(action.transactionally).map(_ > 0)
  }

  def saveAll(hopsList: List[HopAggregate]): Future[List[HopAggregate]] = {
    val hopRows = hopsList.map(mapAggregateToRow)

    val actions = DBIO.sequence(hopRows.map { row =>
      hops.insertOrUpdate(row)
    })

    db.run(actions.transactionally).map { _ =>
      hopsList.foreach(_.markEventsAsCommitted())
      hopsList
    }
  }

  private def mapAggregateToRow(hop: HopAggregate): HopRow = {
    HopRow(
      id = hop.id.value,
      name = hop.name.value,
      alphaAcid = hop.alphaAcid.value,
      betaAcid = hop.betaAcid.map(_.value),
      originCode = hop.origin.code,
      usage = hop.usage.name,
      description = hop.description.map(_.value),
      status = hop.status.name,
      source = hop.source.name,
      credibilityScore = hop.credibilityScore,
      createdAt = hop.createdAt,
      updatedAt = hop.updatedAt,
      version = hop.version
    )
  }
}