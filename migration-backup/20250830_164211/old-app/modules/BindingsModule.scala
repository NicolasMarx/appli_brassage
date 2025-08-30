package modules

import com.google.inject.AbstractModule

// ---- Contrats DDD (Read / Write)
import repositories.read._
import repositories.write._

// ---- Implémentations Slick/DB (selon ton arborescence actuelle)
import infrastructure.db._  // contient AromaReadRepositoryDb, AromaWriteRepositoryDb, etc.

// ---- Services d'application & utilitaires
import application.admin.{AdminBootstrapService, AdminBootstrapServiceImpl}
import services.PasswordService

/**
 * Bindings Guice "prod-ready" — CQRS/DDD
 * Mappe chaque interface de repositories vers son implémentation DB
 * + enregistre les services transverses (PasswordService, AdminBootstrapService).
 */
final class BindingsModule extends AbstractModule {
  override def configure(): Unit = {

    // ===== READ side =====
    bind(classOf[HopReadRepository])        .to(classOf[HopReadRepositoryDb])
    bind(classOf[AromaReadRepository])      .to(classOf[AromaReadRepositoryDb])
    bind(classOf[BeerStyleReadRepository])  .to(classOf[BeerStyleReadRepositoryDb])
    bind(classOf[HopExtrasReadRepository])  .to(classOf[HopExtrasReadRepositoryDb])
    bind(classOf[AdminReadRepository])      .to(classOf[AdminReadRepositoryDb])

    // ===== WRITE side =====
    bind(classOf[HopWriteRepository])       .to(classOf[HopWriteRepositoryDb])
    bind(classOf[AromaWriteRepository])     .to(classOf[AromaWriteRepositoryDb])
    bind(classOf[BeerStyleWriteRepository]) .to(classOf[BeerStyleWriteRepositoryDb])
    bind(classOf[AdminWriteRepository])     .to(classOf[AdminWriteRepositoryDb])

    // ===== Services transverses / Application =====
    bind(classOf[PasswordService]).asEagerSingleton()
    bind(classOf[AdminBootstrapService]).to(classOf[AdminBootstrapServiceImpl])
  }
}
