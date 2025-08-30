package repositories.read

import domain.aromas.Aroma
import scala.concurrent.Future

trait AromaReadRepository {
  def all(): Future[Seq[Aroma]]
}
