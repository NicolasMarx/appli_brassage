package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour la température de fermentation
 * Plage optimale en Celsius
 */
case class FermentationTemp(min: Int, max: Int) extends ValueObject {
  require(min >= 0 && min <= 50, s"Température minimale invalide: ${min}°C (doit être entre 0°C et 50°C)")
  require(max >= 0 && max <= 50, s"Température maximale invalide: ${max}°C (doit être entre 0°C et 50°C)")
  require(min <= max, s"Température min (${min}°C) doit être <= max (${max}°C)")
  require((max - min) <= 25, s"Écart de température trop large: ${max - min}°C (max 25°C)")
  
  def average: Double = (min + max) / 2.0
  def range: Int = max - min
  def isLowTemp: Boolean = max <= 15    // Lager range
  def isMediumTemp: Boolean = min >= 15 && max <= 25  // Ale range
  def isHighTemp: Boolean = min >= 25   // Kveik/Saison range
  
  def contains(temp: Int): Boolean = temp >= min && temp <= max
  def toFahrenheit: (Int, Int) = ((min * 9/5) + 32, (max * 9/5) + 32)
  
  override def toString: String = s"${min}-${max}°C"
}

object FermentationTemp {
  /**
   * Constructeur avec validation
   */
  def apply(min: Int, max: Int): FermentationTemp = new FermentationTemp(min, max)
  
  /**
   * Constructeur à partir d'une température unique (±2°C)
   */
  def fromSingle(temp: Int): FermentationTemp = {
    FermentationTemp(math.max(0, temp - 2), math.min(50, temp + 2))
  }
  
  /**
   * Parsing depuis string "18-22" ou "20"
   */
  def parse(input: String): Either[String, FermentationTemp] = {
    try {
      val cleaned = input.trim.replaceAll("[°CcF\\s]", "").toLowerCase
      if (cleaned.contains("-")) {
        val parts = cleaned.split("-")
        if (parts.length == 2) {
          val min = parts(0).toInt
          val max = parts(1).toInt
          Right(FermentationTemp(min, max))
        } else {
          Left(s"Format invalide: $input (attendu: '18-22' ou '20')")
        }
      } else {
        val value = cleaned.toInt
        Right(fromSingle(value))
      }
    } catch {
      case _: NumberFormatException => Left(s"Format numérique invalide: $input")
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Plages de température typiques
   */
  val LagerRange = FermentationTemp(7, 15)      // Fermentation basse
  val AleRange = FermentationTemp(18, 22)       // Fermentation haute standard
  val WarmAleRange = FermentationTemp(20, 25)   // Fermentation haute chaude
  val SaisonRange = FermentationTemp(25, 35)    // Levures saison
  val KveikRange = FermentationTemp(30, 40)     // Levures kveik
  val SourRange = FermentationTemp(25, 40)      // Bactéries lactiques
}
