#!/bin/bash
# Correction ciblée des 25 erreurs de compilation restantes

set -e

echo "🎯 Correction des 25 erreurs spécifiques..."

# =============================================================================
# ERREUR 1-4 : HANDLERS AVEC PARAMÈTRES INCORRECTS
# =============================================================================

echo "🔧 Correction handlers avec paramètres incorrects..."

# Correction AdminMaltDetailQueryHandler
cat > app/application/queries/admin/malts/handlers/AdminMaltDetailQueryHandler.scala << 'EOF'
package application.queries.admin.malts.handlers

import application.queries.admin.malts.AdminMaltDetailQuery
import application.queries.admin.malts.readmodels._
import domain.malts.repositories.MaltReadRepository
import domain.malts.model.MaltAggregate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminMaltDetailQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: AdminMaltDetailQuery): Future[Option[AdminMaltDetailResult]] = {
    maltReadRepo.findById(query.id).map { maltOpt =>
      maltOpt.map { malt =>
        val adminReadModel = AdminMaltReadModel.fromAggregate(malt)
        AdminMaltDetailResult(
          malt = adminReadModel,
          substitutes = List.empty, // Sera implémenté plus tard
          beerStyleCompatibilities = List.empty, // Sera implémenté plus tard  
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
  }

  private def calculateCompleteness(malt: MaltAggregate): Double = {
    val fields = List(malt.description, Some(malt.flavorProfiles).filter(_.nonEmpty))
    val completedFields = fields.count(_.isDefined)
    completedFields.toDouble / fields.length
  }
}
EOF

# Correction MaltDetailQueryHandler  
cat > app/application/queries/public/malts/handlers/MaltDetailQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.MaltDetailQuery
import application.queries.public.malts.readmodels._
import domain.malts.repositories.MaltReadRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaltDetailQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltDetailQuery): Future[Option[MaltDetailResult]] = {
    maltReadRepo.findById(query.id).map { maltOpt =>
      maltOpt.map { malt =>
        val maltReadModel = MaltReadModel.fromAggregate(malt)
        MaltDetailResult(
          malt = maltReadModel,
          substitutes = List.empty // Sera implémenté plus tard avec findSubstitutes
        )
      }
    }
  }
}
EOF

# =============================================================================
# ERREUR 5-7 : CORRECTION MALTSEARCHQUERYHANDLER
# =============================================================================

echo "🔍 Correction MaltSearchQueryHandler..."

cat > app/application/queries/public/malts/handlers/MaltSearchQueryHandler.scala << 'EOF'
package application.queries.public.malts.handlers

import application.queries.public.malts.MaltSearchQuery
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.repositories.{MaltReadRepository, PagedResult}
import domain.malts.model.{MaltType, MaltStatus}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaltSearchQueryHandler @Inject()(
  maltReadRepo: MaltReadRepository
)(implicit ec: ExecutionContext) {

  def handle(query: MaltSearchQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    executeSearch(query)
  }

  private def executeSearch(query: MaltSearchQuery): Future[Either[String, PagedResult[MaltReadModel]]] = {
    // Conversion des types domain vers String pour les filtres
    val maltTypeFilter = query.maltType.flatMap(MaltType.fromName).map(_.name)
    val statusFilter = if (query.activeOnly) Some("ACTIVE") else None
    
    maltReadRepo.findByFilters(
      maltType = maltTypeFilter,
      minEBC = query.minEBC,
      maxEBC = query.maxEBC,
      originCode = query.originCode,
      status = statusFilter,
      searchTerm = query.searchTerm,
      flavorProfiles = query.flavorProfiles.getOrElse(List.empty),
      page = query.page,
      pageSize = query.pageSize
    ).map { pagedResult =>
      val readModels = pagedResult.items.map(MaltReadModel.fromAggregate)
      Right(PagedResult(
        items = readModels,
        currentPage = pagedResult.currentPage,
        pageSize = pagedResult.pageSize,
        totalCount = pagedResult.totalCount,
        hasNext = pagedResult.hasNext
      ))
    }.recover {
      case ex => Left(s"Erreur lors de la recherche: ${ex.getMessage}")
    }
  }
}
EOF

