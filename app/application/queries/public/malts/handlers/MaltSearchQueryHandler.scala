package application.queries.public.malts.handlers

import application.queries.public.malts.MaltSearchQuery
import application.queries.public.malts.readmodels.{MaltReadModel, MaltListResponse}
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class MaltSearchQueryHandler @Inject()(
    maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[Try[MaltListResponse]] = {
    println(s"🔍 MaltSearchQueryHandler - recherche: ${query.name}")
    
    // Récupère tous les malts actifs avec pagination large pour le filtrage
    maltReadRepository.findAll(
      page = 0,
      pageSize = 1000, // Grande page pour récupérer plus de malts à filtrer
      activeOnly = true
    ).map { allMalts =>
      try {
        // Filtrage basique en mémoire
        val filteredMalts = allMalts.filter { malt =>
          val nameMatch = query.name.forall(n => 
            malt.name.value.toLowerCase.contains(n.toLowerCase)
          )
          val typeMatch = query.maltType.forall(t => 
            malt.maltType.name == t.name
          )
          val ebcMatch = (query.minEbc, query.maxEbc) match {
            case (Some(min), Some(max)) => malt.ebcColor.value >= min && malt.ebcColor.value <= max
            case (Some(min), None) => malt.ebcColor.value >= min
            case (None, Some(max)) => malt.ebcColor.value <= max
            case (None, None) => true
          }
          
          nameMatch && typeMatch && ebcMatch
        }
        
        // Pagination manuelle
        val offset = query.page * query.size
        val paginatedMalts = filteredMalts.drop(offset).take(query.size)
        
        val maltReadModels = paginatedMalts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = filteredMalts.length.toLong,
          page = query.page,
          size = query.size
        )
        
        println(s"📊 Recherche: ${filteredMalts.length} résultats, ${paginatedMalts.length} retournés")
        Success(response)
      } catch {
        case ex: Exception =>
          println(s"❌ Erreur dans MaltSearchQueryHandler: ${ex.getMessage}")
          Failure(ex)
      }
    }.recover {
      case ex: Exception =>
        println(s"❌ Erreur fatale dans MaltSearchQueryHandler: ${ex.getMessage}")
        Failure(ex)
    }
  }
}
