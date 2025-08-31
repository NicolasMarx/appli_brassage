package application.commands.admin.malts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.malts.model.MaltId
import application.commands.admin.malts.DeleteMaltCommand
import domain.common.DomainError

@Singleton
class DeleteMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: DeleteMaltCommand): Future[Either[DomainError, Unit]] = {
    val maltId = MaltId.fromString(command.id).getOrElse(MaltId.unsafe(command.id))
    
    maltReadRepo.findById(maltId).flatMap {
      case None =>
        Future.successful(Left(DomainError.notFound("Malt", command.id)))
      case Some(malt) =>
        try {
          val deactivatedMalt = malt.deactivate()
          
          maltWriteRepo.update(deactivatedMalt).map { _ =>
            Right(())
          }.recover {
            case ex: Throwable => Left(DomainError.technical(s"Erreur suppression: ${ex.getMessage}"))
          }
        } catch {
          case ex: Throwable =>
            Future.successful(Left(DomainError.technical(s"Erreur: ${ex.getMessage}")))
        }
    }
  }
}
