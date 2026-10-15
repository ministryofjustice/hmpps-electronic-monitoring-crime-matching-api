package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser

import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.InvalidRequestException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters

@Component
class DocumentRequestParameterParser(
  private val objectMapper: ObjectMapper,
) {
  fun parse(
    type: DocumentType,
    parameters: Map<String, Any>,
  ): DocumentRequestParameters = try {
    when (type) {
      DocumentType.CONTEXTUAL_REPORT ->
        objectMapper.convertValue(
          parameters,
          ContextualReportRequestParameters::class.java,
        )

      else -> throw IllegalArgumentException(
        "Unsupported document type: $type",
      )
    }
  } catch (e: JacksonException) {
    val validationError = generateSequence(e as Throwable) { it.cause }
      .filterIsInstance<IllegalArgumentException>()
      .firstOrNull()

    throw InvalidRequestException(
      validationError?.message
        ?: "Invalid parameters for document type $type",
      e,
    )
  } catch (e: IllegalArgumentException) {
    throw InvalidRequestException(
      e.message ?: "Invalid parameters for document type $type",
      e,
    )
  }
}
