package application.queries.admin.malts.handlers

import application.queries.admin.malts.{AdminMaltListQuery}
import application.queries.admin.malts.readmodels.AdminMaltReadModel
import domain.malts.repositories.MaltReadRepository
import domain.malts.model._  // Simplified import
import domain.common.PagedResult
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la liste admin des malts (version simplifiée)
 */
@Singleton
class AdminMaltListQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: AdminMaltListQuery): Future[Either[String, PagedResult[AdminMaltReadModel]]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeQuery(validQuery)
    }
  }

  private def executeQuery(query: AdminMaltListQuery): Future[Either[String, PagedResult[AdminMaltReadModel]]] = {
    // Pour l'instant, implémentation simplifiée qui utilise findAll
    maltReadRepo.findAll(query.page, query.pageSize, activeOnly = false).map { malts =>
      val adminReadModels = malts.map(AdminMaltReadModel.fromAggregate)
      Right(PagedResult(
        items = adminReadModels,
        currentPage = query.page,
        pageSize = query.pageSize,
        totalCount = adminReadModels.length.toLong,
        hasNext = false
      ))
    }.recover {
      case ex: Exception => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}
