package infrastructure.auth

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.JsValue
import scala.concurrent.{ExecutionContext, Future}
import java.util.Base64

/**
 * Action pour authentification basique des endpoints admin
 * Implémente Basic Authentication selon les principes du projet
 */
case class AuthenticatedRequest[A](user: AdminUser, request: Request[A]) extends WrappedRequest[A](request)

case class AdminUser(username: String, permissions: Set[String] = Set("admin"))

@Singleton
class AuthAction @Inject()(val parsers: BodyParsers.Default)(implicit ec: ExecutionContext) 
  extends ActionBuilder[AuthenticatedRequest, AnyContent] 
  with ActionRefiner[Request, AuthenticatedRequest] {

  override def executionContext: ExecutionContext = ec
  override def parser: BodyParser[AnyContent] = parsers

  // Configuration basique - en production, utiliser une vraie base de données
  private val validCredentials = Map(
    "admin" -> "brewing2024",
    "editor" -> "ingredients2024"
  )

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Basic ") =>
        try {
          val encodedCredentials = authHeader.substring(6)
          val credentials = new String(Base64.getDecoder.decode(encodedCredentials), "UTF-8")
          val Array(username, password) = credentials.split(":", 2)
          
          validCredentials.get(username) match {
            case Some(validPassword) if validPassword == password =>
              val user = AdminUser(username)
              Future.successful(Right(AuthenticatedRequest(user, request)))
            case _ =>
              Future.successful(Left(Results.Unauthorized("Invalid credentials")))
          }
        } catch {
          case _: Exception =>
            Future.successful(Left(Results.BadRequest("Invalid Authorization header format")))
        }
      case _ =>
        Future.successful(Left(Results.Unauthorized("Authorization required")))
    }
  }
}

/**
 * Action optionnelle pour endpoints qui peuvent bénéficier d'authentification
 * mais ne la requièrent pas (analytics, logs, etc.)
 */
@Singleton  
class OptionalAuthAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilder[Request, AnyContent]
  with ActionTransformer[Request, Request] {

  override def executionContext: ExecutionContext = ec
  override def parser: BodyParser[AnyContent] = parser

  override protected def transform[A](request: Request[A]): Future[Request[A]] = {
    // Simplement retourner la requête inchangée
    // En production, on pourrait ajouter des headers d'audit via un wrapper
    Future.successful(request)
  }
}