package domain.yeasts.model

import play.api.libs.json._

/**
 * Laboratoires de levures
 * CORRECTION: Ajout méthode fromString manquante
 */
sealed abstract class YeastLaboratory(val name: String, val fullName: String) {
  def code: String = name.toUpperCase.replace(" ", "_")
  def numericCode: Int = YeastLaboratory.all.indexOf(this)
}

object YeastLaboratory {

  case object Wyeast extends YeastLaboratory("Wyeast", "Wyeast Laboratories")
  case object WhiteLabs extends YeastLaboratory("White Labs", "White Labs") 
  case object Lallemand extends YeastLaboratory("Lallemand", "Lallemand Brewing")
  case object Fermentis extends YeastLaboratory("Fermentis", "Fermentis")
  case object Safale extends YeastLaboratory("Safale", "Fermentis Safale")
  case object Saflager extends YeastLaboratory("Saflager", "Fermentis Saflager")
  case object Imperial extends YeastLaboratory("Imperial", "Imperial Yeast")
  case object Omega extends YeastLaboratory("Omega", "Omega Yeast Labs")
  case object Other extends YeastLaboratory("Other", "Autre laboratoire")

  val all: List[YeastLaboratory] = List(Wyeast, WhiteLabs, Lallemand, Fermentis, Safale, Saflager, Imperial, Omega, Other)

  // CORRECTION: Méthode fromString manquante
  def fromString(name: String): Either[String, YeastLaboratory] = {
    if (name == null || name.trim.isEmpty) return Right(Other)
    
    val cleanName = name.trim.toUpperCase.replace(" ", "_")
    val found = all.find(_.name.toUpperCase == cleanName) orElse
    all.find(_.fullName.toUpperCase.replace(" ", "_") == cleanName)
    
    found.map(Right(_)).getOrElse(Left(s"Laboratoire invalide: $name"))
  }
  
  // Méthode Option pour compatibilité
  def fromStringOption(name: String): Option[YeastLaboratory] = {
    fromString(name).toOption
  }
  
  // Alias pour compatibilité
  def fromName(name: String): Option[YeastLaboratory] = fromStringOption(name)
  
  // Méthode parse manquante (retourne Either pour validation)
  def parse(name: String): Either[String, YeastLaboratory] = fromString(name)

  implicit val format: Format[YeastLaboratory] = Format(
    Reads(js => js.validate[String].flatMap { str =>
      fromStringOption(str) match {
        case Some(lab) => JsSuccess(lab)
        case None => JsSuccess(Other) // Toujours fallback vers Other
      }
    }),
    Writes(lab => JsString(lab.name))
  )
}
