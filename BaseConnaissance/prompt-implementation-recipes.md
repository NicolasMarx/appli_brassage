# 🍺 Prompt d'Implémentation - Domaine Recipes

## 🎯 Mission Ultra-Précise

Implémenter le domaine **Recipes** en suivant EXACTEMENT les patterns, conventions et architecture établis dans le projet My-brew-app-V2. L'objectif est d'atteindre une **probabilité de compilation de 100% au premier coup** en respectant scrupuleusement tous les standards du codebase existant.

---

## 📋 Contexte Architectural Existant

### **Architecture DDD/CQRS Établie**
```scala
// Pattern exacte utilisé dans le projet
domain/
├── [DOMAIN]/
│   ├── model/
│   │   ├── [DOMAIN]Aggregate.scala      // Agrégat racine
│   │   ├── [DOMAIN]Id.scala             // Identity Value Object
│   │   ├── [DOMAIN]Events.scala         // Domain Events
│   │   └── [ValueObjects].scala         // Value Objects métier
│   ├── repositories/
│   │   ├── [DOMAIN]Repository.scala     // Interface générique
│   │   ├── [DOMAIN]ReadRepository.scala // Interface lecture
│   │   └── [DOMAIN]WriteRepository.scala// Interface écriture
│   └── services/
│       └── [DOMAIN]DomainService.scala  // Logique métier complexe

application/
├── [DOMAIN]/
│   ├── commands/
│   │   └── [DOMAIN]Commands.scala       // Commands avec validation
│   ├── queries/
│   │   └── [DOMAIN]Queries.scala        // Queries READ
│   ├── handlers/
│   │   ├── [DOMAIN]CommandHandler.scala // Business logic
│   │   └── [DOMAIN]QueryHandler.scala   // Read logic
│   ├── services/
│   │   └── [DOMAIN]ApplicationService.scala // Orchestration
│   └── dtos/
│       └── [DOMAIN]DTOs.scala           // Data Transfer Objects

infrastructure/
├── persistence/slick/
│   ├── tables/[DOMAIN]Tables.scala      // Tables Slick
│   └── repositories/Slick[DOMAIN]Repository.scala // Implémentation

interfaces/controllers/
├── [DOMAIN]/
│   ├── [DOMAIN]PublicController.scala   // API publique
│   └── [DOMAIN]AdminController.scala    // API admin sécurisée
```

### **Patterns JSON Obligatoires**
```scala
// EXACTEMENT comme dans YeastDTOs.scala - lignes 48-52
object YeastDTOs {
  implicit val responseFormat: Format[YeastResponseDTO] = Json.format[YeastResponseDTO]
  implicit val listResponseFormat: Format[YeastListResponseDTO] = Json.format[YeastListResponseDTO]
  implicit val searchRequestFormat: Format[YeastSearchRequestDTO] = Json.format[YeastSearchRequestDTO]
}

// Dans les commands - EXACTEMENT comme CreateYeastCommand ligne 24
object CreateYeastCommand {
  implicit val format: Format[CreateYeastCommand] = Json.format[CreateYeastCommand]
}
```

### **Patterns Value Objects Obligatoires**
```scala
// EXACTEMENT comme YeastName.scala - lignes 8-20
case class YeastName private(value: String) extends AnyVal {
  override def toString: String = value
}

object YeastName {
  def create(name: String): Either[String, YeastName] = {
    if (name.trim.isEmpty) Left("Le nom ne peut pas être vide")
    else if (name.length < 2) Left("Le nom doit contenir au moins 2 caractères")
    else if (name.length > 100) Left("Le nom ne peut pas dépasser 100 caractères")
    else Right(YeastName(name.trim))
  }

  implicit val format: Format[YeastName] = Json.valueFormat[YeastName]
}
```

### **Patterns Repository Obligatoires**
```scala
// EXACTEMENT comme YeastRepository.scala - lignes 8-16
trait YeastRepository extends YeastReadRepository with YeastWriteRepository {
  // Interface combinée
}

trait YeastReadRepository {
  def findById(id: YeastId): Future[Option[YeastAggregate]]
  def findAll(filter: YeastFilter): Future[Seq[YeastAggregate]]
}

trait YeastWriteRepository {
  def save(yeast: YeastAggregate): Future[Either[String, YeastAggregate]]
  def delete(id: YeastId): Future[Either[String, Unit]]
}
```

---

## 🏗️ Spécifications Techniques Recipes

### **Phase 1 - Domain Model (Priority 1)**

