package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {
  @Bean
  @ConditionalOnMissingBean(Clock::class)
  fun clock(): Clock = Clock.systemDefaultZone()
}
