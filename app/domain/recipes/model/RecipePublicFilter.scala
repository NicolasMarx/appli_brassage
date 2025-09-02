package domain.recipes.model

import play.api.libs.json._

/**
 * Filtre avancé pour l'API publique des recettes
 * Offre des capacités de filtrage sophistiquées pour la recherche communautaire
 */
case class RecipePublicFilter(
  // Filtres de base
  name: Option[String] = None,
  style: Option[String] = None,
  difficulty: Option[DifficultyLevel] = None,
  
  // Filtres de paramètres de brassage
  minAbv: Option[Double] = None,
  maxAbv: Option[Double] = None,
  minIbu: Option[Double] = None,
  maxIbu: Option[Double] = None,
  minSrm: Option[Double] = None,
  maxSrm: Option[Double] = None,
  
  // Filtres de taille de batch
  minBatchSize: Option[Double] = None,
  maxBatchSize: Option[Double] = None,
  batchUnit: Option[VolumeUnit] = None,
  
  // Filtres d'ingrédients
  hasIngredient: Option[String] = None,
  excludeIngredient: Option[String] = None,
  hasHop: Option[String] = None,
  hasMalt: Option[String] = None,
  hasYeast: Option[String] = None,
  
  // Filtres d'équipement et procédés
  equipmentType: Option[EquipmentType] = None,
  brewingMethod: Option[BrewingMethod] = None,
  fermentationType: Option[FermentationType] = None,
  
  // Filtres temporels
  brewingTimeMax: Option[Int] = None, // heures
  fermentationTimeMax: Option[Int] = None, // jours
  totalTimeMax: Option[Int] = None, // semaines
  
  // Filtres de disponibilité d'ingrédients
  seasonalIngredients: Option[Season] = None,
  commonIngredients: Option[Boolean] = None,
  budgetLevel: Option[BudgetLevel] = None,
  
  // Filtres communautaires
  minPopularity: Option[Double] = None,
  tags: List[String] = List.empty,
  createdAfter: Option[java.time.Instant] = None,
  createdBefore: Option[java.time.Instant] = None,
  
  // Options de tri et pagination
  sortBy: RecipeSortBy = RecipeSortBy.Relevance,
  sortDirection: SortDirection = SortDirection.Desc,
  page: Int = 0,
  size: Int = 20
) {
  
  /**
   * Valide les paramètres du filtre
   */
  def validate(): Either[List[String], RecipePublicFilter] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    // Validation des plages numériques
    if (minAbv.exists(_ < 0) || maxAbv.exists(_ > 20)) {
      errors += "ABV doit être entre 0 et 20%"
    }
    if (minAbv.isDefined && maxAbv.isDefined && minAbv.get > maxAbv.get) {
      errors += "ABV minimum ne peut pas être supérieur au maximum"
    }
    
    if (minIbu.exists(_ < 0) || maxIbu.exists(_ > 200)) {
      errors += "IBU doit être entre 0 et 200"
    }
    if (minIbu.isDefined && maxIbu.isDefined && minIbu.get > maxIbu.get) {
      errors += "IBU minimum ne peut pas être supérieur au maximum"
    }
    
    if (minSrm.exists(_ < 0) || maxSrm.exists(_ > 80)) {
      errors += "SRM doit être entre 0 et 80"
    }
    if (minSrm.isDefined && maxSrm.isDefined && minSrm.get > maxSrm.get) {
      errors += "SRM minimum ne peut pas être supérieur au maximum"
    }
    
    if (minBatchSize.exists(_ <= 0) || maxBatchSize.exists(_ > 10000)) {
      errors += "Taille de batch doit être positive et raisonnable"
    }
    if (minBatchSize.isDefined && maxBatchSize.isDefined && minBatchSize.get > maxBatchSize.get) {
      errors += "Taille minimum ne peut pas être supérieure au maximum"
    }
    
    // Validation des temps
    if (brewingTimeMax.exists(_ <= 0) || brewingTimeMax.exists(_ > 48)) {
      errors += "Temps de brassage doit être entre 1 et 48 heures"
    }
    if (fermentationTimeMax.exists(_ <= 0) || fermentationTimeMax.exists(_ > 365)) {
      errors += "Temps de fermentation doit être entre 1 et 365 jours"
    }
    
    // Validation de la pagination
    if (page < 0) errors += "Page ne peut pas être négative"
    if (size <= 0 || size > 100) errors += "Taille de page doit être entre 1 et 100"
    
    // Validation des tags
    if (tags.exists(_.length > 50)) {
      errors += "Tags ne peuvent pas dépasser 50 caractères"
    }
    
    if (errors.nonEmpty) Left(errors.toList) else Right(this)
  }
  
  /**
   * Indique si des filtres sont appliqués
   */
  def hasFilters: Boolean = 
    name.isDefined || style.isDefined || difficulty.isDefined ||
    minAbv.isDefined || maxAbv.isDefined || minIbu.isDefined || maxIbu.isDefined ||
    minSrm.isDefined || maxSrm.isDefined || minBatchSize.isDefined || maxBatchSize.isDefined ||
    hasIngredient.isDefined || excludeIngredient.isDefined ||
    hasHop.isDefined || hasMalt.isDefined || hasYeast.isDefined ||
    equipmentType.isDefined || brewingMethod.isDefined || fermentationType.isDefined ||
    brewingTimeMax.isDefined || fermentationTimeMax.isDefined || totalTimeMax.isDefined ||
    seasonalIngredients.isDefined || commonIngredients.isDefined || budgetLevel.isDefined ||
    minPopularity.isDefined || tags.nonEmpty ||
    createdAfter.isDefined || createdBefore.isDefined
    
  /**
   * Calcule l'offset pour la pagination
   */
  def offset: Int = page * size
  
  /**
   * Indique si c'est un filtre de recherche textuelle
   */
  def isTextSearch: Boolean = name.isDefined || hasIngredient.isDefined
  
  /**
   * Indique si c'est un filtre de paramètres numériques
   */
  def isParametricFilter: Boolean = 
    minAbv.isDefined || maxAbv.isDefined || minIbu.isDefined || maxIbu.isDefined ||
    minSrm.isDefined || maxSrm.isDefined
    
  /**
   * Indique si c'est un filtre d'ingrédients
   */
  def isIngredientFilter: Boolean = 
    hasIngredient.isDefined || excludeIngredient.isDefined ||
    hasHop.isDefined || hasMalt.isDefined || hasYeast.isDefined
    
  /**
   * Convertit vers un filtre de base pour compatibilité
   */
  def toBaseRecipeFilter: RecipeFilter = {
    RecipeFilter(
      name = name,
      style = style,
      status = Some(RecipeStatus.Published), // Seules les recettes publiées pour l'API publique
      createdBy = None, // Pas de filtrage par créateur en public
      minAbv = minAbv,
      maxAbv = maxAbv,
      minIbu = minIbu,
      maxIbu = maxIbu,
      minSrm = minSrm,
      maxSrm = maxSrm,
      hasHop = hasHop,
      hasMalt = hasMalt,
      hasYeast = hasYeast,
      page = page,
      size = size
    )
  }
  
  /**
   * Crée un filtre optimisé pour la recherche
   */
  def optimizeForSearch(): RecipePublicFilter = {
    if (isTextSearch && !isParametricFilter) {
      // Pour recherche textuelle, augmenter la taille pour meilleur scoring
      this.copy(size = math.min(size * 2, 100))
    } else {
      this
    }
  }
  
  /**
   * Applique des filtres de sécurité pour l'API publique
   */
  def sanitizeForPublicAPI(): RecipePublicFilter = {
    this.copy(
      size = math.min(size, 50), // Limiter la taille maximale
      // Nettoyer les entrées de texte
      name = name.map(_.take(100).trim).filter(_.nonEmpty),
      hasIngredient = hasIngredient.map(_.take(50).trim).filter(_.nonEmpty),
      hasHop = hasHop.map(_.take(50).trim).filter(_.nonEmpty),
      hasMalt = hasMalt.map(_.take(50).trim).filter(_.nonEmpty),
      hasYeast = hasYeast.map(_.take(50).trim).filter(_.nonEmpty),
      tags = tags.take(10).map(_.take(30).trim).filter(_.nonEmpty)
    )
  }
}

