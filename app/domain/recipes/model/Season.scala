package domain.recipes.model

import play.api.libs.json._

/**
 * Énumération des saisons pour les recommandations saisonnières
 */
sealed trait Season {
  def value: String
  def name: String = value
}

object Season {
  case object Spring extends Season { val value = "SPRING" }
  case object Summer extends Season { val value = "SUMMER" }
  case object Autumn extends Season { val value = "AUTUMN" }
  case object Winter extends Season { val value = "WINTER" }
  
  def fromString(season: String): Either[String, Season] = season.toUpperCase match {
    case "SPRING" => Right(Spring)
    case "SUMMER" => Right(Summer)
    case "AUTUMN" => Right(Autumn)
    case "WINTER" => Right(Winter)
    case _ => Left(s"Saison inconnue: $season")
  }
  
  def fromName(name: String): Option[Season] = fromString(name).toOption
  
  implicit val format: Format[Season] = new Format[Season] {
    def reads(json: JsValue): JsResult[Season] = json match {
      case JsString(value) => fromString(value) match {
        case Right(season) => JsSuccess(season)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(season: Season): JsValue = JsString(season.value)
  }
}