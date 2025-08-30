package repositories.read

import domain.beerstyles.BeerStyle
import scala.concurrent.Future

trait BeerStyleReadRepository {
  def all(): Future[Seq[BeerStyle]]
}
