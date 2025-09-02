package domain.hops.model

import play.api.libs.json._

/**
 * Représente un houblon recommandé avec son score et ses explications
 * Réplique exactement RecommendedYeast pour les houblons
 */
case class RecommendedHop(
  hop: HopAggregate,
  score: Double,
  reason: String,
  tips: List[String]
) {
  def id: HopId = hop.id
  def name: String = hop.name.value
  def alphaAcids: Option[Double] = Some(hop.alphaAcid.value)
  def aromaProfiles: List[String] = hop.aromaProfile
}

object RecommendedHop {
  implicit val format: Format[RecommendedHop] = Json.format[RecommendedHop]
}

/**
 * Raisons pour chercher des alternatives à un houblon
 */
sealed trait AlternativeReason {
  def value: String
}

object AlternativeReason {
  case object Unavailable extends AlternativeReason { val value = "UNAVAILABLE" }
  case object TooExpensive extends AlternativeReason { val value = "TOO_EXPENSIVE" }
  case object Experiment extends AlternativeReason { val value = "EXPERIMENT" }
  
  def fromString(reason: String): Either[String, AlternativeReason] = reason.toUpperCase match {
    case "UNAVAILABLE" => Right(Unavailable)
    case "TOO_EXPENSIVE" => Right(TooExpensive)
    case "EXPERIMENT" => Right(Experiment)
    case _ => Left(s"Raison d'alternative inconnue: $reason")
  }
  
  implicit val format: Format[AlternativeReason] = new Format[AlternativeReason] {
    def reads(json: JsValue): JsResult[AlternativeReason] = json match {
      case JsString(value) => fromString(value) match {
        case Right(reason) => JsSuccess(reason)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(reason: AlternativeReason): JsValue = JsString(reason.value)
  }
}