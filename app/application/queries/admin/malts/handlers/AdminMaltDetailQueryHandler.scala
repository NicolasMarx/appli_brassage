package application.queries.admin.malts.handlers

import application.queries.admin.malts.AdminMaltDetailQuery
import domain.malts.model._
import domain.malts.repositories.MaltReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler détail malt pour administration avec informations étendues
 */
@Singleton
class AdminMaltDetailQueryHandler @Inject()(
                                             maltReadRepo: MaltReadRepository
                                             // auditService: AuditService // TODO: Ajouter quand disponible
                                           )(implicit ec: ExecutionContext) {

  def handle(query: AdminMaltDetailQuery): Future[Either[String, AdminMaltDetailResult]] = {
    query.validate() match {
      case Left(error) => Future.successful(Left(error))
      case Right(validQuery) => executeQuery(validQuery)
    }
  }

  private def executeQuery(query: AdminMaltDetailQuery): Future[Either[String, AdminMaltDetailResult]] = {
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
          buildAdminDetailResult(malt, query)
      }
    } yield result
  }

  private def buildAdminDetailResult(
                                      malt: MaltAggregate,
                                      query: AdminMaltDetailQuery
                                    ): Future[Either[String, AdminMaltDetailResult]] = {

    val baseReadModel = AdminMaltReadModel.fromAggregate(malt)

    for {
      substitutes <- if (query.includeSubstitutes) {
        maltReadRepo.findSubstitutes(malt.id).map(_.map(convertAdminSubstitute))
      } else {
        Future.successful(List.empty)
      }

      beerStyleCompatibility <- if (query.includeBeerStyles) {
        maltReadRepo.findCompatibleWithBeerStyle("", 0, 50)
          .map(_.items.filter(_.malt.id == malt.id.value))
      } else {
        Future.successful(List.empty)
      }

      auditLog <- if (query.includeAuditLog) {
        // TODO: Récupérer audit logs
        Future.successful(List.empty[AuditLogEntry])
      } else {
        Future.successful(List.empty)
      }

      statistics <- if (query.includeStatistics) {
        calculateMaltStatistics(malt)
      } else {
        Future.successful(None)
      }
    } yield {
      Right(AdminMaltDetailResult(
        malt = baseReadModel,
        substitutes = substitutes,
        beerStyleCompatibility = beerStyleCompatibility.map(convertCompatibility),
        auditLog = auditLog,
        statistics = statistics,
        qualityAnalysis = analyzeQuality(malt),
        recommendations = generateRecommendations(malt)
      ))
    }
  }

  private def convertAdminSubstitute(substitute: MaltSubstitution): AdminSubstituteReadModel = {
    AdminSubstituteReadModel(
      malt = AdminMaltReadModel.fromAggregate(substitute.substituteMalt),
      substitutionRatio = substitute.substitutionRatio,
      notes = substitute.compatibilityNotes
    )
  }

  private def convertCompatibility(compatibility: MaltCompatibility): BeerStyleCompatibility = {
    BeerStyleCompatibility(
      beerStyleId = compatibility.beerStyleId,
      compatibilityScore = compatibility.compatibilityScore,
      typicalPercentage = compatibility.typicalPercentage
    )
  }

  private def calculateMaltStatistics(malt: MaltAggregate): Future[Option[MaltUsageStatistics]] = {
    // TODO: Calculer statistiques d'usage dans recettes, etc.
    Future.successful(Some(MaltUsageStatistics(
      usageInRecipes = 0,
      averagePercentageInRecipes = 0.0,
      popularityRank = 0,
      lastUsedDate = None
    )))
  }

  private def analyzeQuality(malt: MaltAggregate): QualityAnalysis = {
    val issues = scala.collection.mutable.ListBuffer[String]()
    val suggestions = scala.collection.mutable.ListBuffer[String]()

    // Analyse de la crédibilité
    if (malt.needsReview) {
      issues += "Score de crédibilité faible - révision recommandée"
      suggestions += "Vérifier les informations avec sources officielles"
    }

    // Analyse de la complétude
    if (malt.description.isEmpty) {
      suggestions += "Ajouter une description détaillée"
    }

    if (malt.flavorProfiles.isEmpty) {
      suggestions += "Définir les profils arômes"
    }

    // Analyse cohérence maltage
    if (malt.maltType == MaltType.BASE && !malt.canSelfConvert) {
      issues += "Malt de base avec pouvoir diastasique insuffisant"
    }

    QualityAnalysis(
      overallScore = malt.qualityScore,
      issues = issues.toList,
      suggestions = suggestions.toList,
      completenessPercentage = calculateCompleteness(malt)
    )
  }

  private def calculateCompleteness(malt: MaltAggregate): Double = {
    val factors = List(
      malt.description.nonEmpty,
      malt.flavorProfiles.nonEmpty,
      malt.credibilityScore.value >= 80
    )
    (factors.count(identity).toDouble / factors.length) * 100
  }

  private def generateRecommendations(malt: MaltAggregate): List[String] = {
    val recommendations = scala.collection.mutable.ListBuffer[String]()

    // Recommandations basées sur le type
    malt.maltType match {
      case MaltType.BASE =>
        recommendations += s"Utilisation recommandée: 50-100% du grain bill"
        if (malt.diastaticPower.canConvertAdjuncts) {
          recommendations += "Peut convertir des adjuvants (riz, maïs)"
        }
      case MaltType.CRYSTAL =>
        recommendations += s"Utilisation recommandée: maximum ${malt.maxRecommendedPercent}%"
        recommendations += "Ajoute couleur et douceur caramélisée"
      case MaltType.ROASTED =>
        recommendations += "Utiliser avec parcimonie - saveurs intenses"
        recommendations += "Idéal pour stouts et porters"
      case _ => // autres types
    }

    // Recommandations température d'empâtage
    if (malt.diastaticPower.value > 80) {
      recommendations += "Empâtage possible à basse température (63-65°C)"
    } else if (malt.diastaticPower.value > 0) {
      recommendations += "Empâtage température standard (65-67°C)"
    }

    recommendations.toList
  }
}

