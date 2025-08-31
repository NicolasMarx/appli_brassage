package application.queries.admin.malts.handlers

import application.queries.admin.malts.AdminMaltDetailQuery
import application.queries.admin.malts.readmodels._
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.{MaltAggregate, MaltId}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminMaltDetailQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: AdminMaltDetailQuery): Future[Option[AdminMaltDetailResult]] = {
    // Conversion String → MaltId
    MaltId(query.id) match {
      case Right(maltId) =>
        maltReadRepo.findById(maltId).map { maltOpt =>
          maltOpt.map { malt =>
            val adminReadModel = AdminMaltReadModel.fromAggregate(malt)
            AdminMaltDetailResult(
              malt = adminReadModel,
              substitutes = List.empty,
              beerStyleCompatibilities = List.empty,
              statistics = MaltUsageStatistics(
                recipeCount = 0,
                avgUsagePercent = 0.0,
                popularityScore = 0.0
              ),
              qualityAnalysis = QualityAnalysis(
                dataCompleteness = calculateCompleteness(malt),
                sourceReliability = if (malt.credibilityScore >= 0.8) "High" else "Medium",
                reviewStatus = if (malt.needsReview) "Needs Review" else "Validated",
                lastValidated = Some(malt.updatedAt)
              )
            )
          }
        }
      case Left(_) =>
        Future.successful(None) // ID invalide
    }
  }

  private def calculateCompleteness(malt: MaltAggregate): Double = {
    val fields = List(malt.description, Some(malt.flavorProfiles).filter(_.nonEmpty))
    val completedFields = fields.count(_.isDefined)
    completedFields.toDouble / fields.length
  }
}
