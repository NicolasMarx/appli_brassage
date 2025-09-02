package domain.recipes.model

import play.api.libs.json._

case class RecipeName private(value: String) extends AnyVal {
  override def toString: String = value
}

object RecipeName {
  def create(name: String): Either[String, RecipeName] = {
    if (name.trim.isEmpty) Left("Le nom de la recette ne peut pas être vide")
    else if (name.length < 3) Left("Le nom doit contenir au moins 3 caractères") 
    else if (name.length > 200) Left("Le nom ne peut pas dépasser 200 caractères")
    else if (!name.matches("^[a-zA-Z0-9\\s\\-_àáâãäåæçèéêëìíîïðñòóôõöøùúûüý]*$")) {
      Left("Le nom contient des caractères non autorisés")
    }
    else Right(RecipeName(name.trim))
  }

  def unsafe(name: String): RecipeName = create(name).getOrElse(RecipeName("Unknown Recipe"))

  implicit val format: Format[RecipeName] = Json.valueFormat[RecipeName]
}