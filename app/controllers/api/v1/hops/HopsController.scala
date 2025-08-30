package controllers.api.v1.hops

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import interfaces.http.dto.responses.public._
import interfaces.http.dto.responses.public.HopResponse._
import java.time.Instant

@Singleton
class HopsController @Inject()(
                                val controllerComponents: ControllerComponents,
                                protected val dbConfigProvider: DatabaseConfigProvider
                              )(implicit ec: ExecutionContext) extends BaseController with HasDatabaseConfigProvider[PostgresProfile] {

  // Tables PostgreSQL directes
  case class HopRow(
                     id: String,
                     name: String,
                     alphaAcid: BigDecimal,
                     betaAcid: Option[BigDecimal],
                     originCode: String,
                     usage: String,
                     description: Option[String],
                     status: String,
                     createdAt: Instant
                   )

  case class OriginRow(id: String, name: String, region: Option[String])
  case class AromaProfileRow(id: String, name: String, category: Option[String])
  case class HopAromaRow(hopId: String, aromaId: String, intensity: Option[Int])

  class HopsTable(tag: Tag) extends Table[HopRow](tag, "hops") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def alphaAcid = column[BigDecimal]("alpha_acid")
    def betaAcid = column[Option[BigDecimal]]("beta_acid")
    def originCode = column[String]("origin_code")
    def usage = column[String]("usage")
    def description = column[Option[String]]("description")
    def status = column[String]("status")
    def createdAt = column[Instant]("created_at")

    def * = (id, name, alphaAcid, betaAcid, originCode, usage, description, status, createdAt) <> (HopRow.tupled, HopRow.unapply)
  }

  class OriginsTable(tag: Tag) extends Table[OriginRow](tag, "origins") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def region = column[Option[String]]("region")
    def * = (id, name, region) <> (OriginRow.tupled, OriginRow.unapply)
  }

  class AromaProfilesTable(tag: Tag) extends Table[AromaProfileRow](tag, "aroma_profiles") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def category = column[Option[String]]("category")
    def * = (id, name, category) <> (AromaProfileRow.tupled, AromaProfileRow.unapply)
  }

  class HopAromaProfilesTable(tag: Tag) extends Table[HopAromaRow](tag, "hop_aroma_profiles") {
    def hopId = column[String]("hop_id")
    def aromaId = column[String]("aroma_id")
    def intensity = column[Option[Int]]("intensity")
    def * = (hopId, aromaId, intensity) <> (HopAromaRow.tupled, HopAromaRow.unapply)
  }

  val hops = TableQuery[HopsTable]
  val origins = TableQuery[OriginsTable]
  val aromaProfiles = TableQuery[AromaProfilesTable]
  val hopAromaProfiles = TableQuery[HopAromaProfilesTable]

  def list(page: Int = 0, size: Int = 20) = Action.async { implicit request =>
    val offset = page * size

    val query = for {
      ((hop, origin), aromaData) <- hops
        .filter(_.status === "ACTIVE")
        .drop(offset)
        .take(size)
        .join(origins).on(_.originCode === _.id)
        .joinLeft(
          hopAromaProfiles
            .join(aromaProfiles).on(_.aromaId === _.id)
        ).on(_._1.id === _._1.hopId)
    } yield (hop, origin, aromaData.map(_._2), aromaData.map(_._1.intensity))

    val countQuery = hops.filter(_.status === "ACTIVE").length

    for {
      results <- db.run(query.result)
      totalCount <- db.run(countQuery.result)
    } yield {
      val groupedResults = results.groupBy { case (hop, origin, _, _) => (hop, origin) }

      val hopResponses = groupedResults.map { case ((hopRow, originRow), group) =>
        val aromas = group.flatMap { case (_, _, aromaOpt, intensityOpt) =>
          aromaOpt.map { aroma =>
            AromaProfileResponse(
              id = aroma.id,
              name = aroma.name,
              category = aroma.category.getOrElse("unknown"),
              intensity = intensityOpt.flatten
            )
          }
        }.toList.distinct

        HopResponse(
          id = hopRow.id,
          name = hopRow.name,
          alphaAcid = hopRow.alphaAcid.toDouble,
          betaAcid = hopRow.betaAcid.map(_.toDouble),
          origin = HopOriginResponse(
            code = originRow.id,
            name = originRow.name,
            region = originRow.region.getOrElse("Unknown"),
            isNoble = hopRow.usage == "NOBLE",
            isNewWorld = Set("US", "AU", "NZ", "CA").contains(originRow.id)
          ),
          usage = hopRow.usage,
          description = hopRow.description,
          aromaProfiles = aromas,
          createdAt = hopRow.createdAt
        )
      }.toList

      val response = HopListResponse(
        hops = hopResponses,
        totalCount = totalCount,
        page = page,
        pageSize = size,
        hasNext = (page + 1) * size < totalCount
      )

      Ok(Json.toJson(response))
    }
  }

  def detail(id: String) = Action.async { implicit request =>
    val query = for {
      ((hop, origin), aromaData) <- hops
        .filter(_.id === id)
        .join(origins).on(_.originCode === _.id)
        .joinLeft(
          hopAromaProfiles
            .join(aromaProfiles).on(_.aromaId === _.id)
        ).on(_._1.id === _._1.hopId)
    } yield (hop, origin, aromaData.map(_._2), aromaData.map(_._1.intensity))

    db.run(query.result).map { results =>
      if (results.isEmpty) {
        NotFound(Json.obj("error" -> "Houblon non trouvé"))
      } else {
        val (hopRow, originRow) = (results.head._1, results.head._2)
        val aromas = results.flatMap { case (_, _, aromaOpt, intensityOpt) =>
          aromaOpt.map { aroma =>
            AromaProfileResponse(
              id = aroma.id,
              name = aroma.name,
              category = aroma.category.getOrElse("unknown"),
              intensity = intensityOpt.flatten
            )
          }
        }.distinct

        val hopResponse = HopResponse(
          id = hopRow.id,
          name = hopRow.name,
          alphaAcid = hopRow.alphaAcid.toDouble,
          betaAcid = hopRow.betaAcid.map(_.toDouble),
          origin = HopOriginResponse(
            code = originRow.id,
            name = originRow.name,
            region = originRow.region.getOrElse("Unknown"),
            isNoble = hopRow.usage == "NOBLE",
            isNewWorld = Set("US", "AU", "NZ", "CA").contains(originRow.id)
          ),
          usage = hopRow.usage,
          description = hopRow.description,
          aromaProfiles = aromas.toList,
          createdAt = hopRow.createdAt
        )

        Ok(Json.toJson(hopResponse))
      }
    }
  }

  def search() = Action.async(parse.json) { implicit request =>
    val searchParams = extractSearchParams(request.body)

    var query = hops.filter(_.status === "ACTIVE")

    searchParams.name.foreach { name =>
      query = query.filter(_.name.toLowerCase.like(s"%${name.toLowerCase}%"))
    }

    searchParams.origin.foreach { origin =>
      query = query.filter(_.originCode === origin)
    }

    searchParams.usage.foreach { usage =>
      query = query.filter(_.usage === usage)
    }

    searchParams.minAlphaAcid.foreach { min =>
      query = query.filter(_.alphaAcid >= min)
    }

    searchParams.maxAlphaAcid.foreach { max =>
      query = query.filter(_.alphaAcid <= max)
    }

    val finalQuery = for {
      ((hop, origin), aromaData) <- query
        .join(origins).on(_.originCode === _.id)
        .joinLeft(
          hopAromaProfiles
            .join(aromaProfiles).on(_.aromaId === _.id)
        ).on(_._1.id === _._1.hopId)
    } yield (hop, origin, aromaData.map(_._2), aromaData.map(_._1.intensity))

    db.run(finalQuery.result).map { results =>
      val groupedResults = results.groupBy { case (hop, origin, _, _) => (hop, origin) }

      val hopResponses = groupedResults.map { case ((hopRow, originRow), group) =>
        val aromas = group.flatMap { case (_, _, aromaOpt, intensityOpt) =>
          aromaOpt.map { aroma =>
            AromaProfileResponse(
              id = aroma.id,
              name = aroma.name,
              category = aroma.category.getOrElse("unknown"),
              intensity = intensityOpt.flatten
            )
          }
        }.toList.distinct

        HopResponse(
          id = hopRow.id,
          name = hopRow.name,
          alphaAcid = hopRow.alphaAcid.toDouble,
          betaAcid = hopRow.betaAcid.map(_.toDouble),
          origin = HopOriginResponse(
            code = originRow.id,
            name = originRow.name,
            region = originRow.region.getOrElse("Unknown"),
            isNoble = hopRow.usage == "NOBLE",
            isNewWorld = Set("US", "AU", "NZ", "CA").contains(originRow.id)
          ),
          usage = hopRow.usage,
          description = hopRow.description,
          aromaProfiles = aromas,
          createdAt = hopRow.createdAt
        )
      }.toList

      val response = HopListResponse(
        hops = hopResponses,
        totalCount = hopResponses.length,
        page = 0,
        pageSize = hopResponses.length,
        hasNext = false
      )

      Ok(Json.toJson(response))
    }
  }

  private def extractSearchParams(json: JsValue): SearchParams = {
    SearchParams(
      name = (json \ "name").asOpt[String],
      origin = (json \ "origin").asOpt[String],
      usage = (json \ "usage").asOpt[String],
      minAlphaAcid = (json \ "minAlphaAcid").asOpt[Double].map(BigDecimal(_)),
      maxAlphaAcid = (json \ "maxAlphaAcid").asOpt[Double].map(BigDecimal(_)),
      aromaProfile = (json \ "aromaProfile").asOpt[String]
    )
  }
}

case class SearchParams(
                         name: Option[String] = None,
                         origin: Option[String] = None,
                         usage: Option[String] = None,
                         minAlphaAcid: Option[BigDecimal] = None,
                         maxAlphaAcid: Option[BigDecimal] = None,
                         aromaProfile: Option[String] = None
                       )