package domain.malts.model

import domain.shared.ValueObject
import play.api.libs.json._

/**
 * Filtre intelligent pour la recherche de malts
 * Réplique exactement YeastFilter et HopFilter pour les malts
 */
case class MaltFilter(
  name: Option[String] = None,
  maltType: Option[MaltType] = None,
  originCode: Option[String] = None,
  
  // Filtrage par couleur EBC
  minEbcColor: Option[Double] = None,
  maxEbcColor: Option[Double] = None,
  
  // Filtrage par taux d'extraction
  minExtractionRate: Option[Double] = None,
  maxExtractionRate: Option[Double] = None,
  
  // Filtrage par pouvoir diastasique
  minDiastaticPower: Option[Double] = None,
  maxDiastaticPower: Option[Double] = None,
  
  // Filtrage par profils de saveur
  flavorProfiles: List[String] = List.empty,
  
  // Filtrage par source
  source: Option[MaltSource] = None,
  
  // Filtrage par statut
  isActive: Option[Boolean] = None,
  
  // Filtrage par score de crédibilité
  minCredibilityScore: Option[Double] = None,
  maxCredibilityScore: Option[Double] = None,
  
  // Pagination
  page: Int = 0,
  size: Int = 20
) extends ValueObject {
  
  require(page >= 0, "Page doit être >= 0")
  require(size > 0 && size <= 100, "Size doit être entre 1 et 100")
  require(minEbcColor.forall(_ >= 0), "Couleur EBC min doit être >= 0")
  require(maxEbcColor.forall(_ <= 1000), "Couleur EBC max doit être <= 1000")
  require(minExtractionRate.forall(_ >= 0), "Taux d'extraction min doit être >= 0%")
  require(maxExtractionRate.forall(_ <= 100), "Taux d'extraction max doit être <= 100%")
  require(minDiastaticPower.forall(_ >= 0), "Pouvoir diastasique min doit être >= 0")
  require(maxDiastaticPower.forall(_ <= 200), "Pouvoir diastasique max doit être <= 200")
  require(minCredibilityScore.forall(s => s >= 0.0 && s <= 1.0), "Score crédibilité min doit être entre 0 et 1")
  require(maxCredibilityScore.forall(s => s >= 0.0 && s <= 1.0), "Score crédibilité max doit être entre 0 et 1")
  
  def hasFilters: Boolean = {
    name.isDefined || maltType.isDefined || originCode.isDefined ||
    minEbcColor.isDefined || maxEbcColor.isDefined ||
    minExtractionRate.isDefined || maxExtractionRate.isDefined ||
    minDiastaticPower.isDefined || maxDiastaticPower.isDefined ||
    flavorProfiles.nonEmpty || source.isDefined || isActive.isDefined ||
    minCredibilityScore.isDefined || maxCredibilityScore.isDefined
  }
  
  def isEmptySearch: Boolean = !hasFilters
  
  def withDefaultStatus: MaltFilter = {
    if (isActive.isEmpty) copy(isActive = Some(true))
    else this
  }
  
  def forPublicAPI: MaltFilter = {
    copy(isActive = Some(true))
  }
  
  def nextPage: MaltFilter = copy(page = page + 1)
  def previousPage: MaltFilter = copy(page = math.max(0, page - 1))
  def withSize(newSize: Int): MaltFilter = copy(size = math.min(100, math.max(1, newSize)))
  
  def offset: Int = page * size
  
  // Validation des ranges
  def validate(): Either[String, MaltFilter] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    // Validation de la couleur EBC
    (minEbcColor, maxEbcColor) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Couleur EBC minimum ne peut pas être supérieure au maximum"
      case (Some(min), _) if min < 0 || min > 1000 =>
        errors += "Couleur EBC minimum doit être entre 0 et 1000"
      case (_, Some(max)) if max < 0 || max > 1000 =>
        errors += "Couleur EBC maximum doit être entre 0 et 1000"
      case _ =>
    }
    
    // Validation du taux d'extraction
    (minExtractionRate, maxExtractionRate) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Taux d'extraction minimum ne peut pas être supérieur au maximum"
      case (Some(min), _) if min < 0 || min > 100 =>
        errors += "Taux d'extraction minimum doit être entre 0 et 100%"
      case (_, Some(max)) if max < 0 || max > 100 =>
        errors += "Taux d'extraction maximum doit être entre 0 et 100%"
      case _ =>
    }
    
    // Validation du pouvoir diastasique
    (minDiastaticPower, maxDiastaticPower) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Pouvoir diastasique minimum ne peut pas être supérieur au maximum"
      case (Some(min), _) if min < 0 || min > 200 =>
        errors += "Pouvoir diastasique minimum doit être entre 0 et 200"
      case (_, Some(max)) if max < 0 || max > 200 =>
        errors += "Pouvoir diastasique maximum doit être entre 0 et 200"
      case _ =>
    }
    
    // Validation du score de crédibilité
    (minCredibilityScore, maxCredibilityScore) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Score de crédibilité minimum ne peut pas être supérieur au maximum"
      case (Some(min), _) if min < 0.0 || min > 1.0 =>
        errors += "Score de crédibilité minimum doit être entre 0.0 et 1.0"
      case (_, Some(max)) if max < 0.0 || max > 1.0 =>
        errors += "Score de crédibilité maximum doit être entre 0.0 et 1.0"
      case _ =>
    }
    
    // Validation de la pagination
    if (page < 0) errors += "Page doit être positive"
    if (size < 1 || size > 100) errors += "Taille doit être entre 1 et 100"
    
    if (errors.nonEmpty) Left(errors.mkString(", "))
    else Right(this)
  }
}

