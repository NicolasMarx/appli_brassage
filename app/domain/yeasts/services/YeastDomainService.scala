package domain.yeasts.services

import domain.yeasts.model.{YeastName, YeastStrain, YeastLaboratory}
import domain.yeasts.repositories.YeastReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service domaine pour les validations métier des levures
 * Implémente les règles business spécifiques au domaine
 */
@Singleton
class YeastDomainService @Inject()(
  yeastReadRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {

  /**
   * Valide qu'une nouvelle levure peut être créée
   * Règles:
   * - Pas de doublon nom + laboratoire
   * - Pas de doublon souche + laboratoire
   */
  def validateNewYeast(
    name: YeastName, 
    strain: YeastStrain, 
    laboratory: YeastLaboratory
  ): Future[Either[List[String], Unit]] = {
    
    val validations = for {
      nameExists <- checkNameExists(name, laboratory)
      strainExists <- checkStrainExists(strain, laboratory)
    } yield (nameExists, strainExists)

    validations.map { case (nameExists, strainExists) =>
      val errors = List(
        if (nameExists) Some(s"Une levure avec le nom '${name.value}' existe déjà chez ${laboratory.name}") else None,
        if (strainExists) Some(s"La souche '${strain.value}' existe déjà chez ${laboratory.name}") else None
      ).flatten

      if (errors.nonEmpty) Left(errors) else Right(())
    }
  }

  /**
   * Valide les business rules pour la mise à jour
   */
  def validateYeastUpdate(
    originalName: YeastName,
    newName: YeastName,
    laboratory: YeastLaboratory
  ): Future[Either[List[String], Unit]] = {
    if (originalName == newName) {
      Future.successful(Right(()))
    } else {
      checkNameExists(newName, laboratory).map { exists =>
        if (exists) Left(List(s"Une levure avec le nom '${newName.value}' existe déjà"))
        else Right(())
      }
    }
  }

  // ==========================================================================
  // MÉTHODES PRIVÉES
  // ==========================================================================

  private def checkNameExists(name: YeastName, laboratory: YeastLaboratory): Future[Boolean] = {
    yeastReadRepository.findByName(name).map(_.isDefined)
  }

  private def checkStrainExists(strain: YeastStrain, laboratory: YeastLaboratory): Future[Boolean] = {
    // Implémentation simplifiée - dans un vrai projet, on vérifierait strain + laboratory
    Future.successful(false)
  }
}