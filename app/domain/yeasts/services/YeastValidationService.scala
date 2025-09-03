package domain.yeasts.services

import domain.yeasts.model._
import domain.shared.NonEmptyString
import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service de validation pour le domaine Yeast
 * CORRECTION: Méthode fromString pour YeastLaboratory
 */
@Singleton
class YeastValidationService @Inject()()(implicit ec: ExecutionContext) {

  def validateYeastCreation(
    name: String,
    strain: String,
    yeastType: String,
    laboratory: String,
    attenuationMin: Double,
    attenuationMax: Double,
    temperatureMin: Double,
    temperatureMax: Double,
    flocculation: String,
    alcoholTolerance: Double
  ): Future[Either[DomainError, ValidatedYeastData]] = {
    
    Future.successful {
      for {
        validName <- NonEmptyString.create(name)
          .left.map(err => DomainError.validation(err))
        validStrain <- YeastStrain.fromString(strain)
          .left.map(err => DomainError.validation(err))
        validType <- YeastType.fromName(yeastType) match {
          case Some(value) => Right(value)
          case None => Left(DomainError.validation(s"Type de levure invalide: $yeastType"))
        }
        validLab <- YeastLaboratory.fromString(laboratory)
          .left.map(err => DomainError.validation(s"Laboratoire invalide: $laboratory - $err"))
        validFloc <- FlocculationLevel.fromName(flocculation) match {
          case Some(value) => Right(value)
          case None => Left(DomainError.validation(s"Niveau de floculation invalide: $flocculation"))
        }
        validAttRange <- AttenuationRange.create(attenuationMin.toInt, attenuationMax.toInt)
          .left.map(err => DomainError.validation(err))
        validTempRange <- FermentationTemp.create(temperatureMin.toInt, temperatureMax.toInt)
          .left.map(err => DomainError.validation(err))
        validAlcTol <- AlcoholTolerance.fromDouble(alcoholTolerance)
          .left.map(err => DomainError.validation(err))
      } yield ValidatedYeastData(
        name = validName,
        strain = validStrain,
        yeastType = validType,
        laboratory = validLab,
        attenuationRange = validAttRange,
        fermentationTemp = validTempRange,
        flocculation = validFloc,
        alcoholTolerance = validAlcTol
      )
    }
  }

  def validateYeastUpdate(
    yeast: YeastAggregate,
    updates: Map[String, Any]
  ): Future[Either[DomainError, YeastAggregate]] = {
    // Validation des mises à jour
    Future.successful(Right(yeast)) // Implémentation simplifiée
  }

  private def createValidatedYeast(
    laboratory: String,
    // autres paramètres...
  ): Either[DomainError, ValidatedYeastData] = {
    // CORRECTION: Utilisation de YeastLaboratory.fromString
    for {
      validLab <- YeastLaboratory.fromString(laboratory).getOrElse(YeastLaboratory.Other) match {
        case lab => Right(lab)
      }
      // autres validations...
    } yield ValidatedYeastData(
      name = NonEmptyString.unsafe("temp"),
      strain = YeastStrain.unsafe("temp"),
      yeastType = YeastType.ALE,
      laboratory = validLab,
      attenuationRange = AttenuationRange.unsafe(75, 85),
      fermentationTemp = FermentationTemp.unsafe(18, 22),
      flocculation = FlocculationLevel.MEDIUM,
      alcoholTolerance = AlcoholTolerance.unsafe(12.0)
    )
  }
}

case class ValidatedYeastData(
  name: NonEmptyString,
  strain: YeastStrain,
  yeastType: YeastType,
  laboratory: YeastLaboratory,
  attenuationRange: AttenuationRange,
  fermentationTemp: FermentationTemp,
  flocculation: FlocculationLevel,
  alcoholTolerance: AlcoholTolerance
)
