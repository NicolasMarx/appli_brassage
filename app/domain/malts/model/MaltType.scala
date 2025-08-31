package domain.malts.model

import play.api.libs.json._

/**
 * MaltType - Classification des types de malts selon procédé de maltage
 */
sealed trait MaltType {
  def name: String
  def description: String
  def typicalEBCRange: (Double, Double)  // Range EBC typique
  def typicalUsagePercent: (Double, Double) // % typique dans recette
}

object MaltType {
  case object BASE extends MaltType {
    val name = "BASE"
    val description = "Malts de base - foundation de la recette"
    val typicalEBCRange = (2.0, 25.0)
    val typicalUsagePercent = (50.0, 100.0)
  }

  case object CRYSTAL extends MaltType {
    val name = "CRYSTAL"
    val description = "Malts crystal/caramel - sucres caramélisés"
    val typicalEBCRange = (40.0, 300.0)
    val typicalUsagePercent = (1.0, 20.0)
  }

  case object ROASTED extends MaltType {
    val name = "ROASTED"
    val description = "Malts torréfiés - saveurs grillées/café"
    val typicalEBCRange = (500.0, 1500.0)
    val typicalUsagePercent = (0.5, 10.0)
  }

  case object SPECIALTY extends MaltType {
    val name = "SPECIALTY"
    val description = "Malts spéciaux - arômes/textures spécifiques"
    val typicalEBCRange = (3.0, 80.0)
    val typicalUsagePercent = (1.0, 40.0)
  }

  case object ADJUNCT extends MaltType {
    val name = "ADJUNCT"
    val description = "Adjuvants - riz, maïs, avoine non maltée"
    val typicalEBCRange = (0.0, 10.0)
    val typicalUsagePercent = (5.0, 40.0)
  }

  val all: Set[MaltType] = Set(BASE, CRYSTAL, ROASTED, SPECIALTY, ADJUNCT)

  def fromName(name: String): Option[MaltType] = {
    all.find(_.name == name.toUpperCase)
  }

  implicit val maltTypeFormat: Format[MaltType] = new Format[MaltType] {
    def reads(json: JsValue): JsResult[MaltType] = {
      json.validate[String].flatMap { name =>
        fromName(name) match {
          case Some(maltType) => JsSuccess(maltType)
          case None => JsError(s"Type de malt invalide: $name")
        }
      }
    }

    def writes(maltType: MaltType): JsValue = JsString(maltType.name)
  }
}

/**
 * MaltStatus - Statut du malt dans le système
 */
sealed trait MaltStatus {
  def name: String
  def description: String
  def isAvailable: Boolean
}

object MaltStatus {
  case object ACTIVE extends MaltStatus {
    val name = "ACTIVE"
    val description = "Malt actif et disponible"
    val isAvailable = true
  }

  case object INACTIVE extends MaltStatus {
    val name = "INACTIVE"
    val description = "Malt inactif mais conservé"
    val isAvailable = false
  }

  case object DISCONTINUED extends MaltStatus {
    val name = "DISCONTINUED"
    val description = "Malt discontinué par le producteur"
    val isAvailable = false
  }

  case object SEASONAL extends MaltStatus {
    val name = "SEASONAL"
    val description = "Malt disponible selon saison"
    val isAvailable = true
  }

  case object OUT_OF_STOCK extends MaltStatus {
    val name = "OUT_OF_STOCK"
    val description = "Temporairement en rupture de stock"
    val isAvailable = false
  }

  val all: Set[MaltStatus] = Set(ACTIVE, INACTIVE, DISCONTINUED, SEASONAL, OUT_OF_STOCK)

  def fromName(name: String): Option[MaltStatus] = {
    all.find(_.name == name.toUpperCase)
  }

  implicit val maltStatusFormat: Format[MaltStatus] = new Format[MaltStatus] {
    def reads(json: JsValue): JsResult[MaltStatus] = {
      json.validate[String].flatMap { name =>
        fromName(name) match {
          case Some(status) => JsSuccess(status)
          case None => JsError(s"Statut malt invalide: $name")
        }
      }
    }

    def writes(status: MaltStatus): JsValue = JsString(status.name)
  }
}