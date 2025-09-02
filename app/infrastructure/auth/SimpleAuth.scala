package infrastructure.auth

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import java.util.Base64

/**
 * AUTHENTIFICATION ULTRA SIMPLE - Version production minimaliste
 * Juste ce qu'il faut pour sécuriser les endpoints admin
 */
@Singleton
class SimpleAuth @Inject()(parsers: BodyParsers.Default)(implicit ec: ExecutionContext) {

  private val credentials = Map(
    "admin" -> "brewing2024",
    "editor" -> "ingredients2024"
  )

  def check(request: RequestHeader): Boolean = {
    request.headers.get("Authorization") match {
      case Some(auth) if auth.startsWith("Basic ") =>
        try {
          val encoded = auth.substring(6)
          val decoded = new String(Base64.getDecoder.decode(encoded), "UTF-8")
          val parts = decoded.split(":", 2)
          if (parts.length == 2) {
            credentials.get(parts(0)).contains(parts(1))
          } else false
        } catch {
          case _: Exception => false
        }
      case _ => false
    }
  }

  def secure[A](action: Action[A]): Action[A] = new Action[A] {
    def parser = action.parser
    def executionContext = ec
    def apply(request: Request[A]) = {
      if (check(request)) {
        action(request)
      } else {
        Future.successful(Results.Unauthorized("Auth required"))
      }
    }
  }
}