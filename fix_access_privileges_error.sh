#!/bin/bash

# =============================================================================
# CORRECTION PRIVILÈGES D'ACCÈS - MALTTABLES
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Correction privilèges d'accès MaltTables${NC}"

# =============================================================================
# CORRIGER MALTTABLES - VISIBILITÉ PROFILE
# =============================================================================

echo -e "${YELLOW}Correction visibilité profile dans MaltTables...${NC}"

# Backup
cp "infrastructure/persistence/slick/tables/MaltTables.scala" \
   "infrastructure/persistence/slick/tables/MaltTables.scala.backup.$(date +%Y%m%d_%H%M%S)"

# Corriger la visibilité du profile
sed -i.bak 's/val profile: JdbcProfile/protected val profile: JdbcProfile/' \
    "infrastructure/persistence/slick/tables/MaltTables.scala"

echo -e "${GREEN}Visibilité profile corrigée dans MaltTables${NC}"

# =============================================================================
# CORRIGER MODULE GUICE - SINGLETON
# =============================================================================

echo -e "${YELLOW}Correction module Guice...${NC}"

cat > app/modules/MaltModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import infrastructure.persistence.slick.repositories.malts.{SlickMaltReadRepository, SlickMaltWriteRepository}

class MaltModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[MaltReadRepository]).to(classOf[SlickMaltReadRepository]).asEagerSingleton()
    bind(classOf[MaltWriteRepository]).to(classOf[SlickMaltWriteRepository]).asEagerSingleton()
  }
}
EOF

echo -e "${GREEN}Module Guice corrigé avec Singletons${NC}"

# =============================================================================
# ALTERNATIVE : REPOSITORIES SANS MALTTABLES SI PROBLÈME PERSISTE
# =============================================================================

echo -e "${YELLOW}Création repositories autonomes (alternative)...${NC}"

# SlickMaltReadRepository autonome
cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId, MaltType, EBCColor, ExtractionRate, DiastaticPower, MaltSource}
import domain.malts.repositories.{MaltReadRepository, MaltSubstitution, MaltCompatibility}
import domain.common.PagedResult
import domain.shared.NonEmptyString
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

@Singleton
class SlickMaltReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
extends MaltReadRepository 
with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  
  // Définition directe de la table dans le repository
  case class MaltRow(
    id: String,
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String],
    source: String,
    isActive: Boolean,
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  )

  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[String]("id", O.PrimaryKey)
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
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  private val malts = TableQuery[MaltTable]
  
  private def rowToAggregate(row: MaltRow): Option[MaltAggregate] = {
    for {
      maltId <- MaltId(row.id).toOption
      name <- NonEmptyString.create(row.name).toOption
      maltType <- MaltType.fromName(row.maltType)
      ebcColor <- EBCColor(row.ebcColor).toOption
      extractionRate <- ExtractionRate(row.extractionRate).toOption
      diastaticPower <- DiastaticPower(row.diastaticPower).toOption
      source <- MaltSource.fromName(row.source)
    } yield {
      MaltAggregate(
        id = maltId,
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = row.originCode,
        description = row.description,
        flavorProfiles = row.flavorProfiles.map(_.split(",").toList.filter(_.trim.nonEmpty)).getOrElse(List.empty),
        source = source,
        isActive = row.isActive,
        credibilityScore = row.credibilityScore,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version
      )
    }
  }
  
  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.id === id.value)
    db.run(query.result.headOption).map(_.flatMap(rowToAggregate))
  }
  
  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.name === name)
    db.run(query.result.headOption).map(_.flatMap(rowToAggregate))
  }
  
  override def existsByName(name: String): Future[Boolean] = {
    val query = malts.filter(_.name === name).exists
    db.run(query.result)
  }
  
  override def findAll(page: Int = 0, pageSize: Int = 20, activeOnly: Boolean = true): Future[List[MaltAggregate]] = {
    val query = malts
      .filter(_.isActive === true)
      .sortBy(_.name)
      .drop(page * pageSize)
      .take(pageSize)
      
    db.run(query.result).map(_.flatMap(rowToAggregate).toList)
  }
  
  override def count(activeOnly: Boolean = true): Future[Long] = {
    val query = if (activeOnly) malts.filter(_.isActive === true) else malts
    db.run(query.length.result).map(_.toLong)
  }
  
  override def findSubstitutes(maltId: MaltId): Future[List[MaltSubstitution]] = {
    Future.successful(List.empty)
  }
  
  override def findCompatibleWithBeerStyle(beerStyleId: String, page: Int, pageSize: Int): Future[PagedResult[MaltCompatibility]] = {
    Future.successful(PagedResult.empty)
  }
  
  override def findByFilters(
    maltType: Option[String] = None,
    minEBC: Option[Double] = None,
    maxEBC: Option[Double] = None,
    originCode: Option[String] = None,
    status: Option[String] = None,
    source: Option[String] = None,
    minCredibility: Option[Double] = None,
    searchTerm: Option[String] = None,
    flavorProfiles: List[String] = List.empty,
    minExtraction: Option[Double] = None,
    minDiastaticPower: Option[Double] = None,
    page: Int = 0,
    pageSize: Int = 20
  ): Future[PagedResult[MaltAggregate]] = {
    
    var query = malts.filter(_.isActive === true)
    
    maltType.foreach(mt => query = query.filter(_.maltType === mt))
    originCode.foreach(code => query = query.filter(_.originCode === code))
    source.foreach(src => query = query.filter(_.source === src))
    searchTerm.foreach(term => query = query.filter(_.name.like(s"%$term%")))
    
    val pagedQuery = query
      .sortBy(_.name)
      .drop(page * pageSize)
      .take(pageSize)
    
    for {
      rows <- db.run(pagedQuery.result)
      totalCount <- db.run(query.length.result)
    } yield {
      val aggregates = rows.flatMap(rowToAggregate).toList
      PagedResult(aggregates, page, pageSize, totalCount.toLong)
    }
  }
}
EOF

