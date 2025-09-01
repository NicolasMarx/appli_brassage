package application.queries.public.malts.handlers

import application.queries.public.malts.MaltDetailQuery
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.model.MaltId
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

@Singleton
class MaltDetailQueryHandler @Inject()(
    maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltDetailQuery): Future[Try[Option[MaltReadModel]]] = {
    println(s"🔍 MaltDetailQueryHandler - ID: ${query.maltId}")
    
    try {
      // Création directe du MaltId - il semble que votre MaltId ne retourne pas Either
      val maltId = MaltId(java.util.UUID.fromString(query.maltId))
      println(s"✅ MaltId créé: $maltId")
      
      maltReadRepository.findById(maltId).map { maltOpt =>
        try {
          val readModelOpt = maltOpt.map { malt =>
            println(s"✅ Malt trouvé: ${malt.name.value}")
            MaltReadModel.fromAggregate(malt)
          }
          if (readModelOpt.isEmpty) {
            println(s"⚠️ Aucun malt trouvé pour l'ID: ${query.maltId}")
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
      case ex: IllegalArgumentException =>
        println(s"❌ UUID invalide: ${query.maltId}")
        Future.successful(Failure(new IllegalArgumentException(s"Invalid UUID: ${query.maltId}")))
      case ex: Exception =>
        println(s"❌ Erreur lors de la création MaltId: ${ex.getMessage}")
        Future.successful(Failure(ex))
    }
  }
}
