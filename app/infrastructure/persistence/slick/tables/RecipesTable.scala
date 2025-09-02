package infrastructure.persistence.slick.tables

import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.UUID

/**
 * Définition table recipes pour Slick
 * Suit exactement le pattern YeastsTable pour cohérence architecturale
 */
class RecipesTable(tag: Tag) extends Table[RecipeRow](tag, "recipes") {
  
  // Colonnes de base
  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def description = column[Option[String]]("description")
  
  // Style de bière
  def styleId = column[String]("style_id")
  def styleName = column[String]("style_name")
  def styleCategory = column[String]("style_category")
  
  // Taille du lot
  def batchSizeValue = column[Double]("batch_size_value")
  def batchSizeUnit = column[String]("batch_size_unit")
  def batchSizeLiters = column[Double]("batch_size_liters")
  
  // Ingrédients (JSONB)
  def hops = column[String]("hops")
  def malts = column[String]("malts")
  def yeast = column[Option[String]]("yeast")
  def otherIngredients = column[String]("other_ingredients")
  
  // Calculs
  def originalGravity = column[Option[Double]]("original_gravity")
  def finalGravity = column[Option[Double]]("final_gravity")
  def abv = column[Option[Double]]("abv")
  def ibu = column[Option[Double]]("ibu")
  def srm = column[Option[Double]]("srm")
  def efficiency = column[Option[Double]]("efficiency")
  def calculationsAt = column[Option[Instant]]("calculations_at")
  
  // Procédures
  def hasMashProfile = column[Boolean]("has_mash_profile")
  def hasBoilProcedure = column[Boolean]("has_boil_procedure")
  def hasFermentationProfile = column[Boolean]("has_fermentation_profile")
  def hasPackagingProcedure = column[Boolean]("has_packaging_procedure")
  
  // Métadonnées
  def status = column[String]("status")
  def createdBy = column[UUID]("created_by")
  def version = column[Int]("version")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  
  // Index composés pour performance (suivant pattern YeastsTable)
  def idxNameStatus = index("idx_recipes_name_status", (name, status))
  def idxStyleStatus = index("idx_recipes_style_status", (styleId, status))
  def idxStatus = index("idx_recipes_status", status)
  def idxCreatedBy = index("idx_recipes_created_by", createdBy)
  def idxCreatedAt = index("idx_recipes_created_at", createdAt)
  def idxUpdatedAt = index("idx_recipes_updated_at", updatedAt)
  
  // Mapping vers RecipeRow (custom mapping pour contourner la limite 22 éléments)
  def * = (
    (id, name, description, styleId, styleName, styleCategory),
    (batchSizeValue, batchSizeUnit, batchSizeLiters),
    (hops, malts, yeast, otherIngredients),
    (originalGravity, finalGravity, abv, ibu, srm, efficiency, calculationsAt),
    (hasMashProfile, hasBoilProcedure, hasFermentationProfile, hasPackagingProcedure),
    (status, createdBy, version, createdAt, updatedAt)
  ).shaped <> (
    { case ((id, name, description, styleId, styleName, styleCategory),
            (batchSizeValue, batchSizeUnit, batchSizeLiters),
            (hops, malts, yeast, otherIngredients),
            (originalGravity, finalGravity, abv, ibu, srm, efficiency, calculationsAt),
            (hasMashProfile, hasBoilProcedure, hasFermentationProfile, hasPackagingProcedure),
            (status, createdBy, version, createdAt, updatedAt)) =>
      RecipeRow(
        id, name, description, styleId, styleName, styleCategory,
        batchSizeValue, batchSizeUnit, batchSizeLiters,
        hops, malts, yeast, otherIngredients,
        originalGravity, finalGravity, abv, ibu, srm, efficiency, calculationsAt,
        hasMashProfile, hasBoilProcedure, hasFermentationProfile, hasPackagingProcedure,
        status, createdBy, version, createdAt, updatedAt
      )
    },
    { row: RecipeRow =>
      Some((
        (row.id, row.name, row.description, row.styleId, row.styleName, row.styleCategory),
        (row.batchSizeValue, row.batchSizeUnit, row.batchSizeLiters),
        (row.hops, row.malts, row.yeast, row.otherIngredients),
        (row.originalGravity, row.finalGravity, row.abv, row.ibu, row.srm, row.efficiency, row.calculationsAt),
        (row.hasMashProfile, row.hasBoilProcedure, row.hasFermentationProfile, row.hasPackagingProcedure),
        (row.status, row.createdBy, row.version, row.createdAt, row.updatedAt)
      ))
    }
  )
}

/**
 * Companion object pour la table (suivant exact pattern YeastsTable)
 */
object RecipesTable {
  val recipes = TableQuery[RecipesTable]
}