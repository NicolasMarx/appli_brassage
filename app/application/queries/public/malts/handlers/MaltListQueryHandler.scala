package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltListQuery, MaltReadModel}
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, PagedResult}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la liste des malts (API publique)
 */
@Singleton
class MaltListQueryHandler @Inject()(
                                      maltReadRepo: MaltReadRepository
                                    )(implicit ec: ExecutionContext) {

  def handle(query: MaltListQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeQuery(validQuery)
    }
  }

  private def executeQuery(query: MaltListQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {

    // Construction des filtres
    val maltTypeFilter = query.maltType.flatMap(MaltType.fromName)
    val statusFilter = if (query.activeOnly) Some(MaltStatus.ACTIVE) else None

    maltReadRepo.findByFilters(
      maltType = maltTypeFilter,
      minEBC = query.minEBC,
      maxEBC = query.maxEBC,
      originCode = query.originCode,
      status = statusFilter,
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
      case ex: Exception => Left(s"Erreur lors de la récupération: ${ex.getMessage}")
    }
  }
}