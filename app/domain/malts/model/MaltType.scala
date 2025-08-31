package domain.malts.model

import play.api.libs.json._

/**
 * Énumération des types de malts
 * Version corrigée avec matching case-insensitive
 */
sealed abstract class MaltType(val name: String, val category: String)

object MaltType {
  
  case object BASE extends MaltType("BASE", "Base")
  case object SPECIALTY extends MaltType("SPECIALTY", "Spécialité")  
  case object CRYSTAL extends MaltType("CRYSTAL", "Crystal/Caramel")
  case object ROASTED extends MaltType("ROASTED", "Torréfié")
  case object WHEAT extends MaltType("WHEAT", "Blé")
  case object RYE extends MaltType("RYE", "Seigle")
  case object OATS extends MaltType("OATS", "Avoine")
  
  val all: List[MaltType] = List(BASE, SPECIALTY, CRYSTAL, ROASTED, WHEAT, RYE, OATS)
  
  def fromName(name: String): Option[MaltType] = {
    if (name == null) {
      println(s"⚠️  MaltType.fromName: nom null")
      return None
    }
    
    val cleanName = name.trim.toUpperCase
    println(s"🔍 MaltType.fromName: recherche '$name' -> '$cleanName'")
    
    val result = all.find(_.name.toUpperCase == cleanName)
    if (result.isEmpty) {
      println(s"❌ MaltType.fromName: type non trouvé '$cleanName', types valides: ${all.map(_.name).mkString(", ")}")
    } else {
      println(s"✅ MaltType.fromName: trouvé $result")
    }
    result
  }
  
  // Méthode unsafe pour bypasser validation (debug uniquement)
  def unsafe(name: String): MaltType = {
    fromName(name).getOrElse {
      println(s"⚠️  MaltType.unsafe: type invalide '$name', utilisation de BASE par défaut")
      BASE
    }
  }
  
  implicit val format: Format[MaltType] = Format(
    Reads(js => js.validate[String].flatMap { name =>
      fromName(name) match {
        case Some(maltType) => JsSuccess(maltType)
        case None => JsError(s"Type de malt invalide: $name")
      }
    }),
    Writes(maltType => JsString(maltType.name))
  )
}
