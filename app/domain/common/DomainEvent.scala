// app/domain/common/DomainEvent.scala
package domain.common

import java.time.Instant
import java.util.UUID

/**
 * Trait de base pour tous les événements de domaine
 * Respecte les principes Event Sourcing et CQRS
 */
trait DomainEvent {
  val eventId: String
  val occurredAt: Instant
  val aggregateId: String
  val eventType: String
  val version: Int
}

/**
 * Implémentation de base pour les événements de domaine
 */
abstract class BaseDomainEvent(
  val aggregateId: String,
  val eventType: String,
  val version: Int = 1
) extends DomainEvent {
  
  override val eventId: String = UUID.randomUUID().toString
  override val occurredAt: Instant = Instant.now()
}

/**
 * Collecteur d'événements pour les agrégats
 */
trait EventSourced {
  private var _uncommittedEvents: List[DomainEvent] = List.empty
  private var _version: Int = 0
  
  protected def raise(event: DomainEvent): Unit = {
    _uncommittedEvents = _uncommittedEvents :+ event
    _version += 1
  }
  
  def uncommittedEvents: List[DomainEvent] = _uncommittedEvents
  def version: Int = _version
  
  def markEventsAsCommitted(): Unit = {
    _uncommittedEvents = List.empty
  }
}

/**
 * Gestionnaire d'événements de domaine
 */
trait DomainEventHandler[T <: DomainEvent] {
  def handle(event: T): Unit
  def canHandle(event: DomainEvent): Boolean
}