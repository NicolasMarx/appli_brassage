package domain.malts.model

import java.util.UUID
import play.api.libs.json._
import scala.util.{Try, Success, Failure}

/**
 * Value Object pour identifiant de malt
 * Version harmonisée UUID/String avec conversions explicites
 */
case class MaltId private(value: UUID) extends AnyVal {
  
  // Conversion explicite vers String pour compatibilité
  override def toString: String = value.toString
  def asString: String = value.toString
  def asUUID: UUID = value
  
  // Pour les comparaisons avec String dans Slick
  def equalsString(str: String): Boolean = value.toString == str
}

object MaltId {
  
  def apply(uuid: UUID): MaltId = new MaltId(uuid)
  
  def apply(value: String): Either[String, MaltId] = {
    Try(UUID.fromString(value)) match {
      case Success(uuid) => Right(new MaltId(uuid))
      case Failure(ex) => Left(s"UUID invalide : $value (${ex.getMessage})")
    }
  }
  
  def fromString(value: String): Option[MaltId] = apply(value).toOption
  
  // Méthode unsafe pour bypasser validation (debug uniquement)
  def unsafe(value: String): MaltId = {
    Try(UUID.fromString(value)) match {
      case Success(uuid) => new MaltId(uuid)
      case Failure(_) => 
        println(s"⚠️  MaltId.unsafe: UUID invalide '$value', génération d'un nouvel UUID")
        new MaltId(UUID.randomUUID())
    }
  }
  
  def generate(): MaltId = new MaltId(UUID.randomUUID())
  
  def toOption: Either[String, MaltId] => Option[MaltId] = _.toOption
  
  implicit val format: Format[MaltId] = Format(
    Reads { js => 
      js.validate[String].flatMap { str =>
        fromString(str) match {
          case Some(maltId) => JsSuccess(maltId)
          case None => JsError("UUID invalide")
        }
      }
    },
    Writes(maltId => JsString(maltId.toString))
  )
}
