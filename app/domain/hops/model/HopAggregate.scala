package domain.hops.model

import java.time.Instant
import domain.shared.NonEmptyString

/**
 * Agrégat principal du domaine Hop
 * Représente un houblon avec toutes ses caractéristiques et comportements métier
 */
case class HopAggregate(
                         id: HopId,
                         name: NonEmptyString,
                         alphaAcid: AlphaAcidPercentage,
                         betaAcid: Option[AlphaAcidPercentage], // Temporairement AlphaAcidPercentage en attendant BetaAcidPercentage
                         origin: HopOrigin,
                         usage: HopUsage,
                         description: Option[NonEmptyString],
                         aromaProfile: List[String], // Profils aromatiques associés
                         status: HopStatus,
                         source: HopSource,
                         credibilityScore: Int, // Score de fiabilité des données (0-100)
                         createdAt: Instant,
                         updatedAt: Instant,
                         version: Int // Version pour gestion optimiste des conflits
                       ) {

  /**
   * Met à jour le taux d'acide alpha avec validation métier
   */
  def updateAlphaAcid(alpha: AlphaAcidPercentage): HopAggregate = {
    require(alpha.value >= 0 && alpha.value <= 30, s"Alpha acid must be between 0 and 30%, got: ${alpha.value}")
    this.copy(
      alphaAcid = alpha,
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  /**
   * Met à jour le taux d'acide bêta avec validation métier
   */
  def updateBetaAcid(beta: Option[AlphaAcidPercentage]): HopAggregate = {
    beta.foreach { b =>
      require(b.value >= 0 && b.value <= 15, s"Beta acid must be between 0 and 15%, got: ${b.value}")
    }
    this.copy(
      betaAcid = beta,
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  /**
   * Met à jour la description du houblon
   */
  def updateDescription(desc: Option[String]): HopAggregate = {
    val newDescription = desc.filter(_.trim.nonEmpty).map(NonEmptyString(_))
    this.copy(
      description = newDescription,
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  /**
   * Ajoute un profil aromatique
   */
  def addAromaProfile(aroma: String): HopAggregate = {
    if (!aromaProfile.contains(aroma)) {
      this.copy(
        aromaProfile = (aromaProfile :+ aroma).distinct,
        updatedAt = Instant.now(),
        version = version + 1
      )
    } else {
      this
    }
  }

  /**
   * Supprime un profil aromatique
   */
  def removeAromaProfile(aroma: String): HopAggregate = {
    this.copy(
      aromaProfile = aromaProfile.filterNot(_ == aroma),
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  /**
   * Met à jour le statut du houblon
   */
  def updateStatus(newStatus: HopStatus): HopAggregate = {
    this.copy(
      status = newStatus,
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  /**
   * Met à jour l'usage du houblon
   */
  def updateUsage(newUsage: HopUsage): HopAggregate = {
    this.copy(
      usage = newUsage,
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  /**
   * Met à jour le score de crédibilité
   */
  def updateCredibilityScore(score: Int): HopAggregate = {
    require(score >= 0 && score <= 100, s"Credibility score must be between 0 and 100, got: $score")
    this.copy(
      credibilityScore = score,
      updatedAt = Instant.now(),
      version = version + 1
    )
  }

  // ===============================
  // MÉTHODES MÉTIER
  // ===============================

  /**
   * Détermine si le houblon est considéré comme fiable
   */
  def isReliable: Boolean = credibilityScore >= 70

  /**
   * Détermine si le houblon a été découvert par IA
   */
  def isAiDiscovered: Boolean = source == HopSource.AI_Discovery

  /**
   * Détermine si le houblon est actif
   */
  def isActive: Boolean = status == HopStatus.Active

  /**
   * Détermine si le houblon est noble (basé sur l'origine)
   */
  def isNoble: Boolean = origin.isNoble

  /**
   * Détermine si le houblon est du nouveau monde
   */
  def isNewWorld: Boolean = origin.isNewWorld

  /**
   * Détermine si le houblon est polyvalent (dual-purpose)
   */
  def isDualPurpose: Boolean = usage == HopUsage.DualPurpose

  /**
   * Retourne une représentation textuelle des caractéristiques principales
   */
  def characteristics: String = {
    val usageStr = usage match {
      case HopUsage.Bittering => "Amérisant"
      case HopUsage.Aroma => "Aromatique"
      case HopUsage.DualPurpose => "Polyvalent"
      case HopUsage.NobleHop => "Noble"
    }

    val originStr = if (isNoble) s"${origin.name} (Noble)" else origin.name
    val alphaStr = f"α:${alphaAcid.value}%.1f%%"
    val betaStr = betaAcid.map(b => f"β:${b.value}%.1f%%").getOrElse("β:N/A")

    s"$usageStr - $originStr - $alphaStr, $betaStr"
  }

  /**
   * Validation complète de l'agrégat
   */
  def validate: List[String] = {
    var errors = List.empty[String]

    if (name.value.trim.isEmpty) errors = "Le nom ne peut pas être vide" :: errors
    if (alphaAcid.value < 0 || alphaAcid.value > 30) errors = "L'acide alpha doit être entre 0 et 30%" :: errors
    betaAcid.foreach { b =>
      if (b.value < 0 || b.value > 15) errors = "L'acide bêta doit être entre 0 et 15%" :: errors
    }
    if (credibilityScore < 0 || credibilityScore > 100) errors = "Le score de crédibilité doit être entre 0 et 100" :: errors
    if (version < 0) errors = "La version ne peut pas être négative" :: errors

    errors.reverse
  }

  /**
   * Vérifie si l'agrégat est valide
   */
  def isValid: Boolean = validate.isEmpty

  override def toString: String = {
    s"HopAggregate(${id.value}, ${name.value}, ${characteristics}, score:$credibilityScore, v$version)"
  }
}

/**
 * Objet companion pour la création et les utilitaires
 */
object HopAggregate {

  /**
   * Crée un nouveau HopAggregate avec des valeurs par défaut sensées
   */
  def create(
              id: String,
              name: String,
              alphaAcid: Double,
              origin: HopOrigin,
              usage: HopUsage
            ): HopAggregate = {
    val now = Instant.now()

    HopAggregate(
      id = HopId(id),
      name = NonEmptyString(name),
      alphaAcid = AlphaAcidPercentage(alphaAcid),
      betaAcid = None,
      origin = origin,
      usage = usage,
      description = None,
      aromaProfile = List.empty,
      status = HopStatus.Active,
      source = HopSource.Manual,
      credibilityScore = 50, // Score neutre par défaut
      createdAt = now,
      updatedAt = now,
      version = 1
    )
  }

  /**
   * Crée un HopAggregate découvert par IA avec un score de crédibilité initial
   */
  def createAiDiscovered(
                          id: String,
                          name: String,
                          alphaAcid: Double,
                          origin: HopOrigin,
                          usage: HopUsage,
                          initialCredibilityScore: Int = 30
                        ): HopAggregate = {
    val now = Instant.now()

    HopAggregate(
      id = HopId(id),
      name = NonEmptyString(name),
      alphaAcid = AlphaAcidPercentage(alphaAcid),
      betaAcid = None,
      origin = origin,
      usage = usage,
      description = None,
      aromaProfile = List.empty,
      status = HopStatus.Active,
      source = HopSource.AI_Discovery,
      credibilityScore = initialCredibilityScore,
      createdAt = now,
      updatedAt = now,
      version = 1
    )
  }

  /**
   * Crée un HopAggregate importé depuis une source externe
   */
  def createImported(
                      id: String,
                      name: String,
                      alphaAcid: Double,
                      betaAcid: Option[Double],
                      origin: HopOrigin,
                      usage: HopUsage,
                      description: Option[String],
                      aromas: List[String] = List.empty
                    ): HopAggregate = {
    val now = Instant.now()

    HopAggregate(
      id = HopId(id),
      name = NonEmptyString(name),
      alphaAcid = AlphaAcidPercentage(alphaAcid),
      betaAcid = betaAcid.map(AlphaAcidPercentage(_)),
      origin = origin,
      usage = usage,
      description = description.filter(_.trim.nonEmpty).map(NonEmptyString(_)),
      aromaProfile = aromas.distinct,
      status = HopStatus.Active,
      source = HopSource.Import,
      credibilityScore = 80, // Score élevé pour les données importées
      createdAt = now,
      updatedAt = now,
      version = 1
    )
  }
}
