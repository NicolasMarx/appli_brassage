package application.yeasts.services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import application.yeasts.dtos._

@Singleton
class YeastApplicationService @Inject()(
  yeastDTOs: YeastDTOs
)(implicit ec: ExecutionContext) {

  def findYeasts(request: YeastSearchRequestDTO): Future[Either[List[String], YeastPageResponseDTO]] = {
    yeastDTOs.findYeasts(request)
  }

  def getYeastById(yeastId: java.util.UUID): Future[Option[YeastDetailResponseDTO]] = {
    yeastDTOs.getYeastById(yeastId)
  }

  def searchYeasts(term: String, limit: Option[Int] = None): Future[Either[String, List[YeastSummaryDTO]]] = {
    yeastDTOs.searchYeasts(term, limit)
  }
}
