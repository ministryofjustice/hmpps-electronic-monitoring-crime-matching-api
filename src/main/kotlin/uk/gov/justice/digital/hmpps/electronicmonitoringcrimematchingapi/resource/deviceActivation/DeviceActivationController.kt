package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.deviceActivation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PositionDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.ResponseDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.deviceActivation.DeviceActivationService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.ZonedDateTime

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/device-activations", produces = ["application/json"])
class DeviceActivationController(
  private val deviceActivationService: DeviceActivationService,
) {
  @Operation(tags = ["Device Activations"], summary = "Get a device activation")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Device activation found",
        useReturnTypeSchema = true,
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Device activation not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping("/{deviceActivationId}")
  fun getDeviceActivation(
    @PathVariable deviceActivationId: Long,
  ): ResponseEntity<ResponseDto<DeviceActivationDto>> {
    val deviceActivation = deviceActivationService.getDeviceActivation(deviceActivationId)

    return ResponseEntity.ok(
      ResponseDto(
        DeviceActivationDto(deviceActivation),
      ),
    )
  }

  @Operation(tags = ["Device Activations"], summary = "Get device activation positions")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Device activation positions found",
        useReturnTypeSchema = true,
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping("/{deviceActivationId}/positions")
  fun getDeviceActivationPositions(
    @PathVariable deviceActivationId: Long,
    geolocationMechanism: GeolocationMechanism? = null,
    from: ZonedDateTime? = null,
    to: ZonedDateTime? = null,
  ): ResponseEntity<ResponseDto<List<PositionDto>>> {
    val positions = deviceActivationService.getDeviceActivationPositions(
      deviceActivationId,
      geolocationMechanism,
      from,
      to,
    )

    return ResponseEntity.ok(
      ResponseDto(
        positions.map { PositionDto(it) },
      ),
    )
  }
}
