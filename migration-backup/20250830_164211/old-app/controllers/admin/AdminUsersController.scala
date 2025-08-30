package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import actions.AdminApiSecuredAction
import repositories.read.AdminReadRepository
import repositories.write.AdminWriteRepository
import domain.admins._
import services.PasswordService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class AdminUsersController @Inject()(
                                            cc: ControllerComponents,
                                            secured: AdminApiSecuredAction,
                                            readRepo: AdminReadRepository,
                                            writeRepo: AdminWriteRepository,
                                            passwordService: PasswordService
                                          )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // -------- LIST
  implicit val adminOutWrites: Writes[Admin] = new Writes[Admin] {
    def writes(a: Admin): JsValue = Json.obj(
      "id"        -> a.id.value,
      "email"     -> a.email.value,
      "firstName" -> a.name.first,
      "lastName"  -> a.name.last
    )
  }

  def list: Action[AnyContent] = secured.async { _ =>
    readRepo.all().map(as => Ok(Json.toJson(as)))
  }

  // -------- CREATE
  private case class CreateIn(email: String, firstName: String, lastName: String, password: String)
  private implicit val createReads: Reads[CreateIn] = Json.reads[CreateIn]

  def create: Action[JsValue] = secured(parse.json).async { implicit req =>
    req.body.validate[CreateIn].fold(
      _ => Future.successful(BadRequest(Json.obj("error" -> "invalid_json"))),
      in => {
        if (in.password.length < 8)
          Future.successful(BadRequest(Json.obj("error" -> "password_min_8")))
        else {
          val admin = Admin(
            id       = AdminId(java.util.UUID.randomUUID().toString),
            email    = Email(in.email.trim.toLowerCase),
            name     = AdminName(in.firstName.trim, in.lastName.trim),
            password = HashedPassword(passwordService.hash(in.password))
          )
          writeRepo.create(admin).map(_ => Created(Json.toJson(admin)))
        }
      }
    )
  }

  // -------- UPDATE (partiel: prénom/nom et/ou password)
  private case class UpdateIn(firstName: Option[String], lastName: Option[String], password: Option[String])
  private implicit val updateReads: Reads[UpdateIn] = Json.reads[UpdateIn]

  def update(id: String): Action[JsValue] = secured(parse.json).async { implicit req =>
    req.body.validate[UpdateIn].fold(
      _ => Future.successful(BadRequest(Json.obj("error" -> "invalid_json"))),
      in => {
        // ✅ Valider la longueur AVANT de construire newPwd (pas de `return` ici)
        if (in.password.exists(_.length < 8))
          Future.successful(BadRequest(Json.obj("error" -> "password_min_8")))
        else {
          readRepo.byId(AdminId(id)).flatMap {
            case Some(cur) =>
              val newName = AdminName(
                in.firstName.map(_.trim).getOrElse(cur.name.first),
                in.lastName.map(_.trim).getOrElse(cur.name.last)
              )
              val newPwd  = in.password match {
                case Some(p) if p.nonEmpty => HashedPassword(passwordService.hash(p))
                case _                     => cur.password
              }
              val updated = cur.copy(name = newName, password = newPwd)
              writeRepo.update(updated).map(_ => NoContent)

            case None => Future.successful(NotFound(Json.obj("error" -> "not_found")))
          }
        }
      }
    )
  }

  // -------- DELETE
  def delete(id: String): Action[AnyContent] = secured.async { _ =>
    writeRepo.delete(AdminId(id)).map(_ => NoContent)
  }
}
