package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import repositories.read.AdminReadRepository
import domain.admins._
import services.PasswordService
import scala.concurrent.{ExecutionContext, Future}

/** Auth JSON : login (pose session) / logout (purge session). */
@Singleton
final class AdminAuthController @Inject()(
                                           cc: ControllerComponents,
                                           readRepo: AdminReadRepository,
                                           passwordService: PasswordService
                                         )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val SessionKey = "admin"

  private case class LoginIn(email: String, password: String)
  private implicit val loginReads: Reads[LoginIn] = Json.reads[LoginIn]

  /** POST /api/admin/login */
  def login: Action[JsValue] = Action(parse.json).async { implicit req =>
    req.body.validate[LoginIn].fold(
      _ => Future.successful(BadRequest(Json.obj("error" -> "invalid_json"))),
      in => {
        val email = Email(in.email.trim.toLowerCase)
        readRepo.byEmail(email).map {
          case Some(admin) if passwordService.verify(in.password, admin.password.value) =>
            val adminJson = Json.obj(
              "id"        -> admin.id.value,
              "email"     -> admin.email.value,
              "firstName" -> admin.name.first,
              "lastName"  -> admin.name.last
            )
            val token = admin.id.value // <-- ton front attend un Token (String)
            Ok(adminJson ++ Json.obj("admin" -> adminJson, "token" -> token))
              .withSession(req.session + (SessionKey -> admin.id.value))

          case _ =>
            Unauthorized(Json.obj("error" -> "invalid_credentials"))
        }
      }
    )
  }

  /** POST /api/admin/logout */
  def logout: Action[AnyContent] =
    Action { _ => NoContent.withNewSession }
}
