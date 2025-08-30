package repositories.write

import domain.hops.HopId
import repositories.read.HopExtras
import scala.concurrent.Future

trait HopExtrasWriteRepository {
  def replaceAll(extras: Seq[(HopId, HopExtras)]): Future[Unit]
}
