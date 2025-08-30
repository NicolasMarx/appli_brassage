package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

import repositories.read.HopReadRepository
import repositories.read.{HopExtrasReadRepository, HopExtras}
import domain.hops._
import domain.common.Range

final case class HopDto(
                         id: String,
                         name: String,
                         origin: Option[String],
                         hopType: Option[String],
                         alphaMin: Option[BigDecimal],
                         alphaMax: Option[BigDecimal],
                         betaMin: Option[BigDecimal],
                         betaMax: Option[BigDecimal],
                         oilsTotalMl100gMin: Option[BigDecimal],
                         oilsTotalMl100gMax: Option[BigDecimal],
                         myrceneMin: Option[BigDecimal],
                         myrceneMax: Option[BigDecimal],
                         humuleneMin: Option[BigDecimal],
                         humuleneMax: Option[BigDecimal],
                         caryophylleneMin: Option[BigDecimal],
                         caryophylleneMax: Option[BigDecimal],
                         farneseneMin: Option[BigDecimal],
                         farneseneMax: Option[BigDecimal],
                         aromas: List[String],
                         notes: Option[String],
                         description: Option[String],
                         beerStyles: List[String],
                         substitutes: List[String]
                       )
object HopDto {
  implicit val writes: OWrites[HopDto] = OWrites[HopDto] { h =>
    Json.obj(
      "id" -> h.id,
      "name" -> h.name,
      "origin" -> h.origin,
      "hopType" -> h.hopType,
      "alphaMin" -> h.alphaMin,
      "alphaMax" -> h.alphaMax,
      "betaMin" -> h.betaMin,
      "betaMax" -> h.betaMax,
      "oilsTotalMl100gMin" -> h.oilsTotalMl100gMin,
      "oilsTotalMl100gMax" -> h.oilsTotalMl100gMax,
      "myrceneMin" -> h.myrceneMin,
      "myrceneMax" -> h.myrceneMax,
      "humuleneMin" -> h.humuleneMin,
      "humuleneMax" -> h.humuleneMax,
      "caryophylleneMin" -> h.caryophylleneMin,
      "caryophylleneMax" -> h.caryophylleneMax,
      "farneseneMin" -> h.farneseneMin,
      "farneseneMax" -> h.farneseneMax,
      "aromas" -> h.aromas,
      "notes" -> h.notes,
      "description" -> h.description,
      "beerStyles" -> h.beerStyles,
      "substitutes" -> h.substitutes
    )
  }
}

@Singleton
class HopsV2Controller @Inject()(
                                  cc: ControllerComponents,
                                  hopRepo: HopReadRepository,
                                  extrasRepo: HopExtrasReadRepository
                                )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def rMin(r: Range) = r.min
  private def rMax(r: Range) = r.max

  private def toDto(h: Hop, ex: HopExtras): HopDto = {
    val aromasList = h.aromas
    val aromasText = if (aromasList.nonEmpty) Some(aromasList.mkString(", ")) else None
    HopDto(
      id = h.id.value,
      name = h.name.value,
      origin = Some(h.country.value),
      hopType = ex.hopType,
      alphaMin = rMin(h.alphaAcid),
      alphaMax = rMax(h.alphaAcid),
      betaMin = rMin(h.betaAcid),
      betaMax = rMax(h.betaAcid),
      oilsTotalMl100gMin = rMin(h.totalOilsMlPer100g),
      oilsTotalMl100gMax = rMax(h.totalOilsMlPer100g),
      myrceneMin = rMin(h.myrcene),
      myrceneMax = rMax(h.myrcene),
      humuleneMin = rMin(h.humulene),
      humuleneMax = rMax(h.humulene),
      caryophylleneMin = rMin(h.caryophyllene),
      caryophylleneMax = rMax(h.caryophyllene),
      farneseneMin = rMin(h.farnesene),
      farneseneMax = rMax(h.farnesene),
      aromas = aromasList,
      notes = aromasText,
      description = ex.description,
      beerStyles = ex.beerStyles,
      substitutes = ex.substitutes
    )
  }

  def list(limit: Int) = Action.async {
    hopRepo.all().flatMap { hs =>
      val take = hs.take(limit)
      extrasRepo.findByHopIds(take.map(_.id)).map { emap =>
        val dtos = take.map(h => toDto(h, emap.getOrElse(h.id, HopExtras(None, None, Nil, Nil))))
        Ok(Json.toJson(dtos))
      }
    }
  }
  def listDefault = list(1000)
}
