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
