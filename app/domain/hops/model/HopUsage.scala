// app/domain/hops/model/HopUsage.scala
package domain.hops.model

import play.api.libs.json._

sealed trait HopUsage {
  def name: String
  def description: String
  def typicalAdditionTiming: String
}

object HopUsage {
  case object Bittering extends HopUsage {
    val name = "BITTERING"
    val description = "Houblons à haute teneur en acides alpha, utilisés pour l'amertume"
    val typicalAdditionTiming = "60-90 minutes avant fin ébullition"
  }

  case object Aroma extends HopUsage {
    val name = "AROMA"
    val description = "Houblons à faible alpha, utilisés pour les arômes et saveurs"
    val typicalAdditionTiming = "0-15 minutes avant fin ébullition, ou dry hopping"
  }

  case object DualPurpose extends HopUsage {
    val name = "DUAL_PURPOSE"
    val description = "Houblons polyvalents, utilisables pour amertume et arôme"
    val typicalAdditionTiming = "Flexible selon l'utilisation désirée"
  }

  case object NobleHop extends HopUsage {
    val name = "NOBLE"
    val description = "Houblons nobles traditionnels, arôme fin et délicat"
    val typicalAdditionTiming = "Fin d'ébullition et dry hopping"
  }

  case object Flavoring extends HopUsage {
    val name = "FLAVORING"
    val description = "Houblons pour saveurs spécifiques en milieu d'ébullition"
    val typicalAdditionTiming = "15-30 minutes avant fin ébullition"
  }

  val all: Set[HopUsage] = Set(Bittering, Aroma, DualPurpose, NobleHop, Flavoring)

  def fromName(name: String): Option[HopUsage] = {
    all.find(_.name == name)
  }

  def fromAlphaAcid(alphaPercentage: Double): HopUsage = {
    if (alphaPercentage > 12.0) Bittering
    else if (alphaPercentage >= 6.0) DualPurpose
    else Aroma
  }

  implicit val hopUsageFormat: Format[HopUsage] = new Format[HopUsage] {
    def reads(json: JsValue): JsResult[HopUsage] = {
      json.validate[String].flatMap { name =>
        fromName(name) match {
          case Some(usage) => JsSuccess(usage)
          case None => JsError(s"Usage houblon invalide: $name")
        }
      }
    }

    def writes(usage: HopUsage): JsValue = JsString(usage.name)
  }
}