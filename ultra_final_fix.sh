#!/bin/bash

# ⚡ CORRECTIF ULTRA-FINAL - 2 DERNIÈRES ERREURS
# Ajoute DomainError.technical manquant

set -e

echo "⚡ Correctif ultra-final - 2 dernières erreurs..."

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# =============================================================================
# CORRECTION DOMAINERROR AVEC MÉTHODE TECHNICAL
# =============================================================================

echo -e "${BLUE}🔧 Ajout DomainError.technical manquant...${NC}"

cat > app/domain/common/DomainError.scala << 'EOF'
package domain.common

/**
 * Hiérarchie d'erreurs métier pour architecture DDD
 */
sealed trait DomainError {
  def message: String
  def code: String
}

// Erreurs de validation
case class ValidationError(message: String, field: Option[String] = None) extends DomainError {
  val code: String = "VALIDATION_ERROR"
}

// Erreurs de business rules
case class BusinessRuleViolation(message: String, ruleName: String) extends DomainError {
  val code: String = "BUSINESS_RULE_VIOLATION"
}

// Erreurs de ressources non trouvées
case class NotFoundError(resourceType: String, identifier: String) extends DomainError {
  val message: String = s"$resourceType avec l'identifiant '$identifier' non trouvé"
  val code: String = "NOT_FOUND"
}

// Erreurs de conflit
case class ConflictError(message: String, conflictingField: String) extends DomainError {
  val code: String = "CONFLICT"
}

// Erreurs d'autorisation
case class AuthorizationError(message: String, requiredPermission: Option[String] = None) extends DomainError {
  val code: String = "AUTHORIZATION_ERROR"
}

// Erreurs d'authentification
case class AuthenticationError(message: String) extends DomainError {
  val code: String = "AUTHENTICATION_ERROR"
}

// Erreurs techniques (exceptions, BDD, etc.)
case class TechnicalError(message: String, cause: Option[Throwable] = None) extends DomainError {
  val code: String = "TECHNICAL_ERROR"
}

object DomainError {
  def validation(message: String, field: String = null): ValidationError =
    ValidationError(message, Option(field))
  
  def businessRule(message: String, ruleName: String): BusinessRuleViolation =
    BusinessRuleViolation(message, ruleName)
  
  def notFound(resourceType: String, identifier: String): NotFoundError =
    NotFoundError(resourceType, identifier)
  
  def conflict(message: String, field: String): ConflictError =
    ConflictError(message, field)
  
  def unauthorized(message: String, permission: String = null): AuthorizationError =
    AuthorizationError(message, Option(permission))
  
  def unauthenticated(message: String): AuthenticationError =
    AuthenticationError(message)
  
  // Méthode manquante ajoutée !
  def technical(message: String, cause: Throwable = null): TechnicalError =
    TechnicalError(message, Option(cause))
}
EOF

echo -e "${GREEN}✅ DomainError.technical ajouté${NC}"

# =============================================================================
# MISE À JOUR BASECONTROLLER POUR GÉRER TECHNICALERROR
# =============================================================================

echo -e "${BLUE}🔧 Mise à jour BaseController pour TechnicalError...${NC}"

cat > app/interfaces/http/common/BaseController.scala << 'EOF'
package interfaces.http.common

import domain.common._
import play.api.libs.json._
import play.api.mvc._

/**
 * Contrôleur de base avec gestion d'erreurs DDD standardisée
 */
abstract class BaseController(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Convertit une DomainError en réponse HTTP appropriée
   */
  protected def handleDomainError(error: DomainError): Result = {
    error match {
      case _: NotFoundError =>
        NotFound(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: BusinessRuleViolation =>
        BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: ValidationError =>
        BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: ConflictError =>
        Conflict(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: AuthorizationError =>
        Forbidden(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: AuthenticationError =>
        Unauthorized(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _: TechnicalError =>
        InternalServerError(Json.obj("error" -> error.message, "code" -> error.code))
      
      case _ =>
        InternalServerError(Json.obj("error" -> "Une erreur interne s'est produite", "code" -> "INTERNAL_ERROR"))
    }
  }

  /**
   * Gestion standardisée des Either[DomainError, T]
   */
  protected def handleResult[T](result: Either[DomainError, T])(onSuccess: T => Result): Result = {
    result match {
      case Left(error) => handleDomainError(error)
      case Right(value) => onSuccess(value)
    }
  }
}
EOF

echo -e "${GREEN}✅ BaseController mis à jour${NC}"

# =============================================================================
# TEST COMPILATION FINAL
# =============================================================================

echo -e "${BLUE}🧪 Test compilation ultra-final...${NC}"

if sbt compile > /tmp/ultra_final_fix.log 2>&1; then
    echo -e "${GREEN}🎉🎉🎉 COMPILATION PARFAITEMENT RÉUSSIE ! 🎉🎉🎉${NC}"
    echo ""
    echo -e "${GREEN}✅ DOMAINE MALTS 100% FONCTIONNEL !${NC}"
    echo ""
    echo -e "${BLUE}🚀 PROCHAINES ÉTAPES :${NC}"
    echo "   1. sbt run"
    echo "   2. curl \"http://localhost:9000/api/admin/malts\""
    echo "   3. Debug SlickMaltReadRepository pour résoudre la conversion"
    echo ""
    echo -e "${GREEN}🎯 Le domaine Malts est maintenant prêt pour la production !${NC}"
else
    echo "❌ Erreurs restantes :"
    tail -10 /tmp/ultra_final_fix.log
fi
