package application.yeasts.queries

import domain.yeasts.model._
import java.util.UUID

/**
 * Queries pour les opérations de lecture sur les levures
 * Pattern CQRS - Read Side
 */

/**
 * Query pour récupérer une levure par ID
 */
case class GetYeastByIdQuery(yeastId: UUID)

/**
 * Query pour récupérer une levure par nom
 */
case class GetYeastByNameQuery(name: String)

/**
 * Query pour recherche par laboratoire et souche
 */
case class GetYeastByLaboratoryAndStrainQuery(
  laboratory: String,
  strain: String
)

/**
 * Query pour liste des levures avec filtres
 */
case class FindYeastsQuery(
  name: Option[String] = None,
  laboratory: Option[String] = None,
  yeastType: Option[String] = None,
  minAttenuation: Option[Int] = None,
  maxAttenuation: Option[Int] = None,
  minTemperature: Option[Int] = None,
  maxTemperature: Option[Int] = None,
  minAlcoholTolerance: Option[Double] = None,
  maxAlcoholTolerance: Option[Double] = None,
  flocculation: Option[String] = None,
  characteristics: List[String] = List.empty,
  status: List[String] = List.empty,
  page: Int = 0,
  size: Int = 20
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (page < 0) errors = "Page doit être >= 0" :: errors
    if (size <= 0 || size > 100) errors = "Size doit être entre 1 et 100" :: errors
    minAttenuation.foreach(a => if (a < 30 || a > 100) errors = "Atténuation min invalide" :: errors)
    maxAttenuation.foreach(a => if (a < 30 || a > 100) errors = "Atténuation max invalide" :: errors)
    minTemperature.foreach(t => if (t < 0 || t > 50) errors = "Température min invalide" :: errors)
    maxTemperature.foreach(t => if (t < 0 || t > 50) errors = "Température max invalide" :: errors)
    minAlcoholTolerance.foreach(a => if (a < 0 || a > 20) errors = "Tolérance alcool min invalide" :: errors)
    maxAlcoholTolerance.foreach(a => if (a < 0 || a > 20) errors = "Tolérance alcool max invalide" :: errors)
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
  
  def toFilter: YeastFilter = {
    val parsedLab = laboratory.flatMap(YeastLaboratory.fromName)
    val parsedType = yeastType.flatMap(YeastType.fromName)
    val parsedFloc = flocculation.flatMap(FlocculationLevel.fromName)
    val parsedStatuses = status.flatMap(YeastStatus.fromName)
    
    YeastFilter(
      name = name,
      laboratory = parsedLab,
      yeastType = parsedType,
      minAttenuation = minAttenuation,
      maxAttenuation = maxAttenuation,
      minTemperature = minTemperature,
      maxTemperature = maxTemperature,
      minAlcoholTolerance = minAlcoholTolerance,
      maxAlcoholTolerance = maxAlcoholTolerance,
      flocculation = parsedFloc,
      characteristics = characteristics,
      status = parsedStatuses,
      page = page,
      size = size
    )
  }
}

/**
 * Query pour recherche par type de levure
 */
case class FindYeastsByTypeQuery(
  yeastType: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, YeastType] = {
    YeastType.parse(yeastType)
  }
}

/**
 * Query pour recherche par laboratoire
 */
case class FindYeastsByLaboratoryQuery(
  laboratory: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, YeastLaboratory] = {
    YeastLaboratory.parse(laboratory)
  }
}

/**
 * Query pour recherche par statut
 */
case class FindYeastsByStatusQuery(
  status: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, YeastStatus] = {
    YeastStatus.parse(status)
  }
}

/**
 * Query pour recherche par plage d'atténuation
 */
case class FindYeastsByAttenuationRangeQuery(
  minAttenuation: Int,
  maxAttenuation: Int
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (minAttenuation < 30 || minAttenuation > 100) errors = "Atténuation min invalide (30-100)" :: errors
    if (maxAttenuation < 30 || maxAttenuation > 100) errors = "Atténuation max invalide (30-100)" :: errors
    if (minAttenuation > maxAttenuation) errors = "Atténuation min > max" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Query pour recherche par plage de température
 */
case class FindYeastsByTemperatureRangeQuery(
  minTemperature: Int,
  maxTemperature: Int
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (minTemperature < 0 || minTemperature > 50) errors = "Température min invalide (0-50)" :: errors
    if (maxTemperature < 0 || maxTemperature > 50) errors = "Température max invalide (0-50)" :: errors
    if (minTemperature > maxTemperature) errors = "Température min > max" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Query pour recherche par caractéristiques
 */
case class FindYeastsByCharacteristicsQuery(
  characteristics: List[String]
) {
  def validate: Either[String, Unit] = {
    if (characteristics.isEmpty || characteristics.forall(_.trim.isEmpty)) {
      Left("Au moins une caractéristique requise")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour recherche textuelle
 */
case class SearchYeastsQuery(
  query: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, Unit] = {
    if (query.trim.isEmpty) {
      Left("Terme de recherche requis")
    } else if (query.trim.length < 2) {
      Left("Terme de recherche trop court (min 2 caractères)")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour statistiques par statut
 */
case class GetYeastStatsQuery()

/**
 * Query pour statistiques par laboratoire
 */
case class GetYeastStatsByLaboratoryQuery()

/**
 * Query pour statistiques par type
 */
case class GetYeastStatsByTypeQuery()

/**
 * Query pour levures populaires
 */
case class GetMostPopularYeastsQuery(limit: Int = 10) {
  def validate: Either[String, Unit] = {
    if (limit <= 0 || limit > 50) {
      Left("Limite doit être entre 1 et 50")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour levures récemment ajoutées
 */
case class GetRecentlyAddedYeastsQuery(limit: Int = 5) {
  def validate: Either[String, Unit] = {
    if (limit <= 0 || limit > 20) {
      Left("Limite doit être entre 1 et 20")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour recommandations de levures
 */
case class GetYeastRecommendationsQuery(
  beerStyle: Option[String] = None,
  targetAbv: Option[Double] = None,
  fermentationTemp: Option[Int] = None,
  desiredCharacteristics: List[String] = List.empty,
  limit: Int = 10
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (limit <= 0 || limit > 20) errors = "Limite doit être entre 1 et 20" :: errors
    targetAbv.foreach(abv => if (abv < 0 || abv > 20) errors = "ABV invalide (0-20)" :: errors)
    fermentationTemp.foreach(temp => if (temp < 0 || temp > 50) errors = "Température invalide (0-50)" :: errors)
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Query pour alternatives à une levure
 */
case class GetYeastAlternativesQuery(
  originalYeastId: UUID,
  reason: String = "unavailable",
  limit: Int = 5
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (limit <= 0 || limit > 10) errors = "Limite doit être entre 1 et 10" :: errors
    if (reason.trim.isEmpty) errors = "Raison requise" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}
