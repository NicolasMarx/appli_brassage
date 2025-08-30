package domain.hops.model

sealed trait HopSource
object HopSource {
  case object Manual extends HopSource
  case object AI_Discovery extends HopSource
  case object Import extends HopSource

  def fromString(str: String): HopSource = str.toUpperCase.trim match {
    case "MANUAL" => Manual
    case "AI_DISCOVERED" | "AI_DISCOVERY" => AI_Discovery
    case "IMPORT" => Import
    case _ => Manual
  }
}