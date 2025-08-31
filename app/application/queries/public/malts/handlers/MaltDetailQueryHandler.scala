package application.queries.public.malts.handlers

import application.queries.public.malts.{MaltDetailQuery, MaltReadModel}
import domain.malts.model._
import domain.malts.repositories.MaltReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour le détail d'un malt (API publique)
 */
@Singleton
class MaltDetailQueryHandler @Inject()(
                                        maltReadRepo: MaltReadRepository
                                      )(implicit ec: ExecutionContext) {

  def handle(query: MaltDetailQuery): Future[Either[String, MaltDetailResult]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeQuery(validQuery)
    }
  }

  private def executeQuery(query: MaltDetailQuery): Future[Either[String, MaltDetailResult]] = {
    for {
      maltId <- Future.fromTry(MaltId(query.id).fold(
        error => scala.util.Failure(new IllegalArgumentException(error)),
        id => scala.util.Success(id)
      ))

      maltOpt <- maltReadRepo.findById(maltId)

      result <- maltOpt match {
        case None =>
          Future.successful(Left(s"Malt non trouvé: ${query.id}"))
        case Some(malt) =>
          buildDetailResult(malt, query)
      }
    } yield result
  }

  private def buildDetailResult(
                                 malt: MaltAggregate,
                                 query: MaltDetailQuery
                               ): Future[Either[String, MaltDetailResult]] = {

    val baseReadModel = MaltReadModel.fromAggregate(malt)

    for {
      substitutes <- if (query.includeSubstitutes) {
        maltReadRepo.findSubstitutes(malt.id).map(_.map(convertSubstitute))
      } else {
        Future.successful(List.empty)
      }

      beerStyles <- if (query.includeBeerStyles) {
        // Pour simplification, retourner liste vide
        // En production, récupérer depuis beer styles repository
        Future.successful(List.empty[String])
      } else {
        Future.successful(List.empty)
      }
    } yield {
      Right(MaltDetailResult(
        malt = baseReadModel,
        substitutes = substitutes,
        compatibleBeerStyles = beerStyles,
        qualityScore = malt.qualityScore
      ))
    }
  }

  private def convertSubstitute(substitute: MaltSubstitution): SubstituteReadModel = {
    SubstituteReadModel(
      malt = MaltReadModel.fromAggregate(substitute.substituteMalt),
      substitutionRatio = substitute.substitutionRatio,
      notes = substitute.compatibilityNotes
    )
  }
}

case class MaltDetailResult(
                             malt: MaltReadModel,
                             substitutes: List[SubstituteReadModel],
                             compatibleBeerStyles: List[String],
                             qualityScore: Int
                           )

case class SubstituteReadModel(
                                malt: MaltReadModel,
                                substitutionRatio: Double,
                                notes: Option[String]
                              )