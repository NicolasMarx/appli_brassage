package domain.yeasts.model

import domain.shared.ValueObject

/**
 * Value Object pour les filtres de recherche de levures
 * Critères de filtrage avancé pour les APIs publiques et admin
 */
case class YeastFilter(
  name: Option[String] = None,
  laboratory: Option[YeastLaboratory] = None,
  yeastType: Option[YeastType] = None,
  minAttenuation: Option[Int] = None,
  maxAttenuation: Option[Int] = None,
  minTemperature: Option[Int] = None,
  maxTemperature: Option[Int] = None,
  maxAlcoholTolerance: Option[Double] = None,
  minAlcoholTolerance: Option[Double] = None,
  flocculation: Option[FlocculationLevel] = None,
  characteristics: List[String] = List.empty,
  status: List[YeastStatus] = List.empty,
  page: Int = 0,
  size: Int = 20
) extends ValueObject {
  
  require(page >= 0, "Page doit être >= 0")
  require(size > 0 && size <= 100, "Size doit être entre 1 et 100")
  require(minAttenuation.forall(_ >= 30), "Atténuation min doit être >= 30%")
  require(maxAttenuation.forall(_ <= 100), "Atténuation max doit être <= 100%")
  require(minTemperature.forall(_ >= 0), "Température min doit être >= 0°C")
  require(maxTemperature.forall(_ <= 50), "Température max doit être <= 50°C")
  require(minAlcoholTolerance.forall(_ >= 0), "Tolérance alcool min doit être >= 0%")
  require(maxAlcoholTolerance.forall(_ <= 20), "Tolérance alcool max doit être <= 20%")
  
  def hasFilters: Boolean = {
    name.isDefined || laboratory.isDefined || yeastType.isDefined ||
    minAttenuation.isDefined || maxAttenuation.isDefined ||
    minTemperature.isDefined || maxTemperature.isDefined ||
    minAlcoholTolerance.isDefined || maxAlcoholTolerance.isDefined ||
    flocculation.isDefined || characteristics.nonEmpty || status.nonEmpty
  }
  
  def isEmptySearch: Boolean = !hasFilters
  
  def withDefaultStatus: YeastFilter = {
    if (status.isEmpty) copy(status = List(YeastStatus.Active))
    else this
  }
  
  def forPublicAPI: YeastFilter = {
    copy(status = List(YeastStatus.Active))
  }
  
  def nextPage: YeastFilter = copy(page = page + 1)
  def previousPage: YeastFilter = copy(page = math.max(0, page - 1))
  def withSize(newSize: Int): YeastFilter = copy(size = math.min(100, math.max(1, newSize)))
}

object YeastFilter {
  
  /**
   * Filtre vide par défaut
   */
  val empty: YeastFilter = YeastFilter()
  
  /**
   * Filtre pour API publique (seulement levures actives)
   */
  val publicDefault: YeastFilter = YeastFilter(status = List(YeastStatus.Active))
  
  /**
   * Filtre pour API admin (tous statuts)
   */
  val adminDefault: YeastFilter = YeastFilter()
  
  /**
   * Filtres prédéfinis par type de levure
   */
  def forType(yeastType: YeastType): YeastFilter = {
    YeastFilter(yeastType = Some(yeastType), status = List(YeastStatus.Active))
  }
  
  def forLaboratory(laboratory: YeastLaboratory): YeastFilter = {
    YeastFilter(laboratory = Some(laboratory), status = List(YeastStatus.Active))
  }
  
  /**
   * Filtres par caractéristiques de fermentation
   */
  def forHighAttenuation: YeastFilter = {
    YeastFilter(minAttenuation = Some(82), status = List(YeastStatus.Active))
  }
  
  def forLowTemperature: YeastFilter = {
    YeastFilter(maxTemperature = Some(15), status = List(YeastStatus.Active))
  }
  
  def forHighAlcoholTolerance: YeastFilter = {
    YeastFilter(minAlcoholTolerance = Some(12.0), status = List(YeastStatus.Active))
  }
  
  /**
   * Recherche par caractéristiques aromatiques
   */
  def withCharacteristics(chars: String*): YeastFilter = {
    YeastFilter(characteristics = chars.toList, status = List(YeastStatus.Active))
  }
  
  /**
   * Validation des paramètres de filtre
   */
  def validate(filter: YeastFilter): List[String] = {
    var errors = List.empty[String]
    
    if (filter.minAttenuation.isDefined && filter.maxAttenuation.isDefined) {
      val min = filter.minAttenuation.get
      val max = filter.maxAttenuation.get
      if (min > max) {
        errors = s"Atténuation min ($min%) > max ($max%)" :: errors
      }
    }
    
    if (filter.minTemperature.isDefined && filter.maxTemperature.isDefined) {
      val min = filter.minTemperature.get
      val max = filter.maxTemperature.get
      if (min > max) {
        errors = s"Température min (${min}°C) > max (${max}°C)" :: errors
      }
    }
    
    if (filter.minAlcoholTolerance.isDefined && filter.maxAlcoholTolerance.isDefined) {
      val min = filter.minAlcoholTolerance.get
      val max = filter.maxAlcoholTolerance.get
      if (min > max) {
        errors = s"Tolérance alcool min ($min%) > max ($max%)" :: errors
      }
    }
    
    errors.reverse
  }
}
