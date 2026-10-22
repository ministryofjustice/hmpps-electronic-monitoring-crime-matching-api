package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class DocumentProcessingConfiguration {
  @Bean
  fun documentProcessingExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
    corePoolSize = 1
    maxPoolSize = 1
    queueCapacity = 100
    setThreadNamePrefix("document-processing-")
  }
}
