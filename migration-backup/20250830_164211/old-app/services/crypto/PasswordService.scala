package services

import javax.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt

@Singleton
final class PasswordService @Inject()() {
  def hash(plain: String): String =
    BCrypt.hashpw(plain, BCrypt.gensalt(12))

  def verify(plain: String, hashed: String): Boolean =
    BCrypt.checkpw(plain, hashed)
}
