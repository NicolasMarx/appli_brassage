package infrastructure.persistence.slick.tables

import slick.jdbc.JdbcProfile
import java.time.Instant

trait HopTables {
  val profile: JdbcProfile
  import profile.api._

  // Classe représentant une ligne de la table hops
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
                     version: Long
                   )

  // Table houblons
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
    def version = column[Long]("version")

    def * = (id, name, alphaAcid, betaAcid, originCode, usage, description, status, source, credibilityScore, createdAt, updatedAt, version) .<> (HopRow.tupled, HopRow.unapply)
  }

  // Table origins
  case class OriginRow(
                        id: String,
                        name: String,
                        region: Option[String]
                      )

  class OriginsTable(tag: Tag) extends Table[OriginRow](tag, "origins") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def region = column[Option[String]]("region")

    def * = (id, name, region) .<> (OriginRow.tupled, OriginRow.unapply)
  }

  // Table aroma_profiles
  case class AromaProfileRow(
                              id: String,
                              name: String,
                              category: Option[String]
                            )

  class AromaProfilesTable(tag: Tag) extends Table[AromaProfileRow](tag, "aroma_profiles") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def category = column[Option[String]]("category")

    def * = (id, name, category) .<> (AromaProfileRow.tupled, AromaProfileRow.unapply)
  }

  // Table hop_aroma_profiles (liaison)
  case class HopAromaProfileRow(
                                 hopId: String,
                                 aromaId: String,
                                 intensity: Option[Int]
                               )

  class HopAromaProfilesTable(tag: Tag) extends Table[HopAromaProfileRow](tag, "hop_aroma_profiles") {
    def hopId = column[String]("hop_id")
    def aromaId = column[String]("aroma_id")
    def intensity = column[Option[Int]]("intensity")

    def * = (hopId, aromaId, intensity) .<> (HopAromaProfileRow.tupled, HopAromaProfileRow.unapply)
  }

  // TableQuery instances
  val hops = TableQuery[HopsTable]
  val origins = TableQuery[OriginsTable]
  val aromaProfiles = TableQuery[AromaProfilesTable]
  val hopAromaProfiles = TableQuery[HopAromaProfilesTable]
}