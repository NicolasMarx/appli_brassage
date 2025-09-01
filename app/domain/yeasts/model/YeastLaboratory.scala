package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les laboratoires de levures
 * Énumération des principaux laboratoires de levures brassicoles
 */
sealed trait YeastLaboratory extends ValueObject {
  def name: String
  def code: String
  def description: String
}

object YeastLaboratory {
  
  case object WhiteLabs extends YeastLaboratory {
    val name = "White Labs"
    val code = "WLP"
    val description = "Laboratoire californien spécialisé en levures liquides"
  }
  
  case object Wyeast extends YeastLaboratory {
    val name = "Wyeast Laboratories"
    val code = "WY"
    val description = "Laboratoire de l'Oregon, pionnier des levures liquides"
  }
  
  case object Lallemand extends YeastLaboratory {
    val name = "Lallemand Brewing"
    val code = "LB"
    val description = "Groupe français, levures sèches haute qualité"
  }
  
  case object Fermentis extends YeastLaboratory {
    val name = "Fermentis"
    val code = "F"
    val description = "Division levures sèches de Lesaffre"
  }
  
  case object ImperialYeast extends YeastLaboratory {
    val name = "Imperial Yeast"
    val code = "I"
    val description = "Laboratoire américain innovant"
  }
  
  case object Mangrove extends YeastLaboratory {
    val name = "Mangrove Jack's"
    val code = "MJ"
    val description = "Laboratoire néo-zélandais"
  }
  
  case object Other extends YeastLaboratory {
    val name = "Other"
    val code = "OTH"
    val description = "Autres laboratoires"
  }
  
  val values: List[YeastLaboratory] = List(
    WhiteLabs, Wyeast, Lallemand, Fermentis, 
    ImperialYeast, Mangrove, Other
  )
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[YeastLaboratory] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Recherche par code
   */
  def fromCode(code: String): Option[YeastLaboratory] = {
    values.find(_.code.equalsIgnoreCase(code.trim))
  }
  
  /**
   * Parsing flexible
   */
  def parse(input: String): Either[String, YeastLaboratory] = {
    val trimmed = input.trim
    fromName(trimmed)
      .orElse(fromCode(trimmed))
      .toRight(s"Laboratoire inconnu: $input")
  }
}
