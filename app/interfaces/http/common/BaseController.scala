package interfaces.http.common

import domain.common.DomainError
import play.api.libs.json.Json
import play.api.mvc.{BaseController => PlayBaseController, Result}
import play.api.mvc.Results._

trait BaseController extends PlayBaseController {
  
  def handleDomainError(error: DomainError): Result = {
    // Vérification du type d'erreur basée sur le code
    if (error.code.contains("NOT_FOUND")) {
      NotFound(Json.obj("error" -> error.message, "code" -> error.code))
    } else if (error.code.contains("BUSINESS_RULE")) {
      BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
    } else if (error.code.contains("VALIDATION")) {
      BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
    } else {
      InternalServerError(Json.obj("error" -> "Erreur interne"))
    }
  }
}
