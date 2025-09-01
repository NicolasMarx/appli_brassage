package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltListQuery, MaltListResponse}
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.MaltReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la requête de liste des malts (API publique)
 */
@Singleton
class MaltListQueryHandler @Inject()(
  maltRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltListQuery): Future[MaltListResponse] = {
    for {
      malts <- maltRepository.findAll(query.page, query.pageSize, activeOnly = true)
      count <- maltRepository.count(activeOnly = true)
    } yield {
      val readModels = malts.map(MaltReadModel.fromAggregate)
      MaltListResponse(readModels, count, query.page, query.pageSize)
    }
  }
}
