package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import domain.yeasts.repositories.YeastWriteRepository
import infrastructure.persistence.slick.tables.{YeastEventsTable, YeastEventRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

/**
 * Implémentation Slick du repository d'écriture Yeast
 * CORRECTION: Correction des méthodes manquantes et erreurs de compilation
 */
@Singleton
class SlickYeastWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
  extends YeastWriteRepository 
    with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._

  // CORRECTION: Utilisation correcte de YeastEventsTable
  private val yeastEvents = YeastEventsTable.yeastEvents

  override def create(yeast: YeastAggregate): Future[YeastAggregate] = {
    // CORRECTION: Gestion correcte des événements uncommitted
    val eventRows = yeast.getUncommittedEvents.map(YeastEventRow.fromEvent)
    
    if (eventRows.nonEmpty) {
      val action = yeastEvents ++= eventRows
      db.run(action).map { _ =>
        // CORRECTION: markEventsAsCommitted disponible via EventSourced
        yeast.markEventsAsCommitted()
        yeast
      }
    } else {
      Future.successful(yeast)
    }
  }

  override def update(yeast: YeastAggregate): Future[YeastAggregate] = {
    // CORRECTION: Même logique que create pour Event Sourcing
    val eventRows = yeast.getUncommittedEvents.map(YeastEventRow.fromEvent)
    
    if (eventRows.nonEmpty) {
      val action = yeastEvents ++= eventRows
      db.run(action).map { _ =>
        yeast.markEventsAsCommitted()
        yeast
      }
    } else {
      Future.successful(yeast)
    }
  }

  override def archive(yeastId: YeastId, reason: Option[String]): Future[Unit] = {
    // TODO: Implémentation avec Event Sourcing
    Future.successful(())
  }

  override def activate(yeastId: YeastId): Future[Unit] = {
    // TODO: Implémentation avec Event Sourcing
    Future.successful(())
  }

  override def deactivate(yeastId: YeastId, reason: Option[String]): Future[Unit] = {
    // TODO: Implémentation avec Event Sourcing
    Future.successful(())
  }

  override def changeStatus(
    yeastId: YeastId, 
    newStatus: YeastStatus, 
    reason: Option[String] = None
  ): Future[Unit] = {
    // TODO: Implémentation avec Event Sourcing
    Future.successful(())
  }

  override def saveEvents(yeastId: YeastId, events: List[YeastEvent], expectedVersion: Long): Future[Unit] = {
    if (events.nonEmpty) {
      val eventRows = events.map(YeastEventRow.fromEvent)
      val action = yeastEvents ++= eventRows
      db.run(action).map(_ => ())
    } else {
      Future.successful(())
    }
  }

  override def getEvents(yeastId: YeastId): Future[List[YeastEvent]] = {
    val query = yeastEvents.filter(_.yeastId === yeastId.asString)
    db.run(query.result).map(_.map(YeastEventRow.toEvent).toList)
  }

  override def getEventsSinceVersion(yeastId: YeastId, sinceVersion: Long): Future[List[YeastEvent]] = {
    val query = yeastEvents.filter(row => 
      row.yeastId === yeastId.asString && row.version > sinceVersion.toInt
    )
    db.run(query.result).map(_.map(YeastEventRow.toEvent).toList)
  }

  override def createBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]] = {
    if (yeasts.isEmpty) {
      Future.successful(List.empty)
    } else {
      val allEventRows = yeasts.flatMap(_.getUncommittedEvents.map(YeastEventRow.fromEvent))
      
      if (allEventRows.nonEmpty) {
        val action = yeastEvents ++= allEventRows
        db.run(action).map { _ =>
          yeasts.foreach(_.markEventsAsCommitted())
          yeasts
        }
      } else {
        Future.successful(yeasts)
      }
    }
  }

  override def updateBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]] = {
    createBatch(yeasts) // Même logique pour Event Sourcing
  }
}
