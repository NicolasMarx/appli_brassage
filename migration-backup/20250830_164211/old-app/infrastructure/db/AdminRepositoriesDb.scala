package infrastructure.db

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}

import domain.admins._
import repositories.read.AdminReadRepository
import repositories.write.AdminWriteRepository

// ──────────────────────────────────────────────────────────────────────────────
// Table & Row
// ──────────────────────────────────────────────────────────────────────────────

private[db] final case class AdminRow(
                                       id: String,
                                       email: String,
                                       firstName: String,
                                       lastName: String,
                                       passwordHash: String
                                     )

private[db] class Admins(tag: Tag) extends Table[AdminRow](tag, "admins") {
  def id           = column[String]("id", O.PrimaryKey)
  def email        = column[String]("email", O.Unique)
  def firstName    = column[String]("first_name")
  def lastName     = column[String]("last_name")
  def passwordHash = column[String]("password_hash")

  def * = (id, email, firstName, lastName, passwordHash) <> (AdminRow.tupled, AdminRow.unapply)
}

/** Conteneur pour les TableQuery (évite les vals top-level en Scala 2.13). */
private[db] object AdminTables {
  val admins = TableQuery[Admins]
}

// ──────────────────────────────────────────────────────────────────────────────
// READ
// ──────────────────────────────────────────────────────────────────────────────

@Singleton
final class AdminReadRepositoryDb @Inject()(
                                             dbConfigProvider: DatabaseConfigProvider
                                           )(implicit ec: ExecutionContext) extends AdminReadRepository {

  private val db = dbConfigProvider.get[PostgresProfile].db
  import AdminTables._

  private def toDomain(r: AdminRow): Admin =
    Admin(
      id       = AdminId(r.id),
      email    = Email(r.email),
      name     = AdminName(r.firstName, r.lastName),
      password = HashedPassword(r.passwordHash)
    )

  override def byEmail(email: Email): Future[Option[Admin]] =
    db.run(admins.filter(_.email === email.value).result.headOption).map(_.map(toDomain))

  override def byId(id: AdminId): Future[Option[Admin]] =
    db.run(admins.filter(_.id === id.value).result.headOption).map(_.map(toDomain))

  override def all(): Future[Seq[Admin]] =
    db.run(admins.sortBy(_.lastName.asc).result).map(_.map(toDomain))
}

// ──────────────────────────────────────────────────────────────────────────────
// WRITE
// ──────────────────────────────────────────────────────────────────────────────

@Singleton
final class AdminWriteRepositoryDb @Inject()(
                                              dbConfigProvider: DatabaseConfigProvider
                                            )(implicit ec: ExecutionContext) extends AdminWriteRepository {

  private val db = dbConfigProvider.get[PostgresProfile].db
  import AdminTables._

  private def toRow(a: Admin): AdminRow =
    AdminRow(
      id           = a.id.value,
      email        = a.email.value,
      firstName    = a.name.first,
      lastName     = a.name.last,
      passwordHash = a.password.value
    )

  override def create(admin: Admin): Future[Unit] =
    db.run(admins += toRow(admin)).map(_ => ())

  override def update(admin: Admin): Future[Unit] =
    db.run(admins.filter(_.id === admin.id.value).update(toRow(admin))).map(_ => ())

  override def delete(id: AdminId): Future[Unit] =
    db.run(admins.filter(_.id === id.value).delete).map(_ => ())
}
