package interfaces.http.common

import domain.common._
import play.api.libs.json._
import play.api.mvc._

/**
 * Contrôleur de base avec gestion d'erreurs DDD standardisée
 */
abstract class BaseController(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Convertit une DomainError en réponse HTTP appropriée
   */
  protected def handleDomainError(error: DomainError): Result = {
    error match {
      case _: NotFoundError =>
        NotFound(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: BusinessRuleViolation =>
        BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: ValidationError =>
        BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: ConflictError =>
        Conflict(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: AuthorizationError =>
        Forbidden(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: AuthenticationError =>
        Unauthorized(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: TechnicalError =>
        InternalServerError(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _ =>
        InternalServerError(Json.obj("error" -> "Une erreur interne s'est produite", "code" -> "INTERNAL_ERROR"))
    }
  }

  /**
   * Gestion standardisée des Either[DomainError, T]
   */
  protected def handleResult[T](result: Either[DomainError, T])(onSuccess: T => Result): Result = {
    result match {
      case Left(error) => handleDomainError(error)
      case Right(value) => onSuccess(value)
    }
  }
}
