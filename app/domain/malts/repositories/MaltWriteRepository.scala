package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import scala.concurrent.Future

trait MaltWriteRepository {
  // ✅ CORRIGÉ: Méthodes avec signatures cohérentes
  def create(malt: MaltAggregate): Future[Unit]
  def update(malt: MaltAggregate): Future[Unit]
  def delete(id: MaltId): Future[Unit]  // ✅ CORRIGÉ: Future[Unit] au lieu de Future[Boolean]
}
