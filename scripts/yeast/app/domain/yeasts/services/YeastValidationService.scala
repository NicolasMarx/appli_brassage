package domain.yeasts.services

import domain.yeasts.model._
import domain.shared.DomainError
import javax.inject.Singleton

/**
 * Service de validation spécialisé pour les levures
 * Règles de validation métier centralisées
 */
@Singleton
class YeastValidationService {

  /**
   * Validation complète d'une nouvelle levure
   */
  def validateNewYeast(
    name: String,
    laboratory: String,
    strain: String,
    yeastType: String,
    attenuationMin: Int,
    attenuationMax: Int,
    tempMin: Int,
    tempMax: Int,
    alcoholTolerance: Double,
    flocculation: String,
    aromaProfile: List[String],
    flavorProfile: List[String]
  ): Either[List[ValidationError], ValidatedYeastData] = {
    
    val validations = List(
      validateYeastName(name),
      validateLaboratory(laboratory),
      validateStrain(strain),
      validateYeastType(yeastType),
      validateAttenuation(attenuationMin, attenuationMax),
      validateTemperature(tempMin, tempMax),
      validateAlcoholTolerance(alcoholTolerance),
      validateFlocculation(flocculation),
      validateCharacteristics(aromaProfile, flavorProfile)
    )
    
    val errors = validations.collect { case Left(error) => error }
    if (errors.nonEmpty) {
      Left(errors)
    } else {
      val validatedData = ValidatedYeastData(
        name = validations(0).value.asInstanceOf[YeastName],
        laboratory = validations(1).value.asInstanceOf[YeastLaboratory],
        strain = validations(2).value.asInstanceOf[YeastStrain],
        yeastType = validations(3).value.asInstanceOf[YeastType],
        attenuation = validations(4).value.asInstanceOf[AttenuationRange],
        temperature = validations(5).value.asInstanceOf[FermentationTemp],
        alcoholTolerance = validations(6).value.asInstanceOf[AlcoholTolerance],
        flocculation = validations(7).value.asInstanceOf[FlocculationLevel],
        characteristics = validations(8).value.asInstanceOf[YeastCharacteristics]
      )
      Right(validatedData)
    }
  }

  /**
   * Validation mise à jour partielle
   */
  def validateYeastUpdate(updates: Map[String, Any]): Either[List[ValidationError], Map[String, Any]] = {
    val validatedUpdates = updates.map {
      case ("name", value: String) => 
        validateYeastName(value).map("name" -> _)
      case ("laboratory", value: String) => 
        validateLaboratory(value).map("laboratory" -> _)
      case ("strain", value: String) => 
        validateStrain(value).map("strain" -> _)
      case ("yeastType", value: String) => 
        validateYeastType(value).map("yeastType" -> _)
      case ("alcoholTolerance", value: Double) => 
        validateAlcoholTolerance(value).map("alcoholTolerance" -> _)
      case ("flocculation", value: String) => 
        validateFlocculation(value).map("flocculation" -> _)
      case (key, value) => 
        Left(ValidationError(s"Champ non supporté pour mise à jour: $key"))
    }.toList
    
    val errors = validatedUpdates.collect { case Left(error) => error }
    if (errors.nonEmpty) {
      Left(errors)
    } else {
      Right(validatedUpdates.collect { case Right((key, value)) => key -> value }.toMap)
    }
  }

  /**
   * Validation des plages de recherche
   */
  def validateSearchRanges(
    minAttenuation: Option[Int],
    maxAttenuation: Option[Int],
    minTemperature: Option[Int], 
    maxTemperature: Option[Int],
    minAlcohol: Option[Double],
    maxAlcohol: Option[Double]
  ): Either[List[ValidationError], Unit] = {
    
    val validations = List(
      validateOptionalRange("atténuation", minAttenuation, maxAttenuation, 30, 100),
      validateOptionalRange("température", minTemperature, maxTemperature, 0, 50),
      validateOptionalRange("alcool", minAlcohol, maxAlcohol, 0.0, 20.0)
    )
    
    val errors = validations.collect { case Left(error) => error }
    if (errors.nonEmpty) Left(errors) else Right(())
  }

