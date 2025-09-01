package application.queries.public.hops.handlers

import application.queries.public.hops.HopSearchQuery
import application.queries.public.hops.readmodels.{HopReadModel, HopListResponse}
import domain.hops.repositories.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class HopSearchQueryHandler @Inject()(
    hopReadRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: HopSearchQuery): Future[Try[HopListResponse]] = {
    println(s"🍺 HopSearchQueryHandler - recherche: ${query.name}")
    
    // Utilise la méthode searchHops existante de l'interface Hops
    hopReadRepository.searchHops(
      name = query.name,
      originCode = query.originCode,
      usage = query.usage.map(_.toString),
      minAlphaAcid = query.minAlphaAcid,
      maxAlphaAcid = query.maxAlphaAcid,
      activeOnly = true
    ).map { hops =>
      try {
        // Pagination manuelle sur les résultats
        val offset = query.page * query.size
        val paginatedHops = hops.drop(offset).take(query.size)
        
        val hopReadModels = paginatedHops.map(HopReadModel.fromAggregate)
        val response = HopListResponse.create(
          hops = hopReadModels,
          totalCount = hops.length.toLong,
          page = query.page,
          size = query.size
        )
        
        println(s"📊 Recherche: ${hops.length} résultats totaux, ${paginatedHops.length} retournés")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans HopSearchQueryHandler: ${ex.getMessage}")
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans HopSearchQueryHandler: ${ex.getMessage}")
        Failure(ex)
    }
  }
}
