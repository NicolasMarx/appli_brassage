package domain.malts.repositories

import domain.malts.model._
import domain.shared._
import scala.concurrent.Future

/**
 * Repository Read pour le domaine Malts
 * Interface séparant lecture (queries) de l'écriture (commands) selon CQRS
 */
trait MaltReadRepository {

  /**
   * Récupération par ID
   */
  def findById(id: MaltId): Future[Option[MaltAggregate]]

  /**
   * Récupération par nom (unique)
   */
  def findByName(name: String): Future[Option[MaltAggregate]]

  /**
   * Liste paginée de tous les malts
   */
  def findAll(page: Int = 0, pageSize: Int = 20): Future[PagedResult[MaltAggregate]]

  /**
   * Liste des malts actifs uniquement
   */
  def findActive(page: Int = 0, pageSize: Int = 20): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche par type de malt
   */
  def findByType(maltType: MaltType, page: Int = 0, pageSize: Int = 20): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche par gamme de couleur EBC
   */
  def findByColorRange(
                        minEBC: Double,
                        maxEBC: Double,
                        page: Int = 0,
                        pageSize: Int = 20
                      ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche malts de base avec pouvoir diastasique minimum
   */
  def findBaseMalts(
                     minDiastaticPower: Option[Double] = None,
                     page: Int = 0,
                     pageSize: Int = 20
                   ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche par profils arômes (contient au moins un des profils)
   */
  def findByFlavorProfiles(
                            flavorProfiles: List[String],
                            page: Int = 0,
                            pageSize: Int = 20
                          ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche par origine
   */
  def findByOrigin(
                    originCode: String,
                    page: Int = 0,
                    pageSize: Int = 20
                  ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche par score de crédibilité minimum
   */
  def findByMinCredibility(
                            minScore: Int,
                            page: Int = 0,
                            pageSize: Int = 20
                          ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche par source des données
   */
  def findBySource(
                    source: MaltSource,
                    page: Int = 0,
                    pageSize: Int = 20
                  ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche textuelle avancée (nom, description, profils)
   */
  def search(
              searchTerm: String,
              page: Int = 0,
              pageSize: Int = 20
            ): Future[PagedResult[MaltAggregate]]

  /**
   * Recherche multi-critères combinée
   */
  def findByFilters(
                     maltType: Option[MaltType] = None,
                     minEBC: Option[Double] = None,
                     maxEBC: Option[Double] = None,
                     minExtraction: Option[Double] = None,
                     minDiastaticPower: Option[Double] = None,
                     originCode: Option[String] = None,
                     status: Option[MaltStatus] = None,
                     source: Option[MaltSource] = None,
                     minCredibility: Option[Int] = None,
                     flavorProfiles: List[String] = List.empty,
                     searchTerm: Option[String] = None,
                     page: Int = 0,
                     pageSize: Int = 20
                   ): Future[PagedResult[MaltAggregate]]

  /**
   * Substitutions possibles pour un malt
   */
  def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]]

  /**
   * Malts compatibles avec un style de bière
   */
  def findCompatibleWithBeerStyle(
                                   beerStyleId: String,
                                   page: Int = 0,
                                   pageSize: Int = 20
                                 ): Future[PagedResult[MaltCompatibility]]

  /**
   * Statistiques générales malts
   */
  def getStatistics(): Future[MaltStatistics]

  /**
   * Malts nécessitant révision (crédibilité faible)
   */
  def findNeedingReview(
                         maxCredibility: Int = 70,
                         page: Int = 0,
                         pageSize: Int = 20
                       ): Future[PagedResult[MaltAggregate]]

  /**
   * Malts populaires (les plus utilisés dans recettes)
   */
  def findPopular(
                   limit: Int = 10
                 ): Future[List[MaltAggregate]]

  /**
   * Vérifier si un malt existe
   */
  def exists(id: MaltId): Future[Boolean]

  /**
   * Vérifier si un nom est déjà utilisé
   */
  def existsByName(name: String): Future[Boolean]

  /**
   * Compter total malts actifs
   */
  def countActive(): Future[Int]
}