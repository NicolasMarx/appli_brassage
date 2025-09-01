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
