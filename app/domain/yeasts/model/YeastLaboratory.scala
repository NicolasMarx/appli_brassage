package domain.yeasts.model

import play.api.libs.json._

/**
 * Laboratoires de levures
 * CORRECTION: Ajout méthode fromString manquante
 */
sealed abstract class YeastLaboratory(val name: String, val fullName: String)

object YeastLaboratory {

  case object Wyeast extends YeastLaboratory("WYEAST", "Wyeast Laboratories")
  case object WhiteLabs extends YeastLaboratory("WHITE_LABS", "White Labs") 
  case object Lallemand extends YeastLaboratory("LALLEMAND", "Lallemand Brewing")
  case object Fermentis extends YeastLaboratory("FERMENTIS", "Fermentis")
  case object Imperial extends YeastLaboratory("IMPERIAL", "Imperial Yeast")
  case object Omega extends YeastLaboratory("OMEGA", "Omega Yeast Labs")
  case object Other extends YeastLaboratory("OTHER", "Autre laboratoire")

  val all: List[YeastLaboratory] = List(Wyeast, WhiteLabs, Lallemand, Fermentis, Imperial, Omega, Other)

  // CORRECTION: Méthode fromString manquante
  def fromString(name: String): Option[YeastLaboratory] = {
    if (name == null || name.trim.isEmpty) return Some(Other)
    
    val cleanName = name.trim.toUpperCase.replace(" ", "_")
    all.find(_.name.toUpperCase == cleanName) orElse
    all.find(_.fullName.toUpperCase.replace(" ", "_") == cleanName) orElse
    Some(Other) // Fallback vers Other si non trouvé
  }
  
  // Alias pour compatibilité
  def fromName(name: String): Option[YeastLaboratory] = fromString(name)
  
  // Méthode parse manquante (retourne Either pour validation)
  def parse(name: String): Either[String, YeastLaboratory] = {
    fromString(name).toRight(s"Laboratoire invalide: $name")
  }

  implicit val format: Format[YeastLaboratory] = Format(
    Reads(js => js.validate[String].flatMap { str =>
      fromString(str) match {
        case Some(lab) => JsSuccess(lab)
        case None => JsSuccess(Other) // Toujours fallback vers Other
      }
    }),
    Writes(lab => JsString(lab.name))
  )
}
