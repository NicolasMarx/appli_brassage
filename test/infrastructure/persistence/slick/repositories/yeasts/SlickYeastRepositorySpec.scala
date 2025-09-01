package infrastructure.persistence.slick.repositories.yeasts

import domain.yeasts.model._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.Helpers._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

class SlickYeastRepositorySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {
  
  implicit val timeout: Duration = Duration(5, TimeUnit.SECONDS)
  
  lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
      "slick.dbs.default.db.driver" -> "org.h2.Driver",
      "slick.dbs.default.db.url" -> "jdbc:h2:mem:test-yeast;DB_CLOSE_DELAY=-1",
      "slick.dbs.default.db.user" -> "",
      "slick.dbs.default.db.password" -> ""
    )
    .build()
  
  lazy val dbConfigProvider: DatabaseConfigProvider = app.injector.instanceOf[DatabaseConfigProvider]
  lazy val readRepository = new SlickYeastReadRepository(dbConfigProvider)(ExecutionContext.global)
  lazy val writeRepository = new SlickYeastWriteRepository(dbConfigProvider)(ExecutionContext.global)
  lazy val repository = new SlickYeastRepository(readRepository, writeRepository)(ExecutionContext.global)
  
  override def beforeEach(): Unit = {
    // Setup test database schema
    val profile = dbConfigProvider.get[JdbcProfile]
    import profile.api._
    
    val setupAction = sqlu"""
      CREATE TABLE IF NOT EXISTS yeasts (
        id UUID PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        laboratory VARCHAR(100) NOT NULL,
        strain VARCHAR(50) NOT NULL,
        yeast_type VARCHAR(20) NOT NULL,
        attenuation_min INTEGER NOT NULL,
        attenuation_max INTEGER NOT NULL,
        temperature_min INTEGER NOT NULL,
        temperature_max INTEGER NOT NULL,
        alcohol_tolerance DECIMAL(4,1) NOT NULL,
        flocculation VARCHAR(20) NOT NULL,
        characteristics TEXT NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
        version BIGINT NOT NULL DEFAULT 1,
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP NOT NULL
      )
    """
    
    val eventsSetupAction = sqlu"""
      CREATE TABLE IF NOT EXISTS yeast_events (
        id UUID PRIMARY KEY,
        yeast_id UUID NOT NULL,
        event_type VARCHAR(100) NOT NULL,
        event_data TEXT NOT NULL,
        version BIGINT NOT NULL,
        occurred_at TIMESTAMP NOT NULL,
        created_by UUID
      )
    """
    
    await(profile.db.run(setupAction))
    await(profile.db.run(eventsSetupAction))
  }
  
  override def afterEach(): Unit = {
    val profile = dbConfigProvider.get[JdbcProfile]
    import profile.api._
    
    val cleanupAction = DBIO.seq(
      sqlu"DELETE FROM yeast_events",
      sqlu"DELETE FROM yeasts"
    )
    
    await(profile.db.run(cleanupAction))
  }

  "SlickYeastRepository" should {
    
    "create and find yeast by id" in {
      val yeast = createTestYeast()
      
      val created = await(repository.create(yeast))
      created.id shouldBe yeast.id
      
      val found = await(repository.findById(yeast.id))
      found shouldBe defined
      found.get.name shouldBe yeast.name
      found.get.laboratory shouldBe yeast.laboratory
    }
    
    "update existing yeast" in {
      val yeast = createTestYeast()
      await(repository.create(yeast))
      
      val newName = YeastName("Updated Yeast").value
      val updatedYeast = yeast.updateName(newName, UUID.randomUUID()).value
      
      val updated = await(repository.update(updatedYeast))
      updated.name shouldBe newName
      
      val found = await(repository.findById(yeast.id))
      found.get.name shouldBe newName
    }
    
    "find by laboratory and strain" in {
      val yeast = createTestYeast()
      await(repository.create(yeast))
      
      val found = await(repository.findByLaboratoryAndStrain(yeast.laboratory, yeast.strain))
      found shouldBe defined
      found.get.id shouldBe yeast.id
    }
    
    "find by filter with pagination" in {
      val yeast1 = createTestYeast(name = "Yeast 1", laboratory = YeastLaboratory.Fermentis)
      val yeast2 = createTestYeast(name = "Yeast 2", laboratory = YeastLaboratory.Wyeast)
      
      await(repository.create(yeast1))
      await(repository.create(yeast2))
      
      val filter = YeastFilter(
        laboratory = Some(YeastLaboratory.Fermentis),
        page = 0,
        size = 10
      )
      
      val result = await(repository.findByFilter(filter))
      result.items should have length 1
      result.items.head.laboratory shouldBe YeastLaboratory.Fermentis
      result.totalCount shouldBe 1
    }
    
    "find by type" in {
      val aleYeast = createTestYeast(yeastType = YeastType.Ale)
      val lagerYeast = createTestYeast(yeastType = YeastType.Lager)
      
      await(repository.create(aleYeast))
      await(repository.create(lagerYeast))
      
      val aleResults = await(repository.findByType(YeastType.Ale))
      aleResults should have length 1
      aleResults.head.yeastType shouldBe YeastType.Ale
      
      val lagerResults = await(repository.findByType(YeastType.Lager))
      lagerResults should have length 1
      lagerResults.head.yeastType shouldBe YeastType.Lager
    }
    
    "change status" in {
      val yeast = createTestYeast()
      await(repository.create(yeast))
      
      await(repository.changeStatus(yeast.id, YeastStatus.Active))
      
      val found = await(repository.findById(yeast.id))
      found.get.status shouldBe YeastStatus.Active
    }
    
    "save and retrieve events" in {
      val yeast = createTestYeast()
      val events = yeast.getUncommittedEvents
      
      await(repository.saveEvents(yeast.id, events, 0))
      
      val retrievedEvents = await(repository.getEvents(yeast.id))
      retrievedEvents should have length events.length
    }
    
    "handle batch operations" in {
      val yeast1 = createTestYeast(name = "Batch Yeast 1")
      val yeast2 = createTestYeast(name = "Batch Yeast 2")
      val yeasts = List(yeast1, yeast2)
      
      val created = await(repository.createBatch(yeasts))
      created should have length 2
      
      val found1 = await(repository.findById(yeast1.id))
      val found2 = await(repository.findById(yeast2.id))
      
      found1 shouldBe defined
      found2 shouldBe defined
    }
  }
  
  private def createTestYeast(
    name: String = "Test Yeast",
    laboratory: YeastLaboratory = YeastLaboratory.Fermentis,
    yeastType: YeastType = YeastType.Ale
  ): YeastAggregate = {
    YeastAggregate.create(
      name = YeastName(name).value,
      laboratory = laboratory,
      strain = YeastStrain(s"T-${UUID.randomUUID().toString.take(8)}").value,
      yeastType = yeastType,
      attenuation = AttenuationRange(75, 82),
      temperature = FermentationTemp(18, 22),
      alcoholTolerance = AlcoholTolerance(9.0),
      flocculation = FlocculationLevel.Medium,
      characteristics = YeastCharacteristics.Clean,
      createdBy = UUID.randomUUID()
    ).value
  }
}
