package application.queries.admin.malts.handlers

import application.queries.admin.malts.AdminMaltListQuery
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, PagedResult}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler liste malts pour administration avec informations complètes
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

    // Construction filtres admin
    val maltTypeFilter = query.maltType.flatMap(MaltType.fromName)
    val statusFilter = query.status.flatMap(MaltStatus.fromName)
    val sourceFilter = query.source.flatMap(MaltSource.fromName)

    // Filtrage spécial pour révision nécessaire
    val credibilityFilter = if (query.needsReview) {
      Some(70) // Seuil révision
    } else {
      query.minCredibility
    }

    maltReadRepo.findByFilters(
      maltType = maltTypeFilter,
      status = statusFilter,
      source = sourceFilter,
      minCredibility = credibilityFilter,
      searchTerm = query.searchTerm,
      page = query.page,
      pageSize = query.pageSize
    ).map { pagedResult =>
      val adminReadModels = pagedResult.items.map(AdminMaltReadModel.fromAggregate)

      // Tri côté application si nécessaire
      val sortedItems = sortResults(adminReadModels, query.sortBy, query.sortOrder)

      Right(PagedResult(
        items = sortedItems,
        currentPage = pagedResult.currentPage,
        pageSize = pagedResult.pageSize,
        totalCount = pagedResult.totalCount,
        hasNext = pagedResult.hasNext
      ))
    }.recover {
      case ex: Exception => Left(s"Erreur lors de la récupération admin: ${ex.getMessage}")
    }
  }

  private def sortResults(
                           items: List[AdminMaltReadModel],
                           sortBy: String,
                           sortOrder: String
                         ): List[AdminMaltReadModel] = {
    val sorted = sortBy match {
      case "name" => items.sortBy(_.name)
      case "createdAt" => items.sortBy(_.createdAt)
      case "updatedAt" => items.sortBy(_.updatedAt)
      case "credibilityScore" => items.sortBy(_.credibilityScore)
      case "ebcColor" => items.sortBy(_.ebcColor)
      case _ => items
    }

    if (sortOrder == "desc") sorted.reverse else sorted
  }
}