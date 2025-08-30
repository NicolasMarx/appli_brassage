package actions

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import repositories.read.AdminReadRepository
import domain.admins._
import play.api.Logger

/** Requête typée contenant l'admin authentifié. */
final case class AdminRequest[A](admin: domain.admins.Admin, request: Request[A])
  extends WrappedRequest[A](request)

/** Protège les pages HTML de l'espace admin, redirige vers /admin/login si non connecté. */
@Singleton
final class AdminSecuredAction @Inject()(
                                          val parser: BodyParsers.Default,
                                          repo: AdminReadRepository
                                        )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AdminRequest, AnyContent]
    with ActionRefiner[Request, AdminRequest] {

  private val logger = Logger(this.getClass)
  private val SessionKey = "admin" // ID d'admin stocké en session

  override protected def refine[A](request: Request[A]): Future[Either[Result, AdminRequest[A]]] = {
    logger.debug(s"[AdminSecured] path=${request.path} sessionKeys=${request.session.data.keys.mkString(",")}")

    request.session.get(SessionKey) match {
      case Some(adminId) if adminId.nonEmpty =>
        repo.byId(AdminId(adminId)).map {
          case Some(admin) =>
            Right(AdminRequest(admin, request))
          case None =>
            Left(Results.Redirect("/admin/login").withNewSession)
        }
      case _ =>
        Future.successful(Left(Results.Redirect("/admin/login")))
    }
  }
}
