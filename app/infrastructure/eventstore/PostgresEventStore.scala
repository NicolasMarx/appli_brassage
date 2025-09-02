package infrastructure.eventstore

import javax.inject.{Inject, Singleton}
import slick.jdbc.PostgresProfile.api._
import infrastructure.persistence.slick.tables.YeastEventsTable
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID
import play.api.libs.json._

/**
 * Implémentation PostgreSQL de l'Event Store
 * Compatible avec les tables existantes (yeast_events, recipe_events)
 */
@Singleton
class PostgresEventStore @Inject()(
  database: Database
)(implicit ec: ExecutionContext) extends EventStore {
  
  private val yeastEvents = YeastEventsTable.yeastEvents
  
  override def appendEvent(
    streamId: String,
    expectedVersion: Option[Long],
    event: DomainEvent
  ): Future[Either[EventStoreError, Unit]] = {
    
    val action = for {
      currentVersion <- yeastEvents
        .filter(_.yeastId === streamId)
        .map(_.version)
        .max
        .result
        
      _ <- expectedVersion match {
        case Some(expected) if currentVersion.getOrElse(0L) != expected =>
          DBIO.failed(new RuntimeException(s"Concurrency error: expected $expected, got ${currentVersion.getOrElse(0L)}"))
        case _ => DBIO.successful(())
      }
      
      newVersion = currentVersion.getOrElse(0L) + 1
      
      eventRow = YeastEventRow(
        id = event.eventId.toString,
        yeastId = streamId,
        eventType = event.eventType,
        eventData = event.data,
        version = newVersion.toInt,
        occurredAt = event.occurredAt,
        createdBy = None
      )
      
      _ <- yeastEvents += eventRow
      
    } yield ()
    
    database.run(action.transactionally)
      .map(_ => Right(()))
      .recover {
        case ex: RuntimeException if ex.getMessage.contains("Concurrency error") =>
          Left(ConcurrencyError(expectedVersion, currentVersion.getOrElse(0L)))
        case ex =>
          Left(DatabaseError(ex))
      }
  }
  
  override def readStream(
    streamId: String,
    fromVersion: Option[Long] = None
  ): Future[List[StoredEvent]] = {
    
    val query = yeastEvents
      .filter(_.yeastId === streamId)
      .filter(row => fromVersion.fold(true: Rep[Boolean])(v => row.version >= v.toInt))
      .sortBy(_.version.asc)
    
    database.run(query.result).map(_.toList.map(rowToStoredEvent))
  }
  
  override def readEventsByType(
    eventType: String,
    fromTimestamp: Option[Instant] = None
  ): Future[List[StoredEvent]] = {
    
    val query = yeastEvents
      .filter(_.eventType === eventType)
      .filter(row => fromTimestamp.fold(true: Rep[Boolean])(ts => row.occurredAt >= ts))
      .sortBy(_.occurredAt.asc)
    
    database.run(query.result).map(_.toList.map(rowToStoredEvent))
  }
  
  override def buildSnapshot[T](
    streamId: String,
    folder: (T, DomainEvent) => T,
    initial: T
  ): Future[Option[T]] = {
    
    readStream(streamId).map { events =>
      if (events.isEmpty) {
        None
      } else {
        val domainEvents = events.map(storedEventToDomainEvent)
        Some(domainEvents.foldLeft(initial)(folder))
      }
    }
  }
  
  private def rowToStoredEvent(row: YeastEventRow): StoredEvent = {
    StoredEvent(
      eventId = UUID.fromString(row.id),
      streamId = row.yeastId,
      eventType = row.eventType,
      eventData = row.eventData,
      version = row.version.toLong,
      occurredAt = row.occurredAt,
      createdBy = row.createdBy
    )
  }
  
  private def storedEventToDomainEvent(stored: StoredEvent): DomainEvent = {
    new DomainEvent {
      def eventType: String = stored.eventType
      def eventId: UUID = stored.eventId
      def occurredAt: Instant = stored.occurredAt
      def data: String = stored.eventData
    }
  }
}

/**
 * Row case class pour les événements Yeast 
 * (Compatible avec YeastEventsTable existante)
 */
case class YeastEventRow(
  id: String,
  yeastId: String,
  eventType: String,
  eventData: String,
  version: Int,
  occurredAt: Instant,
  createdBy: Option[String]
)