  // ==========================================================================
  // VALIDATIONS INDIVIDUELLES
  // ==========================================================================

  private def validateYeastName(name: String): Either[ValidationError, YeastName] = {
    YeastName.fromString(name).left.map(ValidationError)
  }

  private def validateLaboratory(laboratory: String): Either[ValidationError, YeastLaboratory] = {
    YeastLaboratory.parse(laboratory).left.map(ValidationError)
  }

  private def validateStrain(strain: String): Either[ValidationError, YeastStrain] = {
    YeastStrain.fromString(strain).left.map(ValidationError)
  }

  private def validateYeastType(yeastType: String): Either[ValidationError, YeastType] = {
    YeastType.parse(yeastType).left.map(ValidationError)
  }

  private def validateAttenuation(min: Int, max: Int): Either[ValidationError, AttenuationRange] = {
    try {
      Right(AttenuationRange(min, max))
    } catch {
      case e: IllegalArgumentException => Left(ValidationError(e.getMessage))
    }
  }

  private def validateTemperature(min: Int, max: Int): Either[ValidationError, FermentationTemp] = {
    try {
      Right(FermentationTemp(min, max))
    } catch {
      case e: IllegalArgumentException => Left(ValidationError(e.getMessage))
    }
  }

  private def validateAlcoholTolerance(tolerance: Double): Either[ValidationError, AlcoholTolerance] = {
    try {
      Right(AlcoholTolerance(tolerance))
    } catch {
      case e: IllegalArgumentException => Left(ValidationError(e.getMessage))
    }
  }

  private def validateFlocculation(flocculation: String): Either[ValidationError, FlocculationLevel] = {
    FlocculationLevel.parse(flocculation).left.map(ValidationError)
  }

  private def validateCharacteristics(
    aromaProfile: List[String], 
    flavorProfile: List[String]
  ): Either[ValidationError, YeastCharacteristics] = {
    try {
      Right(YeastCharacteristics(
        aromaProfile = aromaProfile.filter(_.trim.nonEmpty),
        flavorProfile = flavorProfile.filter(_.trim.nonEmpty),
        esters = List.empty,
        phenols = List.empty,
        otherCompounds = List.empty
      ))
    } catch {
      case e: IllegalArgumentException => Left(ValidationError(e.getMessage))
    }
  }

  private def validateOptionalRange[T: Numeric](
    fieldName: String,
    min: Option[T], 
    max: Option[T],
    absoluteMin: T,
    absoluteMax: T
  ): Either[ValidationError, Unit] = {
    val num = implicitly[Numeric[T]]
    
    (min, max) match {
      case (Some(minVal), Some(maxVal)) =>
        if (num.gt(minVal, maxVal)) {
          Left(ValidationError(s"$fieldName: minimum ($minVal) > maximum ($maxVal)"))
        } else if (num.lt(minVal, absoluteMin) || num.gt(maxVal, absoluteMax)) {
          Left(ValidationError(s"$fieldName: valeurs hors plage autorisée [$absoluteMin, $absoluteMax]"))
        } else {
          Right(())
        }
      case (Some(minVal), None) =>
        if (num.lt(minVal, absoluteMin) || num.gt(minVal, absoluteMax)) {
          Left(ValidationError(s"$fieldName minimum hors plage autorisée [$absoluteMin, $absoluteMax]"))
        } else {
          Right(())
        }
      case (None, Some(maxVal)) =>
        if (num.lt(maxVal, absoluteMin) || num.gt(maxVal, absoluteMax)) {
          Left(ValidationError(s"$fieldName maximum hors plage autorisée [$absoluteMin, $absoluteMax]"))
        } else {
          Right(())
        }
      case (None, None) => Right(())
    }
  }
}

/**
 * Erreur de validation
 */
case class ValidationError(message: String)

/**
 * Données de levure validées
 */
case class ValidatedYeastData(
  name: YeastName,
  laboratory: YeastLaboratory,
  strain: YeastStrain,
  yeastType: YeastType,
  attenuation: AttenuationRange,
  temperature: FermentationTemp,
  alcoholTolerance: AlcoholTolerance,
  flocculation: FlocculationLevel,
  characteristics: YeastCharacteristics
)
