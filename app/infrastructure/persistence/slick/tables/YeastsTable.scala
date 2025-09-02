package infrastructure.persistence.slick.tables

import domain.yeasts.model._
import domain.shared.NonEmptyString
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.UUID

/**
 * Définition table yeasts pour Slick
 * Mapping objet-relationnel pour PostgreSQL
 */
class YeastsTable(tag: Tag) extends Table[YeastRow](tag, "yeasts") {
  
  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def laboratory = column[String]("laboratory")
  def strain = column[String]("strain")
  def yeastType = column[String]("yeast_type")
  def attenuationMin = column[Int]("attenuation_min")
  def attenuationMax = column[Int]("attenuation_max")
  def temperatureMin = column[Int]("temperature_min")
  def temperatureMax = column[Int]("temperature_max")
  def alcoholTolerance = column[Double]("alcohol_tolerance")
  def flocculation = column[String]("flocculation")
  def characteristics = column[String]("characteristics") // JSON
  def status = column[String]("status")
  def version = column[Long]("version")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  
  // Index composé pour performance
  def idxLaboratoryStrain = index("idx_yeasts_laboratory_strain", (laboratory, strain), unique = true)
  def idxStatus = index("idx_yeasts_status", status)
  def idxType = index("idx_yeasts_type", yeastType)
  def idxLaboratory = index("idx_yeasts_laboratory", laboratory)
  def idxAttenuation = index("idx_yeasts_attenuation", (attenuationMin, attenuationMax))
  def idxTemperature = index("idx_yeasts_temperature", (temperatureMin, temperatureMax))
  
  def * = (
    id, name, laboratory, strain, yeastType,
    attenuationMin, attenuationMax, temperatureMin, temperatureMax,
    alcoholTolerance, flocculation, characteristics, status,
    version, createdAt, updatedAt
  ) <> (
    (YeastRow.apply _).tupled,
    YeastRow.unapply
  )
}

/**
 * Row class pour mapping Slick
 */
case class YeastRow(
  id: UUID,
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
  characteristics: String, // JSON serialized
  status: String,
  version: Long,
  createdAt: Instant,
  updatedAt: Instant
)

object YeastRow {
  
  /**
   * Conversion YeastAggregate -> YeastRow
   */
  def fromAggregate(yeast: YeastAggregate): YeastRow = {
    YeastRow(
      id = yeast.id.value,
      name = yeast.name.value,
      laboratory = yeast.laboratory.name,
      strain = yeast.strain.value,
      yeastType = yeast.yeastType.name,
      attenuationMin = yeast.attenuationRange.min,
      attenuationMax = yeast.attenuationRange.max,
      temperatureMin = yeast.fermentationTemp.min,
      temperatureMax = yeast.fermentationTemp.max,
      alcoholTolerance = yeast.alcoholTolerance.value,
      flocculation = yeast.flocculation.name,
      characteristics = Json.toJson(yeast.characteristics).toString(),
      status = if (yeast.isActive) "ACTIVE" else "INACTIVE",
      version = yeast.aggregateVersion.toLong,
      createdAt = yeast.createdAt,
      updatedAt = yeast.updatedAt
    )
  }
  
  /**
   * Conversion YeastRow -> YeastAggregate
   */
  def toAggregate(row: YeastRow): Either[String, YeastAggregate] = {
    for {
      yeastId <- Right(YeastId(row.id))
      yeastName <- YeastName.fromString(row.name)
      name <- Right(NonEmptyString.unsafe(yeastName.value))
      laboratory <- YeastLaboratory.fromName(row.laboratory)
        .toRight(s"Laboratoire inconnu: ${row.laboratory}")
      strain <- YeastStrain.fromString(row.strain)
      yeastType <- YeastType.fromName(row.yeastType)
        .toRight(s"Type levure inconnu: ${row.yeastType}")
      attenuationRange <- Right(AttenuationRange.unsafe(row.attenuationMin, row.attenuationMax))
      fermentationTemp <- Right(FermentationTemp.unsafe(row.temperatureMin, row.temperatureMax))
      alcoholTolerance <- Right(AlcoholTolerance.unsafe(row.alcoholTolerance))
      flocculation <- FlocculationLevel.fromName(row.flocculation)
        .toRight(s"Floculation inconnue: ${row.flocculation}")
      characteristics <- parseCharacteristics(row.characteristics)
      status <- YeastStatus.fromName(row.status)
        .toRight(s"Statut inconnu: ${row.status}")
    } yield {
      YeastAggregate(
        id = yeastId,
        name = name,
        strain = strain,
        yeastType = yeastType,
        laboratory = laboratory,
        attenuationRange = attenuationRange,
        fermentationTemp = fermentationTemp,
        flocculation = flocculation,
        alcoholTolerance = alcoholTolerance,
        description = None, // TODO: add description field to row
        characteristics = characteristics,
        isActive = row.status == "ACTIVE",
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        aggregateVersion = row.version.toInt
      )
    }
  }
  
  private def parseCharacteristics(json: String): Either[String, YeastCharacteristics] = {
    try {
      val parsed = Json.parse(json)
      val aromaProfile = (parsed \ "aromaProfile").asOpt[List[String]].getOrElse(List.empty)
      val flavorProfile = (parsed \ "flavorProfile").asOpt[List[String]].getOrElse(List.empty)
      val esters = (parsed \ "esters").asOpt[List[String]].getOrElse(List.empty)
      val phenols = (parsed \ "phenols").asOpt[List[String]].getOrElse(List.empty)
      val otherCompounds = (parsed \ "otherCompounds").asOpt[List[String]].getOrElse(List.empty)
      val notes = (parsed \ "notes").asOpt[String]
      
      Right(YeastCharacteristics(aromaProfile, flavorProfile, esters, phenols, otherCompounds, notes))
    } catch {
      case _: Exception => Left(s"Impossible de parser les caractéristiques: $json")
    }
  }
  
  // Formatters JSON implicites pour YeastCharacteristics
  implicit val yeastCharacteristicsWrites: Writes[YeastCharacteristics] = Json.writes[YeastCharacteristics]
  implicit val yeastCharacteristicsReads: Reads[YeastCharacteristics] = Json.reads[YeastCharacteristics]
}

/**
 * Companion object pour la table
 */
object YeastsTable {
  val yeasts = TableQuery[YeastsTable]
}
