package domain.malts.model

import java.util.UUID
import play.api.libs.json._

/**
 * Value Object pour l'identifiant des malts
 */
case class MaltId private(value: UUID) extends AnyVal {
  override def toString: String = value.toString
  
  // Méthode pour récupérer l'UUID (utilisée dans Slick)
  def asUUID: UUID = value
}

object MaltId {
  def generate(): MaltId = MaltId(UUID.randomUUID())
  
  def fromString(id: String): MaltId = {
    MaltId(UUID.fromString(id))
  }
  
  // Méthode unsafe pour les cas d'erreur
  def unsafe(id: String): MaltId = {
    try {
      fromString(id)
    } catch {
      case _: IllegalArgumentException => generate() // Génère un nouvel ID si l'UUID est invalide
    }
  }
  
  def apply(uuid: UUID): MaltId = new MaltId(uuid)
  def apply(id: String): MaltId = fromString(id)
  
  implicit val format: Format[MaltId] = Format(
    Reads(js => js.validate[String].map(fromString)),
    Writes(maltId => JsString(maltId.toString))
  )
}
