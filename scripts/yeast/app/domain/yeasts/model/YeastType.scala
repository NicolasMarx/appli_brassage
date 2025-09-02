package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les types de levures
 * Classification par famille de fermentation
 */
sealed trait YeastType extends ValueObject {
  def name: String
  def description: String
  def temperatureRange: (Int, Int) // (min, max) en Celsius
  def characteristics: String
}

object YeastType {
  
  case object Ale extends YeastType {
    val name = "Ale"
    val description = "Saccharomyces cerevisiae - Fermentation haute"
    val temperatureRange = (15, 24)
    val characteristics = "Fermentation rapide, arômes fruités et esters"
  }
  
  case object Lager extends YeastType {
    val name = "Lager"
    val description = "Saccharomyces pastorianus - Fermentation basse"
    val temperatureRange = (7, 15)
    val characteristics = "Fermentation lente, profil propre et net"
  }
  
  case object Wheat extends YeastType {
    val name = "Wheat"
    val description = "Levures spécialisées pour bières de blé"
    val temperatureRange = (18, 24)
    val characteristics = "Arômes banana et clou de girofle"
  }
  
  case object Saison extends YeastType {
    val name = "Saison"
    val description = "Levures farmhouse traditionnelles"
    val temperatureRange = (20, 35)
    val characteristics = "Tolérance chaleur, arômes poivre et épices"
  }
  
  case object Wild extends YeastType {
    val name = "Wild"
    val description = "Brettanomyces et levures sauvages"
    val temperatureRange = (15, 25)
    val characteristics = "Fermentation lente, arômes funky et complexes"
  }
  
  case object Sour extends YeastType {
    val name = "Sour"
    val description = "Lactobacillus et bactéries lactiques"
    val temperatureRange = (25, 40)
    val characteristics = "Production d'acide lactique, acidité"
  }
  
  case object Champagne extends YeastType {
    val name = "Champagne"
    val description = "Levures haute tolérance alcool"
    val temperatureRange = (12, 20)
    val characteristics = "Atténuation élevée, tolérance alcool 15%+"
  }
  
  case object Kveik extends YeastType {
    val name = "Kveik"
    val description = "Levures norvégiennes traditionnelles"
    val temperatureRange = (25, 40)
    val characteristics = "Fermentation ultra-rapide, haute température"
  }
  
  val values: List[YeastType] = List(
    Ale, Lager, Wheat, Saison, Wild, Sour, Champagne, Kveik
  )
  
  /**
   * Recherche par nom
   */
  def fromName(name: String): Option[YeastType] = {
    values.find(_.name.equalsIgnoreCase(name.trim))
  }
  
  /**
   * Parsing avec validation
   */
  def parse(input: String): Either[String, YeastType] = {
    fromName(input.trim).toRight(s"Type de levure inconnu: $input")
  }
  
  /**
   * Types recommandés par style de bière
   */
  def recommendedForBeerStyle(style: String): List[YeastType] = {
    style.toLowerCase match {
      case s if s.contains("ipa") || s.contains("pale ale") => List(Ale)
      case s if s.contains("lager") || s.contains("pilsner") => List(Lager)
      case s if s.contains("wheat") || s.contains("weizen") => List(Wheat)
      case s if s.contains("saison") => List(Saison)
      case s if s.contains("sour") || s.contains("gose") => List(Sour, Wild)
      case s if s.contains("imperial") => List(Ale, Champagne)
      case _ => List(Ale, Lager)
    }
  }
}
