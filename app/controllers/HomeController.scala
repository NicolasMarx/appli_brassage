// app/controllers/HomeController.scala
package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * Contrôleur Home minimal pour Phase 1
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("Application DDD/CQRS Phase 1 - Fondations en cours")
  }
}