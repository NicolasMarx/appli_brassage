// app/domain/admin/model/AdminEvents.scala
package domain.admin.model

import domain.common.BaseDomainEvent
import java.time.Instant

/**
 * Événements de domaine pour l'agrégat Admin
 * Capture tous les changements d'état significatifs
 */

case class AdminCreated(
  adminId: String,
  name: String,
  email: String,
  role: String,
  createdAt: Instant,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminCreated", version)

case class AdminRoleChanged(
  adminId: String,
  previousRole: String,
  newRole: String,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminRoleChanged", version)

case class AdminActivated(
  adminId: String,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminActivated", version)

case class AdminDeactivated(
  adminId: String,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminDeactivated", version)

case class AdminLoginSucceeded(
  adminId: String,
  loginAt: Instant,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminLoginSucceeded", version)

case class AdminLoginFailed(
  adminId: String,
  failedAttempts: Int,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminLoginFailed", version)

case class AdminAccountLocked(
  adminId: String,
  lockedUntil: Instant,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminAccountLocked", version)

case class AdminPasswordChanged(
  adminId: String,
  override val version: Int
) extends BaseDomainEvent(adminId, "AdminPasswordChanged", version)