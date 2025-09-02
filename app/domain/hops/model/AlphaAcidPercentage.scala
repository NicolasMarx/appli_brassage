package domain.hops.model

case class AlphaAcidPercentage(value: Double) extends AnyVal

object AlphaAcidPercentage {
  def apply(value: Double): AlphaAcidPercentage = {
    require(value >= 0 && value <= 30, s"Alpha acid percentage must be between 0 and 30, got: $value")
    new AlphaAcidPercentage(value)
  }

  def create(value: Double): Either[String, AlphaAcidPercentage] = {
    if (value >= 0 && value <= 30) Right(AlphaAcidPercentage(value))
    else Left(s"Alpha acid percentage must be between 0 and 30, got: $value")
  }
  
  // JSON format support
  import play.api.libs.json._
  implicit val format: Format[AlphaAcidPercentage] = new Format[AlphaAcidPercentage] {
    def reads(json: JsValue): JsResult[AlphaAcidPercentage] = json match {
      case JsNumber(value) => create(value.toDouble) match {
        case Right(percentage) => JsSuccess(percentage)
        case Left(error) => JsError(error)
      }
      case _ => JsError("Number expected")
    }
    
    def writes(percentage: AlphaAcidPercentage): JsValue = JsNumber(percentage.value)
  }
}