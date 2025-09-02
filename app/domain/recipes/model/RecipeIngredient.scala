package domain.recipes.model

import play.api.libs.json._
import domain.hops.model.HopId
import domain.malts.model.MaltId
import domain.yeasts.model.YeastId

sealed trait RecipeIngredient {
  def id: String
  def quantity: Double
}

case class RecipeHop(
  hopId: HopId,
  quantityGrams: Double,
  additionTime: Int, // minutes from end of boil
  usage: HopUsage
) extends RecipeIngredient {
  def id: String = hopId.value.toString
  def quantity: Double = quantityGrams
}

case class RecipeMalt(
  maltId: MaltId,
  quantityKg: Double,
  percentage: Option[Double] = None
) extends RecipeIngredient {
  def id: String = maltId.value.toString
  def quantity: Double = quantityKg
}

case class RecipeYeast(
  yeastId: YeastId,
  quantityGrams: Double,
  starterSize: Option[Double] = None
) extends RecipeIngredient {
  def id: String = yeastId.value.toString
  def quantity: Double = quantityGrams
}

case class OtherIngredient(
  name: String,
  quantityGrams: Double,
  additionTime: Int,
  ingredientType: OtherIngredientType
) extends RecipeIngredient {
  def id: String = name
  def quantity: Double = quantityGrams
}

sealed trait HopUsage {
  def value: String
  def name: String = value
}

object HopUsage {
  case object Boil extends HopUsage { val value = "BOIL" }
  case object Aroma extends HopUsage { val value = "AROMA" }
  case object DryHop extends HopUsage { val value = "DRY_HOP" }
  case object Whirlpool extends HopUsage { val value = "WHIRLPOOL" }
  
  def fromString(usage: String): Either[String, HopUsage] = usage.toUpperCase match {
    case "BOIL" => Right(Boil)
    case "AROMA" => Right(Aroma)
    case "DRY_HOP" => Right(DryHop)
    case "WHIRLPOOL" => Right(Whirlpool)
    case _ => Left(s"Usage de houblon non valide: $usage")
  }
  
  def fromName(name: String): Option[HopUsage] = fromString(name).toOption

  implicit val format: Format[HopUsage] = new Format[HopUsage] {
    def reads(json: JsValue): JsResult[HopUsage] = json match {
      case JsString(value) => fromString(value) match {
        case Right(usage) => JsSuccess(usage)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(usage: HopUsage): JsValue = JsString(usage.value)
  }
}

sealed trait OtherIngredientType {
  def value: String
}

object OtherIngredientType {
  case object Spice extends OtherIngredientType { val value = "SPICE" }
  case object Fruit extends OtherIngredientType { val value = "FRUIT" }
  case object Sugar extends OtherIngredientType { val value = "SUGAR" }
  case object Other extends OtherIngredientType { val value = "OTHER" }
  
  def fromString(ingredientType: String): Either[String, OtherIngredientType] = ingredientType.toUpperCase match {
    case "SPICE" => Right(Spice)
    case "FRUIT" => Right(Fruit)
    case "SUGAR" => Right(Sugar)
    case "OTHER" => Right(Other)
    case _ => Left(s"Type d'ingrédient non valide: $ingredientType")
  }

  implicit val format: Format[OtherIngredientType] = new Format[OtherIngredientType] {
    def reads(json: JsValue): JsResult[OtherIngredientType] = json match {
      case JsString(value) => fromString(value) match {
        case Right(ingredientType) => JsSuccess(ingredientType)
        case Left(error) => JsError(error)
      }
      case _ => JsError("String attendu")
    }
    
    def writes(ingredientType: OtherIngredientType): JsValue = JsString(ingredientType.value)
  }
}

object RecipeHop {
  implicit val format: Format[RecipeHop] = Json.format[RecipeHop]
}

object RecipeMalt {
  implicit val format: Format[RecipeMalt] = Json.format[RecipeMalt]
}

object RecipeYeast {
  implicit val format: Format[RecipeYeast] = Json.format[RecipeYeast]
}

object OtherIngredient {
  implicit val format: Format[OtherIngredient] = Json.format[OtherIngredient]
}