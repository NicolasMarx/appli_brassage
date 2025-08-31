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

@Singleton
class SlickMaltReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
extends MaltReadRepository 
with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  
  // Définition directe de la table dans le repository
  case class MaltRow(
    id: String,
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
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  private val malts = TableQuery[MaltTable]
  
  private def rowToAggregate(row: MaltRow): Option[MaltAggregate] = {
    for {
      maltId <- MaltId(row.id).toOption
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
  }
  
  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.id === id.value)
    db.run(query.result.headOption).map(_.flatMap(rowToAggregate))
  }
  
  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.name === name)
    db.run(query.result.headOption).map(_.flatMap(rowToAggregate))
  }
  
  override def existsByName(name: String): Future[Boolean] = {
    val query = malts.filter(_.name === name).exists
    db.run(query.result)
  }
  
  override def findAll(page: Int = 0, pageSize: Int = 20, activeOnly: Boolean = true): Future[List[MaltAggregate]] = {
    val query = malts
      .filter(_.isActive === true)
      .sortBy(_.name)
      .drop(page * pageSize)
      .take(pageSize)
      
    db.run(query.result).map(_.flatMap(rowToAggregate).toList)
  }
  
  override def count(activeOnly: Boolean = true): Future[Long] = {
    val query = if (activeOnly) malts.filter(_.isActive === true) else malts
    db.run(query.length.result).map(_.toLong)
  }
  
  override def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]] = {
    Future.successful(List.empty)
  }
  
  override def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]] = {
    Future.successful(PagedResult.empty)
  }
  
  override def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None,
    originCode: Option[String] = None,
    status: Option[String] = None,
    source: Option[String] = None,
    minCredibility: Option[Double] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    minExtraction: Option[Double] = None,
    minDiastaticPower: Option[Double] = None,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[PagedResult[MaltAggregate]] = {
    
    var query = malts.filter(_.isActive === true)
    
    maltType.foreach(mt => query = query.filter(_.maltType === mt))
    originCode.foreach(code => query = query.filter(_.originCode === code))
    source.foreach(src => query = query.filter(_.source === src))
    searchTerm.foreach(term => query = query.filter(_.name.like(s"%$term%")))
    
    val pagedQuery = query
      .sortBy(_.name)
      .drop(page * pageSize)
      .take(pageSize)
    
    for {
      rows <- db.run(pagedQuery.result)
      totalCount <- db.run(query.length.result)
    } yield {
      val aggregates = rows.flatMap(rowToAggregate).toList
      PagedResult(aggregates, page, pageSize, totalCount.toLong)
    }
  }
}
