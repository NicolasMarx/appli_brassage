package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import domain.yeasts.repositories.{YeastReadRepository, PaginatedResult}
import infrastructure.persistence.slick.tables.{YeastsTable, YeastRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

/**
 * Implémentation Slick du repository de lecture Yeast
 * CORRECTION: Structure de classe correcte avec toutes les méthodes
 */
@Singleton
class SlickYeastReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
  extends YeastReadRepository 
    with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._

  private val yeasts = YeastsTable.yeasts

  override def findById(id: YeastId): Future[Option[YeastAggregate]] = {
    val query = yeasts.filter(_.id === id.value)
    db.run(query.result.headOption).map(_.map(rowToAggregate))
  }

  // Méthode utilitaire (pas dans l'interface)
  def findAll(page: Int = 0, pageSize: Int = 20): Future[List[YeastAggregate]] = {
    val offset = page * pageSize
    val query = yeasts.sortBy(_.name).drop(offset).take(pageSize)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByName(name: YeastName): Future[Option[YeastAggregate]] = {
    val query = yeasts.filter(_.name === name.value)
    db.run(query.result.headOption).map(_.map(rowToAggregate))
  }

  // Méthode de compatibilité avec String
  def findByNameString(name: String): Future[Option[YeastAggregate]] = {
    val query = yeasts.filter(_.name === name)
    db.run(query.result.headOption).map(_.map(rowToAggregate))
  }

  override def findByType(yeastType: YeastType): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(_.yeastType === yeastType.name)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode de compatibilité avec String  
  def findByTypeString(yeastType: String): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(_.yeastType === yeastType)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByLaboratory(laboratory: YeastLaboratory): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(_.laboratory === laboratory.name)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode de compatibilité avec String
  def findByLaboratoryString(laboratory: String): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(_.laboratory === laboratory)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode utilitaire (pas dans l'interface)
  def findActive(): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(_.status === "ACTIVE")
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode utilitaire (pas dans l'interface)
  def search(
    query: String,
    yeastType: Option[String] = None,
    laboratory: Option[String] = None,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[List[YeastAggregate]] = {
    var baseQuery = yeasts.filter(_.name.toLowerCase like s"%${query.toLowerCase}%")

    yeastType.foreach { yt =>
      baseQuery = baseQuery.filter(_.yeastType === yt)
    }

    laboratory.foreach { lab =>
      baseQuery = baseQuery.filter(_.laboratory === lab)
    }

    val paginatedQuery = baseQuery.sortBy(_.name).drop(page * pageSize).take(pageSize)
    db.run(paginatedQuery.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode utilitaire (pas dans l'interface)
  def count(): Future[Long] = {
    db.run(yeasts.length.result).map(_.toLong)
  }

  // Méthode utilitaire (pas dans l'interface)
  def countByType(yeastType: String): Future[Long] = {
    val query = yeasts.filter(_.yeastType === yeastType).length
    db.run(query.result).map(_.toLong)
  }

  // Méthode utilitaire (pas dans l'interface)
  def existsByName(name: String): Future[Boolean] = {
    val query = yeasts.filter(_.name === name).exists
    db.run(query.result)
  }

  // CORRECTION: Méthode findByTypes ajoutée DANS la classe
  def findByTypes(yeastTypes: List[String]): Future[List[YeastAggregate]] = {
    if (yeastTypes.isEmpty) {
      Future.successful(List.empty)
    } else {
      val query = yeasts.filter(_.yeastType inSet yeastTypes)
      db.run(query.result).map(_.map(rowToAggregate).toList)
    }
  }

  // Implémentation des méthodes de l'interface YeastReadRepository
  
  override def findByLaboratoryAndStrain(
    laboratory: YeastLaboratory, 
    strain: YeastStrain
  ): Future[Option[YeastAggregate]] = {
    val query = yeasts.filter(row => 
      row.laboratory === laboratory.name && row.strain === strain.value
    )
    db.run(query.result.headOption).map(_.map(rowToAggregate))
  }
  
  override def findByFilter(filter: YeastFilter): Future[PaginatedResult[YeastAggregate]] = {
    
    // Version simplifiée pour tester uniquement le filtrage laboratory
    val baseQuery = filter.laboratory match {
      case Some(lab) => 
        println(s"Filtering by laboratory: ${lab.name}")
        yeasts.filter(_.laboratory === lab.name)
      case None =>
        yeasts
    }
    
    val countQuery = baseQuery.length
    val dataQuery = baseQuery
      .sortBy(_.name)
      .drop(filter.page * filter.size)
      .take(filter.size)
      
    for {
      totalCount <- db.run(countQuery.result)
      items <- db.run(dataQuery.result).recover {
        case ex => 
          println(s"Erreur requête: ${ex.getMessage}")
          Seq.empty
      }
    } yield {
      println(s"Results: totalCount=$totalCount, items.size=${items.size}")
      println(s"Converting ${items.size} rows to aggregates...")
      
      try {
        val aggregates = items.map { row =>
          println(s"Converting row: ${row.id} - ${row.name}")
          rowToAggregate(row)
        }.toList
        
        println(s"Successfully converted ${aggregates.size} aggregates")
        
        PaginatedResult(
          items = aggregates,
          totalCount = totalCount.toLong,
          page = filter.page,
          size = filter.size
        )
      } catch {
        case ex: Exception =>
          println(s"FATAL ERROR in aggregate conversion: ${ex.getMessage}")
          ex.printStackTrace()
          throw ex
      }
    }
  }
  
  override def findByStatus(status: YeastStatus): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(_.status === status.name)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }
  
  override def findByCharacteristics(characteristics: List[String]): Future[List[YeastAggregate]] = {
    if (characteristics.isEmpty) {
      Future.successful(List.empty)
    } else {
      // Simple implementation - recherche dans les caractéristiques JSON
      val query = yeasts.filter(_.status === "ACTIVE")
      db.run(query.result).map { rows =>
        rows.filter { row =>
          val charText = row.characteristics.toLowerCase
          characteristics.exists(char => charText.contains(char.toLowerCase))
        }.map(rowToAggregate).toList
      }
    }
  }
  
  override def searchText(query: String): Future[List[YeastAggregate]] = {
    val searchQuery = yeasts.filter { row =>
      row.name.toLowerCase.like(s"%${query.toLowerCase}%") ||
      row.characteristics.toLowerCase.like(s"%${query.toLowerCase}%")
    }
    db.run(searchQuery.result).map(_.map(rowToAggregate).toList)
  }
  
  override def countByStatus: Future[Map[YeastStatus, Long]] = {
    val query = yeasts.groupBy(_.status).map {
      case (status, group) => (status, group.length)
    }
    db.run(query.result).map { results =>
      results.flatMap { case (statusName, count) =>
        YeastStatus.fromName(statusName).map(_ -> count.toLong)
      }.toMap
    }
  }
  
  override def getStatsByLaboratory: Future[Map[YeastLaboratory, Long]] = {
    val query = yeasts.groupBy(_.laboratory).map {
      case (lab, group) => (lab, group.length)
    }
    db.run(query.result).map { results =>
      results.flatMap { case (labName, count) =>
        YeastLaboratory.fromName(labName).map(_ -> count.toLong)
      }.toMap
    }
  }
  
  override def getStatsByType: Future[Map[YeastType, Long]] = {
    val query = yeasts.groupBy(_.yeastType).map {
      case (yType, group) => (yType, group.length)
    }
    db.run(query.result).map { results =>
      results.flatMap { case (typeName, count) =>
        YeastType.fromName(typeName).map(_ -> count.toLong)
      }.toMap
    }
  }
  
  override def findMostPopular(limit: Int = 10): Future[List[YeastAggregate]] = {
    // Implémentation simple - les plus récents
    val query = yeasts
      .filter(_.status === "ACTIVE")
      .sortBy(_.updatedAt.desc)
      .take(limit)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }
  
  override def findRecentlyAdded(limit: Int = 5): Future[List[YeastAggregate]] = {
    val query = yeasts
      .sortBy(_.createdAt.desc)
      .take(limit)
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByTemperatureRange(minTemp: Int, maxTemp: Int): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(row =>
      row.temperatureMin <= maxTemp && row.temperatureMax >= minTemp
    )
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  override def findByAttenuationRange(minAttenuation: Int, maxAttenuation: Int): Future[List[YeastAggregate]] = {
    val query = yeasts.filter(row =>
      row.attenuationMin <= maxAttenuation && row.attenuationMax >= minAttenuation
    )
    db.run(query.result).map(_.map(rowToAggregate).toList)
  }

  // Méthode de conversion privée
  private def rowToAggregate(row: YeastRow): YeastAggregate = {
    YeastRow.toAggregate(row) match {
      case Right(aggregate) => aggregate
      case Left(error) => 
        println(s"ERREUR CONVERSION YeastRow: ${row.id} - ${row.name} - $error")
        throw new RuntimeException(s"Erreur conversion YeastRow: $error")
    }
  }
}