// ReadModels admin avec informations étendues
case class AdminMaltReadModel(
                               id: String,
                               name: String,
                               maltType: String,
                               ebcColor: Double,
                               extractionRate: Double,
                               diastaticPower: Double,
                               origin: OriginReadModel,
                               description: Option[String],
                               status: String,
                               source: String,
                               credibilityScore: Int,
                               flavorProfiles: List[String],
                               // Informations calculées
                               colorName: String,
                               extractionCategory: String,
                               enzymaticCategory: String,
                               maxRecommendedPercent: Double,
                               qualityScore: Int,
                               needsReview: Boolean,
                               // Métadonnées
                               createdAt: java.time.Instant,
                               updatedAt: java.time.Instant,
                               version: Int
                             )

object AdminMaltReadModel {
  def fromAggregate(malt: MaltAggregate): AdminMaltReadModel = {
    AdminMaltReadModel(
      id = malt.id.value,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      origin = OriginReadModel(
        code = malt.origin.code,
        name = malt.origin.name,
        region = malt.origin.region,
        isNoble = malt.origin.isNoble,
        isNewWorld = malt.origin.isNewWorld
      ),
      description = malt.description,
      status = malt.status.name,
      source = malt.source.name,
      credibilityScore = malt.credibilityScore.value,
      flavorProfiles = malt.flavorProfiles,
      colorName = malt.ebcColor.colorName,
      extractionCategory = malt.extractionRate.extractionCategory,
      enzymaticCategory = malt.diastaticPower.enzymaticCategory,
      maxRecommendedPercent = malt.maxRecommendedPercent,
      qualityScore = malt.qualityScore,
      needsReview = malt.needsReview,
      createdAt = malt.createdAt,
      updatedAt = malt.updatedAt,
      version = malt.version
    )
  }
}

case class AdminMaltDetailResult(
                                  malt: AdminMaltReadModel,
                                  substitutes: List[AdminSubstituteReadModel],
                                  beerStyleCompatibility: List[BeerStyleCompatibility],
                                  auditLog: List[AuditLogEntry],
                                  statistics: Option[MaltUsageStatistics],
                                  qualityAnalysis: QualityAnalysis,
                                  recommendations: List[String]
                                )

case class AdminSubstituteReadModel(
                                     malt: AdminMaltReadModel,
                                     substitutionRatio: Double,
                                     notes: Option[String]
                                   )

case class BeerStyleCompatibility(
                                   beerStyleId: String,
                                   compatibilityScore: Option[Int],
                                   typicalPercentage: Option[Double]
                                 )

case class AuditLogEntry(
                          id: String,
                          adminId: String,
                          action: String,
                          changes: Map[String, String],
                          timestamp: java.time.Instant
                        )

case class MaltUsageStatistics(
                                usageInRecipes: Int,
                                averagePercentageInRecipes: Double,
                                popularityRank: Int,
                                lastUsedDate: Option[java.time.Instant]
                              )

case class QualityAnalysis(
                            overallScore: Int,
                            issues: List[String],
                            suggestions: List[String],
                            completenessPercentage: Double
                          )