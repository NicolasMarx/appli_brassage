package domain.malts.repositories

trait MaltWriteRepository {

  /**
   * Créer un nouveau malt
   */
  def create(malt: MaltAggregate): Future[Unit]

  /**
   * Mettre à jour un malt existant
   */
  def update(malt: MaltAggregate): Future[Unit]

  /**
   * Supprimer un malt (suppression physique)
   */
  def delete(id: MaltId): Future[Unit]

  /**
   * Créer en lot (import de données)
   */
  def createBatch(malts: List[MaltAggregate]): Future[Unit]

  /**
   * Mettre à jour le score de crédibilité
   */
  def updateCredibilityScore(id: MaltId, newScore: MaltCredibility): Future[Unit]

  /**
   * Ajouter une substitution entre malts
   */
  def addSubstitution(substitution: MaltSubstitution): Future[Unit]

  /**
   * Supprimer une substitution
   */
  def removeSubstitution(originalId: MaltId, substituteId: MaltId): Future[Unit]

  /**
   * Associer un malt à un style de bière
   */
  def addBeerStyleCompatibility(compatibility: MaltBeerStyleCompatibility): Future[Unit]

  /**
   * Supprimer l'association malt-style
   */
  def removeBeerStyleCompatibility(maltId: MaltId, beerStyleId: String): Future[Unit]
}

/**
 * Classes de résultats pour les repositories
 */
case class MaltSubstitution(
                             originalMalt: MaltAggregate,
                             substituteMalt: MaltAggregate,
                             substitutionRatio: Double,
                             compatibilityNotes: Option[String]
                           )

case class MaltCompatibility(
                              malt: MaltAggregate,
                              beerStyleId: String,
                              compatibilityScore: Option[Int],
                              typicalPercentage: Option[Double]
                            )

case class MaltBeerStyleCompatibility(
                                       maltId: MaltId,
                                       beerStyleId: String,
                                       compatibilityScore: Option[Int],
                                       typicalPercentage: Option[Double]
                                     )

case class MaltStatistics(
                           totalCount: Int,
                           activeCount: Int,
                           countByType: Map[MaltType, Int],
                           countByOrigin: Map[String, Int],
                           countBySource: Map[MaltSource, Int],
                           averageCredibilityScore: Double,
                           averageEBC: Double,
                           averageExtraction: Double
                         )

case class PagedResult[T](
                           items: List[T],
                           currentPage: Int,
                           pageSize: Int,
                           totalCount: Int,
                           hasNext: Boolean
                         ) {
  def isEmpty: Boolean = items.isEmpty
  def nonEmpty: Boolean = items.nonEmpty
  def totalPages: Int = math.ceil(totalCount.toDouble / pageSize).toInt
}