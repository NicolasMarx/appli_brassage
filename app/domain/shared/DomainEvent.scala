package domain.shared

import java.time.Instant

/**
 * Trait de base pour tous les événements de domaine
 * Support Event Sourcing
 */
trait DomainEvent {
  def occurredAt: Instant
  def version: Long
}
