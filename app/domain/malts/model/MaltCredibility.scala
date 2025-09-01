package domain.malts.model

import play.api.libs.json._

/**
 * Value Object pour la crédibilité des données de malts
 */
case class MaltCredibility private(value: Double) extends AnyVal {
  
  def isHighQuality: Boolean = value >= 0.8
  def isMediumQuality: Boolean = value >= 0.6 && value < 0.8
  def isLowQuality: Boolean = value < 0.6
  
  def category: String = {
    if (isHighQuality) "HIGH"
    else if (isMediumQuality) "MEDIUM" 
    else "LOW"
  }
}

object MaltCredibility {
  
  val MAX_CREDIBILITY = 1.0
  val MIN_CREDIBILITY = 0.0
  
  def apply(value: Double): Either[String, MaltCredibility] = {
    if (value < MIN_CREDIBILITY || value > MAX_CREDIBILITY) {
      Left(s"La crédibilité doit être entre $MIN_CREDIBILITY et $MAX_CREDIBILITY")
    } else {
      Right(new MaltCredibility(value))
    }
  }
  
  def unsafe(value: Double): MaltCredibility = new MaltCredibility(value)
  
  def fromSource(source: MaltSource): MaltCredibility = {
    val defaultValue = source match {
      case MaltSource.Manual => 1.0
      case MaltSource.AI_Discovery => 0.7
      case MaltSource.Import => 0.8
    }
    new MaltCredibility(defaultValue)
  }
  
  implicit val format: Format[MaltCredibility] = Json.valueFormat[MaltCredibility]
}
