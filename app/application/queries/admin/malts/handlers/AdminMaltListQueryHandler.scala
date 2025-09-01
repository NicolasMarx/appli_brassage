package application.queries.admin.malts.handlers

import application.queries.admin.malts.{AdminMaltListQuery, AdminMaltListResponse}
import application.queries.admin.malts.readmodels.AdminMaltReadModel
import domain.malts.repositories.MaltReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la requête de liste des malts (interface admin)
 */
@Singleton
class AdminMaltListQueryHandler @Inject()(
  maltRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: AdminMaltListQuery): Future[AdminMaltListResponse] = {
    for {
      malts <- maltRepository.findAll(query.page, query.pageSize, activeOnly = false)
      count <- maltRepository.count(activeOnly = false)
    } yield {
      val readModels = malts.map(AdminMaltReadModel.fromAggregate)
      AdminMaltListResponse(readModels, count, query.page, query.pageSize)
    }
  }
}