object RecipePublicFilter {
  def empty: RecipePublicFilter = RecipePublicFilter()
  
  /**
   * Filtre pour découverte de recettes débutants
   */
  def forBeginners: RecipePublicFilter = RecipePublicFilter(
    difficulty = Some(DifficultyLevel.Beginner),
    brewingTimeMax = Some(6),
    commonIngredients = Some(true),
    sortBy = RecipeSortBy.Popular
  )
  
  /**
   * Filtre pour recettes rapides
   */
  def forQuickBrewing: RecipePublicFilter = RecipePublicFilter(
    brewingTimeMax = Some(4),
    fermentationTimeMax = Some(14),
    sortBy = RecipeSortBy.BrewingTime
  )
  
  /**
   * Filtre pour recettes populaires
   */
  def forPopular: RecipePublicFilter = RecipePublicFilter(
    minPopularity = Some(0.7),
    sortBy = RecipeSortBy.Popular
  )
  
  /**
   * Filtre pour un style spécifique
   */
  def forStyle(styleName: String): RecipePublicFilter = RecipePublicFilter(
    style = Some(styleName),
    sortBy = RecipeSortBy.StyleCompliance
  )
  
  implicit val recipeSortByFormat: Format[RecipeSortBy] = Format(
    Reads(js => js.validate[String].flatMap(s => RecipeSortBy.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid sort")))),
    Writes(sort => JsString(sort.name))
  )
  implicit val sortDirectionFormat: Format[SortDirection] = Format(
    Reads(js => js.validate[String].flatMap(s => SortDirection.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid direction")))),
    Writes(dir => JsString(dir.name))
  )
  implicit val volumeUnitFormat: Format[VolumeUnit] = VolumeUnit.format
  implicit val seasonFormat: Format[Season] = Season.format
  implicit val fermentationTypeFormat: Format[FermentationType] = FermentationType.format
  
  implicit val format: Format[RecipePublicFilter] = Format(
    // Read seulement les champs obligatoires, les autres auront leurs valeurs par défaut
    Reads { json =>
      for {
        name <- (json \ "name").validateOpt[String]
        page <- (json \ "page").validateOpt[Int]
        size <- (json \ "size").validateOpt[Int]
      } yield RecipePublicFilter(
        name = name,
        page = page.getOrElse(0),
        size = size.getOrElse(20)
      )
    },
    // Write tous les champs non-default
    Writes { filter =>
      Json.obj(
        "name" -> filter.name,
        "page" -> filter.page,
        "size" -> filter.size
      ).deepMerge(
        Json.obj("sortBy" -> filter.sortBy.name) ++
        (if(filter.sortDirection != SortDirection.Desc) Json.obj("sortDirection" -> filter.sortDirection.name) else Json.obj())
      )
    }
  )
}

/**
 * Niveaux de difficulté pour les recettes
 */
sealed trait DifficultyLevel {
  def name: String
  def description: String
  def order: Int
}

object DifficultyLevel {
  case object Beginner extends DifficultyLevel {
    val name = "beginner"
    val description = "Recettes simples pour débuter"
    val order = 1
  }
  
  case object Intermediate extends DifficultyLevel {
    val name = "intermediate" 
    val description = "Recettes avec quelques techniques avancées"
    val order = 2
  }
  
  case object Advanced extends DifficultyLevel {
    val name = "advanced"
    val description = "Recettes complexes nécessitant de l'expérience"
    val order = 3
  }
  
  case object Expert extends DifficultyLevel {
    val name = "expert"
    val description = "Recettes très techniques pour experts"
    val order = 4
  }
  
  case object All extends DifficultyLevel {
    val name = "all"
    val description = "Tous niveaux"
    val order = 0
  }
  
  val all = List(Beginner, Intermediate, Advanced, Expert, All)
  
  def fromString(s: String): Option[DifficultyLevel] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[DifficultyLevel] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(level) => JsSuccess(level)
          case None => JsError(s"Invalid difficulty level: $s")
        }
      }
    },
    Writes { level => JsString(level.name) }
  )
}

