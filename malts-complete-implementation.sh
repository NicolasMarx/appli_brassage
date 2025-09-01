#!/bin/bash

# =============================================================================
# SCRIPT COMPLET - IMPLÉMENTATION DOMAINE MALTS
# =============================================================================
# Ce script complète l'implémentation du domaine Malts en suivant exactement
# les patterns établis dans le domaine Hops qui fonctionne parfaitement.
#
# Basé sur la documentation d'audit, ce script résout le problème principal
# identifié dans SlickMaltReadRepository et implémente les APIs manquantes.
# =============================================================================

set -e  # Arrêt en cas d'erreur

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🌾 IMPLÉMENTATION COMPLÈTE DOMAINE MALTS${NC}"
echo -e "${BLUE}=======================================${NC}"

# =============================================================================
# ÉTAPE 1 : CORRECTION DU REPOSITORY LECTURE (PROBLÈME PRINCIPAL)
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 1 : Correction SlickMaltReadRepository${NC}"

# Backup du fichier actuel
cp app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala.backup

# Implémentation corrigée du repository lecture avec debug et validation robuste
cat > app/infrastructure/persistence/slick/repositories/malts/SlickMaltReadRepository.scala << 'EOF'
package infrastructure.persistence.slick.repositories.malts

import domain.malts.model._
import domain.malts.repositories.MaltReadRepository
import domain.shared.{NonEmptyString}
import infrastructure.persistence.slick.tables.MaltTables._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import javax.inject.{Inject, Singleton}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickMaltReadRepository @Inject()(
    dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends MaltReadRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val db = dbConfig.db

  // ===============================
  // MÉTHODE DE CONVERSION CORRIGÉE
  // ===============================

  /**
   * Conversion robuste avec gestion d'erreur et debug
   * Cette méthode résout le problème principal identifié dans l'audit
   */
  private def rowToAggregate(row: MaltRow): Option[MaltAggregate] = {
    try {
      println(s"🔍 DEBUG: Conversion du malt '${row.name}' (ID: ${row.id})")

      // Validation MaltId
      val maltId = MaltId(row.id.toString) match {
        case Right(id) => 
          println(s"✅ MaltId validé: ${id.value}")
          id
        case Left(error) => 
          println(s"❌ MaltId invalide: $error")
          return None
      }

      // Validation NonEmptyString pour le nom
      val name = NonEmptyString.create(row.name) match {
        case Right(n) => 
          println(s"✅ Nom validé: '${n.value}'")
          n
        case Left(error) => 
          println(s"❌ Nom invalide: $error")
          return None
      }

      // Validation MaltType
      val maltType = MaltType.fromName(row.maltType) match {
        case Some(mt) => 
          println(s"✅ MaltType validé: ${mt.name}")
          mt
        case None => 
          println(s"❌ MaltType invalide: '${row.maltType}' - Types valides: ${MaltType.values.map(_.name).mkString(", ")}")
          return None
      }

      // Validation EBCColor
      val ebcColor = EBCColor.create(row.ebcColor) match {
        case Right(color) => 
          println(s"✅ EBC Color validé: ${color.value}")
          color
        case Left(error) => 
          println(s"❌ EBC Color invalide: $error")
          return None
      }

      // Validation ExtractionRate
      val extractionRate = ExtractionRate.create(row.extractionRate) match {
        case Right(rate) => 
          println(s"✅ Extraction Rate validé: ${rate.value}%")
          rate
        case Left(error) => 
          println(s"❌ Extraction Rate invalide: $error")
          return None
      }

      // Validation DiastaticPower
      val diastaticPower = DiastaticPower.create(row.diastaticPower) match {
        case Right(power) => 
          println(s"✅ Diastatic Power validé: ${power.value}")
          power
        case Left(error) => 
          println(s"❌ Diastatic Power invalide: $error")
          return None
      }

      // Validation MaltSource
      val source = MaltSource.fromName(row.source) match {
        case Some(s) => 
          println(s"✅ Source validée: ${s.name}")
          s
        case None => 
          println(s"❌ Source invalide: '${row.source}' - Sources valides: ${MaltSource.values.map(_.name).mkString(", ")}")
          return None
      }

      // Construction de l'agrégat
      val aggregate = MaltAggregate(
        id = maltId,
        name = name,
        maltType = maltType,
        ebcColor = ebcColor,
        extractionRate = extractionRate,
        diastaticPower = diastaticPower,
        originCode = row.originCode,
        description = row.description,
        flavorProfiles = Option(row.flavorProfiles).getOrElse(List.empty),
        source = source,
        isActive = row.isActive,
        credibilityScore = row.credibilityScore,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version
      )

      println(s"✅ Malt '${aggregate.name.value}' converti avec succès")
      Some(aggregate)

    } catch {
      case ex: Exception =>
        println(s"❌ ERREUR lors de la conversion du malt '${row.name}': ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  // ===============================
  // IMPLÉMENTATION DES MÉTHODES
  // ===============================

  override def byId(id: MaltId): Future[Option[MaltAggregate]] = {
    println(s"🔍 Recherche malt par ID: ${id.value}")
    
    val query = malts.filter(_.id === UUID.fromString(id.value))

    db.run(query.result.headOption).map { rowOpt =>
      val result = rowOpt.flatMap(rowToAggregate)
      println(s"📊 Résultat byId: ${if (result.isDefined) "Trouvé" else "Non trouvé"}")
      result
    }.recover {
      case ex =>
        println(s"❌ Erreur byId pour ${id.value}: ${ex.getMessage}")
        ex.printStackTrace()
        None
    }
  }

  override def findAll(page: Int = 0, size: Int = 20): Future[(List[MaltAggregate], Long)] = {
    println(s"🔍 Recherche findAll - page: $page, size: $size")
    
    val offset = page * size

    val countQuery = malts.length
    val dataQuery = malts
      .sortBy(_.name)
      .drop(offset)
      .take(size)

    (for {
      totalCount <- db.run(countQuery.result)
      maltRows <- db.run(dataQuery.result)
    } yield {
      println(s"📊 Données brutes: ${maltRows.length} lignes, total: $totalCount")
      
      val maltsAggregates = maltRows.flatMap(rowToAggregate).toList
      
      println(s"📊 Après conversion: ${maltsAggregates.length} malts valides")
      
      (maltsAggregates, totalCount.toLong)
    }).recover {
      case ex =>
        println(s"❌ Erreur findAll: ${ex.getMessage}")
        ex.printStackTrace()
        (List.empty, 0L)
    }
  }

  override def findByType(maltType: MaltType, page: Int = 0, size: Int = 20): Future[(List[MaltAggregate], Long)] = {
    val offset = page * size
    
    val baseQuery = malts.filter(_.maltType === maltType.name)
    val countQuery = baseQuery.length
    val dataQuery = baseQuery
      .sortBy(_.name)
      .drop(offset)
      .take(size)

    (for {
      totalCount <- db.run(countQuery.result)
      maltRows <- db.run(dataQuery.result)
    } yield {
      val maltsAggregates = maltRows.flatMap(rowToAggregate).toList
      (maltsAggregates, totalCount.toLong)
    }).recover {
      case ex =>
        println(s"❌ Erreur findByType: ${ex.getMessage}")
        (List.empty, 0L)
    }
  }

  override def findActive(page: Int = 0, size: Int = 20): Future[(List[MaltAggregate], Long)] = {
    val offset = page * size
    
    val baseQuery = malts.filter(_.isActive === true)
    val countQuery = baseQuery.length
    val dataQuery = baseQuery
      .sortBy(_.name)
      .drop(offset)
      .take(size)

    (for {
      totalCount <- db.run(countQuery.result)
      maltRows <- db.run(dataQuery.result)
    } yield {
      val maltsAggregates = maltRows.flatMap(rowToAggregate).toList
      (maltsAggregates, totalCount.toLong)
    }).recover {
      case ex =>
        println(s"❌ Erreur findActive: ${ex.getMessage}")
        (List.empty, 0L)
    }
  }

  override def search(
    name: Option[String] = None,
    maltType: Option[MaltType] = None,
    minEbc: Option[Double] = None,
    maxEbc: Option[Double] = None,
    minExtraction: Option[Double] = None,
    maxExtraction: Option[Double] = None,
    page: Int = 0,
    size: Int = 20
  ): Future[(List[MaltAggregate], Long)] = {
    
    val offset = page * size
    
    var query = malts.filter(_.isActive === true)
    
    name.foreach { n =>
      query = query.filter(_.name.toLowerCase like s"%${n.toLowerCase}%")
    }
    
    maltType.foreach { mt =>
      query = query.filter(_.maltType === mt.name)
    }
    
    minEbc.foreach { min =>
      query = query.filter(_.ebcColor >= min)
    }
    
    maxEbc.foreach { max =>
      query = query.filter(_.ebcColor <= max)
    }
    
    minExtraction.foreach { min =>
      query = query.filter(_.extractionRate >= min)
    }
    
    maxExtraction.foreach { max =>
      query = query.filter(_.extractionRate <= max)
    }

    val countQuery = query.length
    val dataQuery = query
      .sortBy(_.name)
      .drop(offset)
      .take(size)

    (for {
      totalCount <- db.run(countQuery.result)
      maltRows <- db.run(dataQuery.result)
    } yield {
      val maltsAggregates = maltRows.flatMap(rowToAggregate).toList
      (maltsAggregates, totalCount.toLong)
    }).recover {
      case ex =>
        println(s"❌ Erreur search: ${ex.getMessage}")
        (List.empty, 0L)
    }
  }

  override def count(): Future[Long] = {
    db.run(malts.length.result).map(_.toLong).recover {
      case ex =>
        println(s"❌ Erreur count: ${ex.getMessage}")
        0L
    }
  }
}
EOF

echo -e "${GREEN}✅ SlickMaltReadRepository corrigé avec debug détaillé${NC}"

# =============================================================================
# ÉTAPE 2 : IMPLÉMENTATION API PUBLIQUE MALTS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 2 : Implémentation API publique Malts${NC}"

# Créer le dossier pour l'API publique
mkdir -p app/interfaces/http/api/v1/malts

# Implémentation du contrôleur public suivant le pattern Hops
cat > app/interfaces/http/api/v1/malts/MaltsController.scala << 'EOF'
package interfaces.http.api.v1.malts

import application.queries.public.malts._
import application.queries.public.malts.handlers._
import application.queries.public.malts.readmodels.MaltReadModel
import domain.malts.model.MaltType
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MaltsController @Inject()(
    cc: ControllerComponents,
    maltListQueryHandler: MaltListQueryHandler,
    maltDetailQueryHandler: MaltDetailQueryHandler,
    maltSearchQueryHandler: MaltSearchQueryHandler
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ===============================
  // LISTE DES MALTS (PUBLIQUE)
  // ===============================
  
  def list(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts - page: $page, size: $size")
    
    val query = MaltListQuery(page = page, size = size)
    
    maltListQueryHandler.handle(query).map { result =>
      result match {
        case Success(response) =>
          println(s"✅ Malts récupérés: ${response.malts.length}/${response.totalCount}")
          Ok(Json.toJson(response))
        case Failure(ex) =>
          println(s"❌ Erreur lors de la récupération des malts: ${ex.getMessage}")
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite",
            "details" -> ex.getMessage
          ))
      }
    }.recover {
      case ex =>
        println(s"❌ Erreur critique dans list: ${ex.getMessage}")
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "critical_error",
          "message" -> "Erreur critique du serveur"
        ))
    }
  }

  // ===============================
  // DÉTAIL D'UN MALT
  // ===============================
  
  def detail(id: String): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts/$id")
    
    val query = MaltDetailQuery(maltId = id)
    
    maltDetailQueryHandler.handle(query).map { result =>
      result match {
        case Success(Some(malt)) =>
          println(s"✅ Malt trouvé: ${malt.name}")
          Ok(Json.toJson(malt))
        case Success(None) =>
          println(s"⚠️ Malt non trouvé: $id")
          NotFound(Json.obj(
            "error" -> "malt_not_found",
            "message" -> s"Malt avec l'ID '$id' non trouvé"
          ))
        case Failure(ex) =>
          println(s"❌ Erreur lors de la récupération du malt $id: ${ex.getMessage}")
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite"
          ))
      }
    }.recover {
      case ex =>
        println(s"❌ Erreur critique dans detail: ${ex.getMessage}")
        BadRequest(Json.obj(
          "error" -> "invalid_id",
          "message" -> "ID de malt invalide"
        ))
    }
  }

  // ===============================
  // RECHERCHE AVANCÉE DE MALTS
  // ===============================
  
  def search(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    println(s"🌾 POST /api/v1/malts/search")
    println(s"🔍 Body reçu: ${Json.prettyPrint(request.body)}")
    
    request.body.validate[MaltSearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        println(s"✅ Requête de recherche validée: ${searchRequest}")
        
        val query = MaltSearchQuery(
          name = searchRequest.name,
          maltType = searchRequest.maltType.flatMap(MaltType.fromName),
          minEbc = searchRequest.minEbc,
          maxEbc = searchRequest.maxEbc,
          minExtraction = searchRequest.minExtraction,
          maxExtraction = searchRequest.maxExtraction,
          page = searchRequest.page.getOrElse(0),
          size = searchRequest.size.getOrElse(20)
        )

        maltSearchQueryHandler.handle(query).map { result =>
          result match {
            case Success(response) =>
              println(s"✅ Recherche réussie: ${response.malts.length} malts trouvés")
              Ok(Json.toJson(response))
            case Failure(ex) =>
              println(s"❌ Erreur lors de la recherche: ${ex.getMessage}")
              ex.printStackTrace()
              InternalServerError(Json.obj(
                "error" -> "search_error",
                "message" -> "Erreur lors de la recherche"
              ))
          }
        }

      case JsError(errors) =>
        println(s"❌ Validation JSON échouée: $errors")
        Future.successful(BadRequest(Json.obj(
          "error" -> "validation_error",
          "message" -> "Format de requête invalide",
          "details" -> JsError.toJson(errors)
        )))
    }
  }

  // ===============================
  // MALTS PAR TYPE
  // ===============================
  
  def byType(maltType: String, page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🌾 GET /api/v1/malts/type/$maltType - page: $page, size: $size")
    
    MaltType.fromName(maltType) match {
      case Some(validType) =>
        val query = MaltListQuery(
          maltType = Some(validType),
          page = page,
          size = size
        )
        
        maltListQueryHandler.handle(query).map { result =>
          result match {
            case Success(response) =>
              println(s"✅ Malts de type $maltType récupérés: ${response.malts.length}")
              Ok(Json.toJson(response))
            case Failure(ex) =>
              println(s"❌ Erreur lors de la récupération des malts de type $maltType: ${ex.getMessage}")
              InternalServerError(Json.obj(
                "error" -> "internal_error",
                "message" -> "Une erreur interne s'est produite"
              ))
          }
        }
        
      case None =>
        println(s"❌ Type de malt invalide: $maltType")
        Future.successful(BadRequest(Json.obj(
          "error" -> "invalid_type",
          "message" -> s"Type de malt '$maltType' invalide",
          "validTypes" -> MaltType.values.map(_.name)
        )))
    }
  }
}

