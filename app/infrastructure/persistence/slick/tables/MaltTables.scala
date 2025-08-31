package infrastructure.persistence.slick.tables

import slick.jdbc.JdbcProfile
import java.time.Instant

trait MaltTables {
  val profile: JdbcProfile
  import profile.api._

  // Case class pour les lignes de la table malts
  case class MaltRow(
    id: String, // UUID au format String
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String],
    source: String,
    isActive: Boolean,
    credibilityScore: Int,
    createdAt: Instant,
    updatedAt: Instant,
    version: Int
  )

  // Table Slick pour malts
  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[String]("id", O.PrimaryKey) // String dans la DB
    def name = column[String]("name")
    def maltType = column[String]("malt_type")
    def ebcColor = column[Double]("ebc_color")
    def extractionRate = column[Double]("extraction_rate")
    def diastaticPower = column[Double]("diastatic_power")
    def originCode = column[String]("origin_code")
    def description = column[Option[String]]("description")
    def flavorProfiles = column[Option[String]]("flavor_profiles")
    def source = column[String]("source")
    def isActive = column[Boolean]("is_active")
    def credibilityScore = column[Int]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Int]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  // Tables de support (simplifiées)
  case class MaltBeerStyleRow(
    id: String,
    maltId: String,
    beerStyleId: String,
    compatibilityScore: Double,
    usageNotes: Option[String]
  )

  class MaltBeerStyleTable(tag: Tag) extends Table[MaltBeerStyleRow](tag, "malt_beer_styles") {
    def id = column[String]("id", O.PrimaryKey)
    def maltId = column[String]("malt_id")
    def beerStyleId = column[String]("beer_style_id")
    def compatibilityScore = column[Double]("compatibility_score")
    def usageNotes = column[Option[String]]("usage_notes")

    def * = (id, maltId, beerStyleId, compatibilityScore, usageNotes).mapTo[MaltBeerStyleRow]
  }

  case class MaltSubstitutionRow(
    id: String,
    maltId: String,
    substituteId: String,
    substitutionRatio: Double,
    notes: Option[String],
    qualityScore: Double
  )

  class MaltSubstitutionTable(tag: Tag) extends Table[MaltSubstitutionRow](tag, "malt_substitutions") {
    def id = column[String]("id", O.PrimaryKey)
    def maltId = column[String]("malt_id")
    def substituteId = column[String]("substitute_id")
    def substitutionRatio = column[Double]("substitution_ratio")
    def notes = column[Option[String]]("notes")
    def qualityScore = column[Double]("quality_score")

    def * = (id, maltId, substituteId, substitutionRatio, notes, qualityScore).mapTo[MaltSubstitutionRow]
  }

  // Conversion row -> aggregate
  def rowToAggregate(row: MaltRow): Either[String, domain.malts.model.MaltAggregate] = {
    import domain.malts.model._
    import domain.shared.NonEmptyString
    
    for {
      maltId <- MaltId(row.id) // row.id est String (UUID au format String)
      name <- NonEmptyString.create(row.name) // Utilise create qui retourne Either
      maltType <- MaltType.fromName(row.maltType).toRight(s"Type malt invalide: ${row.maltType}")
      ebcColor <- EBCColor(row.ebcColor)
      extractionRate <- ExtractionRate(row.extractionRate)
      diastaticPower <- DiastaticPower(row.diastaticPower)
      source <- MaltSource.fromName(row.source).toRight(s"Source invalide: ${row.source}")
    } yield MaltAggregate(
      id = maltId,
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      originCode = row.originCode,
      description = row.description,
      flavorProfiles = row.flavorProfiles.map(_.split(",").toList).getOrElse(List.empty),
      source = source,
      isActive = row.isActive,
      credibilityScore = row.credibilityScore.toDouble,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = row.version.toLong
    )
  }

  // Conversion aggregate -> row
  def aggregateToRow(aggregate: domain.malts.model.MaltAggregate): MaltRow = {
    MaltRow(
      id = aggregate.id.value, // MaltId.value est String (UUID au format String)
      name = aggregate.name.value,
      maltType = aggregate.maltType.name,
      ebcColor = aggregate.ebcColor.value,
      extractionRate = aggregate.extractionRate.value,
      diastaticPower = aggregate.diastaticPower.value,
      originCode = aggregate.originCode,
      description = aggregate.description,
      flavorProfiles = if (aggregate.flavorProfiles.nonEmpty) Some(aggregate.flavorProfiles.mkString(",")) else None,
      source = aggregate.source.name,
      isActive = aggregate.isActive,
      credibilityScore = aggregate.credibilityScore.toInt,
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version.toInt
    )
  }

  // TableQuery instances
  val malts = TableQuery[MaltTable]
  val maltBeerStyles = TableQuery[MaltBeerStyleTable]
  val maltSubstitutions = TableQuery[MaltSubstitutionTable]
}
