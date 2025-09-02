package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour le statut des levures
 * Gestion du cycle de vie des levures dans le système
 */
sealed trait YeastStatus extends ValueObject {
  def name: String
  def description: String
  def isActive: Boolean
  def isVisible: Boolean
}

object YeastStatus {
  
  case object Active extends YeastStatus {
    val name = "ACTIVE"
    val description = "Levure active et disponible"
    val isActive = true
    val isVisible = true
  }
  
  case object Inactive extends YeastStatus {
    val name = "INACTIVE" 
    val description = "Levure temporairement indisponible"
    val isActive = false
    val isVisible = true
  }
  
  case object Discontinued extends YeastStatus {
    val name = "DISCONTINUED"
    val description = "Levure arrêtée par le laboratoire"
    val isActive = false
    val isVisible = true
  }
  
  case object Draft extends YeastStatus {
    val name = "DRAFT"
    val description = "Levure en cours de validation"
    val isActive = false
    val isVisible = false
  }
  
  case object Archived extends YeastStatus {
    val name = "ARCHIVED"
    val description = "Levure archivée (non visible)"
    val isActive = false
    val isVisible = false
  }
  
  val values: List[YeastStatus] = List(Active, Inactive, Discontinued, Draft, Archived)
  val activeStatuses: List[YeastStatus] = values.filter(_.isActive)
  val visibleStatuses: List[YeastStatus] = values.filter(_.isVisible)
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[YeastStatus] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, YeastStatus] = {
    fromName(input.trim).toRight(s"Statut inconnu: $input")
  }
  
  /**
   * Statut par défaut pour nouvelles levures
   */
  val default: YeastStatus = Draft
}