# =============================================================================
# ERREUR 8-11 : CORRECTION REPOSITORIES SLICK
# =============================================================================

echo "🗄️ Correction repositories Slick..."

# Correction SlickMaltReadRepository
cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.{MaltReadRepository, PagedResult, MaltSubstitution, MaltCompatibility}
import infrastructure.persistence.slick.tables.MaltTables
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltReadRepository @Inject()(
  val profile: PostgresProfile,
  db: PostgresProfile#Backend#Database
)(implicit ec: ExecutionContext) extends MaltReadRepository with MaltTables {

  import profile.api._

  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.id === java.util.UUID.fromString(id.value))
    db.run(query.result.headOption).map(_.flatMap { row =>
      rowToAggregate(row).toOption
    })
  }

  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.name === name)
    db.run(query.result.headOption).map(_.flatMap { row =>
      rowToAggregate(row).toOption
    })
  }

  override def existsByName(name: String): Future[Boolean] = {
    val query = malts.filter(_.name === name).exists
    db.run(query.result)
  }

  override def findAll(page: Int, pageSize: Int, activeOnly: Boolean): Future[List[MaltAggregate]] = {
    var query = malts
    
    if (activeOnly) {
      query = query.filter(_.isActive === true)
    }
    
    val pagedQuery = query
      .drop(page * pageSize)
      .take(pageSize)
      
    db.run(pagedQuery.result).map { rows =>
      rows.flatMap(rowToAggregate(_).toOption).toList
    }
  }

  override def count(activeOnly: Boolean): Future[Long] = {
    var query = malts
    
    if (activeOnly) {
      query = query.filter(_.isActive === true)
    }
    
    db.run(query.length.result).map(_.toLong)
  }

  override def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]] = {
    Future.successful(List.empty) // TODO: Implémenter avec les tables de substitution
  }

  override def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]] = {
    Future.successful(PagedResult(
      items = List.empty,
      currentPage = page,
      pageSize = pageSize,
      totalCount = 0,
      hasNext = false
    ))
  }

  override def findByFilters(
    maltType: Option[String],
    minEBC: Option[Double],
    maxEBC: Option[Double],
    originCode: Option[String],
    status: Option[String],
    source: Option[String],
    minCredibility: Option[Double],
    searchTerm: Option[String],
    flavorProfiles: List[String],
    minExtraction: Option[Double],
    minDiastaticPower: Option[Double],
    page: Int,
    pageSize: Int
  ): Future[PagedResult[MaltAggregate]] = {
    
    var query = malts
    
    // Appliquer les filtres
    maltType.foreach(mt => query = query.filter(_.maltType === mt))
    minEBC.foreach(min => query = query.filter(_.ebcColor >= min))
    maxEBC.foreach(max => query = query.filter(_.ebcColor <= max))
    originCode.foreach(oc => query = query.filter(_.originCode === oc))
    source.foreach(s => query = query.filter(_.source === s))
    minCredibility.foreach(mc => query = query.filter(_.credibilityScore >= mc.toInt))
    searchTerm.foreach(term => query = query.filter(_.name.toLowerCase.like(s"%${term.toLowerCase}%")))
    
    // Requête paginée
    val pagedQuery = query.drop(page * pageSize).take(pageSize)
    val countQuery = query.length
    
    for {
      rows <- db.run(pagedQuery.result)
      count <- db.run(countQuery.result)
    } yield {
      val aggregates = rows.flatMap(rowToAggregate(_).toOption).toList
      PagedResult(
        items = aggregates,
        currentPage = page,
        pageSize = pageSize,
        totalCount = count.toLong,
        hasNext = (page + 1) * pageSize < count
      )
    }
  }
}
EOF

