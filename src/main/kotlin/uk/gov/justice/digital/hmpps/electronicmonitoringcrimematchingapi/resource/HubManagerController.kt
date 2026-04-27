package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CreateHubManagerRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.HubManagerResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.HubManagerMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.HubManagerService
import java.util.UUID

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW')")
@RequestMapping("/hub-managers", produces = ["application/json"])
class HubManagerController(
  private val service: HubManagerService,
  private val mapper: HubManagerMapper,
) {

  @Operation(
    tags = ["Hub Managers"],
    summary = "Create a hub manager",

  )
  @RequestMapping(
    method = [RequestMethod.POST],
    path = [
      "/",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun createHubManager(
    @Valid @RequestBody request: CreateHubManagerRequest,
  ): ResponseEntity<Response<HubManagerResponse>> {
    val manager = service.createHubManager(request)

    return ResponseEntity.status(201).body(
      Response(
        mapper.toDto(
          manager,
        ),
      ),
    )
  }

  @Operation(
    tags = ["Hub Managers"],
    summary = "Delete a hub manager",
  )
  @RequestMapping(
    method = [RequestMethod.DELETE],
    path = [
      "/{id}/",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun deleteHubManager(
    @PathVariable id: UUID,
  ): ResponseEntity<Void> {
    service.deleteHubManager(id)

    return ResponseEntity.noContent().build()
  }

  @Operation(
    tags = ["Hub Managers"],
    summary = "Get a hub manager",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{id}/",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getHubManager(
    @PathVariable id: UUID,
  ): ResponseEntity<Response<HubManagerResponse>> {
    val manager = service.getHubManager(id)

    return ResponseEntity.ok(
      Response(
        mapper.toDto(manager),
      ),
    )
  }

  @Operation(
    tags = ["Hub Managers"],
    summary = "Get a hub manager signature",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{id}/signature",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getHubManagerSignature(
    @PathVariable id: UUID,
  ): ResponseEntity<ByteArray> {
    val manager = service.getHubManager(id)

    if (manager.signatureImage == null) {
      throw EntityNotFoundException("No signature found for hub manager found with id: $id")
    }

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(manager.signatureImageContentType ?: "image/png"))
      .body(manager.signatureImage)
  }

  @Operation(
    tags = ["Hub Managers"],
    summary = "List hub managers",
  )
  @GetMapping(
    path = ["/"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getHubManagers(
    @Parameter(
      description = "Filter by whether the manager has a signature",
    )
    @RequestParam(value = "hasSignature", required = true)
    hasSignature: Boolean = false,
  ): ResponseEntity<Response<List<HubManagerResponse>>> {
    val managers = service.getHubManagers(hasSignature)

    return ResponseEntity.ok(
      Response(
        managers.map { mapper.toDto(it) },
      ),
    )
  }

  @Operation(
    tags = ["Hub Managers"],
    summary = "Update hub manager signature",
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    path = [
      "/{id}/signature",
    ],
    consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun updateHubManagerSignature(
    @PathVariable id: UUID,
    @RequestParam("signature") signature: MultipartFile,
  ): ResponseEntity<Response<HubManagerResponse>> {
    val manager = service.updateHubManagerSignature(id, signature)

    return ResponseEntity.ok(
      Response(
        mapper.toDto(manager),
      ),
    )
  }
}
