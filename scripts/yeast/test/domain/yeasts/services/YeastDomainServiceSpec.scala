package domain.yeasts.services

import domain.yeasts.model._
import domain.yeasts.repositories.YeastReadRepository
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

class YeastDomainServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  "YeastDomainService" should {
    
    "detect unique laboratory/strain combinations" in {
      val mockRepo = mock[YeastReadRepository]
      val service = new YeastDomainService(mockRepo)
      
      when(mockRepo.findByLaboratoryAndStrain(YeastLaboratory.Fermentis, YeastStrain("S-04").value))
        .thenReturn(Future.successful(None))
      
      val result = service.checkUniqueLaboratoryStrain(YeastLaboratory.Fermentis, YeastStrain("S-04").value)
      
      result.map(_ shouldBe a[Right[_, _]])
    }
    
    "find similar yeasts by type" in {
      val mockRepo = mock[YeastReadRepository]
      val service = new YeastDomainService(mockRepo)
      val testYeast = createTestYeast()
      
      val similarYeasts = List(createTestYeast(yeastType = YeastType.Ale))
      val mockResult = PaginatedResult(similarYeasts, 1, 0, 10)
      
      when(mockRepo.findByFilter(any[YeastFilter]))
        .thenReturn(Future.successful(mockResult))
      
      val result = service.findSimilarYeasts(testYeast, 5)
      
      result.map(_.length should be <= 5)
    }
    
    "analyze fermentation compatibility" in {
      val mockRepo = mock[YeastReadRepository]
      val service = new YeastDomainService(mockRepo)
      val yeast = createTestYeast()
      
      val report = service.analyzeFermentationCompatibility(yeast, 20, 5.5, 80)
      
      report.yeast shouldBe yeast
      report.overallScore should be >= 0.0
      report.overallScore should be <= 1.0
    }
  }
  
  private def createTestYeast(
    yeastType: YeastType = YeastType.Ale,
    laboratory: YeastLaboratory = YeastLaboratory.Fermentis
  ): YeastAggregate = {
    YeastAggregate.create(
      name = YeastName("Test Yeast").value,
      laboratory = laboratory,
      strain = YeastStrain("T-001").value,
      yeastType = yeastType,
      attenuation = AttenuationRange(75, 82),
      temperature = FermentationTemp(18, 22),
      alcoholTolerance = AlcoholTolerance(10.0),
      flocculation = FlocculationLevel.Medium,
      characteristics = YeastCharacteristics.Clean,
      createdBy = UUID.randomUUID()
    ).value
  }
}
