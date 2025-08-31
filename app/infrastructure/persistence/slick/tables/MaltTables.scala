package infrastructure.persistence.slick.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}
import domain.malts.model._
import domain.shared._
import java.time.Instant
import java.util.UUID
import play.api.libs.json._

/**
 * Définitions des tables Slick pour le domaine Malts
 * Suit le pattern HopTables pour cohérence architecturale
 */

/**
 * Représentation row pour la table malts
 */
case class MaltRow(
                    id: UUID,
                    name: String,
                    maltType: String,
                    ebcColor: Double,
                    extractionRate: Double,
                    diastaticPower: Double,
                    originCode: String,
                    description: Option[String],
                    status: String,
                    source: String,
                    credibilityScore: Int,
                    flavorProfiles: String, // JSON string
                    createdAt: Instant,
                    updatedAt: Instant,
                    version: Int
                  )

/**
 * Table malts principale
 */
class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {

  // Colonnes de base
  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maltType = column[String]("malt_type")
  def ebcColor = column[Double]("ebc_color")
  def extractionRate = column[Double]("extraction_rate")
  def diastaticPower = column[Double]("diastatic_power")
  def originCode = column[String]("origin_code")
  def description = column[Option[String]]("description")
  def status = column[String]("status")
  def source = column[String]("source")
  def credibilityScore = column[Int]("credibility_score")
  def flavorProfiles = column[String]("flavor_profiles")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def version = column[Int]("version")

  // Projection complète
  override def * : ProvenShape[MaltRow] = (
    id, name, maltType, ebcColor, extractionRate, diastaticPower,
    originCode, description, status, source, credibilityScore,
    flavorProfiles, createdAt, updatedAt, version
  ) <> (MaltRow.tupled, MaltRow.unapply)

  // Index composites (définis dans évolution SQL)
  def idxMaltTypeColor = index("idx_malts_type_color", (maltType, ebcColor))
  def idxActiveType = index("idx_malts_active_type", (status, maltType))
  def idxBaseExtraction = index("idx_malts_base_extraction", (extractionRate, diastaticPower))
}

/**
 * Représentation row pour malt_beer_styles
 */
case class MaltBeerStyleRow(
                             maltId: UUID,
                             beerStyleId: String,
                             compatibilityScore: Option[Int],
                             typicalPercentage: Option[Double],
                             createdAt: Instant
                           )

/**
 * Table relation malts-styles de bière
 */
class MaltBeerStyleTable(tag: Tag) extends Table[MaltBeerStyleRow](tag, "malt_beer_styles") {
  def maltId = column[UUID]("malt_id")
  def beerStyleId = column[String]("beer_style_id")
  def compatibilityScore = column[Option[Int]]("compatibility_score")
  def typicalPercentage = column[Option[Double]]("typical_percentage")
  def createdAt = column[Instant]("created_at")

  override def * : ProvenShape[MaltBeerStyleRow] = (
    maltId, beerStyleId, compatibilityScore, typicalPercentage, createdAt
  ) <> (MaltBeerStyleRow.tupled, MaltBeerStyleRow.unapply)

  // Clé primaire composite
  def pk = primaryKey("pk_malt_beer_styles", (maltId, beerStyleId))
}

/**
 * Représentation row pour malt_substitutions
 */
case class MaltSubstitutionRow(
                                originalMaltId: UUID,
                                substituteMaltId: UUID,
                                substitutionRatio: Double,
                                compatibilityNotes: Option[String],
                                createdAt: Instant
                              )

/**
 * Table substitutions malts
 */
class MaltSubstitutionTable(tag: Tag) extends Table[MaltSubstitutionRow](tag, "malt_substitutions") {
  def originalMaltId = column[UUID]("original_malt_id")
  def substituteMaltId = column[UUID]("substitute_malt_id")
  def substitutionRatio = column[Double]("substitution_ratio")
  def compatibilityNotes = column[Option[String]]("compatibility_notes")
  def createdAt = column[Instant]("created_at")

  override def * : ProvenShape[MaltSubstitutionRow] = (
    originalMaltId, substituteMaltId, substitutionRatio, compatibilityNotes, createdAt
  ) <> (MaltSubstitutionRow.tupled, MaltSubstitutionRow.unapply)

