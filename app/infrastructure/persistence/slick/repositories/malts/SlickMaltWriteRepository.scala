package infrastructure.persistence.slick.repositories.malts

import domain.malts.model._
import domain.malts.repositories._
import infrastructure.persistence.slick.tables.MaltTables
import infrastructure.persistence.slick.SlickDatabase
import slick.jdbc.PostgresProfile.api._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

/**
 * Implémentation Slick du MaltWriteRepository
 * Gère toutes les opérations d'écriture selon CQRS
 */
@Singleton
class SlickMaltWriteRepository @Inject()(
                                          db: SlickDatabase
                                        )(implicit ec: ExecutionContext) extends MaltWriteRepository {

  import MaltTables._

  override def create(malt: MaltAggregate): Future[Unit] = {
    val row = MaltTables.aggregateToRow(malt)
    val action = malts += row
    db.run(action).map(_ => ())
  }

  override def update(malt: MaltAggregate): Future[Unit] = {
    val row = MaltTables.aggregateToRow(malt)
    val action = malts
      .filter(_.id === UUID.fromString(malt.id.value))
      .update(row)

    db.run(action).flatMap { updatedRows =>
      if (updatedRows == 0) {
        Future.failed(new RuntimeException(s"Malt non trouvé pour mise à jour: ${malt.id.value}"))
      } else {
        Future.successful(())
      }
    }
  }

  override def delete(id: MaltId): Future[Unit] = {
    val action = malts
      .filter(_.id === UUID.fromString(id.value))
      .delete

    db.run(action).flatMap { deletedRows =>
      if (deletedRows == 0) {
        Future.failed(new RuntimeException(s"Malt non trouvé pour suppression: ${id.value}"))
      } else {
        Future.successful(())
      }
    }
  }

  override def createBatch(malts: List[MaltAggregate]): Future[Unit] = {
    if (malts.isEmpty) {
      Future.successful(())
    } else {
      val rows = malts.map(MaltTables.aggregateToRow)
      val action = MaltTables.malts ++= rows
      db.run(action).map(_ => ())
    }
  }

  override def updateCredibilityScore(id: MaltId, newScore: MaltCredibility): Future[Unit] = {
    val action = malts
      .filter(_.id === UUID.fromString(id.value))
      .map(m => (m.credibilityScore, m.updatedAt, m.version))
      .update((newScore.value, Instant.now(), 0)) // Version sera incrémentée par trigger

    db.run(action).flatMap { updatedRows =>
      if (updatedRows == 0) {
        Future.failed(new RuntimeException(s"Malt non trouvé pour mise à jour crédibilité: ${id.value}"))
      } else {
        Future.successful(())
      }
    }
  }

  override def addSubstitution(substitution: MaltSubstitution): Future[Unit] = {
    val row = MaltSubstitutionRow(
      originalMaltId = UUID.fromString(substitution.originalMalt.id.value),
      substituteMaltId = UUID.fromString(substitution.substituteMalt.id.value),
      substitutionRatio = substitution.substitutionRatio,
      compatibilityNotes = substitution.compatibilityNotes,
      createdAt = Instant.now()
    )

    val action = maltSubstitutions += row
    db.run(action).map(_ => ()).recover {
      case _: Exception =>
        throw new RuntimeException("Erreur lors de l'ajout de la substitution (doublon possible)")
    }
  }

  override def removeSubstitution(originalId: MaltId, substituteId: MaltId): Future[Unit] = {
    val action = maltSubstitutions
      .filter(s => s.originalMaltId === UUID.fromString(originalId.value) &&
        s.substituteMaltId === UUID.fromString(substituteId.value))
      .delete

    db.run(action).flatMap { deletedRows =>
      if (deletedRows == 0) {
        Future.failed(new RuntimeException("Substitution non trouvée pour suppression"))
      } else {
        Future.successful(())
      }
    }
  }

  override def addBeerStyleCompatibility(compatibility: MaltBeerStyleCompatibility): Future[Unit] = {
    val row = MaltBeerStyleRow(
      maltId = UUID.fromString(compatibility.maltId.value),
      beerStyleId = compatibility.beerStyleId,
      compatibilityScore = compatibility.compatibilityScore,
      typicalPercentage = compatibility.typicalPercentage,
      createdAt = Instant.now()
    )

    val action = maltBeerStyles += row
    db.run(action).map(_ => ()).recover {
      case _: Exception =>
        throw new RuntimeException("Erreur lors de l'ajout de compatibilité style (doublon possible)")
    }
  }

  override def removeBeerStyleCompatibility(maltId: MaltId, beerStyleId: String): Future[Unit] = {
    val action = maltBeerStyles
      .filter(mbs => mbs.maltId === UUID.fromString(maltId.value) &&
        mbs.beerStyleId === beerStyleId)
      .delete

    db.run(action).flatMap { deletedRows =>
      if (deletedRows == 0) {
        Future.failed(new RuntimeException("Compatibilité style non trouvée pour suppression"))
      } else {
        Future.successful(())
      }
    }
  }

  /**
   * Méthodes utilitaires pour opérations avancées
   */

  def updateStatusBatch(ids: List[MaltId], newStatus: MaltStatus): Future[Int] = {
    val uuids = ids.map(id => UUID.fromString(id.value))
    val action = malts
      .filter(_.id.inSet(uuids))
      .map(m => (m.status, m.updatedAt))
      .update((newStatus.name, Instant.now()))

    db.run(action)
  }

  def updateCredibilityBatch(adjustments: Map[MaltId, MaltCredibility]): Future[Unit] = {
    val actions = adjustments.map { case (id, credibility) =>
      malts
        .filter(_.id === UUID.fromString(id.value))
        .map(m => (m.credibilityScore, m.updatedAt))
        .update((credibility.value, Instant.now()))
    }.toSeq

    db.run(DBIO.sequence(actions)).map(_ => ())
  }

  def addSubstitutionsBatch(substitutions: List[MaltSubstitution]): Future[Unit] = {
    if (substitutions.isEmpty) {
      Future.successful(())
    } else {
      val rows = substitutions.map { sub =>
        MaltSubstitutionRow(
          originalMaltId = UUID.fromString(sub.originalMalt.id.value),
          substituteMaltId = UUID.fromString(sub.substituteMalt.id.value),
          substitutionRatio = sub.substitutionRatio,
          compatibilityNotes = sub.compatibilityNotes,
          createdAt = Instant.now()
        )
      }

      val action = maltSubstitutions ++= rows
      db.run(action).map(_ => ())
    }
  }

  def addBeerStyleCompatibilityBatch(compatibilities: List[MaltBeerStyleCompatibility]): Future[Unit] = {
    if (compatibilities.isEmpty) {
      Future.successful(())
    } else {
      val rows = compatibilities.map { comp =>
        MaltBeerStyleRow(
          maltId = UUID.fromString(comp.maltId.value),
          beerStyleId = comp.beerStyleId,
          compatibilityScore = comp.compatibilityScore,
          typicalPercentage = comp.typicalPercentage,
          createdAt = Instant.now()
        )
      }

      val action = maltBeerStyles ++= rows
      db.run(action).map(_ => ())
    }
  }

  /**
   * Maintenance et nettoyage
   */

  def cleanupOldSubstitutions(olderThanDays: Int): Future[Int] = {
    val cutoffDate = Instant.now().minusSeconds(olderThanDays * 24 * 3600)
    val action = maltSubstitutions
      .filter(_.createdAt < cutoffDate)
      .delete

    db.run(action)
  }

  def removeOrphanedRelations(): Future[Unit] = {
    // Supprimer relations vers malts inexistants
    val cleanSubstitutions = for {
      orphanedSubs <- maltSubstitutions
        .filterNot(_.originalMaltId.in(malts.map(_.id)))
        .delete
      orphanedStyles <- maltBeerStyles
        .filterNot(_.maltId.in(malts.map(_.id)))
        .delete
    } yield (orphanedSubs, orphanedStyles)

    db.run(cleanSubstitutions).map(_ => ())
  }

  /**
   * Statistiques d'écriture pour monitoring
   */

  def getWriteStatistics(): Future[WriteStatistics] = {
    val queries = for {
      totalMalts <- malts.length.result
      totalSubstitutions <- maltSubstitutions.length.result
      totalCompatibilities <- maltBeerStyles.length.result
      recentlyCreated <- malts
        .filter(_.createdAt > Instant.now().minusSeconds(24 * 3600))
        .length.result
      recentlyUpdated <- malts
        .filter(_.updatedAt > Instant.now().minusSeconds(24 * 3600))
        .length.result
    } yield WriteStatistics(
      totalMalts = totalMalts,
      totalSubstitutions = totalSubstitutions,
      totalBeerStyleCompatibilities = totalCompatibilities,
      maltsCreatedLast24h = recentlyCreated,
      maltsUpdatedLast24h = recentlyUpdated
    )

    db.run(queries)
  }
}

/**
 * Statistiques d'écriture pour monitoring
 */
case class WriteStatistics(
                            totalMalts: Int,
                            totalSubstitutions: Int,
                            totalBeerStyleCompatibilities: Int,
                            maltsCreatedLast24h: Int,
                            maltsUpdatedLast24h: Int
                          )