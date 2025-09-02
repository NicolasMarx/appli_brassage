package domain.yeasts.model

/**
 * Enumération des saisons pour les recommandations saisonnières de levures
 */
sealed trait Season {
  def name: String
  def description: String
}

object Season {
  
  case object Spring extends Season {
    val name = "SPRING"
    val description = "Printemps"
  }
  
  case object Summer extends Season {
    val name = "SUMMER" 
    val description = "Été"
  }
  
  case object Autumn extends Season {
    val name = "AUTUMN"
    val description = "Automne"
  }
  
  case object Winter extends Season {
    val name = "WINTER"
    val description = "Hiver"
  }
  
  val all: List[Season] = List(Spring, Summer, Autumn, Winter)
  
  def fromName(name: String): Option[Season] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }
}