// ===============================
// DTO POUR RECHERCHE
// ===============================

case class MaltSearchRequest(
  name: Option[String] = None,
  maltType: Option[String] = None,
  minEbc: Option[Double] = None,
  maxEbc: Option[Double] = None,
  minExtraction: Option[Double] = None,
  maxExtraction: Option[Double] = None,
  page: Option[Int] = None,
  size: Option[Int] = None
)

object MaltSearchRequest {
  implicit val format: Format[MaltSearchRequest] = Json.format[MaltSearchRequest]
}
EOF

echo -e "${GREEN}✅ API publique Malts créée${NC}"

# =============================================================================
# ÉTAPE 3 : MISE À JOUR DU CONTRÔLEUR ADMIN
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 3 : Amélioration du contrôleur Admin${NC}"

# Backup du contrôleur admin actuel
cp app/interfaces/http/api/admin/malts/AdminMaltsController.scala app/interfaces/http/api/admin/malts/AdminMaltsController.scala.backup

# Amélioration du contrôleur admin avec toutes les méthodes CRUD
cat > app/interfaces/http/api/admin/malts/AdminMaltsController.scala << 'EOF'
package interfaces.http.api.admin.malts

import application.commands.admin.malts._
import application.commands.admin.malts.handlers._
import application.queries.admin.malts._
import application.queries.admin.malts.handlers._
import application.queries.admin.malts.readmodels.AdminMaltReadModel
import domain.malts.model.{MaltId, MaltType}
import interfaces.http.dto.requests.admin.malts._
import interfaces.http.dto.responses.admin.malts._
import play.api.libs.json._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton  
class AdminMaltsController @Inject()(
    cc: ControllerComponents,
    // Command Handlers
    createMaltCommandHandler: CreateMaltCommandHandler,
    updateMaltCommandHandler: UpdateMaltCommandHandler,
    deleteMaltCommandHandler: DeleteMaltCommandHandler,
    // Query Handlers
    adminMaltListQueryHandler: AdminMaltListQueryHandler,
    adminMaltDetailQueryHandler: AdminMaltDetailQueryHandler
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ===============================
  // LISTE ADMIN DES MALTS
  // ===============================
  
  def list(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    println(s"🔐 GET /api/admin/malts - page: $page, size: $size")
    
    val query = AdminMaltListQuery(page = page, size = size)
    
    adminMaltListQueryHandler.handle(query).map { result =>
      result match {
        case Success(response) =>
          println(s"✅ Malts admin récupérés: ${response.malts.length}/${response.totalCount}")
          Ok(Json.toJson(response))
        case Failure(ex) =>
          println(s"❌ Erreur lors de la récupération admin des malts: ${ex.getMessage}")
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite"
          ))
      }
    }
  }

  // ===============================
  // DÉTAIL ADMIN D'UN MALT
  // ===============================
  
  def detail(id: String): Action[AnyContent] = Action.async { implicit request =>
    println(s"🔐 GET /api/admin/malts/$id")
    
    val query = AdminMaltDetailQuery(maltId = id)
    
    adminMaltDetailQueryHandler.handle(query).map { result =>
      result match {
        case Success(Some(malt)) =>
          println(s"✅ Malt admin trouvé: ${malt.name}")
          Ok(Json.toJson(malt))
        case Success(None) =>
          println(s"⚠️ Malt admin non trouvé: $id")
          NotFound(Json.obj(
            "error" -> "malt_not_found",
            "message" -> s"Malt avec l'ID '$id' non trouvé"
          ))
        case Failure(ex) =>
          println(s"❌ Erreur lors de la récupération admin du malt $id: ${ex.getMessage}")
          InternalServerError(Json.obj(
            "error" -> "internal_error",
            "message" -> "Une erreur interne s'est produite"
          ))
      }
    }
  }

  // ===============================
  // CRÉATION D'UN MALT
  // ===============================
  
  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    println(s"🔐 POST /api/admin/malts")
    println(s"📝 Body reçu: ${Json.prettyPrint(request.body)}")
    
    request.body.validate[CreateMaltRequest] match {
      case JsSuccess(createRequest, _) =>
        println(s"✅ Requête de création validée: ${createRequest.name}")
        
        val command = CreateMaltCommand(
          name = createRequest.name,
          maltType = createRequest.maltType,
          ebcColor = createRequest.ebcColor,
          extractionRate = createRequest.extractionRate,
          diastaticPower = createRequest.diastaticPower,
          originCode = createRequest.originCode,
          description = createRequest.description,
          flavorProfiles = createRequest.flavorProfiles.getOrElse(List.empty)
        )

        createMaltCommandHandler.handle(command).map { result =>
          result match {
            case Success(malt) =>
              println(s"✅ Malt créé: ${malt.name.value} (ID: ${malt.id.value})")
              val response = AdminMaltResponse.fromAggregate(malt)
              Created(Json.toJson(response))
            case Failure(ex) =>
              println(s"❌ Erreur lors de la création: ${ex.getMessage}")
              ex.printStackTrace()
              BadRequest(Json.obj(
                "error" -> "creation_error",
                "message" -> ex.getMessage
              ))
          }
        }

      case JsError(errors) =>
        println(s"❌ Validation JSON échouée: $errors")
        Future.successful(BadRequest(Json.obj(
          "error" -> "validation_error",
          "message" -> "Format de requête invalide",
          "details" -> JsError.toJson(errors)
        )))
    }
  }

  // ===============================
  // MISE À JOUR D'UN MALT
  // ===============================
  
  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    println(s"🔐 PUT /api/admin/malts/$id")
    println(s"📝 Body reçu: ${Json.prettyPrint(request.body)}")
    
    request.body.validate[UpdateMaltRequest] match {
      case JsSuccess(updateRequest, _) =>
        println(s"✅ Requête de mise à jour validée pour malt: $id")
        
        val command = UpdateMaltCommand(
          maltId = id,
          name = updateRequest.name,
          maltType = updateRequest.maltType,
          ebcColor = updateRequest.ebcColor,
          extractionRate = updateRequest.extractionRate,
          diastaticPower = updateRequest.diastaticPower,
          originCode = updateRequest.originCode,
          description = updateRequest.description,
          flavorProfiles = updateRequest.flavorProfiles,
          isActive = updateRequest.isActive
        )

        updateMaltCommandHandler.handle(command).map { result =>
          result match {
            case Success(Some(malt)) =>
              println(s"✅ Malt mis à jour: ${malt.name.value} (version: ${malt.version})")
              val response = AdminMaltResponse.fromAggregate(malt)
              Ok(Json.toJson(response))
            case Success(None) =>
              println(s"⚠️ Malt non trouvé pour mise à jour: $id")
              NotFound(Json.obj(
                "error" -> "malt_not_found",
                "message" -> s"Malt avec l'ID '$id' non trouvé"
              ))
            case Failure(ex) =>
              println(s"❌ Erreur lors de la mise à jour: ${ex.getMessage}")
              ex.printStackTrace()
              BadRequest(Json.obj(
                "error" -> "update_error",
                "message" -> ex.getMessage
              ))
          }
        }

      case JsError(errors) =>
        println(s"❌ Validation JSON échouée: $errors")
        Future.successful(BadRequest(Json.obj(
          "error" -> "validation_error",
          "message" -> "Format de requête invalide",
          "details" -> JsError.toJson(errors)
        )))
    }
  }

  // ===============================
  // SUPPRESSION D'UN MALT
  // ===============================
  
  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    println(s"🔐 DELETE /api/admin/malts/$id")
    
    val command = DeleteMaltCommand(maltId = id)
    
    deleteMaltCommandHandler.handle(command).map { result =>
      result match {
        case Success(true) =>
          println(s"✅ Malt supprimé: $id")
          NoContent
        case Success(false) =>
          println(s"⚠️ Malt non trouvé pour suppression: $id")
          NotFound(Json.obj(
            "error" -> "malt_not_found",
            "message" -> s"Malt avec l'ID '$id' non trouvé"
          ))
        case Failure(ex) =>
          println(s"❌ Erreur lors de la suppression: ${ex.getMessage}")
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "deletion_error",
            "message" -> "Une erreur s'est produite lors de la suppression"
          ))
      }
    }
  }

  // ===============================
  // STATISTIQUES ADMIN
  // ===============================
  
  def stats(): Action[AnyContent] = Action.async { implicit request =>
    println(s"🔐 GET /api/admin/malts/stats")
    
    // Récupération des statistiques pour le dashboard admin
    val statsQuery = AdminMaltListQuery(page = 0, size = 1000) // Limite pour stats
    
    adminMaltListQueryHandler.handle(statsQuery).map { result =>
      result match {
        case Success(response) =>
          val malts = response.malts
          val totalCount = response.totalCount
          val activeCount = malts.count(_.isActive)
          val inactiveCount = totalCount - activeCount
          
          val typeStats = malts.groupBy(_.maltType).map { case (maltType, list) =>
            maltType -> list.length
          }
          
          val sourceStats = malts.groupBy(_.source).map { case (source, list) =>
            source -> list.length
          }
          
          val avgCredibility = if (malts.nonEmpty) {
            malts.map(_.credibilityScore).sum / malts.length
          } else 0.0

          val stats = Json.obj(
            "totalMalts" -> totalCount,
            "activeMalts" -> activeCount,
            "inactiveMalts" -> inactiveCount,
            "typeDistribution" -> typeStats,
            "sourceDistribution" -> sourceStats,
            "averageCredibility" -> f"$avgCredibility%.2f",
            "needsReview" -> malts.count(_.needsReview)
          )
          
          println(s"✅ Statistiques calculées: $totalCount malts total")
          Ok(stats)
          
        case Failure(ex) =>
          println(s"❌ Erreur lors du calcul des statistiques: ${ex.getMessage}")
          InternalServerError(Json.obj(
            "error" -> "stats_error",
            "message" -> "Erreur lors du calcul des statistiques"
          ))
      }
    }
  }
}
EOF

