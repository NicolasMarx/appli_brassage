package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les caractéristiques organoleptiques des levures
 * Profil aromatique et gustatif produit
 */
case class YeastCharacteristics(
  aromaProfile: List[String],
  flavorProfile: List[String], 
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String] = None
) extends ValueObject {
  
  require(aromaProfile.nonEmpty || flavorProfile.nonEmpty, 
    "Au moins un profil aromatique ou gustatif requis")
  require(aromaProfile.forall(_.trim.nonEmpty), "Profils aromatiques ne peuvent pas être vides")
  require(flavorProfile.forall(_.trim.nonEmpty), "Profils gustatifs ne peuvent pas être vides")
  
  def allCharacteristics: List[String] = 
    aromaProfile ++ flavorProfile ++ esters ++ phenols ++ otherCompounds
  
  def hasEsters: Boolean = esters.nonEmpty
  def hasPhenols: Boolean = phenols.nonEmpty
  def isFruity: Boolean = allCharacteristics.exists(_.toLowerCase.contains("fruit"))
  def isSpicy: Boolean = allCharacteristics.exists(c => 
    c.toLowerCase.matches(".*(spic|pepper|clove|phenol).*"))
  def isClean: Boolean = allCharacteristics.exists(_.toLowerCase.contains("clean"))
  
  override def toString: String = {
    val characteristics = List(
      if (aromaProfile.nonEmpty) Some(s"Arômes: ${aromaProfile.mkString(", ")}") else None,
      if (flavorProfile.nonEmpty) Some(s"Saveurs: ${flavorProfile.mkString(", ")}") else None,
      if (esters.nonEmpty) Some(s"Esters: ${esters.mkString(", ")}") else None,
      if (phenols.nonEmpty) Some(s"Phénols: ${phenols.mkString(", ")}") else None
    ).flatten
    
    characteristics.mkString(" | ") + notes.map(n => s" | Notes: $n").getOrElse("")
  }
}

object YeastCharacteristics {
  
  /**
   * Constructeur simple pour profils basiques
   */
  def simple(characteristics: String*): YeastCharacteristics = {
    YeastCharacteristics(
      aromaProfile = characteristics.toList,
      flavorProfile = List.empty,
      esters = List.empty,
      phenols = List.empty,
      otherCompounds = List.empty
    )
  }
  
  /**
   * Caractéristiques prédéfinies communes
   */
  val Clean = YeastCharacteristics(
    aromaProfile = List("Clean", "Neutral"),
    flavorProfile = List("Clean", "Subtle"),
    esters = List.empty,
    phenols = List.empty,
    otherCompounds = List.empty,
    notes = Some("Profil neutre, met en valeur les malts et houblons")
  )
  
  val Fruity = YeastCharacteristics(
    aromaProfile = List("Fruity", "Stone fruit", "Citrus"),
    flavorProfile = List("Fruity", "Sweet"),
    esters = List("Ethyl acetate", "Isoamyl acetate"),
    phenols = List.empty,
    otherCompounds = List.empty,
    notes = Some("Esters fruités prononcés")
  )
  
  val Spicy = YeastCharacteristics(
    aromaProfile = List("Spicy", "Phenolic", "Clove"),
    flavorProfile = List("Spicy", "Peppery"),
    esters = List.empty,
    phenols = List("4-vinyl guaiacol"),
    otherCompounds = List.empty,
    notes = Some("Phénols épicés caractéristiques")
  )
  
  val Tropical = YeastCharacteristics(
    aromaProfile = List("Tropical", "Pineapple", "Mango", "Passion fruit"),
    flavorProfile = List("Fruity", "Tropical"),
    esters = List("Ethyl butyrate", "Ethyl hexanoate"),
    phenols = List.empty,
    otherCompounds = List("Thiols"),
    notes = Some("Arômes tropicaux intenses, populaire pour NEIPAs")
  )
}
