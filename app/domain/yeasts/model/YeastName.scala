package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour le nom des levures
 * Validation et normalisation des noms de levures
 */
case class YeastName(value: String) extends ValueObject {
  require(value != null && value.trim.nonEmpty, "Le nom de la levure ne peut pas être vide")
  require(value.trim.length >= 2, "Le nom de la levure doit contenir au moins 2 caractères")
  require(value.trim.length <= 100, "Le nom de la levure ne peut pas dépasser 100 caractères")
  
  // Normalisation : trim et suppression des espaces multiples
  val normalized: String = value.trim.replaceAll("\\s+", " ")
  
  override def toString: String = normalized
}

object YeastName {
  /**
   * Crée un YeastName avec validation
   */
  def fromString(name: String): Either[String, YeastName] = {
    try {
      Right(YeastName(name))
    } catch {
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Validation sans création d'objet
   */
  def isValid(name: String): Boolean = {
    name != null &&
    name.trim.nonEmpty &&
    name.trim.length >= 2 &&
    name.trim.length <= 100
  }
}
