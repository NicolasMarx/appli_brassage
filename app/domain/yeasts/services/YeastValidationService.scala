package domain.yeasts.services

import domain.yeasts.model._

case class ValidationError(message: String)

object YeastValidationService {

  // Version simplifiée pour éviter les erreurs complexes
  def validateYeastCreation(
    name: String,
    laboratory: String,
    strain: String,
    yeastType: String,
    attenuationMin: Int,
    attenuationMax: Int,
    temperatureMin: Int,
    temperatureMax: Int,
    alcoholTolerance: Double,
    flocculation: String,
    aromaProfile: List[String] = List.empty,
    flavorProfile: List[String] = List.empty,
    esters: List[String] = List.empty,
    phenols: List[String] = List.empty,
    otherCompounds: List[String] = List.empty
  ): Either[List[ValidationError], ValidYeastData] = {
    
    val errors = scala.collection.mutable.ListBuffer[ValidationError]()
    
    // Validations simples
    if (name.trim.isEmpty) errors += ValidationError("Name cannot be empty")
    if (laboratory.trim.isEmpty) errors += ValidationError("Laboratory cannot be empty")
    if (strain.trim.isEmpty) errors += ValidationError("Strain cannot be empty")
    
    if (attenuationMin < 30 || attenuationMax > 100) {
      errors += ValidationError("Attenuation must be between 30% and 100%")
    }
    if (attenuationMin > attenuationMax) {
      errors += ValidationError("Min attenuation cannot be greater than max")
    }
    
    if (temperatureMin < 0 || temperatureMax > 50) {
      errors += ValidationError("Temperature must be between 0°C and 50°C")
    }
    if (temperatureMin > temperatureMax) {
      errors += ValidationError("Min temperature cannot be greater than max")
    }
    
    if (alcoholTolerance < 0 || alcoholTolerance > 25) {
      errors += ValidationError("Alcohol tolerance must be between 0% and 25%")
    }
    
    if (errors.nonEmpty) {
      Left(errors.toList)
    } else {
      // Créer les Value Objects de manière sécurisée
      val characteristics = YeastCharacteristics(
        aromaProfile = aromaProfile,
        flavorProfile = flavorProfile,
        esters = esters,
        phenols = phenols,
        otherCompounds = otherCompounds,
        notes = None  // Paramètre manquant
      )
      
      Right(ValidYeastData(
        name = YeastName.unsafe(name),
        laboratory = YeastLaboratory.fromString(laboratory).getOrElse(YeastLaboratory.Other),
        strain = YeastStrain.unsafe(strain),
        yeastType = YeastType.fromString(yeastType).getOrElse(YeastType.Ale),
        attenuation = AttenuationRange.unsafe(attenuationMin, attenuationMax),
        temperature = FermentationTemp.unsafe(temperatureMin, temperatureMax),
        alcoholTolerance = AlcoholTolerance.unsafe(alcoholTolerance),
        flocculation = FlocculationLevel.fromString(flocculation).getOrElse(FlocculationLevel.Medium),
        characteristics = characteristics
      ))
    }
  }
}

case class ValidYeastData(
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
