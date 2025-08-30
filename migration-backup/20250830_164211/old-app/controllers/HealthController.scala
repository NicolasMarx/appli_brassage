package controllers

import play.api.mvc._
import javax.inject._

@Singleton
class HealthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def ping: Action[AnyContent] = Action { Ok("pong") }
}
