#!/bin/bash
# =============================================================================
# SCRIPT : Création Application Layer domaine Yeast
# OBJECTIF : Créer Commands, Queries, Handlers (CQRS)
# USAGE : ./scripts/yeast/05-create-application-layer.sh
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}⚡ CRÉATION APPLICATION LAYER YEAST${NC}"
echo -e "${BLUE}==================================${NC}"
echo ""

# =============================================================================
# ÉTAPE 1: COMMANDS (CQRS WRITE SIDE)
# =============================================================================

echo -e "${YELLOW}✍️ Création Commands...${NC}"

mkdir -p app/application/yeasts/commands

cat > app/application/yeasts/commands/YeastCommands.scala << 'EOF'
package application.yeasts.commands

import domain.yeasts.model._
import java.util.UUID

/**
 * Commands pour les opérations d'écriture sur les levures
 * Pattern CQRS - Write Side
 */

/**
 * Commande de création d'une nouvelle levure
 */
case class CreateYeastCommand(
  name: String,
  laboratory: String,
  strain: String,
  yeastType: String,
  attenuationMin: Int,
  attenuationMax: Int,
  temperatureMin: Int,
  temperatureMax: Int,
  alcoholTolerance: Double,
  flocculation: String,
  aromaProfile: List[String] = List.empty,
  flavorProfile: List[String] = List.empty,
  esters: List[String] = List.empty,
  phenols: List[String] = List.empty,
  otherCompounds: List[String] = List.empty,
  notes: Option[String] = None,
  createdBy: UUID
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (name.trim.isEmpty) errors = "Nom requis" :: errors
    if (laboratory.trim.isEmpty) errors = "Laboratoire requis" :: errors  
    if (strain.trim.isEmpty) errors = "Souche requise" :: errors
    if (yeastType.trim.isEmpty) errors = "Type de levure requis" :: errors
    if (attenuationMin < 30 || attenuationMin > 100) errors = "Atténuation min invalide (30-100)" :: errors
    if (attenuationMax < 30 || attenuationMax > 100) errors = "Atténuation max invalide (30-100)" :: errors
    if (attenuationMin > attenuationMax) errors = "Atténuation min > max" :: errors
    if (temperatureMin < 0 || temperatureMin > 50) errors = "Température min invalide (0-50)" :: errors
    if (temperatureMax < 0 || temperatureMax > 50) errors = "Température max invalide (0-50)" :: errors
    if (temperatureMin > temperatureMax) errors = "Température min > max" :: errors
    if (alcoholTolerance < 0 || alcoholTolerance > 20) errors = "Tolérance alcool invalide (0-20)" :: errors
    if (flocculation.trim.isEmpty) errors = "Floculation requise" :: errors
    if (aromaProfile.isEmpty && flavorProfile.isEmpty) errors = "Au moins un profil aromatique requis" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Commande de mise à jour d'une levure existante
 */
case class UpdateYeastCommand(
  yeastId: UUID,
  name: Option[String] = None,
  laboratory: Option[String] = None,
  strain: Option[String] = None,
  attenuationMin: Option[Int] = None,
  attenuationMax: Option[Int] = None,
  temperatureMin: Option[Int] = None,
  temperatureMax: Option[Int] = None,
  alcoholTolerance: Option[Double] = None,
  flocculation: Option[String] = None,
  aromaProfile: Option[List[String]] = None,
  flavorProfile: Option[List[String]] = None,
  esters: Option[List[String]] = None,
  phenols: Option[List[String]] = None,
  otherCompounds: Option[List[String]] = None,
  notes: Option[String] = None,
  updatedBy: UUID
) {
  def hasChanges: Boolean = {
    name.isDefined || laboratory.isDefined || strain.isDefined ||
    attenuationMin.isDefined || attenuationMax.isDefined ||
    temperatureMin.isDefined || temperatureMax.isDefined ||
    alcoholTolerance.isDefined || flocculation.isDefined ||
    aromaProfile.isDefined || flavorProfile.isDefined ||
    esters.isDefined || phenols.isDefined || otherCompounds.isDefined ||
    notes.isDefined
  }
  
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    name.foreach(n => if (n.trim.isEmpty) errors = "Nom ne peut pas être vide" :: errors)
    laboratory.foreach(l => if (l.trim.isEmpty) errors = "Laboratoire ne peut pas être vide" :: errors)
    strain.foreach(s => if (s.trim.isEmpty) errors = "Souche ne peut pas être vide" :: errors)
    attenuationMin.foreach(a => if (a < 30 || a > 100) errors = "Atténuation min invalide (30-100)" :: errors)
    attenuationMax.foreach(a => if (a < 30 || a > 100) errors = "Atténuation max invalide (30-100)" :: errors)
    temperatureMin.foreach(t => if (t < 0 || t > 50) errors = "Température min invalide (0-50)" :: errors)
    temperatureMax.foreach(t => if (t < 0 || t > 50) errors = "Température max invalide (0-50)" :: errors)
    alcoholTolerance.foreach(a => if (a < 0 || a > 20) errors = "Tolérance alcool invalide (0-20)" :: errors)
    flocculation.foreach(f => if (f.trim.isEmpty) errors = "Floculation ne peut pas être vide" :: errors)
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Commande de changement de statut
 */
case class ChangeYeastStatusCommand(
  yeastId: UUID,
  newStatus: String,
  reason: Option[String] = None,
  changedBy: UUID
) {
  def validate: Either[List[String], Unit] = {
    if (newStatus.trim.isEmpty) {
      Left(List("Statut requis"))
    } else {
      Right(())
    }
  }
}

/**
 * Commande d'activation d'une levure
 */
case class ActivateYeastCommand(
  yeastId: UUID,
  activatedBy: UUID
)

/**
 * Commande de désactivation d'une levure
 */
case class DeactivateYeastCommand(
  yeastId: UUID,
  reason: Option[String] = None,
  deactivatedBy: UUID
)

/**
 * Commande d'archivage d'une levure
 */
case class ArchiveYeastCommand(
  yeastId: UUID,
  reason: Option[String] = None,
  archivedBy: UUID
)

/**
 * Commande de mise à jour des données techniques uniquement
 */
case class UpdateYeastTechnicalDataCommand(
  yeastId: UUID,
  attenuationMin: Option[Int] = None,
  attenuationMax: Option[Int] = None,
  temperatureMin: Option[Int] = None,
  temperatureMax: Option[Int] = None,
  alcoholTolerance: Option[Double] = None,
  flocculation: Option[String] = None,
  aromaProfile: Option[List[String]] = None,
  flavorProfile: Option[List[String]] = None,
  esters: Option[List[String]] = None,
  phenols: Option[List[String]] = None,
  otherCompounds: Option[List[String]] = None,
  notes: Option[String] = None,
  updatedBy: UUID
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    attenuationMin.foreach(a => if (a < 30 || a > 100) errors = "Atténuation min invalide" :: errors)
    attenuationMax.foreach(a => if (a < 30 || a > 100) errors = "Atténuation max invalide" :: errors)
    temperatureMin.foreach(t => if (t < 0 || t > 50) errors = "Température min invalide" :: errors)
    temperatureMax.foreach(t => if (t < 0 || t > 50) errors = "Température max invalide" :: errors)
    alcoholTolerance.foreach(a => if (a < 0 || a > 20) errors = "Tolérance alcool invalide" :: errors)
    
    // Vérification cohérence si les deux valeurs sont fournies
    (attenuationMin, attenuationMax) match {
      case (Some(min), Some(max)) if min > max => 
        errors = "Atténuation min > max" :: errors
      case _ => // OK
    }
    
    (temperatureMin, temperatureMax) match {
      case (Some(min), Some(max)) if min > max => 
        errors = "Température min > max" :: errors
      case _ => // OK
    }
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Commande de suppression (archive) de levure
 */
case class DeleteYeastCommand(
  yeastId: UUID,
  reason: String = "Suppression demandée",
  deletedBy: UUID
)

/**
 * Commande batch pour créer plusieurs levures
 */
case class CreateYeastsBatchCommand(
  yeasts: List[CreateYeastCommand]
) {
  def validate: Either[List[String], Unit] = {
    if (yeasts.isEmpty) {
      Left(List("Aucune levure à créer"))
    } else {
      val allErrors = yeasts.zipWithIndex.flatMap { case (yeast, index) =>
        yeast.validate.left.toOption.map(errors => 
          errors.map(err => s"Levure $index: $err")
        )
      }.flatten
      
      if (allErrors.nonEmpty) Left(allErrors) else Right(())
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 2: QUERIES (CQRS READ SIDE)
# =============================================================================

echo -e "\n${YELLOW}🔍 Création Queries...${NC}"

mkdir -p app/application/yeasts/queries

cat > app/application/yeasts/queries/YeastQueries.scala << 'EOF'
package application.yeasts.queries

import domain.yeasts.model._
import java.util.UUID

/**
 * Queries pour les opérations de lecture sur les levures
 * Pattern CQRS - Read Side
 */

/**
 * Query pour récupérer une levure par ID
 */
case class GetYeastByIdQuery(yeastId: UUID)

/**
 * Query pour récupérer une levure par nom
 */
case class GetYeastByNameQuery(name: String)

/**
 * Query pour recherche par laboratoire et souche
 */
case class GetYeastByLaboratoryAndStrainQuery(
  laboratory: String,
  strain: String
)

/**
 * Query pour liste des levures avec filtres
 */
case class FindYeastsQuery(
  name: Option[String] = None,
  laboratory: Option[String] = None,
  yeastType: Option[String] = None,
  minAttenuation: Option[Int] = None,
  maxAttenuation: Option[Int] = None,
  minTemperature: Option[Int] = None,
  maxTemperature: Option[Int] = None,
  minAlcoholTolerance: Option[Double] = None,
  maxAlcoholTolerance: Option[Double] = None,
  flocculation: Option[String] = None,
  characteristics: List[String] = List.empty,
  status: List[String] = List.empty,
  page: Int = 0,
  size: Int = 20
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (page < 0) errors = "Page doit être >= 0" :: errors
    if (size <= 0 || size > 100) errors = "Size doit être entre 1 et 100" :: errors
    minAttenuation.foreach(a => if (a < 30 || a > 100) errors = "Atténuation min invalide" :: errors)
    maxAttenuation.foreach(a => if (a < 30 || a > 100) errors = "Atténuation max invalide" :: errors)
    minTemperature.foreach(t => if (t < 0 || t > 50) errors = "Température min invalide" :: errors)
    maxTemperature.foreach(t => if (t < 0 || t > 50) errors = "Température max invalide" :: errors)
    minAlcoholTolerance.foreach(a => if (a < 0 || a > 20) errors = "Tolérance alcool min invalide" :: errors)
    maxAlcoholTolerance.foreach(a => if (a < 0 || a > 20) errors = "Tolérance alcool max invalide" :: errors)
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
  
  def toFilter: YeastFilter = {
    val parsedLab = laboratory.flatMap(YeastLaboratory.fromName)
    val parsedType = yeastType.flatMap(YeastType.fromName)
    val parsedFloc = flocculation.flatMap(FlocculationLevel.fromName)
    val parsedStatuses = status.flatMap(YeastStatus.fromName)
    
    YeastFilter(
      name = name,
      laboratory = parsedLab,
      yeastType = parsedType,
      minAttenuation = minAttenuation,
      maxAttenuation = maxAttenuation,
      minTemperature = minTemperature,
      maxTemperature = maxTemperature,
      minAlcoholTolerance = minAlcoholTolerance,
      maxAlcoholTolerance = maxAlcoholTolerance,
      flocculation = parsedFloc,
      characteristics = characteristics,
      status = parsedStatuses,
      page = page,
      size = size
    )
  }
}

/**
 * Query pour recherche par type de levure
 */
case class FindYeastsByTypeQuery(
  yeastType: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, YeastType] = {
    YeastType.parse(yeastType)
  }
}

/**
 * Query pour recherche par laboratoire
 */
case class FindYeastsByLaboratoryQuery(
  laboratory: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, YeastLaboratory] = {
    YeastLaboratory.parse(laboratory)
  }
}

/**
 * Query pour recherche par statut
 */
case class FindYeastsByStatusQuery(
  status: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, YeastStatus] = {
    YeastStatus.parse(status)
  }
}

/**
 * Query pour recherche par plage d'atténuation
 */
case class FindYeastsByAttenuationRangeQuery(
  minAttenuation: Int,
  maxAttenuation: Int
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (minAttenuation < 30 || minAttenuation > 100) errors = "Atténuation min invalide (30-100)" :: errors
    if (maxAttenuation < 30 || maxAttenuation > 100) errors = "Atténuation max invalide (30-100)" :: errors
    if (minAttenuation > maxAttenuation) errors = "Atténuation min > max" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Query pour recherche par plage de température
 */
case class FindYeastsByTemperatureRangeQuery(
  minTemperature: Int,
  maxTemperature: Int
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (minTemperature < 0 || minTemperature > 50) errors = "Température min invalide (0-50)" :: errors
    if (maxTemperature < 0 || maxTemperature > 50) errors = "Température max invalide (0-50)" :: errors
    if (minTemperature > maxTemperature) errors = "Température min > max" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Query pour recherche par caractéristiques
 */
case class FindYeastsByCharacteristicsQuery(
  characteristics: List[String]
) {
  def validate: Either[String, Unit] = {
    if (characteristics.isEmpty || characteristics.forall(_.trim.isEmpty)) {
      Left("Au moins une caractéristique requise")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour recherche textuelle
 */
case class SearchYeastsQuery(
  query: String,
  limit: Option[Int] = None
) {
  def validate: Either[String, Unit] = {
    if (query.trim.isEmpty) {
      Left("Terme de recherche requis")
    } else if (query.trim.length < 2) {
      Left("Terme de recherche trop court (min 2 caractères)")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour statistiques par statut
 */
case class GetYeastStatsQuery()

/**
 * Query pour statistiques par laboratoire
 */
case class GetYeastStatsByLaboratoryQuery()

/**
 * Query pour statistiques par type
 */
case class GetYeastStatsByTypeQuery()

/**
 * Query pour levures populaires
 */
case class GetMostPopularYeastsQuery(limit: Int = 10) {
  def validate: Either[String, Unit] = {
    if (limit <= 0 || limit > 50) {
      Left("Limite doit être entre 1 et 50")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour levures récemment ajoutées
 */
case class GetRecentlyAddedYeastsQuery(limit: Int = 5) {
  def validate: Either[String, Unit] = {
    if (limit <= 0 || limit > 20) {
      Left("Limite doit être entre 1 et 20")
    } else {
      Right(())
    }
  }
}

/**
 * Query pour recommandations de levures
 */
case class GetYeastRecommendationsQuery(
  beerStyle: Option[String] = None,
  targetAbv: Option[Double] = None,
  fermentationTemp: Option[Int] = None,
  desiredCharacteristics: List[String] = List.empty,
  limit: Int = 10
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (limit <= 0 || limit > 20) errors = "Limite doit être entre 1 et 20" :: errors
    targetAbv.foreach(abv => if (abv < 0 || abv > 20) errors = "ABV invalide (0-20)" :: errors)
    fermentationTemp.foreach(temp => if (temp < 0 || temp > 50) errors = "Température invalide (0-50)" :: errors)
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}

/**
 * Query pour alternatives à une levure
 */
case class GetYeastAlternativesQuery(
  originalYeastId: UUID,
  reason: String = "unavailable",
  limit: Int = 5
) {
  def validate: Either[List[String], Unit] = {
    var errors = List.empty[String]
    
    if (limit <= 0 || limit > 10) errors = "Limite doit être entre 1 et 10" :: errors
    if (reason.trim.isEmpty) errors = "Raison requise" :: errors
    
    if (errors.nonEmpty) Left(errors.reverse) else Right(())
  }
}
EOF

echo -e "${GREEN}✅ Commands et Queries créées${NC}"

# =============================================================================
# ÉTAPE 3: COMMAND HANDLERS
# =============================================================================

echo -e "\n${YELLOW}⚡ Création Command Handlers...${NC}"

mkdir -p app/application/yeasts/handlers

cat > app/application/yeasts/handlers/YeastCommandHandlers.scala << 'EOF'
package application.yeasts.handlers

import application.yeasts.commands._
import domain.yeasts.model._
import domain.yeasts.repositories.YeastRepository
import domain.yeasts.services.{YeastDomainService, YeastValidationService}
import domain.shared.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

/**
 * Handlers pour les commandes de levures
 * Pattern CQRS - Command Side
 */
@Singleton
class YeastCommandHandlers @Inject()(
  yeastRepository: YeastRepository,
  yeastDomainService: YeastDomainService,
  yeastValidationService: YeastValidationService
)(implicit ec: ExecutionContext) {

  /**
   * Handler pour créer une nouvelle levure
   */
  def handle(command: CreateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    // 1. Validation de la commande
    command.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        // 2. Validation métier avec le service de validation
        yeastValidationService.validateNewYeast(
          name = command.name,
          laboratory = command.laboratory,
          strain = command.strain,
          yeastType = command.yeastType,
          attenuationMin = command.attenuationMin,
          attenuationMax = command.attenuationMax,
          tempMin = command.temperatureMin,
          tempMax = command.temperatureMax,
          alcoholTolerance = command.alcoholTolerance,
          flocculation = command.flocculation,
          aromaProfile = command.aromaProfile,
          flavorProfile = command.flavorProfile
        ) match {
          case Left(validationErrors) => 
            Future.successful(Left(validationErrors.map(_.message)))
            
          case Right(validatedData) =>
            // 3. Vérification unicité laboratoire/souche
            yeastDomainService.checkUniqueLaboratoryStrain(
              validatedData.laboratory, 
              validatedData.strain
            ).flatMap {
              case Left(error) => Future.successful(Left(List(error.message)))
              case Right(_) =>
                
                // 4. Création de l'agrégat
                val characteristics = YeastCharacteristics(
                  aromaProfile = command.aromaProfile,
                  flavorProfile = command.flavorProfile,
                  esters = command.esters,
                  phenols = command.phenols,
                  otherCompounds = command.otherCompounds,
                  notes = command.notes
                )
                
                YeastAggregate.create(
                  name = validatedData.name,
                  laboratory = validatedData.laboratory,
                  strain = validatedData.strain,
                  yeastType = validatedData.yeastType,
                  attenuation = validatedData.attenuation,
                  temperature = validatedData.temperature,
                  alcoholTolerance = validatedData.alcoholTolerance,
                  flocculation = validatedData.flocculation,
                  characteristics = characteristics,
                  createdBy = command.createdBy
                ) match {
                  case Left(domainError) => 
                    Future.successful(Left(List(domainError.message)))
                  case Right(yeast) =>
                    // 5. Sauvegarde
                    yeastRepository.create(yeast).map(Right(_))
                }
            }
        }
    }
  }

  /**
   * Handler pour mettre à jour une levure
   */
  def handle(command: UpdateYeastCommand): Future[Either[List[String], YeastAggregate]] = {
    if (!command.hasChanges) {
      return Future.successful(Left(List("Aucune modification fournie")))
    }
    
    command.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        val yeastId = YeastId(command.yeastId)
        yeastRepository.findById(yeastId).flatMap {
          case None => 
            Future.successful(Left(List(s"Levure non trouvée: ${command.yeastId}")))
            
          case Some(yeast) =>
            // Construire les mises à jour
            var updatedYeast = yeast
            var hasErrors = false
            var errors = List.empty[String]
            
            // Mise à jour du nom si fourni
            command.name.foreach { newName =>
              YeastName.fromString(newName) match {
                case Left(error) => 
                  errors = error :: errors
                  hasErrors = true
                case Right(yeastName) =>
                  updatedYeast.updateName(yeastName, command.updatedBy) match {
                    case Left(domainError) => 
                      errors = domainError.message :: errors
                      hasErrors = true
                    case Right(updated) => 
                      updatedYeast = updated
                  }
              }
            }
            
            // Mise à jour des données techniques si fournies
            val technicalUpdates = (
              command.attenuationMin.map(AttenuationRange(_, command.attenuationMax.getOrElse(yeast.attenuation.max))),
              command.temperatureMin.map(FermentationTemp(_, command.temperatureMax.getOrElse(yeast.temperature.max))),
              command.alcoholTolerance.map(AlcoholTolerance(_)),
              command.flocculation.flatMap(FlocculationLevel.fromName),
              buildUpdatedCharacteristics(yeast.characteristics, command)
            )
            
            technicalUpdates match {
              case (newAttenuation, newTemperature, newAlcoholTolerance, newFlocculation, newCharacteristics) =>
                if (newAttenuation.isDefined || newTemperature.isDefined || 
                    newAlcoholTolerance.isDefined || newFlocculation.isDefined || newCharacteristics.isDefined) {
                  
                  updatedYeast.updateTechnicalData(
                    newAttenuation = newAttenuation,
                    newTemperature = newTemperature,
                    newAlcoholTolerance = newAlcoholTolerance,
                    newFlocculation = newFlocculation,
                    newCharacteristics = newCharacteristics,
                    updatedBy = command.updatedBy
                  ) match {
                    case Left(domainError) => 
                      errors = domainError.message :: errors
                      hasErrors = true
                    case Right(updated) => 
                      updatedYeast = updated
                  }
                }
            }
            
            // Mise à jour laboratoire/souche si fournis
            (command.laboratory, command.strain) match {
              case (Some(labName), strainOpt) => 
                YeastLaboratory.fromName(labName) match {
                  case Some(lab) =>
                    val strainValue = strainOpt.getOrElse(yeast.strain.value)
                    YeastStrain.fromString(strainValue) match {
                      case Right(strain) =>
                        updatedYeast.updateLaboratoryInfo(lab, strain, command.updatedBy) match {
                          case Left(domainError) => 
                            errors = domainError.message :: errors
                            hasErrors = true
                          case Right(updated) => 
                            updatedYeast = updated
                        }
                      case Left(error) => 
                        errors = error :: errors
                        hasErrors = true
                    }
                  case None => 
                    errors = s"Laboratoire inconnu: $labName" :: errors
                    hasErrors = true
                }
              case (None, Some(strainName)) =>
                YeastStrain.fromString(strainName) match {
                  case Right(strain) =>
                    updatedYeast.updateLaboratoryInfo(yeast.laboratory, strain, command.updatedBy) match {
                      case Left(domainError) => 
                        errors = domainError.message :: errors
                        hasErrors = true
                      case Right(updated) => 
                        updatedYeast = updated
                    }
                  case Left(error) => 
                    errors = error :: errors
                    hasErrors = true
                }
              case _ => // Pas de modification laboratoire/souche
            }
            
            if (hasErrors) {
              Future.successful(Left(errors.reverse))
            } else {
              yeastRepository.update(updatedYeast).map(Right(_))
            }
        }
    }
  }

  /**
   * Handler pour changer le statut d'une levure
   */
  def handle(command: ChangeYeastStatusCommand): Future[Either[String, Unit]] = {
    command.validate match {
      case Left(errors) => Future.successful(Left(errors.mkString(", ")))
      case Right(_) =>
        
        YeastStatus.parse(command.newStatus) match {
          case Left(error) => Future.successful(Left(error))
          case Right(newStatus) =>
            
            val yeastId = YeastId(command.yeastId)
            yeastRepository.changeStatus(yeastId, newStatus, command.reason).map(Right(_))
        }
    }
  }

  /**
   * Handler pour activer une levure
   */
  def handle(command: ActivateYeastCommand): Future[Either[String, YeastAggregate]] = {
    val yeastId = YeastId(command.yeastId)
    
    yeastRepository.findById(yeastId).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: ${command.yeastId}"))
      case Some(yeast) =>
        yeast.activate(command.activatedBy) match {
          case Left(domainError) => Future.successful(Left(domainError.message))
          case Right(activatedYeast) => 
            yeastRepository.update(activatedYeast).map(Right(_))
        }
    }
  }

  /**
   * Handler pour désactiver une levure
   */
  def handle(command: DeactivateYeastCommand): Future[Either[String, YeastAggregate]] = {
    val yeastId = YeastId(command.yeastId)
    
    yeastRepository.findById(yeastId).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: ${command.yeastId}"))
      case Some(yeast) =>
        yeast.deactivate(command.reason, command.deactivatedBy) match {
          case Left(domainError) => Future.successful(Left(domainError.message))
          case Right(deactivatedYeast) => 
            yeastRepository.update(deactivatedYeast).map(Right(_))
        }
    }
  }

  /**
   * Handler pour archiver une levure
   */
  def handle(command: ArchiveYeastCommand): Future[Either[String, YeastAggregate]] = {
    val yeastId = YeastId(command.yeastId)
    
    yeastRepository.findById(yeastId).flatMap {
      case None => Future.successful(Left(s"Levure non trouvée: ${command.yeastId}"))
      case Some(yeast) =>
        yeast.archive(command.reason, command.archivedBy) match {
          case Left(domainError) => Future.successful(Left(domainError.message))
          case Right(archivedYeast) => 
            yeastRepository.update(archivedYeast).map(Right(_))
        }
    }
  }

  /**
   * Handler pour supprimer (archiver) une levure
   */
  def handle(command: DeleteYeastCommand): Future[Either[String, Unit]] = {
    val archiveCommand = ArchiveYeastCommand(
      yeastId = command.yeastId,
      reason = Some(command.reason),
      archivedBy = command.deletedBy
    )
    
    handle(archiveCommand).map(_.map(_ => ()))
  }

  /**
   * Handler pour mise à jour batch
   */
  def handle(command: CreateYeastsBatchCommand): Future[Either[List[String], List[YeastAggregate]]] = {
    command.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        // Traiter chaque levure individuellement
        val futures = command.yeasts.map(handle)
        
        Future.sequence(futures).map { results =>
          val errors = results.collect { case Left(errs) => errs }.flatten
          val successes = results.collect { case Right(yeast) => yeast }
          
          if (errors.nonEmpty) {
            Left(errors)
          } else {
            Right(successes)
          }
        }
    }
  }

  // ==========================================================================
  // MÉTHODES UTILITAIRES PRIVÉES
  // ==========================================================================

  private def buildUpdatedCharacteristics(
    current: YeastCharacteristics,
    command: UpdateYeastCommand
  ): Option[YeastCharacteristics] = {
    
    val hasCharacteristicChanges = command.aromaProfile.isDefined || 
                                  command.flavorProfile.isDefined ||
                                  command.esters.isDefined || 
                                  command.phenols.isDefined || 
                                  command.otherCompounds.isDefined || 
                                  command.notes.isDefined
    
    if (hasCharacteristicChanges) {
      Some(YeastCharacteristics(
        aromaProfile = command.aromaProfile.getOrElse(current.aromaProfile),
        flavorProfile = command.flavorProfile.getOrElse(current.flavorProfile),
        esters = command.esters.getOrElse(current.esters),
        phenols = command.phenols.getOrElse(current.phenols),
        otherCompounds = command.otherCompounds.getOrElse(current.otherCompounds),
        notes = command.notes.orElse(current.notes)
      ))
    } else {
      None
    }
  }
}
EOF

# =============================================================================
# ÉTAPE 4: QUERY HANDLERS
# =============================================================================

echo -e "\n${YELLOW}🔍 Création Query Handlers...${NC}"

cat > app/application/yeasts/handlers/YeastQueryHandlers.scala << 'EOF'
package application.yeasts.handlers

import application.yeasts.queries._
import domain.yeasts.model._
import domain.yeasts.repositories.{YeastReadRepository, PaginatedResult}
import domain.yeasts.services.YeastRecommendationService
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handlers pour les queries de levures
 * Pattern CQRS - Query Side
 */
@Singleton
class YeastQueryHandlers @Inject()(
  yeastReadRepository: YeastReadRepository,
  yeastRecommendationService: YeastRecommendationService
)(implicit ec: ExecutionContext) {

  /**
   * Handler pour récupérer une levure par ID
   */
  def handle(query: GetYeastByIdQuery): Future[Option[YeastAggregate]] = {
    val yeastId = YeastId(query.yeastId)
    yeastReadRepository.findById(yeastId)
  }

  /**
   * Handler pour récupérer une levure par nom
   */
  def handle(query: GetYeastByNameQuery): Future[Either[String, Option[YeastAggregate]]] = {
    YeastName.fromString(query.name) match {
      case Left(error) => Future.successful(Left(error))
      case Right(name) => 
        yeastReadRepository.findByName(name).map(Right(_))
    }
  }

  /**
   * Handler pour récupérer une levure par laboratoire et souche
   */
  def handle(query: GetYeastByLaboratoryAndStrainQuery): Future[Either[String, Option[YeastAggregate]]] = {
    (YeastLaboratory.parse(query.laboratory), YeastStrain.fromString(query.strain)) match {
      case (Right(lab), Right(strain)) => 
        yeastReadRepository.findByLaboratoryAndStrain(lab, strain).map(Right(_))
      case (Left(labError), _) => Future.successful(Left(labError))
      case (_, Left(strainError)) => Future.successful(Left(strainError))
    }
  }

  /**
   * Handler pour recherche avec filtres
   */
  def handle(query: FindYeastsQuery): Future[Either[List[String], PaginatedResult[YeastAggregate]]] = {
    query.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) => 
        val filter = query.toFilter
        yeastReadRepository.findByFilter(filter).map(Right(_))
    }
  }

  /**
   * Handler pour recherche par type
   */
  def handle(query: FindYeastsByTypeQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(yeastType) => 
        val results = yeastReadRepository.findByType(yeastType)
        query.limit match {
          case Some(limit) => results.map(_.take(limit)).map(Right(_))
          case None => results.map(Right(_))
        }
    }
  }

  /**
   * Handler pour recherche par laboratoire
   */
  def handle(query: FindYeastsByLaboratoryQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(laboratory) => 
        val results = yeastReadRepository.findByLaboratory(laboratory)
        query.limit match {
          case Some(limit) => results.map(_.take(limit)).map(Right(_))
          case None => results.map(Right(_))
        }
    }
  }

  /**
   * Handler pour recherche par statut
   */
  def handle(query: FindYeastsByStatusQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(status) => 
        val results = yeastReadRepository.findByStatus(status)
        query.limit match {
          case Some(limit) => results.map(_.take(limit)).map(Right(_))
          case None => results.map(Right(_))
        }
    }
  }

  /**
   * Handler pour recherche par plage d'atténuation
   */
  def handle(query: FindYeastsByAttenuationRangeQuery): Future[Either[List[String], List[YeastAggregate]]] = {
    query.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) => 
        yeastReadRepository.findByAttenuationRange(query.minAttenuation, query.maxAttenuation).map(Right(_))
    }
  }

  /**
   * Handler pour recherche par plage de température
   */
  def handle(query: FindYeastsByTemperatureRangeQuery): Future[Either[List[String], List[YeastAggregate]]] = {
    query.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) => 
        yeastReadRepository.findByTemperatureRange(query.minTemperature, query.maxTemperature).map(Right(_))
    }
  }

  /**
   * Handler pour recherche par caractéristiques
   */
  def handle(query: FindYeastsByCharacteristicsQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) => 
        yeastReadRepository.findByCharacteristics(query.characteristics).map(Right(_))
    }
  }

  /**
   * Handler pour recherche textuelle
   */
  def handle(query: SearchYeastsQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) => 
        val results = yeastReadRepository.searchText(query.query)
        query.limit match {
          case Some(limit) => results.map(_.take(limit)).map(Right(_))
          case None => results.map(Right(_))
        }
    }
  }

  /**
   * Handler pour statistiques par statut
   */
  def handle(query: GetYeastStatsQuery): Future[Map[YeastStatus, Long]] = {
    yeastReadRepository.countByStatus
  }

  /**
   * Handler pour statistiques par laboratoire
   */
  def handle(query: GetYeastStatsByLaboratoryQuery): Future[Map[YeastLaboratory, Long]] = {
    yeastReadRepository.getStatsByLaboratory
  }

  /**
   * Handler pour statistiques par type
   */
  def handle(query: GetYeastStatsByTypeQuery): Future[Map[YeastType, Long]] = {
    yeastReadRepository.getStatsByType
  }

  /**
   * Handler pour levures populaires
   */
  def handle(query: GetMostPopularYeastsQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) => 
        yeastReadRepository.findMostPopular(query.limit).map(Right(_))
    }
  }

  /**
   * Handler pour levures récemment ajoutées
   */
  def handle(query: GetRecentlyAddedYeastsQuery): Future[Either[String, List[YeastAggregate]]] = {
    query.validate match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) => 
        yeastReadRepository.findRecentlyAdded(query.limit).map(Right(_))
    }
  }

  /**
   * Handler pour recommandations
   */
  def handle(query: GetYeastRecommendationsQuery): Future[Either[List[String], List[RecommendedYeast]]] = {
    query.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        // Différents types de recommandations selon les paramètres
        (query.beerStyle, query.desiredCharacteristics.nonEmpty) match {
          case (Some(style), _) =>
            yeastRecommendationService.recommendYeastsForBeerStyle(
              style, 
              query.targetAbv, 
              query.limit
            ).map(_.map(_._1)).map(Right(_))
            
          case (None, true) =>
            yeastRecommendationService.getByAromaProfile(
              query.desiredCharacteristics, 
              query.limit
            ).map(Right(_))
            
          case (None, false) =>
            // Recommandations générales pour débutants
            yeastRecommendationService.getBeginnerFriendlyYeasts(query.limit).map(Right(_))
        }
    }
  }

  /**
   * Handler pour alternatives à une levure
   */
  def handle(query: GetYeastAlternativesQuery): Future[Either[List[String], List[RecommendedYeast]]] = {
    query.validate match {
      case Left(errors) => Future.successful(Left(errors))
      case Right(_) =>
        
        val reason = query.reason.toLowerCase match {
          case "unavailable" => AlternativeReason.Unavailable
          case "expensive" => AlternativeReason.TooExpensive
          case "experiment" => AlternativeReason.Experiment
          case _ => AlternativeReason.Unavailable
        }
        
        val yeastId = YeastId(query.originalYeastId)
        yeastRecommendationService.findAlternatives(yeastId, reason, query.limit).map(Right(_))
    }
  }
}
EOF

echo -e "${GREEN}✅ Command et Query Handlers créés${NC}"