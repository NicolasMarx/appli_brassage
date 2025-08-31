package application.commands.admin.malts.handlers

import application.commands.admin.malts.DeleteMaltCommand
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

/**
 * Handler pour la suppression de malts
 * Gère suppression logique (désactivation) ou physique selon contexte
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

  private def executeDelete(
                             malt: MaltAggregate,
                             command: DeleteMaltCommand
                           ): Future[Either[DomainError, Unit]] = {

    if (command.forceDelete) {
      // Suppression physique (dangereuse, pour admin uniquement)
      physicalDelete(malt.id)
    } else {
      // Suppression logique (désactivation - recommandée)
      logicalDelete(malt, command.reason.getOrElse("Suppression via API"))
    }
  }

  private def logicalDelete(malt: MaltAggregate, reason: String): Future[Either[DomainError, Unit]] = {
    malt.deactivate(reason) match {
      case Left(domainError) =>
        Future.successful(Left(domainError))
      case Right(deactivatedMalt) =>
        maltWriteRepo.update(deactivatedMalt).map { _ =>
          // TODO: Publier MaltDeactivated event
          Right(())
        }.recover {
          case ex: Exception =>
            Left(DomainError.validation(s"Erreur lors de la désactivation: ${ex.getMessage}"))
        }
    }
  }

  private def physicalDelete(maltId: MaltId): Future[Either[DomainError, Unit]] = {
    maltWriteRepo.delete(maltId).map { _ =>
      // TODO: Publier MaltDeleted event
      Right(())
    }.recover {
      case ex: Exception =>
        Left(DomainError.validation(s"Erreur lors de la suppression physique: ${ex.getMessage}"))
    }
  }

  /**
   * Vérifications de sécurité avant suppression physique
   */
  private def canPhysicallyDelete(malt: MaltAggregate): Future[Either[DomainError, Unit]] = {
    // TODO: Vérifier références dans recipes, etc.
    if (malt.isActive) {
      Future.successful(Left(DomainError.businessRule(
        "Impossible de supprimer un malt actif. Désactivez-le d'abord.",
        "CANNOT_DELETE_ACTIVE_MALT"
      )))
    } else {
      Future.successful(Right(()))
    }
  }
}