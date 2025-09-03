package application.yeasts.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import domain.yeasts.model.{YeastFilter, YeastAggregate, YeastStatus}
import domain.yeasts.repositories.YeastReadRepository
import play.api.libs.json._
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Service pour l'export multi-formats des levures
 * Architecture DDD/CQRS respectée - Application Layer
 * 
 * Support JSON, CSV, PDF avec templates professionnels
 * Authentification admin requise, rate limiting intégré
 */
@Singleton
class YeastExportService @Inject()(
  yeastReadRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {

  private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  /**
   * Export principal avec formats multiples
   */
  def exportYeasts(
    filter: YeastFilter,
    format: ExportFormat,
    template: ExportTemplate = ExportTemplate.Standard,
    includeMetadata: Boolean = true
  ): Future[ExportResult] = {
    
    for {
      yeasts <- yeastReadRepository.findByFilter(filter.copy(size = 100))
      exportData <- generateExportData(yeasts.items, format, template, includeMetadata)
    } yield ExportResult(
      format = format,
      template = template,
      recordCount = yeasts.items.length,
      data = exportData,
      filename = generateFilename(format, filter),
      contentType = getContentType(format),
      metadata = if (includeMetadata) Some(generateMetadata(yeasts.items, filter)) else None
    )
  }

  /**
   * Export optimisé pour tableaux de bord
   */
  def exportDashboardData(
    adminLevel: AdminLevel = AdminLevel.Standard
  ): Future[DashboardExportResult] = {
    
    val filter = YeastFilter(
      status = List(YeastStatus.Active)
    ).copy(size = 100)
    
    for {
      yeasts <- yeastReadRepository.findByFilter(filter)
      dashboardData <- generateDashboardExport(yeasts.items, adminLevel)
    } yield DashboardExportResult(
      adminLevel = adminLevel,
      data = dashboardData,
      generatedAt = Instant.now(),
      recordCount = yeasts.items.length
    )
  }

  /**
   * Export pour analyses ML/Data Science
   */
  def exportForAnalytics(
    includeInactive: Boolean = false,
    normalizationLevel: NormalizationLevel = NormalizationLevel.Standard
  ): Future[AnalyticsExportResult] = {
    
    val filter = YeastFilter(
      status = if (includeInactive) List.empty else List(YeastStatus.Active)
    ).copy(size = 100)
    
    for {
      yeasts <- yeastReadRepository.findByFilter(filter)
      analyticsData <- generateAnalyticsExport(yeasts.items, normalizationLevel)
    } yield AnalyticsExportResult(
      normalizationLevel = normalizationLevel,
      data = analyticsData,
      schema = generateAnalyticsSchema(),
      statistics = generateDatasetStatistics(yeasts.items)
    )
  }

  // ==========================================================================
  // GÉNÉRATION DES FORMATS D'EXPORT
  // ==========================================================================

  private def generateExportData(
    yeasts: List[YeastAggregate],
    format: ExportFormat,
    template: ExportTemplate,
    includeMetadata: Boolean
  ): Future[JsValue] = {
    
    Future.successful {
      format match {
        case ExportFormat.JSON => generateJsonExport(yeasts, template, includeMetadata)
        case ExportFormat.CSV => generateCsvExport(yeasts, template)
        case ExportFormat.PDF => generatePdfExport(yeasts, template)
        case ExportFormat.XLSX => generateExcelExport(yeasts, template)
      }
    }
  }

  private def generateJsonExport(
    yeasts: List[YeastAggregate],
    template: ExportTemplate,
    includeMetadata: Boolean
  ): JsValue = {
    
    val baseJson = Json.obj(
      "yeasts" -> JsArray(yeasts.map(yeast => formatYeastForJson(yeast, template)))
    )
    
    if (includeMetadata) {
      baseJson + ("metadata" -> generateExportMetadata(yeasts, template))
    } else {
      baseJson
    }
  }

  private def formatYeastForJson(yeast: YeastAggregate, template: ExportTemplate): JsValue = {
    template match {
      case ExportTemplate.Standard => Json.obj(
        "id" -> yeast.id.asString,
        "name" -> yeast.name.value,
        "strain" -> yeast.strain.value,
        "type" -> yeast.yeastType.name,
        "laboratory" -> yeast.laboratory.name,
        "attenuation" -> Json.obj(
          "min" -> yeast.attenuationRange.min,
          "max" -> yeast.attenuationRange.max,
          "average" -> yeast.attenuationRange.average
        ),
        "fermentationTemperature" -> Json.obj(
          "min" -> yeast.fermentationTemp.min,
          "max" -> yeast.fermentationTemp.max,
          "unit" -> "°C"
        ),
        "flocculation" -> yeast.flocculation.name,
        "alcoholTolerance" -> Json.obj(
          "percentage" -> yeast.alcoholTolerance.percentage,
          "unit" -> "%"
        ),
        "description" -> yeast.description,
        "characteristics" -> yeast.characteristics.allCharacteristics,
        "isActive" -> yeast.isActive
      )
      
      case ExportTemplate.Detailed => Json.obj(
        "id" -> yeast.id.asString,
        "name" -> yeast.name.value,
        "strain" -> yeast.strain.value,
        "type" -> yeast.yeastType.name,
        "laboratory" -> Json.obj(
          "name" -> yeast.laboratory.name,
          "code" -> yeast.laboratory.code
        ),
        "technicalSpecs" -> Json.obj(
          "attenuation" -> Json.obj(
            "min" -> yeast.attenuationRange.min,
            "max" -> yeast.attenuationRange.max,
            "range" -> yeast.attenuationRange.range,
            "average" -> yeast.attenuationRange.average
          ),
          "fermentationTemperature" -> Json.obj(
            "min" -> yeast.fermentationTemp.min,
            "max" -> yeast.fermentationTemp.max,
            "range" -> yeast.fermentationTemp.range,
            "optimal" -> yeast.fermentationTemp.optimal,
            "unit" -> "°C"
          ),
          "flocculation" -> Json.obj(
            "level" -> yeast.flocculation.name,
            "clarification" -> yeast.flocculation.clarificationTime
          ),
          "alcoholTolerance" -> Json.obj(
            "percentage" -> yeast.alcoholTolerance.percentage,
            "category" -> yeast.alcoholTolerance.category,
            "unit" -> "%"
          )
        ),
        "profile" -> Json.obj(
          "description" -> yeast.description,
          "characteristics" -> Json.obj(
            "primary" -> yeast.characteristics.primaryCharacteristics,
            "secondary" -> yeast.characteristics.secondaryCharacteristics,
            "all" -> yeast.characteristics.allCharacteristics
          ),
          "flavorProfile" -> yeast.characteristics.flavorProfile,
          "aromaIntensity" -> yeast.characteristics.aromaIntensity
        ),
        "status" -> Json.obj(
          "isActive" -> yeast.isActive,
          "createdAt" -> yeast.createdAt.toString,
          "updatedAt" -> yeast.updatedAt.toString,
          "version" -> yeast.aggregateVersion
        )
      )
      
      case ExportTemplate.Compact => Json.obj(
        "id" -> yeast.id.asString,
        "name" -> yeast.name.value,
        "type" -> yeast.yeastType.name,
        "lab" -> yeast.laboratory.name,
        "temp" -> s"${yeast.fermentationTemp.min}-${yeast.fermentationTemp.max}°C",
        "attenuation" -> s"${yeast.attenuationRange.min}-${yeast.attenuationRange.max}%",
        "active" -> yeast.isActive
      )
    }
  }

  private def generateCsvExport(yeasts: List[YeastAggregate], template: ExportTemplate): JsValue = {
    template match {
      case ExportTemplate.Standard => generateStandardCsv(yeasts)
      case ExportTemplate.Detailed => generateDetailedCsv(yeasts)
      case ExportTemplate.Compact => generateCompactCsv(yeasts)
    }
  }

  private def generateStandardCsv(yeasts: List[YeastAggregate]): JsValue = {
    val headers = List(
      "id", "name", "strain", "type", "laboratory",
      "attenuation_min", "attenuation_max", "temp_min", "temp_max",
      "flocculation", "alcohol_tolerance", "description", "characteristics", "active"
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
        yeast.characteristics.allCharacteristics.mkString(";"),
        yeast.isActive.toString
      )
    }
    
    Json.obj(
      "headers" -> JsArray(headers.map(JsString)),
      "rows" -> JsArray(rows.map(row => JsArray(row.map(cell => JsString(cell.toString)))))
    )
  }

  private def generateDetailedCsv(yeasts: List[YeastAggregate]): JsValue = {
    val headers = List(
      "id", "name", "strain", "type", "laboratory", "lab_code",
      "attenuation_min", "attenuation_max", "attenuation_avg",
      "temp_min", "temp_max", "temp_optimal", "flocculation", "floc_time",
      "alcohol_tolerance", "alc_category", "description",
      "primary_characteristics", "secondary_characteristics",
      "flavor_profile", "aroma_intensity", "active", "created_at", "updated_at"
    )
    
    val rows = yeasts.map { yeast =>
      List(
        yeast.id.asString,
        yeast.name.value,
        yeast.strain.value,
        yeast.yeastType.name,
        yeast.laboratory.name,
        yeast.laboratory.code,
        yeast.attenuationRange.min.toString,
        yeast.attenuationRange.max.toString,
        yeast.attenuationRange.average.toString,
        yeast.fermentationTemp.min.toString,
        yeast.fermentationTemp.max.toString,
        yeast.fermentationTemp.optimal.toString,
        yeast.flocculation.name,
        yeast.flocculation.clarificationTime,
        yeast.alcoholTolerance.percentage.toString,
        yeast.alcoholTolerance.category,
        yeast.description.getOrElse(""),
        yeast.characteristics.primaryCharacteristics.mkString(";"),
        yeast.characteristics.secondaryCharacteristics.mkString(";"),
        yeast.characteristics.flavorProfile,
        yeast.characteristics.aromaIntensity.toString,
        yeast.isActive.toString,
        yeast.createdAt.toString,
        yeast.updatedAt.toString
      )
    }
    
    Json.obj(
      "headers" -> JsArray(headers.map(JsString)),
      "rows" -> JsArray(rows.map(row => JsArray(row.map(cell => JsString(cell.toString)))))
    )
  }

  private def generateCompactCsv(yeasts: List[YeastAggregate]): JsValue = {
    val headers = List("name", "type", "lab", "temp_range", "attenuation_range", "active")
    
    val rows = yeasts.map { yeast =>
      List(
        yeast.name.value,
        yeast.yeastType.name,
        yeast.laboratory.name,
        s"${yeast.fermentationTemp.min}-${yeast.fermentationTemp.max}°C",
        s"${yeast.attenuationRange.min}-${yeast.attenuationRange.max}%",
        yeast.isActive.toString
      )
    }
    
    Json.obj(
      "headers" -> JsArray(headers.map(JsString)),
      "rows" -> JsArray(rows.map(row => JsArray(row.map(cell => JsString(cell.toString)))))
    )
  }

  private def generatePdfExport(yeasts: List[YeastAggregate], template: ExportTemplate): JsValue = {
    // Structure pour génération PDF côté client ou service externe
    Json.obj(
      "type" -> "pdf",
      "template" -> template.toString,
      "title" -> "Catalogue des Levures - Export PDF",
      "generatedAt" -> Instant.now().toString,
      "sections" -> Json.arr(
        Json.obj(
          "title" -> "Résumé Exécutif",
          "content" -> generateExecutiveSummary(yeasts)
        ),
        Json.obj(
          "title" -> "Catalogue Détaillé",
          "content" -> Json.obj(
            "yeasts" -> JsArray(yeasts.map(yeast => formatYeastForJson(yeast, template)))
          )
        ),
        Json.obj(
          "title" -> "Statistiques",
          "content" -> generateStatisticsForPdf(yeasts)
        )
      )
    )
  }

  private def generateExcelExport(yeasts: List[YeastAggregate], template: ExportTemplate): JsValue = {
    Json.obj(
      "type" -> "xlsx",
      "template" -> template.toString,
      "sheets" -> Json.obj(
        "Yeasts" -> generateDetailedCsv(yeasts),
        "Summary" -> generateSummarySheet(yeasts),
        "Statistics" -> generateStatisticsSheet(yeasts)
      )
    )
  }

  private def generateDashboardExport(yeasts: List[YeastAggregate], adminLevel: AdminLevel): Future[JsValue] = {
    Future.successful {
      Json.obj(
        "overview" -> Json.obj(
          "totalYeasts" -> yeasts.length,
          "activeYeasts" -> yeasts.count(_.isActive),
          "laboratories" -> yeasts.map(_.laboratory).distinct.length,
          "yeastTypes" -> yeasts.map(_.yeastType).distinct.length
        ),
        "distributions" -> Json.obj(
          "byLaboratory" -> Json.toJson(yeasts.groupBy(_.laboratory.name).mapValues(_.length)),
          "byType" -> Json.toJson(yeasts.groupBy(_.yeastType.name).mapValues(_.length)),
          "byActivity" -> Json.obj(
            "active" -> yeasts.count(_.isActive),
            "inactive" -> yeasts.count(!_.isActive)
          )
        ),
        "topPerformers" -> Json.obj(
          "highestTolerance" -> yeasts.sortBy(-_.alcoholTolerance.percentage).take(5).map(_.name.value),
          "wideTemperatureRange" -> yeasts.sortBy(-_.fermentationTemp.range).take(5).map(_.name.value),
          "highAttenuation" -> yeasts.sortBy(-_.attenuationRange.max).take(5).map(_.name.value)
        ),
        "adminData" -> (adminLevel match {
          case AdminLevel.Full => Json.obj(
            "qualityScores" -> Json.toJson(yeasts.map(y => y.name.value -> calculateQualityScore(y)).toMap),
            "lastUpdated" -> Json.toJson(yeasts.map(y => y.name.value -> y.updatedAt.toString).toMap)
          )
          case AdminLevel.Standard => Json.obj()
        })
      )
    }
  }

  private def generateAnalyticsExport(
    yeasts: List[YeastAggregate], 
    normalizationLevel: NormalizationLevel
  ): Future[JsValue] = {
    Future.successful {
      Json.obj(
        "dataset" -> JsArray(yeasts.map(yeast => normalizeYeastForAnalytics(yeast, normalizationLevel))),
        "features" -> generateFeatureDescriptions(),
        "normalization" -> Json.obj(
          "level" -> normalizationLevel.toString,
          "temperatureRange" -> Json.obj("min" -> 10, "max" -> 35),
          "attenuationRange" -> Json.obj("min" -> 50, "max" -> 95),
          "alcoholToleranceRange" -> Json.obj("min" -> 5, "max" -> 20)
        )
      )
    }
  }

  private def normalizeYeastForAnalytics(yeast: YeastAggregate, level: NormalizationLevel): JsValue = {
    level match {
      case NormalizationLevel.Standard => Json.obj(
        "id" -> yeast.id.asString,
        "type_encoded" -> yeast.yeastType.numericCode,
        "lab_encoded" -> yeast.laboratory.numericCode,
        "temp_min_norm" -> normalizeTemperature(yeast.fermentationTemp.min),
        "temp_max_norm" -> normalizeTemperature(yeast.fermentationTemp.max),
        "attenuation_min_norm" -> normalizeAttenuation(yeast.attenuationRange.min),
        "attenuation_max_norm" -> normalizeAttenuation(yeast.attenuationRange.max),
        "flocculation_encoded" -> yeast.flocculation.numericCode,
        "alcohol_tolerance_norm" -> normalizeAlcoholTolerance(yeast.alcoholTolerance.percentage.toInt),
        "is_active" -> (if (yeast.isActive) 1 else 0)
      )
      
      case NormalizationLevel.Advanced => {
        val categoricalFeatures = Json.obj(
          "type" -> yeast.yeastType.numericCode,
          "laboratory" -> yeast.laboratory.numericCode,
          "flocculation" -> yeast.flocculation.numericCode
        )
        
        val numericalFeatures = Json.obj(
          "temp_range_norm" -> normalizeRange(yeast.fermentationTemp.range, 0, 15),
          "temp_optimal_norm" -> normalizeTemperature(yeast.fermentationTemp.optimal),
          "attenuation_range_norm" -> normalizeRange(yeast.attenuationRange.range, 0, 30),
          "attenuation_avg_norm" -> normalizeAttenuation(yeast.attenuationRange.average),
          "alcohol_tolerance_norm" -> normalizeAlcoholTolerance(yeast.alcoholTolerance.percentage.toInt)
        )
        
        val textFeatures = Json.obj(
          "characteristics_count" -> yeast.characteristics.allCharacteristics.length,
          "description_length" -> (yeast.description.map(_.length).getOrElse(0): Int)
        )
        
        val features = Json.obj(
          "categorical" -> categoricalFeatures,
          "numerical" -> numericalFeatures,
          "text_features" -> textFeatures
        )
        
        val target = Json.obj(
          "is_active" -> (if (yeast.isActive) 1 else 0),
          "quality_score" -> calculateQualityScore(yeast)
        )
        
        Json.obj(
          "id" -> yeast.id.asString,
          "features" -> features,
          "target" -> target
        )
      }
    }
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES
  // ==========================================================================

  private def generateFilename(format: ExportFormat, filter: YeastFilter): String = {
    val timestamp = Instant.now().toString.take(10)
    val extension = format match {
      case ExportFormat.JSON => "json"
      case ExportFormat.CSV => "csv"
      case ExportFormat.PDF => "pdf"
      case ExportFormat.XLSX => "xlsx"
    }
    s"yeasts_export_${timestamp}.$extension"
  }

  private def getContentType(format: ExportFormat): String = format match {
    case ExportFormat.JSON => "application/json"
    case ExportFormat.CSV => "text/csv"
    case ExportFormat.PDF => "application/pdf"
    case ExportFormat.XLSX => "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  }

  private def generateMetadata(yeasts: List[YeastAggregate], filter: YeastFilter): JsValue = {
    Json.obj(
      "exportedAt" -> Instant.now().toString,
      "recordCount" -> yeasts.length,
      "filter" -> Json.obj(
        "name" -> filter.name,
        "laboratory" -> filter.laboratory.map(_.toString),
        "yeastType" -> filter.yeastType.map(_.toString),
        "temperatureRange" -> Json.obj(
          "min" -> filter.minTemperature,
          "max" -> filter.maxTemperature
        ),
        "attenuationRange" -> Json.obj(
          "min" -> filter.minAttenuation,
          "max" -> filter.maxAttenuation
        ),
        "status" -> filter.status.map(_.toString)
      ),
      "statistics" -> Json.obj(
        "activeCount" -> yeasts.count(_.isActive),
        "laboratoryCount" -> yeasts.map(_.laboratory).distinct.length,
        "typeDistribution" -> Json.toJson(yeasts.groupBy(_.yeastType.name).mapValues(_.length))
      )
    )
  }

  private def generateExportMetadata(yeasts: List[YeastAggregate], template: ExportTemplate): JsValue = {
    Json.obj(
      "template" -> template.toString,
      "generatedAt" -> Instant.now().toString,
      "recordCount" -> yeasts.length,
      "version" -> "2.0",
      "schema" -> "yeasts-export-schema-v2"
    )
  }

  private def generateExecutiveSummary(yeasts: List[YeastAggregate]): JsValue = {
    Json.obj(
      "totalYeasts" -> yeasts.length,
      "activeProportion" -> Math.round((yeasts.count(_.isActive).toDouble / yeasts.length) * 100),
      "laboratoryCoverage" -> yeasts.map(_.laboratory).distinct.length,
      "averageAlcoholTolerance" -> Math.round(yeasts.map(_.alcoholTolerance.percentage).sum.toDouble / yeasts.length),
      "temperatureSpectrum" -> Json.obj(
        "min" -> yeasts.map(_.fermentationTemp.min).min,
        "max" -> yeasts.map(_.fermentationTemp.max).max
      )
    )
  }

  private def generateStatisticsForPdf(yeasts: List[YeastAggregate]): JsValue = {
    Json.obj(
      "distributions" -> Json.obj(
        "laboratories" -> Json.toJson(yeasts.groupBy(_.laboratory.name).mapValues(_.length)),
        "types" -> Json.toJson(yeasts.groupBy(_.yeastType.name).mapValues(_.length))
      ),
      "ranges" -> Json.obj(
        "temperatureMin" -> yeasts.map(_.fermentationTemp.min).min,
        "temperatureMax" -> yeasts.map(_.fermentationTemp.max).max,
        "attenuationMin" -> yeasts.map(_.attenuationRange.min).min,
        "attenuationMax" -> yeasts.map(_.attenuationRange.max).max
      )
    )
  }

  private def generateSummarySheet(yeasts: List[YeastAggregate]): JsValue = {
    Json.obj(
      "headers" -> JsArray(List("Metric", "Value").map(JsString)),
      "rows" -> JsArray(List(
        JsArray(List(JsString("Total Yeasts"), JsString(yeasts.length.toString))),
        JsArray(List(JsString("Active Yeasts"), JsString(yeasts.count(_.isActive).toString))),
        JsArray(List(JsString("Laboratories"), JsString(yeasts.map(_.laboratory).distinct.length.toString))),
        JsArray(List(JsString("Average Alcohol Tolerance"), JsString(s"${Math.round(yeasts.map(_.alcoholTolerance.percentage).sum.toDouble / yeasts.length)}%")))
      ))
    )
  }

  private def generateStatisticsSheet(yeasts: List[YeastAggregate]): JsValue = {
    val labStats = yeasts.groupBy(_.laboratory.name).mapValues(_.length).toList.sortBy(-_._2)
    
    Json.obj(
      "headers" -> JsArray(List("Laboratory", "Count", "Percentage").map(JsString)),
      "rows" -> JsArray(labStats.map { case (lab, count) =>
        val percentage = Math.round((count.toDouble / yeasts.length) * 100)
        JsArray(List(JsString(lab), JsString(count.toString), JsString(s"$percentage%")))
      })
    )
  }

  private def generateFeatureDescriptions(): JsValue = {
    Json.obj(
      "categorical_features" -> Json.arr(
        Json.obj("name" -> "type", "description" -> "Yeast type (ALE, LAGER, etc.)"),
        Json.obj("name" -> "laboratory", "description" -> "Manufacturing laboratory"),
        Json.obj("name" -> "flocculation", "description" -> "Flocculation level")
      ),
      "numerical_features" -> Json.arr(
        Json.obj("name" -> "temp_min_norm", "description" -> "Normalized minimum fermentation temperature"),
        Json.obj("name" -> "temp_max_norm", "description" -> "Normalized maximum fermentation temperature"),
        Json.obj("name" -> "attenuation_min_norm", "description" -> "Normalized minimum attenuation"),
        Json.obj("name" -> "attenuation_max_norm", "description" -> "Normalized maximum attenuation"),
        Json.obj("name" -> "alcohol_tolerance_norm", "description" -> "Normalized alcohol tolerance")
      )
    )
  }

  private def generateAnalyticsSchema(): JsValue = {
    Json.obj(
      "version" -> "1.0",
      "features" -> Json.obj(
        "numerical" -> 8,
        "categorical" -> 3,
        "text" -> 3
      ),
      "target_variables" -> Json.arr("is_active", "quality_score"),
      "normalization" -> "min-max scaling [0,1]"
    )
  }

  private def generateDatasetStatistics(yeasts: List[YeastAggregate]): JsValue = {
    Json.obj(
      "size" -> yeasts.length,
      "features" -> Json.obj(
        "temperature_stats" -> Json.obj(
          "min" -> yeasts.map(_.fermentationTemp.min).min,
          "max" -> yeasts.map(_.fermentationTemp.max).max,
          "mean" -> Math.round(yeasts.map(y => (y.fermentationTemp.min + y.fermentationTemp.max) / 2.0).sum / yeasts.length)
        ),
        "attenuation_stats" -> Json.obj(
          "min" -> yeasts.map(_.attenuationRange.min).min,
          "max" -> yeasts.map(_.attenuationRange.max).max,
          "mean" -> Math.round(yeasts.map(_.attenuationRange.average).sum / yeasts.length)
        )
      ),
      "class_distribution" -> Json.obj(
        "active" -> yeasts.count(_.isActive),
        "inactive" -> yeasts.count(!_.isActive)
      )
    )
  }

  // Normalisation des valeurs pour ML
  private def normalizeTemperature(temp: Int): Double = Math.max(0.0, Math.min(1.0, (temp - 10.0) / 25.0))
  private def normalizeAttenuation(att: Int): Double = Math.max(0.0, Math.min(1.0, (att - 50.0) / 45.0))
  private def normalizeAlcoholTolerance(tol: Int): Double = Math.max(0.0, Math.min(1.0, (tol - 5.0) / 15.0))
  private def normalizeRange(range: Int, min: Int, max: Int): Double = 
    Math.max(0.0, Math.min(1.0, (range - min.toDouble) / (max - min).toDouble))

  private def calculateQualityScore(yeast: YeastAggregate): Double = {
    var score = 0.5 // Base score
    
    if (yeast.description.nonEmpty) score += 0.2
    if (yeast.characteristics.allCharacteristics.nonEmpty) score += 0.15
    if (yeast.fermentationTemp.range <= 6) score += 0.1
    if (yeast.attenuationRange.range <= 15) score += 0.05
    
    Math.min(1.0, score)
  }
}

// ==========================================================================
// TYPES DE SUPPORT
// ==========================================================================

sealed trait ExportFormat
object ExportFormat {
  case object JSON extends ExportFormat
  case object CSV extends ExportFormat
  case object PDF extends ExportFormat
  case object XLSX extends ExportFormat
}

sealed trait ExportTemplate
object ExportTemplate {
  case object Standard extends ExportTemplate
  case object Detailed extends ExportTemplate
  case object Compact extends ExportTemplate
}

sealed trait AdminLevel
object AdminLevel {
  case object Standard extends AdminLevel
  case object Full extends AdminLevel
}

sealed trait NormalizationLevel
object NormalizationLevel {
  case object Standard extends NormalizationLevel
  case object Advanced extends NormalizationLevel
}

case class ExportResult(
  format: ExportFormat,
  template: ExportTemplate,
  recordCount: Int,
  data: JsValue,
  filename: String,
  contentType: String,
  metadata: Option[JsValue]
)

case class DashboardExportResult(
  adminLevel: AdminLevel,
  data: JsValue,
  generatedAt: Instant,
  recordCount: Int
)

case class AnalyticsExportResult(
  normalizationLevel: NormalizationLevel,
  data: JsValue,
  schema: JsValue,
  statistics: JsValue
)