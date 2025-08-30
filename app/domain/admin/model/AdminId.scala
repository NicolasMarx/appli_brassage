// app/domain/admin/model/AdminId.scala
package domain.admin.model

import play.api.libs.json._
import java.util.UUID

/**
 * Value Object AdminId - Identifiant unique pour les administrateurs
 * Utilise UUID pour garantir l'unicité globale
 */
final case class AdminId private (value: String) extends AnyVal {
  override def toString: String = value
}

object AdminId {
  def apply(value: String): Either[String, AdminId] = {
    val trimmed = value.trim
    
    if (trimmed.isEmpty) {
      Left("AdminId ne peut pas être vide")
    } else {
      try {
        UUID.fromString(trimmed) // Validation UUID
        Right(new AdminId(trimmed))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"AdminId doit être un UUID valide: '$trimmed'")
      }
    }
  }
  
  def generate(): AdminId = new AdminId(UUID.randomUUID().toString)
  
  def fromString(value: String): AdminId = {
    apply(value) match {
      case Right(adminId) => adminId
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }
  
  implicit val adminIdFormat: Format[AdminId] = new Format[AdminId] {
    def reads(json: JsValue): JsResult[AdminId] = {
      json.validate[String].flatMap { str =>
        AdminId(str) match {
          case Right(adminId) => JsSuccess(adminId)
          case Left(error) => JsError(error)
        }
      }
    }
    
    def writes(adminId: AdminId): JsValue = JsString(adminId.value)
  }
}