object MaltFilter {
  
  /**
   * Filtre vide par défaut
   */
  val empty: MaltFilter = MaltFilter()
  
  /**
   * Filtre pour API publique (seulement malts actifs)
   */
  val publicDefault: MaltFilter = MaltFilter(isActive = Some(true))
  
  /**
   * Filtre pour API admin (tous statuts)
   */
  val adminDefault: MaltFilter = MaltFilter()
  
  // ==========================================================================
  // FILTRES PRÉDÉFINIS PAR TYPE
  // ==========================================================================
  
  def forType(maltType: MaltType): MaltFilter = {
    MaltFilter(maltType = Some(maltType), isActive = Some(true))
  }
  
  def forSource(source: MaltSource): MaltFilter = {
    MaltFilter(source = Some(source), isActive = Some(true))
  }
  
  // ==========================================================================
  // FILTRES PAR CARACTÉRISTIQUES DE BRASSAGE
  // ==========================================================================
  
  def forBaseMalts: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.BASE),
      minExtractionRate = Some(75.0),
      isActive = Some(true)
    )
  }
  
  def forLightMalts: MaltFilter = {
    MaltFilter(
      maxEbcColor = Some(20.0),
      isActive = Some(true)
    )
  }
  
  def forDarkMalts: MaltFilter = {
    MaltFilter(
      minEbcColor = Some(100.0),
      isActive = Some(true)
    )
  }
  
  def forHighExtraction: MaltFilter = {
    MaltFilter(
      minExtractionRate = Some(80.0),
      isActive = Some(true)
    )
  }
  
  def forDiastaticMalts: MaltFilter = {
    MaltFilter(
      minDiastaticPower = Some(30.0),
      isActive = Some(true)
    )
  }
  
  def forSpecialtyMalts: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.SPECIALTY),
      isActive = Some(true)
    )
  }
  
  def forCrystalMalts: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.CRYSTAL),
      minEbcColor = Some(20.0),
      maxEbcColor = Some(300.0),
      isActive = Some(true)
    )
  }
  
  def forRoastedMalts: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.ROASTED),
      minEbcColor = Some(200.0),
      isActive = Some(true)
    )
  }
  
  def forWheatMalts: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.WHEAT),
      isActive = Some(true)
    )
  }
  
  // ==========================================================================
  // RECHERCHE PAR PROFILS DE SAVEUR
  // ==========================================================================
  
  def withFlavorProfiles(flavors: String*): MaltFilter = {
    MaltFilter(flavorProfiles = flavors.toList, isActive = Some(true))
  }
  
  def forCaramelFlavors: MaltFilter = {
    MaltFilter(
      flavorProfiles = List("caramel", "toffee", "sweet"),
      maltType = Some(MaltType.CRYSTAL),
      isActive = Some(true)
    )
  }
  
  def forChocolateFlavors: MaltFilter = {
    MaltFilter(
      flavorProfiles = List("chocolate", "cocoa", "roasted"),
      maltType = Some(MaltType.ROASTED),
      isActive = Some(true)
    )
  }
  
  def forNuttyFlavors: MaltFilter = {
    MaltFilter(
      flavorProfiles = List("nutty", "biscuit", "bread"),
      isActive = Some(true)
    )
  }
  
  def forSmokyFlavors: MaltFilter = {
    MaltFilter(
      flavorProfiles = List("smoky", "peated", "smoked"),
      isActive = Some(true)
    )
  }
  
  // ==========================================================================
  // FILTRES PAR STYLE DE BIÈRE
  // ==========================================================================
  
  def forIPA: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.BASE),
      maxEbcColor = Some(30.0),
      minExtractionRate = Some(75.0),
      isActive = Some(true)
    )
  }
  
  def forStout: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.ROASTED),
      minEbcColor = Some(200.0),
      isActive = Some(true)
    )
  }
  
  def forWheatBeer: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.WHEAT),
      maxEbcColor = Some(20.0),
      isActive = Some(true)
    )
  }
  
  def forPilsner: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.BASE),
      maxEbcColor = Some(5.0),
      minExtractionRate = Some(80.0),
      isActive = Some(true)
    )
  }
  
  def forAmberAle: MaltFilter = {
    MaltFilter(
      maltType = Some(MaltType.CRYSTAL),
      minEbcColor = Some(30.0),
      maxEbcColor = Some(150.0),
      isActive = Some(true)
    )
  }
  
  // ==========================================================================
  // FILTRES PAR QUALITÉ
  // ==========================================================================
  
  def forHighQuality: MaltFilter = {
    MaltFilter(
      minCredibilityScore = Some(0.8),
      source = Some(MaltSource.Manual),
      isActive = Some(true)
    )
  }
  
  def forReliableMalts: MaltFilter = {
    MaltFilter(
      minCredibilityScore = Some(0.7),
      minExtractionRate = Some(75.0),
      isActive = Some(true)
    )
  }
  
  // ==========================================================================
  // VALIDATION DES PARAMÈTRES DE FILTRE
  // ==========================================================================
  
  def validate(filter: MaltFilter): List[String] = {
    var errors = List.empty[String]
    
    // Validation des plages de couleur EBC
    if (filter.minEbcColor.isDefined && filter.maxEbcColor.isDefined) {
      val min = filter.minEbcColor.get
      val max = filter.maxEbcColor.get
      if (min > max) {
        errors = s"Couleur EBC min (${min}) > max (${max})" :: errors
      }
    }
    
    // Validation des plages d'extraction
    if (filter.minExtractionRate.isDefined && filter.maxExtractionRate.isDefined) {
      val min = filter.minExtractionRate.get
      val max = filter.maxExtractionRate.get
      if (min > max) {
        errors = s"Taux extraction min (${min}%) > max (${max}%)" :: errors
      }
    }
    
    // Validation des plages de pouvoir diastasique
    if (filter.minDiastaticPower.isDefined && filter.maxDiastaticPower.isDefined) {
      val min = filter.minDiastaticPower.get
      val max = filter.maxDiastaticPower.get
      if (min > max) {
        errors = s"Pouvoir diastasique min ($min) > max ($max)" :: errors
      }
    }
    
    // Validation des scores de crédibilité
    if (filter.minCredibilityScore.isDefined && filter.maxCredibilityScore.isDefined) {
      val min = filter.minCredibilityScore.get
      val max = filter.maxCredibilityScore.get
      if (min > max) {
        errors = s"Score crédibilité min ($min) > max ($max)" :: errors
      }
    }
    
    errors.reverse
  }
  
  // ==========================================================================
  // JSON FORMAT
  // ==========================================================================
  
  implicit val format: Format[MaltFilter] = Json.format[MaltFilter]
}