package domain.hops.model

case class HopOrigin(code: String, name: String, region: String) {

  /**
   * Détermine si l'origine est considérée comme "noble"
   * Les houblons nobles viennent traditionnellement d'Europe
   */
  def isNoble: Boolean = code.toUpperCase match {
    case "DE" | "CZ" | "UK" | "AT" | "BE" | "PL" => true // Allemagne, République Tchèque, UK, Autriche, Belgique, Pologne
    case _ => false
  }

  /**
   * Détermine si l'origine est du "Nouveau Monde"
   * Principalement Amérique du Nord, Océanie et certaines régions modernes
   */
  def isNewWorld: Boolean = code.toUpperCase match {
    case "US" | "CA" | "AU" | "NZ" | "AR" | "CL" | "ZA" => true // USA, Canada, Australie, Nouvelle-Zélande, Argentine, Chili, Afrique du Sud
    case _ => false
  }
}

object HopOrigin {
  def fromCode(code: String): Option[HopOrigin] = code.toUpperCase match {
    case "US" => Some(HopOrigin("US", "United States", "North America"))
    case "DE" => Some(HopOrigin("DE", "Germany", "Europe"))
    case "UK" => Some(HopOrigin("UK", "United Kingdom", "Europe"))
    case "CZ" => Some(HopOrigin("CZ", "Czech Republic", "Europe"))
    case "NZ" => Some(HopOrigin("NZ", "New Zealand", "Oceania"))
    case "AU" => Some(HopOrigin("AU", "Australia", "Oceania"))
    case "FR" => Some(HopOrigin("FR", "France", "Europe"))
    case "CA" => Some(HopOrigin("CA", "Canada", "North America"))
    case "BE" => Some(HopOrigin("BE", "Belgium", "Europe"))
    case "AT" => Some(HopOrigin("AT", "Austria", "Europe"))
    case "PL" => Some(HopOrigin("PL", "Poland", "Europe"))
    case "JP" => Some(HopOrigin("JP", "Japan", "Asia"))
    case "AR" => Some(HopOrigin("AR", "Argentina", "South America"))
    case "CL" => Some(HopOrigin("CL", "Chile", "South America"))
    case "ZA" => Some(HopOrigin("ZA", "South Africa", "Africa"))
    case _ => None
  }
  
  // JSON format support
  import play.api.libs.json._
  implicit val format: Format[HopOrigin] = Json.format[HopOrigin]
}