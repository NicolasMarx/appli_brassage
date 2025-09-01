package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import domain.yeasts.repositories.YeastWriteRepository
import infrastructure.persistence.slick.tables.{YeastsTable, YeastEventsTable, YeastRow, YeastEventRow}
import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

/**
 * Implémentation Slick du repository d'écriture des levures
 * Gestion Event Sourcing et modifications transactionnelles
 */
@Singleton
class SlickYeastWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends YeastWriteRepository with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  private val yeasts = YeastsTable.yeasts
  private val yeastEvents = YeastEventsTable.yeastEvents

  override def create(yeast: YeastAggregate): Future[YeastAggregate] = {
    val row = YeastRow.fromAggregate(yeast)
    val eventRows = yeast.getUncommittedEvents.map(YeastEventRow.fromEvent)
    
    val action = (for {
      _ <- yeasts += row
      _ <- yeastEvents ++= eventRows
    } yield yeast).transactionally
    
    db.run(action).map(_.markEventsAsCommitted)
  }

  override def update(yeast: YeastAggregate): Future[YeastAggregate] = {
    val row = YeastRow.fromAggregate(yeast)
    val eventRows = yeast.getUncommittedEvents.map(YeastEventRow.fromEvent)
    
    val action = (for {
      updateCount <- yeasts.filter(_.id === yeast.id.value).update(row)
      _ <- if (updateCount == 0) DBIO.failed(new RuntimeException(s"Yeast not found: ${yeast.id}")) else DBIO.successful(())
      _ <- if (eventRows.nonEmpty) yeastEvents ++= eventRows else DBIO.successful(())
    } yield yeast).transactionally
    
    db.run(action).map(_.markEventsAsCommitted)
  }

  override def archive(yeastId: YeastId, reason: Option[String]): Future[Unit] = {
    val updateAction = yeasts
      .filter(_.id === yeastId.value)
      .map(y => (y.status, y.updatedAt, y.version))
      .update((YeastStatus.Archived.name, Instant.now(), 0L)) // Version will be updated by aggregate
    
    db.run(updateAction).map(_ => ())
  }

  override def activate(yeastId: YeastId): Future[Unit] = {
    val updateAction = yeasts
      .filter(_.id === yeastId.value)
      .map(y => (y.status, y.updatedAt, y.version))
      .update((YeastStatus.Active.name, Instant.now(), 0L))
    
    db.run(updateAction).map(_ => ())
  }

  override def deactivate(yeastId: YeastId, reason: Option[String]): Future[Unit] = {
    val updateAction = yeasts
      .filter(_.id === yeastId.value)
      .map(y => (y.status, y.updatedAt, y.version))
      .update((YeastStatus.Inactive.name, Instant.now(), 0L))
    
    db.run(updateAction).map(_ => ())
  }

  override def changeStatus(
    yeastId: YeastId, 
    newStatus: YeastStatus, 
    reason: Option[String] = None
  ): Future[Unit] = {
    val updateAction = yeasts
      .filter(_.id === yeastId.value)
      .map(y => (y.status, y.updatedAt, y.version))
      .update((newStatus.name, Instant.now(), 0L))
    
    db.run(updateAction).map(_ => ())
  }

  override def saveEvents(yeastId: YeastId, events: List[YeastEvent], expectedVersion: Long): Future[Unit] = {
    if (events.isEmpty) return Future.successful(())
    
    // Vérifier la version pour éviter les conflits de concurrence
    val versionCheck = yeasts
      .filter(_.id === yeastId.value)
      .map(_.version)
      .result
      .headOption
    
    val eventRows = events.map(YeastEventRow.fromEvent)
    
    val action = (for {
      currentVersion <- versionCheck
      _ <- currentVersion match {
        case Some(version) if version == expectedVersion => 
          yeastEvents ++= eventRows
        case Some(version) => 
          DBIO.failed(new RuntimeException(s"Conflict de version: attendu $expectedVersion, trouvé $version"))
        case None => 
          DBIO.failed(new RuntimeException(s"Yeast non trouvé: $yeastId"))
      }
    } yield ()).transactionally
    
    db.run(action)
  }

  override def getEvents(yeastId: YeastId): Future[List[YeastEvent]] = {
    db.run(
      yeastEvents
        .filter(_.yeastId === yeastId.value)
        .sortBy(_.version)
        .result
    ).map(_.toList.flatMap(row => YeastEventRow.toEvent(row).toOption))
  }

  override def getEventsSinceVersion(yeastId: YeastId, sinceVersion: Long): Future[List[YeastEvent]] = {
    db.run(
      yeastEvents
        .filter(e => e.yeastId === yeastId.value && e.version > sinceVersion)
        .sortBy(_.version)
        .result
    ).map(_.toList.flatMap(row => YeastEventRow.toEvent(row).toOption))
  }

  override def createBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]] = {
    if (yeasts.isEmpty) return Future.successful(List.empty)
    
    val rows = yeasts.map(YeastRow.fromAggregate)
    val allEventRows = yeasts.flatMap(_.getUncommittedEvents.map(YeastEventRow.fromEvent))
    
    val action = (for {
      _ <- this.yeasts ++= rows
      _ <- if (allEventRows.nonEmpty) yeastEvents ++= allEventRows else DBIO.successful(())
    } yield yeasts.map(_.markEventsAsCommitted)).transactionally
    
    db.run(action)
  }

  override def updateBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]] = {
    if (yeasts.isEmpty) return Future.successful(List.empty)
    
    val actions = yeasts.map { yeast =>
      val row = YeastRow.fromAggregate(yeast)
      val eventRows = yeast.getUncommittedEvents.map(YeastEventRow.fromEvent)
      
      for {
        updateCount <- this.yeasts.filter(_.id === yeast.id.value).update(row)
        _ <- if (updateCount == 0) DBIO.failed(new RuntimeException(s"Yeast not found: ${yeast.id}")) else DBIO.successful(())
        _ <- if (eventRows.nonEmpty) yeastEvents ++= eventRows else DBIO.successful(())
      } yield yeast.markEventsAsCommitted
    }
    
    db.run(DBIO.sequence(actions).transactionally)
  }
}
