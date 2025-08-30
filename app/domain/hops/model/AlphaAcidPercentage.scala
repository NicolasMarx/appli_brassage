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
}