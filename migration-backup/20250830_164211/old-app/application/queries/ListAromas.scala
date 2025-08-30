package application.queries

import domain.aromas.Aroma
import repositories.read.AromaReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final case class ListAromasQuery()

@Singleton
class ListAromasHandler @Inject() (repo: AromaReadRepository)(implicit ec: ExecutionContext) {
  def handle(q: ListAromasQuery): Future[Seq[Aroma]] = repo.all()
}
