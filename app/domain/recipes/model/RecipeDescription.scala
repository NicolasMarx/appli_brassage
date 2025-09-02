package domain.recipes.model

import play.api.libs.json._

case class RecipeDescription private(value: String) extends AnyVal {
  override def toString: String = value
}

object RecipeDescription {
  def create(description: String): Either[String, RecipeDescription] = {
    if (description.trim.isEmpty) Left("La description ne peut pas être vide")
    else if (description.length > 2000) Left("La description ne peut pas dépasser 2000 caractères")
    else Right(RecipeDescription(description.trim))
  }
  
  def unsafe(description: String): RecipeDescription = RecipeDescription(description)

  implicit val format: Format[RecipeDescription] = Json.valueFormat[RecipeDescription]
}