# Correction SlickMaltWriteRepository
cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.MaltWriteRepository
import infrastructure.persistence.slick.tables.MaltTables
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltWriteRepository @Inject()(
  val profile: PostgresProfile,
  db: PostgresProfile#Backend#Database
)(implicit ec: ExecutionContext) extends MaltWriteRepository with MaltTables {

  import profile.api._

  override def save(malt: MaltAggregate): Future[MaltAggregate] = {
    val row = aggregateToRow(malt)
    val upsertQuery = malts.insertOrUpdate(row)
    
    db.run(upsertQuery).map(_ => malt)
  }

  override def delete(id: MaltId): Future[Boolean] = {
    val deleteQuery = malts.filter(_.id === java.util.UUID.fromString(id.value)).delete
    db.run(deleteQuery).map(_ > 0)
  }
}
EOF

# =============================================================================
# ERREUR 12-14 : CORRECTION MALTTABLES
# =============================================================================

echo "📊 Correction MaltTables..."

cat > app/infrastructure/persistence/slick/tables/MaltTables.scala << 'EOF'
package infrastructure.persistence.slick.tables

import slick.jdbc.JdbcProfile
import java.time.Instant
import java.util.UUID

trait MaltTables {
  val profile: JdbcProfile
  import profile.api._

  // Classe représentant une ligne de la table malts
  case class MaltRow(
    id: UUID,
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String], // JSON String
    source: String,
    isActive: Boolean,
    credibilityScore: Int,
    createdAt: Instant,
    updatedAt: Instant,
    version: Int
  )

  // Table malts principale
  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def maltType = column[String]("malt_type")
    def ebcColor = column[Double]("ebc_color")
    def extractionRate = column[Double]("extraction_rate")
    def diastaticPower = column[Double]("diastatic_power")
    def originCode = column[String]("origin_code")
    def description = column[Option[String]]("description")
    def flavorProfiles = column[Option[String]]("flavor_profiles")
    def source = column[String]("source")
    def isActive = column[Boolean]("is_active")
    def credibilityScore = column[Int]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Int]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  // Table beer styles compatibility 
  case class MaltBeerStyleRow(
    id: UUID,
    maltId: UUID,
    beerStyleId: String,
    compatibilityScore: Double,
    usageNotes: Option[String]
  )

  class MaltBeerStyleTable(tag: Tag) extends Table[MaltBeerStyleRow](tag, "malt_beer_styles") {
    def id = column[UUID]("id", O.PrimaryKey)
    def maltId = column[UUID]("malt_id")
    def beerStyleId = column[String]("beer_style_id")
    def compatibilityScore = column[Double]("compatibility_score")
    def usageNotes = column[Option[String]]("usage_notes")

    def * = (id, maltId, beerStyleId, compatibilityScore, usageNotes).mapTo[MaltBeerStyleRow]
  }

  // Table substitutions
  case class MaltSubstitutionRow(
    id: UUID,
    maltId: UUID,
    substituteId: UUID,
    substitutionRatio: Double,
    notes: Option[String],
    qualityScore: Double
  )

  class MaltSubstitutionTable(tag: Tag) extends Table[MaltSubstitutionRow](tag, "malt_substitutions") {
    def id = column[UUID]("id", O.PrimaryKey)
    def maltId = column[UUID]("malt_id")
    def substituteId = column[UUID]("substitute_id")
    def substitutionRatio = column[Double]("substitution_ratio")
    def notes = column[Option[String]]("notes")
    def qualityScore = column[Double]("quality_score")

    def * = (id, maltId, substituteId, substitutionRatio, notes, qualityScore).mapTo[MaltSubstitutionRow]
  }

  // Helper pour convertir row vers aggregate
  def rowToAggregate(row: MaltRow): Either[String, domain.malts.model.MaltAggregate] = {
    import domain.malts.model._
    import domain.shared.NonEmptyString
    
    for {
      maltId <- MaltId(row.id.toString)
      name <- NonEmptyString(row.name)
      maltType <- MaltType.fromName(row.maltType).toRight(s"Type malt invalide: ${row.maltType}")
      ebcColor <- EBCColor(row.ebcColor)
      extractionRate <- ExtractionRate(row.extractionRate)
      diastaticPower <- DiastaticPower(row.diastaticPower)
      source <- MaltSource.fromName(row.source).toRight(s"Source invalide: ${row.source}")
    } yield MaltAggregate(
      id = maltId,
      name = name,
      maltType = maltType,
      ebcColor = ebcColor,
      extractionRate = extractionRate,
      diastaticPower = diastaticPower,
      originCode = row.originCode,
      description = row.description,
      flavorProfiles = row.flavorProfiles.map(_.split(",").toList).getOrElse(List.empty),
      source = source,
      isActive = row.isActive,
      credibilityScore = row.credibilityScore.toDouble,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = row.version.toLong
    )
  }

  // Helper pour convertir aggregate vers row
  def aggregateToRow(aggregate: domain.malts.model.MaltAggregate): MaltRow = {
    MaltRow(
      id = UUID.fromString(aggregate.id.value),
      name = aggregate.name.value,
      maltType = aggregate.maltType.name,
      ebcColor = aggregate.ebcColor.value,
      extractionRate = aggregate.extractionRate.value,
      diastaticPower = aggregate.diastaticPower.value,
      originCode = aggregate.originCode,
      description = aggregate.description,
      flavorProfiles = if (aggregate.flavorProfiles.nonEmpty) Some(aggregate.flavorProfiles.mkString(",")) else None,
      source = aggregate.source.name,
      isActive = aggregate.isActive,
      credibilityScore = aggregate.credibilityScore.toInt,
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version.toInt
    )
  }

  // TableQuery instances
  val malts = TableQuery[MaltTable]
  val maltBeerStyles = TableQuery[MaltBeerStyleTable]
  val maltSubstitutions = TableQuery[MaltSubstitutionTable]
}
EOF

