package domain.hops.model

sealed trait HopUsage {
  def value: String
}

object HopUsage {
  case object Bittering extends HopUsage { val value = "BITTERING" }
  case object Aroma extends HopUsage { val value = "AROMA" }
  case object DualPurpose extends HopUsage { val value = "DUAL_PURPOSE" }
  case object NobleHop extends HopUsage { val value = "NOBLE_HOP" }

  def fromString(str: String): Either[String, HopUsage] = str.toUpperCase.trim match {
    case "BITTERING" => Right(Bittering)
    case "AROMA" => Right(Aroma)
    case "DUAL_PURPOSE" | "DUAL-PURPOSE" => Right(DualPurpose)
    case "NOBLE" | "NOBLE_HOP" => Right(NobleHop)
    case _ => Left(s"Unknown hop usage: $str")
  }
  
  // JSON Support
  import play.api.libs.json._
  implicit val format: Format[HopUsage] = new Format[HopUsage] {
    def reads(json: JsValue): JsResult[HopUsage] = json match {
      case JsString(value) => fromString(value) match {
        case Right(usage) => JsSuccess(usage)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String expected")
    }
    
    def writes(usage: HopUsage): JsValue = JsString(usage.value)
  }
}