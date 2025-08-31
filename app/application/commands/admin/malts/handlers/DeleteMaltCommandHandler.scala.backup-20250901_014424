package application.commands.admin.malts.handlers

import application.commands.admin.malts.DeleteMaltCommand
import domain.malts.model._
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
    command.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validCommand) => processDelete(validCommand)
    }
  }

  private def processDelete(command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    MaltId(command.id) match {
      case Left(error) => Future.successful(Left(DomainError.validation(error)))
      case Right(maltId) =>
        for {
          maltOpt <- maltReadRepo.findById(maltId)
          result <- maltOpt match {
            case None =>
              Future.successful(Left(DomainError.notFound("MALT", command.id)))
            case Some(existingMalt) =>
              executeDelete(existingMalt, command)
          }
        } yield result
    }
  }

  private def executeDelete(malt: MaltAggregate, command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    if (command.forceDelete) {
      maltWriteRepo.delete(malt.id).map(_ => Right(())).recover {
        case ex: Exception =>
          Left(DomainError.validation(s"Erreur lors de la suppression: ${ex.getMessage}"))
      }
    } else {
      // Suppression logique
      malt.deactivate(command.reason.getOrElse("Suppression via API")) match {
        case Left(error) => Future.successful(Left(error))
        case Right(deactivatedMalt) =>
          maltWriteRepo.update(deactivatedMalt).map(_ => Right(())).recover {
            case ex: Exception =>
              Left(DomainError.validation(s"Erreur lors de la désactivation: ${ex.getMessage}"))
          }
      }
    }
  }
}
