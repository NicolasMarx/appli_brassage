package controllers.admin

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class AdminAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilder[Request, AnyContent] with ActionFilter[Request] {

  override def executionContext: ExecutionContext = ec
  override def parser: BodyParser[AnyContent] = parser

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful {
    request.session.get("admin") match {
      case Some("true") => None
      case _ => Some(Results.Unauthorized("""{"error":"admin.unauthorized"}""").as("application/json"))
    }
  }
}
