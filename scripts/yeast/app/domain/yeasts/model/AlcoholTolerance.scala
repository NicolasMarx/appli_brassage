package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour la tolérance à l'alcool des levures
 * Pourcentage maximum d'alcool supporté
 */
case class AlcoholTolerance(percentage: Double) extends ValueObject {
  require(percentage >= 0.0 && percentage <= 20.0, 
    s"Tolérance alcool invalide: $percentage% (doit être entre 0% et 20%)")
  
  // Arrondi à une décimale
  val rounded: Double = math.round(percentage * 10) / 10.0
  
  def isLow: Boolean = percentage < 8.0
  def isMedium: Boolean = percentage >= 8.0 && percentage < 12.0
  def isHigh: Boolean = percentage >= 12.0 && percentage < 16.0
  def isVeryHigh: Boolean = percentage >= 16.0
  
  def canFerment(targetAbv: Double): Boolean = percentage >= targetAbv
  
  override def toString: String = s"$rounded%"
}

object AlcoholTolerance {
  /**
   * Constructeur avec validation
   */
  def apply(percentage: Double): AlcoholTolerance = new AlcoholTolerance(percentage)
  
  /**
   * Constructeur depuis entier
   */
  def fromInt(percentage: Int): AlcoholTolerance = AlcoholTolerance(percentage.toDouble)
  
  /**
   * Parsing depuis string
   */
  def parse(input: String): Either[String, AlcoholTolerance] = {
    try {
      val cleaned = input.trim.replaceAll("[%\\s]", "")
      val value = cleaned.toDouble
      Right(AlcoholTolerance(value))
    } catch {
      case _: NumberFormatException => Left(s"Format numérique invalide: $input")
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
  
  /**
   * Tolérances typiques
   */
  val Low = AlcoholTolerance(6.0)        // Levures délicates
  val Standard = AlcoholTolerance(10.0)  // Levures bière standard
  val High = AlcoholTolerance(14.0)      // Levures haute tolérance
  val Champagne = AlcoholTolerance(18.0) // Levures champagne/vin
}
