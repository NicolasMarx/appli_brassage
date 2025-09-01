package domain.yeasts.repositories

import domain.yeasts.model._
import scala.concurrent.Future

/**
 * Repository principal pour les opérations CRUD des levures
 * Interface générique combinant lecture et écriture
 */
trait YeastRepository extends YeastReadRepository with YeastWriteRepository {
  
  /**
   * Opération transactionnelle : créer ou mettre à jour
   */
  def save(yeast: YeastAggregate): Future[YeastAggregate]
  
  /**
   * Opération transactionnelle : supprimer définitivement
   */  
  def delete(yeastId: YeastId): Future[Unit]
  
  /**
   * Vérification existence
   */
  def exists(yeastId: YeastId): Future[Boolean]
}
