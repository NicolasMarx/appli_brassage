package domain.malts.model

import play.api.libs.json._

sealed trait MaltStatus {
  def name: String
}

object MaltStatus {
  case object ACTIVE extends MaltStatus { val name = "ACTIVE" }
  case object INACTIVE extends MaltStatus { val name = "INACTIVE" }
  case object PENDING_REVIEW extends MaltStatus { val name = "PENDING_REVIEW" }

  val all: List[MaltStatus] = List(ACTIVE, INACTIVE, PENDING_REVIEW)

  def fromName(name: String): Option[MaltStatus] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }

  implicit val format: Format[MaltStatus] = Format(
    Reads(js => js.validate[String].map(fromName).flatMap {
      case Some(status) => JsSuccess(status)
      case None => JsError("Statut de malt invalide")
    }),
    Writes(status => JsString(status.name))
  )
}