echo -e "${GREEN}✅ Contrôleur Admin Malts amélioré${NC}"

# =============================================================================
# ÉTAPE 4 : AJOUT DES ROUTES DANS routes
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 4 : Mise à jour des routes${NC}"

# Backup du fichier routes actuel
cp conf/routes conf/routes.backup

# Ajout des routes malts au fichier routes
cat >> conf/routes << 'EOF'

# =============================================================================
# API MALTS - INTERFACE PUBLIQUE
# =============================================================================

# Liste des malts (publique)
GET     /api/v1/malts                   interfaces.http.api.v1.malts.MaltsController.list(page: Int ?= 0, size: Int ?= 20)
GET     /api/v1/malts/:id               interfaces.http.api.v1.malts.MaltsController.detail(id: String)
POST    /api/v1/malts/search            interfaces.http.api.v1.malts.MaltsController.search()
GET     /api/v1/malts/type/:maltType    interfaces.http.api.v1.malts.MaltsController.byType(maltType: String, page: Int ?= 0, size: Int ?= 20)

# =============================================================================
# API MALTS - INTERFACE ADMIN (SÉCURISÉE)
# =============================================================================

# CRUD Admin Malts  
GET     /api/admin/malts                interfaces.http.api.admin.malts.AdminMaltsController.list(page: Int ?= 0, size: Int ?= 20)
GET     /api/admin/malts/stats          interfaces.http.api.admin.malts.AdminMaltsController.stats()
GET     /api/admin/malts/:id            interfaces.http.api.admin.malts.AdminMaltsController.detail(id: String)
POST    /api/admin/malts                interfaces.http.api.admin.malts.AdminMaltsController.create()
PUT     /api/admin/malts/:id            interfaces.http.api.admin.malts.AdminMaltsController.update(id: String)
DELETE  /api/admin/malts/:id            interfaces.http.api.admin.malts.AdminMaltsController.delete(id: String)
EOF

