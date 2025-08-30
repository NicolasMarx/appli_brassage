package application.queries

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import domain.beerstyles.BeerStyle
import repositories.read.BeerStyleReadRepository

final case class ListBeerStylesQuery()

@Singleton
class ListBeerStylesHandler @Inject() (repo: BeerStyleReadRepository)(implicit ec: ExecutionContext) {
  def handle(q: ListBeerStylesQuery): Future[Seq[BeerStyle]] = repo.all()
}
