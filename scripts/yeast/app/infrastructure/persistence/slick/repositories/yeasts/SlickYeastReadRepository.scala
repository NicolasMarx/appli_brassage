package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import domain.yeasts.repositories.{YeastReadRepository, PaginatedResult}
import infrastructure.persistence.slick.tables.{YeastsTable, YeastRow}
import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

/**
 * Implémentation Slick du repository de lecture des levures
 */
@Singleton
class SlickYeastReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends YeastReadRepository with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  private val yeasts = YeastsTable.yeasts

  override def findById(yeastId: YeastId): Future[Option[YeastAggregate]] = {
    db.run(yeasts.filter(_.id === yeastId.value).result.headOption)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption))
  }

  override def findByName(name: YeastName): Future[Option[YeastAggregate]] = {
    db.run(yeasts.filter(_.name === name.value).result.headOption)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption))
  }

  override def findByLaboratoryAndStrain(
    laboratory: YeastLaboratory, 
    strain: YeastStrain
  ): Future[Option[YeastAggregate]] = {
    db.run(
      yeasts
        .filter(y => y.laboratory === laboratory.name && y.strain === strain.value)
        .result.headOption
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption))
  }

  override def findByFilter(filter: YeastFilter): Future[PaginatedResult[YeastAggregate]] = {
    val baseQuery = yeasts.filter(buildWhereClause(filter))
    
    val countQuery = baseQuery.length
    val dataQuery = baseQuery
      .drop(filter.page * filter.size)
      .take(filter.size)
      .sortBy(_.name) // Tri par défaut par nom
    
    val combinedQuery = for {
      total <- countQuery.result
      items <- dataQuery.result
    } yield (total, items)
    
    db.run(combinedQuery).map { case (total, rows) =>
      val aggregates = rows.flatMap(row => YeastRow.toAggregate(row).toOption).toList
      PaginatedResult(aggregates, total.toLong, filter.page, filter.size)
    }
  }

  override def findByType(yeastType: YeastType): Future[List[YeastAggregate]] = {
    db.run(yeasts.filter(_.yeastType === yeastType.name).result)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByLaboratory(laboratory: YeastLaboratory): Future[List[YeastAggregate]] = {
    db.run(yeasts.filter(_.laboratory === laboratory.name).result)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByStatus(status: YeastStatus): Future[List[YeastAggregate]] = {
    db.run(yeasts.filter(_.status === status.name).result)
      .map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByAttenuationRange(
    minAttenuation: Int, 
    maxAttenuation: Int
  ): Future[List[YeastAggregate]] = {
    db.run(
      yeasts.filter(y => 
        y.attenuationMin >= minAttenuation && y.attenuationMax <= maxAttenuation
      ).result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByTemperatureRange(minTemp: Int, maxTemp: Int): Future[List[YeastAggregate]] = {
    db.run(
      yeasts.filter(y => 
        y.temperatureMin >= minTemp && y.temperatureMax <= maxTemp
      ).result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def findByCharacteristics(characteristics: List[String]): Future[List[YeastAggregate]] = {
    if (characteristics.isEmpty) {
      Future.successful(List.empty)
    } else {
      val pattern = characteristics.mkString("|") // OR pattern pour recherche
      db.run(
        yeasts.filter(_.characteristics.toLowerCase like s"%${pattern.toLowerCase}%").result
      ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
    }
  }

  override def searchText(query: String): Future[List[YeastAggregate]] = {
    val searchPattern = s"%${query.toLowerCase}%"
    db.run(
      yeasts.filter(y => 
        y.name.toLowerCase.like(searchPattern) || 
        y.characteristics.toLowerCase.like(searchPattern)
      ).result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  override def countByStatus: Future[Map[YeastStatus, Long]] = {
    db.run(
      yeasts.groupBy(_.status).map { case (status, group) => 
        (status, group.length) 
      }.result
    ).map(_.toMap.flatMap { case (statusName, count) =>
      YeastStatus.fromName(statusName).map(_ -> count.toLong)
    })
  }

  override def getStatsByLaboratory: Future[Map[YeastLaboratory, Long]] = {
    db.run(
      yeasts.groupBy(_.laboratory).map { case (lab, group) => 
        (lab, group.length) 
      }.result
    ).map(_.toMap.flatMap { case (labName, count) =>
      YeastLaboratory.fromName(labName).map(_ -> count.toLong)
    })
  }

  override def getStatsByType: Future[Map[YeastType, Long]] = {
    db.run(
      yeasts.groupBy(_.yeastType).map { case (yeastType, group) => 
        (yeastType, group.length) 
      }.result
    ).map(_.toMap.flatMap { case (typeName, count) =>
      YeastType.fromName(typeName).map(_ -> count.toLong)
    })
  }

  override def findMostPopular(limit: Int = 10): Future[List[YeastAggregate]] = {
    // TODO: Implémenter selon métriques d'usage réelles
    // Pour l'instant, retourne les plus récentes
    findRecentlyAdded(limit)
  }

  override def findRecentlyAdded(limit: Int = 5): Future[List[YeastAggregate]] = {
    db.run(
      yeasts
        .sortBy(_.createdAt.desc)
        .take(limit)
        .result
    ).map(_.flatMap(row => YeastRow.toAggregate(row).toOption).toList)
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES - CONSTRUCTION REQUÊTES
  // ==========================================================================

  private def buildWhereClause(filter: YeastFilter): YeastsTable => Rep[Boolean] = { yeast =>
    var conditions: Rep[Boolean] = LiteralColumn(true)

    filter.name.foreach { name =>
      conditions = conditions && yeast.name.toLowerCase.like(s"%${name.toLowerCase}%")
    }

    filter.laboratory.foreach { lab =>
      conditions = conditions && yeast.laboratory === lab.name
    }

    filter.yeastType.foreach { yType =>
      conditions = conditions && yeast.yeastType === yType.name
    }

    filter.minAttenuation.foreach { minAtt =>
      conditions = conditions && yeast.attenuationMax >= minAtt
    }

    filter.maxAttenuation.foreach { maxAtt =>
      conditions = conditions && yeast.attenuationMin <= maxAtt
    }

    filter.minTemperature.foreach { minTemp =>
      conditions = conditions && yeast.temperatureMax >= minTemp
    }

    filter.maxTemperature.foreach { maxTemp =>
      conditions = conditions && yeast.temperatureMin <= maxTemp
    }

    filter.minAlcoholTolerance.foreach { minAlc =>
      conditions = conditions && yeast.alcoholTolerance >= minAlc
    }

    filter.maxAlcoholTolerance.foreach { maxAlc =>
      conditions = conditions && yeast.alcoholTolerance <= maxAlc
    }

    filter.flocculation.foreach { floc =>
      conditions = conditions && yeast.flocculation === floc.name
    }

    if (filter.characteristics.nonEmpty) {
      val charPattern = filter.characteristics.mkString("|").toLowerCase
      conditions = conditions && yeast.characteristics.toLowerCase.like(s"%$charPattern%")
    }

    if (filter.status.nonEmpty) {
      val statusNames = filter.status.map(_.name)
      conditions = conditions && yeast.status.inSet(statusNames)
    }

    conditions
  }
}
