package domain.yeasts.model

import play.api.libs.json._

/**
 * Modèle pour les recommandations de levures avec score et contexte
 */
case class RecommendedYeast(
  yeast: YeastAggregate,
  score: Double,
  reason: String,
  tips: List[String]
) {
  require(score >= 0.0 && score <= 1.0, "Score doit être entre 0 et 1")
  require(reason.nonEmpty, "Reason ne peut pas être vide")
}

object RecommendedYeast {
  implicit val format: Format[RecommendedYeast] = Json.format[RecommendedYeast]
}