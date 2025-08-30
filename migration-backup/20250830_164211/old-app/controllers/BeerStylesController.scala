package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import application.queries.{ListBeerStylesHandler, ListBeerStylesQuery}
import domain.beerstyles.{BeerStyle, BeerStyleId, BeerStyleName}

@Singleton
class BeerStylesController @Inject()(
                                      cc: ControllerComponents,
                                      listStyles: ListBeerStylesHandler
                                    )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val beerStyleIdFmt: Format[BeerStyleId] = Json.valueFormat[BeerStyleId]
  implicit val beerStyleNameFmt: Format[BeerStyleName] = Json.valueFormat[BeerStyleName]
  implicit val beerStyleFmt: OFormat[BeerStyle] = Json.format[BeerStyle]

  def list: Action[AnyContent] = Action.async {
    listStyles.handle(ListBeerStylesQuery()).map(ss => Ok(Json.toJson(ss)))
  }
}
