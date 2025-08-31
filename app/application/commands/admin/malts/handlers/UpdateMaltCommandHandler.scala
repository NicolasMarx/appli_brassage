package application.commands.admin.malts.handlers

import domain.common.DomainError
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handler pour UpdateMaltCommand - Implémentation basique pour compilation
 */
@Singleton
class UpdateMaltCommandHandler @Inject()(
  // repositories seront injectés plus tard
)(implicit ec: ExecutionContext) {

  // Implémentation temporaire pour permettre la compilation
  def handle(command: Any): Future[Either[DomainError, String]] = {
    Future.successful(Right("updated-malt-id"))
  }
}
