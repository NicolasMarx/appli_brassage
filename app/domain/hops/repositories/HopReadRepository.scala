package domain.hops.repositories

import scala.concurrent.Future
import domain.hops.model._
import domain.shared.NonEmptyString

/**
 * Repository pour les opérations de lecture sur les houblons
 * Suit le pattern CQRS avec séparation Read/Write
 */
trait HopReadRepository {

  // ===============================
  // MÉTHODES PRINCIPALES DU TRAIT DDD
  // ===============================

  /**
   * Récupère un houblon par son ID
   */
  def byId(id: HopId): Future[Option[HopAggregate]]

  /**
   * Récupère un houblon par son nom
   */
  def byName(name: NonEmptyString): Future[Option[HopAggregate]]

  /**
   * Récupère tous les houblons avec pagination
   */
  def all(page: Int = 0, size: Int = 20): Future[(List[HopAggregate], Int)]

  /**
   * Recherche par caractéristiques
   */
  def searchByCharacteristics(
                               name: Option[String] = None,
                               originCode: Option[String] = None,
                               usage: Option[String] = None,
                               minAlphaAcid: Option[Double] = None,
                               maxAlphaAcid: Option[Double] = None,
                               activeOnly: Boolean = true
                             ): Future[Seq[HopAggregate]]

  /**
   * Récupère les houblons par statut
   */
  def byStatus(status: String): Future[Seq[HopAggregate]]

  /**
   * Récupère les houblons découverts par IA
   */
  def aiDiscovered(limit: Int = 50): Future[Seq[HopAggregate]]

  // ===============================
  // MÉTHODES DE COMPATIBILITÉ CONTRÔLEUR
  // ===============================

  /**
   * Alias pour byId - compatibilité contrôleur
   */
  def findById(id: HopId): Future[Option[HopAggregate]]

  /**
   * Récupère les houblons actifs avec pagination
   */
  def findActiveHops(page: Int, pageSize: Int): Future[(List[HopAggregate], Int)]

  /**
   * Recherche de houblons - alias pour searchByCharacteristics retournant List
   */
  def searchHops(
                  name: Option[String] = None,
                  originCode: Option[String] = None,
                  usage: Option[String] = None,
                  minAlphaAcid: Option[Double] = None,
                  maxAlphaAcid: Option[Double] = None,
                  activeOnly: Boolean = true
                ): Future[List[HopAggregate]]

  // ===============================
  // MÉTHODES UTILITAIRES POUR LE SERVICE
  // ===============================

  /**
   * Récupère les houblons par usage
   */
  def findByUsage(usage: String, activeOnly: Boolean = true): Future[List[HopAggregate]]

  /**
   * Récupère les houblons par origine
   */
  def findByOrigin(originCode: String, activeOnly: Boolean = true): Future[List[HopAggregate]]

  /**
   * Vérifie si un nom de houblon existe déjà
   */
  def existsByName(name: String): Future[Boolean]

  /**
   * Récupère les houblons par profil aromatique
   */
  def findByAromaProfile(aromaId: String, activeOnly: Boolean = true): Future[List[HopAggregate]]
}