/**
 * Types d'équipement pour le brassage
 */
sealed trait EquipmentType {
  def name: String
  def description: String
}

object EquipmentType {
  case object Extract extends EquipmentType {
    val name = "extract"
    val description = "Kit extrait - équipement minimal"
  }
  
  case object PartialMash extends EquipmentType {
    val name = "partial_mash"
    val description = "Empâtage partiel"
  }
  
  case object AllGrain extends EquipmentType {
    val name = "all_grain"
    val description = "Tout grain - équipement complet"
  }
  
  case object BIAB extends EquipmentType {
    val name = "biab"
    val description = "Brew in a Bag"
  }
  
  val all = List(Extract, PartialMash, AllGrain, BIAB)
  
  def fromString(s: String): Option[EquipmentType] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[EquipmentType] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(equipmentType) => JsSuccess(equipmentType)
          case None => JsError(s"Invalid equipment type: $s")
        }
      }
    },
    Writes { equipmentType => JsString(equipmentType.name) }
  )
}

/**
 * Méthodes de brassage
 */
sealed trait BrewingMethod {
  def name: String
  def description: String
}

object BrewingMethod {
  case object Traditional extends BrewingMethod {
    val name = "traditional"
    val description = "Méthode traditionnelle"
  }
  
  case object NoBoil extends BrewingMethod {
    val name = "no_boil"
    val description = "Sans ébullition"
  }
  
