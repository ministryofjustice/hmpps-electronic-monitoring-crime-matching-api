package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.application.usecase

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.inbound.web.CreateContextualReportRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.positionsegments.PositionSegmentsProvider

@Service
class CreateContextualReport(
  private val positionSegmentsProvider: PositionSegmentsProvider,
  private val createDocumentRequest: CreateDocumentRequest,
) {
  fun create(crimeMatchingResultId: String, deviceActivationId: Long, request: CreateContextualReportRequest, user: String): String {
    // Get position segments data
    val positionSegments = positionSegmentsProvider.getPositionSegments(
      deviceActivationId = deviceActivationId.toString(),
      startTime = request.startTime.toString(),
      endTime = request.endTime.toString(),
    )

    // Construct document request
    val reportParameters = mapOf(
      "additionalInformation" to request.additionalInformation,
      // Get from hub manager table
      "authorisingManager" to mapOf(
        "name" to "Example Manager",
        "occupation" to "AC Hub Manager",
      ),
      // Get from crime version linked to matching result
      "crime" to mapOf(
        "batchId" to "EXAMPLE-BATCH",
        "proximityAlertCreatedAt" to "2025-08-16T00:00:00Z",
        "reference" to "EXAMPLE-REFERENCE",
        "type" to "Example crime type",
      ),
      // Get from device wearer details
      "deviceWearer" to mapOf(
        "address" to "Example address",
        "dateOfBirth" to "1990-01-01",
        "emsId" to "EXAMPLE-EMS-ID",
        "pncNumber" to "EXAMPLE-PNC",
        "name" to "Example wearer",
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
