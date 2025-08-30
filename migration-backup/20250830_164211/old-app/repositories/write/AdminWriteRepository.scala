package repositories.write

import domain.admins._
import scala.concurrent.Future

trait AdminWriteRepository {
  def create(admin: Admin): Future[Unit]
  def update(admin: Admin): Future[Unit]
  def delete(id: AdminId): Future[Unit]
}