  case object ColdBrew extends BrewingMethod {
    val name = "cold_brew"
    val description = "Brassage à froid"
  }
  
  case object Decoction extends BrewingMethod {
    val name = "decoction"
    val description = "Décoction"
  }
  
  val all = List(Traditional, NoBoil, ColdBrew, Decoction)
  
  def fromString(s: String): Option[BrewingMethod] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[BrewingMethod] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(method) => JsSuccess(method)
          case None => JsError(s"Invalid brewing method: $s")
        }
      }
    },
    Writes { method => JsString(method.name) }
  )
}

/**
 * Types de fermentation
 */
sealed trait FermentationType {
  def name: String
  def description: String
}

object FermentationType {
  case object Primary extends FermentationType {
    val name = "primary"
    val description = "Fermentation primaire uniquement"
  }
  
  case object Secondary extends FermentationType {
    val name = "secondary"
    val description = "Fermentation secondaire"
  }
  
  case object Mixed extends FermentationType {
    val name = "mixed"
    val description = "Fermentation mixte"
  }
  
  case object Wild extends FermentationType {
    val name = "wild"
    val description = "Fermentation sauvage"
  }
  
  val all = List(Primary, Secondary, Mixed, Wild)
  
  def fromString(s: String): Option[FermentationType] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[FermentationType] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(fermentationType) => JsSuccess(fermentationType)
          case None => JsError(s"Invalid fermentation type: $s")
        }
      }
    },
    Writes { fermentationType => JsString(fermentationType.name) }
  )
}

/**
 * Niveaux de budget
 */
sealed trait BudgetLevel {
  def name: String
  def description: String
  def multiplier: Double
}

