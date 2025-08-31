package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import domain.malts.repositories.MaltReadRepository

@Singleton
class AdminMaltsController @Inject()(
  val controllerComponents: ControllerComponents,
  maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) extends BaseController {

  /**
   * Liste tous les malts pour l'admin
   */
  def getAllMalts(page: Int, pageSize: Int, activeOnly: Boolean): Action[AnyContent] = Action.async {
    println(s"🔍 AdminMaltsController.getAllMalts: page=$page, pageSize=$pageSize, activeOnly=$activeOnly")
    
    for {
      malts <- maltReadRepository.findAll(page, pageSize, activeOnly)
      totalCount <- maltReadRepository.count(activeOnly)
    } yield {
      println(s"   Récupéré ${malts.length} malts, total: $totalCount")
      
      val maltsJson = malts.map { malt =>
        Json.obj(
          "id" -> malt.id.toString,
          "name" -> malt.name.value,
          "maltType" -> malt.maltType.name,
          "ebcColor" -> malt.ebcColor.value,
          "extractionRate" -> malt.extractionRate.value,
          "diastaticPower" -> malt.diastaticPower.value,
          "originCode" -> malt.originCode,
          "description" -> malt.description,
          "flavorProfiles" -> malt.flavorProfiles,
          "source" -> malt.source.name,
          "isActive" -> malt.isActive,
          "credibilityScore" -> malt.credibilityScore,
          "createdAt" -> malt.createdAt.toString,
          "updatedAt" -> malt.updatedAt.toString,
          "version" -> malt.version
        )
      }

      Ok(Json.obj(
        "malts" -> maltsJson,
        "totalCount" -> totalCount,
        "page" -> page,
        "pageSize" -> pageSize,
        "hasMore" -> (malts.length == pageSize)
      ))
    }
  }

  /**
   * Route par défaut compatible avec l'ancienne API
   */
  def getAllMaltsDefault: Action[AnyContent] = getAllMalts(0, 20, false)

  /**
   * Recherche de malts
   */
  def searchMalts(query: String, page: Int, pageSize: Int): Action[AnyContent] = Action.async {
    maltReadRepository.search(query, page, pageSize).map { malts =>
      val maltsJson = malts.map { malt =>
        Json.obj(
          "id" -> malt.id.toString,
          "name" -> malt.name.value,
          "maltType" -> malt.maltType.name,
          "ebcColor" -> malt.ebcColor.value,
          "extractionRate" -> malt.extractionRate.value
        )
      }
      
      Ok(Json.obj(
        "malts" -> maltsJson,
        "query" -> query,
        "totalResults" -> malts.length
      ))
    }
  }
}
