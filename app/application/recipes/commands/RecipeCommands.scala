package application.recipes.commands

import play.api.libs.json._
import domain.recipes.model._

case class CreateRecipeCommand(
  name: String,
  description: Option[String],
  styleId: String,
  styleName: String,
  styleCategory: String,
  batchSizeValue: Double,
  batchSizeUnit: String,
  hops: List[CreateRecipeHopCommand] = List.empty,
  malts: List[CreateRecipeMaltCommand] = List.empty,
  yeastId: Option[String] = None
) {
  
  // Validation métier OBLIGATOIRE
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (name.trim.isEmpty) errors += "Nom requis"
    if (name.length < 3) errors += "Le nom doit contenir au moins 3 caractères"
    if (name.length > 200) errors += "Le nom ne peut pas dépasser 200 caractères"
    if (batchSizeValue <= 0) errors += "Volume doit être positif"
    if (batchSizeValue > 10000) errors += "Volume trop important (max 10000L)"
    if (styleId.trim.isEmpty) errors += "Style de bière requis"
    if (styleName.trim.isEmpty) errors += "Nom du style requis"
    if (styleCategory.trim.isEmpty) errors += "Catégorie du style requise"
    
    // Validation unité de volume
    VolumeUnit.fromString(batchSizeUnit) match {
      case Left(error) => errors += error
      case Right(_) => // OK
    }
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object CreateRecipeCommand {
  implicit val format: Format[CreateRecipeCommand] = Json.format[CreateRecipeCommand]
}

case class CreateRecipeHopCommand(
  hopId: String,
  quantityGrams: Double,
  additionTime: Int, // minutes
  usage: String // "BOIL", "AROMA", "DRY_HOP", "WHIRLPOOL"
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (hopId.trim.isEmpty) errors += "ID houblon requis"
    if (quantityGrams <= 0) errors += "Quantité de houblon doit être positive"
    if (additionTime < 0) errors += "Temps d'addition ne peut pas être négatif"
    
    // Validation usage
    HopUsage.fromString(usage) match {
      case Left(error) => errors += error
      case Right(_) => // OK
    }
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object CreateRecipeHopCommand {
  implicit val format: Format[CreateRecipeHopCommand] = Json.format[CreateRecipeHopCommand]
}

case class CreateRecipeMaltCommand(
  maltId: String,
  quantityKg: Double,
  percentage: Option[Double] = None
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (maltId.trim.isEmpty) errors += "ID malt requis"
    if (quantityKg <= 0) errors += "Quantité de malt doit être positive"
    
    percentage.foreach { pct =>
      if (pct < 0 || pct > 100) errors += "Pourcentage doit être entre 0 et 100"
    }
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object CreateRecipeMaltCommand {
  implicit val format: Format[CreateRecipeMaltCommand] = Json.format[CreateRecipeMaltCommand]
}

case class UpdateRecipeCommand(
  recipeId: String,
  name: Option[String] = None,
  description: Option[String] = None,
  styleId: Option[String] = None,
  styleName: Option[String] = None,
  styleCategory: Option[String] = None,
  batchSizeValue: Option[Double] = None,
  batchSizeUnit: Option[String] = None
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (recipeId.trim.isEmpty) errors += "ID recette requis"
    
    name.foreach { n =>
      if (n.trim.isEmpty) errors += "Nom ne peut pas être vide"
      else if (n.length < 3) errors += "Le nom doit contenir au moins 3 caractères"
      else if (n.length > 200) errors += "Le nom ne peut pas dépasser 200 caractères"
    }
    
    batchSizeValue.foreach { size =>
      if (size <= 0) errors += "Volume doit être positif"
      else if (size > 10000) errors += "Volume trop important (max 10000L)"
    }
    
    batchSizeUnit.foreach { unit =>
      VolumeUnit.fromString(unit) match {
        case Left(error) => errors += error
        case Right(_) => // OK
      }
    }
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object UpdateRecipeCommand {
  implicit val format: Format[UpdateRecipeCommand] = Json.format[UpdateRecipeCommand]
}

case class DeleteRecipeCommand(
  recipeId: String,
  deletedBy: Option[String] = None,
  reason: Option[String] = None
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (recipeId.trim.isEmpty) errors += "ID recette requis"
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object DeleteRecipeCommand {
  implicit val format: Format[DeleteRecipeCommand] = Json.format[DeleteRecipeCommand]
}

case class AddIngredientToRecipeCommand(
  recipeId: String,
  ingredientType: String, // "HOP", "MALT", "YEAST", "OTHER"
  ingredientId: String,
  quantity: Double,
  additionalData: Map[String, String] = Map.empty // Pour données spécifiques (temps, usage, etc.)
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (recipeId.trim.isEmpty) errors += "ID recette requis"
    if (ingredientId.trim.isEmpty) errors += "ID ingrédient requis"
    if (quantity <= 0) errors += "Quantité doit être positive"
    
    ingredientType.toUpperCase match {
      case "HOP" | "MALT" | "YEAST" | "OTHER" => // OK
      case _ => errors += "Type d'ingrédient non valide"
    }
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object AddIngredientToRecipeCommand {
  implicit val format: Format[AddIngredientToRecipeCommand] = Json.format[AddIngredientToRecipeCommand]
}

case class RemoveIngredientFromRecipeCommand(
  recipeId: String,
  ingredientType: String,
  ingredientId: String
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (recipeId.trim.isEmpty) errors += "ID recette requis"
    if (ingredientId.trim.isEmpty) errors += "ID ingrédient requis"
    
    ingredientType.toUpperCase match {
      case "HOP" | "MALT" | "YEAST" | "OTHER" => // OK
      case _ => errors += "Type d'ingrédient non valide"
    }
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object RemoveIngredientFromRecipeCommand {
  implicit val format: Format[RemoveIngredientFromRecipeCommand] = Json.format[RemoveIngredientFromRecipeCommand]
}

case class PublishRecipeCommand(
  recipeId: String,
  publishedBy: String
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (recipeId.trim.isEmpty) errors += "ID recette requis"
    if (publishedBy.trim.isEmpty) errors += "Utilisateur requis"
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object PublishRecipeCommand {
  implicit val format: Format[PublishRecipeCommand] = Json.format[PublishRecipeCommand]
}

case class ArchiveRecipeCommand(
  recipeId: String,
  archivedBy: String,
  reason: Option[String] = None
) {
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (recipeId.trim.isEmpty) errors += "ID recette requis"
    if (archivedBy.trim.isEmpty) errors += "Utilisateur requis"
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object ArchiveRecipeCommand {
  implicit val format: Format[ArchiveRecipeCommand] = Json.format[ArchiveRecipeCommand]
}