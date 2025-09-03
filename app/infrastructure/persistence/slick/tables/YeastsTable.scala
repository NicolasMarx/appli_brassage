package infrastructure.persistence.slick.tables

import domain.yeasts.model._
import domain.shared.NonEmptyString
import play.api.libs.json._
import play.api.libs.functional.syntax._
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
    println(s"Converting row: ${row.id} - ${row.name}")
    
    try {
      for {
        yeastId <- {
          println(s"  Creating YeastId...")
          Right(YeastId(row.id))
        }
        yeastName <- {
          println(s"  Parsing YeastName: ${row.name}")
          YeastName.fromString(row.name)
        }
        name <- {
          println(s"  Creating NonEmptyString...")
          Right(NonEmptyString.unsafe(yeastName.value))
        }
        laboratory <- {
          println(s"  Parsing YeastLaboratory: ${row.laboratory}")
          YeastLaboratory.fromName(row.laboratory).toRight(s"Laboratoire inconnu: ${row.laboratory}")
        }
        strain <- {
          println(s"  Parsing YeastStrain: ${row.strain}")
          YeastStrain.fromString(row.strain)
        }
        yeastType <- {
          println(s"  Parsing YeastType: ${row.yeastType}")
          YeastType.fromName(row.yeastType).toRight(s"Type levure inconnu: ${row.yeastType}")
        }
        attenuationRange <- {
          println(s"  Creating AttenuationRange: ${row.attenuationMin}-${row.attenuationMax}")
          Right(AttenuationRange.unsafe(row.attenuationMin, row.attenuationMax))
        }
        fermentationTemp <- {
          println(s"  Creating FermentationTemp: ${row.temperatureMin}-${row.temperatureMax}")
          Right(FermentationTemp.unsafe(row.temperatureMin, row.temperatureMax))
        }
        alcoholTolerance <- {
          println(s"  Creating AlcoholTolerance: ${row.alcoholTolerance}")
          Right(AlcoholTolerance.unsafe(row.alcoholTolerance))
        }
        flocculation <- {
          println(s"  Parsing FlocculationLevel: ${row.flocculation}")
          FlocculationLevel.fromName(row.flocculation).toRight(s"Floculation inconnue: ${row.flocculation}")
        }
        characteristics <- {
          println(s"  Parsing characteristics JSON: ${row.characteristics}")
          parseCharacteristics(row.characteristics)
        }
        status <- {
          println(s"  Parsing YeastStatus: ${row.status}")
          YeastStatus.fromName(row.status).toRight(s"Statut inconnu: ${row.status}")
        }
      } yield {
        println(s"  Creating YeastAggregate...")
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
    } catch {
      case ex: Exception =>
        println(s"EXCEPTION in toAggregate for ${row.name}: ${ex.getMessage}")
        ex.printStackTrace()
        Left(s"Exception during conversion: ${ex.getMessage}")
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
  implicit val yeastCharacteristicsReads: Reads[YeastCharacteristics] = (
    (JsPath \ "aromaProfile").read[List[String]] and
    (JsPath \ "flavorProfile").read[List[String]] and
    (JsPath \ "esters").read[List[String]] and
    (JsPath \ "phenols").read[List[String]] and
    (JsPath \ "otherCompounds").read[List[String]] and
    (JsPath \ "notes").readNullable[String]
  )((aromaProfile: List[String], flavorProfile: List[String], esters: List[String], phenols: List[String], otherCompounds: List[String], notes: Option[String]) =>
    YeastCharacteristics(aromaProfile, flavorProfile, esters, phenols, otherCompounds, notes)
  )
}

/**
 * Companion object pour la table
 */
object YeastsTable {
  val yeasts = TableQuery[YeastsTable]
}
