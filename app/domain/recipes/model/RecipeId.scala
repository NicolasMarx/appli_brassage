package domain.recipes.model

import play.api.libs.json._
import java.util.UUID
import scala.util.Try

case class RecipeId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
  def asString: String = value.toString
}

object RecipeId {
  def generate(): RecipeId = RecipeId(UUID.randomUUID())
  
  def fromString(str: String): Either[String, RecipeId] = {
    Try(UUID.fromString(str)).toEither
      .left.map(_ => "Format UUID invalide")
      .map(RecipeId(_))
  }
  
  def unsafe(str: String): RecipeId = RecipeId(UUID.fromString(str))
  
  implicit val format: Format[RecipeId] = Json.valueFormat[RecipeId]
}