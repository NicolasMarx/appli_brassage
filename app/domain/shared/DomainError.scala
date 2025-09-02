package domain.shared

sealed trait DomainError {
  def message: String
}

object DomainError {
  case class ValidationError(message: String) extends DomainError
  case class BusinessRuleViolation(message: String) extends DomainError  
  case class InvalidState(message: String) extends DomainError
  case class NotFound(message: String) extends DomainError
  case class Unauthorized(message: String) extends DomainError
}
