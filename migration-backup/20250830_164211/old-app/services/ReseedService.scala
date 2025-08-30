package services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

import play.api.{Configuration, Environment, Logger}
import play.api.libs.json._

import domain.common.Range
import domain.hops._
import domain.aromas.{Aroma, AromaId, AromaName}
import domain.beerstyles.{BeerStyle, BeerStyleId, BeerStyleName}
import repositories.write.{HopWriteRepository, AromaWriteRepository, BeerStyleWriteRepository}

object ReseedService {
  final case class Result(
                           hops: Int,
                           details: Int,
                           styles: Int,
                           subs: Int,
                           aromas: Int,
                           links: Int
                         )
  // 👉 sérialisation JSON pour l’API admin
  implicit val resultFormat: OFormat[Result] = Json.format[Result]

  // Déplacée ici pour éviter l’avertissement “outer reference”
  private[services] final case class CsvHop(
                                             id: String,
                                             name: String,
                                             country: String,
                                             alphaMin: String,
                                             alphaMax: String,
                                             betaMin: String,
                                             betaMax: String,
                                             cohumMin: String,
                                             cohumMax: String,
                                             oilsMin: String,
                                             oilsMax: String,
                                             myrceneMin: String,
                                             myrceneMax: String,
                                             humuleneMin: String,
                                             humuleneMax: String,
                                             caryoMin: String,
                                             caryoMax: String,
                                             farneseneMin: String,
                                             farneseneMax: String,
                                             hopType: String,
                                             description: String,
                                             beerStyles: String,
                                             substitutes: String,
                                             aromas: String
                                           )
}

