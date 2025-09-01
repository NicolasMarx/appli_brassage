package interfaces.validation.yeasts

import play.api.mvc._
import play.api.libs.json._
import application.yeasts.dtos._
import scala.concurrent.Future

/**
 * Validation HTTP spécialisée pour les endpoints levures
 * Validation côté interface avant passage à l'application
 */
object YeastHttpValidation {
  
  /**
   * Validation paramètres de pagination
   */
  def validatePagination(page: Int, size: Int): Either[List[String], (Int, Int)] = {
    var errors = List.empty[String]
    
    if (page < 0) errors = "Page doit être >= 0" :: errors
    if (size <= 0) errors = "Size doit être > 0" :: errors
    if (size > 100) errors = "Size maximum est 100" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right((page, size))
  }
  
  /**
   * Validation paramètres de recherche
   */
  def validateSearchParams(
    name: Option[String],
    laboratory: Option[String],
    yeastType: Option[String]
  ): Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    name.foreach { n =>
      if (n.trim.isEmpty) errors = "Nom ne peut pas être vide" :: errors
      if (n.length > 100) errors = "Nom trop long (max 100 caractères)" :: errors
    }
    
    laboratory.foreach { lab =>
      if (lab.trim.isEmpty) errors = "Laboratoire ne peut pas être vide" :: errors
    }
    
    yeastType.foreach { yType =>
      if (yType.trim.isEmpty) errors = "Type levure ne peut pas être vide" :: errors
    }
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
  
  /**
   * Validation paramètre de recherche textuelle
   */
  def validateSearchQuery(query: String, minLength: Int = 2): Either[String, String] = {
    val trimmed = query.trim
    if (trimmed.isEmpty) {
      Left("Terme de recherche requis")
    } else if (trimmed.length < minLength) {
      Left(s"Terme de recherche trop court (minimum $minLength caractères)")
    } else if (trimmed.length > 100) {
      Left("Terme de recherche trop long (maximum 100 caractères)")
    } else {
      Right(trimmed)
    }
  }
  
  /**
   * Validation limite pour les listes
   */
  def validateLimit(limit: Int, maxLimit: Int = 50): Either[String, Int] = {
    if (limit <= 0) {
      Left("Limite doit être > 0")
    } else if (limit > maxLimit) {
      Left(s"Limite maximum est $maxLimit")
    } else {
      Right(limit)
    }
  }
  
  /**
   * Validation UUID dans URL
   */
  def validateUUID(uuidString: String): Either[String, String] = {
    try {
      java.util.UUID.fromString(uuidString)
      Right(uuidString)
    } catch {
      case _: IllegalArgumentException => Left(s"Format UUID invalide: $uuidString")
    }
  }
  
  /**
   * Validation format d'export
   */
  def validateExportFormat(format: String): Either[String, String] = {
    format.toLowerCase match {
      case "json" | "csv" => Right(format.toLowerCase)
      case _ => Left("Format d'export non supporté (json, csv)")
    }
  }
  
  /**
   * Validation paramètres ABV
   */
  def validateAbv(abv: Option[Double]): Either[String, Option[Double]] = {
    abv match {
      case Some(value) =>
        if (value < 0.0 || value > 20.0) {
          Left("ABV doit être entre 0% et 20%")
        } else {
          Right(Some(value))
        }
      case None => Right(None)
    }
  }
  
  /**
   * Validation température de fermentation
   */
  def validateFermentationTemp(temp: Option[Int]): Either[String, Option[Int]] = {
    temp match {
      case Some(value) =>
        if (value < 0 || value > 50) {
          Left("Température doit être entre 0°C et 50°C")
        } else {
          Right(Some(value))
        }
      case None => Right(None)
    }
  }
  
  /**
   * Validation JSON body présent
   */
  def validateJsonBody[T](request: Request[JsValue])(implicit reads: Reads[T]): Either[List[String], T] = {
    request.body.validate[T] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) => 
        val errorMessages = errors.flatMap(_._2).map(_.message).toList
        Left(errorMessages)
    }
  }
  
  /**
   * Validation taille batch
   */
  def validateBatchSize[T](items: List[T], maxSize: Int = 50): Either[String, List[T]] = {
    if (items.isEmpty) {
      Left("Batch vide")
    } else if (items.size > maxSize) {
      Left(s"Batch trop volumineux (maximum $maxSize éléments)")
    } else {
      Right(items)
    }
  }
}

/**
 * Filtre de validation pour les actions controller
 */
class YeastValidationFilter extends EssentialFilter {
  def apply(next: EssentialAction) = EssentialAction { request =>
    // Validation générale des headers, content-type, etc.
    validateRequest(request) match {
      case Right(_) => next(request)
      case Left(error) => 
        Future.successful(Results.BadRequest(Json.obj("error" -> error)))
    }
  }
  
  private def validateRequest(request: RequestHeader): Either[String, Unit] = {
    // Validation Content-Type pour les requêtes POST/PUT
    if (List("POST", "PUT", "PATCH").contains(request.method)) {
      request.contentType match {
        case Some("application/json") => Right(())
        case Some(other) => Left(s"Content-Type non supporté: $other (attendu: application/json)")
        case None => Left("Content-Type requis pour les requêtes de modification")
      }
    } else {
      Right(())
    }
  }
}
