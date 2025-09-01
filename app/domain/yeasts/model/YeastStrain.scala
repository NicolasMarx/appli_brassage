package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les souches de levures
 * Référence unique de la souche (WLP001, S-04, etc.)
 */
case class YeastStrain(value: String) extends ValueObject {
  require(value != null && value.trim.nonEmpty, "La souche ne peut pas être vide")
  require(value.trim.length >= 1, "La souche doit contenir au moins 1 caractère")
  require(value.trim.length <= 20, "La souche ne peut pas dépasser 20 caractères")
  require(isValidFormat(value.trim), "Format de souche invalide")
  
  val normalized: String = value.trim.toUpperCase
  
  override def toString: String = normalized
  
  private def isValidFormat(strain: String): Boolean = {
    // Formats acceptés : WLP001, S-04, 1056, BE-134, etc.
    val pattern = "^[A-Z0-9-]{1,20}$".r
    pattern.matches(strain.toUpperCase)
  }
}

object YeastStrain {
  /**
   * Crée un YeastStrain avec validation
   */
  def fromString(strain: String): Either[String, YeastStrain] = {
    try {
      Right(YeastStrain(strain))
    } catch {
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Souches populaires pour suggestions
   */
  val popularStrains: List[String] = List(
    // White Labs
    "WLP001", "WLP002", "WLP004", "WLP005", "WLP007", "WLP029", "WLP099",
    // Wyeast
    "1056", "1084", "1098", "1272", "1318", "1469", "2565", "3068", "3724",
    // Fermentis
    "S-04", "S-05", "S-23", "W-34/70", "T-58", "BE-134", "BE-256",
    // Lallemand
    "VERDANT", "NOUVEAU", "VOSS", "CBC-1", "WILD-1"
  )
  
  /**
   * Validation sans création d'objet
   */
  def isValid(strain: String): Boolean = {
    try {
      YeastStrain(strain)
      true
    } catch {
      case _: IllegalArgumentException => false
    }
  }
}
