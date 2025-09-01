package controllers.admin

import application.queries.admin.malts.{AdminMaltListQuery}
import application.queries.admin.malts.handlers.AdminMaltListQueryHandler
import interfaces.http.common.BaseController
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}

/**
 * Contrôleur admin pour la gestion des malts
 */
@Singleton
class AdminMaltsController @Inject()(
  cc: ControllerComponents,
  adminMaltListQueryHandler: AdminMaltListQueryHandler
)(implicit ec: ExecutionContext) extends BaseController(cc) {

  def list(page: Int = 0, size: Int = 20): Action[AnyContent] = Action.async {
    val query = AdminMaltListQuery(page, size)
    
    adminMaltListQueryHandler.handle(query).map { response =>
      Ok(Json.toJson(response))
    }
  }

  def create(): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.obj("message" -> "Create malt - TODO")))
  }

  def get(id: String): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.obj("message" -> s"Get malt $id - TODO")))
  }

  def update(id: String): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.obj("message" -> s"Update malt $id - TODO")))
  }

  def delete(id: String): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.obj("message" -> s"Delete malt $id - TODO")))
  }
}
