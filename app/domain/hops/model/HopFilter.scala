package domain.hops.model

import play.api.libs.json._

/**
 * Filtre intelligent pour la recherche de houblons
 * Réplique les fonctionnalités avancées de YeastFilter
 */
case class HopFilter(
  name: Option[String] = None,
  origin: Option[String] = None,
  country: Option[String] = None,
  usage: Option[HopUsage] = None,
  
  // Filtrage par alpha acids
  minAlphaAcids: Option[Double] = None,
  maxAlphaAcids: Option[Double] = None,
  
  // Filtrage par beta acids
  minBetaAcids: Option[Double] = None,
  maxBetaAcids: Option[Double] = None,
  
  // Filtrage par cohumulone
  minCohumulone: Option[Double] = None,
  maxCohumulone: Option[Double] = None,
  
  // Filtrage par profils aromatiques
  aromaProfiles: List[String] = List.empty,
  
  // Filtrage par année de récolte
  harvestYear: Option[Int] = None,
  minHarvestYear: Option[Int] = None,
  maxHarvestYear: Option[Int] = None,
  
  // Filtrage par disponibilité
  available: Option[Boolean] = None,
  
  // Pagination
  page: Int = 0,
  size: Int = 20
) {
  
  def hasFilters: Boolean = 
    name.isDefined || origin.isDefined || country.isDefined || usage.isDefined ||
    minAlphaAcids.isDefined || maxAlphaAcids.isDefined ||
    minBetaAcids.isDefined || maxBetaAcids.isDefined ||
    minCohumulone.isDefined || maxCohumulone.isDefined ||
    aromaProfiles.nonEmpty || harvestYear.isDefined ||
    minHarvestYear.isDefined || maxHarvestYear.isDefined ||
    available.isDefined
    
  def offset: Int = page * size
  
  // Validation des ranges
  def validate(): Either[String, HopFilter] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    // Validation des alpha acids
    (minAlphaAcids, maxAlphaAcids) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Alpha acids minimum ne peut pas être supérieur au maximum"
      case (Some(min), _) if min < 0 || min > 30 =>
        errors += "Alpha acids minimum doit être entre 0 et 30%"
      case (_, Some(max)) if max < 0 || max > 30 =>
        errors += "Alpha acids maximum doit être entre 0 et 30%"
      case _ =>
    }
    
    // Validation des beta acids
    (minBetaAcids, maxBetaAcids) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Beta acids minimum ne peut pas être supérieur au maximum"
      case (Some(min), _) if min < 0 || min > 15 =>
        errors += "Beta acids minimum doit être entre 0 et 15%"
      case (_, Some(max)) if max < 0 || max > 15 =>
        errors += "Beta acids maximum doit être entre 0 et 15%"
      case _ =>
    }
    
    // Validation du cohumulone
    (minCohumulone, maxCohumulone) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Cohumulone minimum ne peut pas être supérieur au maximum"
      case (Some(min), _) if min < 0 || min > 100 =>
        errors += "Cohumulone minimum doit être entre 0 et 100%"
      case (_, Some(max)) if max < 0 || max > 100 =>
        errors += "Cohumulone maximum doit être entre 0 et 100%"
      case _ =>
    }
    
    // Validation des années de récolte
    (minHarvestYear, maxHarvestYear) match {
      case (Some(min), Some(max)) if min > max =>
        errors += "Année de récolte minimum ne peut pas être supérieure au maximum"
      case (Some(min), _) if min < 1900 || min > 2030 =>
        errors += "Année de récolte minimum doit être entre 1900 et 2030"
      case (_, Some(max)) if max < 1900 || max > 2030 =>
        errors += "Année de récolte maximum doit être entre 1900 et 2030"
      case _ =>
    }
    
    // Validation de la pagination
    if (page < 0) errors += "Page doit être positive"
    if (size < 1 || size > 100) errors += "Taille doit être entre 1 et 100"
    
    if (errors.nonEmpty) Left(errors.mkString(", "))
    else Right(this)
  }
}

object HopFilter {
  def empty: HopFilter = HopFilter()
  
  // Filtres pré-configurés pour des cas d'usage courants
  def forBittering: HopFilter = HopFilter(
    usage = Some(HopUsage.Bittering),
    minAlphaAcids = Some(8.0)
  )
  
  def forAroma: HopFilter = HopFilter(
    usage = Some(HopUsage.Aroma),
    maxAlphaAcids = Some(8.0)
  )
  
  def forDualPurpose: HopFilter = HopFilter(
    usage = Some(HopUsage.DualPurpose),
    minAlphaAcids = Some(4.0),
    maxAlphaAcids = Some(12.0)
  )
  
  def forCitrusProfile: HopFilter = HopFilter(
    aromaProfiles = List("citrus", "grapefruit", "orange", "lemon")
  )
  
  def forFloralProfile: HopFilter = HopFilter(
    aromaProfiles = List("floral", "rose", "lavender", "perfume")
  )
  
  def forPineProfile: HopFilter = HopFilter(
    aromaProfiles = List("pine", "resinous", "woody")
  )
  
  def forTropicalProfile: HopFilter = HopFilter(
    aromaProfiles = List("tropical", "mango", "passion fruit", "papaya", "coconut")
  )
  
  def forNewWorld: HopFilter = HopFilter(
    country = Some("United States"),
    minHarvestYear = Some(2000) // Variétés modernes
  )
  
  def forOldWorld: HopFilter = HopFilter(
    country = Some("Germany")
  )
  
  def forHighAlpha: HopFilter = HopFilter(
    minAlphaAcids = Some(15.0),
    usage = Some(HopUsage.Bittering)
  )
  
  def forLowCohumulone: HopFilter = HopFilter(
    maxCohumulone = Some(30.0) // Cohumulone faible pour amertume douce
  )
  
  implicit val format: Format[HopFilter] = Json.format[HopFilter]
}