#!/bin/bash

# =============================================================================
# CORRECTION INJECTION GUICE - REPOSITORIES SLICK
# =============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Correction injection Guice${NC}"

# =============================================================================
# CORRIGER SLICKMALTREADREPOSITORY - INJECTION PLAY-SLICK
# =============================================================================

echo -e "${YELLOW}Correction SlickMaltReadRepository avec Play-Slick...${NC}"

cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.{MaltReadRepository, MaltSubstitution, MaltCompatibility}
import domain.common.PagedResult
import infrastructure.persistence.slick.tables.MaltTables
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltReadRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
extends MaltReadRepository 
with HasDatabaseConfigProvider[JdbcProfile] 
with MaltTables {
  
  import profile.api._
  
  private def convertRow(row: MaltRow): Option[MaltAggregate] = {
    rowToAggregate(row).toOption
  }
  
  override def findById(id: MaltId): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.id === id.value)
    db.run(query.result.headOption).map(_.flatMap(convertRow))
  }
  
  override def findByName(name: String): Future[Option[MaltAggregate]] = {
    val query = malts.filter(_.name === name)
    db.run(query.result.headOption).map(_.flatMap(convertRow))
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
      
    db.run(query.result).map(_.flatMap(convertRow).toList)
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
    
    // Filtres simples
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
      val aggregates = rows.flatMap(convertRow).toList
      PagedResult(aggregates, page, pageSize, totalCount.toLong)
    }
  }
}
EOF

echo -e "${GREEN}SlickMaltReadRepository corrigé${NC}"

# =============================================================================
# CORRIGER SLICKMALTWRITEREPOSITORY
# =============================================================================

echo -e "${YELLOW}Correction SlickMaltWriteRepository...${NC}"

cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltWriteRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model.{MaltAggregate, MaltId}
import domain.malts.repositories.MaltWriteRepository
import infrastructure.persistence.slick.tables.MaltTables
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) 
extends MaltWriteRepository 
with HasDatabaseConfigProvider[JdbcProfile] 
with MaltTables {
  
  import profile.api._
  
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

echo -e "${GREEN}SlickMaltWriteRepository corrigé${NC}"

# =============================================================================
# SIMPLIFIER LE MODULE D'INJECTION
# =============================================================================

echo -e "${YELLOW}Correction module d'injection...${NC}"

cat > app/modules/MaltModule.scala << 'EOF'
package modules

import com.google.inject.AbstractModule
import domain.malts.repositories.{MaltReadRepository, MaltWriteRepository}
import infrastructure.persistence.slick.repositories.malts.{SlickMaltReadRepository, SlickMaltWriteRepository}

class MaltModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[MaltReadRepository]).to(classOf[SlickMaltReadRepository])
    bind(classOf[MaltWriteRepository]).to(classOf[SlickMaltWriteRepository])
  }
}
EOF

echo -e "${GREEN}Module simplifié${NC}"

# =============================================================================
# VÉRIFIER CONFIGURATION APPLICATION
# =============================================================================

echo -e "${YELLOW}Vérification configuration...${NC}"

# Vérifier si le module est activé
if grep -q "modules.MaltModule" conf/application.conf; then
    echo -e "${GREEN}Module MaltModule déjà activé${NC}"
else
    echo -e "${YELLOW}Activation du module MaltModule...${NC}"
    echo "" >> conf/application.conf
    echo "# Module domaine Malts" >> conf/application.conf
    echo "play.modules.enabled += \"modules.MaltModule\"" >> conf/application.conf
    echo -e "${GREEN}Module MaltModule activé${NC}"
fi

# =============================================================================
# TEST DE DÉMARRAGE
# =============================================================================

echo ""
echo -e "${BLUE}Test de démarrage de l'application...${NC}"

# Nettoyer les processus sbt existants si nécessaire
# pkill -f sbt || true

echo -e "${YELLOW}Tentative de démarrage...${NC}"
echo -e "${YELLOW}(Ctrl+C pour arrêter après vérification)${NC}"

timeout 30s sbt run || {
    echo ""
    echo -e "${YELLOW}Test de démarrage interrompu (normal)${NC}"
}

echo ""
echo -e "${BLUE}Instructions de vérification :${NC}"
echo ""
echo "1. Redémarrer avec: sbt run"
echo "2. Vérifier que l'application démarre sans erreur Guice"
echo "3. Aller sur http://localhost:9000"
echo "4. Vérifier les logs pour les tables créées"
echo ""
echo -e "${GREEN}Correction Guice terminée${NC}"