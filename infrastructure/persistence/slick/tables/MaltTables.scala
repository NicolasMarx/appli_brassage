package infrastructure.persistence.slick.tables

import slick.jdbc.JdbcProfile
import java.time.Instant

trait MaltTables {
  protected val profile: JdbcProfile
  import profile.api._

  // Case class pour les lignes de la table malts (corrigé)
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
    credibilityScore: Double, // ❗ Corrigé: Double au lieu d'Int
    createdAt: Instant,
    updatedAt: Instant,
    version: Long // ❗ Corrigé: Long au lieu d'Int
  )

  // Table Slick pour malts (corrigée)
  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[String]("id", O.PrimaryKey)
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
    def credibilityScore = column[Double]("credibility_score") // ❗ Corrigé: Double
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version") // ❗ Corrigé: Long

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
    
    // Index pour optimiser les requêtes courantes
    def idx = index("idx_malts_active_type", (isActive, maltType))
    def nameIdx = index("idx_malts_name", name, unique = true)
    def colorIdx = index("idx_malts_color", ebcColor)
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
    
    // Foreign keys pour intégrité référentielle
    def maltFk = foreignKey("fk_malt_beer_style_malt", maltId, malts)(_.id)
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
    
    // Foreign keys pour intégrité référentielle
    def maltFk = foreignKey("fk_malt_substitution_malt", maltId, malts)(_.id)
    def substituteFk = foreignKey("fk_malt_substitution_substitute", substituteId, malts)(_.id)
  }

  // Conversion row -> aggregate (corrigée)
  def rowToAggregate(row: MaltRow): Either[String, domain.malts.model.MaltAggregate] = {
    import domain.malts.model._
    import domain.shared.NonEmptyString
    
    for {
      maltId <- MaltId(row.id) // MaltId.apply(String): Either[String, MaltId]
      name <- NonEmptyString.create(row.name) // ✅ Utilise create qui retourne Either[String, NonEmptyString]
      maltType <- MaltType.fromName(row.maltType).toRight(s"Type malt invalide: ${row.maltType}")
      ebcColor <- EBCColor(row.ebcColor) // EBCColor.apply(Double): Either[String, EBCColor]
      extractionRate <- ExtractionRate(row.extractionRate) // ExtractionRate.apply(Double): Either[String, ExtractionRate]
      diastaticPower <- DiastaticPower(row.diastaticPower) // DiastaticPower.apply(Double): Either[String, DiastaticPower]
      source <- MaltSource.fromName(row.source).toRight(s"Source invalide: ${row.source}")
    } yield {
      // ❗ Utilise la méthode de création directe de MaltAggregate (pour persistence)
      MaltAggregate(
        id = maltId,
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = row.originCode,
        description = row.description,
        flavorProfiles = row.flavorProfiles.map(_.split(",").toList.filter(_.trim.nonEmpty)).getOrElse(List.empty),
        source = source,
        isActive = row.isActive,
        credibilityScore = row.credibilityScore, // ✅ Déjà Double
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version // ✅ Déjà Long
      )
    }
  }

  // Conversion aggregate -> row (corrigée)
  def aggregateToRow(aggregate: domain.malts.model.MaltAggregate): MaltRow = {
    MaltRow(
      id = aggregate.id.value, // MaltId.value est String (UUID au format String)
      name = aggregate.name.value, // NonEmptyString.value
      maltType = aggregate.maltType.name, // MaltType.name
      ebcColor = aggregate.ebcColor.value, // EBCColor.value: Double
      extractionRate = aggregate.extractionRate.value, // ExtractionRate.value: Double
      diastaticPower = aggregate.diastaticPower.value, // DiastaticPower.value: Double
      originCode = aggregate.originCode,
      description = aggregate.description,
      flavorProfiles = if (aggregate.flavorProfiles.nonEmpty) Some(aggregate.flavorProfiles.mkString(",")) else None,
      source = aggregate.source.name, // MaltSource.name
      isActive = aggregate.isActive,
      credibilityScore = aggregate.credibilityScore, // ✅ Déjà Double
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version // ✅ Déjà Long
    )
  }

  // ❗ Helper pour conversion sécurisée avec gestion d'erreurs
  def safeRowToAggregate(row: MaltRow): Option[domain.malts.model.MaltAggregate] = {
    rowToAggregate(row) match {
      case Right(aggregate) => Some(aggregate)
      case Left(error) => 
        // Log l'erreur dans un vrai système
        println(s"Erreur conversion row -> aggregate: $error")
        None
    }
  }

  // TableQuery instances
  val malts = TableQuery[MaltTable]
  val maltBeerStyles = TableQuery[MaltBeerStyleTable]
  val maltSubstitutions = TableQuery[MaltSubstitutionTable]
}
