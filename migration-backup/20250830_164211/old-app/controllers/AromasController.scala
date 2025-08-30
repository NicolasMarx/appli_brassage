package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.ExecutionContext
import application.queries.{ListAromasHandler, ListAromasQuery}
import domain.aromas.{Aroma, AromaId, AromaName}

@Singleton
class AromasController @Inject()(
  cc: ControllerComponents,
  listAromas: ListAromasHandler
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val aromaIdFmt: Format[AromaId] = Json.valueFormat[AromaId]
  implicit val aromaNameFmt: Format[AromaName] = Json.valueFormat[AromaName]
  implicit val aromaFmt: OFormat[Aroma] = Json.format[Aroma]

  def list: Action[AnyContent] = Action.async {
    listAromas.handle(ListAromasQuery()).map(as => Ok(Json.toJson(as)))
  }
}
