package domain.recipes.model

import play.api.libs.json._

case class RecipeFilter(
  name: Option[String] = None,
  style: Option[String] = None,
  status: Option[RecipeStatus] = None,
  createdBy: Option[UserId] = None,
  minAbv: Option[Double] = None,
  maxAbv: Option[Double] = None,
  minIbu: Option[Double] = None,
  maxIbu: Option[Double] = None,
  minSrm: Option[Double] = None,
  maxSrm: Option[Double] = None,
  hasHop: Option[String] = None,
  hasMalt: Option[String] = None,
  hasYeast: Option[String] = None,
  page: Int = 0,
  size: Int = 20
) {
  def hasFilters: Boolean = 
    name.isDefined || style.isDefined || status.isDefined || 
    createdBy.isDefined || minAbv.isDefined || maxAbv.isDefined ||
    minIbu.isDefined || maxIbu.isDefined || minSrm.isDefined || maxSrm.isDefined ||
    hasHop.isDefined || hasMalt.isDefined || hasYeast.isDefined
    
  def offset: Int = page * size
}

object RecipeFilter {
  def empty: RecipeFilter = RecipeFilter()
  
  implicit val format: Format[RecipeFilter] = Json.format[RecipeFilter]
}