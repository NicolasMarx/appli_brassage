package domain.recipes.model

import play.api.libs.json._

case class BeerStyle private(
  id: String,
  name: String,
  category: String
) {
  def fullName: String = s"$id - $name"
}

object BeerStyle {
  def create(id: String, name: String, category: String): Either[String, BeerStyle] = {
    if (id.trim.isEmpty) Left("L'ID du style ne peut pas être vide")
    else if (name.trim.isEmpty) Left("Le nom du style ne peut pas être vide")
    else if (category.trim.isEmpty) Left("La catégorie ne peut pas être vide")
    else Right(BeerStyle(id.trim, name.trim, category.trim))
  }

  // Styles BJCP prédéfinis pour démarrage
  val AmericanIPA = BeerStyle("21A", "American IPA", "IPA")
  val AmericanPaleAle = BeerStyle("18B", "American Pale Ale", "Pale American Ale")
  val AmericanStout = BeerStyle("20B", "American Stout", "American Porter and Stout")
  val WheatBeer = BeerStyle("1D", "American Wheat Beer", "Standard American Beer")

  implicit val format: Format[BeerStyle] = Json.format[BeerStyle]
}