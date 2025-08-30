package controllers.admin

import javax.inject._
import play.api.mvc._
import actions.AdminSecuredAction

/** Sert l'UI admin (HTML/JS/CSS). index & deep-links PROTÉGÉS ; login PUBLIC. */
@Singleton
final class AdminPageController @Inject()(
                                           cc: ControllerComponents,
                                           secured: AdminSecuredAction
                                         ) extends AbstractController(cc) {

  /** /admin (protégé) */
  def index: Action[AnyContent] =
    secured { implicit req: Request[AnyContent] =>
      Ok(views.html.admin())
    }

/** /admin/path (protégé) — deep-links de la SPA renvoient le même index.html */
 def spa(path: String): Action[AnyContent] =
    secured { implicit req: Request[AnyContent] =>
 Ok(views.html.admin())
 }

 /** /admin/login (public) — sert la même SPA ; le front affiche l’écran de login */
 def login: Action[AnyContent] =
    Action { implicit req: Request[AnyContent] =>
 Ok(views.html.admin())
 }

 // --------- Assets (servis depuis /public/admin/*) ---------

 def appJs: Action[AnyContent] = Action {
 loadText("public/admin/admin.js")
 .map(js => Ok(js).as("application/javascript"))
 .getOrElse(NotFound("admin.js not found"))
 }

 def bootJs: Action[AnyContent] = Action {
 loadText("public/admin/boot.js")
 .map(js => Ok(js).as("application/javascript"))
 .getOrElse(NotFound("boot.js not found"))
 }

 def css: Action[AnyContent] = Action {
 loadText("public/admin/admin.css")
 .map(css => Ok(css).as("text/css"))
 .getOrElse(NotFound("admin.css not found"))
 }

 // --------- Helpers ---------
 private def loadText(path: String): Option[String] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).map { is =>
 val src = scala.io.Source.fromInputStream(is, "UTF-8")
 try src.mkString finally src.close()
 }
 }
