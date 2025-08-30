package application.commands

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final case class ReseedCommand()

@Singleton
class ReseedHandler @Inject() (
                                svc: services.ReseedService  // 👈 FQCN
                              )(implicit ec: ExecutionContext) {

  def handle(cmd: ReseedCommand): Future[services.ReseedService.Result] =  // 👈 FQCN
    svc.reseed()
}
