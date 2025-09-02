package domain.recipes.model

import play.api.libs.json._

case class BatchSize private(
  value: Double,
  unit: VolumeUnit
) {
  def toLiters: Double = unit match {
    case VolumeUnit.Liters => value
    case VolumeUnit.Gallons => value * 3.78541
    case VolumeUnit.Milliliters => value / 1000.0
  }
  
  override def toString: String = s"$value ${unit.symbol}"
}

object BatchSize {
  def create(value: Double, unit: VolumeUnit): Either[String, BatchSize] = {
    if (value <= 0) Left("Le volume doit être positif")
    else if (value > 10000) Left("Volume trop important (max 10000L)")
    else Right(BatchSize(value, unit))
  }

  implicit val format: Format[BatchSize] = Json.format[BatchSize]
}

sealed trait VolumeUnit {
  def symbol: String
  def value: String
  def name: String = value
}

object VolumeUnit {
  case object Liters extends VolumeUnit { 
    val symbol = "L"
    val value = "LITERS"
  }
  case object Gallons extends VolumeUnit { 
    val symbol = "gal"
    val value = "GALLONS"
  }
  case object Milliliters extends VolumeUnit { 
    val symbol = "mL"
    val value = "MILLILITERS"
  }
  
  def fromString(unit: String): Either[String, VolumeUnit] = unit.toUpperCase match {
    case "LITERS" | "L" => Right(Liters)
    case "GALLONS" | "GAL" => Right(Gallons)
    case "MILLILITERS" | "ML" => Right(Milliliters)
    case _ => Left(s"Unité de volume non valide: $unit")
  }

  def fromName(name: String): Option[VolumeUnit] = fromString(name).toOption

  implicit val format: Format[VolumeUnit] = new Format[VolumeUnit] {
    def reads(json: JsValue): JsResult[VolumeUnit] = json match {
      case JsString(value) => fromString(value) match {
        case Right(unit) => JsSuccess(unit)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(unit: VolumeUnit): JsValue = JsString(unit.value)
  }
}