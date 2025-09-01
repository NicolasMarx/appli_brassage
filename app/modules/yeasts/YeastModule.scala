package modules.yeasts

import com.google.inject.AbstractModule
import domain.yeasts.repositories.{YeastRepository, YeastReadRepository, YeastWriteRepository}
import infrastructure.persistence.slick.repositories.yeasts.{SlickYeastRepository, SlickYeastReadRepository, SlickYeastWriteRepository}

/**
 * Module Guice pour l'injection de dépendances du domaine Yeast
 */
class YeastModule extends AbstractModule {
  
  override def configure(): Unit = {
    // Repositories
    bind(classOf[YeastReadRepository]).to(classOf[SlickYeastReadRepository])
    bind(classOf[YeastWriteRepository]).to(classOf[SlickYeastWriteRepository])
    bind(classOf[YeastRepository]).to(classOf[SlickYeastRepository])
    
    // Services sont automatiquement injectés via @Singleton
    // Handlers sont automatiquement injectés via @Singleton
  }
}
