package application.yeasts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import application.commands.admin.yeasts._
import application.yeasts.dtos._

@Singleton
class YeastCommandHandlers @Inject()(
  // Les vrais handlers seraient injectés ici
)(implicit ec: ExecutionContext) {

  def handleCreateYeast(command: CreateYeastCommand): Future[Either[List[String], YeastDetailResponseDTO]] = {
    // TODO: Implémenter avec le vrai handler
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleUpdateYeast(command: UpdateYeastCommand): Future[Either[List[String], YeastDetailResponseDTO]] = {
    Future.successful(Left(List("Not implemented yet")))
  }

  def handleDeleteYeast(command: DeleteYeastCommand): Future[Either[String, Unit]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleActivateYeast(command: ActivateYeastCommand): Future[Either[String, YeastDetailResponseDTO]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleDeactivateYeast(command: DeactivateYeastCommand): Future[Either[String, YeastDetailResponseDTO]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleArchiveYeast(command: ArchiveYeastCommand): Future[Either[String, YeastDetailResponseDTO]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleChangeStatus(command: ChangeYeastStatusCommand): Future[Either[String, Unit]] = {
    Future.successful(Left("Not implemented yet"))
  }

  def handleBatchCreate(command: CreateYeastsBatchCommand): Future[Either[List[String], List[YeastDetailResponseDTO]]] = {
    Future.successful(Left(List("Not implemented yet")))
  }
}
