package domain.hops.model

sealed trait HopStatus {
  def value: String
}

object HopStatus {
  case object Active extends HopStatus { val value = "ACTIVE" }
  case object Discontinued extends HopStatus { val value = "DISCONTINUED" }
  case object Limited extends HopStatus { val value = "LIMITED" }

  def fromString(str: String): HopStatus = str.toUpperCase.trim match {
    case "ACTIVE" => Active
    case "DISCONTINUED" => Discontinued
    case "LIMITED" => Limited
    case _ => Active
  }
  
  // JSON format support
  import play.api.libs.json._
  implicit val format: Format[HopStatus] = new Format[HopStatus] {
    def reads(json: JsValue): JsResult[HopStatus] = json match {
      case JsString(value) => JsSuccess(fromString(value))
      case _ => JsError("String expected")
    }
    
    def writes(status: HopStatus): JsValue = JsString(status.value)
  }
}