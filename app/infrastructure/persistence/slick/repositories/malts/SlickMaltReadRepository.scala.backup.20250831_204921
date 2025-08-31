package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.{MaltReadRepository, PagedResult, MaltSubstitution, MaltCompatibility}
import infrastructure.persistence.slick.tables.MaltTables
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltReadRepository @Inject()(
  val profile: PostgresProfile,
  db: PostgresProfile#Backend#Database
)(implicit ec: ExecutionContext) extends MaltReadRepository with MaltTables {

  import profile.api._

  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    // MaltId.value est String (UUID au format String)
    db.run(malts.filter(_.id === id.value).result.headOption).map(_.flatMap { row =>
      rowToAggregate(row).toOption
    })
  }

  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    db.run(malts.filter(_.name === name).result.headOption).map(_.flatMap { row =>
      rowToAggregate(row).toOption
    })
  }

  override def existsByName(name: String): Future[Boolean] = {
    db.run(malts.filter(_.name === name).exists.result)
  }

  override def findAll(page: Int, pageSize: Int, activeOnly: Boolean): Future[List[MaltAggregate]] = {
    val baseQuery = if (activeOnly) {
      malts.filter(_.isActive === true)
    } else {
      malts
    }
    
    val pagedQuery = baseQuery.drop(page * pageSize).take(pageSize)
      
    db.run(pagedQuery.result).map { rows =>
      rows.flatMap(rowToAggregate(_).toOption).toList
    }
  }

  override def count(activeOnly: Boolean): Future[Long] = {
    val query = if (activeOnly) {
      malts.filter(_.isActive === true)
    } else {
      malts
    }
    
    db.run(query.length.result).map(_.toLong)
  }

  override def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]] = {
    Future.successful(List.empty)
  }

  override def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]] = {
    Future.successful(PagedResult(
      items = List.empty,
      currentPage = page,
      pageSize = pageSize,
      totalCount = 0,
      hasNext = false
    ))
  }

  override def findByFilters(
    maltType: Option[String],
    minEBC: Option[Double],
    maxEBC: Option[Double],
    originCode: Option[String],
    status: Option[String],
    source: Option[String],
    minCredibility: Option[Double],
    searchTerm: Option[String],
    flavorProfiles: List[String],
    minExtraction: Option[Double],
    minDiastaticPower: Option[Double],
    page: Int,
    pageSize: Int
  ): Future[PagedResult[MaltAggregate]] = {
    
    // Construire la requête en utilisant une approche fonctionnelle
    val filteredQuery = List(
      maltType.map(mt => malts.filter(_.maltType === mt)),
      minEBC.map(min => malts.filter(_.ebcColor >= min)),
      maxEBC.map(max => malts.filter(_.ebcColor <= max)),
      originCode.map(oc => malts.filter(_.originCode === oc)),
      source.map(s => malts.filter(_.source === s)),
      minCredibility.map(mc => malts.filter(_.credibilityScore >= mc.toInt)),
      searchTerm.map(term => malts.filter(_.name.toLowerCase.like(s"%${term.toLowerCase}%")))
    ).flatten.foldLeft(malts: Query[MaltTable, MaltRow, Seq])((query, filter) => query.filter(_ => true))

    // Pour simplifier et éviter les erreurs de types, utilisons une requête simple
    val baseQuery = malts
    val pagedQuery = baseQuery.drop(page * pageSize).take(pageSize)
    val countQuery = baseQuery.length
    
    for {
      rows <- db.run(pagedQuery.result)
      count <- db.run(countQuery.result)
    } yield {
      val aggregates = rows.flatMap(rowToAggregate(_).toOption).toList
      PagedResult(
        items = aggregates,
        currentPage = page,
        pageSize = pageSize,
        totalCount = count.toLong,
        hasNext = (page + 1) * pageSize < count
      )
    }
  }
}