object BudgetLevel {
  case object Budget extends BudgetLevel {
    val name = "budget"
    val description = "Ingrédients économiques"
    val multiplier = 0.7
  }
  
  case object Standard extends BudgetLevel {
    val name = "standard"
    val description = "Ingrédients standards"
    val multiplier = 1.0
  }
  
  case object Premium extends BudgetLevel {
    val name = "premium"
    val description = "Ingrédients premium"
    val multiplier = 1.5
  }
  
  val all = List(Budget, Standard, Premium)
  
  def fromString(s: String): Option[BudgetLevel] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[BudgetLevel] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(level) => JsSuccess(level)
          case None => JsError(s"Invalid budget level: $s")
        }
      }
    },
    Writes { level => JsString(level.name) }
  )
}

/**
 * Options de tri pour les recettes
 */
sealed trait RecipeSortBy {
  def name: String
  def description: String
  def requiresScoring: Boolean = false
}

object RecipeSortBy {
  case object Relevance extends RecipeSortBy {
    val name = "relevance"
    val description = "Pertinence (avec recherche textuelle)"
    override val requiresScoring = true
  }
  
  case object Popular extends RecipeSortBy {
    val name = "popular"
    val description = "Popularité"
  }
  
  case object Recent extends RecipeSortBy {
    val name = "recent"
    val description = "Plus récents"
  }
  
  case object Trending extends RecipeSortBy {
    val name = "trending"
    val description = "Tendances actuelles"
    override val requiresScoring = true
  }
  
  case object Seasonal extends RecipeSortBy {
    val name = "seasonal"
    val description = "Saisonnier"
    override val requiresScoring = true
  }
  
  case object Difficulty extends RecipeSortBy {
    val name = "difficulty"
    val description = "Difficulté croissante"
  }
  
  case object BrewingTime extends RecipeSortBy {
    val name = "brewing_time"
    val description = "Temps de brassage"
  }
  
  case object Abv extends RecipeSortBy {
    val name = "abv"
    val description = "Teneur en alcool"
  }
  
  case object StyleCompliance extends RecipeSortBy {
    val name = "style_compliance"
    val description = "Conformité au style"
    override val requiresScoring = true
  }
  
  val all = List(
    Relevance, Popular, Recent, Trending, Seasonal,
    Difficulty, BrewingTime, Abv, StyleCompliance
  )
  
  def fromString(s: String): Option[RecipeSortBy] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[RecipeSortBy] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(sortBy) => JsSuccess(sortBy)
          case None => JsError(s"Invalid sort option: $s")
        }
      }
    },
    Writes { sortBy => JsString(sortBy.name) }
  )
}

/**
 * Direction de tri
 */
sealed trait SortDirection {
  def name: String
}

object SortDirection {
  case object Asc extends SortDirection {
    val name = "asc"
  }
  
  case object Desc extends SortDirection {
    val name = "desc"
  }
  
  val all = List(Asc, Desc)
  
  def fromString(s: String): Option[SortDirection] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  implicit val format: Format[SortDirection] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(direction) => JsSuccess(direction)
          case None => JsError(s"Invalid sort direction: $s")
        }
      }
    },
    Writes { direction => JsString(direction.name) }
  )
}

/**
 * Catégories de découverte
 */
sealed trait DiscoveryCategory {
  def name: String
  def description: String
}

object DiscoveryCategory {
  case object Trending extends DiscoveryCategory {
    val name = "trending"
    val description = "Recettes en tendance"
  }
  
  case object Recent extends DiscoveryCategory {
    val name = "recent"
    val description = "Dernières ajoutées"
  }
  
  case object Popular extends DiscoveryCategory {
    val name = "popular"
    val description = "Plus populaires"
  }
  
  case object Seasonal extends DiscoveryCategory {
    val name = "seasonal"
    val description = "Saisonnières"
  }
  
