package infrastructure.db

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

/** Tables partagées entre read/write (Slick). */
object Tables {

  // -------- HOPS (maître)
  final case class HopRow(
                           id: String,
                           name: String,
                           country: String,
                           alphaMin: Option[BigDecimal], alphaMax: Option[BigDecimal],
                           betaMin: Option[BigDecimal],  betaMax: Option[BigDecimal],
                           cohumMin: Option[BigDecimal], cohumMax: Option[BigDecimal],
                           oilsMin: Option[BigDecimal],  oilsMax: Option[BigDecimal],
                           myrceneMin: Option[BigDecimal], myrceneMax: Option[BigDecimal],
                           humuleneMin: Option[BigDecimal], humuleneMax: Option[BigDecimal],
                           caryoMin: Option[BigDecimal],  caryoMax: Option[BigDecimal],
                           farneseneMin: Option[BigDecimal], farneseneMax: Option[BigDecimal]
                         )
  final class Hops(tag: Tag) extends Table[HopRow](tag, "hops") {
    def id    = column[String]("id", O.PrimaryKey)
    def name  = column[String]("name")
    def country = column[String]("country")
    def alphaMin = column[Option[BigDecimal]]("alpha_min")
    def alphaMax = column[Option[BigDecimal]]("alpha_max")
    def betaMin  = column[Option[BigDecimal]]("beta_min")
    def betaMax  = column[Option[BigDecimal]]("beta_max")
    def cohumMin = column[Option[BigDecimal]]("cohumulone_min")
    def cohumMax = column[Option[BigDecimal]]("cohumulone_max")
    def oilsMin  = column[Option[BigDecimal]]("oils_total_ml_100g_min")
    def oilsMax  = column[Option[BigDecimal]]("oils_total_ml_100g_max")
    def myrceneMin  = column[Option[BigDecimal]]("myrcene_min")
    def myrceneMax  = column[Option[BigDecimal]]("myrcene_max")
    def humuleneMin = column[Option[BigDecimal]]("humulene_min")
    def humuleneMax = column[Option[BigDecimal]]("humulene_max")
    def caryoMin    = column[Option[BigDecimal]]("caryophyllene_min")
    def caryoMax    = column[Option[BigDecimal]]("caryophyllene_max")
    def farneseneMin= column[Option[BigDecimal]]("farnesene_min")
    def farneseneMax= column[Option[BigDecimal]]("farnesene_max")

    def * =
      (id, name, country, alphaMin, alphaMax, betaMin, betaMax, cohumMin, cohumMax, oilsMin, oilsMax,
        myrceneMin, myrceneMax, humuleneMin, humuleneMax, caryoMin, caryoMax, farneseneMin, farneseneMax)
        .mapTo[HopRow]
  }

  // -------- HOP_DETAILS
  final case class HopDetailRow(hopId: String, description: Option[String], hopType: Option[String])
  final class HopDetails(tag: Tag) extends Table[HopDetailRow](tag, "hop_details") {
    def hopId = column[String]("hop_id")
    def description = column[Option[String]]("description")
    def hopType     = column[Option[String]]("hop_type")
    def pk = primaryKey("pk_hop_details", hopId)
    def * = (hopId, description, hopType).mapTo[HopDetailRow]
  }

  // -------- Substituts
  final case class HopSubstituteRow(hopId: String, name: String)
  final class HopSubstitutes(tag: Tag) extends Table[HopSubstituteRow](tag, "hop_substitutes") {
    def hopId = column[String]("hop_id")
    def name  = column[String]("name")
    def pk    = primaryKey("pk_hop_substitutes", (hopId, name))
    def * = (hopId, name).mapTo[HopSubstituteRow]
  }

  // -------- Aromas (dictionnaire) + liaison
  final case class AromaRow(id: String, name: String)
  final class Aromas(tag: Tag) extends Table[AromaRow](tag, "aromas") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def * = (id, name).mapTo[AromaRow]
  }

  final case class HopAromaRow(hopId: String, aromaId: String)
  final class HopAromas(tag: Tag) extends Table[HopAromaRow](tag, "hop_aromas") {
    def hopId  = column[String]("hop_id")
    def aromaId= column[String]("aroma_id")
    def pk     = primaryKey("pk_hop_aromas", (hopId, aromaId))
    def *      = (hopId, aromaId).mapTo[HopAromaRow]
  }

  // -------- Beer styles (dictionnaire) + liaison
  final case class BeerStyleRow(id: String, name: String)
  final class BeerStyles(tag: Tag) extends Table[BeerStyleRow](tag, "beer_styles") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def * = (id, name).mapTo[BeerStyleRow]
  }

  final case class HopBeerStyleRow(hopId: String, styleId: String)
  final class HopBeerStyles(tag: Tag) extends Table[HopBeerStyleRow](tag, "hop_beer_styles") {
    def hopId   = column[String]("hop_id")
    def styleId = column[String]("style_id")
    def pk      = primaryKey("pk_hop_beer_styles", (hopId, styleId))
    def *       = (hopId, styleId).mapTo[HopBeerStyleRow]
  }

  // -------- TableQuery (une seule instance partagée)
  val hops            = TableQuery[Hops]
  val hopDetails      = TableQuery[HopDetails]
  val hopSubstitutes  = TableQuery[HopSubstitutes]
  val aromas          = TableQuery[Aromas]
  val hopAromas       = TableQuery[HopAromas]
  val beerStyles      = TableQuery[BeerStyles]
  val hopBeerStyles   = TableQuery[HopBeerStyles]
}
