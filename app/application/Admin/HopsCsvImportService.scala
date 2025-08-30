package application.Admin

import scala.concurrent.Future

// Résultats d'import
sealed trait ImportError { def message: String }
final case class InvalidFormat(message: String) extends ImportError
final case class ProcessingFailed(message: String) extends ImportError

final case class ImportReport(imported: Int, skipped: Int)

trait HopsCsvImportService {
  /**
   * @param filename    nom de fichier (indicatif/log)
   * @param contentType content-type reçu
   * @param bytes       contenu du CSV
   */
  def importCsv(
                 filename: String,
                 contentType: Option[String],
                 bytes: Array[Byte]
               ): Future[Either[ImportError, ImportReport]]
}
