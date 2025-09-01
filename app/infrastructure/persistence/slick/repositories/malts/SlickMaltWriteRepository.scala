package infrastructure.persistence.slick.repositories.malts

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

import domain.malts.model._
import domain.malts.repositories.MaltWriteRepository

@Singleton
class SlickMaltWriteRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
  extends MaltWriteRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  // Réutiliser la même structure de table que le ReadRepository
  case class MaltRow(
    id: UUID,
    name: String,
    maltType: String,
    ebcColor: Double,
    extractionRate: Double,
    diastaticPower: Double,
    originCode: String,
    description: Option[String],
    flavorProfiles: Option[String],
    source: String,
    isActive: Boolean,
    credibilityScore: Double,
    createdAt: Instant,
    updatedAt: Instant,
    version: Long
  )

  class MaltsTable(tag: Tag) extends Table[MaltRow](tag, "malts") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def maltType = column[String]("malt_type")
    def ebcColor = column[Double]("ebc_color")
    def extractionRate = column[Double]("extraction_rate")
    def diastaticPower = column[Double]("diastatic_power")
    def originCode = column[String]("origin_code")
    def description = column[Option[String]]("description")
    def flavorProfiles = column[Option[String]]("flavor_profiles")
    def source = column[String]("source")
    def isActive = column[Boolean]("is_active")
    def credibilityScore = column[Double]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Long]("version")

    def * = (id, name, maltType, ebcColor, extractionRate, diastaticPower, 
             originCode, description, flavorProfiles, source, isActive, 
             credibilityScore, createdAt, updatedAt, version).mapTo[MaltRow]
  }

  val malts = TableQuery[MaltsTable]

  private def aggregateToRow(aggregate: MaltAggregate): MaltRow = {
    val flavorProfilesJson = if (aggregate.flavorProfiles.nonEmpty) {
      Some(s"""[${aggregate.flavorProfiles.map(s => s""""$s"""").mkString(",")}]""")
    } else {
      None
    }

    MaltRow(
      id = aggregate.id.asUUID,  // Utiliser asUUID pour avoir le bon type
      name = aggregate.name.value,
      maltType = aggregate.maltType.name,
      ebcColor = aggregate.ebcColor.value,
      extractionRate = aggregate.extractionRate.value,
      diastaticPower = aggregate.diastaticPower.value,
      originCode = aggregate.originCode,
      description = aggregate.description,
      flavorProfiles = flavorProfilesJson,
      source = aggregate.source.name,
      isActive = aggregate.isActive,
      credibilityScore = aggregate.credibilityScore,
      createdAt = aggregate.createdAt,
      updatedAt = aggregate.updatedAt,
      version = aggregate.version
    )
  }

  override def save(malt: MaltAggregate): Future[Unit] = {
    val row = aggregateToRow(malt)
    val action = malts += row
    
    db.run(action).map(_ => ()).recover {
      case ex =>
        println(s"❌ Erreur sauvegarde malt ${malt.name.value}: ${ex.getMessage}")
        throw ex
    }
  }

  override def update(malt: MaltAggregate): Future[Unit] = {
    val row = aggregateToRow(malt)
    // Utiliser asUUID pour garantir la correspondance de type
    val action = malts.filter(_.id === malt.id.asUUID).update(row)
    
    db.run(action).map(_ => ()).recover {
      case ex =>
        println(s"❌ Erreur mise à jour malt ${malt.name.value}: ${ex.getMessage}")
        throw ex
    }
  }

  override def delete(id: MaltId): Future[Unit] = {
    // Utiliser asUUID pour garantir la correspondance de type
    val action = malts.filter(_.id === id.asUUID).delete
    
    db.run(action).map(_ => ()).recover {
      case ex =>
        println(s"❌ Erreur suppression malt ${id}: ${ex.getMessage}")
        throw ex
    }
  }
}
