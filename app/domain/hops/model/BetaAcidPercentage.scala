package domain.hops.model

case class BetaAcidPercentage(value: Double) extends AnyVal

object BetaAcidPercentage {
  def apply(value: Double): BetaAcidPercentage = {
    require(value >= 0 && value <= 15, s"Beta acid percentage must be between 0 and 15, got: $value")
    new BetaAcidPercentage(value)
  }

  def create(value: Double): Either[String, BetaAcidPercentage] = {
    if (value >= 0 && value <= 15) Right(BetaAcidPercentage(value))
    else Left(s"Beta acid percentage must be between 0 and 15, got: $value")
  }
}