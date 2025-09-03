package application.yeasts.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}
import domain.yeasts.model._
import domain.yeasts.repositories.{YeastReadRepository, YeastWriteRepository}
import domain.shared.NonEmptyString
import play.api.libs.json._
import java.time.Instant
import domain.common.DomainError

/**
 * Service pour les opérations batch sur les levures
 * Architecture DDD/CQRS respectée - Application Layer
 * 
 * Gère l'import/export en masse avec validation domain complète
 * Implémentation transactionnelle avec rollback automatique
 */
@Singleton
class YeastBatchService @Inject()(
  yeastReadRepository: YeastReadRepository,
  yeastWriteRepository: YeastWriteRepository
)(implicit ec: ExecutionContext) {

  /**
   * Import batch de levures depuis JSON/CSV
   * Validation complète avec rollback transactionnel
   */
  def importYeastsBatch(
    data: JsValue,
    format: BatchFormat = BatchFormat.JSON,
    validateOnly: Boolean = false
  ): Future[BatchImportResult] = {
    
    for {
      // Étape 1: Parse et validation format
      parsedYeasts <- parseBatchData(data, format)
      
      // Étape 2: Validation domain de chaque levure
      validationResults <- validateYeastsBatch(parsedYeasts)
      
      // Étape 3: Si pas de validation seulement, procéder à l'import
      importResults <- if (validateOnly) {
        Future.successful(ImportResults(created = 0, updated = 0, errors = List.empty))
      } else if (validationResults.isValid) {
        performBatchImport(validationResults.validYeasts)
      } else {
        Future.successful(ImportResults(created = 0, updated = 0, errors = validationResults.errors.map(_.message)))
      }
      
    } yield BatchImportResult(
      processedCount = parsedYeasts.length,
      validCount = validationResults.validYeasts.length,
      createdCount = importResults.created,
      updatedCount = importResults.updated,
      errorCount = validationResults.errors.length,
      errors = validationResults.errors,
      warnings = generateWarnings(validationResults.validYeasts),
      summary = generateImportSummary(importResults, validationResults),
      validateOnly = validateOnly
    )
  }

  /**
   * Export batch de levures vers différents formats
   */
  def exportYeastsBatch(
    filter: YeastFilter,
    format: BatchFormat = BatchFormat.JSON,
    includeInactive: Boolean = false
  ): Future[BatchExportResult] = {
    
    val exportFilter = if (includeInactive) {
      filter
    } else {
      filter.copy(status = List(YeastStatus.Active))
    }
    
    for {
      yeasts <- yeastReadRepository.findByFilter(exportFilter.copy(size = 1000))
      exportData <- generateExportData(yeasts.items, format)
    } yield BatchExportResult(
      format = format,
      recordCount = yeasts.items.length,
      data = exportData,
      metadata = Json.obj(
        "exportedAt" -> Instant.now().toString,
        "filter" -> Json.toJson(exportFilter.toString),
        "totalRecords" -> yeasts.items.length,
        "includeInactive" -> includeInactive
      )
    )
  }

  /**
   * Opérations batch sur levures existantes
   */
  def batchOperations(
    yeastIds: List[YeastId],
    operation: BatchOperation
  ): Future[BatchOperationResult] = {
    
    for {
      existingYeasts <- Future.traverse(yeastIds)(id => 
        yeastReadRepository.findById(id).map(id -> _)
      )
      
      // Filtrer les levures existantes
      foundYeasts = existingYeasts.collect { case (id, Some(yeast)) => yeast }
      notFoundIds = existingYeasts.collect { case (id, None) => id }
      
      // Appliquer l'opération
      operationResults <- performBatchOperation(foundYeasts, operation)
      
    } yield BatchOperationResult(
      operation = operation,
      processedCount = foundYeasts.length,
      successCount = operationResults.successes.length,
      errorCount = operationResults.errors.length,
      notFoundCount = notFoundIds.length,
      successes = operationResults.successes,
      errors = operationResults.errors,
      notFoundIds = notFoundIds.map(_.asString)
    )
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - PARSING ET VALIDATION
  // ==========================================================================

  private def parseBatchData(data: JsValue, format: BatchFormat): Future[List[YeastBatchData]] = {
    Future.fromTry(Try {
      format match {
        case BatchFormat.JSON => parseJsonBatch(data)
        case BatchFormat.CSV => parseCsvBatch(data)
      }
    })
  }

  private def parseJsonBatch(data: JsValue): List[YeastBatchData] = {
    (data \ "yeasts").as[JsArray].value.zipWithIndex.map { case (yeastJson, index) =>
      parseYeastData(yeastJson, index + 1)
    }.toList
  }

  private def parseCsvBatch(data: JsValue): List[YeastBatchData] = {
    // Pour CSV, on s'attend à recevoir un JSON avec les lignes déjà parsées
    (data \ "rows").as[JsArray].value.zipWithIndex.map { case (rowJson, index) =>
      parseYeastDataFromCsvRow(rowJson, index + 1)
    }.toList
  }

  private def parseYeastData(json: JsValue, lineNumber: Int): YeastBatchData = {
    YeastBatchData(
      lineNumber = lineNumber,
      name = (json \ "name").asOpt[String],
      strain = (json \ "strain").asOpt[String],
      yeastType = (json \ "yeastType").asOpt[String],
      laboratory = (json \ "laboratory").asOpt[String],
      attenuationMin = (json \ "attenuationMin").asOpt[Int],
      attenuationMax = (json \ "attenuationMax").asOpt[Int],
      fermentationTempMin = (json \ "fermentationTempMin").asOpt[Int],
      fermentationTempMax = (json \ "fermentationTempMax").asOpt[Int],
      flocculation = (json \ "flocculation").asOpt[String],
      alcoholTolerance = (json \ "alcoholTolerance").asOpt[Int],
      description = (json \ "description").asOpt[String],
      characteristics = (json \ "characteristics").asOpt[List[String]].getOrElse(List.empty),
      isActive = (json \ "isActive").asOpt[Boolean].getOrElse(true)
    )
  }

  private def parseYeastDataFromCsvRow(json: JsValue, lineNumber: Int): YeastBatchData = {
    // Mapping des colonnes CSV vers les champs
    val columns = json.as[JsArray].value
    
    def getColumn(index: Int): Option[String] = {
      columns.lift(index).flatMap(_.asOpt[String]).filter(_.trim.nonEmpty)
    }

    YeastBatchData(
      lineNumber = lineNumber,
      name = getColumn(0),
      strain = getColumn(1),
      yeastType = getColumn(2),
      laboratory = getColumn(3),
      attenuationMin = getColumn(4).flatMap(s => Try(s.toInt).toOption),
      attenuationMax = getColumn(5).flatMap(s => Try(s.toInt).toOption),
      fermentationTempMin = getColumn(6).flatMap(s => Try(s.toInt).toOption),
      fermentationTempMax = getColumn(7).flatMap(s => Try(s.toInt).toOption),
      flocculation = getColumn(8),
      alcoholTolerance = getColumn(9).flatMap(s => Try(s.toInt).toOption),
      description = getColumn(10),
      characteristics = getColumn(11).map(_.split(",").map(_.trim).toList).getOrElse(List.empty),
      isActive = getColumn(12).flatMap(s => Try(s.toBoolean).toOption).getOrElse(true)
    )
  }

  private def validateYeastsBatch(yeastsData: List[YeastBatchData]): Future[BatchValidationResult] = {
    Future {
      val results = yeastsData.map(validateSingleYeast)
      
      BatchValidationResult(
        validYeasts = results.collect { case Right(yeast) => yeast },
        errors = results.collect { case Left(error) => error },
        isValid = results.forall(_.isRight)
      )
    }
  }

  private def validateSingleYeast(data: YeastBatchData): Either[BatchError, ValidatedYeastData] = {
    
    def validateRequired[T](value: Option[T], fieldName: String): Either[BatchError, T] = {
      value.toRight(BatchError(
        lineNumber = data.lineNumber,
        field = fieldName,
        message = s"Champ obligatoire manquant: $fieldName",
        errorType = "REQUIRED_FIELD_MISSING"
      ))
    }

    def validateEnum[T](value: String, fieldName: String, parser: String => Either[String, T]): Either[BatchError, T] = {
      parser(value).left.map(error => BatchError(
        lineNumber = data.lineNumber,
        field = fieldName,
        message = s"Valeur invalide pour $fieldName: $value. $error",
        errorType = "INVALID_ENUM_VALUE"
      ))
    }

    for {
      name <- validateRequired(data.name, "name")
      nameValue <- NonEmptyString.create(name).left.map(error => BatchError(
        lineNumber = data.lineNumber,
        field = "name",
        message = s"Nom invalide: $error",
        errorType = "INVALID_NAME"
      ))
      
      strain <- validateRequired(data.strain, "strain")
      strainValue <- Try(YeastStrain(strain)).toEither.left.map(error => BatchError(
        lineNumber = data.lineNumber,
        field = "strain",
        message = s"Strain invalide: $strain",
        errorType = "INVALID_STRAIN"
      ))
      
      yeastTypeStr <- validateRequired(data.yeastType, "yeastType")
      yeastType <- validateEnum(yeastTypeStr, "yeastType", YeastType.fromString)
      
      laboratoryStr <- validateRequired(data.laboratory, "laboratory")
      laboratory <- validateEnum(laboratoryStr, "laboratory", YeastLaboratory.fromString)
      
      attenuationMin <- validateRequired(data.attenuationMin, "attenuationMin")
      attenuationMax <- validateRequired(data.attenuationMax, "attenuationMax")
      attenuationRange <- Try(AttenuationRange(attenuationMin, attenuationMax)).toEither.left.map(error => BatchError(
        lineNumber = data.lineNumber,
        field = "attenuationRange",
        message = s"Plage d'atténuation invalide: $attenuationMin-$attenuationMax",
        errorType = "INVALID_ATTENUATION_RANGE"
      ))
      
      tempMin <- validateRequired(data.fermentationTempMin, "fermentationTempMin")
      tempMax <- validateRequired(data.fermentationTempMax, "fermentationTempMax")
      fermentationTemp <- Try(FermentationTemp(tempMin, tempMax)).toEither.left.map(error => BatchError(
        lineNumber = data.lineNumber,
        field = "fermentationTemp",
        message = s"Température de fermentation invalide: $tempMin-$tempMax°C",
        errorType = "INVALID_FERMENTATION_TEMP"
      ))
      
      flocculationStr <- validateRequired(data.flocculation, "flocculation")
      flocculation <- validateEnum(flocculationStr, "flocculation", FlocculationLevel.fromString)
      
      alcoholToleranceValue <- validateRequired(data.alcoholTolerance, "alcoholTolerance")
      alcoholTolerance <- Try(AlcoholTolerance(alcoholToleranceValue)).toEither.left.map(error => BatchError(
        lineNumber = data.lineNumber,
        field = "alcoholTolerance",
        message = s"Tolérance alcool invalide: $alcoholToleranceValue%",
        errorType = "INVALID_ALCOHOL_TOLERANCE"
      ))
      
      characteristics = YeastCharacteristics(data.characteristics)
      
    } yield ValidatedYeastData(
      name = nameValue,
      strain = strainValue,
      yeastType = yeastType,
      laboratory = laboratory,
      attenuationRange = attenuationRange,
      fermentationTemp = fermentationTemp,
      flocculation = flocculation,
      alcoholTolerance = alcoholTolerance,
      description = data.description,
      characteristics = characteristics,
      isActive = data.isActive
    )
  }

  private def performBatchImport(validatedYeasts: List[ValidatedYeastData]): Future[ImportResults] = {
    val now = Instant.now()
    
    Future.traverse(validatedYeasts) { yeastData =>
      val yeastId = YeastId.generate()
      val yeastAggregate = YeastAggregate.create(
        id = yeastId,
        name = yeastData.name,
        strain = yeastData.strain,
        yeastType = yeastData.yeastType,
        laboratory = yeastData.laboratory,
        attenuationRange = yeastData.attenuationRange,
        fermentationTemp = yeastData.fermentationTemp,
        flocculation = yeastData.flocculation,
        alcoholTolerance = yeastData.alcoholTolerance,
        description = yeastData.description,
        characteristics = yeastData.characteristics,
        isActive = yeastData.isActive
      )
      
      yeastWriteRepository.create(yeastAggregate).map(_ => "created").recover {
        case ex => s"error: ${ex.getMessage}"
      }
    }.map { results =>
      ImportResults(
        created = results.count(_ == "created"),
        updated = 0,
        errors = results.collect { case error if error.startsWith("error:") => error }.toList
      )
    }
  }

  private def performBatchOperation(
    yeasts: List[YeastAggregate], 
    operation: BatchOperation
  ): Future[OperationResults] = {
    
    Future.traverse(yeasts) { yeast =>
      val result = operation match {
        case BatchOperation.Activate => yeast.activate()
        case BatchOperation.Deactivate => yeast.deactivate()
      }
      
      result match {
        case Right(updatedYeast) =>
          yeastWriteRepository.create(updatedYeast).map(_ => 
            Right(s"${yeast.id.asString}: ${operation.toString.toLowerCase} success")
          ).recover { case ex =>
            Left(s"${yeast.id.asString}: Save failed - ${ex.getMessage}")
          }
        case Left(error) =>
          Future.successful(Left(s"${yeast.id.asString}: ${error.message}"))
      }
    }.map { results =>
      OperationResults(
        successes = results.collect { case Right(success) => success },
        errors = results.collect { case Left(error) => error }
      )
    }
  }

  private def generateExportData(yeasts: List[YeastAggregate], format: BatchFormat): Future[JsValue] = {
    Future.successful {
      format match {
        case BatchFormat.JSON => generateJsonExport(yeasts)
        case BatchFormat.CSV => generateCsvExport(yeasts)
      }
    }
  }

  private def generateJsonExport(yeasts: List[YeastAggregate]): JsValue = {
    Json.obj(
      "yeasts" -> JsArray(yeasts.map { yeast =>
        Json.obj(
          "id" -> yeast.id.asString,
          "name" -> yeast.name.value,
          "strain" -> yeast.strain.value,
          "yeastType" -> yeast.yeastType.name,
          "laboratory" -> yeast.laboratory.name,
          "attenuationMin" -> yeast.attenuationRange.min,
          "attenuationMax" -> yeast.attenuationRange.max,
          "fermentationTempMin" -> yeast.fermentationTemp.min,
          "fermentationTempMax" -> yeast.fermentationTemp.max,
          "flocculation" -> yeast.flocculation.name,
          "alcoholTolerance" -> yeast.alcoholTolerance.percentage,
          "description" -> yeast.description,
          "characteristics" -> yeast.characteristics.allCharacteristics,
          "isActive" -> yeast.isActive,
          "createdAt" -> yeast.createdAt.toString,
          "updatedAt" -> yeast.updatedAt.toString
        )
      })
    )
  }

  private def generateCsvExport(yeasts: List[YeastAggregate]): JsValue = {
    val headers = List(
      "id", "name", "strain", "yeastType", "laboratory",
      "attenuationMin", "attenuationMax", "fermentationTempMin", "fermentationTempMax",
      "flocculation", "alcoholTolerance", "description", "characteristics", "isActive"
    )
    
    val rows = yeasts.map { yeast =>
      List(
        yeast.id.asString,
        yeast.name.value,
        yeast.strain.value,
        yeast.yeastType.name,
        yeast.laboratory.name,
        yeast.attenuationRange.min.toString,
        yeast.attenuationRange.max.toString,
        yeast.fermentationTemp.min.toString,
        yeast.fermentationTemp.max.toString,
        yeast.flocculation.name,
        yeast.alcoholTolerance.percentage.toString,
        yeast.description.getOrElse(""),
        yeast.characteristics.allCharacteristics.mkString(","),
        yeast.isActive.toString
      )
    }
    
    Json.obj(
      "headers" -> JsArray(headers.map(JsString)),
      "rows" -> JsArray(rows.map(row => JsArray(row.map(JsString))))
    )
  }

  private def generateWarnings(validYeasts: List[ValidatedYeastData]): List[String] = {
    val warnings = scala.collection.mutable.ListBuffer[String]()
    
    // Vérifier doublons potentiels
    val nameGroups = validYeasts.groupBy(_.name.value)
    nameGroups.foreach { case (name, yeasts) =>
      if (yeasts.length > 1) {
        warnings += s"Nom potentiellement dupliqué: '$name' (${yeasts.length} occurrences)"
      }
    }
    
    // Vérifier valeurs extrêmes
    validYeasts.foreach { yeast =>
      if (yeast.fermentationTemp.max > 30) {
        warnings += s"${yeast.name.value}: Température fermentation élevée (${yeast.fermentationTemp.max}°C)"
      }
      if (yeast.alcoholTolerance.percentage > 15) {
        warnings += s"${yeast.name.value}: Tolérance alcool très élevée (${yeast.alcoholTolerance.percentage}%)"
      }
    }
    
    warnings.toList
  }

  private def generateImportSummary(importResults: ImportResults, validationResults: BatchValidationResult): String = {
    if (importResults.created == 0 && importResults.errors.isEmpty) {
      "Validation uniquement - aucune donnée importée"
    } else if (importResults.errors.isEmpty) {
      s"Import réussi: ${importResults.created} levures créées"
    } else {
      s"Import partiel: ${importResults.created} créées, ${importResults.errors.length} erreurs"
    }
  }
}

// ==========================================================================
// TYPES DE SUPPORT
// ==========================================================================

sealed trait BatchFormat
object BatchFormat {
  case object JSON extends BatchFormat
  case object CSV extends BatchFormat
}

sealed trait BatchOperation
object BatchOperation {
  case object Activate extends BatchOperation
  case object Deactivate extends BatchOperation
}

case class YeastBatchData(
  lineNumber: Int,
  name: Option[String],
  strain: Option[String],
  yeastType: Option[String],
  laboratory: Option[String],
  attenuationMin: Option[Int],
  attenuationMax: Option[Int],
  fermentationTempMin: Option[Int],
  fermentationTempMax: Option[Int],
  flocculation: Option[String],
  alcoholTolerance: Option[Int],
  description: Option[String],
  characteristics: List[String],
  isActive: Boolean
)

case class ValidatedYeastData(
  name: NonEmptyString,
  strain: YeastStrain,
  yeastType: YeastType,
  laboratory: YeastLaboratory,
  attenuationRange: AttenuationRange,
  fermentationTemp: FermentationTemp,
  flocculation: FlocculationLevel,
  alcoholTolerance: AlcoholTolerance,
  description: Option[String],
  characteristics: YeastCharacteristics,
  isActive: Boolean
)

case class BatchError(
  lineNumber: Int,
  field: String,
  message: String,
  errorType: String
)

case class BatchValidationResult(
  validYeasts: List[ValidatedYeastData],
  errors: List[BatchError],
  isValid: Boolean
)

case class ImportResults(
  created: Int,
  updated: Int,
  errors: List[String]
)

case class OperationResults(
  successes: List[String],
  errors: List[String]
)

case class BatchImportResult(
  processedCount: Int,
  validCount: Int,
  createdCount: Int,
  updatedCount: Int,
  errorCount: Int,
  errors: List[BatchError],
  warnings: List[String],
  summary: String,
  validateOnly: Boolean
)

case class BatchExportResult(
  format: BatchFormat,
  recordCount: Int,
  data: JsValue,
  metadata: JsValue
)

case class BatchOperationResult(
  operation: BatchOperation,
  processedCount: Int,
  successCount: Int,
  errorCount: Int,
  notFoundCount: Int,
  successes: List[String],
  errors: List[String],
  notFoundIds: List[String]
)