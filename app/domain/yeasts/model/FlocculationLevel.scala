package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour le niveau de floculation des levures
 * Capacité d'agglomération et de sédimentation
 */
sealed trait FlocculationLevel extends ValueObject {
  def name: String
  def description: String
  def clarificationTime: String
  def rackingRecommendation: String
}

object FlocculationLevel {
  
  case object Low extends FlocculationLevel {
    val name = "Low"
    val description = "Faible floculation - levures restent en suspension"
    val clarificationTime = "Longue (2-4 semaines)"
    val rackingRecommendation = "Soutirage tardif, filtration recommandée"
  }
  
  case object Medium extends FlocculationLevel {
    val name = "Medium" 
    val description = "Floculation moyenne - équilibre suspension/sédimentation"
    val clarificationTime = "Moyenne (1-2 semaines)"
    val rackingRecommendation = "Soutirage standard après fermentation"
  }
  
  case object MediumHigh extends FlocculationLevel {
    val name = "Medium-High"
    val description = "Floculation élevée - bonne sédimentation"
    val clarificationTime = "Rapide (5-10 jours)"
    val rackingRecommendation = "Soutirage précoce possible"
  }
  
  case object High extends FlocculationLevel {
    val name = "High"
    val description = "Haute floculation - sédimentation rapide et compacte"
    val clarificationTime = "Très rapide (3-7 jours)"
    val rackingRecommendation = "Soutirage précoce, attention atténuation"
  }
  
  case object VeryHigh extends FlocculationLevel {
    val name = "Very High"
    val description = "Floculation très élevée - risque d'atténuation incomplète"
    val clarificationTime = "Immédiate (2-5 jours)"
    val rackingRecommendation = "Remise en suspension si nécessaire"
  }
  
  val values: List[FlocculationLevel] = List(Low, Medium, MediumHigh, High, VeryHigh)
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[FlocculationLevel] = {
    values.find(_.name.equalsIgnoreCase(name.trim.replaceAll("-", "")))
      .orElse(values.find(_.name.toLowerCase.contains(name.toLowerCase.trim)))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, FlocculationLevel] = {
    val cleaned = input.trim.toLowerCase
    cleaned match {
      case s if s.matches("(very )?low|faible") => Right(Low)
      case s if s.matches("medium|moyenne?|moy") => Right(Medium)
      case s if s.matches("medium.?high|élevée?") => Right(MediumHigh)
      case s if s.matches("(very )?high|haute") => Right(High)
      case s if s.matches("very.?high|très.?haute") => Right(VeryHigh)
      case _ => fromName(input).toRight(s"Niveau de floculation inconnu: $input")
    }
  }
}
