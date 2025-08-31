package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltSearchQuery, MaltReadModel}
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, PagedResult}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la recherche avancée de malts (API publique)
 */
@Singleton
class MaltSearchQueryHandler @Inject()(
                                        maltReadRepo: MaltReadRepository
                                      )(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeSearch(validQuery)
    }
  }

  private def executeSearch(query: MaltSearchQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {

    // Construction des filtres de recherche
    val maltTypeFilter = query.maltType.flatMap(MaltType.fromName)
    val statusFilter = if (query.activeOnly) Some(MaltStatus.ACTIVE) else None

    maltReadRepo.findByFilters(
      maltType = maltTypeFilter,
      minEBC = query.minEBC,
      maxEBC = query.maxEBC,
      minExtraction = query.minExtraction,
      minDiastaticPower = query.minDiastaticPower,
      originCode = query.originCode,
      status = statusFilter,
      flavorProfiles = query.flavorProfiles,
      searchTerm = Some(query.searchTerm),
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
      case ex: Exception => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}