package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.Osgb36ToWgs84Converter

@Configuration
class GeoConfig {
  @Bean
  fun coordinateResolver() = CoordinateResolver(Osgb36ToWgs84Converter())
}
