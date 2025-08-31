package domain.malts.repositories

import domain.malts.model.{MaltAggregate, MaltId}
import scala.concurrent.Future

trait MaltWriteRepository {
  def create(malt: MaltAggregate): Future[Unit]
  def update(malt: MaltAggregate): Future[Unit]
  def delete(id: MaltId): Future[Unit]
}
