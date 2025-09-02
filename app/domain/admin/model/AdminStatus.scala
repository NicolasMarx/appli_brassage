package domain.admin.model

import domain.shared.ValueObject

/**
 * Value Object pour le statut des administrateurs
 * Gestion du cycle de vie des administrateurs dans le système
 */
sealed trait AdminStatus extends ValueObject {
  def name: String
  def description: String
  def isActive: Boolean
  def canLogin: Boolean
}

object AdminStatus {
  
  case object Active extends AdminStatus {
    val name = "ACTIVE"
    val description = "Administrateur actif"
    val isActive = true
    val canLogin = true
  }
  
  case object Inactive extends AdminStatus {
    val name = "INACTIVE" 
    val description = "Administrateur temporairement inactif"
    val isActive = false
    val canLogin = false
  }
  
  case object Suspended extends AdminStatus {
    val name = "SUSPENDED"
    val description = "Administrateur suspendu"
    val isActive = false
    val canLogin = false
  }
  
  case object PendingActivation extends AdminStatus {
    val name = "PENDING_ACTIVATION"
    val description = "En attente d'activation"
    val isActive = false
    val canLogin = false
  }
  
  val values: List[AdminStatus] = List(Active, Inactive, Suspended, PendingActivation)
  val activeStatuses: List[AdminStatus] = values.filter(_.isActive)
  val loginAllowedStatuses: List[AdminStatus] = values.filter(_.canLogin)
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[AdminStatus] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, AdminStatus] = {
    fromName(input.trim).toRight(s"Statut admin inconnu: $input")
  }
  
  /**
   * Statut par défaut pour nouveaux admins
   */
  val default: AdminStatus = PendingActivation
}