# =============================================================================
# ERREUR 15 : CORRECTION BASECONTROLLER
# =============================================================================

echo "🎮 Correction BaseController..."

cat > app/interfaces/http/common/BaseController.scala << 'EOF'
package interfaces.http.common

import domain.common.DomainError
import play.api.libs.json.Json
import play.api.mvc.{BaseController => PlayBaseController, Result}
import play.api.mvc.Results._

trait BaseController extends PlayBaseController {
  
  def handleDomainError(error: DomainError): Result = {
    // Vérification du type d'erreur basée sur le code
    if (error.code.contains("NOT_FOUND")) {
      NotFound(Json.obj("error" -> error.message, "code" -> error.code))
    } else if (error.code.contains("BUSINESS_RULE")) {
      BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
    } else if (error.code.contains("VALIDATION")) {
      BadRequest(Json.obj("error" -> error.message, "code" -> error.code))
    } else {
      InternalServerError(Json.obj("error" -> "Erreur interne"))
    }
  }
}
EOF

# =============================================================================
# ERREUR 16-17 : CORRECTION ROUTES (DÉPLACEMENT CONTROLLERS)
# =============================================================================

echo "🛣️ Correction structure controllers pour routes..."

# Créer les dossiers manquants pour les routes
mkdir -p app/controllers/api/v1/malts
mkdir -p app/controllers/admin

# Déplacer/copier les controllers aux bons endroits pour les routes
cp app/interfaces/http/api/v1/malts/MaltsController.scala app/controllers/api/v1/malts/MaltsController.scala
cp app/interfaces/http/api/admin/malts/AdminMaltsController.scala app/controllers/admin/AdminMaltsController.scala

# Mettre à jour les packages
sed -i.bak 's/package interfaces.http.api.v1.malts/package controllers.api.v1.malts/' app/controllers/api/v1/malts/MaltsController.scala
sed -i.bak 's/package interfaces.http.api.admin.malts/package controllers.admin/' app/controllers/admin/AdminMaltsController.scala

echo "✅ Corrections appliquées"

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo "🔍 Test de compilation après corrections ciblées..."

if sbt compile > /tmp/targeted_fix.log 2>&1; then
    echo "✅ COMPILATION RÉUSSIE !"
    echo ""
    echo "🎉 Toutes les 25 erreurs ont été corrigées !"
    echo "📊 Le domaine Malts compile maintenant complètement"
else
    echo "❌ Erreurs restantes:"
    head -20 /tmp/targeted_fix.log
    echo ""
    echo "Log complet: /tmp/targeted_fix.log"
fi