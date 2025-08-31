package domain.common

/**
 * Hiérarchie des erreurs domaine
 */
sealed trait DomainError {
  def message: String
}

case class ValidationError(message: String) extends DomainError
case class NotFoundError(resourceType: String, identifier: String) extends DomainError {
  def message: String = s"$resourceType avec l'identifiant $identifier non trouvé"
}
case class TechnicalError(message: String) extends DomainError
case class BusinessRuleError(message: String) extends DomainError

object DomainError {
  
  def validation(message: String): ValidationError = ValidationError(message)
  
  def notFound(resourceType: String, identifier: String): NotFoundError = 
    NotFoundError(resourceType, identifier)
  
  def notFound(message: String): ValidationError = ValidationError(message)
    
  def technical(message: String): TechnicalError = TechnicalError(message)
  
  def businessRule(message: String): BusinessRuleError = BusinessRuleError(message)
}
