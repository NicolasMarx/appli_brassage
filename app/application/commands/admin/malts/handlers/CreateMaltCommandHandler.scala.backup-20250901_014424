package application.commands.admin.malts.handlers

import application.commands.admin.malts.CreateMaltCommand
import domain.malts.model._
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.shared._
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour la création de malts (version corrigée)
 */
@Singleton
class CreateMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    command.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validCommand) => processValidCommand(validCommand)
    }
  }

  private def processValidCommand(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    for {
      nameExists <- maltReadRepo.existsByName(command.name)
      result <- if (nameExists) {
        Future.successful(Left(DomainError.conflict("Un malt avec ce nom existe déjà", "name")))
      } else {
        createMalt(command)
      }
    } yield result
  }

  private def createMalt(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    // Création des Value Objects avec conversion de types
    val valueObjectsResult = for {
      name <- NonEmptyString.create(command.name)
      maltType <- MaltType.fromName(command.maltType).toRight(s"Type de malt invalide: ${command.maltType}")
      ebcColor <- EBCColor(command.ebcColor)
      extractionRate <- ExtractionRate(command.extractionRate)
      diastaticPower <- DiastaticPower(command.diastaticPower) // Conversion Double -> DiastaticPower
      source <- MaltSource.fromName(command.source).toRight(s"Source invalide: ${command.source}") // Conversion String -> MaltSource
    } yield (name, maltType, ebcColor, extractionRate, diastaticPower, source)

    valueObjectsResult match {
      case Left(error) =>
        Future.successful(Left(DomainError.validation(error)))
      case Right((name, maltType, ebcColor, extractionRate, diastaticPower, source)) =>
        createMaltAggregate(name, maltType, ebcColor, extractionRate, diastaticPower, source, command)
    }
  }

  private def createMaltAggregate(
    name: NonEmptyString,
    maltType: MaltType,
    ebcColor: EBCColor,
    extractionRate: ExtractionRate,
    diastaticPower: DiastaticPower,
    source: MaltSource,
    command: CreateMaltCommand
  ): Future[Either[DomainError, MaltId]] = {
    
    MaltAggregate.create(
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      originCode = command.originCode,
      source = source,
      description = command.description,
      flavorProfiles = command.flavorProfiles.filter(_.trim.nonEmpty).distinct
    ) match {
      case Left(domainError) =>
        Future.successful(Left(domainError))
      case Right(maltAggregate) =>
        maltWriteRepo.create(maltAggregate).map { _ =>
          Right(maltAggregate.id)
        }.recover {
          case ex: Exception =>
            Left(DomainError.validation(s"Erreur lors de la création: ${ex.getMessage}"))
        }
    }
  }
}
