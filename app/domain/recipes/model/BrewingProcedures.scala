package domain.recipes.model

import play.api.libs.json._

case class BrewingProcedures(
  mashProfile: Option[MashProfile] = None,
  boilProcedure: Option[BoilProcedure] = None,
  fermentationProfile: Option[FermentationProfile] = None,
  packagingProcedure: Option[PackagingProcedure] = None
) {
  def hasBasicProcedures: Boolean = boilProcedure.isDefined
  
  // Boolean properties for compatibility with database schema
  def hasMashProfile: Boolean = mashProfile.isDefined
  def hasBoilProcedure: Boolean = boilProcedure.isDefined
  def hasFermentationProfile: Boolean = fermentationProfile.isDefined
  def hasPackagingProcedure: Boolean = packagingProcedure.isDefined
}

case class MashProfile(
  mashTemperature: Double, // Celsius
  mashTime: Int, // minutes
  mashType: MashType,
  steps: List[MashStep] = List.empty
)

case class MashStep(
  temperature: Double, // Celsius
  time: Int, // minutes
  stepType: String
)

case class BoilProcedure(
  boilTime: Int, // minutes
  hopAdditions: List[HopAddition] = List.empty
)

case class HopAddition(
  hopId: String,
  time: Int, // minutes from end of boil
  quantity: Double // grams
)

case class FermentationProfile(
  primaryTemp: Double, // Celsius
  primaryTime: Int, // days
  secondaryTemp: Option[Double] = None,
  secondaryTime: Option[Int] = None
)

case class PackagingProcedure(
  packagingType: PackagingType,
  carbonationLevel: Option[Double] = None,
  agingTime: Option[Int] = None // days
)

sealed trait MashType {
  def value: String
}

object MashType {
  case object SingleInfusion extends MashType { val value = "SINGLE_INFUSION" }
  case object StepMash extends MashType { val value = "STEP_MASH" }
  case object Decoction extends MashType { val value = "DECOCTION" }
  
  def fromString(mashType: String): Either[String, MashType] = mashType.toUpperCase match {
    case "SINGLE_INFUSION" => Right(SingleInfusion)
    case "STEP_MASH" => Right(StepMash)
    case "DECOCTION" => Right(Decoction)
    case _ => Left(s"Type d'empâtage non valide: $mashType")
  }

  implicit val format: Format[MashType] = new Format[MashType] {
    def reads(json: JsValue): JsResult[MashType] = json match {
      case JsString(value) => fromString(value) match {
        case Right(mashType) => JsSuccess(mashType)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(mashType: MashType): JsValue = JsString(mashType.value)
  }
}

sealed trait PackagingType {
  def value: String
}

object PackagingType {
  case object Bottle extends PackagingType { val value = "BOTTLE" }
  case object Keg extends PackagingType { val value = "KEG" }
  case object Cask extends PackagingType { val value = "CASK" }
  
  def fromString(packagingType: String): Either[String, PackagingType] = packagingType.toUpperCase match {
    case "BOTTLE" => Right(Bottle)
    case "KEG" => Right(Keg)
    case "CASK" => Right(Cask)
    case _ => Left(s"Type de conditionnement non valide: $packagingType")
  }

  implicit val format: Format[PackagingType] = new Format[PackagingType] {
    def reads(json: JsValue): JsResult[PackagingType] = json match {
      case JsString(value) => fromString(value) match {
        case Right(packagingType) => JsSuccess(packagingType)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(packagingType: PackagingType): JsValue = JsString(packagingType.value)
  }
}

object MashStep {
  implicit val format: Format[MashStep] = Json.format[MashStep]
}

object MashProfile {
  implicit val format: Format[MashProfile] = Json.format[MashProfile]
}

object HopAddition {
  implicit val format: Format[HopAddition] = Json.format[HopAddition]
}

object BoilProcedure {
  implicit val format: Format[BoilProcedure] = Json.format[BoilProcedure]
}

object FermentationProfile {
  implicit val format: Format[FermentationProfile] = Json.format[FermentationProfile]
}

object PackagingProcedure {
  implicit val format: Format[PackagingProcedure] = Json.format[PackagingProcedure]
}

object BrewingProcedures {
  def empty: BrewingProcedures = BrewingProcedures()
  
  implicit val format: Format[BrewingProcedures] = Json.format[BrewingProcedures]
}