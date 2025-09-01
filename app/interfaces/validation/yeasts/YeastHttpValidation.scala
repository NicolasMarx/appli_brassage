package interfaces.validation.yeasts

import akka.stream.Materializer
import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YeastValidationFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    // Pas de validation pour le moment - laisser passer toutes les requêtes
    nextFilter(request)
  }
}
