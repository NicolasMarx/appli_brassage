package domain.recipes.model

import play.api.libs.json._
import java.util.UUID
import scala.util.Try

case class UserId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
  def asString: String = value.toString
}

object UserId {
  def generate(): UserId = UserId(UUID.randomUUID())
  
  def fromString(str: String): Either[String, UserId] = {
    Try(UUID.fromString(str)).toEither
      .left.map(_ => "Format UUID invalide")
      .map(UserId(_))
  }

  def unsafe(str: String): UserId = fromString(str).getOrElse(generate())
  
  implicit val format: Format[UserId] = Json.valueFormat[UserId]
}