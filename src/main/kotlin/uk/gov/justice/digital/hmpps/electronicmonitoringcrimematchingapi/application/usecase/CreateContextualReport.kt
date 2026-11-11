package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.application.usecase

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.inbound.web.CreateContextualReportRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.positionsegments.PositionSegmentsProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.HubManagerService
import java.util.UUID

@Service
class CreateContextualReport(
  private val positionSegmentsProvider: PositionSegmentsProvider,
  private val createDocumentRequest: CreateDocumentRequest,
  private val hubManagerService: HubManagerService,
  private val crimeMatchingResultStore: CrimeMatchingResultStore,
) {
  fun create(crimeMatchingResultId: UUID, deviceId: Long, request: CreateContextualReportRequest, user: String): String {
    // Get Crime matching result data
    val crimeMatchingResult = crimeMatchingResultStore.find(crimeMatchingResultId, deviceId)
      ?: throw EntityNotFoundException("No matched wearer with device ID $deviceId for crime matching result $crimeMatchingResultId")

    // Get Hub manager
    val hubManager = hubManagerService.getHubManager(request.hubManagerId)

    // Get position segments data
    val positionSegments = positionSegmentsProvider.getPositionSegments(
      deviceId = deviceId.toString(),
      startTime = request.startTime.toString(),
      endTime = request.endTime.toString(),
    )

    // Construct document request
    val reportParameters = mapOf(
      "additionalInformation" to request.additionalInformation,
      "authorisingManager" to mapOf(
        "name" to hubManager.name,
        "occupation" to hubManager.occupation,
      ),
      "crime" to mapOf(
        "batchId" to crimeMatchingResult.batchId,
        "proximityAlertCreatedAt" to crimeMatchingResult.matchingResultCreatedAt.toString(),
        "reference" to crimeMatchingResult.crimeReference,
        "type" to crimeMatchingResult.crimeType,
      ),
      "deviceWearer" to mapOf(
        "address" to crimeMatchingResult.address,
        "dateOfBirth" to crimeMatchingResult.dateOfBirth.toString(),
        "emsId" to crimeMatchingResult.identifier,
        "pncNumber" to crimeMatchingResult.pncRef,
        "name" to crimeMatchingResult.name,
        "offenderManager" to "Example manager",
      ),
      "positionSegments" to positionSegments,
      "reportPeriod" to mapOf(
        "start" to request.startTime.toString(),
        "end" to request.endTime.toString(),
      ),
      "reportRequest" to mapOf(
        "policeForceArea" to request.policeForceArea,
        "requestedAt" to request.requestedAt.toString(),
        "requestedBy" to request.officerName,
      ),
    )

    val reportRequest = CreateDocumentRequestCommand(
      parameters = reportParameters,
      type = DocumentType.CONTEXTUAL_REPORT,
    )

    val documentRequest = createDocumentRequest.create(
      request = reportRequest,
      user = user,
    )

    return documentRequest.id
  }
}
