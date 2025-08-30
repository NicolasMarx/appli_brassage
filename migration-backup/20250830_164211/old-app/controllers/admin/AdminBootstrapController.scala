package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import application.admin._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class AdminBootstrapController @Inject()(
                                                cc: ControllerComponents,
                                                svc: AdminBootstrapService
                                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private case class In(email: String, password: String)
  private implicit val inReads: Reads[In] = Json.reads[In]

  def bootstrap: Action[JsValue] = Action(parse.json).async { implicit req =>
    req.body.validate[In].fold(
      _ => Future.successful(BadRequest(Json.obj("error" -> "invalid_json"))),
      in => svc.bootstrap(in.email, in.password).map {
        case Right(admin) =>
          Created(Json.obj(
            "id"    -> admin.id.value,
            "email" -> admin.email.value   // <- le fix est ici
          ))
        case Left(BootstrapError.AlreadyInitialized) =>
          Forbidden(Json.obj("error" -> "already_initialized"))
        case Left(BootstrapError.InvalidInput(msg)) =>
          BadRequest(Json.obj("error" -> msg))
      }
    )
  }
}
