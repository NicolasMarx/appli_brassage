package application.commands.admin.malts.handlers

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import domain.malts.model._
import domain.shared.NonEmptyString
import application.commands.admin.malts.CreateMaltCommand
import domain.common.DomainError

@Singleton
class CreateMaltCommandHandler @Inject()(
  maltReadRepo: MaltReadRepository,
  maltWriteRepo: MaltWriteRepository
)(implicit ec: ExecutionContext) {

  def handle(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    for {
      nameExists <- maltReadRepo.existsByName(NonEmptyString.unsafe(command.name))
      result <- if (nameExists) {
        Future.successful(Left(DomainError.validation(s"Un malt avec le nom '${command.name}' existe déjà")))
      } else {
        createNewMalt(command)
      }
    } yield result
  }

  private def createNewMalt(command: CreateMaltCommand): Future[Either[DomainError, MaltId]] = {
    try {
      val name = NonEmptyString.unsafe(command.name)
      val maltType = MaltType.fromName(command.maltType).getOrElse(MaltType.BASE)
      val ebcColor = EBCColor.unsafe(command.ebcColor)
      val extractionRate = ExtractionRate.unsafe(command.extractionRate)
      val diastaticPower = DiastaticPower.unsafe(command.diastaticPower)
      val source = MaltSource.fromName(command.source).getOrElse(MaltSource.Manual)

      val malt = MaltAggregate.create(
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = command.originCode,
        source = source,
        description = command.description,
        flavorProfiles = command.flavorProfiles
      )

      maltWriteRepo.save(malt).map { _ =>
        Right(malt.id)
      }.recover {
        case ex: Throwable => Left(DomainError.technical(s"Erreur sauvegarde: ${ex.getMessage}"))
      }
    } catch {
      case ex: Throwable =>
        Future.successful(Left(DomainError.technical(s"Erreur création: ${ex.getMessage}")))
    }
  }
}
