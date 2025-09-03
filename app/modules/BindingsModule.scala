package modules

import com.google.inject.AbstractModule
import domain.hops.repositories.{HopReadRepository, HopWriteRepository}
import infrastructure.persistence.slick.repositories.hops.{SlickHopReadRepository, SlickHopWriteRepository}
import domain.recipes.repositories.{RecipeRepository, RecipeReadRepository, RecipeWriteRepository}
import infrastructure.persistence.slick.repositories.recipes.{SlickRecipeRepository, SlickRecipeReadRepository, SlickRecipeWriteRepository}

// Import repositories yeasts
import domain.yeasts.repositories.{YeastReadRepository, YeastWriteRepository}
import infrastructure.persistence.slick.repositories.yeasts.{SlickYeastReadRepository, SlickYeastWriteRepository}

// Import repositories malts

/**
 * Bindings Guice - Connecte les interfaces du domaine aux implémentations Slick
 */
final class BindingsModule extends AbstractModule {
  override def configure(): Unit = {

    // ===== HOP REPOSITORIES =====
    bind(classOf[HopReadRepository]).to(classOf[SlickHopReadRepository])
    bind(classOf[HopWriteRepository]).to(classOf[SlickHopWriteRepository])

    // ===== RECIPE REPOSITORIES =====
    bind(classOf[RecipeReadRepository]).to(classOf[SlickRecipeReadRepository])
    bind(classOf[RecipeWriteRepository]).to(classOf[SlickRecipeWriteRepository])
    bind(classOf[RecipeRepository]).to(classOf[SlickRecipeRepository])

    // ===== YEAST REPOSITORIES =====
    bind(classOf[YeastReadRepository]).to(classOf[SlickYeastReadRepository])
    bind(classOf[YeastWriteRepository]).to(classOf[SlickYeastWriteRepository])

    println("BindingsModule chargé - Repositories Hops, Recipes et Yeasts connectés à PostgreSQL")
  }
}
