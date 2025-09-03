package domain.yeasts.model

import play.api.libs.json._

/**
 * Niveau de floculation des levures
 * CORRECTION: Ajout méthodes fromName et propriété clarificationTime
 */
sealed abstract class FlocculationLevel(
  val name: String, 
  val description: String,
  val clarificationTime: Int // CORRECTION: Propriété manquante (en jours)
) {
  def rackingRecommendation: String = name match {
    case "LOW" => "Soutirage après 2-3 semaines, attention aux levures en suspension"
    case "MEDIUM" => "Soutirage après 10-14 jours, équilibre idéal"
    case "HIGH" => "Soutirage après 7-10 jours, clarification rapide"
    case "VERY_HIGH" => "Soutirage après 4-7 jours, floculation très rapide"
    case _ => "Soutirage selon observation visuelle"
  }
  
  def numericCode: Int = FlocculationLevel.all.indexOf(this)
}

object FlocculationLevel {

  case object LOW extends FlocculationLevel("LOW", "Floculation faible", 14)
  case object MEDIUM extends FlocculationLevel("MEDIUM", "Floculation moyenne", 10) 
  case object HIGH extends FlocculationLevel("HIGH", "Floculation élevée", 7)
  case object VERY_HIGH extends FlocculationLevel("VERY_HIGH", "Floculation très élevée", 4)

  val all: List[FlocculationLevel] = List(LOW, MEDIUM, HIGH, VERY_HIGH)

  // CORRECTION: Méthode fromName manquante
  def fromName(name: String): Option[FlocculationLevel] = {
    if (name == null || name.trim.isEmpty) return None
    val cleanName = name.trim.toUpperCase.replace(" ", "_")
    all.find(_.name.toUpperCase == cleanName)
  }
  
  // Méthode fromString pour batch service
  def fromString(name: String): Either[String, FlocculationLevel] = {
    fromName(name).toRight(s"Niveau de floculation invalide: $name")
  }

  implicit val format: Format[FlocculationLevel] = Format(
    Reads(js => js.validate[String].flatMap { str =>
      fromName(str) match {
        case Some(level) => JsSuccess(level)
        case None => JsError(s"Niveau de floculation invalide: $str")
      }
    }),
    Writes(level => JsString(level.name))
  )
}
