package domain.malts.model

import play.api.libs.json._

sealed trait MaltSource {
  def name: String
  def defaultCredibility: Double
}

object MaltSource {
  case object Manual extends MaltSource { 
    val name = "MANUAL"
    val defaultCredibility = 1.0
  }
  case object AI_Discovery extends MaltSource { 
    val name = "AI_DISCOVERY"
    val defaultCredibility = 0.7
  }
  case object Import extends MaltSource { 
    val name = "IMPORT"
    val defaultCredibility = 0.8
  }

  val all: List[MaltSource] = List(Manual, AI_Discovery, Import)

  def fromName(name: String): Option[MaltSource] = {
    all.find(_.name.equalsIgnoreCase(name.trim))
  }

  implicit val format: Format[MaltSource] = Format(
    Reads(js => js.validate[String].map(fromName).flatMap {
      case Some(source) => JsSuccess(source)
      case None => JsError("Source de malt invalide")
    }),
    Writes(source => JsString(source.name))
  )
}
