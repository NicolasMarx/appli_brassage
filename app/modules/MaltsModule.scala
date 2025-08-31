package modules

import com.google.inject.AbstractModule
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import infrastructure.persistence.slick.repositories.malts.{SlickMaltReadRepository, SlickMaltWriteRepository}

/**
 * Module Guice pour le domaine Malts
 * Connecte les interfaces aux implémentations Slick
 */
final class MaltsModule extends AbstractModule {
  override def configure(): Unit = {

    // ===== MALT REPOSITORIES =====
    bind(classOf[MaltReadRepository]).to(classOf[SlickMaltReadRepository])
    bind(classOf[MaltWriteRepository]).to(classOf[SlickMaltWriteRepository])

    println("MaltsModule chargé - Repositories Malts connectés à PostgreSQL")
  }
}