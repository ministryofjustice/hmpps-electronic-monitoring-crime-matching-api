package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CreateHubManagerRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.HubManagerRepository
import java.util.UUID

@Service
class HubManagerService(
  private val repository: HubManagerRepository,
) {
  fun createHubManager(request: CreateHubManagerRequest): HubManager = repository.save(
    HubManager(
      name = request.name,
    ),
  )

  fun deleteHubManager(id: UUID) = repository.deleteById(id)

  fun getHubManager(id: UUID): HubManager = repository.findById(id).orElseThrow { EntityNotFoundException("No hub manager found with id: $id") }

  fun getHubManagers(hasSignature: Boolean): List<HubManager> {
    if (hasSignature) {
      return repository.findBySignatureImageIsNotNull()
    }

    return repository.findAll()
  }

  fun updateHubManagerSignature(id: UUID, file: MultipartFile): HubManager {
    if (file.contentType !in listOf(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE)) {
      throw ValidationException("Invalid file type ${file.contentType}")
    }

    val manager = getHubManager(id)

    manager.signatureImage = file.bytes
    manager.signatureImageContentType = file.contentType

    return repository.save(manager)
  }
}
