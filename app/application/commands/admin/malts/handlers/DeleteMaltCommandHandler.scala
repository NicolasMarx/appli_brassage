package application.commands.admin.malts.handlers

import application.commands.admin.malts.DeleteMaltCommand
import domain.malts.model.MaltId
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la suppression de malts
 */
@Singleton
class DeleteMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    MaltId.fromString(command.id) match {
      case Right(maltId) =>
        for {
          maltOpt <- maltReadRepo.findById(maltId)
          result <- maltOpt match {
            case Some(_) => 
              // TODO: Implémenter la suppression complète
              Future.successful(Right(()))
            case None => 
              Future.successful(Left(DomainError.notFound("Malt", command.id)))
          }
        } yield result
      case Left(error) =>
        Future.successful(Left(DomainError.validation(error)))
    }
  }
}
