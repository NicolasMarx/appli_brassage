package domain.malts.model

import play.api.libs.json._

/**
 * Énumération des sources de données malts
 * Version corrigée avec debug
 */
sealed abstract class MaltSource(val name: String, val description: String)

object MaltSource {
  
  case object Manual extends MaltSource("MANUAL", "Saisie manuelle")
  case object AI_Discovery extends MaltSource("AI_DISCOVERED", "Découverte par IA")
  case object Import extends MaltSource("IMPORT", "Import CSV/API")
  case object Community extends MaltSource("COMMUNITY", "Contribution communauté")
  
  val all: List[MaltSource] = List(Manual, AI_Discovery, Import, Community)
  
  def fromName(name: String): Option[MaltSource] = {
    if (name == null) {
      println(s"⚠️  MaltSource.fromName: nom null")
      return None
    }
    
    val cleanName = name.trim.toUpperCase
    println(s"🔍 MaltSource.fromName: recherche '$name' -> '$cleanName'")
    
    val result = all.find(_.name.toUpperCase == cleanName)
    if (result.isEmpty) {
      println(s"❌ MaltSource.fromName: source non trouvée '$cleanName', sources valides: ${all.map(_.name).mkString(", ")}")
    } else {
      println(s"✅ MaltSource.fromName: trouvé $result")
    }
    result
  }
  
  // Méthode unsafe pour bypasser validation
  def unsafe(name: String): MaltSource = {
    fromName(name).getOrElse {
      println(s"⚠️  MaltSource.unsafe: source invalide '$name', utilisation de MANUAL par défaut")
      Manual
    }
  }
  
  implicit val format: Format[MaltSource] = Format(
    Reads(js => js.validate[String].flatMap { name =>
      fromName(name) match {
        case Some(source) => JsSuccess(source)
        case None => JsError(s"Source invalide: $name")
      }
    }),
    Writes(source => JsString(source.name))
  )
}
