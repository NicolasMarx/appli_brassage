package application.Admin

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.nio.charset.{Charset, StandardCharsets}

/**
 * Implémentation simple et robuste de l'import CSV pour les houblons.
 *
 * - Décode en UTF-8 (erreur contrôlée si échec)
 * - Valide un minimum la structure (au moins une ligne de données après l'entête)
 * - Calcule un rapport (imported/skipped)
 *
 * NOTE:
 * Cette implémentation ne persiste pas encore les données.
 * Si tu veux déclencher une écriture atomique (CQRS/DDD côté write model),
 * injecte ici un service de commande ou un repository write et compose un DBIO.transactionally.
 */
@Singleton
class HopsCsvImportServiceImpl @Inject()(
                                          // Exemple d’injection si tu veux brancher plus tard la persistance:
                                          // hopWriteRepo: infrastructure.db.HopWriteRepositoriesDb
                                        )(
                                          implicit ec: ExecutionContext
                                        ) extends HopsCsvImportService {

  private val Utf8: Charset = StandardCharsets.UTF_8

  override def importCsv(
                          filename: String,
                          contentType: Option[String],
                          bytes: Array[Byte]
                        ): Future[Either[ImportError, ImportReport]] = {

    // 1) Décodage UTF-8 sûr
    val decodedEither: Either[ImportError, String] =
      try Right(new String(bytes, Utf8))
      catch {
        case e: Throwable =>
          Left(InvalidFormat(s"Impossible de décoder le fichier en UTF-8: ${e.getMessage}"))
      }

    decodedEither match {
      case Left(err) => Future.successful(Left(err))

      case Right(text) =>
        // 2) Validation de base du contenu
        val rawLines = splitLines(text).map(_.trim)
        val lines    = rawLines.filterNot(_.isEmpty)

        if (lines.isEmpty) {
          return Future.successful(Left(InvalidFormat("Fichier vide")))
        }

        // Supposons une première ligne d'entête
        val dataLines = if (lines.size >= 2) lines.tail else Nil
        if (dataLines.isEmpty) {
          return Future.successful(Left(InvalidFormat("Aucune donnée (entête seule)")))
        }

        // 3) (Facultatif) Heuristique minimale CSV : au moins 1 séparateur sur les lignes de données
        val sep = detectSeparator(lines.head)
        val parsed =
          if (sep.isDefined) dataLines.map(_.split(sep.get, -1).toList)
          else dataLines.map(l => List(l)) // fallback : une seule colonne

        // 4) Ici tu pourrais mapper vers ton domaine puis persister de manière atomique.
        //    Exemple (à implémenter si tu veux persister) :
        //    val tx = DBIO.seq(
        //      hopWriteRepo.replaceHopsAction(mappedHops),
        //      hopWriteRepo.replaceDetailsAction(mappedDetails),
        //      ...
        //    ).transactionally
        //    db.run(tx).map(_ => Right(ImportReport(imported = parsed.size, skipped = 0)))

        // 5) Pour l’instant, on renvoie juste le rapport d’import "compté"
        val imported = parsed.size
        val skipped  = 0

        Future.successful(Right(ImportReport(imported = imported, skipped = skipped)))
    }
  }

  // --- Utils ------------------------------------------------------------------------------------

  /** Sépare de manière fiable les lignes (CRLF, LF, CR) */
  private def splitLines(s: String): List[String] =
    s.split("\r\n|\n|\r", -1).toList

  /** Détecte un séparateur probable sur la ligne d'entête */
  private def detectSeparator(header: String): Option[String] = {
    val candidates = List(",", ";", "\t", "|", ":")
    candidates.find(sep => header.contains(sep))
  }
}
