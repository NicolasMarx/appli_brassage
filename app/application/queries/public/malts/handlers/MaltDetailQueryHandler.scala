package application.queries.public.malts.handlers

import application.queries.public.malts.MaltDetailQuery
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.MaltId
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour les requêtes de détail de malt (API publique)
 */
@Singleton
class MaltDetailQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Handle pour obtenir le détail d'un malt
   * ✅ CORRIGÉ: Gestion correcte des types et erreurs
   */
  def handle(query: MaltDetailQuery): Future[Option[MaltReadModel]] = {
    // Conversion String -> MaltId avec gestion d'erreurs
    MaltId(query.maltId) match {
      case Right(maltId) =>
        // ✅ maltId est maintenant de type MaltId
        maltReadRepo.findById(maltId).map(_.map(MaltReadModel.fromAggregate))
      
      case Left(_) =>
        // ID invalide, retourner None
        Future.successful(None)
    }
  }
}