#### **RecipeAggregate.scala** - Agrégat Racine
```scala
package domain.recipes.model

// RESPECTER EXACTEMENT cette structure (inspirée de YeastAggregate ligne 10-35)
case class RecipeAggregate(
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
  version: Int = 1
) {
  
  // Méthodes métier OBLIGATOIRES
  def addHop(hop: RecipeHop): Either[String, RecipeAggregate] = ???
  def updateCalculations(): RecipeAggregate = ???
  def validateCompleteness(): Either[List[String], Unit] = ???
  def canBePublished(): Boolean = ???
  
  // Pattern Event Sourcing comme YeastAggregate
  def applyEvent(event: RecipeEvent): RecipeAggregate = event match {
    case RecipeCreated(_, name, description, style) => 
      this.copy(name = name, description = description, style = style)
    // ... autres events
  }
}
```

#### **RecipeId.scala** - Identity
```scala
// EXACTEMENT comme YeastId.scala - lignes 6-16
case class RecipeId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object RecipeId {
  def generate(): RecipeId = RecipeId(UUID.randomUUID())
  
  def fromString(str: String): Either[String, RecipeId] = {
    Try(UUID.fromString(str)).toEither
      .left.map(_ => "Format UUID invalide")
      .map(RecipeId(_))
  }
  
  implicit val format: Format[RecipeId] = Json.valueFormat[RecipeId]
}
```

#### **RecipeName.scala** - Value Object Nom
```scala
// EXACTEMENT comme YeastName.scala pattern - lignes 8-22
case class RecipeName private(value: String) extends AnyVal {
  override def toString: String = value
}

object RecipeName {
  def create(name: String): Either[String, RecipeName] = {
    if (name.trim.isEmpty) Left("Le nom de la recette ne peut pas être vide")
    else if (name.length < 3) Left("Le nom doit contenir au moins 3 caractères") 
    else if (name.length > 200) Left("Le nom ne peut pas dépasser 200 caractères")
    else if (!name.matches("^[a-zA-Z0-9\\s\\-_àáâãäåæçèéêëìíîïðñòóôõöøùúûüý]*$")) {
      Left("Le nom contient des caractères non autorisés")
    }
    else Right(RecipeName(name.trim))
  }

  implicit val format: Format[RecipeName] = Json.valueFormat[RecipeName]
}
```

### **Phase 2 - Commands & Application Layer**

#### **RecipeCommands.scala** - Commands avec Validation
```scala
package application.recipes.commands

// Pattern EXACT de CreateYeastCommand lignes 8-25
case class CreateRecipeCommand(
  name: String,
  description: Option[String],
  styleId: String,
  batchSizeValue: Double,
  batchSizeUnit: String,
  hops: List[CreateRecipeHopCommand] = List.empty,
  malts: List[CreateRecipeMaltCommand] = List.empty,
  yeastId: Option[String] = None
) {
  
  // Validation métier OBLIGATOIRE
  def validate(): Either[List[String], Unit] = {
    val errors = List.newBuilder[String]
    
    if (name.trim.isEmpty) errors += "Nom requis"
    if (batchSizeValue <= 0) errors += "Volume doit être positif"
    if (malts.isEmpty) errors += "Au moins un malt requis"
    
    val allErrors = errors.result()
    if (allErrors.nonEmpty) Left(allErrors) else Right(())
  }
}

object CreateRecipeCommand {
  implicit val format: Format[CreateRecipeCommand] = Json.format[CreateRecipeCommand]
}

// Commands liées aux ingrédients
case class CreateRecipeHopCommand(
  hopId: String,
  quantityGrams: Double,
  additionTime: Int, // minutes
  usage: String // "Boil", "Aroma", "DryHop"
)

object CreateRecipeHopCommand {
  implicit val format: Format[CreateRecipeHopCommand] = Json.format[CreateRecipeHopCommand]
}
```

