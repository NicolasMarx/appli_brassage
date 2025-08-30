package domain.hops.model

/**
 * Value Object représentant l'identifiant unique d'un houblon
 */
case class HopId(value: String) extends AnyVal {
  override def toString: String = value
}

object HopId {

  /**
   * Constructeur principal avec validation
   */
  def apply(value: String): HopId = {
    val trimmed = value.trim
    require(trimmed.nonEmpty, "HopId cannot be empty or blank")
    require(trimmed.length <= 50, s"HopId too long: ${trimmed.length} characters (max 50)")
    require(isValidHopId(trimmed), s"Invalid HopId format: $trimmed")

    new HopId(trimmed)
  }

  /**
   * Alias pour compatibilité - identique à apply
   */
  def fromString(value: String): HopId = apply(value)

  /**
   * Version safe qui retourne un Either pour la gestion d'erreur explicite
   */
  def create(value: String): Either[String, HopId] = {
    try {
      Right(apply(value))
    } catch {
      case e: IllegalArgumentException => Left(e.getMessage)
      case e: Exception => Left(s"Invalid HopId: ${e.getMessage}")
    }
  }

  /**
   * Crée un HopId en format slug à partir d'un nom de houblon
   */
  def fromHopName(name: String): HopId = {
    val slug = toSlug(name)
    apply(slug)
  }

  /**
   * Validation du format d'un HopId
   * Autorise: lettres, chiffres, tirets, underscores
   * Doit commencer par une lettre ou un chiffre
   */
  private def isValidHopId(value: String): Boolean = {
    val hopIdPattern = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$".r
    hopIdPattern.matches(value)
  }

  /**
   * Convertit un nom en slug (format d'ID)
   */
  private def toSlug(name: String): String = {
    name
      .toLowerCase
      .trim
      .replaceAll("\\s+", "-")           // Espaces -> tirets
      .replaceAll("[^a-z0-9-]", "")      // Supprimer caractères spéciaux
      .replaceAll("-+", "-")             // Multiples tirets -> un seul
      .replaceAll("^-|-$", "")           // Supprimer tirets début/fin
      .take(50)                          // Limiter longueur
  }

  /**
   * Constantes pour des IDs communs
   */
  object Common {
    val CASCADE = HopId("cascade")
    val CENTENNIAL = HopId("centennial")
    val CITRA = HopId("citra")
    val COLUMBUS = HopId("columbus")
    val FUGGLE = HopId("fuggle")
    val HALLERTAU = HopId("hallertau")
    val MOSAIC = HopId("mosaic")
    val SAAZ = HopId("saaz")
    val SIMCOE = HopId("simcoe")
    val WILLAMETTE = HopId("willamette")
  }

  /**
   * Génère un ID unique basé sur le nom et l'origine
   */
  def generateId(hopName: String, originCode: String): HopId = {
    val nameSlug = toSlug(hopName)
    val originSlug = originCode.toLowerCase
    val combinedId = s"$nameSlug-$originSlug"
    apply(combinedId)
  }

  /**
   * Parsing à partir d'une URL ou chaîne composite
   */
  def parseFromPath(path: String): Option[HopId] = {
    val cleaned = path.split("/").lastOption.getOrElse(path)
    create(cleaned).toOption
  }
}