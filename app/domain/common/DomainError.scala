package domain.common

/**
 * Hiérarchie d'erreurs métier pour architecture DDD
 */
sealed trait DomainError {
  def message: String
  def code: String
}

// Erreurs de validation
case class ValidationError(message: String, field: Option[String] = None) extends DomainError {
  val code: String = "VALIDATION_ERROR"
}

// Erreurs de business rules
case class BusinessRuleViolation(message: String, ruleName: String) extends DomainError {
  val code: String = "BUSINESS_RULE_VIOLATION"
}

// Erreurs de ressources non trouvées
case class NotFoundError(resourceType: String, identifier: String) extends DomainError {
  val message: String = s"$resourceType avec l'identifiant '$identifier' non trouvé"
  val code: String = "NOT_FOUND"
}

// Erreurs de conflit
case class ConflictError(message: String, conflictingField: String) extends DomainError {
  val code: String = "CONFLICT"
}

// Erreurs d'autorisation
case class AuthorizationError(message: String, requiredPermission: Option[String] = None) extends DomainError {
  val code: String = "AUTHORIZATION_ERROR"
}

// Erreurs d'authentification
case class AuthenticationError(message: String) extends DomainError {
  val code: String = "AUTHENTICATION_ERROR"
}

// Erreurs techniques (exceptions, BDD, etc.)
case class TechnicalError(message: String, cause: Option[Throwable] = None) extends DomainError {
  val code: String = "TECHNICAL_ERROR"
}

object DomainError {
  def validation(message: String, field: String = null): ValidationError =
    ValidationError(message, Option(field))
  
  def businessRule(message: String, ruleName: String): BusinessRuleViolation =
    BusinessRuleViolation(message, ruleName)
  
  def notFound(resourceType: String, identifier: String): NotFoundError =
    NotFoundError(resourceType, identifier)
  
  def conflict(message: String, field: String): ConflictError =
    ConflictError(message, field)
  
  def unauthorized(message: String, permission: String = null): AuthorizationError =
    AuthorizationError(message, Option(permission))
  
  def unauthenticated(message: String): AuthenticationError =
    AuthenticationError(message)
  
  // Méthode manquante ajoutée !
  def technical(message: String, cause: Throwable = null): TechnicalError =
    TechnicalError(message, Option(cause))
}
