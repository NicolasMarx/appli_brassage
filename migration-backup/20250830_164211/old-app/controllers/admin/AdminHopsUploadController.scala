package controllers.admin

import javax.inject._
import play.api.mvc._
import actions.AdminApiSecuredAction
import scala.util.Try
import java.nio.file.Files
import java.nio.charset.StandardCharsets

@Singleton
final class AdminHopsUploadController @Inject()(
                                                 cc: ControllerComponents,
                                                 secured: AdminApiSecuredAction
                                               ) extends AbstractController(cc) {

  /** POST /api/admin/hops/upload
   * Accepte:
   *  - text/csv brut
   *  - multipart/form-data (champ fichier quelconque)
   * Protégé par session admin (401 JSON si non auth).
   */
  def upload: Action[AnyContent] = secured { implicit req =>
    val body = req.body

    // text/csv brut
    val asText: Option[String] =
      body.asText.map(_.trim).filter(_.nonEmpty)

    // multipart -> lit le premier champ fichier non vide
    val asMultipart: Option[String] =
      body.asMultipartFormData
        .flatMap(_.files.headOption)
        .flatMap { f =>
          Try(new String(Files.readAllBytes(f.ref.path), StandardCharsets.UTF_8)).toOption
        }
        .map(_.trim)
        .filter(_.nonEmpty)

    val csv: Option[String] = asText.orElse(asMultipart)

    csv match {
      case Some(content) =>
        // TODO: brancher ici ton Application Service (côté WRITE) pour importer le CSV
        // ex: adminImportService.importHopsCsv(content)
        NoContent
      case None =>
        BadRequest("""{"error":"no_csv_content"}""").as("application/json")
    }
  }
}