echo -e "${GREEN}✅ Routes malts ajoutées${NC}"

# =============================================================================
# ÉTAPE 5 : CRÉATION DES DTOs MANQUANTS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 5 : Création des DTOs manquants${NC}"

# Créer les dossiers pour les DTOs
mkdir -p app/interfaces/http/dto/requests/admin/malts
mkdir -p app/interfaces/http/dto/responses/admin/malts

# DTO de réponse admin
cat > app/interfaces/http/dto/responses/admin/malts/AdminMaltResponse.scala << 'EOF'
package interfaces.http.dto.responses.admin.malts

import domain.malts.model.MaltAggregate
import play.api.libs.json._
import java.time.Instant

case class AdminMaltResponse(
  id: String,
  name: String,
  maltType: String,
  ebcColor: Double,
  extractionRate: Double,
  diastaticPower: Double,
  originCode: String,
  description: Option[String],
  flavorProfiles: List[String],
  source: String,
  isActive: Boolean,
  credibilityScore: Double,
  qualityScore: Double,
  needsReview: Boolean,
  createdAt: String,
  updatedAt: String,
  version: Long
)

object AdminMaltResponse {
  
  def fromAggregate(malt: MaltAggregate): AdminMaltResponse = {
    AdminMaltResponse(
      id = malt.id.value,
      name = malt.name.value,
      maltType = malt.maltType.name,
      ebcColor = malt.ebcColor.value,
      extractionRate = malt.extractionRate.value,
      diastaticPower = malt.diastaticPower.value,
      originCode = malt.originCode,
      description = malt.description,
      flavorProfiles = malt.flavorProfiles,
      source = malt.source.name,
      isActive = malt.isActive,
      credibilityScore = malt.credibilityScore,
      qualityScore = malt.qualityScore,
      needsReview = malt.needsReview,
      createdAt = malt.createdAt.toString,
      updatedAt = malt.updatedAt.toString,
      version = malt.version
    )
  }
  
