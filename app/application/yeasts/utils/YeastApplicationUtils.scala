package application.yeasts.utils

import java.util.UUID
import java.time.Instant
import play.api.libs.json._

case class YeastErrorResponseDTO(
  errors: List[String],
  timestamp: Instant
)

object YeastErrorResponseDTO {
  implicit val format: Format[YeastErrorResponseDTO] = Json.format[YeastErrorResponseDTO]
}

object YeastApplicationUtils {

  def parseUUID(str: String): Either[String, UUID] = {
    try {
      Right(UUID.fromString(str))
    } catch {
      case _: IllegalArgumentException => Left(s"Invalid UUID format: $str")
    }
  }

  def buildErrorResponse(errors: List[String]): YeastErrorResponseDTO = {
    YeastErrorResponseDTO(
      errors = errors,
      timestamp = Instant.now()
    )
  }

  def buildErrorResponse(error: String): YeastErrorResponseDTO = {
    buildErrorResponse(List(error))
  }
}
