package domain.malts.model

import play.api.libs.json._
import java.util.UUID

/**
 * Value Object représentant l'identifiant unique d'un malt
 * Basé sur le pattern HopId qui fonctionne dans le projet
 */
case class MaltId(value: String) extends AnyVal {
  override def toString: String = value
}

object MaltId {

  /**
   * Constructeur principal avec validation
   */
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

  /**
   * Génère un nouvel ID unique
   */
  def generate(): MaltId = new MaltId(UUID.randomUUID().toString)

  /**
   * Version unsafe pour les cas où on est sûr de la validité
   */
  def fromString(value: String): MaltId = {
    apply(value) match {
      case Right(maltId) => maltId
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }

  /**
   * Validation du format d'un MaltId
   */
  private def isValidMaltId(value: String): Boolean = {
    try {
      UUID.fromString(value)
      true
    } catch {
      case _: IllegalArgumentException => false
    }
  }

  /**
   * Crée un MaltId en format UUID à partir d'un nom de malt
   */
  def fromMaltName(name: String): MaltId = {
    generate() // Pour l'instant, génère toujours un UUID unique
  }

  implicit val format: Format[MaltId] = new Format[MaltId] {
    def reads(json: JsValue): JsResult[MaltId] = {
      json.validate[String].flatMap { str =>
        MaltId.apply(str) match {
          case Right(maltId) => JsSuccess(maltId)
          case Left(error) => JsError(error)
        }
      }
    }
    
    def writes(maltId: MaltId): JsValue = JsString(maltId.value)
  }
}
