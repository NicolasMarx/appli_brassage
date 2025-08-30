package domain.aromas

final case class AromaId(value: String) extends AnyVal
final case class AromaName(value: String) extends AnyVal

final case class Aroma(id: AromaId, name: AromaName)
