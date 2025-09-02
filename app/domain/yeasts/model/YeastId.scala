package domain.yeasts.model

import domain.shared.ValueObject
import play.api.libs.json._
import java.util.UUID

/**
 * Value Object pour l'identifiant unique des levures
 * Garantit l'unicité et la validité des identifiants yeast
 */
case class YeastId(value: UUID) extends ValueObject {
  require(value != null, "YeastId ne peut pas être null")
  
  def asString: String = value.toString
  
  override def toString: String = value.toString
}

object YeastId {
  /**
   * Génère un nouvel identifiant unique
   */
  def generate(): YeastId = YeastId(UUID.randomUUID())
  
  /**
   * Crée un YeastId à partir d'une chaîne UUID
   */
  def fromString(id: String): Either[String, YeastId] = {
    try {
      Right(YeastId(UUID.fromString(id)))
    } catch {
      case _: IllegalArgumentException => Left(s"Format UUID invalide: $id")
    }
  }
  
  /**
   * Crée un YeastId à partir d'un UUID existant
   */
  def apply(uuid: UUID): YeastId = new YeastId(uuid)
  
  /**
   * Version unsafe pour les cas où on a déjà validé la donnée
   */
  def unsafe(str: String): YeastId = YeastId(UUID.fromString(str))
  
  implicit val format: Format[YeastId] = Format(
    Reads(js => js.validate[String].flatMap { str =>
      fromString(str) match {
        case Right(yeastId) => JsSuccess(yeastId)
        case Left(error) => JsError(error)
      }
    }),
    Writes(yeastId => JsString(yeastId.asString))
  )
}
