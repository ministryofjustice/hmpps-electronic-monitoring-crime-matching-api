package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.Osgb36ToWgs84Converter

@Configuration
class GeoConfig {
  @Bean
  fun osgb36ToWgs84Converter() = Osgb36ToWgs84Converter()
}
