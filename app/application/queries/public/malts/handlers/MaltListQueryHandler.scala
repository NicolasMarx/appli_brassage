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
    query.maltType.foreach(t => println(s"🔍 Filtrage par type: $t"))
    
    // Appel avec les paramètres requis : page, pageSize, activeOnly
    maltReadRepository.findAll(
      page = query.page,
      pageSize = query.size,
      activeOnly = true  // On veut seulement les malts actifs pour l'API publique
    ).map { malts =>
      try {
        println(s"📊 Repository retourné: ${malts.length} malts")
        
        // CORRECTION: Appliquer le filtre par type après récupération
        val filteredMalts = query.maltType match {
          case Some(targetType) =>
            val filtered = malts.filter(_.maltType == targetType)
            println(s"🎯 Filtré par type $targetType: ${filtered.length}/${malts.length} malts")
            filtered
          case None => malts
        }
        
        val maltReadModels = filteredMalts.map(MaltReadModel.fromAggregate)
        val response = MaltListResponse.create(
          malts = maltReadModels,
          totalCount = filteredMalts.length.toLong,
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
