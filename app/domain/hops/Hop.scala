package domain.hops

import domain.common.Range

final case class HopId(value: String) extends AnyVal
final case class HopName(value: String) extends AnyVal
final case class Country(value: String) extends AnyVal

final case class Hop(
  id: HopId,
  name: HopName,
  country: Country,
  alphaAcid: Range,
  betaAcid: Range,
  cohumulone: Range,
  totalOilsMlPer100g: Range,
  myrcene: Range,
  humulene: Range,
  caryophyllene: Range,
  farnesene: Range,
  aromas: List[String]
)
final case class HopDetail(
                            hopId: HopId,
                            description: Option[String],
                            hopType:    Option[String]
                          )