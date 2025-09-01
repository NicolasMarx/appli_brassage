package application.queries.public.hops.handlers

import application.queries.public.hops.HopListQuery
import application.queries.public.hops.readmodels.{HopReadModel, HopListResponse}
import domain.hops.repositories.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class HopListQueryHandler @Inject()(
    hopReadRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: HopListQuery): Future[Try[HopListResponse]] = {
    println(s"🍺 HopListQueryHandler - page: ${query.page}, size: ${query.size}")
    
    // Utilise la méthode existante de l'interface Hops
    hopReadRepository.findActiveHops(query.page, query.size).map { case (hops, totalCount) =>
      try {
        println(s"📊 Repository retourné: ${hops.length} hops, total: $totalCount")
        
        val hopReadModels = hops.map(HopReadModel.fromAggregate)
        val response = HopListResponse.create(
          hops = hopReadModels,
          totalCount = totalCount,
          page = query.page,
          size = query.size
        )
        println(s"✅ Response créé: ${response.hops.length} hops dans la réponse")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans HopListQueryHandler: ${ex.getMessage}")
          ex.printStackTrace()
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans HopListQueryHandler: ${ex.getMessage}")
        ex.printStackTrace()
        Failure(ex)
    }
  }
}
