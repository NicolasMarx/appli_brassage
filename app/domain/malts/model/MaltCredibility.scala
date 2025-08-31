package domain.malts.model

import play.api.libs.json._

/**
 * Value Object MaltCredibility - Score de crédibilité des données malt
 * Score 0-100 indiquant la fiabilité des informations
 * Utilisé pour l'IA de veille et validation automatique
 */
final case class MaltCredibility private (value: Int) extends AnyVal {
  override def toString: String = s"${value}%"

  /**
   * Classification du niveau de confiance
   */
  def confidenceLevel: String = value match {
    case v if v < 30  => "Très Faible"    // Données non vérifiées
    case v if v < 50  => "Faible"         // Données partielles
    case v if v < 70  => "Modérée"        // Données correctes
    case v if v < 85  => "Bonne"          // Données vérifiées
    case v if v < 95  => "Très Bonne"     // Données officielles
    case _            => "Excellente"     // Données certifiées
  }

  /**
   * Indique si les données nécessitent validation humaine
   */
  def needsHumanReview: Boolean = value < 70

  /**
   * Indique si les données peuvent être utilisées en production
   */
  def isProductionReady: Boolean = value >= 50

  /**
   * Indique si les données sont considérées comme officielles
   */
  def isOfficial: Boolean = value >= 95

  /**
   * Calcul du score pondéré avec un autre score
   */
  def weightedWith(other: MaltCredibility, thisWeight: Double): MaltCredibility = {
    val otherWeight = 1.0 - thisWeight
    val weightedScore = (this.value * thisWeight) + (other.value * otherWeight)
    new MaltCredibility(math.round(weightedScore).toInt)
  }

  /**
   * Ajustement du score selon facteurs qualité
   */
  def adjustedBy(factor: Double): Either[String, MaltCredibility] = {
    val newScore = math.round(value * factor).toInt
    MaltCredibility(newScore)
  }

  /**
   * Dégradation temporelle du score (données qui vieillissent)
   */
  def degradeByAge(monthsOld: Int): MaltCredibility = {
    val degradationFactor = math.max(0.5, 1.0 - (monthsOld * 0.02)) // -2% par mois
    val newScore = math.round(value * degradationFactor).toInt
    new MaltCredibility(math.max(20, newScore)) // Minimum 20%
  }
}

object MaltCredibility {
  def apply(value: Int): Either[String, MaltCredibility] = {
    if (value < 0) {
      Left("Le score de crédibilité ne peut pas être négatif")
    } else if (value > 100) {
      Left("Le score de crédibilité ne peut pas dépasser 100")
    } else {
      Right(new MaltCredibility(value))
    }
  }

  def fromDouble(value: Double): Either[String, MaltCredibility] = {
    apply(math.round(value).toInt)
  }

  def fromSource(source: MaltSource): MaltCredibility = {
    new MaltCredibility(source.defaultCredibility)
  }

  def fromString(str: String): Either[String, MaltCredibility] = {
    try {
      val cleanStr = str.trim.replace("%", "")
      val value = cleanStr.toInt
      apply(value)
    } catch {
      case _: NumberFormatException =>
        Left(s"Format de score de crédibilité invalide: '$str'")
    }
  }

  // Constantes pour différents niveaux
  val MINIMUM_ACCEPTABLE = new MaltCredibility(50)
  val NEEDS_REVIEW = new MaltCredibility(69)  // Seuil validation humaine
  val GOOD_QUALITY = new MaltCredibility(80)
  val EXCELLENT_QUALITY = new MaltCredibility(95)
  val PERFECT = new MaltCredibility(100)

  // Scores par source typiques
  val MANUAL_ENTRY = new MaltCredibility(95)
  val AI_DISCOVERED = new MaltCredibility(70)
  val IMPORT_DATA = new MaltCredibility(85)
  val MANUFACTURER_DATA = new MaltCredibility(98)
  val COMMUNITY_DATA = new MaltCredibility(75)

  implicit val maltCredibilityFormat: Format[MaltCredibility] = new Format[MaltCredibility] {
    def reads(json: JsValue): JsResult[MaltCredibility] = {
      json.validate[Int].flatMap { value =>
        MaltCredibility(value) match {
          case Right(credibility) => JsSuccess(credibility)
          case Left(error) => JsError(error)
        }
      }
    }

    def writes(credibility: MaltCredibility): JsValue = JsNumber(credibility.value)
  }
}