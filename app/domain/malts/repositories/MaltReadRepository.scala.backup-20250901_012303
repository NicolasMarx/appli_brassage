package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import domain.common.PagedResult  // ✅ CORRIGÉ: Import depuis domain.common
import scala.concurrent.Future

// Types utilitaires pour les méthodes avancées
case class MaltSubstitution(
  id: String,
  maltId: MaltId,
  substituteId: MaltId,
  substituteName: String,
  compatibilityScore: Double
)

case class MaltCompatibility(
  maltId: MaltId,
  beerStyleId: String,
  compatibilityScore: Double,
  usageNotes: String
)

trait MaltReadRepository {
  def findById(id: MaltId): Future[Option[MaltAggregate]]
  def findByName(name: String): Future[Option[MaltAggregate]]
  def existsByName(name: String): Future[Boolean]
  def findAll(page: Int = 0, pageSize: Int = 20, activeOnly: Boolean = true): Future[List[MaltAggregate]]
  def count(activeOnly: Boolean = true): Future[Long]
  
  // ✅ CORRIGÉ: Utilise domain.common.PagedResult
  def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]]
  def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]]
  
  // ✅ CORRIGÉ: Utilise domain.common.PagedResult
  def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None,
    originCode: Option[String] = None,
    status: Option[String] = None,
    source: Option[String] = None,
    minCredibility: Option[Double] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    minExtraction: Option[Double] = None,
    minDiastaticPower: Option[Double] = None,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[PagedResult[MaltAggregate]]
}
