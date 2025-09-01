package application.yeasts.utils

import domain.yeasts.model._
import application.yeasts.dtos._
import play.api.libs.json._
import java.util.UUID
import java.time.Instant

/**
 * Utilitaires pour la couche application Yeast
 */
object YeastApplicationUtils {
  
  /**
   * Validation UUID depuis string
   */
  def parseUUID(uuidString: String): Either[String, UUID] = {
    try {
      Right(UUID.fromString(uuidString))
    } catch {
      case _: IllegalArgumentException => Left(s"UUID invalide: $uuidString")
    }
  }
  
  /**
   * Validation page de pagination
   */
  def validatePagination(page: Int, size: Int): Either[List[String], (Int, Int)] = {
    var errors = List.empty[String]
    
    if (page < 0) errors = "Page doit être >= 0" :: errors
    if (size <= 0) errors = "Size doit être > 0" :: errors
    if (size > 100) errors = "Size ne peut pas dépasser 100" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right((page, size))
  }
  
  /**
   * Nettoyage et validation liste de caractéristiques
   */
  def cleanCharacteristics(characteristics: List[String]): List[String] = {
    characteristics
      .filter(_.trim.nonEmpty)
      .map(_.trim.toLowerCase.capitalize)
      .distinct
      .take(20) // Max 20 caractéristiques
  }
  
  /**
   * Génération d'un résumé de levure pour logs
   */
  def yeastSummaryForLog(yeast: YeastAggregate): String = {
    s"Yeast(${yeast.id.asString.take(8)}, ${yeast.name.value}, ${yeast.laboratory.name} ${yeast.strain.value}, ${yeast.status.name})"
  }
  
  /**
   * Conversion safe d'un Map vers JSON
   */
  def mapToJson[K, V](map: Map[K, V])(implicit keyWrites: Writes[K], valueWrites: Writes[V]): JsObject = {
    JsObject(map.map { case (k, v) => 
      Json.toJson(k).as[String] -> Json.toJson(v)
    }.toSeq)
  }
  
  /**
   * Validation et parsing de statut multiples
   */
  def parseStatuses(statusStrings: List[String]): Either[List[String], List[YeastStatus]] = {
    val results = statusStrings.map(YeastStatus.parse)
    val errors = results.collect { case Left(error) => error }
    val successes = results.collect { case Right(status) => status }
    
    if (errors.nonEmpty) Left(errors) else Right(successes)
  }
  
  /**
   * Construction réponse d'erreur standardisée
   */
  def buildErrorResponse(errors: List[String]): YeastErrorResponseDTO = {
    YeastErrorResponseDTO(
      errors = errors,
      timestamp = Instant.now()
    )
  }
  
  /**
   * Construction réponse d'erreur unique
   */
  def buildErrorResponse(error: String): YeastErrorResponseDTO = {
    buildErrorResponse(List(error))
  }
  
  /**
   * Validation basique nom de levure
   */
  def validateYeastName(name: String): Either[String, String] = {
    val trimmed = name.trim
    if (trimmed.isEmpty) {
      Left("Nom de levure requis")
    } else if (trimmed.length < 2) {
      Left("Nom de levure trop court (minimum 2 caractères)")
    } else if (trimmed.length > 100) {
      Left("Nom de levure trop long (maximum 100 caractères)")
    } else {
      Right(trimmed)
    }
  }
  
  /**
   * Validation basique souche de levure
   */
  def validateYeastStrain(strain: String): Either[String, String] = {
    val trimmed = strain.trim.toUpperCase
    if (trimmed.isEmpty) {
      Left("Souche de levure requise")
    } else if (!trimmed.matches("^[A-Z0-9-]{1,20}$")) {
      Left("Format de souche invalide (lettres, chiffres et tirets uniquement)")
    } else {
      Right(trimmed)
    }
  }
  
  /**
   * Helpers pour conversion températures
   */
  def celsiusToFahrenheit(celsius: Int): Int = {
    (celsius * 9/5) + 32
  }
  
  def fahrenheitToCelsius(fahrenheit: Int): Int = {
    ((fahrenheit - 32) * 5/9).round.toInt
  }
  
  /**
   * Formatage display pour plages
   */
  def formatRange(min: Int, max: Int, unit: String): String = {
    if (min == max) s"$min$unit" else s"$min-$max$unit"
  }
  
  /**
   * Génération tags de recherche pour une levure
   */
  def generateSearchTags(yeast: YeastAggregate): List[String] = {
    List(
      yeast.name.value.toLowerCase,
      yeast.laboratory.name.toLowerCase,
      yeast.laboratory.code.toLowerCase,
      yeast.strain.value.toLowerCase,
      yeast.yeastType.name.toLowerCase,
      yeast.flocculation.name.toLowerCase
    ) ++ yeast.characteristics.allCharacteristics.map(_.toLowerCase)
  }
  
  /**
   * Calcul score de pertinence pour recherche textuelle
   */
  def calculateSearchRelevance(yeast: YeastAggregate, query: String): Double = {
    val queryLower = query.toLowerCase
    val searchTags = generateSearchTags(yeast)
    
    var score = 0.0
    
    // Correspondance exacte nom (score max)
    if (yeast.name.value.toLowerCase == queryLower) score += 1.0
    
    // Correspondance début nom
    else if (yeast.name.value.toLowerCase.startsWith(queryLower)) score += 0.8
    
    // Correspondance dans le nom
    else if (yeast.name.value.toLowerCase.contains(queryLower)) score += 0.6
    
    // Correspondance souche exacte
    if (yeast.strain.value.toLowerCase == queryLower) score += 0.7
    
    // Correspondance dans tags de recherche
    val matchingTags = searchTags.count(_.contains(queryLower))
    score += (matchingTags * 0.1)
    
    math.min(1.0, score) // Cap à 1.0
  }
  
  /**
   * Tri des résultats par pertinence
   */
  def sortByRelevance(yeasts: List[YeastAggregate], query: String): List[YeastAggregate] = {
    yeasts
      .map(yeast => (yeast, calculateSearchRelevance(yeast, query)))
      .sortBy(-_._2) // Tri décroissant par score
      .map(_._1)
  }
}
