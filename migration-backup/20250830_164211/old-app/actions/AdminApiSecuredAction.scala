package actions

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import repositories.read.AdminReadRepository
import domain.admins._

/** Requête typée pour API JSON : renvoie 401 JSON si non auth (pas de redirect). */
final case class AdminApiRequest[A](admin: domain.admins.Admin, request: Request[A])
  extends WrappedRequest[A](request)

@Singleton
final class AdminApiSecuredAction @Inject()(
                                             val parser: BodyParsers.Default,
                                             repo: AdminReadRepository
                                           )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AdminApiRequest, AnyContent]
    with ActionRefiner[Request, AdminApiRequest] {

  private val SessionKey = "admin" // contient l'ID d'admin

  override protected def refine[A](request: Request[A]): Future[Either[Result, AdminApiRequest[A]]] = {
    request.session.get(SessionKey) match {
      case Some(adminId) if adminId.nonEmpty =>
        repo.byId(AdminId(adminId)).map {
          case Some(admin) => Right(AdminApiRequest(admin, request))
          case None => Left(Results.Unauthorized("""{"error":"unauthenticated"}""").as("application/json").withNewSession)
        }
      case _ =>
        Future.successful(Left(Results.Unauthorized("""{"error":"unauthenticated"}""").as("application/json")))
    }
  }
}
