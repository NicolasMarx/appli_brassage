package application.queries

import domain.hops.Hop
import repositories.read.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final case class ListHopsQuery()

@Singleton
class ListHopsHandler @Inject() (repo: HopReadRepository)(implicit ec: ExecutionContext) {
  def handle(q: ListHopsQuery): Future[Seq[Hop]] = repo.all()
}