#### **RecipeApplicationService.scala** - Orchestration
```scala
package application.recipes.services

// Pattern EXACT de YeastApplicationService lignes 16-45
@Singleton
class RecipeApplicationService @Inject()(
  recipeRepository: RecipeRepository,
  recipeValidationService: RecipeValidationService,
  recipeCalculationService: RecipeCalculationService,
  hopRepository: HopReadRepository,
  maltRepository: MaltReadRepository,
  yeastRepository: YeastReadRepository
)(implicit ec: ExecutionContext) {
  
  def createRecipe(command: CreateRecipeCommand): Future[Either[List[String], RecipeAggregate]] = {
    for {
      // 1. Validation command
      _ <- Future.fromTry(command.validate().toTry)
      
      // 2. Validation métier domaine
      validatedRecipe <- validateRecipeCreation(command)
      
      // 3. Calculs automatiques
      recipeWithCalculations <- Future.successful(
        recipeCalculationService.calculateAll(validatedRecipe)
      )
      
      // 4. Persistence
      result <- recipeRepository.save(recipeWithCalculations)
      
    } yield result
  }
  
  def getRecipeById(id: RecipeId): Future[Option[RecipeAggregate]] = {
    recipeRepository.findById(id)
  }
  
  // Méthodes CRUD complètes obligatoires
  def updateRecipe(command: UpdateRecipeCommand): Future[Either[List[String], RecipeAggregate]] = ???
  def deleteRecipe(command: DeleteRecipeCommand): Future[Either[String, Unit]] = ???
  def findRecipes(search: RecipeSearchRequestDTO): Future[Either[List[String], RecipeListResponseDTO]] = ???
}
```

### **Phase 3 - Infrastructure & Persistence**

#### **RecipeTables.scala** - Tables Slick
```scala
package infrastructure.persistence.slick.tables

// Pattern EXACT des tables Yeast - YeastEventRow.scala structure
case class RecipeRow(
  id: String,
  name: String,
  description: Option[String],
  styleId: String,
  batchSizeValue: Double,
  batchSizeUnit: String,
  status: String,
  calculationsJson: String, // JSONB pour calculs
  proceduresJson: String,   // JSONB pour procédures
  createdAt: Instant,
  updatedAt: Instant,
  createdBy: String,
  version: Int
)

// Tables ingrédients liées
case class RecipeHopRow(
  id: String,
  recipeId: String,
  hopId: String,
  quantityGrams: Double,
  additionTimeMinutes: Int,
  usage: String
)

// Table Slick avec pattern exact du projet
class RecipeTable(tag: Tag) extends Table[RecipeRow](tag, "recipes") {
  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def description = column[Option[String]]("description")
  def styleId = column[String]("style_id")
  def batchSizeValue = column[Double]("batch_size_value")
  def batchSizeUnit = column[String]("batch_size_unit")
  def status = column[String]("status")
  def calculationsJson = column[String]("calculations_json")
  def proceduresJson = column[String]("procedures_json")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def createdBy = column[String]("created_by")
  def version = column[Int]("version")
  
  def * = (id, name, description, styleId, batchSizeValue, batchSizeUnit, status, calculationsJson, proceduresJson, createdAt, updatedAt, createdBy, version) <> (RecipeRow.tupled, RecipeRow.unapply)
}
```

### **Phase 4 - Controllers & API**

#### **RecipeAdminController.scala** - API Admin
```scala
package interfaces.controllers.recipes

// Pattern EXACT de SimpleYeastAdminController lignes 18-40
@Singleton
class RecipeAdminController @Inject()(
  cc: ControllerComponents,
  recipeApplicationService: RecipeApplicationService,
  auth: SimpleAuth,
  recipeDTOs: RecipeDTOs
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // CRUD complet avec pattern exact YeastAdmin
  def list(page: Int = 0, size: Int = 20): Action[AnyContent] = auth.secure {
    Action.async { request =>
      val searchRequest = RecipeSearchRequestDTO(
        name = None,
        style = None,
        author = None,
        page = page,
        size = size
      )
      
      recipeApplicationService.findRecipes(searchRequest).map {
        case Right(response) => Ok(Json.toJson(response)(RecipeDTOs.listResponseFormat))
        case Left(errors) => InternalServerError(Json.obj("errors" -> errors))
      }
    }
  }

  def create(): Action[JsValue] = auth.secure {
    Action.async(parse.json) { request =>
      request.body.validate[CreateRecipeCommand] match {
        case JsSuccess(command, _) =>
          recipeApplicationService.createRecipe(command).map {
            case Left(errors) => BadRequest(Json.obj("errors" -> errors))
            case Right(recipe) => 
              val recipeResponse = recipeDTOs.fromAggregate(recipe)
              Created(Json.toJson(recipeResponse)(RecipeDTOs.responseFormat))
          }
        case JsError(errors) =>
          Future.successful(BadRequest(Json.obj("errors" -> JsError.toJson(errors))))
      }
    }
  }
}
```

---

## 🎯 Checklist Compilation 100%

