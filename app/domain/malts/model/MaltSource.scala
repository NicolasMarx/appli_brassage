sealed trait MaltSource {
  def name: String
  def description: String
  def defaultCredibility: Int  // Score crédibilité par défaut
}

object MaltSource {
  case object MANUAL extends MaltSource {
    val name = "MANUAL"
    val description = "Saisie manuelle par admin"
    val defaultCredibility = 95
  }

  case object AI_DISCOVERED extends MaltSource {
    val name = "AI_DISCOVERED"
    val description = "Découvert automatiquement par IA"
    val defaultCredibility = 70
  }

  case object IMPORT extends MaltSource {
    val name = "IMPORT"
    val description = "Importé depuis source externe"
    val defaultCredibility = 85
  }

  case object MANUFACTURER extends MaltSource {
    val name = "MANUFACTURER"
    val description = "Données officielles fabricant"
    val defaultCredibility = 98
  }

  case object COMMUNITY extends MaltSource {
    val name = "COMMUNITY"
    val description = "Contribution communauté utilisateurs"
    val defaultCredibility = 75
  }

  val all: Set[MaltSource] = Set(MANUAL, AI_DISCOVERED, IMPORT, MANUFACTURER, COMMUNITY)

  def fromName(name: String): Option[MaltSource] = {
    all.find(_.name == name.toUpperCase)
  }

  implicit val maltSourceFormat: Format[MaltSource] = new Format[MaltSource] {
    def reads(json: JsValue): JsResult[MaltSource] = {
      json.validate[String].flatMap { name =>
        fromName(name) match {
          case Some(source) => JsSuccess(source)
          case None => JsError(s"Source malt invalide: $name")
        }
      }
    }

    def writes(source: MaltSource): JsValue = JsString(source.name)
  }
}