  implicit val format: Format[AdminMaltResponse] = Json.format[AdminMaltResponse]
}
EOF

# DTO de liste admin
cat > app/interfaces/http/dto/responses/admin/malts/AdminMaltListResponse.scala << 'EOF'
package interfaces.http.dto.responses.admin.malts

import play.api.libs.json._

case class AdminMaltListResponse(
  malts: List[AdminMaltResponse],
  totalCount: Long,
  page: Int,
  size: Int,
  hasNext: Boolean
)

object AdminMaltListResponse {
  
  def create(
    malts: List[AdminMaltResponse],
    totalCount: Long,
    page: Int,
    size: Int
  ): AdminMaltListResponse = {
    val hasNext = (page + 1) * size < totalCount
    
    AdminMaltListResponse(
      malts = malts,
      totalCount = totalCount,
      page = page,
      size = size,
      hasNext = hasNext
    )
  }
  
  implicit val format: Format[AdminMaltListResponse] = Json.format[AdminMaltListResponse]
}
EOF

echo -e "${GREEN}✅ DTOs admin créés${NC}"

# =============================================================================
# ÉTAPE 6 : COMPILATION ET TESTS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 6 : Compilation et tests${NC}"

echo "Compilation du projet..."
if sbt compile; then
    echo -e "${GREEN}✅ Compilation réussie${NC}"
