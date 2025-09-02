package interfaces.controllers.yeasts

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.libs.json._
import play.api.cache.AsyncCacheApi
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

class YeastPublicControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  "YeastPublicController" should {
    
    "return active yeasts list" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      val mockResult = YeastPageResponseDTO(
        yeasts = mockYeasts,
        pagination = PaginationDTO(0, 20, 1, 1, false, false)
      )
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockResult)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts")
      val result: Future[Result] = controller.listActiveYeasts()(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return yeast details by ID" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val yeastId = UUID.randomUUID()
      val mockYeast = createMockYeastDetail(yeastId)
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Some(mockYeast)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result: Future[Result] = controller.getYeast(yeastId.toString)(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      
      val json = contentAsJson(result)
      (json \ "id").as[String] shouldBe yeastId.toString
    }
    
    "return 404 for non-existent yeast" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val yeastId = UUID.randomUUID()
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(None))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/v1/yeasts/$yeastId")
      val result: Future[Result] = controller.getYeast(yeastId.toString)(request)
      
      status(result) shouldBe NOT_FOUND
    }
    
    "return 400 for invalid UUID" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/invalid-uuid")
      val result: Future[Result] = controller.getYeast("invalid-uuid")(request)
      
      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "errors").as[List[String]] should not be empty
    }
    
    "perform text search with valid query" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockYeasts)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/search?q=test")
      val result: Future[Result] = controller.searchYeasts("test")(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return 400 for search query too short" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/search?q=a")
      val result: Future[Result] = controller.searchYeasts("a")(request)
      
      status(result) shouldBe BAD_REQUEST
    }
    
    "return yeasts by type" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockYeasts = List(createMockYeastSummary())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(Right(mockYeasts)))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/type/Ale")
      val result: Future[Result] = controller.getYeastsByType("Ale")(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
    
    "return beginner recommendations" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockCache = mock[AsyncCacheApi]
      val controller = new YeastPublicController(mockApplicationService, mockCache, stubControllerComponents())
      
      val mockRecommendations = List(createMockRecommendation())
      
      when(mockCache.getOrElseUpdate(any[String], any, any)(any))
        .thenReturn(Future.successful(mockRecommendations))
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/v1/yeasts/recommendations/beginner")
      val result: Future[Result] = controller.getBeginnerRecommendations()(request)
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
    }
  }
  
  private def createMockYeastSummary(): YeastSummaryDTO = {
    YeastSummaryDTO(
      id = UUID.randomUUID().toString,
      name = "Test Yeast",
      laboratory = "Fermentis",
      strain = "S-04",
      yeastType = "Ale",
      attenuationRange = "75-82%",
      temperatureRange = "15-24°C",
      alcoholTolerance = "9.0%",
      flocculation = "High",
      status = "ACTIVE",
      mainCharacteristics = List("Clean", "Fruity")
    )
  }
  
  private def createMockYeastDetail(yeastId: UUID): YeastDetailResponseDTO = {
    YeastDetailResponseDTO(
      id = yeastId.toString,
      name = "Test Yeast Detailed",
      laboratory = YeastLaboratoryDTO("Fermentis", "F", "French laboratory"),
      strain = "S-04",
      yeastType = YeastTypeDTO("Ale", "Top fermenting", "15-25°C", "Fruity esters"),
      attenuation = AttenuationRangeDTO(75, 82, 78.5, "75-82%"),
      temperature = FermentationTempDTO(15, 24, 19.5, "15-24°C", "59-75°F"),
      alcoholTolerance = AlcoholToleranceDTO(9.0, "9.0%", "Medium"),
      flocculation = FlocculationLevelDTO("High", "High flocculation", "3-7 days", "Early racking possible"),
      characteristics = YeastCharacteristicsDTO(
        aromaProfile = List("Clean", "Fruity"),
        flavorProfile = List("Balanced"),
        esters = List("Ethyl acetate"),
        phenols = List.empty,
        otherCompounds = List.empty,
        notes = Some("Great for beginners"),
        summary = "Clean and fruity profile"
      ),
      status = "ACTIVE",
      version = 1L,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      recommendations = Some(List("Ferment at 18-20°C")),
      warnings = Some(List.empty)
    )
  }
  
  private def createMockRecommendation(): YeastRecommendationDTO = {
    YeastRecommendationDTO(
      yeast = createMockYeastSummary(),
      score = 0.9,
      reason = "Perfect for beginners",
      tips = List("Use at moderate temperature", "Easy to handle")
    )
  }
}
