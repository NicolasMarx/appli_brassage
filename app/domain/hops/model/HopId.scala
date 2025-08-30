// app/domain/hops/model/HopId.scala
package domain.hops.model

import play.api.libs.json._
import java.util.UUID

final case class HopId private (value: String) extends AnyVal {
  override def toString: String = value
}

object HopId {
  def apply(value: String): Either[String, HopId] = {
    val trimmed = value.trim

    if (trimmed.isEmpty) {
      Left("HopId ne peut pas être vide")
    } else {
      try {
        UUID.fromString(trimmed)
        Right(new HopId(trimmed))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"HopId doit être un UUID valide: '$trimmed'")
      }
    }
  }

  def generate(): HopId = new HopId(UUID.randomUUID().toString)

  def fromString(value: String): HopId = {
    apply(value) match {
      case Right(hopId) => hopId
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }

  implicit val hopIdFormat: Format[HopId] = new Format[HopId] {
    def reads(json: JsValue): JsResult[HopId] = {
      json.validate[String].flatMap { str =>
        HopId(str) match {
          case Right(hopId) => JsSuccess(hopId)
          case Left(error) => JsError(error)
        }
      }
    }

    def writes(hopId: HopId): JsValue = JsString(hopId.value)
  }
}// Identity Hop
// TODO: Implémenter selon l'architecture DDD/CQRS

