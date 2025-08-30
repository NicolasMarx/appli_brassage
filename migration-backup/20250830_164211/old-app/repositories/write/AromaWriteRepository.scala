package repositories.write

import domain.aromas.Aroma
import scala.concurrent.Future

trait AromaWriteRepository {
  def replaceAll(aromas: Seq[Aroma]): Future[Unit]
}
