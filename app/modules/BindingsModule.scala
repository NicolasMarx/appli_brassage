package modules

import com.google.inject.AbstractModule

// ---- Imports adaptés à votre structure locale ----
// Ajustez ces imports selon vos packages réels

// Si vous avez ces services dans votre structure :
// import application.services.hops.HopService

/**
 * Bindings Guice temporaire - adapté à votre structure locale actuelle
 */
final class BindingsModule extends AbstractModule {
  override def configure(): Unit = {

    // ===== Bindings temporaires - commentez/décommentez selon vos besoins =====

    // Si vous avez HopService :
    // bind(classOf[HopService]).asEagerSingleton()

    // Ajoutez ici les bindings qui correspondent à vos classes existantes
    // Par exemple, si vous avez d'autres services ou repositories :
    // bind(classOf[VotreService]).asEagerSingleton()

    println("BindingsModule chargé - configuration minimale")
  }
}