package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltListQuery}
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.MaltReadRepository
import domain.malts.model._  // Simplified import
import domain.common.PagedResult
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la liste publique des malts (version simplifiée)
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
    maltReadRepo.findAll(query.page, query.pageSize, query.activeOnly).map { malts =>
      val readModels = malts.map(MaltReadModel.fromAggregate)
      Right(PagedResult(
        items = readModels,
        currentPage = query.page,
        pageSize = query.pageSize,
        totalCount = readModels.length.toLong,
        hasNext = false
      ))
    }.recover {
      case ex: Exception => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}
