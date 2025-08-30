package repositories.read

import domain.hops.HopId
import scala.concurrent.Future

final case class HopExtras(
                            description: Option[String],
                            hopType:     Option[String],
                            beerStyles:  List[String],
                            substitutes: List[String]
                          )

trait HopExtrasReadRepository {
  def findByHopIds(ids: Seq[HopId]): Future[Map[HopId, HopExtras]]
}
