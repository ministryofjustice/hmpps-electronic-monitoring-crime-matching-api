package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportDocxGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.port.inbound.DocumentGenerator

@Configuration
class DocumentConfiguration {

  @Bean
  fun contextualReportGenerator(
    documentStore: DocumentRequestStore,
  ): ContextualReportDocxGenerator = ContextualReportDocxGenerator(documentStore)

  @Bean
  fun documentGeneratorRegistry(
    generators: List<DocumentGenerator>,
  ): DocumentGeneratorRegistry = DocumentGeneratorRegistry(generators)
}
