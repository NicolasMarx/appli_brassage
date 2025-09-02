package infrastructure.eventstore

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

/**
 * Module Guice pour configuration Event Store
 * Alternative à pg-event-store avec implémentation PostgreSQL native
 */
class EventStoreModule(
  environment: Environment,
  configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[EventStore]).to(classOf[PostgresEventStore])
  }
}