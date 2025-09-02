package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import domain.yeasts.repositories.{YeastRepository, PaginatedResult}
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Repository combiné qui délègue aux implémentations read/write
 * Implémentation du pattern Repository complet
 */
@Singleton
class SlickYeastRepository @Inject()(
  readRepository: SlickYeastReadRepository,
  writeRepository: SlickYeastWriteRepository
)(implicit ec: ExecutionContext) extends YeastRepository {

  // ==========================================================================
  // DÉLÉGATION AUX REPOSITORIES SPÉCIALISÉS
  // ==========================================================================

  // Read operations
  override def findById(yeastId: YeastId): Future[Option[YeastAggregate]] = 
    readRepository.findById(yeastId)

  override def findByName(name: YeastName): Future[Option[YeastAggregate]] = 
    readRepository.findByName(name)

  override def findByLaboratoryAndStrain(laboratory: YeastLaboratory, strain: YeastStrain): Future[Option[YeastAggregate]] = 
    readRepository.findByLaboratoryAndStrain(laboratory, strain)

  override def findByFilter(filter: YeastFilter): Future[PaginatedResult[YeastAggregate]] = 
    readRepository.findByFilter(filter)

  override def findByType(yeastType: YeastType): Future[List[YeastAggregate]] = 
    readRepository.findByType(yeastType)

  override def findByLaboratory(laboratory: YeastLaboratory): Future[List[YeastAggregate]] = 
    readRepository.findByLaboratory(laboratory)

  override def findByStatus(status: YeastStatus): Future[List[YeastAggregate]] = 
    readRepository.findByStatus(status)

  override def findByAttenuationRange(minAttenuation: Int, maxAttenuation: Int): Future[List[YeastAggregate]] = 
    readRepository.findByAttenuationRange(minAttenuation, maxAttenuation)

  override def findByTemperatureRange(minTemp: Int, maxTemp: Int): Future[List[YeastAggregate]] = 
    readRepository.findByTemperatureRange(minTemp, maxTemp)

  override def findByCharacteristics(characteristics: List[String]): Future[List[YeastAggregate]] = 
    readRepository.findByCharacteristics(characteristics)

  override def searchText(query: String): Future[List[YeastAggregate]] = 
    readRepository.searchText(query)

  override def countByStatus: Future[Map[YeastStatus, Long]] = 
    readRepository.countByStatus

  override def getStatsByLaboratory: Future[Map[YeastLaboratory, Long]] = 
    readRepository.getStatsByLaboratory

  override def getStatsByType: Future[Map[YeastType, Long]] = 
    readRepository.getStatsByType

  override def findMostPopular(limit: Int): Future[List[YeastAggregate]] = 
    readRepository.findMostPopular(limit)

  override def findRecentlyAdded(limit: Int): Future[List[YeastAggregate]] = 
    readRepository.findRecentlyAdded(limit)

  // Write operations
  override def create(yeast: YeastAggregate): Future[YeastAggregate] = 
    writeRepository.create(yeast)

  override def update(yeast: YeastAggregate): Future[YeastAggregate] = 
    writeRepository.update(yeast)

  override def archive(yeastId: YeastId, reason: Option[String]): Future[Unit] = 
    writeRepository.archive(yeastId, reason)

  override def activate(yeastId: YeastId): Future[Unit] = 
    writeRepository.activate(yeastId)

  override def deactivate(yeastId: YeastId, reason: Option[String]): Future[Unit] = 
    writeRepository.deactivate(yeastId, reason)

  override def changeStatus(yeastId: YeastId, newStatus: YeastStatus, reason: Option[String]): Future[Unit] = 
    writeRepository.changeStatus(yeastId, newStatus, reason)

  override def saveEvents(yeastId: YeastId, events: List[YeastEvent], expectedVersion: Long): Future[Unit] = 
    writeRepository.saveEvents(yeastId, events, expectedVersion)

  override def getEvents(yeastId: YeastId): Future[List[YeastEvent]] = 
    writeRepository.getEvents(yeastId)

  override def getEventsSinceVersion(yeastId: YeastId, sinceVersion: Long): Future[List[YeastEvent]] = 
    writeRepository.getEventsSinceVersion(yeastId, sinceVersion)

  override def createBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]] = 
    writeRepository.createBatch(yeasts)

  override def updateBatch(yeasts: List[YeastAggregate]): Future[List[YeastAggregate]] = 
    writeRepository.updateBatch(yeasts)

  // ==========================================================================
  // OPÉRATIONS SPÉCIFIQUES AU REPOSITORY COMBINÉ
  // ==========================================================================

  override def save(yeast: YeastAggregate): Future[YeastAggregate] = {
    findById(yeast.id).flatMap {
      case Some(_) => update(yeast)
      case None => create(yeast)
    }
  }

  override def delete(yeastId: YeastId): Future[Unit] = {
    // Pour l'instant, on archive plutôt que de supprimer
    archive(yeastId, Some("Suppression demandée"))
  }

  override def exists(yeastId: YeastId): Future[Boolean] = {
    findById(yeastId).map(_.isDefined)
  }
}
