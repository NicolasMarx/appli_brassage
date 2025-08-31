package application.commands.admin.malts.handlers

import application.commands.admin.malts.UpdateMaltCommand
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.shared._
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la mise à jour de malts
 * Gère les modifications partielles avec validation métier
 */
@Singleton
class UpdateMaltCommandHandler @Inject()(
                                          maltReadRepo: MaltReadRepository,
                                          maltWriteRepo: MaltWriteRepository
                                        )(implicit ec: ExecutionContext) {

  def handle(command: UpdateMaltCommand): Future[Either[DomainError, MaltAggregate]] = {
    command.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validCommand) => processUpdate(validCommand)
    }
  }

  private def processUpdate(command: UpdateMaltCommand): Future[Either[DomainError, MaltAggregate]] = {
    for {
      maltId <- Future.fromTry(MaltId(command.id).toTry)
      maltOpt <- maltReadRepo.findById(maltId)
      result <- maltOpt match {
        case None =>
          Future.successful(Left(DomainError.notFound("MALT", command.id)))
        case Some(existingMalt) =>
          applyUpdates(existingMalt, command)
      }
    } yield result
  }

  private def applyUpdates(
                            malt: MaltAggregate,
                            command: UpdateMaltCommand
                          ): Future[Either[DomainError, MaltAggregate]] = {

    var updatedMalt = malt

    // Application séquentielle des modifications avec validation
    val updates = for {
      // Mise à jour infos de base
      maltAfterBasicUpdate <- command.name.orElse(command.description) match {
        case Some(_) =>
          val newName = command.name.map(NonEmptyString.create).getOrElse(Right(malt.name))
          val newDesc = command.description.orElse(malt.description)

          newName match {
            case Left(error) => Left(DomainError.validation(error))
            case Right(name) => malt.updateBasicInfo(name, newDesc)
          }
        case None => Right(malt)
      }

      // Mise à jour spécifications maltage
      maltAfterSpecsUpdate <- if (command.ebcColor.isDefined || command.extractionRate.isDefined || command.diastaticPower.isDefined) {
        for {
          newEBC <- command.ebcColor.map(EBCColor(_)).getOrElse(Right(maltAfterBasicUpdate.ebcColor))
          newExtraction <- command.extractionRate.map(ExtractionRate(_)).getOrElse(Right(maltAfterBasicUpdate.extractionRate))
          newDiastatic <- command.diastaticPower.map(DiastaticPower(_)).getOrElse(Right(maltAfterBasicUpdate.diastaticPower))
          updatedMalt <- maltAfterBasicUpdate.updateMaltingSpecs(newEBC, newExtraction, newDiastatic)
        } yield updatedMalt
      } else {
        Right(maltAfterBasicUpdate)
      }

      // Mise à jour profils arômes
      maltAfterFlavorUpdate <- command.flavorProfiles match {
        case Some(profiles) => maltAfterSpecsUpdate.updateFlavorProfiles(profiles)
        case None => Right(maltAfterSpecsUpdate)
      }

      // Mise à jour statut
      finalMalt <- command.status match {
        case Some(statusName) =>
          MaltStatus.fromName(statusName) match {
            case Some(newStatus) =>
              maltAfterFlavorUpdate.changeStatus(newStatus, "Mise à jour via API")
            case None =>
              Left(DomainError.validation(s"Statut invalide: $statusName"))
          }
        case None => Right(maltAfterFlavorUpdate)
      }
    } yield finalMalt

    updates match {
      case Left(domainError) =>
        Future.successful(Left(domainError))
      case Right(updatedMalt) =>
        // Vérifier si changement de nom nécessite validation unicité
        val nameChanged = command.name.exists(_ != malt.name.value)
        if (nameChanged) {
          command.name.map(_.trim) match {
            case Some(newName) =>
              maltReadRepo.existsByName(newName).flatMap { exists =>
                if (exists) {
                  Future.successful(Left(DomainError.conflict("Un malt avec ce nom existe déjà", "name")))
                } else {
                  persistUpdate(updatedMalt)
                }
              }
            case None => persistUpdate(updatedMalt)
          }
        } else {
          persistUpdate(updatedMalt)
        }
    }
  }

  private def persistUpdate(malt: MaltAggregate): Future[Either[DomainError, MaltAggregate]] = {
    maltWriteRepo.update(malt).map { _ =>
      // TODO: Publier events domaine
      Right(malt)
    }.recover {
      case ex: Exception =>
        Left(DomainError.validation(s"Erreur lors de la mise à jour: ${ex.getMessage}"))
    }
  }
}