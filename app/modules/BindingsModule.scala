package modules

import com.google.inject.AbstractModule

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    // Bindings domaine Hops (existants)
    bind(classOf[domain.hops.repositories.HopReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.hops.SlickHopReadRepository])
      .asEagerSingleton()
    bind(classOf[domain.hops.repositories.HopWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.hops.SlickHopWriteRepository])
      .asEagerSingleton()


    // Bindings domaine Malts (UNIQUES)
    bind(classOf[domain.malts.repositories.MaltReadRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltReadRepository])
      .asEagerSingleton()
    bind(classOf[domain.malts.repositories.MaltWriteRepository])
      .to(classOf[infrastructure.persistence.slick.repositories.malts.SlickMaltWriteRepository])
      .asEagerSingleton()
  }
}
