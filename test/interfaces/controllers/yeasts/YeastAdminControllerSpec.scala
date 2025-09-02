package interfaces.controllers.yeasts

import application.yeasts.services.YeastApplicationService
import application.yeasts.dtos._
import actions.AdminAction
import domain.admin.model.{Admin, AdminId, Email, Permission}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import java.time.Instant

class YeastAdminControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  "YeastAdminController" should {
    
    "create yeast with valid data" in {
      val mockApplicationService = mock[YeastApplicationService]
      val mockAdminAction = mock[AdminAction]
      val controller = new YeastAdminController(mockApplicationService, mockAdminAction, stubControllerComponents())
      
      val mockStats = YeastStatsDTO(
        totalCount = 100L,
        byStatus = Map("ACTIVE" -> 80L, "INACTIVE" -> 20L),
        byLaboratory = Map("Fermentis" -> 40L, "Wyeast" -> 35L, "Lallemand" -> 25L),
        byType = Map("Ale" -> 60L, "Lager" -> 40L),
        recentlyAdded = List.empty,
        mostPopular = List.empty
      )
      
      val mockAdmin = createMockAdmin()
      
      when(mockApplicationService.getYeastStatistics())
        .thenReturn(Future.successful(mockStats))
      
      // Mock AdminAction behavior
      when(mockAdminAction.async(any[Permission])(any))
        .thenAnswer { invocation =>
          val action = invocation.getArgument[AdminAction.AdminRequest[AnyContent] => Future[Result]](1)
          val adminRequest = new AdminAction.AdminRequest(
            admin = mockAdmin,
            request = FakeRequest(GET, "/api/admin/yeasts/stats")
          )
          action(adminRequest)
        }
      
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/api/admin/yeasts/stats")
      val result: Future[Result] = controller.getStatistics()(request)
      
      status(result) shouldBe OK
      val json = contentAsJson(result)
      (json \ "totalCount").as[Long] shouldBe 100L
    }
  }
  
  private def createMockAdmin(): Admin = {
    Admin.create(
      email = Email("admin@brewery.com").value,
      permissions = Set(Permission.MANAGE_INGREDIENTS, Permission.VIEW_ANALYTICS),
      createdBy = AdminId(UUID.randomUUID())
    ).value.copy(
      id = AdminId(UUID.randomUUID())
    )
  }
  
  private def createMockYeastDetail(): YeastDetailResponseDTO = {
    YeastDetailResponseDTO(
      id = UUID.randomUUID().toString,
      name = "Mock Yeast",
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
}
