package repositories.read

import domain.admins._
import scala.concurrent.Future

trait AdminReadRepository {
  def byEmail(email: Email): Future[Option[Admin]]
  def byId(id: AdminId): Future[Option[Admin]]
  def all(): Future[Seq[Admin]]
}
