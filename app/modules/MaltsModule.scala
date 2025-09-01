package modules

import com.google.inject.AbstractModule

/**
 * Module dédié exclusivement au domaine Malts
 * N'interfère pas avec les autres modules existants
 */
class MaltsModule extends AbstractModule {
  override def configure(): Unit = {
    // Bindings domaine Malts uniquement
    bind(classOf[domain.malts.repositories.MaltReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository])
      .asEagerSingleton()
    
    bind(classOf[domain.malts.repositories.MaltWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository])
      .asEagerSingleton()
  }
}
