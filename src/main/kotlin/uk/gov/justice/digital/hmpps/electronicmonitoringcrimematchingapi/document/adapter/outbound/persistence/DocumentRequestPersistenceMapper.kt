package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence

import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser.DocumentRequestParameterParser
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestParameters

@Component
class DocumentRequestPersistenceMapper(
  private val parser: DocumentRequestParameterParser,
  private val objectMapper: ObjectMapper,
) {
  fun toEntity(request: DocumentRequest): DocumentRequestEntity = DocumentRequestEntity(
    id = request.id,
    type = request.type,
    status = request.status,
    parameters = serializeParameters(request.parameters),
    createdBy = request.createdBy,
    createdAt = request.createdAt,
  )

  fun toDomain(entity: DocumentRequestEntity): DocumentRequest = DocumentRequest.rehydrate(
    id = entity.id,
    type = entity.type,
    parameters = parser.parse(entity.type, entity.parameters),
    status = entity.status,
    createdBy = entity.createdBy,
    createdAt = entity.createdAt,
  )

  private fun serializeParameters(
    parameters: DocumentRequestParameters,
  ): Map<String, Any> {
    val tree = objectMapper.valueToTree<ObjectNode>(parameters)
    tree.remove("type")

    return objectMapper.convertValue(
      tree,
      object : TypeReference<Map<String, Any>>() {},
    )
  }
}
