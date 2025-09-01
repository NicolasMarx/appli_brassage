package modules

import com.google.inject.AbstractModule
import domain.hops.repositories.{HopReadRepository, HopWriteRepository}
import infrastructure.persistence.slick.repositories.hops.{SlickHopReadRepository, SlickHopWriteRepository}

// Import repositories malts

/**
 * Bindings Guice - Connecte les interfaces du domaine aux implémentations Slick
 */
final class BindingsModule extends AbstractModule {
  override def configure(): Unit = {

    // ===== HOP REPOSITORIES =====
    bind(classOf[HopReadRepository]).to(classOf[SlickHopReadRepository])
    bind(classOf[HopWriteRepository]).to(classOf[SlickHopWriteRepository])


    println("BindingsModule chargé - Repositories Hops et Malts connectés à PostgreSQL")
  }
}
