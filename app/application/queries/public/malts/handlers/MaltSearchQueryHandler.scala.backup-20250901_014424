package application.queries.public.malts.handlers

import application.queries.public.malts.MaltSearchQuery
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.{MaltReadRepository, PagedResult}
import domain.malts.model.{MaltType, MaltStatus}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaltSearchQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    executeSearch(query)
  }

  private def executeSearch(query: MaltSearchQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    val maltTypeFilter = query.maltType.flatMap(MaltType.fromName).map(_.name)
    val statusFilter = if (query.activeOnly) Some("ACTIVE") else None
    
    maltReadRepo.findByFilters(
      maltType = maltTypeFilter,
      minEBC = query.minEBC,
      maxEBC = query.maxEBC,
      originCode = query.originCode,
      status = statusFilter,
      searchTerm = Some(query.searchTerm), // String → Option[String]
      flavorProfiles = query.flavorProfiles, // List[String] → List[String] (pas de getOrElse)
      page = query.page,
      pageSize = query.pageSize
    ).map { pagedResult =>
      val readModels = pagedResult.items.map(MaltReadModel.fromAggregate)
      Right(PagedResult(
        items = readModels,
        currentPage = pagedResult.currentPage,
        pageSize = pagedResult.pageSize,
        totalCount = pagedResult.totalCount,
        hasNext = pagedResult.hasNext
      ))
    }.recover {
      case ex => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}
