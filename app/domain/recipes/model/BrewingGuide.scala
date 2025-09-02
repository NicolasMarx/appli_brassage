package domain.recipes.model

import java.time.LocalTime
import play.api.libs.json._

/**
 * Guide de brassage détaillé - Modèle du domaine
 */
case class BrewingGuide(
  recipe: RecipeAggregate,
  steps: List[BrewingStep],
  timeline: BrewingTimeline,
  equipment: List[String],
  tips: List[BrewingTip],
  troubleshooting: List[TroubleshootingItem]
)

case class BrewingStep(
  order: Int,
  phase: BrewingPhase,
  title: String,
  description: String,
  duration: Option[Int] = None, // minutes
  temperature: Option[Double] = None, // °C
  instructions: List[String],
  criticalPoints: List[String] = List.empty,
  tips: List[String] = List.empty
)

sealed trait BrewingPhase {
  def name: String
  def description: String
}

object BrewingPhase {
  case object Preparation extends BrewingPhase {
    val name = "preparation"
    val description = "Préparation et nettoyage"
  }
  case object Mashing extends BrewingPhase {
    val name = "mashing"
    val description = "Empâtage"
  }
  case object Lautering extends BrewingPhase {
    val name = "lautering"
    val description = "Filtration"
  }
  case object Boiling extends BrewingPhase {
    val name = "boiling"
    val description = "Ébullition"
  }
  case object Cooling extends BrewingPhase {
    val name = "cooling"
    val description = "Refroidissement"
  }
  case object Fermentation extends BrewingPhase {
    val name = "fermentation"
    val description = "Fermentation"
  }
  case object Packaging extends BrewingPhase {
    val name = "packaging"
    val description = "Embouteillage/Mise en fût"
  }
  
  val all = List(Preparation, Mashing, Lautering, Boiling, Cooling, Fermentation, Packaging)
  
  def fromString(s: String): Option[BrewingPhase] = {
    all.find(_.name.equalsIgnoreCase(s))
  }
}

case class BrewingTimeline(
  preparationTasks: List[TimelineTask],
  brewingDay: List[TimelineTask],
  postBrewing: List[TimelineTask]
)

case class TimelineTask(
  time: LocalTime,
  task: String,
  duration: Int, // minutes
  phase: BrewingPhase,
  priority: TaskPriority = TaskPriority.Normal
)

sealed trait TaskPriority {
  def name: String
}

object TaskPriority {
  case object Critical extends TaskPriority { val name = "critical" }
  case object High extends TaskPriority { val name = "high" }
  case object Normal extends TaskPriority { val name = "normal" }
  case object Low extends TaskPriority { val name = "low" }
  
  def fromString(s: String): Option[TaskPriority] = s.toLowerCase match {
    case "critical" => Some(Critical)
    case "high" => Some(High)
    case "normal" => Some(Normal)
    case "low" => Some(Low)
    case _ => None
  }
}

case class BrewingTip(
  category: TipCategory,
  title: String,
  description: String,
  importance: TipImportance,
  phase: Option[BrewingPhase] = None
)

sealed trait TipCategory {
  def name: String
}

object TipCategory {
  case object Safety extends TipCategory { val name = "safety" }
  case object Quality extends TipCategory { val name = "quality" }
  case object Efficiency extends TipCategory { val name = "efficiency" }
  case object Troubleshooting extends TipCategory { val name = "troubleshooting" }
  case object Technique extends TipCategory { val name = "technique" }
  
  def fromString(s: String): Option[TipCategory] = s.toLowerCase match {
    case "safety" => Some(Safety)
    case "quality" => Some(Quality)
    case "efficiency" => Some(Efficiency)
    case "troubleshooting" => Some(Troubleshooting)
    case "technique" => Some(Technique)
    case _ => None
  }
}

sealed trait TipImportance {
  def name: String
  def level: Int
}

object TipImportance {
  case object Critical extends TipImportance { val name = "critical"; val level = 3 }
  case object Important extends TipImportance { val name = "important"; val level = 2 }
  case object Helpful extends TipImportance { val name = "helpful"; val level = 1 }
  
  def fromString(s: String): Option[TipImportance] = s.toLowerCase match {
    case "critical" => Some(Critical)
    case "important" => Some(Important)
    case "helpful" => Some(Helpful)
    case _ => None
  }
}

case class TroubleshootingItem(
  problem: String,
  symptoms: List[String],
  causes: List[String],
  solutions: List[String],
  prevention: List[String]
)

object BrewingGuide {
  implicit val brewingPhaseFormat: Format[BrewingPhase] = Format(
    Reads(js => js.validate[String].flatMap(s => BrewingPhase.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid phase")))),
    Writes(phase => JsString(phase.name))
  )
  implicit val taskPriorityFormat: Format[TaskPriority] = Format(
    Reads(js => js.validate[String].flatMap(s => TaskPriority.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid priority")))),
    Writes(priority => JsString(priority.name))
  )
  implicit val tipCategoryFormat: Format[TipCategory] = Format(
    Reads(js => js.validate[String].flatMap(s => TipCategory.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid category")))),
    Writes(category => JsString(category.name))
  )
  implicit val tipImportanceFormat: Format[TipImportance] = Format(
    Reads(js => js.validate[String].flatMap(s => TipImportance.fromString(s).map(JsSuccess(_)).getOrElse(JsError("Invalid importance")))),
    Writes(importance => JsString(importance.name))
  )
  
  implicit val brewingStepFormat: Format[BrewingStep] = Json.format[BrewingStep]
  implicit val timelineTaskFormat: Format[TimelineTask] = Json.format[TimelineTask]
  implicit val brewingTimelineFormat: Format[BrewingTimeline] = Json.format[BrewingTimeline]
  implicit val brewingTipFormat: Format[BrewingTip] = Json.format[BrewingTip]
  implicit val troubleshootingItemFormat: Format[TroubleshootingItem] = Json.format[TroubleshootingItem]
  
  implicit val format: Format[BrewingGuide] = Json.format[BrewingGuide]
}