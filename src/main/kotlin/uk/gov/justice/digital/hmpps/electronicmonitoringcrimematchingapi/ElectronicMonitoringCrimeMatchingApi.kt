package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class ElectronicMonitoringCrimeMatchingApi

fun main(args: Array<String>) {
  runApplication<ElectronicMonitoringCrimeMatchingApi>(*args)
}
