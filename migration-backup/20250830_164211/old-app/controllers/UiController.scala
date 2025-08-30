package controllers

import javax.inject._
import play.api.mvc._
import actions.AdminSecuredAction
import play.api.Environment
import play.api.http.FileMimeTypes

import java.nio.file.Path
import scala.concurrent.ExecutionContext

@Singleton
final class UiController @Inject()(
                                    cc: ControllerComponents,
                                    env: Environment,
                                    secured: AdminSecuredAction
                                  ) extends AbstractController(cc) {

  // Fournit les implicites requis par sendFile
  private implicit val ec: ExecutionContext = cc.executionContext
  private implicit val fmts: FileMimeTypes  = cc.fileMimeTypes

  // Dossier racine UI (dans le repo)
  private val uiBase: java.nio.file.Path = env.rootPath.toPath.resolve("ui").normalize()

  private def safeResolve(rel: String): Option[java.nio.file.Path] = {
    val p    = uiBase.resolve(rel).normalize()
    val base = uiBase.toFile.getCanonicalPath
    if (p.toFile.exists && p.toFile.isFile && p.toFile.getCanonicalPath.startsWith(base)) Some(p) else None
  }

  private def serve(rel: String, asHtml: Boolean): Result =
    safeResolve(rel).map { p =>
      val mime =
        if (asHtml) "text/html; charset=utf-8"
        else fmts.forFileName(p.getFileName.toString).getOrElse("application/octet-stream")
      Ok.sendFile(p.toFile, inline = true).as(mime)
    }.getOrElse(NotFound(s"UI file not found: $rel"))

  // -------- Admin SPA protégée (server-side) --------
  def admin(): Action[AnyContent] =
    secured { _ => serve("admin/index.html", asHtml = true) }

  def adminDeep(path: String): Action[AnyContent] =
    secured { _ =>
      // SPA catch-all : renvoie index.html si le fichier demandé n'existe pas
      safeResolve(s"admin/$path").map(_ => serve(s"admin/$path", asHtml = false))
        .getOrElse(serve("admin/index.html", asHtml = true))
    }

  // -------- Login public --------
  def adminLogin(): Action[AnyContent] =
    Action { _ =>
      if (safeResolve("login/index.html").isDefined) serve("login/index.html", asHtml = true)
      else serve("admin/index.html", asHtml = true)
    }

  // -------- Front (optionnel) --------
  def front(): Action[AnyContent] =
    Action { _ =>
      if (safeResolve("front/index.html").isDefined) serve("front/index.html", asHtml = true)
      else NotFound("front UI not found (expected /ui/front/index.html)")
    }

  // -------- Assets /ui/* (js, css, images...) --------
  def asset(path: String): Action[AnyContent] =
    Action { _ => serve(path, asHtml = false) }
}
