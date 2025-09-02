// Test script pour les commandes Recipes
import application.recipes.commands._

// Test CreateRecipeCommand validation
val validCommand = CreateRecipeCommand(
  name = "Test IPA",
  description = Some("Une recette test"),
  styleId = "21A",
  styleName = "American IPA", 
  styleCategory = "IPA",
  batchSizeValue = 20.0,
  batchSizeUnit = "LITERS"
)

println("=== Test Command Validation ===")
println(s"Valid command: ${validCommand.validate()}")

val invalidCommand = CreateRecipeCommand(
  name = "",
  description = None,
  styleId = "",
  styleName = "",
  styleCategory = "",
  batchSizeValue = -5.0,
  batchSizeUnit = "INVALID"
)

println(s"Invalid command: ${invalidCommand.validate()}")

// Test Value Objects
import domain.recipes.model._

println("\n=== Test Value Objects ===")
println(s"Valid RecipeName: ${RecipeName.create("Test Recipe")}")
println(s"Invalid RecipeName: ${RecipeName.create("")}")
println(s"Valid BatchSize: ${BatchSize.create(20.0, VolumeUnit.Liters)}")
println(s"Invalid BatchSize: ${BatchSize.create(-5.0, VolumeUnit.Liters)}")