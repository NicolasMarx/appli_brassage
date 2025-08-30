package controllers

import javax.inject._
import play.api.mvc._
import repositories.read.HopReadRepository
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class FrontendController @Inject()(
                                          cc: ControllerComponents,
                                          hopRepo: HopReadRepository
                                        )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /** Page d'accueil publique avec SSR des houblons */
  def index: Action[AnyContent] = Action.async { implicit req =>
    hopRepo.all().map { hops =>
      Ok(views.html.index(hops))
    }
  }
}
