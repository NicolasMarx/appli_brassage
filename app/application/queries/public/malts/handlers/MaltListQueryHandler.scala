package application.queries.public.malts.handlers

import application.queries.public.malts.MaltListQuery
import application.queries.public.malts.readmodels.{MaltReadModel, MaltListResponse}
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class MaltListQueryHandler @Inject()(
    maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltListQuery): Future[Try[MaltListResponse]] = {
    println(s"🔍 MaltListQueryHandler - page: ${query.page}, size: ${query.size}")
    
    // Appel avec les paramètres requis : page, pageSize, activeOnly
    maltReadRepository.findAll(
      page = query.page,
      pageSize = query.size,
      activeOnly = true  // On veut seulement les malts actifs pour l'API publique
    ).map { malts =>
      try {
        println(s"📊 Repository retourné: ${malts.length} malts")
        
        val maltReadModels = malts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = malts.length.toLong, // Approximation - pas de count séparé
          page = query.page,
          size = query.size
        )
        println(s"✅ Response créé: ${response.malts.length} malts dans la réponse")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans MaltListQueryHandler: ${ex.getMessage}")
          ex.printStackTrace()
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans MaltListQueryHandler: ${ex.getMessage}")
        ex.printStackTrace()
        Failure(ex)
    }
  }
}
