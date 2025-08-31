package domain.malts.model

import play.api.libs.json._
import java.util.UUID

/**
 * Value Object MaltId - Identifiant unique pour les malts
 * Utilise UUID pour garantir l'unicité globale
 */
final case class MaltId private (value: String) extends AnyVal {
  override def toString: String = value
}

object MaltId {
  
  def apply(value: String): Either[String, MaltId] = {
    val trimmed = value.trim
    
    if (trimmed.isEmpty) {
      Left("MaltId ne peut pas être vide")
    } else {
      try {
        UUID.fromString(trimmed) // Validation UUID
        Right(new MaltId(trimmed))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"MaltId doit être un UUID valide: '$trimmed'")
      }
    }
  }
  
  def generate(): MaltId = new MaltId(UUID.randomUUID().toString)
  
  def fromString(value: String): MaltId = {
    apply(value) match {
      case Right(maltId) => maltId
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }
  
  implicit val maltIdFormat: Format[MaltId] = new Format[MaltId] {
    def reads(json: JsValue): JsResult[MaltId] = {
      json.validate[String].flatMap { str =>
        MaltId(str) match {
          case Right(maltId) => JsSuccess(maltId)
          case Left(error) => JsError(error)
        }
      }
    }
    
    def writes(maltId: MaltId): JsValue = JsString(maltId.value)
  }
}
