package domain.malts.model

import play.api.libs.json._

/**
 * Value Object EBCColor - Couleur des malts selon échelle EBC
 * EBC (European Brewery Convention) : 0 = très pâle, 1500+ = très foncé
 * Inclut conversions vers SRM et Lovibond pour compatibilité
 */
final case class EBCColor private (value: Double) extends AnyVal {
  override def toString: String = f"$value%.1f EBC"

  /**
   * Conversion vers SRM (Standard Reference Method)
   * Formule: SRM ≈ EBC × 0.508
   */
  def toSRM: Double = value * 0.508

  /**
   * Conversion vers Lovibond
   * Formule: °L ≈ (EBC × 0.375) - 0.46
   */
  def toLovibond: Double = (value * 0.375) - 0.46

  /**
   * Classification couleur selon EBC
   */
  def colorName: String = value match {
    case v if v <= 8    => "Très Pâle"     // Pilsner, Wheat
    case v if v <= 16   => "Pâle"          // Pale Ale, Munich Light
    case v if v <= 33   => "Ambré"         // Vienna, Munich Dark
    case v if v <= 66   => "Cuivré"        // Crystal/Caramel malts
    case v if v <= 130  => "Brun"          // Chocolate, Brown malts
    case v if v <= 300  => "Brun Foncé"    // Roasted malts
    case _              => "Noir"          // Black malts, Roasted Barley
  }

  /**
   * Indique si ce malt peut être utilisé comme malt de base
   * Les malts de base ont généralement EBC < 25
   */
  def isBaseMalt: Boolean = value < 25.0

  /**
   * Indique si ce malt est un malt spécial/arôme
   * Les malts spéciaux ont généralement EBC > 25
   */
  def isSpecialtyMalt: Boolean = value >= 25.0
}

object EBCColor {
  def apply(value: Double): Either[String, EBCColor] = {
    if (value.isNaN || value.isInfinite) {
      Left("Couleur EBC invalide (NaN ou infini)")
    } else if (value < 0.0) {
      Left("La couleur EBC ne peut pas être négative")
    } else if (value > 1500.0) {
      Left("La couleur EBC ne peut pas dépasser 1500 EBC (limite pratique)")
    } else {
      Right(new EBCColor(value))
    }
  }

  /**
   * Création depuis SRM
   * Formule: EBC ≈ SRM / 0.508
   */
  def fromSRM(srm: Double): Either[String, EBCColor] = {
    val ebcValue = srm / 0.508
    apply(ebcValue)
  }

  /**
   * Création depuis Lovibond
   * Formule: EBC ≈ (°L + 0.46) / 0.375
   */
  def fromLovibond(lovibond: Double): Either[String, EBCColor] = {
    val ebcValue = (lovibond + 0.46) / 0.375
    apply(ebcValue)
  }

  def fromString(str: String): Either[String, EBCColor] = {
    try {
      val cleanStr = str.trim.toUpperCase
        .replace("EBC", "")
        .replace("°", "")
        .trim
      val value = cleanStr.toDouble
      apply(value)
    } catch {
      case _: NumberFormatException =>
        Left(s"Format de couleur EBC invalide: '$str'")
    }
  }

  // Constantes utiles pour malts courants
  val PILSNER = new EBCColor(3.0)          // Pilsner malt
  val PALE_ALE = new EBCColor(5.5)         // Pale Ale malt
  val MUNICH = new EBCColor(15.0)          // Munich malt
  val VIENNA = new EBCColor(7.0)           // Vienna malt
  val CRYSTAL_60 = new EBCColor(118.0)     // Crystal 60L
  val CHOCOLATE = new EBCColor(900.0)      // Chocolate malt
  val ROASTED_BARLEY = new EBCColor(1300.0) // Roasted barley

  implicit val ebcColorFormat: Format[EBCColor] = new Format[EBCColor] {
    def reads(json: JsValue): JsResult[EBCColor] = {
      json.validate[Double].flatMap { value =>
        EBCColor(value) match {
          case Right(color) => JsSuccess(color)
          case Left(error) => JsError(error)
        }
      }
    }

    def writes(color: EBCColor): JsValue = JsNumber(color.value)
  }
}