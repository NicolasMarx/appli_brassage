package repositories.write

import domain.beerstyles.BeerStyle
import scala.concurrent.Future

trait BeerStyleWriteRepository {
  def replaceAll(styles: Seq[BeerStyle]): Future[Unit]
}
