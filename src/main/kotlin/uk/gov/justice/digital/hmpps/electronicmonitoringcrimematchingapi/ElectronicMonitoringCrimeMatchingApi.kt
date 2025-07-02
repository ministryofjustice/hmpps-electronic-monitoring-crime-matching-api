package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ElectronicMonitoringCrimeMatchingApi

fun main(args: Array<String>) {
  runApplication<ElectronicMonitoringCrimeMatchingApi>(*args)
}