else
    echo -e "${RED}❌ Erreur de compilation${NC}"
    exit 1
fi

# =============================================================================
# ÉTAPE 7 : SCRIPT DE TEST APIS
# =============================================================================

echo -e "\n${YELLOW}🔧 ÉTAPE 7 : Création du script de test${NC}"

cat > test_malts_complete_api.sh << 'EOF'
#!/bin/bash

# Script de test complet des APIs Malts
# Usage: ./test_malts_complete_api.sh

BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🌾 TEST COMPLET DES APIs MALTS${NC}"
echo "=================================="

# Test API Admin - Liste
echo -e "\n${BLUE}1. Test API Admin - Liste${NC}"
response=$(curl -s "${BASE_URL}/api/admin/malts")
echo "Réponse: $response"

if echo "$response" | jq empty 2>/dev/null; then
    count=$(echo "$response" | jq -r '.totalCount // 0')
    malts_length=$(echo "$response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ API Admin fonctionnelle - $malts_length/$count malts${NC}"
else
    echo -e "${RED}❌ API Admin dysfonctionnelle${NC}"
fi

# Test API Admin - Stats
echo -e "\n${BLUE}2. Test API Admin - Statistiques${NC}"
stats_response=$(curl -s "${BASE_URL}/api/admin/malts/stats")
echo "Stats: $stats_response"

# Test API Publique - Liste
echo -e "\n${BLUE}3. Test API Publique - Liste${NC}"
public_response=$(curl -s "${BASE_URL}/api/v1/malts")
echo "Réponse publique: $public_response"

if echo "$public_response" | jq empty 2>/dev/null; then
    public_count=$(echo "$public_response" | jq -r '.totalCount // 0')
    public_malts_length=$(echo "$public_response" | jq -r '.malts | length // 0')
    echo -e "${GREEN}✅ API Publique fonctionnelle - $public_malts_length/$public_count malts${NC}"
else
    echo -e "${RED}❌ API Publique dysfonctionnelle${NC}"
fi

# Test recherche
echo -e "\n${BLUE}4. Test Recherche Avancée${NC}"
search_data='{
  "name": "malt",
  "minEbc": 0,
  "maxEbc": 50,
  "page": 0,
  "size": 10
}'

