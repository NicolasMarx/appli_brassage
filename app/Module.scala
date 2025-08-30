package modules

import com.google.inject.AbstractModule

// Service d’import Admin (package correct avec A majuscule)
import application.Admin.{HopsCsvImportService, HopsCsvImportServiceImpl}

/**
 * Module Guice racine.
 * - Installe les bindings "métiers" via BindingsModule
 * - Ajoute le binding du service d'import Admin (si tu préfères, tu peux aussi le laisser dans BindingsModule)
 */
final class Module extends AbstractModule {
  override def configure(): Unit = {
    // Délègue à notre module dédié
    install(new BindingsModule)

    // Binding explicite du service d'import Admin
    bind(classOf[HopsCsvImportService]).to(classOf[HopsCsvImportServiceImpl]).asEagerSingleton()
  }
}
