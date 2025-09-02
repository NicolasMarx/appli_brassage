package application.yeasts.services

import application.yeasts.dtos._
import application.yeasts.handlers.{YeastCommandHandlers, YeastQueryHandlers}
import domain.yeasts.model._
import domain.yeasts.services.YeastRecommendationService
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

class YeastApplicationServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  "YeastApplicationService" should {
    
    "convert yeast aggregate to detail response DTO" in {
      val yeast = createTestYeast()
      val dto = YeastDTOs.toDetailResponse(yeast)
      
      dto.id shouldBe yeast.id.asString
      dto.name shouldBe yeast.name.value
      dto.laboratory.name shouldBe yeast.laboratory.name
      dto.strain shouldBe yeast.strain.value
      dto.status shouldBe yeast.status.name
    }
    
    "convert yeast aggregate to summary DTO" in {
      val yeast = createTestYeast()
      val dto = YeastDTOs.toSummary(yeast)
      
      dto.id shouldBe yeast.id.asString
      dto.name shouldBe yeast.name.value
      dto.yeastType shouldBe yeast.yeastType.name
      dto.attenuationRange shouldBe yeast.attenuation.toString
    }
    
    "handle create yeast request" in {
      val mockCommandHandlers = mock[YeastCommandHandlers]
      val mockQueryHandlers = mock[YeastQueryHandlers]
      val mockRecommendationService = mock[YeastRecommendationService]
      
      val service = new YeastApplicationService(
        mockCommandHandlers, 
        mockQueryHandlers, 
        mockRecommendationService
      )
      
      val testYeast = createTestYeast()
      val request = CreateYeastRequestDTO(
        name = "Test Yeast",
        laboratory = "Fermentis",
        strain = "S-04",
        yeastType = "Ale",
        attenuationMin = 75,
        attenuationMax = 82,
        temperatureMin = 15,
        temperatureMax = 24,
        alcoholTolerance = 9.0,
        flocculation = "High"
      )
      
      when(mockCommandHandlers.handle(any[CreateYeastCommand]))
        .thenReturn(Future.successful(Right(testYeast)))
      
      val result = service.createYeast(request, UUID.randomUUID())
      
      result.map { response =>
        response shouldBe a[Right[_, _]]
        response.map(_.name shouldBe "Test Yeast")
      }
    }
    
    "handle search request with filters" in {
      val mockCommandHandlers = mock[YeastCommandHandlers]
      val mockQueryHandlers = mock[YeastQueryHandlers]
      val mockRecommendationService = mock[YeastRecommendationService]
      
      val service = new YeastApplicationService(
        mockCommandHandlers, 
        mockQueryHandlers, 
        mockRecommendationService
      )
      
      val testYeasts = List(createTestYeast())
      val mockResult = PaginatedResult(testYeasts, 1, 0, 20)
      
      when(mockQueryHandlers.handle(any[FindYeastsQuery]))
        .thenReturn(Future.successful(Right(mockResult)))
      
      val searchRequest = YeastSearchRequestDTO(
        yeastType = Some("Ale"),
        page = 0,
        size = 20
      )
      
      val result = service.findYeasts(searchRequest)
      
      result.map { response =>
        response shouldBe a[Right[_, _]]
        response.map(_.yeasts should have length 1)
      }
    }
    
    "handle update yeast request" in {
      val mockCommandHandlers = mock[YeastCommandHandlers]
      val mockQueryHandlers = mock[YeastQueryHandlers]
      val mockRecommendationService = mock[YeastRecommendationService]
      
      val service = new YeastApplicationService(
        mockCommandHandlers, 
        mockQueryHandlers, 
        mockRecommendationService
      )
      
      val testYeast = createTestYeast()
      val updateRequest = UpdateYeastRequestDTO(
        name = Some("Updated Yeast Name")
      )
      
      when(mockCommandHandlers.handle(any[UpdateYeastCommand]))
        .thenReturn(Future.successful(Right(testYeast)))
      
      val result = service.updateYeast(testYeast.id.value, updateRequest, UUID.randomUUID())
      
      result.map { response =>
        response shouldBe a[Right[_, _]]
      }
    }
    
    "get statistics" in {
      val mockCommandHandlers = mock[YeastCommandHandlers]
      val mockQueryHandlers = mock[YeastQueryHandlers]
      val mockRecommendationService = mock[YeastRecommendationService]
      
      val service = new YeastApplicationService(
        mockCommandHandlers, 
        mockQueryHandlers, 
        mockRecommendationService
      )
      
      val mockStatusStats = Map(YeastStatus.Active -> 5L, YeastStatus.Inactive -> 2L)
      val mockLabStats = Map(YeastLaboratory.Fermentis -> 3L, YeastLaboratory.Wyeast -> 2L)
      val mockTypeStats = Map(YeastType.Ale -> 4L, YeastType.Lager -> 1L)
      val mockRecentYeasts = List(createTestYeast())
      val mockPopularYeasts = List(createTestYeast())
      
      when(mockQueryHandlers.handle(any[GetYeastStatsQuery]))
        .thenReturn(Future.successful(mockStatusStats))
      when(mockQueryHandlers.handle(any[GetYeastStatsByLaboratoryQuery]))
        .thenReturn(Future.successful(mockLabStats))
      when(mockQueryHandlers.handle(any[GetYeastStatsByTypeQuery]))
        .thenReturn(Future.successful(mockTypeStats))
      when(mockQueryHandlers.handle(any[GetRecentlyAddedYeastsQuery]))
        .thenReturn(Future.successful(Right(mockRecentYeasts)))
      when(mockQueryHandlers.handle(any[GetMostPopularYeastsQuery]))
        .thenReturn(Future.successful(Right(mockPopularYeasts)))
      
      val result = service.getYeastStatistics()
      
      result.map { stats =>
        stats.totalCount shouldBe 7L
        stats.byStatus should contain key "ACTIVE"
        stats.byLaboratory should contain key "Fermentis"
        stats.byType should contain key "Ale"
      }
    }
  }
  
  private def createTestYeast(): YeastAggregate = {
    YeastAggregate.create(
      name = YeastName("Test Yeast").value,
      laboratory = YeastLaboratory.Fermentis,
      strain = YeastStrain("S-04").value,
      yeastType = YeastType.Ale,
      attenuation = AttenuationRange(75, 82),
      temperature = FermentationTemp(15, 24),
      alcoholTolerance = AlcoholTolerance(9.0),
      flocculation = FlocculationLevel.High,
      characteristics = YeastCharacteristics.Clean,
      createdBy = UUID.randomUUID()
    ).value.copy(
      status = YeastStatus.Active,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  }
}
