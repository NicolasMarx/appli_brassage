package controllers.admin

import play.api.mvc._
import play.api.libs.json._
import domain.malts.repositories.MaltReadRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton  
class AdminMaltsController @Inject()(
  val controllerComponents: ControllerComponents,
  maltReadRepository: MaltReadRepository
)(implicit ec: ExecutionContext) extends BaseController {

  def list(
    page: Int = 0,
    pageSize: Int = 20,
    maltType: Option[String] = None,
    status: Option[String] = None,
    source: Option[String] = None,
    minCredibility: Option[Int] = None,
    needsReview: Boolean = false,
    searchTerm: Option[String] = None,
    sortBy: String = "name",
    sortOrder: String = "asc"
  ): Action[AnyContent] = Action.async { implicit request =>
    
    for {
      malts <- maltReadRepository.findAll(page, pageSize, activeOnly = true)
      totalCount <- maltReadRepository.count(activeOnly = true)
    } yield {
      val maltJson = malts.map(malt => Json.obj(
        "id" -> malt.id.value,
        "name" -> malt.name.value,
        "maltType" -> malt.maltType.name,
        "ebcColor" -> malt.ebcColor.value,
        "extractionRate" -> malt.extractionRate.value,
        "isActive" -> malt.isActive
      ))
      
      Ok(Json.obj(
        "malts" -> maltJson,
        "pagination" -> Json.obj(
          "currentPage" -> page,
          "pageSize" -> pageSize,
          "totalCount" -> totalCount,
          "hasNext" -> ((page + 1) * pageSize < totalCount.toInt)
        )
      ))
    }
  }

  // Stubs pour les autres méthodes
  def create(): Action[AnyContent] = Action { 
    BadRequest(Json.obj("error" -> "Non implémenté"))
  }
  def detail(id: String, includeAuditLog: Boolean = false, includeSubstitutes: Boolean = true, 
             includeBeerStyles: Boolean = true, includeStatistics: Boolean = false): Action[AnyContent] = Action {
    NotImplemented
  }
  def update(id: String): Action[AnyContent] = Action { NotImplemented }
  def delete(id: String): Action[AnyContent] = Action { NotImplemented }
  def statistics(): Action[AnyContent] = Action { NotImplemented }
  def needsReview(page: Int = 0, pageSize: Int = 20, maxCredibility: Int = 70): Action[AnyContent] = Action { NotImplemented }
  def adjustCredibility(id: String): Action[AnyContent] = Action { NotImplemented }
  def batchImport(): Action[AnyContent] = Action { NotImplemented }
}