  case object BeginnerFriendly extends DiscoveryCategory {
    val name = "beginner_friendly"
    val description = "Pour débutants"
  }
  
  case object Advanced extends DiscoveryCategory {
    val name = "advanced"
    val description = "Avancées"
  }
  
  val all = List(Trending, Recent, Popular, Seasonal, BeginnerFriendly, Advanced)
  
  def fromString(s: String): Option[DiscoveryCategory] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
}

/**
 * Raisons de recherche d'alternatives
 */
sealed trait AlternativeReason {
  def name: String
  def description: String
}

object AlternativeReason {
  case object UnavailableIngredients extends AlternativeReason {
    val name = "unavailable_ingredients"
    val description = "Ingrédients non disponibles"
  }
  
  case object TooExpensive extends AlternativeReason {
    val name = "too_expensive"
    val description = "Trop cher"
  }
  
  case object DifferentEquipment extends AlternativeReason {
    val name = "different_equipment"
    val description = "Équipement différent"
  }
  
  case object TimeConstraints extends AlternativeReason {
    val name = "time_constraints"
    val description = "Contraintes de temps"
  }
  
  case object Experimentation extends AlternativeReason {
    val name = "experimentation"
    val description = "Expérimentation"
  }
  
  val all = List(UnavailableIngredients, TooExpensive, DifferentEquipment, TimeConstraints, Experimentation)
  
  def fromString(s: String): Option[AlternativeReason] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
}

/**
 * Inventaire d'ingrédients pour recommandations
 */
case class IngredientInventory(
  availableHops: List[String] = List.empty,
  availableMalts: List[String] = List.empty,
  availableYeasts: List[String] = List.empty,
  availableOthers: List[String] = List.empty
) {
  def isEmpty: Boolean = 
    availableHops.isEmpty && availableMalts.isEmpty && 
    availableYeasts.isEmpty && availableOthers.isEmpty
    
  def hasHop(hopName: String): Boolean = 
    availableHops.exists(_.toLowerCase.contains(hopName.toLowerCase))
    
  def hasMalt(maltName: String): Boolean = 
    availableMalts.exists(_.toLowerCase.contains(maltName.toLowerCase))
    
  def hasYeast(yeastName: String): Boolean = 
    availableYeasts.exists(_.toLowerCase.contains(yeastName.toLowerCase))
    
  def totalIngredients: Int = 
    availableHops.length + availableMalts.length + availableYeasts.length + availableOthers.length
}

object IngredientInventory {
  implicit val format: Format[IngredientInventory] = Json.format[IngredientInventory]
}

/**
 * Niveaux de brasseur pour recommandations de progression
 */
sealed trait BrewerLevel {
  def name: String
  def description: String
  def order: Int
}

object BrewerLevel {
  case object Beginner extends BrewerLevel {
    val name = "beginner"
    val description = "Débutant - premières bières"
    val order = 1
  }
  
  case object Intermediate extends BrewerLevel {
    val name = "intermediate"
    val description = "Intermédiaire - maîtrise les bases"
    val order = 2
  }
  
  case object Advanced extends BrewerLevel {
    val name = "advanced"
    val description = "Avancé - techniques avancées"
    val order = 3
  }
  
  case object Expert extends BrewerLevel {
    val name = "expert"
    val description = "Expert - maîtrise complète"
    val order = 4
  }
  
  val all = List(Beginner, Intermediate, Advanced, Expert)
  
  def fromString(s: String): Option[BrewerLevel] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
  
  def getNext(current: BrewerLevel): Option[BrewerLevel] = {
    all.find(_.order == current.order + 1)
  }
  
  implicit val format: Format[BrewerLevel] = Format(
    Reads { json =>
      json.validate[String].flatMap { s =>
        fromString(s) match {
          case Some(level) => JsSuccess(level)
          case None => JsError(s"Invalid brewer level: $s")
        }
      }
    },
    Writes { level => JsString(level.name) }
  )
}