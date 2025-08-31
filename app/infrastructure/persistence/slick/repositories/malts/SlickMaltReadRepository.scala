package infrastructure.persistence.slick.repositories.malts

import domain.malts.model._
import domain.malts.repositories._
import domain.shared._
import infrastructure.persistence.slick.tables.{MaltTables, OriginTables}
import infrastructure.persistence.slick.SlickDatabase
import slick.jdbc.PostgresProfile.api._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

/**
 * Implémentation Slick du MaltReadRepository
 * Suit le pattern SlickHopReadRepository pour cohérence
 */
@Singleton
class SlickMaltReadRepository @Inject()(
                                         db: SlickDatabase
                                       )(implicit ec: ExecutionContext) extends MaltReadRepository {

  import MaltTables._
  import OriginTables.origins

  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    val query = malts
      .filter(_.id === java.util.UUID.fromString(id.value))
      .join(origins).on(_.originCode === _.id)
      .result
      .headOption

    db.run(query).map {
      case Some((maltRow, originRow)) =>
        val origin = OriginTables.rowToOrigin(originRow)
        MaltTables.rowToAggregate(maltRow, origin).toOption
      case None => None
    }
  }

  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    val query = malts
      .filter(_.name.toLowerCase === name.toLowerCase.trim)
      .join(origins).on(_.originCode === _.id)
      .result
      .headOption

    db.run(query).map {
      case Some((maltRow, originRow)) =>
        val origin = OriginTables.rowToOrigin(originRow)
        MaltTables.rowToAggregate(maltRow, origin).toOption
      case None => None
    }
  }

  override def findAll(page: Int, pageSize: Int): Future[PagedResult[MaltAggregate]] = {
    findByQuery(malts, page, pageSize)
  }

  override def findActive(page: Int, pageSize: Int): Future[PagedResult[MaltAggregate]] = {
    findByQuery(activeFilter, page, pageSize)
  }

  override def findByType(maltType: MaltType, page: Int, pageSize: Int): Future[PagedResult[MaltAggregate]] = {
    val query = malts.filter(_.maltType === maltType.name)
    findByQuery(query, page, pageSize)
  }

  override def findByColorRange(
                                 minEBC: Double,
                                 maxEBC: Double,
                                 page: Int,
                                 pageSize: Int
                               ): Future[PagedResult[MaltAggregate]] = {
    val query = malts.filter(m => m.ebcColor >= minEBC && m.ebcColor <= maxEBC)
    findByQuery(query, page, pageSize)
  }

  override def findBaseMalts(
                              minDiastaticPower: Option[Double],
                              page: Int,
                              pageSize: Int
                            ): Future[PagedResult[MaltAggregate]] = {
    val baseQuery = malts.filter(_.maltType === "BASE")
    val finalQuery = minDiastaticPower match {
      case Some(minPower) => baseQuery.filter(_.diastaticPower >= minPower)
      case None => baseQuery
    }
    findByQuery(finalQuery, page, pageSize)
  }

  override def findByFlavorProfiles(
                                     flavorProfiles: List[String],
                                     page: Int,
                                     pageSize: Int
                                   ): Future[PagedResult[MaltAggregate]] = {
    if (flavorProfiles.isEmpty) {
      Future.successful(PagedResult(List.empty, page, pageSize, 0, hasNext = false))
    } else {
      // Recherche JSONB contient au moins un des profils
      val profileConditions = flavorProfiles.map { profile =>
        sql"""flavor_profiles::jsonb ? ${profile}""".as[Boolean]
      }
      val combinedCondition = profileConditions.reduce(_ || _)

      val query = malts.filter(_ => combinedCondition)
      findByQuery(query, page, pageSize)
    }
  }

  override def findByOrigin(
                             originCode: String,
                             page: Int,
                             pageSize: Int
                           ): Future[PagedResult[MaltAggregate]] = {
    val query = malts.filter(_.originCode === originCode)
    findByQuery(query, page, pageSize)
  }

  override def findByMinCredibility(
                                     minScore: Int,
                                     page: Int,
                                     pageSize: Int
                                   ): Future[PagedResult[MaltAggregate]] = {
    val query = malts.filter(_.credibilityScore >= minScore)
    findByQuery(query, page, pageSize)
  }

  override def findBySource(
                             source: MaltSource,
                             page: Int,
                             pageSize: Int
                           ): Future[PagedResult[MaltAggregate]] = {
    val query = malts.filter(_.source === source.name)
    findByQuery(query, page, pageSize)
  }

  override def search(
                       searchTerm: String,
                       page: Int,
                       pageSize: Int
                     ): Future[PagedResult[MaltAggregate]] = {
    val term = s"%${searchTerm.toLowerCase.trim}%"
    val query = malts.filter { m =>
      m.name.toLowerCase.like(term) ||
        m.description.toLowerCase.like(term) ||
        m.flavorProfiles.toLowerCase.like(term)
    }
    findByQuery(query, page, pageSize)
  }

  override def findByFilters(
                              maltType: Option[MaltType],
                              minEBC: Option[Double],
                              maxEBC: Option[Double],
                              minExtraction: Option[Double],
                              minDiastaticPower: Option[Double],
                              originCode: Option[String],
                              status: Option[MaltStatus],
                              source: Option[MaltSource],
                              minCredibility: Option[Int],
                              flavorProfiles: List[String],
                              searchTerm: Option[String],
                              page: Int,
                              pageSize: Int
                            ): Future[PagedResult[MaltAggregate]] = {

    var query = malts.asInstanceOf[Query[MaltTable, MaltRow, Seq]]

    // Application des filtres conditionnels
    maltType.foreach(t => query = query.filter(_.maltType === t.name))
    minEBC.foreach(min => query = query.filter(_.ebcColor >= min))
    maxEBC.foreach(max => query = query.filter(_.ebcColor <= max))
    minExtraction.foreach(min => query = query.filter(_.extractionRate >= min))
    minDiastaticPower.foreach(min => query = query.filter(_.diastaticPower >= min))
    originCode.foreach(code => query = query.filter(_.originCode === code))
    status.foreach(s => query = query.filter(_.status === s.name))
    source.foreach(s => query = query.filter(_.source === s.name))
    minCredibility.foreach(min => query = query.filter(_.credibilityScore >= min))

    // Recherche textuelle
    searchTerm.foreach { term =>
      val searchPattern = s"%${term.toLowerCase.trim}%"
      query = query.filter { m =>
        m.name.toLowerCase.like(searchPattern) ||
          m.description.toLowerCase.like(searchPattern)
      }
    }

    // Profils arômes (simplifié pour éviter SQL complexe)
    if (flavorProfiles.nonEmpty) {
      // Filtrage côté application après récupération
      findByQuery(query, page, pageSize * 2).flatMap { result =>
        val filteredItems = result.items.filter { malt =>
          flavorProfiles.exists(profile =>
            malt.flavorProfiles.exists(_.toLowerCase.contains(profile.toLowerCase))
          )
        }
        val finalItems = filteredItems.take(pageSize)
        Future.successful(PagedResult(
          finalItems,
          page,
          pageSize,
          filteredItems.length,
          finalItems.length == pageSize
        ))
      }
    } else {
      findByQuery(query, page, pageSize)
    }
  }

  override def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]] = {
    val query = maltSubstitutions
      .filter(_.originalMaltId === java.util.UUID.fromString(maltId.value))
      .join(malts).on(_.substituteMaltId === _.id)
      .join(malts).on(_._1.originalMaltId === _.id)
      .join(origins).on(_._1._2.originCode === _.id)
      .join(origins).on(_._1._1._2.originCode === _.id)
      .result

    db.run(query).map { results =>
      results.flatMap { case ((((substRow, substituteMalt), originalMalt), substituteOrigin), originalOrigin) =>
        for {
          original <- MaltTables.rowToAggregate(originalMalt, OriginTables.rowToOrigin(originalOrigin)).toOption
          substitute <- MaltTables.rowToAggregate(substituteMalt, OriginTables.rowToOrigin(substituteOrigin)).toOption
        } yield MaltSubstitution(
          originalMalt = original,
          substituteMalt = substitute,
          substitutionRatio = substRow.substitutionRatio,
          compatibilityNotes = substRow.compatibilityNotes
        )
      }.toList
    }
  }

  override def findCompatibleWithBeerStyle(
                                            beerStyleId: String,
                                            page: Int,
                                            pageSize: Int
                                          ): Future[PagedResult[MaltCompatibility]] = {
    val query = maltBeerStyles
      .filter(_.beerStyleId === beerStyleId)
      .join(malts).on(_.maltId === _.id)
      .join(origins).on(_._2.originCode === _.id)
      .sortBy(_._1._1.compatibilityScore.desc.nullsLast)
      .drop(page * pageSize)
      .take(pageSize)
      .result

    val countQuery = maltBeerStyles.filter(_.beerStyleId === beerStyleId).length.result

    for {
      results <- db.run(query)
      totalCount <- db.run(countQuery)
    } yield {
      val compatibilities = results.flatMap { case ((styleRow, maltRow), originRow) =>
        MaltTables.rowToAggregate(maltRow, OriginTables.rowToOrigin(originRow)).toOption.map { malt =>
          MaltCompatibility(
            malt = malt,
            beerStyleId = styleRow.beerStyleId,
            compatibilityScore = styleRow.compatibilityScore,
            typicalPercentage = styleRow.typicalPercentage
          )
        }
      }.toList

      PagedResult(
        compatibilities,
        page,
        pageSize,
        totalCount,
        (page + 1) * pageSize < totalCount
      )
    }
  }

  override def getStatistics(): Future[MaltStatistics] = {
    val totalQuery = malts.length.result
    val activeQuery = activeFilter.length.result

    val typeStatsQuery = malts
      .groupBy(_.maltType)
      .map { case (maltType, group) => (maltType, group.length) }
      .result

    val originStatsQuery = malts
      .groupBy(_.originCode)
      .map { case (origin, group) => (origin, group.length) }
      .result

    val sourceStatsQuery = malts
      .groupBy(_.source)
      .map { case (source, group) => (source, group.length) }
      .result

    val averagesQuery = malts
      .map(m => (m.credibilityScore, m.ebcColor, m.extractionRate))
      .result

    for {
      total <- db.run(totalQuery)
      active <- db.run(activeQuery)
      typeStats <- db.run(typeStatsQuery)
      originStats <- db.run(originStatsQuery)
      sourceStats <- db.run(sourceStatsQuery)
      averages <- db.run(averagesQuery)
    } yield {
      val (avgCredibility, avgEBC, avgExtraction) = if (averages.nonEmpty) {
        val credSum = averages.map(_._1).sum.toDouble
        val ebcSum = averages.map(_._2).sum
        val extractSum = averages.map(_._3).sum
        val count = averages.length
        (credSum / count, ebcSum / count, extractSum / count)
      } else {
        (0.0, 0.0, 0.0)
      }

      MaltStatistics(
        totalCount = total,
        activeCount = active,
        countByType = typeStats.flatMap { case (typeName, count) =>
          MaltType.fromName(typeName).map(_ -> count)
        }.toMap,
        countByOrigin = originStats.toMap,
        countBySource = sourceStats.flatMap { case (sourceName, count) =>
          MaltSource.fromName(sourceName).map(_ -> count)
        }.toMap,
        averageCredibilityScore = avgCredibility,
        averageEBC = avgEBC,
        averageExtraction = avgExtraction
      )
    }
  }

  override def findNeedingReview(
                                  maxCredibility: Int,
                                  page: Int,
                                  pageSize: Int
                                ): Future[PagedResult[MaltAggregate]] = {
    val query = malts
      .filter(_.credibilityScore <= maxCredibility)
      .sortBy(_.credibilityScore.asc)
    findByQuery(query, page, pageSize)
  }

  override def findPopular(limit: Int): Future[List[MaltAggregate]] = {
    val popularityQuery = maltBeerStyles
      .groupBy(_.maltId)
      .map { case (maltId, group) => (maltId, group.length) }

    val query = malts
      .joinLeft(popularityQuery).on(_.id === _._1)
      .sortBy(_._2.map(_._2).getOrElse(0).desc)
      .take(limit)
      .join(origins).on(_._1.originCode === _.id)
      .result

    db.run(query).map { results =>
      results.flatMap { case ((maltRow, _), originRow) =>
        MaltTables.rowToAggregate(maltRow, OriginTables.rowToOrigin(originRow)).toOption
      }.toList
    }
  }

  override def exists(id: MaltId): Future[Boolean] = {
    val query = malts.filter(_.id === java.util.UUID.fromString(id.value)).exists.result
    db.run(query)
  }

  override def existsByName(name: String): Future[Boolean] = {
    val query = malts.filter(_.name.toLowerCase === name.toLowerCase.trim).exists.result
    db.run(query)
  }

  override def countActive(): Future[Int] = {
    db.run(activeFilter.length.result)
  }

  /**
   * Méthode utilitaire pour exécuter une query avec pagination
   */
  private def findByQuery(
                           query: Query[MaltTable, MaltRow, Seq],
                           page: Int,
                           pageSize: Int
                         ): Future[PagedResult[MaltAggregate]] = {
    val pagedQuery = query
      .sortBy(_.name.asc)
      .drop(page * pageSize)
      .take(pageSize)
      .join(origins).on(_.originCode === _.id)
      .result

    val countQuery = query.length.result

    for {
      results <- db.run(pagedQuery)
      totalCount <- db.run(countQuery)
    } yield {
      val malts = results.flatMap { case (maltRow, originRow) =>
        MaltTables.rowToAggregate(maltRow, OriginTables.rowToOrigin(originRow)).toOption
      }.toList

      PagedResult(
        malts,
        page,
        pageSize,
        totalCount,
        (page + 1) * pageSize < totalCount
      )
    }
  }
}