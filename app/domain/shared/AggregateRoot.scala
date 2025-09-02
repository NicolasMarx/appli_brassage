package domain.shared

import scala.collection.mutable

/**
 * Trait de base pour tous les agrégats DDD
 * Support Event Sourcing et gestion des événements
 */
trait AggregateRoot[T <: DomainEvent] {
  private val _uncommittedEvents = mutable.ListBuffer[T]()
  
  def getUncommittedEvents: List[T] = _uncommittedEvents.toList
  
  def markEventsAsCommitted: this.type = {
    _uncommittedEvents.clear()
    this
  }
  
  protected def raise(event: T): Unit = {
    _uncommittedEvents += event
  }
}
