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
  
  def toJson: JsValue = Json.toJson(this)
}

object YeastCharacteristics {
  implicit val format: Format[YeastCharacteristics] = Json.format[YeastCharacteristics]
}
