package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.ExecutionContext

import application.commands.{ReseedCommand, ReseedHandler}

@Singleton
class ReseedController @Inject()(
                                  cc: ControllerComponents,
                                  reseedHandler: ReseedHandler
                                )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val reseedResultFmt: OFormat[services.ReseedService.Result] =
    Json.format[services.ReseedService.Result]

  def reseed: Action[AnyContent] = Action.async {
    reseedHandler.handle(ReseedCommand()).map(r => Ok(Json.toJson(r)))
  }
}
