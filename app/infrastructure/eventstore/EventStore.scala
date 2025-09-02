package infrastructure.eventstore

import scala.concurrent.Future
import java.time.Instant
import java.util.UUID

/**
 * Core Event Store abstraction inspired by pg-event-store
 * Architecture compatible with PostgreSQL et extensible
 */
trait EventStore {
  
  /**
   * Persiste un événement dans l'event store
   */
  def appendEvent(
    streamId: String,
    expectedVersion: Option[Long],
    event: DomainEvent
  ): Future[Either[EventStoreError, Unit]]
  
  /**
   * Récupère tous les événements d'un stream
   */
  def readStream(
    streamId: String,
    fromVersion: Option[Long] = None
  ): Future[List[StoredEvent]]
  
  /**
   * Récupère tous les événements par type
   */
  def readEventsByType(
    eventType: String,
    fromTimestamp: Option[Instant] = None
  ): Future[List[StoredEvent]]
  
  /**
   * Construit un snapshot à partir des événements
   */
  def buildSnapshot[T](
    streamId: String,
    folder: (T, DomainEvent) => T,
    initial: T
  ): Future[Option[T]]
}

/**
 * Événement domaine générique
 */
trait DomainEvent {
  def eventType: String
  def eventId: UUID
  def occurredAt: Instant
  def data: String
}

/**
 * Événement stocké avec métadonnées
 */
case class StoredEvent(
  eventId: UUID,
  streamId: String,
  eventType: String,
  eventData: String,
  version: Long,
  occurredAt: Instant,
  createdBy: Option[String]
)

/**
 * Erreurs Event Store
 */
sealed trait EventStoreError
case class ConcurrencyError(expectedVersion: Option[Long], actualVersion: Long) extends EventStoreError
case class StreamNotFound(streamId: String) extends EventStoreError
case class SerializationError(message: String) extends EventStoreError
case class DatabaseError(cause: Throwable) extends EventStoreError