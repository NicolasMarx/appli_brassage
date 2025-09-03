package domain.yeasts.model

import play.api.libs.json._

/**
 * Énumération des types de levures
 * CORRECTION: Ajout des méthodes fromName, parse et recommendedForBeerStyle
 */
sealed abstract class YeastType(val name: String, val description: String) {
  def numericCode: Int = YeastType.all.indexOf(this)
}

object YeastType {
  
  case object ALE extends YeastType("ALE", "Levure ale haute fermentation")
  case object LAGER extends YeastType("LAGER", "Levure lager basse fermentation") 
  case object WILD extends YeastType("WILD", "Levure sauvage")
  case object BRETT extends YeastType("BRETT", "Brettanomyces")
  case object WINE extends YeastType("WINE", "Levure vinique")
  case object MEAD extends YeastType("MEAD", "Levure hydromel")
  case object CIDER extends YeastType("CIDER", "Levure cidre")

  val all: List[YeastType] = List(ALE, LAGER, WILD, BRETT, WINE, MEAD, CIDER)

  // CORRECTION: Méthode fromName manquante
  def fromName(name: String): Option[YeastType] = {
    if (name == null || name.trim.isEmpty) return None
    val cleanName = name.trim.toUpperCase
    all.find(_.name.toUpperCase == cleanName)
  }

  // CORRECTION: Méthode parse manquante (retourne Either pour validation)
  def parse(name: String): Either[String, YeastType] = {
    fromName(name).toRight(s"Type de levure invalide: $name")
  }

  // CORRECTION: Méthode fromString pour compatibilité batch service
  def fromString(name: String): Either[String, YeastType] = parse(name)

  // CORRECTION: Méthode recommendedForBeerStyle manquante
  def recommendedForBeerStyle(beerStyle: String): List[YeastType] = {
    if (beerStyle == null || beerStyle.trim.isEmpty) return List.empty
    
    val style = beerStyle.toLowerCase
    style match {
      case s if s.contains("ipa") || s.contains("pale ale") => List(ALE)
      case s if s.contains("lager") || s.contains("pilsner") => List(LAGER) 
      case s if s.contains("wheat") => List(ALE, LAGER)
      case s if s.contains("stout") || s.contains("porter") => List(ALE)
      case s if s.contains("lambic") || s.contains("sour") => List(WILD, BRETT)
      case s if s.contains("saison") => List(ALE, BRETT)
      case _ => List(ALE, LAGER) // Par défaut
    }
  }

  implicit val format: Format[YeastType] = Format(
    Reads(js => js.validate[String].flatMap { str =>
      fromName(str) match {
        case Some(yeastType) => JsSuccess(yeastType)
        case None => JsError(s"Type de levure invalide: $str")
      }
    }),
    Writes(yeastType => JsString(yeastType.name))
  )
}
