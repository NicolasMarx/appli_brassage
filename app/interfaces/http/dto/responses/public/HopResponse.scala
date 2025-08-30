// app/interfaces/http/dto/responses/public/HopResponse.scala
package interfaces.http.dto.responses.public

import play.api.libs.json._
import domain.hops.model.HopAggregate
import java.time.Instant

case class HopResponse(
                        id: String,
                        name: String,
                        alphaAcid: Double,
                        betaAcid: Option[Double],
                        origin: HopOriginResponse,
                        usage: String,
                        description: Option[String],
                        aromaProfiles: List[AromaProfileResponse],
                        createdAt: Instant
                      )

case class HopOriginResponse(
                              code: String,
                              name: String,
                              region: String,
                              isNoble: Boolean,
                              isNewWorld: Boolean
                            )

case class AromaProfileResponse(
                                 id: String,
                                 name: String,
                                 category: String,
                                 intensity: Option[Int]
                               )

case class HopListResponse(
                            hops: List[HopResponse],
                            totalCount: Int,
                            page: Int,
                            pageSize: Int,
                            hasNext: Boolean
                          )

// DTOs pour l'admin
case class CreateHopRequest(
                             name: String,
                             alphaAcid: Double,
                             betaAcid: Option[Double],
                             originCode: String,
                             usage: String,
                             description: Option[String]
                           )

case class UpdateHopRequest(
                             name: Option[String],
                             alphaAcid: Option[Double],
                             betaAcid: Option[Double],
                             description: Option[String]
                           )

case class AdminHopResponse(
                             id: String,
                             name: String,
                             alphaAcid: Double,
                             betaAcid: Option[Double],
                             origin: HopOriginResponse,
                             usage: String,
                             description: Option[String],
                             status: String,
                             source: String,
                             credibilityScore: Int,
                             aromaProfiles: List[AromaProfileResponse],
                             createdAt: Instant,
                             updatedAt: Instant
                           )

case class AdminHopListResponse(
                                 hops: List[AdminHopResponse],
                                 totalCount: Int,
                                 page: Int,
                                 pageSize: Int,
                                 hasNext: Boolean
                               )

object HopResponse {
  def fromAggregate(hop: HopAggregate, aromaProfiles: List[AromaProfileResponse] = List.empty): HopResponse = {
    HopResponse(
      id = hop.id.value,
      name = hop.name.value,
      alphaAcid = hop.alphaAcid.value,
      betaAcid = hop.betaAcid.map(_.value),
      origin = HopOriginResponse(
        code = hop.origin.code,
        name = hop.origin.name,
        region = hop.origin.region,
        isNoble = hop.origin.isNoble,
        isNewWorld = hop.origin.isNewWorld
      ),
      usage = hop.usage.name,
      description = hop.description.map(_.value),
      aromaProfiles = aromaProfiles,
      createdAt = hop.createdAt
    )
  }

  // Formats JSON pour tous les types
  implicit val hopOriginResponseFormat: OFormat[HopOriginResponse] = Json.format[HopOriginResponse]
  implicit val aromaProfileResponseFormat: OFormat[AromaProfileResponse] = Json.format[AromaProfileResponse]
  implicit val hopResponseFormat: OFormat[HopResponse] = Json.format[HopResponse]
  implicit val hopListResponseFormat: OFormat[HopListResponse] = Json.format[HopListResponse]
  implicit val createHopRequestFormat: OFormat[CreateHopRequest] = Json.format[CreateHopRequest]
  implicit val updateHopRequestFormat: OFormat[UpdateHopRequest] = Json.format[UpdateHopRequest]
  implicit val adminHopResponseFormat: OFormat[AdminHopResponse] = Json.format[AdminHopResponse]
  implicit val adminHopListResponseFormat: OFormat[AdminHopListResponse] = Json.format[AdminHopListResponse]
}