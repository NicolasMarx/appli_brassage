package application.commands.admin.malts.handlers

import application.commands.admin.malts.UpdateMaltCommand
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.malts.model.MaltId
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la mise à jour de malts
 */
@Singleton
class UpdateMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: UpdateMaltCommand): Future[Either[DomainError, MaltId]] = {
    command.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validCommand) => processUpdate(validCommand)
    }
  }

  private def processUpdate(command: UpdateMaltCommand): Future[Either[DomainError, MaltId]] = {
    MaltId.fromString(command.id) match {
      case Right(maltId) =>
        for {
          maltOpt <- maltReadRepo.findById(maltId)
          result <- maltOpt match {
            case Some(malt) => 
              // TODO: Implémenter la mise à jour complète
              Future.successful(Right(malt.id))
            case None => 
              Future.successful(Left(DomainError.notFound("Malt", command.id)))
          }
        } yield result
      case Left(error) =>
        Future.successful(Left(DomainError.validation(error)))
    }
  }
}
