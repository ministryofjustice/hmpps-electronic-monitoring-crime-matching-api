package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.deviceActivation

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceActivationResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PositionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.deviceActivation.DeviceActivationService
import java.time.ZonedDateTime

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/device-activations", produces = ["application/json"])
class DeviceActivationController(
  private val deviceActivationService: DeviceActivationService,
) {
  @Operation(
    tags = ["Device Activation"],
    summary = "Get a device activation",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{deviceActivationId}",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getDeviceActivation(
    @PathVariable deviceActivationId: Long,
  ): ResponseEntity<Response<DeviceActivationResponse>> {
    val deviceActivation = deviceActivationService.getDeviceActivation(deviceActivationId)

    return ResponseEntity.ok(
      Response(
        DeviceActivationResponse(deviceActivation),
      ),
    )
  }

  @Operation(
    tags = ["Device Activation"],
    summary = "Get positions for a device activation",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{deviceActivationId}/positions",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getDeviceActivationPositions(
    @PathVariable deviceActivationId: Long,
    geolocationMechanism: GeolocationMechanism? = null,
    from: ZonedDateTime? = null,
    to: ZonedDateTime? = null,
  ): ResponseEntity<Response<List<PositionResponse>>> {
    val positions = deviceActivationService.getDeviceActivationPositions(
      deviceActivationId,
      geolocationMechanism,
      from,
      to,
    )

    return ResponseEntity.ok(
      Response(
        positions.map { PositionResponse(it) },
      ),
    )
  }
}
