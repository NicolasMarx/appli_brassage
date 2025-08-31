package interfaces.actions

import play.api.mvc._
import play.api.libs.json.Json

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminSecuredAction @Inject()(
  override val parser: BodyParsers.Default
)(implicit ec: ExecutionContext) extends ActionBuilder[Request, AnyContent] {

  override def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    // Implémentation temporaire - TODO: implémenter vraie sécurité admin
    block(request)
  }
}
