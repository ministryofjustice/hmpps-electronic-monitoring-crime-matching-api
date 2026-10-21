package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType

@Component
class DocumentGeneratorRegistry(
  generators: List<DocumentGenerator>,
) {
  private val generatorsByType = generators.associateBy { it.type }

  init {
    require(generatorsByType.size == generators.size) {
      "Multiple document generators registered for the same document type"
    }
  }

  fun get(type: DocumentType): DocumentGenerator = generatorsByType[type]
    ?: error("No generator registered for document type $type")
}
