package domain.yeasts.model

/**
 * Modèle de base de recette pour les recommandations de levures
 */
case class BaseRecipe(
  fermentationTemp: Int,
  expectedAbv: Double,
  targetAttenuation: Int,
  style: Option[String] = None
) {
  require(fermentationTemp >= 0 && fermentationTemp <= 50, "Température doit être entre 0 et 50°C")
  require(expectedAbv >= 0 && expectedAbv <= 20, "ABV doit être entre 0 et 20%")
  require(targetAttenuation >= 30 && targetAttenuation <= 100, "Atténuation doit être entre 30 et 100%")
}