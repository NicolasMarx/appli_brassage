package domain.yeasts.model

import play.api.libs.json._

sealed trait FlocculationLevel {
  def name: String
  def description: String
  def rackingRecommendation: String
}

object FlocculationLevel {
  case object Low extends FlocculationLevel { 
    val name = "Low"
    val description = "Reste en suspension longtemps"
    val rackingRecommendation = "Attendre 2-3 semaines avant soutirage"
  }
  case object Medium extends FlocculationLevel { 
    val name = "Medium"
    val description = "Floculation modérée"
    val rackingRecommendation = "Soutirer après 1-2 semaines"
  }
  case object MediumHigh extends FlocculationLevel { 
    val name = "Medium-High"
    val description = "Floculation élevée"
    val rackingRecommendation = "Soutirer après 1 semaine"
  }
  case object High extends FlocculationLevel { 
    val name = "High"
    val description = "Floculation rapide"
    val rackingRecommendation = "Soutirer après 5-7 jours"
  }
  case object VeryHigh extends FlocculationLevel { 
    val name = "Very High"
    val description = "Floculation très rapide"
    val rackingRecommendation = "Soutirer après 3-5 jours"
  }
  
  val all: List[FlocculationLevel] = List(Low, Medium, MediumHigh, High, VeryHigh)
  
  def fromString(value: String): Option[FlocculationLevel] = {
    all.find(_.name.equalsIgnoreCase(value))
  }
  
  implicit val format: Format[FlocculationLevel] = Format(
    Reads(json => json.validate[String].flatMap(s => 
      fromString(s).map(JsSuccess(_)).getOrElse(JsError(s"Invalid flocculation level: $s"))
    )),
    Writes(fl => JsString(fl.name))
  )
}
