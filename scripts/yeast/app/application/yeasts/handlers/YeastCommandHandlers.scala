package application.yeasts.handlers

import application.yeasts.commands._
import domain.yeasts.model._
import domain.yeasts.repositories.YeastRepository
import domain.yeasts.services.{YeastDomainService, YeastValidationService}
import domain.shared.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

/**
 * Handlers pour les commandes de levures
 * Pattern CQRS - Command Side
 */
@Singleton
class YeastCommandHandlers @Inject()(
  yeastRepository: YeastRepository,
  yeastDomainService: YeastDomainService,
  yeastValidationService: YeastValidationService
)(implicit ec: ExecutionContext) {

  /**
   * Handler pour créer une nouvelle levure
   */
  def handle(command: CreateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    // 1. Validation de la commande
    command.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        // 2. Validation métier avec le service de validation
        yeastValidationService.validateNewYeast(
          name = command.name,
          laboratory = command.laboratory,
          strain = command.strain,
          yeastType = command.yeastType,
          attenuationMin = command.attenuationMin,
          attenuationMax = command.attenuationMax,
          tempMin = command.temperatureMin,
          tempMax = command.temperatureMax,
          alcoholTolerance = command.alcoholTolerance,
          flocculation = command.flocculation,
          aromaProfile = command.aromaProfile,
          flavorProfile = command.flavorProfile
        ) match {
          case Left(validationErrors) => 
            Future.successful(Left(validationErrors.map(_.message)))
            
          case Right(validatedData) =>
            // 3. Vérification unicité laboratoire/souche
            yeastDomainService.checkUniqueLaboratoryStrain(
              validatedData.laboratory, 
              validatedData.strain
            ).flatMap {
              case Left(error) => Future.successful(Left(List(error.message)))
              case Right(_) =>
                
                // 4. Création de l'agrégat
                val characteristics = YeastCharacteristics(
                  aromaProfile = command.aromaProfile,
                  flavorProfile = command.flavorProfile,
                  esters = command.esters,
                  phenols = command.phenols,
                  otherCompounds = command.otherCompounds,
                  notes = command.notes
                )
                
                YeastAggregate.create(
                  name = validatedData.name,
                  laboratory = validatedData.laboratory,
                  strain = validatedData.strain,
                  yeastType = validatedData.yeastType,
                  attenuation = validatedData.attenuation,
                  temperature = validatedData.temperature,
                  alcoholTolerance = validatedData.alcoholTolerance,
                  flocculation = validatedData.flocculation,
                  characteristics = characteristics,
                  createdBy = command.createdBy
                ) match {
                  case Left(domainError) => 
                    Future.successful(Left(List(domainError.message)))
                  case Right(yeast) =>
                    // 5. Sauvegarde
                    yeastRepository.create(yeast).map(Right(_))
                }
            }
        }
    }
  }

  /**
   * Handler pour mettre à jour une levure
   */
  def handle(command: UpdateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    if (!command.hasChanges) {
      return Future.successful(Left(List("Aucune modification fournie")))
    }
    
    command.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        val yeastId = YeastId(command.yeastId)
        yeastRepository.findById(yeastId).flatMap {
          case None => 
            Future.successful(Left(List(s"Levure non trouvée: ${command.yeastId}")))
            
          case Some(yeast) =>
            // Construire les mises à jour
            var updatedYeast = yeast
            var hasErrors = false
            var errors = List.empty[String]
            
            // Mise à jour du nom si fourni
            command.name.foreach { newName =>
              YeastName.fromString(newName) match {
                case Left(error) => 
                  errors = error :: errors
                  hasErrors = true
                case Right(yeastName) =>
                  updatedYeast.updateName(yeastName, command.updatedBy) match {
                    case Left(domainError) => 
                      errors = domainError.message :: errors
                      hasErrors = true
                    case Right(updated) => 
                      updatedYeast = updated
                  }
              }
            }
            
            // Mise à jour des données techniques si fournies
            val technicalUpdates = (
              command.attenuationMin.map(AttenuationRange(_, command.attenuationMax.getOrElse(yeast.attenuation.max))),
              command.temperatureMin.map(FermentationTemp(_, command.temperatureMax.getOrElse(yeast.temperature.max))),
              command.alcoholTolerance.map(AlcoholTolerance(_)),
              command.flocculation.flatMap(FlocculationLevel.fromName),
              buildUpdatedCharacteristics(yeast.characteristics, command)
            )
            
            technicalUpdates match {
              case (newAttenuation, newTemperature, newAlcoholTolerance, newFlocculation, newCharacteristics) =>
                if (newAttenuation.isDefined || newTemperature.isDefined || 
                    newAlcoholTolerance.isDefined || newFlocculation.isDefined || newCharacteristics.isDefined) {
                  
                  updatedYeast.updateTechnicalData(
                    newAttenuation = newAttenuation,
                    newTemperature = newTemperature,
                    newAlcoholTolerance = newAlcoholTolerance,
                    newFlocculation = newFlocculation,
                    newCharacteristics = newCharacteristics,
                    updatedBy = command.updatedBy
                  ) match {
                    case Left(domainError) => 
                      errors = domainError.message :: errors
                      hasErrors = true
                    case Right(updated) => 
                      updatedYeast = updated
                  }
                }
            }
            
            // Mise à jour laboratoire/souche si fournis
            (command.laboratory, command.strain) match {
              case (Some(labName), strainOpt) => 
                YeastLaboratory.fromName(labName) match {
                  case Some(lab) =>
                    val strainValue = strainOpt.getOrElse(yeast.strain.value)
                    YeastStrain.fromString(strainValue) match {
                      case Right(strain) =>
                        updatedYeast.updateLaboratoryInfo(lab, strain, command.updatedBy) match {
                          case Left(domainError) => 
                            errors = domainError.message :: errors
                            hasErrors = true
                          case Right(updated) => 
                            updatedYeast = updated
                        }
                      case Left(error) => 
                        errors = error :: errors
                        hasErrors = true
                    }
                  case None => 
                    errors = s"Laboratoire inconnu: $labName" :: errors
                    hasErrors = true
                }
              case (None, Some(strainName)) =>
                YeastStrain.fromString(strainName) match {
                  case Right(strain) =>
                    updatedYeast.updateLaboratoryInfo(yeast.laboratory, strain, command.updatedBy) match {
                      case Left(domainError) => 
                        errors = domainError.message :: errors
                        hasErrors = true
                      case Right(updated) => 
                        updatedYeast = updated
                    }
                  case Left(error) => 
                    errors = error :: errors
                    hasErrors = true
                }
              case _ => // Pas de modification laboratoire/souche
            }
            
            if (hasErrors) {
              Future.successful(Left(errors.reverse))
            } else {
              yeastRepository.update(updatedYeast).map(Right(_))
            }
        }
    }
  }

  /**
   * Handler pour changer le statut d'une levure
   */
  def handle(command: ChangeYeastStatusCommand): Future[Either[String, Unit]] = {
    command.validate match {
      case Left(errors) => Future.successful(Left(errors.mkString(", ")))
      case Right(_) =>
        
        YeastStatus.parse(command.newStatus) match {
          case Left(error) => Future.successful(Left(error))
          case Right(newStatus) =>
            
            val yeastId = YeastId(command.yeastId)
            yeastRepository.changeStatus(yeastId, newStatus, command.reason).map(Right(_))
        }
    }
  }

  /**
   * Handler pour activer une levure
   */
  def handle(command: ActivateYeastCommand): Future[Either[String, YeastAggregate]] = {
    val yeastId = YeastId(command.yeastId)
    
    yeastRepository.findById(yeastId).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: ${command.yeastId}"))
      case Some(yeast) =>
        yeast.activate(command.activatedBy) match {
          case Left(domainError) => Future.successful(Left(domainError.message))
          case Right(activatedYeast) => 
            yeastRepository.update(activatedYeast).map(Right(_))
        }
    }
  }

  /**
   * Handler pour désactiver une levure
   */
  def handle(command: DeactivateYeastCommand): Future[Either[String, YeastAggregate]] = {
    val yeastId = YeastId(command.yeastId)
    
    yeastRepository.findById(yeastId).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: ${command.yeastId}"))
      case Some(yeast) =>
        yeast.deactivate(command.reason, command.deactivatedBy) match {
          case Left(domainError) => Future.successful(Left(domainError.message))
          case Right(deactivatedYeast) => 
            yeastRepository.update(deactivatedYeast).map(Right(_))
        }
    }
  }

  /**
   * Handler pour archiver une levure
   */
  def handle(command: ArchiveYeastCommand): Future[Either[String, YeastAggregate]] = {
    val yeastId = YeastId(command.yeastId)
    
    yeastRepository.findById(yeastId).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: ${command.yeastId}"))
      case Some(yeast) =>
        yeast.archive(command.reason, command.archivedBy) match {
          case Left(domainError) => Future.successful(Left(domainError.message))
          case Right(archivedYeast) => 
            yeastRepository.update(archivedYeast).map(Right(_))
        }
    }
  }

  /**
   * Handler pour supprimer (archiver) une levure
   */
  def handle(command: DeleteYeastCommand): Future[Either[String, Unit]] = {
    val archiveCommand = ArchiveYeastCommand(
      yeastId = command.yeastId,
      reason = Some(command.reason),
      archivedBy = command.deletedBy
    )
    
    handle(archiveCommand).map(_.map(_ => ()))
  }

  /**
   * Handler pour mise à jour batch
   */
  def handle(command: CreateYeastsBatchCommand): Future[Either[List[String], List[YeastAggregate]]] = {
    command.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        // Traiter chaque levure individuellement
        val futures = command.yeasts.map(handle)
        
        Future.sequence(futures).map { results =>
          val errors = results.collect { case Left(errs) => errs }.flatten
          val successes = results.collect { case Right(yeast) => yeast }
          
          if (errors.nonEmpty) {
            Left(errors)
          } else {
            Right(successes)
          }
        }
    }
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def buildUpdatedCharacteristics(
    current: YeastCharacteristics,
    command: UpdateYeastCommand
  ): Option[YeastCharacteristics] = {
    
    val hasCharacteristicChanges = command.aromaProfile.isDefined || 
                                  command.flavorProfile.isDefined ||
                                  command.esters.isDefined || 
                                  command.phenols.isDefined || 
                                  command.otherCompounds.isDefined || 
                                  command.notes.isDefined
    
    if (hasCharacteristicChanges) {
      Some(YeastCharacteristics(
        aromaProfile = command.aromaProfile.getOrElse(current.aromaProfile),
        flavorProfile = command.flavorProfile.getOrElse(current.flavorProfile),
        esters = command.esters.getOrElse(current.esters),
        phenols = command.phenols.getOrElse(current.phenols),
        otherCompounds = command.otherCompounds.getOrElse(current.otherCompounds),
        notes = command.notes.orElse(current.notes)
      ))
    } else {
      None
    }
  }
}