  def pk = primaryKey("pk_malt_substitutions", (originalMaltId, substituteMaltId))
}

/**
 * Companion object avec instances de tables et conversions
 */
object MaltTables {

  // Instances de tables
  val malts = TableQuery[MaltTable]
  val maltBeerStyles = TableQuery[MaltBeerStyleTable]
  val maltSubstitutions = TableQuery[MaltSubstitutionTable]

  /**
   * Conversion MaltRow → MaltAggregate
   */
  def rowToAggregate(row: MaltRow, origin: Origin): Either[String, MaltAggregate] = {
    for {
      maltId <- MaltId(row.id.toString)
      name <- NonEmptyString.create(row.name)
      maltType <- MaltType.fromName(row.maltType)
        .toRight(s"Type de malt invalide: ${row.maltType}")
      ebcColor <- EBCColor(row.ebcColor)
      extractionRate <- ExtractionRate(row.extractionRate)
      diastaticPower <- DiastaticPower(row.diastaticPower)
      status <- MaltStatus.fromName(row.status)
        .toRight(s"Statut malt invalide: ${row.status}")
      source <- MaltSource.fromName(row.source)
        .toRight(s"Source malt invalide: ${row.source}")
      credibility <- MaltCredibility(row.credibilityScore)
    } yield {
      // Parse flavor profiles JSON
      val flavorProfiles = try {
        Json.parse(row.flavorProfiles).as[List[String]]
      } catch {
        case _: Exception => List.empty[String]
      }

      MaltAggregate(
        id = maltId,
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        origin = origin,
        description = row.description,
        status = status,
        source = source,
        credibilityScore = credibility,
        flavorProfiles = flavorProfiles,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version
      )
    }
  }

  /**
   * Conversion MaltAggregate → MaltRow
   */
  def aggregateToRow(aggregate: MaltAggregate): MaltRow = {
    val flavorProfilesJson = Json.toJson(aggregate.flavorProfiles).toString()

    MaltRow(
      id = UUID.fromString(aggregate.id.value),
      name = aggregate.name.value,
      maltType = aggregate.maltType.name,
      ebcColor = aggregate.ebcColor.value,
      extractionRate = aggregate.extractionRate.value,
      diastaticPower = aggregate.diastaticPower.value,
      originCode = aggregate.origin.code,
      description = aggregate.description,
      status = aggregate.status.name,
      source = aggregate.source.name,
      credibilityScore = aggregate.credibilityScore.value,
      flavorProfiles = flavorProfilesJson,
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version
    )
  }

  /**
   * Filtre pour malts actifs
   */
  def activeFilter = malts.filter(_.status.inSet(Set("ACTIVE", "SEASONAL")))

  /**
   * Filtre par type de malt
   */
  def byTypeFilter(maltType: MaltType) = malts.filter(_.maltType === maltType.name)

  /**
   * Filtre par gamme de couleur EBC
   */
  def byColorRangeFilter(minEBC: Double, maxEBC: Double) =
    malts.filter(m => m.ebcColor >= minEBC && m.ebcColor <= maxEBC)

  /**
   * Filtre par pouvoir diastasique minimum
   */
  def byMinDiastaticPowerFilter(minPower: Double) =
    malts.filter(_.diastaticPower >= minPower)

  /**
   * Filtre pour malts de base (BASE type + extraction élevée)
   */
  def baseMaltsFilter = malts.filter(m =>
    m.maltType === "BASE" && m.extractionRate >= 75.0
  )

  /**
   * Filtre par score de crédibilité minimum
   */
  def byMinCredibilityFilter(minScore: Int) =
    malts.filter(_.credibilityScore >= minScore)

  /**
   * Tri par popularité (nombre de relations avec styles de bière)
   */
  def sortByPopularity = {
    val popularityQuery = maltBeerStyles
      .groupBy(_.maltId)
      .map { case (maltId, group) => (maltId, group.length) }

    malts
      .joinLeft(popularityQuery).on(_.id === _._1)
      .sortBy(_._2.map(_._2).getOrElse(0).desc)
      .map(_._1)
  }
}