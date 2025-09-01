package domain.yeasts.model

import play.api.libs.json._

sealed trait YeastType {
  def name: String
}

object YeastType {
  case object Ale extends YeastType { val name = "Ale" }
  case object Lager extends YeastType { val name = "Lager" }
  case object Wheat extends YeastType { val name = "Wheat" }
  case object Saison extends YeastType { val name = "Saison" }
  case object Wild extends YeastType { val name = "Wild" }
  case object Sour extends YeastType { val name = "Sour" }
  case object Champagne extends YeastType { val name = "Champagne" }
  case object Kveik extends YeastType { val name = "Kveik" }
  
  val all: List[YeastType] = List(Ale, Lager, Wheat, Saison, Wild, Sour, Champagne, Kveik)
  
  def fromString(value: String): Option[YeastType] = {
    all.find(_.name.equalsIgnoreCase(value))
  }
  
  implicit val format: Format[YeastType] = Format(
    Reads(json => json.validate[String].flatMap(s => 
      fromString(s).map(JsSuccess(_)).getOrElse(JsError(s"Invalid yeast type: $s"))
    )),
    Writes(yt => JsString(yt.name))
  )
}
