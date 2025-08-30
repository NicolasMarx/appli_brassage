// app/domain/admin/model/AdminAggregate.scala
package domain.admin.model

import domain.common._
import domain.shared._
import java.time.Instant

/**
 * Agrégat Admin - Racine d'agrégat pour la gestion des administrateurs
 * Encapsule la logique métier et maintient la cohérence des données
 */
case class AdminAggregate private (
                                    id: AdminId,
                                    name: NonEmptyString,
                                    email: Email,
                                    passwordHash: String,
                                    role: AdminRole,
                                    status: AdminStatus,
                                    createdAt: Instant,
                                    lastLoginAt: Option[Instant],
                                    failedLoginAttempts: Int,
                                    lockedUntil: Option[Instant],
                                    override val version: Int
                                  ) extends EventSourced {

  // Méthodes de lecture (queries)
  def isActive: Boolean = status == AdminStatus.Active
  def isLocked: Boolean = lockedUntil.exists(_.isAfter(Instant.now()))
  def canLogin: Boolean = isActive && !isLocked
  def hasPermission(permission: AdminPermission): Boolean = role.canPerform(permission)

  // Méthodes métier (commands)
  def changeRole(newRole: AdminRole): Either[DomainError, AdminAggregate] = {
    if (role == newRole) {
      Left(DomainError.validation("Le rôle est déjà celui spécifié"))
    } else {
      val updatedAdmin = this.copy(role = newRole, version = version + 1)
      updatedAdmin.raise(AdminRoleChanged(id.value, role.name, newRole.name, version + 1))
      Right(updatedAdmin)
    }
  }

  def deactivate(): Either[DomainError, AdminAggregate] = {
    if (status == AdminStatus.Inactive) {
      Left(DomainError.validation("L'administrateur est déjà inactif"))
    } else {
      val updatedAdmin = this.copy(status = AdminStatus.Inactive, version = version + 1)
      updatedAdmin.raise(AdminDeactivated(id.value, version + 1))
      Right(updatedAdmin)
    }
  }

  def activate(): Either[DomainError, AdminAggregate] = {
    if (status == AdminStatus.Active) {
      Left(DomainError.validation("L'administrateur est déjà actif"))
    } else {
      val updatedAdmin = this.copy(
        status = AdminStatus.Active,
        failedLoginAttempts = 0,
        lockedUntil = None,
        version = version + 1
      )
      updatedAdmin.raise(AdminActivated(id.value, version + 1))
      Right(updatedAdmin)
    }
  }

  def recordSuccessfulLogin(): AdminAggregate = {
    val updatedAdmin = this.copy(
      lastLoginAt = Some(Instant.now()),
      failedLoginAttempts = 0,
      lockedUntil = None,
      version = version + 1
    )
    updatedAdmin.raise(AdminLoginSucceeded(id.value, Instant.now(), version + 1))
    updatedAdmin
  }

  def recordFailedLogin(): AdminAggregate = {
    val newAttempts = failedLoginAttempts + 1
    val shouldLock = newAttempts >= AdminAggregate.MaxFailedAttempts

    val updatedAdmin = if (shouldLock) {
      val lockUntil = Instant.now().plusSeconds(AdminAggregate.LockoutDurationSeconds)
      this.copy(
        failedLoginAttempts = newAttempts,
        lockedUntil = Some(lockUntil),
        version = version + 1
      )
    } else {
      this.copy(failedLoginAttempts = newAttempts, version = version + 1)
    }

    if (shouldLock) {
      updatedAdmin.raise(AdminAccountLocked(id.value, updatedAdmin.lockedUntil.get, version + 1))
    } else {
      updatedAdmin.raise(AdminLoginFailed(id.value, newAttempts, version + 1))
    }

    updatedAdmin
  }

  def updatePassword(newPasswordHash: String): Either[DomainError, AdminAggregate] = {
    if (newPasswordHash.trim.isEmpty) {
      Left(DomainError.validation("Le hash du mot de passe ne peut pas être vide"))
    } else if (passwordHash == newPasswordHash) {
      Left(DomainError.validation("Le nouveau mot de passe doit être différent"))
    } else {
      val updatedAdmin = this.copy(passwordHash = newPasswordHash, version = version + 1)
      updatedAdmin.raise(AdminPasswordChanged(id.value, version + 1))
      Right(updatedAdmin)
    }
  }
}

object AdminAggregate {
  val MaxFailedAttempts = 5
  val LockoutDurationSeconds = 900L // 15 minutes

  def create(
              name: NonEmptyString,
              email: Email,
              passwordHash: String,
              role: AdminRole
            ): Either[DomainError, AdminAggregate] = {

    if (passwordHash.trim.isEmpty) {
      Left(DomainError.validation("Le hash du mot de passe ne peut pas être vide"))
    } else {
      val adminId = AdminId.generate()
      val now = Instant.now()

      val admin = AdminAggregate(
        id = adminId,
        name = name,
        email = email,
        passwordHash = passwordHash,
        role = role,
        status = AdminStatus.Active,
        createdAt = now,
        lastLoginAt = None,
        failedLoginAttempts = 0,
        lockedUntil = None,
        version = 1
      )

      admin.raise(AdminCreated(adminId.value, name.value, email.value, role.name, now, 1))
      Right(admin)
    }
  }
}

/**
 * Statut de l'administrateur
 */
sealed trait AdminStatus {
  def name: String
}

object AdminStatus {
  case object Active extends AdminStatus { val name = "ACTIVE" }
  case object Inactive extends AdminStatus { val name = "INACTIVE" }

  def fromName(name: String): Option[AdminStatus] = name match {
    case "ACTIVE" => Some(Active)
    case "INACTIVE" => Some(Inactive)
    case _ => None
  }
}