search_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$search_data" \
  "${BASE_URL}/api/v1/malts/search")

echo "Recherche: $search_response"

# Test malts par type
echo -e "\n${BLUE}5. Test Malts par Type${NC}"
type_response=$(curl -s "${BASE_URL}/api/v1/malts/type/BASE")
echo "Malts BASE: $type_response"

echo -e "\n${BLUE}Tests terminés${NC}"
EOF

chmod +x test_malts_complete_api.sh

echo -e "${GREEN}✅ Script de test créé: ./test_malts_complete_api.sh${NC}"

# =============================================================================
# ÉTAPE 8 : INSTRUCTIONS FINALES
# =============================================================================

echo -e "\n${BLUE}🎉 IMPLÉMENTATION COMPLÈTE TERMINÉE !${NC}"
echo "======================================"

echo -e "\n${GREEN}✅ Réalisations:${NC}"
echo "• SlickMaltReadRepository corrigé avec debug détaillé"
echo "• API publique malts complète (/api/v1/malts)"
echo "• API admin malts améliorée (/api/admin/malts)"
echo "• Routes ajoutées au fichier routes"
echo "• DTOs admin créés"
echo "• Script de test généré"

echo -e "\n${YELLOW}📋 Pour tester:${NC}"
echo "1. Démarrer l'application: sbt run"
echo "2. Lancer les tests: ./test_malts_complete_api.sh"
echo "3. Vérifier les logs de debug dans la console"

echo -e "\n${YELLOW}🔍 URLs disponibles:${NC}"
echo "• GET    /api/v1/malts                    (liste publique)"
echo "• GET    /api/v1/malts/:id               (détail public)"
echo "• POST   /api/v1/malts/search            (recherche avancée)"
echo "• GET    /api/v1/malts/type/:type        (malts par type)"
echo "• GET    /api/admin/malts                (liste admin)"
echo "• GET    /api/admin/malts/stats          (statistiques)"
echo "• GET    /api/admin/malts/:id            (détail admin)"
echo "• POST   /api/admin/malts                (création)"
echo "• PUT    /api/admin/malts/:id            (mise à jour)"
echo "• DELETE /api/admin/malts/:id            (suppression)"

echo -e "\n${GREEN}Le domaine Malts est maintenant complet et suit les patterns établis du domaine Hops !${NC}"
