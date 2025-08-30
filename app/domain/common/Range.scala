package domain.common

final case class Range(min: Option[BigDecimal], max: Option[BigDecimal]) {
  def pretty: String = (min, max) match {
    case (Some(a), Some(b)) if a == b => s"$a"
    case (Some(a), Some(b))           => s"$a - $b"
    case (None, Some(b))              => s"< $b"
    case (Some(a), None)              => s">= $a"
    case _                            => ""
  }
}
