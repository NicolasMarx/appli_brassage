package domain.malts.model

import play.api.libs.json._

sealed trait MaltType {
  def name: String
  def description: String
}

object MaltType {
  
  case object BASE extends MaltType {
    val name = "BASE"
    val description = "Malt de base avec pouvoir enzymatique élevé"
  }
  
  case object SPECIALTY extends MaltType {
    val name = "SPECIALTY"  
    val description = "Malt spécial pour saveur et couleur"
  }
  
  case object CARAMEL extends MaltType {
    val name = "CARAMEL"
    val description = "Malt caramel/crystal pour douceur"
  }
  
  case object CRYSTAL extends MaltType {
    val name = "CRYSTAL"
    val description = "Malt crystal pour douceur et couleur"
  }
  
  case object ROASTED extends MaltType {
    val name = "ROASTED"
    val description = "Malt torréfié pour bières sombres"
  }
  
  case object OTHER extends MaltType {
    val name = "OTHER"
    val description = "Autres types de malts"
  }
  
  val all: List[MaltType] = List(BASE, SPECIALTY, CARAMEL, CRYSTAL, ROASTED, OTHER)
  
  def fromName(name: String): Option[MaltType] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  implicit val format: Format[MaltType] = Format(
    Reads(js => js.validate[String].map(fromName).flatMap {
      case Some(maltType) => JsSuccess(maltType)
      case None => JsError("Type de malt invalide")
    }),
    Writes(maltType => JsString(maltType.name))
  )
}
