package domain.yeasts.model

import play.api.libs.json._

case class YeastCharacteristics(
  aromaProfile: List[String],
  flavorProfile: List[String], 
  esters: List[String],
  phenols: List[String],
  otherCompounds: List[String],
  notes: Option[String]
) {
  def isClean: Boolean = esters.isEmpty && phenols.isEmpty && otherCompounds.isEmpty
  def allCharacteristics: List[String] = aromaProfile ++ flavorProfile ++ esters ++ phenols ++ otherCompounds
  def primaryCharacteristics: List[String] = aromaProfile
  def secondaryCharacteristics: List[String] = flavorProfile
  def aromaIntensity: Int = allCharacteristics.length.min(10)
  def flavorProfileString: String = flavorProfile.mkString(", ")
  
  def toJson: JsValue = Json.toJson(this)
}

object YeastCharacteristics {
  
  def apply(characteristics: List[String]): YeastCharacteristics = {
    YeastCharacteristics(
      aromaProfile = characteristics,
      flavorProfile = List.empty,
      esters = List.empty,
      phenols = List.empty,
      otherCompounds = List.empty,
      notes = None
    )
  }
  
  implicit val format: Format[YeastCharacteristics] = Json.format[YeastCharacteristics]
}
