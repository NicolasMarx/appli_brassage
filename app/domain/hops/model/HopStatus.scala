package domain.hops.model

sealed trait HopStatus
object HopStatus {
  case object Active extends HopStatus
  case object Discontinued extends HopStatus
  case object Limited extends HopStatus

  def fromString(str: String): HopStatus = str.toUpperCase.trim match {
    case "ACTIVE" => Active
    case "DISCONTINUED" => Discontinued
    case "LIMITED" => Limited
    case _ => Active
  }
}