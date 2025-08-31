package application.queries.public.malts.handlers

import application.queries.public.malts.MaltDetailQuery
import application.queries.public.malts.readmodels._
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.MaltId

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaltDetailQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltDetailQuery): Future[Option[MaltDetailResult]] = {
    // Conversion String → MaltId
    MaltId(query.id) match {
      case Right(maltId) =>
        maltReadRepo.findById(maltId).map { maltOpt =>
          maltOpt.map { malt =>
            val maltReadModel = MaltReadModel.fromAggregate(malt)
            MaltDetailResult(
              malt = maltReadModel,
              substitutes = List.empty
            )
          }
        }
      case Left(_) =>
        Future.successful(None) // ID invalide
    }
  }
}