@Singleton
final class ReseedService @Inject()(
                                     env: Environment,
                                     config: Configuration,
                                     hopWriteRepo: HopWriteRepository,
                                     aromaWriteRepo: AromaWriteRepository,
                                     beerStyleWriteRepo: BeerStyleWriteRepository
                                   )(implicit ec: ExecutionContext) {

  import ReseedService._

  private val logger = Logger(this.getClass)

  // ---------------- Helpers ----------------

  private def parseBigDecimalOpt(s: String): Option[BigDecimal] =
    Option(s).map(_.trim).filter(_.nonEmpty)
      .flatMap(x => Try(BigDecimal(x.replace(",", "."))).toOption)

  private def parseRange(min: String, max: String): Range =
    Range(parseBigDecimalOpt(min), parseBigDecimalOpt(max))

  private def slug(raw: String): String =
    Option(raw).getOrElse("").trim
      .toLowerCase
      .replaceAll("[àáâãäå]", "a")
      .replaceAll("[ç]", "c")
      .replaceAll("[èéêë]", "e")
      .replaceAll("[ìíîï]", "i")
      .replaceAll("[ñ]", "n")
      .replaceAll("[òóôõö]", "o")
      .replaceAll("[ùúûü]", "u")
      .replaceAll("[ýÿ]", "y")
      .replaceAll("\\s+", "-")
      .replaceAll("[^a-z0-9\\-]", "")
      .replaceAll("-+", "-")
      .stripPrefix("-").stripSuffix("-")

  /** Listes du CSV : on NE coupe PAS à la virgule. */
  private def splitList(s: String): List[String] =
    Option(s)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("""[|;/•·]| - | – | — """))
      .toList
      .flatten
      .map(_.trim)
      .filter(_.nonEmpty)

  // ---------------- Parsing CSV (classpath) ----------------

  private def readCsv(): List[CsvHop] = {
    val resourcePath = config.getOptional[String]("reseed.hops.path").getOrElse("reseed/hops.csv")

    val is = env.resourceAsStream(resourcePath)
      .getOrElse(throw new IllegalStateException(s"Ressource CSV introuvable: $resourcePath"))

    val src = Source.fromInputStream(is, "UTF-8")
    try {
      val all = src.getLines().toList
      if (all.isEmpty) return Nil

      val headerLine = all.head
      val semiCount  = headerLine.count(_ == ';')
      val commaCount = headerLine.count(_ == ',')
      val chosenSep  = if (semiCount >= commaCount) ';' else ','
      val sepRegex   =
        if (chosenSep == ';') """;(?![^"]*"(?:(?:[^"]*"){2})*[^"]*$)"""
        else                   """,(?![^"]*"(?:(?:[^"]*"){2})*[^"]*$)"""

      readCsvLike(all, sepRegex)
    } finally src.close()
  }

  // ---------------- Public API ----------------

  def reseed(): Future[Result] = reseedAll()

  /** Lecture fichier (config/Classpath) + import */
  def reseedAll(): Future[Result] = {
    val rows = readCsv()
    importAll(rows)
  }

  /** Point d’entrée pour l’upload admin (texte CSV brut) */
  def reseedFromCsvText(csv: String): Future[Result] = {
    val all = csv.split("\\r?\\n").toList
    if (all.isEmpty) Future.successful(Result(0,0,0,0,0,0))
    else {
      val headerLine = all.head
      val semiCount  = headerLine.count(_ == ';')
      val commaCount = headerLine.count(_ == ',')
      val chosenSep  = if (semiCount >= commaCount) ';' else ','
      val sepRegex   =
        if (chosenSep == ';') """;(?![^"]*"(?:(?:[^"]*"){2})*[^"]*$)"""
        else                   """,(?![^"]*"(?:(?:[^"]*"){2})*[^"]*$)"""
      val rows = readCsvLike(all, sepRegex)
      importAll(rows)
    }
  }

  // ---------------- Core import ----------------

  private def hopIdOf(c: CsvHop): HopId = {
    val raw = Option(c.id).map(_.trim).getOrElse("")
    val use = if (raw.nonEmpty) raw else c.name
    HopId(slug(use))
  }

  private def toDomainHop(c: CsvHop): Hop =
    Hop(
      id                 = hopIdOf(c),
      name               = HopName(c.name.trim),
      country            = Country(c.country.trim.toUpperCase),
      alphaAcid          = parseRange(c.alphaMin, c.alphaMax),
      betaAcid           = parseRange(c.betaMin,  c.betaMax),
      cohumulone         = parseRange(c.cohumMin, c.cohumMax),
      totalOilsMlPer100g = parseRange(c.oilsMin,  c.oilsMax),
      myrcene            = parseRange(c.myrceneMin, c.myrceneMax),
      humulene           = parseRange(c.humuleneMin, c.humuleneMax),
      caryophyllene      = parseRange(c.caryoMin, c.caryoMax),
      farnesene          = parseRange(c.farneseneMin, c.farneseneMax),
      aromas             = Nil // jointure read-side
    )

  private def toDetail(c: CsvHop): HopDetail =
    HopDetail(
      hopId       = hopIdOf(c),
      description = Option(c.description).map(_.trim).filter(_.nonEmpty),
      hopType     = Option(c.hopType).map(_.trim).filter(_.nonEmpty)
    )

  private def stylesOf(c: CsvHop): List[(HopId, String)] =
    splitList(c.beerStyles).map(s => (hopIdOf(c), slug(s)))

  private def substitutesOf(c: CsvHop): List[(HopId, String)] =
    splitList(c.substitutes).map(s => (hopIdOf(c), s))

  private def aromasOf(c: CsvHop): List[(HopId, String)] =
    splitList(c.aromas).map(a => (hopIdOf(c), slug(a)))

  private def importAll(rows: List[CsvHop]): Future[Result] = {
    val uniqRows = rows.groupBy(h => hopIdOf(h).value).values.map(_.head).toList

    val domainHops  = uniqRows.map(toDomainHop)
    val hopDetails  = uniqRows.map(toDetail)

    val styleIdToName: Map[String,String] =
      uniqRows.flatMap(_.beerStyles match {
        case s if s == null => Nil
        case s => splitList(s).map(raw => slug(raw) -> raw.trim)
      }).groupBy(_._1).view.mapValues(_.head._2).toMap

    val beerStyleEntities: Seq[BeerStyle] =
      styleIdToName.toSeq.sortBy(_._1).map { case (id, name) =>
        BeerStyle(BeerStyleId(id), BeerStyleName(name))
      }

    val hopStylePairs: Seq[(HopId, String)] =
      uniqRows.flatMap(stylesOf)

    val aromaIdToName: Map[String, String] =
      uniqRows.flatMap(_.aromas match {
        case s if s == null => Nil
        case s => splitList(s).map(raw => slug(raw) -> raw.trim)
      }).groupBy(_._1).view.mapValues(_.head._2).toMap

    val aromaEntities: Seq[Aroma] =
      aromaIdToName.toSeq.sortBy(_._1).map { case (id, name) =>
        val display = if (name.nonEmpty) name else id.replace('-', ' ').capitalize
        Aroma(AromaId(id), AromaName(display))
      }

    val hopAromaPairs: Seq[(HopId, String)] =
      uniqRows.flatMap(aromasOf)

    val substitutes = uniqRows.flatMap(substitutesOf)

    val nH = domainHops.distinct.size
    val nD = hopDetails.distinct.size
    val nS = beerStyleEntities.distinct.size
    val nU = substitutes.distinct.size
    val nA = aromaEntities.distinct.size
    val nL = (hopAromaPairs ++ hopStylePairs).distinct.size

    for {
      _ <- hopWriteRepo.replaceAll(domainHops)
      _ <- hopWriteRepo.replaceDetails(hopDetails)

      _ <- beerStyleWriteRepo.replaceAll(beerStyleEntities)
      _ <- hopWriteRepo.replaceBeerStyles(hopStylePairs)

      _ <- aromaWriteRepo.replaceAll(aromaEntities)
      _ <- hopWriteRepo.replaceAromas(hopAromaPairs)

      _ <- hopWriteRepo.replaceSubstitutes(substitutes)
    } yield Result(hops = nH, details = nD, styles = nS, subs = nU, aromas = nA, links = nL)
  }

  // ------------ parsing "like readCsv" depuis une liste de lignes ------------
  private def readCsvLike(all: List[String], sepRegex: String): List[CsvHop] = {
    if (all.isEmpty) return Nil

    val headers = all.head

    import java.text.Normalizer
    def norm(s: String): String =
      Normalizer.normalize(Option(s).getOrElse("").toLowerCase, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .replaceAll("[^a-z0-9]+", " ")
        .trim

    val headersArr  = headers.split(sepRegex, -1).map(_.trim)
    val headersNorm = headersArr.map(norm)

    def indexFor(possible: List[String], default: Int = -1, containsOk: Boolean = false): Int = {
      val candidates = possible.map(norm)
      val exact = headersNorm.indexWhere(h => candidates.contains(h))
      if (exact >= 0) exact
      else if (containsOk) headersNorm.indexWhere(h => candidates.exists(c => h.contains(c)))
      else default
    }

    val idxId          = indexFor(List("id","slug","identifiant"), -1, containsOk = false)
    val idxName        = indexFor(List("nom","name"), 0)
    val idxCountry     = indexFor(List("origine","country","pays"), 1)
    val idxHopType     = indexFor(List("type de houblon","hop_type","usage","type"), 2)

    val idxAlphaMin    = indexFor(List("acide alpha min %","acide alpha min","alpha_min"), 3)
    val idxAlphaMax    = indexFor(List("acide alpha max %","acide alpha max","alpha_max"), 4)
    val idxBetaMin     = indexFor(List("acide beta min %","beta min","beta_min"), 5)
    val idxBetaMax     = indexFor(List("acide beta max %","beta max","beta_max"), 6)
    val idxCohuMin     = indexFor(List("cohumulone min %","cohumulone min","cohumulone_min","cohu_min"), 7)
    val idxCohuMax     = indexFor(List("cohumulone max %","cohumulone max","cohumulone_max","cohu_max"), 8)
    val idxOilsMin     = indexFor(List("total des huiles min ml/100g","oils total ml/100g min","oils_total_ml_100g_min","huiles totales min ml/100g"), 9)
    val idxOilsMax     = indexFor(List("total des huiles max ml/100g","oils total ml/100g max","oils_total_ml_100g_max","huiles totales max ml/100g"), 10)
    val idxMyrMin      = indexFor(List("myrcene min %","myrcene min","myrcene_min","myrcène min %"), 11)
    val idxMyrMax      = indexFor(List("myrcene max %","myrcene max","myrcene_max","myrcène max %"), 12)
    val idxHumMin      = indexFor(List("humulene min %","humulene min","humulene_min","humulène min %"), 13)
    val idxHumMax      = indexFor(List("humulene max %","humulene max","humulene_max","humulène max %"), 14)
    val idxCaryoMin    = indexFor(List("caryophyllene min %","caryophyllene min","caryophyllene_min","caryophyllène min %"), 15)
    val idxCaryoMax    = indexFor(List("caryophyllene max %","caryophyllene max","caryophyllene_max","caryophyllène max %"), 16)
    val idxFarnMin     = indexFor(List("farnesene min %","farnesene min","farnesene_min","farnesène min %"), 17)
    val idxFarnMax     = indexFor(List("farnesene max %","farnesene max","farnesene_max","farnesène max %"), 18)

    val idxBeerStyles  = indexFor(List("types de biere","types de bière","styles de biere","styles de bière","beer_styles","styles","styles_biere"), 19)
    val idxAromas      = indexFor(List("gout et odeur","goût et odeur","aromes","arômes","aroma","aromas","arômes et odeur","aromes et odeur"), 20)
    val idxDescription = indexFor(List("description","notes"), 21)
    val idxSubs        = indexFor(List("variete de houblon alternative","variété de houblon alternative","substitutes","equivalents","substitut"), 22)

    def at(cols: Array[String], i: Int): String = if (i >= 0 && i < cols.length) cols(i) else ""

    all.tail.flatMap { line =>
      val cols = line.split(sepRegex, -1).map(_.trim.stripPrefix("\uFEFF"))
      val name = at(cols, idxName)
      if (name.isEmpty) None
      else Some(CsvHop(
        id           = at(cols, idxId),
        name         = name,
        country      = at(cols, idxCountry),
        alphaMin     = at(cols, idxAlphaMin),
        alphaMax     = at(cols, idxAlphaMax),
        betaMin      = at(cols, idxBetaMin),
        betaMax      = at(cols, idxBetaMax),
        cohumMin     = at(cols, idxCohuMin),
        cohumMax     = at(cols, idxCohuMax),
        oilsMin      = at(cols, idxOilsMin),
        oilsMax      = at(cols, idxOilsMax),
        myrceneMin   = at(cols, idxMyrMin),
        myrceneMax   = at(cols, idxMyrMax),
        humuleneMin  = at(cols, idxHumMin),
        humuleneMax  = at(cols, idxHumMax),
        caryoMin     = at(cols, idxCaryoMin),
        caryoMax     = at(cols, idxCaryoMax),
        farneseneMin = at(cols, idxFarnMin),
        farneseneMax = at(cols, idxFarnMax),
        hopType      = at(cols, idxHopType),
        description  = at(cols, idxDescription),
        beerStyles   = at(cols, idxBeerStyles),
        substitutes  = at(cols, idxSubs),
        aromas       = at(cols, idxAromas)
      ))
    }
  }
}
