package application.yeasts.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import application.yeasts.dtos._
import application.yeasts.commands.{CreateYeastCommand, UpdateYeastCommand, DeleteYeastCommand}
import application.yeasts.handlers.CreateYeastCommandHandler
import domain.yeasts.repositories.{YeastReadRepository, YeastWriteRepository}
import domain.yeasts.model.{YeastId, YeastFilter, YeastStatus, YeastAggregate}
import java.util.UUID

@Singleton
class YeastApplicationService @Inject()(
  yeastReadRepository: YeastReadRepository,
  yeastWriteRepository: YeastWriteRepository,
  createYeastCommandHandler: CreateYeastCommandHandler
)(implicit ec: ExecutionContext) {

  private val yeastDTOs = new YeastDTOs()

  def findYeasts(request: YeastSearchRequestDTO): Future[Either[List[String], YeastListResponse]] = {
    // Debug logging pour diagnostiquer le problème de filtrage
    println(s"YeastApplicationService.findYeasts - Request: name=${request.name}, laboratory=${request.laboratory}, yeastType=${request.yeastType}")
    
    val parsedLaboratory = request.laboratory.flatMap(domain.yeasts.model.YeastLaboratory.fromName)
    println(s"Parsed laboratory: ${parsedLaboratory}")
    
    val parsedYeastType = request.yeastType.flatMap(domain.yeasts.model.YeastType.fromName)
    println(s"Parsed yeastType: ${parsedYeastType}")
    
    val filter = YeastFilter(
      name = request.name,
      yeastType = parsedYeastType,
      laboratory = parsedLaboratory,
      status = List.empty, // Temporairement - pour tester sans filtre statut
      page = request.page,
      size = request.size
    )
    
    yeastReadRepository.findByFilter(filter).map { result =>
      Right(yeastDTOs.toListResponse(result.items, result.totalCount, result.page, result.size))
    }
  }

  def getYeastById(yeastId: UUID): Future[Option[YeastAggregate]] = {
    YeastId.fromString(yeastId.toString) match {
      case Right(id) => 
        yeastReadRepository.findById(id)
      case Left(_) => 
        Future.successful(None)
    }
  }

  def searchYeasts(term: String, limit: Option[Int] = None): Future[Either[String, List[YeastResponse]]] = {
    yeastReadRepository.searchText(term).map { yeasts =>
      val results = yeasts.take(limit.getOrElse(10)).map(yeastDTOs.fromAggregate)
      Right(results)
    }
  }

  // ==========================================================================
  // MÉTHODES CRUD ADMIN
  // ==========================================================================

  /**
   * Créer une nouvelle levure
   */
  def createYeast(command: CreateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    createYeastCommandHandler.handle(command).map {
      case Left(errors) => Left(errors)
      case Right(yeast) => Right(yeast)
    }
  }

  /**
   * Mettre à jour une levure existante
   */
  def updateYeast(command: UpdateYeastCommand): Future[Either[List[String], YeastResponse]] = {
    YeastId.fromString(command.yeastId) match {
      case Left(error) => Future.successful(Left(List(error)))
      case Right(yeastId) =>
        yeastReadRepository.findById(yeastId).flatMap {
          case None => Future.successful(Left(List("Levure non trouvée")))
          case Some(existingYeast) =>
            // Logique de mise à jour (simplifiée pour l'instant)
            updateYeastFromCommand(existingYeast, command).flatMap {
              case Left(errors) => Future.successful(Left(errors))
              case Right(updatedYeast) =>
                yeastWriteRepository.update(updatedYeast).map { saved =>
                  Right(yeastDTOs.fromAggregate(saved))
                }.recover {
                  case ex => Left(List(s"Erreur lors de la mise à jour: ${ex.getMessage}"))
                }
            }
        }
    }
  }

  /**
   * Supprimer (archiver) une levure
   */
  def deleteYeast(command: DeleteYeastCommand): Future[Either[List[String], Unit]] = {
    YeastId.fromString(command.yeastId) match {
      case Left(error) => Future.successful(Left(List(error)))
      case Right(yeastId) =>
        yeastWriteRepository.archive(yeastId, command.reason).map { _ =>
          Right(())
        }.recover {
          case ex => Left(List(s"Erreur lors de la suppression: ${ex.getMessage}"))
        }
    }
  }

  /**
   * Changer le statut d'une levure
   */
  def changeYeastStatus(yeastId: String, newStatus: String, reason: Option[String] = None): Future[Either[List[String], Unit]] = {
    (YeastId.fromString(yeastId), parseYeastStatus(newStatus)) match {
      case (Left(idError), _) => Future.successful(Left(List(idError)))
      case (_, Left(statusError)) => Future.successful(Left(List(statusError)))
      case (Right(id), Right(status)) =>
        yeastWriteRepository.changeStatus(id, status, reason).map { _ =>
          Right(())
        }.recover {
          case ex => Left(List(s"Erreur lors du changement de statut: ${ex.getMessage}"))
        }
    }
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES
  // ==========================================================================

  private def updateYeastFromCommand(yeast: YeastAggregate, command: UpdateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    // Logique de mise à jour simplifiée - à implémenter selon les besoins
    Future.successful(Right(yeast)) // Temporaire
  }

  private def parseYeastStatus(status: String): Either[String, YeastStatus] = {
    status.toLowerCase match {
      case "active" => Right(YeastStatus.Active)
      case "inactive" => Right(YeastStatus.Inactive)
      case "archived" => Right(YeastStatus.Archived)
      case "seasonal" => Right(YeastStatus.Active) // Seasonal yeasts are considered active
      case _ => Left(s"Statut invalide: $status")
    }
  }
}
