package repositories.read

import domain.hops.Hop
import scala.concurrent.Future

trait HopReadRepository {
  def all(): Future[Seq[Hop]]
}
