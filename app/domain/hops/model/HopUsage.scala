package domain.hops.model

sealed trait HopUsage
object HopUsage {
  case object Bittering extends HopUsage
  case object Aroma extends HopUsage
  case object DualPurpose extends HopUsage
  case object NobleHop extends HopUsage

  def fromString(str: String): HopUsage = str.toUpperCase.trim match {
    case "BITTERING" => Bittering
    case "AROMA" => Aroma
    case "DUAL_PURPOSE" | "DUAL-PURPOSE" => DualPurpose
    case "NOBLE" | "NOBLE_HOP" => NobleHop
    case _ => Aroma
  }
}