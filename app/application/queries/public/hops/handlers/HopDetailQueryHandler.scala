package application.queries.public.hops.handlers

import application.queries.public.hops.HopDetailQuery
import application.queries.public.hops.readmodels.HopReadModel
import domain.hops.model.HopId
import domain.hops.repositories.HopReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class HopDetailQueryHandler @Inject()(
    hopReadRepository: HopReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: HopDetailQuery): Future[Try[Option[HopReadModel]]] = {
    println(s"🍺 HopDetailQueryHandler - ID: ${query.hopId}")
    
    try {
      val hopId = HopId(query.hopId)
      println(s"✅ HopId créé: $hopId")
      
      hopReadRepository.findById(hopId).map { hopOpt =>
        try {
          val readModelOpt = hopOpt.map { hop =>
            println(s"✅ Hop trouvé: ${hop.name.value}")
            HopReadModel.fromAggregate(hop)
          }
          if (readModelOpt.isEmpty) {
            println(s"⚠️ Aucun hop trouvé pour l'ID: ${query.hopId}")
          }
          Success(readModelOpt)
        } catch {
          case ex: Exception =>
            println(s"❌ Erreur lors de la conversion: ${ex.getMessage}")
            Failure(ex)
        }
      }.recover {
        case ex: Exception => 
          println(s"❌ Erreur repository: ${ex.getMessage}")
          Failure(ex)
      }
      
    } catch {
      case ex: Exception =>
        println(s"❌ Erreur lors de la création HopId: ${ex.getMessage}")
        Future.successful(Failure(ex))
    }
  }
}
