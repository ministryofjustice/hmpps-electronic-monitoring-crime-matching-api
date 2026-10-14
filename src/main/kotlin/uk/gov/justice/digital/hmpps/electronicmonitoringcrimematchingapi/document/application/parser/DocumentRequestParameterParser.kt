package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.InvalidRequestException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.time.Instant

@Component
class DocumentRequestParameterParser {
  fun parse(type: DocumentType, parameters: Map<String, Any>): DocumentRequestParameters = try {
    when (type) {
      DocumentType.CONTEXTUAL_REPORT -> parseContextualReportParameters(parameters)

      else -> throw IllegalArgumentException(
        "Unsupported document type: $type",
      )
    }
  } catch (e: IllegalArgumentException) {
    throw InvalidRequestException(e.message ?: "Invalid document request parameters", e)
  }

  private fun parseContextualReportParameters(parameters: Map<String, Any>): ContextualReportRequestParameters {
    val deviceId =
      (parameters["deviceId"] as? Number)
        ?.toInt()
        ?: throw IllegalArgumentException("Missing or invalid 'deviceId' parameter")

    val from =
      (parameters["from"] as? String)
        ?.let { Instant.parse(it) }
        ?: throw IllegalArgumentException("Missing or invalid 'from' parameter")

    val to =
      (parameters["to"] as? String)
        ?.let { Instant.parse(it) }
        ?: throw IllegalArgumentException("Missing or invalid 'to' parameter")

    return ContextualReportRequestParameters(
      deviceId = deviceId,
      from = from,
      to = to,
    )
  }
}
