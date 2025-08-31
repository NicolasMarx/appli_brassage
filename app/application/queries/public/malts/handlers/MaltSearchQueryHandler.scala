package application.queries.public.malts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.malts.repositories.MaltReadRepository
import application.queries.public.malts.MaltSearchQuery
import application.queries.public.malts.readmodels.MaltReadModel

@Singleton  
class MaltSearchQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[List[MaltReadModel]] = {
    maltReadRepo.findByFilters(
      maltType = query.maltType,
      minEBC = query.minEBC,
      maxEBC = query.maxEBC,
      originCode = query.originCode,
      status = if (query.activeOnly) Some("ACTIVE") else None,
      searchTerm = Some(query.searchTerm),
      flavorProfiles = query.flavorProfiles,
      page = query.page,
      pageSize = query.pageSize
    ).map { malts =>
      malts.map(MaltReadModel.fromAggregate)
    }
  }
}
