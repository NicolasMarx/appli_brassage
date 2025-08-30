// app/application/services/hops/HopService.scala
package application.services.hops

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import domain.hops.model._
import infrastructure.persistence.slick.repositories.hops.{SlickHopReadRepository, SlickHopWriteRepository}

@Singleton
class HopService @Inject()(
                            hopReadRepository: SlickHopReadRepository,
                            hopWriteRepository: SlickHopWriteRepository
                          )(implicit ec: ExecutionContext) {

  /**
   * Récupère tous les houblons actifs avec pagination
   */
  def getActiveHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)] = {
    hopReadRepository.findActiveHops(page, pageSize)
  }

  /**
   * Récupère un houblon par ID
   */
  def getHopById(id: String): Future[Option[HopAggregate]] = {
    val hopIdResult = HopId.fromString(id)
    hopReadRepository.findById(hopIdResult)
  }

  /**
   * Recherche de houblons
   */
  def searchHops(
                  name: Option[String] = None,
                  originCode: Option[String] = None,
                  usage: Option[String] = None,
                  minAlphaAcid: Option[Double] = None,
                  maxAlphaAcid: Option[Double] = None,
                  activeOnly: Boolean = true
                ): Future[List[HopAggregate]] = {
    hopReadRepository.searchHops(name, originCode, usage, minAlphaAcid, maxAlphaAcid, activeOnly)
  }

  /**
   * Trouve les houblons par origine
   */
  def getHopsByOrigin(originCode: String): Future[List[HopAggregate]] = {
    hopReadRepository.findByOrigin(originCode, activeOnly = true)
  }

  /**
   * Trouve les houblons par usage
   */
  def getHopsByUsage(usage: String): Future[List[HopAggregate]] = {
    hopReadRepository.findByUsage(usage, activeOnly = true)
  }
}