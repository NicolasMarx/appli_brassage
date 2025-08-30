// app/domain/hops/model/HopOrigin.scala
package domain.hops.model

import play.api.libs.json._

final case class HopOrigin private (code: String, name: String, region: String) {
  override def toString: String = s"$name ($code)"

  def isNoble: Boolean = HopOrigin.NobleOrigins.contains(code)
  def isNewWorld: Boolean = HopOrigin.NewWorldOrigins.contains(code)
  def isTraditional: Boolean = HopOrigin.TraditionalOrigins.contains(code)
}

object HopOrigin {
  // Origines nobles traditionnelles
  val NobleOrigins = Set("DE", "CZ", "BE")

  // Nouveaux pays producteurs
  val NewWorldOrigins = Set("US", "AU", "NZ", "CA")

  // Origines traditionnelles européennes
  val TraditionalOrigins = Set("DE", "CZ", "BE", "UK", "FR")

  // Origines prédéfinies
  val US = new HopOrigin("US", "États-Unis", "Amérique du Nord")
  val DE = new HopOrigin("DE", "Allemagne", "Europe")
  val CZ = new HopOrigin("CZ", "République Tchèque", "Europe")
  val UK = new HopOrigin("UK", "Royaume-Uni", "Europe")
  val NZ = new HopOrigin("NZ", "Nouvelle-Zélande", "Océanie")
  val AU = new HopOrigin("AU", "Australie", "Océanie")
  val BE = new HopOrigin("BE", "Belgique", "Europe")
  val FR = new HopOrigin("FR", "France", "Europe")
  val CA = new HopOrigin("CA", "Canada", "Amérique du Nord")
  val JP = new HopOrigin("JP", "Japon", "Asie")

  val all: Map[String, HopOrigin] = Map(
    "US" -> US, "DE" -> DE, "CZ" -> CZ, "UK" -> UK, "NZ" -> NZ,
    "AU" -> AU, "BE" -> BE, "FR" -> FR, "CA" -> CA, "JP" -> JP
  )

  def apply(code: String): Either[String, HopOrigin] = {
    val upperCode = code.trim.toUpperCase
    all.get(upperCode) match {
      case Some(origin) => Right(origin)
      case None => Left(s"Origine houblon invalide: '$code'. Origines valides: ${all.keys.mkString(", ")}")
    }
  }

  def fromCode(code: String): Option[HopOrigin] = all.get(code.trim.toUpperCase)

  def fromString(value: String): HopOrigin = {
    apply(value) match {
      case Right(origin) => origin
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }

  // Groupement par caractéristiques
  def getNobleOrigins: List[HopOrigin] = all.values.filter(_.isNoble).toList.sortBy(_.name)
  def getNewWorldOrigins: List[HopOrigin] = all.values.filter(_.isNewWorld).toList.sortBy(_.name)
  def getTraditionalOrigins: List[HopOrigin] = all.values.filter(_.isTraditional).toList.sortBy(_.name)

  implicit val hopOriginFormat: Format[HopOrigin] = new Format[HopOrigin] {
    def reads(json: JsValue): JsResult[HopOrigin] = {
      json.validate[String].flatMap { code =>
        HopOrigin(code) match {
          case Right(origin) => JsSuccess(origin)
          case Left(error) => JsError(error)
        }
      }
    }

    def writes(origin: HopOrigin): JsValue = JsString(origin.code)
  }
}