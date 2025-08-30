package application.admin

import domain.admins._
import repositories.read.AdminReadRepository
import repositories.write.AdminWriteRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

trait AdminBootstrapService {
  def bootstrap(email: String, plainPassword: String): Future[Either[BootstrapError, Admin]]
}

sealed trait BootstrapError
object BootstrapError {
  case object AlreadyInitialized extends BootstrapError
  final case class InvalidInput(msg: String) extends BootstrapError
}

@Singleton
final class AdminBootstrapServiceImpl @Inject()(
                                                 readRepo: AdminReadRepository,
                                                 writeRepo: AdminWriteRepository
                                               )(implicit ec: ExecutionContext) extends AdminBootstrapService {

  override def bootstrap(email: String, plainPassword: String): Future[Either[BootstrapError, Admin]] = {
    val normalizedEmail = Option(email).map(_.trim.toLowerCase).getOrElse("")
    val pwd             = Option(plainPassword).getOrElse("")

    if (normalizedEmail.isEmpty || !normalizedEmail.contains("@") || pwd.length < 8)
      return Future.successful(Left(BootstrapError.InvalidInput("email/password invalid (min 8 chars)")))

    readRepo.all().flatMap { admins =>
      if (admins.nonEmpty) Future.successful(Left(BootstrapError.AlreadyInitialized))
      else {
        val id   = AdminId(UUID.randomUUID().toString)
        val name = deriveNameFromEmail(normalizedEmail)
        val hash = BCrypt.hashpw(pwd, BCrypt.gensalt(12))

        val admin = Admin(
          id       = id,
          email    = Email(normalizedEmail),
          name     = name,
          password = HashedPassword(hash)
        )

        writeRepo.create(admin).map(_ => Right(admin))
      }
    }
  }

  private def deriveNameFromEmail(email: String): AdminName = {
    val local = email.takeWhile(_ != '@')
    val parts = local.split("[._-]+").filter(_.nonEmpty).map(_.toLowerCase.capitalize)
    parts.toList match {
      case f :: l :: _ => AdminName(f, l)
      case f :: Nil    => AdminName(f, "")
      case _           => AdminName("Admin", "Bootstrap")
    }
  }
}
