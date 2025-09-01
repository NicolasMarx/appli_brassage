package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour la plage d'atténuation des levures
 * Représente le pourcentage d'atténuation (ex: 75-82%)
 */
case class AttenuationRange(min: Int, max: Int) extends ValueObject {
  require(min >= 30 && min <= 100, s"Atténuation minimale invalide: $min% (doit être entre 30% et 100%)")
  require(max >= 30 && max <= 100, s"Atténuation maximale invalide: $max% (doit être entre 30% et 100%)")
  require(min <= max, s"Atténuation min ($min%) doit être <= max ($max%)")
  require((max - min) <= 30, s"Écart d'atténuation trop large: ${max - min}% (max 30%)")
  
  def average: Double = (min + max) / 2.0
  def range: Int = max - min
  def isHigh: Boolean = min >= 80
  def isMedium: Boolean = min >= 70 && max <= 85
  def isLow: Boolean = max <= 75
  
  def contains(attenuation: Int): Boolean = attenuation >= min && attenuation <= max
  
  override def toString: String = s"$min-$max%"
}

object AttenuationRange {
  /**
   * Constructeur avec validation
   */
  def apply(min: Int, max: Int): AttenuationRange = new AttenuationRange(min, max)
  
  /**
   * Constructeur à partir d'une valeur unique (±3%)
   */
  def fromSingle(attenuation: Int): AttenuationRange = {
    AttenuationRange(math.max(30, attenuation - 3), math.min(100, attenuation + 3))
  }
  
  /**
   * Parsing depuis string "75-82" ou "78"
   */
  def parse(input: String): Either[String, AttenuationRange] = {
    try {
      val cleaned = input.trim.replaceAll("[%\\s]", "")
      if (cleaned.contains("-")) {
        val parts = cleaned.split("-")
        if (parts.length == 2) {
          val min = parts(0).toInt
          val max = parts(1).toInt
          Right(AttenuationRange(min, max))
        } else {
          Left(s"Format invalide: $input (attendu: '75-82' ou '75')")
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
   * Plages d'atténuation typiques
   */
  val Low = AttenuationRange(65, 75)      // Faible atténuation
  val Medium = AttenuationRange(75, 82)   // Atténuation moyenne
  val High = AttenuationRange(82, 88)     // Haute atténuation
  val VeryHigh = AttenuationRange(88, 95) // Très haute atténuation
}
