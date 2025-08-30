package modules

import com.google.inject.AbstractModule
import domain.hops.repositories.{HopReadRepository, HopWriteRepository}
import infrastructure.persistence.slick.repositories.hops.{SlickHopReadRepository, SlickHopWriteRepository}

/**
 * Bindings Guice - Connecte les interfaces du domaine aux implémentations Slick
 */
final class BindingsModule extends AbstractModule {
  override def configure(): Unit = {

    // ===== HOP REPOSITORIES =====
    bind(classOf[HopReadRepository]).to(classOf[SlickHopReadRepository])
    bind(classOf[HopWriteRepository]).to(classOf[SlickHopWriteRepository])

    // TODO: Ajouter les autres repositories quand ils seront prêts
    // bind(classOf[AdminReadRepository]).to(classOf[SlickAdminReadRepository])
    // bind(classOf[AdminWriteRepository]).to(classOf[SlickAdminWriteRepository])

    println("BindingsModule chargé - Repositories connectés à PostgreSQL")
  }
}