### **Imports & Packages - CRITIQUE**
```scala
// Respecter EXACTEMENT ces imports dans chaque fichier
import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

// Domain imports avec chemin exact
import domain.recipes.model._
import domain.recipes.repositories._
import application.recipes.services._
import application.recipes.commands._
import infrastructure.auth.SimpleAuth
```

### **Pattern Matching - OBLIGATOIRE**
```scala
// Pattern EXACT du projet (comme YeastEvents.scala lignes 40-50)
event match {
  case RecipeCreated(id, name, description, style) => 
    // Logique
  case RecipeUpdated(id, changes) =>
    // Logique  
  case _ => 
    // Cas par défaut OBLIGATOIRE
}

// Either pattern EXACT (comme partout dans le projet)
result match {
  case Left(errors) => BadRequest(Json.obj("errors" -> errors))
  case Right(value) => Ok(Json.toJson(value))
}
```

### **Déclaration Variables - Standards Projet**
```scala
// Pattern val/var EXACT du projet
val recipeId = RecipeId.generate()                    // Immutable préféré
val searchRequest = RecipeSearchRequestDTO(...)       // Construction immédiate
val errors = List.newBuilder[String]                  // Builder pour listes

// Future/Option patterns exacts
def findById(id: RecipeId): Future[Option[RecipeAggregate]] = {
  // Implementation
}

// Either pour validation - pattern EXACT
def validate(): Either[List[String], Unit] = {
  val errors = List.newBuilder[String]
  // ... validations
  val allErrors = errors.result()
  if (allErrors.nonEmpty) Left(allErrors) else Right(())
}
```

### **JSON Serialization - CRITIQUE**
```scala
// Format JSON EXACT - même pattern que YeastDTOs
object RecipeDTOs {
  implicit val responseFormat: Format[RecipeResponseDTO] = Json.format[RecipeResponseDTO]
  implicit val listResponseFormat: Format[RecipeListResponseDTO] = Json.format[RecipeListResponseDTO]
  implicit val searchRequestFormat: Format[RecipeSearchRequestDTO] = Json.format[RecipeSearchRequestDTO]
}

// Dans Value Objects - pattern EXACT
object RecipeName {
  implicit val format: Format[RecipeName] = Json.valueFormat[RecipeName]
}

// Dans Commands - pattern EXACT  
object CreateRecipeCommand {
  implicit val format: Format[CreateRecipeCommand] = Json.format[CreateRecipeCommand]
}
```

---

## 🚨 Erreurs à Éviter Absolument

### **Erreurs de Compilation Fréquentes**
1. **Imports manquants** : Vérifier TOUS les imports nécessaires
2. **Pattern matching incomplet** : TOUJOURS avoir un cas `_`
3. **JSON formats oubliés** : Chaque case class DOIT avoir son format
4. **Either mal typé** : `Either[List[String], T]` pour erreurs multiples
5. **ExecutionContext manquant** : `(implicit ec: ExecutionContext)` PARTOUT
6. **Option/Future confusion** : Respecter les signatures exactes

### **Standards Nommage OBLIGATOIRES**
```scala
// Packages - lowercase
package domain.recipes.model
package application.recipes.commands

// Classes - PascalCase
class RecipeApplicationService
case class RecipeAggregate

// Méthodes/Variables - camelCase  
def createRecipe(command: CreateRecipeCommand)
val batchSizeValue: Double

// Constants - UPPER_CASE
val DEFAULT_BATCH_SIZE = 20.0
```

### **Architecture Layers - STRICT**
```scala
// JAMAIS mélanger les layers
// ❌ INTERDIT
class RecipeController @Inject()(recipeRepository: RecipeRepository) // Controller -> Repository direct

// ✅ CORRECT  
class RecipeController @Inject()(recipeApplicationService: RecipeApplicationService) // Controller -> Application -> Domain -> Infrastructure
```

---

## 🎯 Objectif Final

Créer le domaine Recipes qui:
1. **Compile au premier coup** (0 erreur)
2. **Respecte 100% l'architecture** DDD/CQRS existante
3. **Utilise exactement les mêmes patterns** que Hops/Malts/Yeasts
4. **Intègre parfaitement** avec l'infrastructure existante
5. **Maintient la cohérence** de style et conventions du codebase

**Success criteria**: `sbt compile` returns `success` without any error or warning.

---

**Ce prompt garantit une implémentation cohérente et compilable du domaine Recipes en suivant exactement les patterns établis dans le projet My-brew-app-V2.** 🍺