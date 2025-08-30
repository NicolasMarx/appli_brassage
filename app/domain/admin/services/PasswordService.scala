// app/domain/admin/services/PasswordService.scala
package domain.admin.services

import org.mindrot.jbcrypt.BCrypt
import javax.inject.Singleton
import scala.util.{Try, Success, Failure}

/**
 * Service de gestion sécurisée des mots de passe
 * Utilise BCrypt avec salt automatique et coût adaptatif
 */
@Singleton
class PasswordService {

  private val DefaultCost = 12 // Coût BCrypt recommandé (2^12 rounds)
  private val MinPasswordLength = 8
  private val MaxPasswordLength = 128

  /**
   * Hash un mot de passe avec BCrypt
   */
  def hashPassword(plainPassword: String): Either[String, String] = {
    if (plainPassword.length < MinPasswordLength) {
      Left(s"Le mot de passe doit contenir au moins $MinPasswordLength caractères")
    } else if (plainPassword.length > MaxPasswordLength) {
      Left(s"Le mot de passe ne peut pas dépasser $MaxPasswordLength caractères")
    } else {
      Try {
        BCrypt.hashpw(plainPassword, BCrypt.gensalt(DefaultCost))
      } match {
        case Success(hash) => Right(hash)
        case Failure(ex) => Left(s"Erreur lors du hashage: ${ex.getMessage}")
      }
    }
  }

  /**
   * Vérifie un mot de passe contre son hash
   */
  def verifyPassword(plainPassword: String, hashedPassword: String): Boolean = {
    Try {
      BCrypt.checkpw(plainPassword, hashedPassword)
    }.getOrElse(false)
  }

  /**
   * Valide la force d'un mot de passe
   */
  def validatePasswordStrength(password: String): Either[List[String], Unit] = {
    var errors = List.empty[String]

    if (password.length < MinPasswordLength) {
      errors = s"Doit contenir au moins $MinPasswordLength caractères" :: errors
    }

    if (password.length > MaxPasswordLength) {
      errors = s"Ne peut pas dépasser $MaxPasswordLength caractères" :: errors
    }

    if (!password.exists(_.isUpper)) {
      errors = "Doit contenir au moins une majuscule" :: errors
    }

    if (!password.exists(_.isLower)) {
      errors = "Doit contenir au moins une minuscule" :: errors
    }

    if (!password.exists(_.isDigit)) {
      errors = "Doit contenir au moins un chiffre" :: errors
    }

    if (!password.exists(c => !c.isLetterOrDigit)) {
      errors = "Doit contenir au moins un caractère spécial" :: errors
    }

    // Vérifier les mots de passe trop communs
    if (isCommonPassword(password)) {
      errors = "Ce mot de passe est trop commun, choisissez-en un autre" :: errors
    }

    if (errors.nonEmpty) Left(errors) else Right(())
  }

  /**
   * Génère un mot de passe aléatoire sécurisé
   */
  def generateSecurePassword(length: Int = 16): String = {
    val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercase = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val special = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    val allChars = uppercase + lowercase + digits + special

    val random = new scala.util.Random()

    // Garantir au moins un caractère de chaque type
    val password = new StringBuilder()
    password += uppercase(random.nextInt(uppercase.length))
    password += lowercase(random.nextInt(lowercase.length))
    password += digits(random.nextInt(digits.length))
    password += special(random.nextInt(special.length))

    // Compléter avec des caractères aléatoires
    for (_ <- 4 until length) {
      password += allChars(random.nextInt(allChars.length))
    }

    // Mélanger les caractères
    password.result().toList.sortBy(_ => random.nextDouble()).mkString
  }

  private def isCommonPassword(password: String): Boolean = {
    val commonPasswords = Set(
      "password", "123456", "password123", "admin", "admin123",
      "qwerty", "letmein", "welcome", "monkey", "dragon",
      "password1", "123456789", "football", "iloveyou", "admin123"
    )
    commonPasswords.contains(password.toLowerCase)
  }
}