package domain.admin.model

import domain.common.{DomainError, EventSourced}
import java.time.Instant
// Imports des Value Objects manquants

/**
 * Agrégat Admin - Racine d'agrégat pour la gestion des administrateurs
 * CORRECTION: Import spécifique domain.common.DomainError uniquement
 */
case class AdminAggregate private (
  id: AdminId,
  name: AdminName, 
  email: AdminEmail,
  passwordHash: String,
  role: AdminRole,
  status: AdminStatus,
  createdAt: Instant,
  lastLoginAt: Option[Instant],
  failedLoginAttempts: Int,
  lockedUntil: Option[Instant],
  override val version: Int
) extends EventSourced {

  // Méthodes de lecture
  def isActive: Boolean = status == AdminStatus.Active
  def isLocked: Boolean = lockedUntil.exists(_.isAfter(Instant.now()))
  def canLogin: Boolean = isActive && !isLocked
  def hasPermission(permission: AdminPermission): Boolean = role.canPerform(permission)

  // Méthodes métier avec DomainError résolu
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
      val deactivatedAdmin = this.copy(status = AdminStatus.Inactive, version = version + 1)
      deactivatedAdmin.raise(AdminDeactivated(id.value, version + 1))
      Right(deactivatedAdmin)
    }
  }

  def activate(): Either[DomainError, AdminAggregate] = {
    if (status == AdminStatus.Active) {
      Left(DomainError.validation("L'administrateur est déjà actif"))
    } else {
      val activatedAdmin = this.copy(status = AdminStatus.Active, version = version + 1)
      activatedAdmin.raise(AdminActivated(id.value, version + 1))
      Right(activatedAdmin)
    }
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

  def recordSuccessfulLogin(): Either[DomainError, AdminAggregate] = {
    val updatedAdmin = this.copy(
      lastLoginAt = Some(Instant.now()),
      failedLoginAttempts = 0,
      lockedUntil = None,
      version = version + 1
    )
    updatedAdmin.raise(AdminLoginSucceeded(id.value, Instant.now(), version + 1))
    Right(updatedAdmin)
  }
}

object AdminAggregate {
  def create(
    id: AdminId,
    email: AdminEmail,
    name: AdminName,
    passwordHash: String,
    role: AdminRole
  ): AdminAggregate = {
    AdminAggregate(
      id = id,
      name = name,
      email = email,
      passwordHash = passwordHash,
      role = role,
      status = AdminStatus.Active,
      createdAt = Instant.now(),
      lastLoginAt = None,
      failedLoginAttempts = 0,
      lockedUntil = None,
      version = 1
    )
  }
}
