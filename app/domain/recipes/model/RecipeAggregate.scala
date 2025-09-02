package domain.recipes.model

import domain.common.{DomainError, EventSourced}
import domain.shared.NonEmptyString
import play.api.libs.json._
import java.time.Instant

case class RecipeAggregate private (
  id: RecipeId,
  name: RecipeName,
  description: Option[RecipeDescription],
  style: BeerStyle,
  batchSize: BatchSize,
  status: RecipeStatus,
  
  // Ingrédients - Collections typées
  hops: List[RecipeHop],
  malts: List[RecipeMalt],
  yeast: Option[RecipeYeast],
  otherIngredients: List[OtherIngredient],
  
  // Calculs automatiques
  calculations: RecipeCalculations,
  
  // Procédures
  procedures: BrewingProcedures,
  
  // Métadonnées
  createdAt: Instant,
  updatedAt: Instant,
  createdBy: UserId,
  aggregateVersion: Int = 1
) extends EventSourced {
  
  // Méthodes métier OBLIGATOIRES
  def addHop(hop: RecipeHop): Either[String, RecipeAggregate] = {
    if (hops.exists(_.hopId == hop.hopId)) {
      Left("Ce houblon est déjà présent dans la recette")
    } else {
      Right(this.copy(
        hops = hops :+ hop,
        updatedAt = Instant.now(),
        aggregateVersion = aggregateVersion + 1
      ))
    }
  }
  
  def addMalt(malt: RecipeMalt): Either[String, RecipeAggregate] = {
    if (malts.exists(_.maltId == malt.maltId)) {
      Left("Ce malt est déjà présent dans la recette")
    } else {
      Right(this.copy(
        malts = malts :+ malt,
        updatedAt = Instant.now(),
        aggregateVersion = aggregateVersion + 1
      ))
    }
  }
  
  def setYeast(newYeast: RecipeYeast): RecipeAggregate = {
    this.copy(
      yeast = Some(newYeast),
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
  }
  
  def updateCalculations(newCalculations: RecipeCalculations): RecipeAggregate = {
    this.copy(
      calculations = newCalculations,
      updatedAt = Instant.now()
    )
  }
  
  /**
   * Met à jour les calculs avec un résultat de calcul avancé
   */
  def updateAdvancedCalculations(
    brewingResult: BrewingCalculationResult,
    waterResult: Option[WaterChemistryResult] = None
  ): RecipeAggregate = {
    val updatedCalculations = calculations.withAdvancedCalculations(brewingResult, waterResult)
    this.copy(
      calculations = updatedCalculations,
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
  }
  
  /**
   * Indique si la recette nécessite un recalcul des paramètres avancés
   */
  def needsAdvancedRecalculation(currentParametersHash: String): Boolean = {
    calculations.needsAdvancedRecalculation(currentParametersHash)
  }
  
  /**
   * Obtient un résumé des calculs de brassage
   */
  def calculationSummary: String = calculations.summary
  
  /**
   * Obtient les détails des calculs de brassage
   */
  def detailedCalculationSummary: String = calculations.detailedSummary
  
  /**
   * Indique si la recette a des avertissements de calcul
   */
  def hasCalculationWarnings: Boolean = calculations.hasWarnings
  
  /**
   * Obtient la liste des avertissements de calcul
   */
  def calculationWarnings: List[String] = calculations.warnings
  
  /**
   * Vérifie si la recette est équilibrée selon les calculs avancés
   */
  def isBalancedRecipe: Option[Boolean] = {
    calculations.brewingCalculationResult.map(_.isBalanced)
  }
  
  /**
   * Obtient la catégorie de force de la bière
   */
  def strengthCategory: Option[String] = {
    calculations.brewingCalculationResult.map(_.strengthCategory)
  }
  
  /**
   * Obtient la catégorie de couleur de la bière
   */
  def colorCategory: Option[String] = {
    calculations.brewingCalculationResult.map(_.colorCategory)
  }
  
  /**
   * Obtient la catégorie d'amertume de la bière
   */
  def bitternessCategory: Option[String] = {
    calculations.brewingCalculationResult.map(_.bitternessCategory)
  }
  
  def validateCompleteness(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (malts.isEmpty) errors += "Au moins un malt est requis"
    if (yeast.isEmpty) errors += "Une levure est requise"
    if (hops.isEmpty) errors += "Au moins un houblon est requis"
    if (batchSize.value <= 0) errors += "Le volume doit être positif"
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
  
  def canBePublished(): Boolean = {
    status == RecipeStatus.Draft && 
    validateCompleteness().isRight &&
    calculations.isComplete
  }
  
  def publish(publishedBy: UserId): Either[String, RecipeAggregate] = {
    if (!canBePublished()) {
      Left("La recette n'est pas complète ou ne peut pas être publiée")
    } else {
      Right(this.copy(
        status = RecipeStatus.Published,
        updatedAt = Instant.now(),
        aggregateVersion = aggregateVersion + 1
      ))
    }
  }
  
  def archive(archivedBy: UserId, reason: Option[String] = None): RecipeAggregate = {
    this.copy(
      status = RecipeStatus.Archived,
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
  }
  
  def getUncommittedEvents: List[RecipeEvent] = uncommittedEvents.asInstanceOf[List[RecipeEvent]]
  
  override def version: Int = aggregateVersion

  // Méthodes métier avec Event Sourcing
  def updateBasicInfo(
    name: Option[RecipeName] = None,
    description: Option[RecipeDescription] = None,
    style: Option[BeerStyle] = None,
    batchSize: Option[BatchSize] = None
  ): Either[DomainError, RecipeAggregate] = {
    val updated = this.copy(
      name = name.getOrElse(this.name),
      description = description.orElse(this.description),
      style = style.getOrElse(this.style),
      batchSize = batchSize.getOrElse(this.batchSize),
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
    updated.raise(RecipeUpdated(
      recipeId = id.asString,
      name = name.map(_.value),
      description = description.map(_.value),
      styleId = style.map(_.id),
      styleName = style.map(_.name),
      styleCategory = style.map(_.category),
      batchSizeValue = batchSize.map(_.value),
      batchSizeUnit = batchSize.map(_.unit.name),
      version = aggregateVersion + 1
    ))
    Right(updated)
  }

  def changeStatus(newStatus: RecipeStatus, changedBy: UserId, reason: Option[String] = None): Either[DomainError, RecipeAggregate] = {
    if (status == newStatus) {
      Left(DomainError.businessRule(s"La recette est déjà dans le statut ${newStatus.name}", "ALREADY_IN_STATUS"))
    } else {
      val updated = this.copy(
        status = newStatus,
        updatedAt = Instant.now(),
        aggregateVersion = aggregateVersion + 1
      )
      updated.raise(RecipeStatusChanged(
        recipeId = id.asString,
        oldStatus = status.name,
        newStatus = newStatus.name,
        reason = reason,
        changedBy = changedBy.asString,
        version = aggregateVersion + 1
      ))
      Right(updated)
    }
  }

  def addIngredient(ingredientType: String, ingredientId: String, quantity: Double, additionalData: Option[String] = None): Either[DomainError, RecipeAggregate] = {
    // Logique métier selon le type d'ingrédient
    val updated = this.copy(
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
    updated.raise(RecipeIngredientAdded(
      recipeId = id.asString,
      ingredientType = ingredientType,
      ingredientId = ingredientId,
      quantity = quantity,
      additionalData = additionalData,
      version = aggregateVersion + 1
    ))
    Right(updated)
  }

  def removeIngredient(ingredientType: String, ingredientId: String): Either[DomainError, RecipeAggregate] = {
    val updated = this.copy(
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
    updated.raise(RecipeIngredientRemoved(
      recipeId = id.asString,
      ingredientType = ingredientType,
      ingredientId = ingredientId,
      version = aggregateVersion + 1
    ))
    Right(updated)
  }

  def updateCalculations(
    originalGravity: Option[Double] = None,
    finalGravity: Option[Double] = None,
    abv: Option[Double] = None,
    ibu: Option[Double] = None,
    srm: Option[Double] = None,
    efficiency: Option[Double] = None
  ): RecipeAggregate = {
    val updated = this.copy(
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
    updated.raise(RecipeCalculationsUpdated(
      recipeId = id.asString,
      originalGravity = originalGravity,
      finalGravity = finalGravity,
      abv = abv,
      ibu = ibu,
      srm = srm,
      efficiency = efficiency,
      version = aggregateVersion + 1
    ))
    updated
  }

  def updateProcedure(procedureType: String, procedureData: String): RecipeAggregate = {
    val updated = this.copy(
      updatedAt = Instant.now(),
      aggregateVersion = aggregateVersion + 1
    )
    updated.raise(RecipeProcedureUpdated(
      recipeId = id.asString,
      procedureType = procedureType,
      procedureData = procedureData,
      version = aggregateVersion + 1
    ))
    updated
  }
  
  def applyEvent(event: RecipeEvent): RecipeAggregate = event match {
    case RecipeCreated(recipeId, name, description, styleId, styleName, styleCategory, batchSizeValue, batchSizeUnit, createdBy, _) =>
      // Pour l'application d'événements historiques - reconstruction d'agrégat
      this.copy(aggregateVersion = event.version)
    
    case RecipeUpdated(recipeId, nameOpt, descriptionOpt, styleIdOpt, styleNameOpt, styleCategoryOpt, batchSizeValueOpt, batchSizeUnitOpt, _) =>
      this.copy(
        updatedAt = event.occurredAt,
        aggregateVersion = event.version
      )
    
    case RecipeStatusChanged(recipeId, oldStatus, newStatus, reason, changedBy, _) =>
      this.copy(
        status = RecipeStatus.fromString(newStatus).getOrElse(status),
        updatedAt = event.occurredAt,
        aggregateVersion = event.version
      )
    
    case RecipeCalculationsUpdated(recipeId, og, fg, abv, ibu, srm, efficiency, _) =>
      this.copy(
        updatedAt = event.occurredAt,
        aggregateVersion = event.version
      )
    
    case _ => this.copy(aggregateVersion = event.version)
  }

  // Méthodes pour serialization JSON
  def toJson: JsValue = Json.obj(
    "id" -> id.asString,
    "name" -> name.value,
    "description" -> description.map(_.value),
    "style" -> Json.obj(
      "id" -> style.id,
      "name" -> style.name,
      "category" -> style.category
    ),
    "batchSize" -> Json.obj(
      "value" -> batchSize.value,
      "unit" -> batchSize.unit.name
    ),
    "status" -> status.name,
    "hops" -> Json.toJson(hops),
    "malts" -> Json.toJson(malts),
    "yeast" -> Json.toJson(yeast),
    "otherIngredients" -> Json.toJson(otherIngredients),
    "calculations" -> Json.toJson(calculations),
    "procedures" -> Json.toJson(procedures),
    "createdAt" -> createdAt.toString,
    "updatedAt" -> updatedAt.toString,
    "createdBy" -> createdBy.asString,
    "version" -> aggregateVersion
  )
}

object RecipeAggregate {
  def create(
    name: RecipeName,
    description: Option[RecipeDescription],
    style: BeerStyle,
    batchSize: BatchSize,
    createdBy: UserId
  ): RecipeAggregate = {
    val now = Instant.now()
    val id = RecipeId.generate()
    val recipe = RecipeAggregate(
      id = id,
      name = name,
      description = description,
      style = style,
      batchSize = batchSize,
      status = RecipeStatus.Draft,
      hops = List.empty,
      malts = List.empty,
      yeast = None,
      otherIngredients = List.empty,
      calculations = RecipeCalculations.empty,
      procedures = BrewingProcedures.empty,
      createdAt = now,
      updatedAt = now,
      createdBy = createdBy,
      aggregateVersion = 1
    )
    recipe.raise(RecipeCreated(
      recipeId = id.asString,
      name = name.value,
      description = description.map(_.value),
      styleId = style.id,
      styleName = style.name,
      styleCategory = style.category,
      batchSizeValue = batchSize.value,
      batchSizeUnit = batchSize.unit.name,
      createdBy = createdBy.asString,
      version = 1
    ))
    recipe
  }
  
  implicit val format: Format[RecipeAggregate] = Json.format[RecipeAggregate]
}