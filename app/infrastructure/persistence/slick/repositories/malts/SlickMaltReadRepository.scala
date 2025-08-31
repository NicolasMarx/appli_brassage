package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId, MaltType, EBCColor, ExtractionRate, DiastaticPower, MaltSource}
import domain.malts.repositories.{MaltReadRepository, MaltSubstitution, MaltCompatibility}
import domain.common.PagedResult
import domain.shared.NonEmptyString
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

@Singleton
class SlickMaltReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
extends MaltReadRepository 
with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  
  // Case class avec UUID correct
  case class MaltRow(
    id: UUID,  // ✅ UUID au lieu de String
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
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  )

  // Table Slick avec UUID correct
  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[UUID]("id", O.PrimaryKey)  // ✅ UUID au lieu de String
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
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, originCode,
             description, flavorProfiles, source, isActive,
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  private val malts = TableQuery[MaltTable]
  
  // Conversion robuste avec gestion d'erreurs
  private def rowToAggregate(row: MaltRow): Option[MaltAggregate] = {
    try {
      for {
        maltId <- MaltId(row.id.toString).toOption
        name <- NonEmptyString.create(row.name).toOption
        maltType <- MaltType.fromName(row.maltType)
        ebcColor <- EBCColor(row.ebcColor).toOption
        extractionRate <- ExtractionRate(row.extractionRate).toOption
        diastaticPower <- DiastaticPower(row.diastaticPower).toOption
        source <- MaltSource.fromName(row.source)
      } yield {
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
          credibilityScore = row.credibilityScore,
          createdAt = row.createdAt,
          updatedAt = row.updatedAt,
          version = row.version
        )
      }
    } catch {
      case e: Exception =>
        println(s"ERROR: Exception converting malt ${row.name}: ${e.getMessage}")
        None
    }
  }
  
  override def findAll(page: Int = 0, pageSize: Int = 20, activeOnly: Boolean = true): Future[List[MaltAggregate]] = {
    val query = malts
      .filter(_.isActive === activeOnly)
      .sortBy(_.name)
      .drop(page * pageSize)
      .take(pageSize)
      
    db.run(query.result).map(_.flatMap(rowToAggregate).toList)
  }
  
  override def count(activeOnly: Boolean = true): Future[Long] = {
    val query = if (activeOnly) malts.filter(_.isActive === true) else malts
    db.run(query.length.result).map(_.toLong)
  }

  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    val uuid = UUID.fromString(id.value)
    val query = malts.filter(_.id === uuid)
    db.run(query.result.headOption).map(_.flatMap(rowToAggregate))
  }
  
  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.name === name)
    db.run(query.result.headOption).map(_.flatMap(rowToAggregate))
  }
  
  override def existsByName(name: String): Future[Boolean] = {
    val query = malts.filter(_.name === name)
    db.run(query.exists.result)
  }

  // Stubs pour méthodes avancées
  override def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]] = 
    Future.successful(List.empty)
    
  override def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]] = 
    Future.successful(PagedResult(List.empty, page, pageSize, 0, false))
    
  override def findByFilters(
    maltType: Option[String] = None, minEBC: Option[Double] = None, maxEBC: Option[Double] = None,
    originCode: Option[String] = None, status: Option[String] = None, source: Option[String] = None,
    minCredibility: Option[Double] = None, searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty, minExtraction: Option[Double] = None,
    minDiastaticPower: Option[Double] = None, page: Int = 0, pageSize: Int = 20
  ): Future[PagedResult[MaltAggregate]] = {
    Future.successful(PagedResult(List.empty, page, pageSize, 0, false))
  }
}
