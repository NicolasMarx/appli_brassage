package application.yeasts.handlers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import application.yeasts.commands.CreateYeastCommand
import domain.yeasts.model._
import domain.yeasts.repositories.YeastWriteRepository
import domain.yeasts.services.YeastDomainService
import domain.shared.NonEmptyString

/**
 * Handler pour la création de levures
 * Implémente la logique CQRS avec validation domaine
 */
@Singleton
class CreateYeastCommandHandler @Inject()(
  yeastWriteRepository: YeastWriteRepository,
  yeastDomainService: YeastDomainService
)(implicit ec: ExecutionContext) {

  def handle(command: CreateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    
    // Construction des Value Objects avec validation
    val yeastId = YeastId.generate()
    val buildResult = for {
      yeastName <- YeastName.fromString(command.name)
      strain <- YeastStrain.fromString(command.strain)
      yeastType <- YeastType.parse(command.yeastType)
      laboratory <- YeastLaboratory.parse(command.laboratory)
      attenuationRange <- AttenuationRange.create(command.attenuationMin, command.attenuationMax)
      fermentationTemp <- FermentationTemp.create(command.temperatureMin, command.temperatureMax)
      alcoholTolerance <- AlcoholTolerance.fromDouble(command.alcoholTolerance)
      flocculation <- Right(FlocculationLevel.fromName(command.flocculation)
        .getOrElse(FlocculationLevel.MEDIUM)) // Default fallback
    } yield (yeastId, yeastName, strain, yeastType, laboratory, attenuationRange, fermentationTemp, alcoholTolerance, flocculation)

    buildResult match {
      case Left(error) => Future.successful(Left(List(error)))
      case Right((yeastId, yeastName, strain, yeastType, laboratory, attenuationRange, fermentationTemp, alcoholTolerance, flocculation)) =>
        
        // Construction des caractéristiques
        val characteristics = YeastCharacteristics(
          aromaProfile = command.aromaProfile,
          flavorProfile = command.flavorProfile,
          esters = command.esters,
          phenols = command.phenols,
          otherCompounds = command.otherCompounds,
          notes = command.notes
        )

        // Validation métier via domain service
        yeastDomainService.validateNewYeast(yeastName, strain, laboratory).flatMap {
          case Left(domainErrors) => Future.successful(Left(domainErrors))
          case Right(_) =>
            // Création de l'agrégat
            val yeastAggregate = YeastAggregate.create(
              id = yeastId,
              name = NonEmptyString.unsafe(yeastName.value),
              strain = strain,
              yeastType = yeastType,
              laboratory = laboratory,
              attenuationRange = attenuationRange,
              fermentationTemp = fermentationTemp,
              alcoholTolerance = alcoholTolerance,
              flocculation = flocculation,
              characteristics = characteristics
            )

            // Persistance avec Event Sourcing
            yeastWriteRepository.create(yeastAggregate).map { savedYeast =>
              Right(savedYeast)
            }.recover {
              case ex => Left(List(s"Erreur lors de la persistance: ${ex.getMessage}"))
            }
        }
    }
  }
}