echo -e "${GREEN}SlickMaltReadRepository autonome créé${NC}"

# SlickMaltWriteRepository autonome
cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.MaltWriteRepository
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

@Singleton
class SlickMaltWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
extends MaltWriteRepository 
with HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  
  // Même définition de table que ReadRepository
  case class MaltRow(
    id: String,
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String],
    source: String,
    isActive: Boolean,
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  )

  class MaltTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[String]("id", O.PrimaryKey)
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
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  private val malts = TableQuery[MaltTable]
  
  private def aggregateToRow(aggregate: MaltAggregate): MaltRow = {
    MaltRow(
      id = aggregate.id.value,
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
      credibilityScore = aggregate.credibilityScore,
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version
    )
  }
  
  override def create(malt: MaltAggregate): Future[Unit] = {
    val row = aggregateToRow(malt)
    val action = malts += row
    
    db.run(action).map(_ => ())
  }
  
  override def update(malt: MaltAggregate): Future[Unit] = {
    val row = aggregateToRow(malt)
    val action = malts.filter(_.id === malt.id.value).update(row)
    
    db.run(action).map(_ => ())
  }
  
  override def delete(id: MaltId): Future[Unit] = {
    val action = malts.filter(_.id === id.value).delete
    
    db.run(action).map(_ => ())
  }
}
EOF

echo -e "${GREEN}SlickMaltWriteRepository autonome créé${NC}"

# =============================================================================
# TEST DE COMPILATION
# =============================================================================

echo ""
echo -e "${BLUE}Test de compilation...${NC}"

if sbt compile > /tmp/access_privileges_fix.log 2>&1; then
    echo -e "${GREEN}COMPILATION RÉUSSIE !${NC}"
    echo ""
    echo -e "${GREEN}Corrections appliquées avec succès${NC}"
    echo "• Visibilité profile corrigée"
    echo "• Module Guice avec Singletons"
    echo "• Repositories autonomes créés"
    echo ""
    echo -e "${BLUE}Prochaine étape : sbt run${NC}"
    
else
    echo -e "${RED}Erreurs persistantes${NC}"
    echo ""
    grep "error" /tmp/access_privileges_fix.log | head -5
    echo ""
    echo -e "${YELLOW}Log : /tmp/access_privileges_fix.log${NC}"
fi

echo ""
echo -e "${GREEN}Correction privilèges d'accès terminée${NC}"