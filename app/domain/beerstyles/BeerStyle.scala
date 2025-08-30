package domain.beerstyles

final case class BeerStyleId(value: String) extends AnyVal
final case class BeerStyleName(value: String) extends AnyVal

final case class BeerStyle(id: BeerStyleId, name: BeerStyleName)
