// app/domain/admin/model/AdminRole.scala
package domain.admin.model

import play.api.libs.json._

/**
 * Énumération des rôles administrateurs avec permissions granulaires
 * Respecte le principe de séparation des responsabilités
 */
sealed trait AdminRole {
  def name: String
  def permissions: Set[AdminPermission]
  def canPerform(permission: AdminPermission): Boolean = permissions.contains(permission)
}

case object SuperAdmin extends AdminRole {
  val name = "SUPER_ADMIN"
  val permissions: Set[AdminPermission] = Set(
    AdminPermission.MANAGE_REFERENTIALS,
    AdminPermission.MANAGE_INGREDIENTS,
    AdminPermission.APPROVE_AI_PROPOSALS,
    AdminPermission.VIEW_ANALYTICS,
    AdminPermission.MANAGE_USERS,
    AdminPermission.VIEW_AUDIT_LOGS,
    AdminPermission.MANAGE_SYSTEM_CONFIG
  )
}

case object ContentManager extends AdminRole {
  val name = "CONTENT_MANAGER"
  val permissions: Set[AdminPermission] = Set(
    AdminPermission.MANAGE_REFERENTIALS,
    AdminPermission.MANAGE_INGREDIENTS,
    AdminPermission.APPROVE_AI_PROPOSALS,
    AdminPermission.VIEW_ANALYTICS
  )
}

case object DataReviewer extends AdminRole {
  val name = "DATA_REVIEWER" 
  val permissions: Set[AdminPermission] = Set(
    AdminPermission.APPROVE_AI_PROPOSALS,
    AdminPermission.VIEW_ANALYTICS
  )
}

case object Analyst extends AdminRole {
  val name = "ANALYST"
  val permissions: Set[AdminPermission] = Set(
    AdminPermission.VIEW_ANALYTICS,
    AdminPermission.VIEW_AUDIT_LOGS
  )
}

/**
 * Énumération des permissions granulaires
 */
sealed trait AdminPermission {
  def code: String
  def description: String
}

object AdminPermission {
  case object MANAGE_REFERENTIALS extends AdminPermission {
    val code = "MANAGE_REFERENTIALS"
    val description = "Gérer les référentiels (styles, origines, arômes)"
  }
  
  case object MANAGE_INGREDIENTS extends AdminPermission {
    val code = "MANAGE_INGREDIENTS" 
    val description = "Gérer les ingrédients (houblons, malts, levures)"
  }
  
  case object APPROVE_AI_PROPOSALS extends AdminPermission {
    val code = "APPROVE_AI_PROPOSALS"
    val description = "Approuver les propositions de l'IA"
  }
  
  case object VIEW_ANALYTICS extends AdminPermission {
    val code = "VIEW_ANALYTICS"
    val description = "Voir les analytics et statistiques"
  }
  
  case object MANAGE_USERS extends AdminPermission {
    val code = "MANAGE_USERS"
    val description = "Gérer les comptes administrateurs"
  }
  
  case object VIEW_AUDIT_LOGS extends AdminPermission {
    val code = "VIEW_AUDIT_LOGS" 
    val description = "Consulter les logs d'audit"
  }
  
  case object MANAGE_SYSTEM_CONFIG extends AdminPermission {
    val code = "MANAGE_SYSTEM_CONFIG"
    val description = "Gérer la configuration système"
  }
  
  val all: Set[AdminPermission] = Set(
    MANAGE_REFERENTIALS, MANAGE_INGREDIENTS, APPROVE_AI_PROPOSALS,
    VIEW_ANALYTICS, MANAGE_USERS, VIEW_AUDIT_LOGS, MANAGE_SYSTEM_CONFIG
  )
  
  def fromCode(code: String): Option[AdminPermission] = {
    all.find(_.code == code)
  }
}

object AdminRole {
  val all: Set[AdminRole] = Set(SuperAdmin, ContentManager, DataReviewer, Analyst)
  
  def fromName(name: String): Option[AdminRole] = {
    all.find(_.name == name)
  }
  
  implicit val adminRoleFormat: Format[AdminRole] = new Format[AdminRole] {
    def reads(json: JsValue): JsResult[AdminRole] = {
      json.validate[String].flatMap { name =>
        fromName(name) match {
          case Some(role) => JsSuccess(role)
          case None => JsError(s"Rôle admin invalide: $name")
        }
      }
    }
    
    def writes(role: AdminRole): JsValue = JsString(role.name)
  }
  
  implicit val adminPermissionFormat: Format[AdminPermission] = new Format[AdminPermission] {
    def reads(json: JsValue): JsResult[AdminPermission] = {
      json.validate[String].flatMap { code =>
        AdminPermission.fromCode(code) match {
          case Some(permission) => JsSuccess(permission)
          case None => JsError(s"Permission admin invalide: $code")
        }
      }
    }
    
    def writes(permission: AdminPermission): JsValue = JsString(permission.code)
  }
}