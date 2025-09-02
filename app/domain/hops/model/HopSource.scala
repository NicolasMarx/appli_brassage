package domain.hops.model

sealed trait HopSource {
  def value: String
}

object HopSource {
  case object Manual extends HopSource { val value = "MANUAL" }
  case object AI_Discovery extends HopSource { val value = "AI_DISCOVERY" }
  case object Import extends HopSource { val value = "IMPORT" }

  def fromString(str: String): HopSource = str.toUpperCase.trim match {
    case "MANUAL" => Manual
    case "AI_DISCOVERED" | "AI_DISCOVERY" => AI_Discovery
    case "IMPORT" => Import
    case _ => Manual
  }
  
  // JSON format support
  import play.api.libs.json._
  implicit val format: Format[HopSource] = new Format[HopSource] {
    def reads(json: JsValue): JsResult[HopSource] = json match {
      case JsString(value) => JsSuccess(fromString(value))
      case _ => JsError("String expected")
    }
    
    def writes(source: HopSource): JsValue = JsString(source.value)
  }
}