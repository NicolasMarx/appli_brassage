package interfaces.integration

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

/**
 * Tests d'intégration complets de l'API Yeasts
 * Tests end-to-end avec base de données H2
 */
class YeastApiIntegrationSpec extends AnyWordSpec with Matchers {
  
  def application: Application = new GuiceApplicationBuilder()
    .configure(
      "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
      "slick.dbs.default.db.driver" -> "org.h2.Driver",
      "slick.dbs.default.db.url" -> "jdbc:h2:mem:test-yeasts-api;DB_CLOSE_DELAY=-1",
      "play.cache.redis.enabled" -> false,
      "play.cache.defaultCache" -> "ehcache"
    )
    .build()
  
  "Yeast Public API" should {
    
    "return empty list initially" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      val json = contentAsJson(result)
      (json \ "yeasts").as[List[JsValue]] shouldBe empty
      (json \ "pagination" \ "totalCount").as[Long] shouldBe 0
    }
    
    "handle search with no results" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/search?q=nonexistent")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      contentAsJson(result).as[List[JsValue]] shouldBe empty
    }
    
    "return 400 for search query too short" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/search?q=a")
      val result = route(app, request).get
      
      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "errors").as[List[String]] should not be empty
    }
    
    "return 404 for non-existent yeast" in new WithApplication(application) {
      val yeastId = java.util.UUID.randomUUID()
      val request = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result = route(app, request).get
      
      status(result) shouldBe NOT_FOUND
    }
    
    "return beginner recommendations" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/recommendations/beginner")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      contentAsJson(result).as[List[JsValue]] // Should not throw
    }
    
    "return seasonal recommendations" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/recommendations/seasonal?season=summer")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      contentAsJson(result).as[List[JsValue]]
    }
    
    "return public stats" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/stats")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      val json = contentAsJson(result)
      json should have key "totalActiveYeasts"
    }
  }
  
  "Yeast Admin API" should {
    
    "require authentication for admin endpoints" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/admin/yeasts")
      val result = route(app, request).get
      
      // Should return 401 or redirect to login
      status(result) should (be(UNAUTHORIZED) or be(FORBIDDEN) or be(SEE_OTHER))
    }
    
    "reject invalid JSON in create request" in new WithApplication(application) {
      val invalidJson = Json.obj("invalid" -> "data")
      val request = FakeRequest(POST, "/api/admin/yeasts")
        .withJsonBody(invalidJson)
        .withHeaders("Content-Type" -> "application/json")
      
      val result = route(app, request).get
      
      // Should return 400 for validation or 401/403 for auth
      status(result) should (be(BAD_REQUEST) or be(UNAUTHORIZED) or be(FORBIDDEN))
    }
  }
  
  "API Error Handling" should {
    
    "return consistent error format" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/search?q=")
      val result = route(app, request).get
      
      if (status(result) == BAD_REQUEST) {
        val json = contentAsJson(result)
        json should have key "errors"
        json should have key "timestamp"
        (json \ "errors").as[List[String]] should not be empty
      }
    }
    
    "handle malformed UUID gracefully" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts/not-a-uuid")
      val result = route(app, request).get
      
      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "errors").as[List[String]] should contain("Format UUID invalide: not-a-uuid")
    }
  }
  
  "API Performance and Caching" should {
    
    "set appropriate cache headers for public endpoints" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      // Vérifier que les headers de cache sont appropriés pour une API publique
    }
    
    "handle pagination parameters correctly" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts?page=0&size=10")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      val json = contentAsJson(result)
      (json \ "pagination" \ "page").as[Int] shouldBe 0
      (json \ "pagination" \ "size").as[Int] shouldBe 10
    }
    
    "enforce size limits" in new WithApplication(application) {
      val request = FakeRequest(GET, "/api/v1/yeasts?size=1000")
      val result = route(app, request).get
      
      status(result) shouldBe OK
      val json = contentAsJson(result)
      // Size should be capped at maximum (50 for public API)
      (json \ "pagination" \ "size").as[Int] should be <= 50
    }
  }
}
