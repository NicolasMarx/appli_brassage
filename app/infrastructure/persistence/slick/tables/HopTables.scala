// app/infrastructure/persistence/slick/tables/HopTables.scala
package infrastructure.persistence.slick.tables

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import java.time.Instant
import java.util.UUID

trait HopTables { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  // Case class représentant une ligne de la table hops
  case class HopRow(
                     id: String,
                     name: String,
                     alphaAcid: Double,
                     betaAcid: Option[Double],
                     originCode: String,
                     usage: String,
                     description: Option[String],
                     status: String,
                     source: String,
                     credibilityScore: Int,
                     createdAt: Instant,
                     updatedAt: Instant,
                     version: Int
                   )

  // Définition de la table hops
  class HopsTable(tag: Tag) extends Table[HopRow](tag, "hops") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def alphaAcid = column[Double]("alpha_acid")
    def betaAcid = column[Option[Double]]("beta_acid")
    def originCode = column[String]("origin_code")
    def usage = column[String]("usage")
    def description = column[Option[String]]("description")
    def status = column[String]("status")
    def source = column[String]("source")
    def credibilityScore = column[Int]("credibility_score")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
    def version = column[Int]("version")

    def * = (id, name, alphaAcid, betaAcid, originCode, usage, description,
      status, source, credibilityScore, createdAt, updatedAt, version) <> (HopRow.tupled, HopRow.unapply)
  }

  // Case class pour la table de liaison hop_aroma_profiles
  case class HopAromaProfileRow(
                                 hopId: String,
                                 aromaId: String,
                                 intensity: Int,
                                 createdAt: Instant
                               )

  // Définition de la table hop_aroma_profiles
  class HopAromaProfilesTable(tag: Tag) extends Table[HopAromaProfileRow](tag, "hop_aroma_profiles") {
    def hopId = column[String]("hop_id")
    def aromaId = column[String]("aroma_id")
    def intensity = column[Int]("intensity")
    def createdAt = column[Instant]("created_at")

    def pk = primaryKey("pk_hop_aroma", (hopId, aromaId))

    def * = (hopId, aromaId, intensity, createdAt) <> (HopAromaProfileRow.tupled, HopAromaProfileRow.unapply)
  }

  // Case class pour la table origins
  case class OriginRow(
                        id: String,
                        name: String,
                        region: Option[String],
                        description: Option[String],
                        createdAt: Instant
                      )

  // Définition de la table origins
  class OriginsTable(tag: Tag) extends Table[OriginRow](tag, "origins") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def region = column[Option[String]]("region")
    def description = column[Option[String]]("description")
    def createdAt = column[Instant]("created_at")

    def * = (id, name, region, description, createdAt) <> (OriginRow.tupled, OriginRow.unapply)
  }

  // Case class pour la table aroma_profiles
  case class AromaProfileRow(
                              id: String,
                              name: String,
                              category: Option[String],
                              description: Option[String],
                              createdAt: Instant
                            )

  // Définition de la table aroma_profiles
  class AromaProfilesTable(tag: Tag) extends Table[AromaProfileRow](tag, "aroma_profiles") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def category = column[Option[String]]("category")
    def description = column[Option[String]]("description")
    def createdAt = column[Instant]("created_at")

    def * = (id, name, category, description, createdAt) <> (AromaProfileRow.tupled, AromaProfileRow.unapply)
  }

  // Instances des tables
  lazy val hops = TableQuery[HopsTable]
  lazy val hopAromaProfiles = TableQuery[HopAromaProfilesTable]
  lazy val origins = TableQuery[OriginsTable]
  lazy val aromaProfiles = TableQuery[AromaProfilesTable]
}