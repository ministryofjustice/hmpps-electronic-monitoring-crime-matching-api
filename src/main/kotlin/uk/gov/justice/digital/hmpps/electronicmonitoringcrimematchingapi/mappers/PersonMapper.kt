package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.AthenaPersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person

@Component
class PersonMapper(
  val deviceActivationMapper: DeviceActivationMapper,
) {
  fun fromModelToDto(person: Person): PersonDto = with(person) {
    PersonDto(
      personId = person.personId,
      name = person.personName,
      nomisId = person.nomisId,
      pncRef = "",
      dateOfBirth = dob,
      probationPractitioner = "",
      address = "$street $city $zip",
      deviceActivations = deviceActivations.map { deviceActivationMapper.fromModelToDto(it) },
    )
  }

  fun fromAthenaToModel(person: AthenaPersonDto): Person = with(person) {
    Person(
      personId = personId,
      personName = personName,
      nomisId = uIdNomis,
      pncRef = "",
      probationPractitioner = "",
      dob = uDob,
      zip = zip,
      city = city,
      street = street,
      deviceActivations = listOf(),
    )
  }
}
