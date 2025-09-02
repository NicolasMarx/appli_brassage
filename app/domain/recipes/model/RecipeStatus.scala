package domain.recipes.model

import play.api.libs.json._

sealed trait RecipeStatus {
  def value: String
  def name: String = value
}

object RecipeStatus {
  case object Draft extends RecipeStatus { val value = "DRAFT" }
  case object Published extends RecipeStatus { val value = "PUBLISHED" }
  case object Archived extends RecipeStatus { val value = "ARCHIVED" }
  
  def fromString(status: String): Either[String, RecipeStatus] = status.toUpperCase match {
    case "DRAFT" => Right(Draft)
    case "PUBLISHED" => Right(Published)
    case "ARCHIVED" => Right(Archived)
    case _ => Left(s"Statut non valide: $status")
  }
  
  def fromName(name: String): Option[RecipeStatus] = fromString(name).toOption

  implicit val format: Format[RecipeStatus] = new Format[RecipeStatus] {
    def reads(json: JsValue): JsResult[RecipeStatus] = json match {
      case JsString(value) => fromString(value) match {
        case Right(status) => JsSuccess(status)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(status: RecipeStatus): JsValue = JsString(status.value)
  }
}