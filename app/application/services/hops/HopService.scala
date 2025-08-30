package application.services.hops

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import domain.hops.model._
import domain.hops.repositories.{HopReadRepository, HopWriteRepository}
import domain.shared.NonEmptyString

/**
 * Service d'application pour la gestion des houblons
 * Orchestre les opérations entre les repositories et applique la logique métier
 */
@Singleton
class HopService @Inject()(
                            readRepository: HopReadRepository,
                            writeRepository: HopWriteRepository
                          )(implicit ec: ExecutionContext) {

  /**
   * Récupère un houblon par son identifiant
   */
  def getHopById(id: String): Future[Option[HopAggregate]] = {
    val hopId = HopId(id)
    readRepository.byId(hopId)
  }

  /**
   * Récupère un houblon par son nom
   */
  def getHopByName(name: String): Future[Option[HopAggregate]] = {
    val hopName = NonEmptyString(name)
    readRepository.byName(hopName)
  }

  /**
   * Récupère tous les houblons actifs avec pagination
   */
  def getActiveHops(page: Int = 0, size: Int = 20): Future[(List[HopAggregate], Int)] = {
    readRepository.findActiveHops(page, size)
  }

  /**
   * Récupère tous les houblons avec pagination
   */
  def getAllHops(page: Int = 0, size: Int = 20): Future[(List[HopAggregate], Int)] = {
    readRepository.all(page, size)
  }

  /**
   * Recherche des houblons selon des critères
   */
  def searchHops(
                  name: Option[String] = None,
                  originCode: Option[String] = None,
                  usage: Option[String] = None,
                  minAlphaAcid: Option[Double] = None,
                  maxAlphaAcid: Option[Double] = None,
                  activeOnly: Boolean = true
                ): Future[List[HopAggregate]] = {
    readRepository.searchHops(name, originCode, usage, minAlphaAcid, maxAlphaAcid, activeOnly)
  }

  /**
   * Récupère les houblons par statut
   */
  def getHopsByStatus(status: HopStatus): Future[Seq[HopAggregate]] = {
    val statusString = status match {
      case HopStatus.Active => "ACTIVE"
      case HopStatus.Discontinued => "DISCONTINUED"
      case HopStatus.Limited => "LIMITED"
    }
    readRepository.byStatus(statusString)
  }

  /**
   * Récupère les houblons par usage
   */
  def getHopsByUsage(usage: HopUsage, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    val usageString = usage match {
      case HopUsage.Bittering => "BITTERING"
      case HopUsage.Aroma => "AROMA"
      case HopUsage.DualPurpose => "DUAL_PURPOSE"
      case HopUsage.NobleHop => "NOBLE_HOP"
    }
    readRepository.findByUsage(usageString, activeOnly)
  }

  /**
   * Récupère les houblons par origine
   */
  def getHopsByOrigin(originCode: String, activeOnly: Boolean = true): Future[List[HopAggregate]] = {
    readRepository.findByOrigin(originCode, activeOnly)
  }

  /**
   * Récupère les houblons découverts par IA
   */
  def getAiDiscoveredHops(limit: Int = 50): Future[Seq[HopAggregate]] = {
    readRepository.aiDiscovered(limit)
  }

  /**
   * Sauvegarde un nouveau houblon
   */
  def createHop(hopData: CreateHopData): Future[HopAggregate] = {
    val hop = HopAggregate.create(
      id = hopData.id,
      name = hopData.name,
      alphaAcid = hopData.alphaAcid,
      origin = hopData.origin,
      usage = hopData.usage
    )

    writeRepository.save(hop)
  }

  /**
   * Met à jour un houblon existant
   */
  def updateHop(hopId: String, updateData: UpdateHopData): Future[Option[HopAggregate]] = {
    val id = HopId(hopId)

    for {
      existingHopOpt <- readRepository.byId(id)
      result <- existingHopOpt match {
        case Some(existingHop) =>
          val updatedHop = applyUpdates(existingHop, updateData)
          writeRepository.update(updatedHop)
        case None =>
          Future.successful(None)
      }
    } yield result
  }

  /**
   * Supprime un houblon
   */
  def deleteHop(hopId: String): Future[Boolean] = {
    val id = HopId(hopId)
    writeRepository.delete(id)
  }

  /**
   * Vérifie si un houblon existe
   */
  def hopExists(hopId: String): Future[Boolean] = {
    val id = HopId(hopId)
    writeRepository.exists(id)
  }

  /**
   * Vérifie si un nom de houblon existe déjà
   */
  def hopNameExists(name: String): Future[Boolean] = {
    readRepository.existsByName(name)
  }

  /**
   * Sauvegarde plusieurs houblons en lot
   */
  def createHopsInBatch(hopDataList: List[CreateHopData]): Future[List[HopAggregate]] = {
    val hops = hopDataList.map { hopData =>
      HopAggregate.create(
        id = hopData.id,
        name = hopData.name,
        alphaAcid = hopData.alphaAcid,
        origin = hopData.origin,
        usage = hopData.usage
      )
    }

    writeRepository.saveAll(hops)
  }

  /**
   * Applique les mises à jour à un houblon existant
   */
  private def applyUpdates(hop: HopAggregate, updateData: UpdateHopData): HopAggregate = {
    var updated = hop

    updateData.alphaAcid.foreach { alphaAcid =>
      updated = updated.updateAlphaAcid(AlphaAcidPercentage(alphaAcid))
    }

    updateData.betaAcid.foreach { betaAcid =>
      updated = updated.updateBetaAcid(betaAcid.map(AlphaAcidPercentage(_)))
    }

    updateData.description.foreach { desc =>
      updated = updated.updateDescription(desc)
    }

    updateData.usage.foreach { usage =>
      updated = updated.updateUsage(usage)
    }

    updateData.status.foreach { status =>
      updated = updated.updateStatus(status)
    }

    updateData.credibilityScore.foreach { score =>
      updated = updated.updateCredibilityScore(score)
    }

    updateData.aromaProfilesToAdd.foreach { aromas =>
      aromas.foreach { aroma =>
        updated = updated.addAromaProfile(aroma)
      }
    }

    updateData.aromaProfilesToRemove.foreach { aromas =>
      aromas.foreach { aroma =>
        updated = updated.removeAromaProfile(aroma)
      }
    }

    updated
  }
}

/**
 * Données pour créer un nouveau houblon
 */
case class CreateHopData(
                          id: String,
                          name: String,
                          alphaAcid: Double,
                          origin: HopOrigin,
                          usage: HopUsage,
                          betaAcid: Option[Double] = None,
                          description: Option[String] = None,
                          aromas: List[String] = List.empty
                        )

/**
 * Données pour mettre à jour un houblon existant
 */
case class UpdateHopData(
                          alphaAcid: Option[Double] = None,
                          betaAcid: Option[Option[Double]] = None,
                          description: Option[Option[String]] = None,
                          usage: Option[HopUsage] = None,
                          status: Option[HopStatus] = None,
                          credibilityScore: Option[Int] = None,
                          aromaProfilesToAdd: Option[List[String]] = None,
                          aromaProfilesToRemove: Option[List[String]] = None
                        )